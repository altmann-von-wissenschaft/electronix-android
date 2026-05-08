package com.pnzgu.electronix

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import coil3.ImageLoader
import coil3.SingletonImageLoader
import com.pnzgu.electronix.push.PushNotificationChannels
import com.pnzgu.electronix.R

class ElectronixApplication : Application(), SingletonImageLoader.Factory {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        ensurePushChannel()
    }

    private fun ensurePushChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val nm = getSystemService(NotificationManager::class.java)
        val ch = NotificationChannel(
            PushNotificationChannels.DEFAULT,
            getString(R.string.push_channel_name),
            NotificationManager.IMPORTANCE_DEFAULT,
        )
        ch.description = getString(R.string.push_channel_description)
        nm.createNotificationChannel(ch)
    }

    override fun newImageLoader(context: Context): ImageLoader = container.imageLoader
}
