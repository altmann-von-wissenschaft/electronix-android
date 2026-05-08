package com.pnzgu.electronix.data.api

import android.content.Context
import com.pnzgu.electronix.BuildConfig
import com.pnzgu.electronix.R
import java.security.KeyStore
import java.security.SecureRandom
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.TrustManagerFactory
import javax.net.ssl.X509TrustManager
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

object ApiFactory {
    fun createJson(): Json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        explicitNulls = false
    }

    /**
     * Debug only: accept self-signed / private CA HTTPS (e.g. dev server). Never enabled for release.
     */
    private fun OkHttpClient.Builder.applyDevTlsIfDebug(): OkHttpClient.Builder = apply {
        if (!BuildConfig.DEBUG) return@apply
        val trustAll = arrayOf<TrustManager>(
            object : X509TrustManager {
                override fun checkClientTrusted(chain: Array<out X509Certificate>?, authType: String?) {}
                override fun checkServerTrusted(chain: Array<out X509Certificate>?, authType: String?) {}
                override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
            },
        )
        val ctx = SSLContext.getInstance("TLS")
        ctx.init(null, trustAll, SecureRandom())
        sslSocketFactory(ctx.socketFactory, trustAll[0] as X509TrustManager)
        hostnameVerifier { _, _ -> true }
    }

    /**
     * Release-safe self-signed/private CA support.
     * If `res/raw/electronix_ca.crt` exists and is valid, trust it in addition to system CAs.
     */
    private fun OkHttpClient.Builder.applyBundledCaIfPresent(context: Context): OkHttpClient.Builder = apply {
        if (BuildConfig.DEBUG) return@apply
        val customTrust = loadBundledTrustManagerOrNull(context) ?: return@apply
        val systemTrust = systemTrustManager()
        val merged = CompositeX509TrustManager(listOf(customTrust, systemTrust))
        val sslContext = SSLContext.getInstance("TLS")
        sslContext.init(null, arrayOf<TrustManager>(merged), SecureRandom())
        sslSocketFactory(sslContext.socketFactory, merged)
    }

    private fun loadBundledTrustManagerOrNull(context: Context): X509TrustManager? = runCatching {
        val certFactory = CertificateFactory.getInstance("X.509")
        val cert = context.resources.openRawResource(R.raw.electronix_ca).use { input ->
            certFactory.generateCertificate(input) as X509Certificate
        }
        val ks = KeyStore.getInstance(KeyStore.getDefaultType()).apply { load(null, null) }
        ks.setCertificateEntry("electronix_ca", cert)
        val tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm()).apply { init(ks) }
        tmf.trustManagers.first { it is X509TrustManager } as X509TrustManager
    }.getOrNull()

    private fun systemTrustManager(): X509TrustManager {
        val tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm()).apply { init(null as KeyStore?) }
        return tmf.trustManagers.first { it is X509TrustManager } as X509TrustManager
    }

    fun createApi(
        context: Context,
        tokenHolder: AuthTokenHolder,
        json: Json = createJson(),
    ): ElectronixApi {
        val logging = HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG) {
                HttpLoggingInterceptor.Level.BODY
            } else {
                HttpLoggingInterceptor.Level.NONE
            }
        }
        val client = OkHttpClient.Builder()
            .applyBundledCaIfPresent(context)
            .applyDevTlsIfDebug()
            .addInterceptor(AuthInterceptor(tokenHolder))
            .addInterceptor(logging)
            .build()
        return Retrofit.Builder()
            .baseUrl(BuildConfig.API_BASE_URL)
            .client(client)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(ElectronixApi::class.java)
    }

    fun createImageOkHttpClient(context: Context, tokenHolder: AuthTokenHolder): OkHttpClient =
        OkHttpClient.Builder()
            .applyBundledCaIfPresent(context)
            .applyDevTlsIfDebug()
            .addInterceptor(AuthInterceptor(tokenHolder))
            .build()
}

private class CompositeX509TrustManager(
    private val delegates: List<X509TrustManager>,
) : X509TrustManager {
    override fun getAcceptedIssuers(): Array<X509Certificate> =
        delegates.flatMap { it.acceptedIssuers.asList() }.toTypedArray()

    override fun checkClientTrusted(chain: Array<out X509Certificate>?, authType: String?) {
        var last: Exception? = null
        for (tm in delegates) {
            try {
                tm.checkClientTrusted(chain, authType)
                return
            } catch (e: Exception) {
                last = e
            }
        }
        throw last ?: IllegalStateException("No trust manager accepted the client certificate chain")
    }

    override fun checkServerTrusted(chain: Array<out X509Certificate>?, authType: String?) {
        var last: Exception? = null
        for (tm in delegates) {
            try {
                tm.checkServerTrusted(chain, authType)
                return
            } catch (e: Exception) {
                last = e
            }
        }
        throw last ?: IllegalStateException("No trust manager accepted the server certificate chain")
    }
}
