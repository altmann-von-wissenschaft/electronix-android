package com.pnzgu.electronix.push

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.pnzgu.electronix.ElectronixApplication
import com.pnzgu.electronix.MainActivity
import com.pnzgu.electronix.R
import com.pnzgu.electronix.data.dto.PutFcmTokenRequest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

class ElectronixFirebaseMessagingService : FirebaseMessagingService() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onNewToken(token: String) {
        val app = applicationContext as? ElectronixApplication ?: return
        scope.launch {
            if (app.container.authTokenHolder.token.isNullOrBlank()) return@launch
            runCatching { app.container.api.putFcmToken(PutFcmTokenRequest(token)) }
                .onFailure { Log.w("ElectronixFCM", "putFcmToken failed in onNewToken", it) }
        }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        val type = message.data["type"] ?: return
        val app = applicationContext as? ElectronixApplication ?: return
        val allowed = runBlocking {
            val snap = app.container.preferences.readNotificationSnapshot()
            when (type) {
                "order_status" -> snap.notifyOrderStatus
                "support_reply" -> snap.notifySupportReply
                "support_queue" -> snap.notifySupportQueue
                "review_moderation" -> snap.notifyReviewModeration
                else -> true
            }
        }
        if (!allowed) return

        val title = message.notification?.title
            ?: message.data["title"]
            ?: getString(R.string.app_name)
        val body = message.notification?.body
            ?: message.data["body"]
            ?: return

        showNotification(title, body, type, message.data["orderId"], message.data["questionId"], message.data["reviewId"])
    }

    private fun showNotification(
        title: String,
        body: String,
        type: String,
        orderId: String?,
        questionId: String?,
        reviewId: String?,
    ) {
        val channelId = PushNotificationChannels.DEFAULT
        val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val ch = NotificationChannel(
                channelId,
                getString(R.string.push_channel_name),
                NotificationManager.IMPORTANCE_DEFAULT,
            )
            ch.description = getString(R.string.push_channel_description)
            nm.createNotificationChannel(ch)
        }

        val launch = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_FROM_PUSH_TYPE, type)
            orderId?.let { putExtra(EXTRA_ORDER_ID, it) }
            questionId?.let { putExtra(EXTRA_QUESTION_ID, it) }
            reviewId?.let { putExtra(EXTRA_REVIEW_ID, it) }
        }
        val pending = PendingIntent.getActivity(
            this,
            (type + (orderId ?: questionId ?: reviewId ?: "0")).hashCode(),
            launch,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val notification = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pending)
            .setAutoCancel(true)
            .build()

        nm.notify((type + body + System.currentTimeMillis().toString()).hashCode(), notification)
    }

    companion object {
        const val EXTRA_FROM_PUSH_TYPE = "from_push_type"
        const val EXTRA_ORDER_ID = "push_order_id"
        const val EXTRA_QUESTION_ID = "push_question_id"
        const val EXTRA_REVIEW_ID = "push_review_id"
    }
}

object PushNotificationChannels {
    const val DEFAULT = "electronix_push"
}
