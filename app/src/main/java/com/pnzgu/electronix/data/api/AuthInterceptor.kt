package com.pnzgu.electronix.data.api

import okhttp3.Interceptor
import okhttp3.Response

class AuthInterceptor(
    private val tokenHolder: AuthTokenHolder,
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val token = tokenHolder.token
        val request = if (token.isNullOrBlank()) {
            chain.request()
        } else {
            chain.request().newBuilder()
                .header("Authorization", "Bearer $token")
                .build()
        }
        return chain.proceed(request)
    }
}
