package com.pnzgu.electronix.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import kotlinx.coroutines.delay

/**
 * Refreshes management data when the screen is resumed and periodically while [lifecycleOwner] is at least
 * `Lifecycle.State.STARTED`. When the app or this owner is in the background (state below that), the periodic callback
 * is skipped so we do not hammer the API for energy. User-visible alerts for orders/support/reviews use FCM when the app
 * is backgrounded; drawer badges here are a lightweight in-app hint while a staff screen is open.
 */
@Composable
fun ManagementForegroundSync(
    lifecycleOwner: LifecycleOwner,
    intervalMs: Long = 25_000L,
    onSync: () -> Unit,
) {
    DisposableEffect(lifecycleOwner, onSync) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) onSync()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    LaunchedEffect(lifecycleOwner, intervalMs, onSync) {
        while (true) {
            delay(intervalMs)
            if (lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) {
                onSync()
            }
        }
    }
}
