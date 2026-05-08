package com.pnzgu.electronix

import android.content.Context
import coil3.ImageLoader
import coil3.disk.DiskCache
import coil3.memory.MemoryCache
import coil3.network.okhttp.OkHttpNetworkFetcherFactory
import coil3.util.DebugLogger
import com.pnzgu.electronix.BuildConfig
import com.pnzgu.electronix.data.api.ApiFactory
import com.pnzgu.electronix.data.api.AuthTokenHolder
import com.pnzgu.electronix.data.api.ElectronixApi
import com.pnzgu.electronix.data.local.AppPreferences
import com.pnzgu.electronix.data.dto.CartDto
import com.pnzgu.electronix.data.repository.CatalogRepository
import com.pnzgu.electronix.data.repository.SessionRepository
import com.pnzgu.electronix.push.PushSyncCoordinator
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import okio.Path.Companion.toOkioPath

class AppContainer(context: Context) {
    private val appContext = context.applicationContext

    val preferences = AppPreferences(appContext)
    val authTokenHolder = AuthTokenHolder()
    val json = ApiFactory.createJson()
    val api: ElectronixApi = ApiFactory.createApi(appContext, authTokenHolder, json)

    val catalogRepository = CatalogRepository(api)
    val sessionRepository = SessionRepository(api, preferences, authTokenHolder)
    val pushSync = PushSyncCoordinator(appContext, api, preferences, authTokenHolder)

    private val _supportListsRefresh = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val supportListsRefresh: SharedFlow<Unit> = _supportListsRefresh.asSharedFlow()

    private val _drawerBadgesRefresh = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val drawerBadgesRefresh: SharedFlow<Unit> = _drawerBadgesRefresh.asSharedFlow()

    /** Total positions (cart lines) in cart; drives catalog cart icon badge. */
    private val _cartBadgeCount = MutableStateFlow(0)
    val cartBadgeCount: StateFlow<Int> = _cartBadgeCount.asStateFlow()

    fun updateCartBadgeFromSnapshot(cart: CartDto) {
        _cartBadgeCount.value = cart.items.size
    }

    fun resetCartBadge() {
        _cartBadgeCount.value = 0
    }

    suspend fun refreshCartBadgeFromServer() {
        if (authTokenHolder.token.isNullOrBlank()) {
            resetCartBadge()
            return
        }
        runCatching { api.cart() }
            .onSuccess { updateCartBadgeFromSnapshot(it) }
    }

    fun notifySupportListsChanged() {
        _supportListsRefresh.tryEmit(Unit)
    }

    fun requestDrawerBadgesRefresh() {
        _drawerBadgesRefresh.tryEmit(Unit)
    }

    val imageLoader: ImageLoader = ImageLoader.Builder(appContext)
        .components {
            add(
                OkHttpNetworkFetcherFactory(
                    callFactory = { ApiFactory.createImageOkHttpClient(appContext, authTokenHolder) },
                ),
            )
        }
        .memoryCache {
            MemoryCache.Builder()
                .maxSizePercent(appContext, 0.25)
                .build()
        }
        .diskCache {
            DiskCache.Builder()
                .directory(appContext.cacheDir.resolve("image_cache").toOkioPath())
                .maxSizeBytes(256L * 1024 * 1024)
                .build()
        }
        .apply { if (BuildConfig.DEBUG) logger(DebugLogger()) }
        .build()

    val contentBaseUrl: String get() = BuildConfig.CONTENT_BASE_URL
}
