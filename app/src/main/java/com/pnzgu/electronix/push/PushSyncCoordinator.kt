package com.pnzgu.electronix.push

import android.content.Context
import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.messaging.FirebaseMessaging
import com.pnzgu.electronix.data.api.AuthTokenHolder
import com.pnzgu.electronix.data.api.ElectronixApi
import com.pnzgu.electronix.data.dto.PutFcmTokenRequest
import com.pnzgu.electronix.data.dto.PutPushPreferencesRequest
import com.pnzgu.electronix.data.local.AppPreferences
import kotlinx.coroutines.tasks.await

/**
 * Registers FCM token and push preference flags on the server so the backend can target pushes
 * without the app polling in the background.
 */
class PushSyncCoordinator(
    context: Context,
    private val api: ElectronixApi,
    private val preferences: AppPreferences,
    private val tokenHolder: AuthTokenHolder,
) {
    private val appContext = context.applicationContext

    suspend fun syncFcmTokenAndPreferences() {
        if (tokenHolder.token.isNullOrBlank()) return
        if (!isFirebaseConfigured()) {
            Log.w(TAG, "FCM sync skipped: Firebase config is placeholder or missing")
            return
        }
        val token = runCatching { FirebaseMessaging.getInstance().token.await() }.getOrNull()
            ?: return
        runCatching { api.putFcmToken(PutFcmTokenRequest(token)) }
            .onFailure { Log.w(TAG, "Failed to register FCM token on backend", it) }
        syncPushPreferencesToServer()
    }

    suspend fun syncPushPreferencesToServer() {
        if (tokenHolder.token.isNullOrBlank()) return
        val snap = preferences.readNotificationSnapshot()
        runCatching {
            api.putPushPreferences(
                PutPushPreferencesRequest(
                    notifyOrderStatus = snap.notifyOrderStatus,
                    notifySupportReply = snap.notifySupportReply,
                    notifyReviewModeration = snap.notifyReviewModeration,
                    notifySupportQueue = snap.notifySupportQueue,
                ),
            )
        }.onFailure { Log.w(TAG, "Failed to sync push preferences", it) }
    }

    suspend fun unregisterDeviceOnLogout() {
        val token = runCatching { FirebaseMessaging.getInstance().token.await() }.getOrNull()
        if (token != null) {
            runCatching { api.deleteFcmToken(PutFcmTokenRequest(token)) }
                .onFailure { Log.w(TAG, "Failed to unregister FCM token on backend", it) }
            runCatching { FirebaseMessaging.getInstance().deleteToken().await() }
                .onFailure { Log.w(TAG, "Failed to delete local FCM token", it) }
        }
    }

    private fun isFirebaseConfigured(): Boolean {
        val app = FirebaseApp.getApps(appContext).firstOrNull() ?: return false
        val projectId = app.options.projectId.orEmpty()
        val appId = app.options.applicationId.orEmpty()
        return projectId.isNotBlank() &&
            !projectId.contains("placeholder", ignoreCase = true) &&
            appId.isNotBlank() &&
            !appId.contains("000000000000:android:0000000000000000000000")
    }

    private companion object {
        const val TAG = "PushSyncCoordinator"
    }
}
