package com.pnzgu.electronix.data.local

import android.content.Context
import android.content.SharedPreferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.pnzgu.electronix.ui.theme.ThemeMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "electronix_prefs")

class AppPreferences(
    private val context: Context,
) {
    private val tokenKey = "jwt"
    private val securePrefs: SharedPreferences by lazy {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        EncryptedSharedPreferences.create(
            context,
            "electronix_secure_prefs",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
        )
    }
    private val _accessToken = MutableStateFlow(securePrefs.getString(tokenKey, null))
    private val keyTheme = intPreferencesKey("theme_mode")

    private val keyNotifyOrderStatus = booleanPreferencesKey("notify_order_status")
    private val keyNotifySupportReply = booleanPreferencesKey("notify_support_reply")
    private val keyNotifyReviewModeration = booleanPreferencesKey("notify_review_moderation")
    private val keyNotifySupportQueue = booleanPreferencesKey("notify_support_queue")

    private val keyLastSeenOrders = longPreferencesKey("last_seen_orders_ms")
    private val keyLastSeenSupportMy = longPreferencesKey("last_seen_support_my_ms")
    private val keyLastSeenSupportQueue = longPreferencesKey("last_seen_support_queue_ms")
    private val keyLastSeenModReviews = longPreferencesKey("last_seen_mod_reviews_ms")
    private val keyNotifBaselinesDone = booleanPreferencesKey("notif_baselines_done")

    val accessTokenFlow: Flow<String?> = _accessToken

    val themeModeFlow: Flow<ThemeMode> = context.dataStore.data.map { prefs ->
        when (prefs[keyTheme]) {
            1 -> ThemeMode.Light
            2 -> ThemeMode.Dark
            else -> ThemeMode.System
        }
    }

    val notificationSnapshotFlow: Flow<NotificationSnapshot> = context.dataStore.data.map { p ->
        NotificationSnapshot(
            notifyOrderStatus = p[keyNotifyOrderStatus] ?: true,
            notifySupportReply = p[keyNotifySupportReply] ?: true,
            notifyReviewModeration = p[keyNotifyReviewModeration] ?: true,
            notifySupportQueue = p[keyNotifySupportQueue] ?: true,
            lastSeenOrdersMs = p[keyLastSeenOrders] ?: 0L,
            lastSeenSupportMyMs = p[keyLastSeenSupportMy] ?: 0L,
            lastSeenSupportQueueMs = p[keyLastSeenSupportQueue] ?: 0L,
            lastSeenModReviewsMs = p[keyLastSeenModReviews] ?: 0L,
        )
    }

    suspend fun readNotificationSnapshot(): NotificationSnapshot =
        notificationSnapshotFlow.first()

    suspend fun setAccessToken(token: String?) {
        if (token == null) {
            securePrefs.edit().remove(tokenKey).apply()
        } else {
            securePrefs.edit().putString(tokenKey, token).apply()
        }
        _accessToken.value = token
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        context.dataStore.edit { prefs ->
            prefs[keyTheme] = when (mode) {
                ThemeMode.System -> 0
                ThemeMode.Light -> 1
                ThemeMode.Dark -> 2
            }
        }
    }

    suspend fun setNotifyOrderStatus(value: Boolean) {
        context.dataStore.edit { it[keyNotifyOrderStatus] = value }
    }

    suspend fun setNotifySupportReply(value: Boolean) {
        context.dataStore.edit { it[keyNotifySupportReply] = value }
    }

    suspend fun setNotifyReviewModeration(value: Boolean) {
        context.dataStore.edit { it[keyNotifyReviewModeration] = value }
    }

    suspend fun setNotifySupportQueue(value: Boolean) {
        context.dataStore.edit { it[keyNotifySupportQueue] = value }
    }

    /** First login: start from “now” so old history does not flood badges. */
    suspend fun ensureNotificationBaselines() {
        context.dataStore.edit { prefs ->
            if (prefs[keyNotifBaselinesDone] != true) {
                val now = System.currentTimeMillis()
                prefs[keyLastSeenOrders] = now
                prefs[keyLastSeenSupportMy] = now
                prefs[keyLastSeenSupportQueue] = now
                prefs[keyLastSeenModReviews] = now
                prefs[keyNotifBaselinesDone] = true
            }
        }
    }

    /**
     * Turn off prefs the current role cannot use; ensure defaults when a role is newly available.
     */
    suspend fun syncNotificationPrefsForRoles(isModerator: Boolean, isManager: Boolean) {
        context.dataStore.edit { prefs ->
            if (isModerator) {
                if (prefs[keyNotifyReviewModeration] == null) prefs[keyNotifyReviewModeration] = true
            } else {
                prefs[keyNotifyReviewModeration] = false
            }
            if (isManager) {
                if (prefs[keyNotifySupportQueue] == null) prefs[keyNotifySupportQueue] = true
            } else {
                prefs[keyNotifySupportQueue] = false
            }
        }
    }

    suspend fun markOrdersSectionVisited() {
        context.dataStore.edit { it[keyLastSeenOrders] = System.currentTimeMillis() }
    }

    suspend fun markSupportMySectionVisited() {
        context.dataStore.edit { it[keyLastSeenSupportMy] = System.currentTimeMillis() }
    }

    suspend fun markSupportQueueSectionVisited() {
        context.dataStore.edit { it[keyLastSeenSupportQueue] = System.currentTimeMillis() }
    }

    suspend fun markModReviewsSectionVisited() {
        context.dataStore.edit { it[keyLastSeenModReviews] = System.currentTimeMillis() }
    }
}
