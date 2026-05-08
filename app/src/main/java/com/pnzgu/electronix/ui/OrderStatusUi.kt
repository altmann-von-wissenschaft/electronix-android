package com.pnzgu.electronix.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.pnzgu.electronix.R

@Composable
fun orderStatusLabel(status: String): String =
    when (status.trim()) {
        "Pending" -> stringResource(R.string.order_status_pending)
        "Processing" -> stringResource(R.string.order_status_processing)
        "ReadyForPickup" -> stringResource(R.string.order_status_ready)
        "Completed" -> stringResource(R.string.order_status_completed)
        "Cancelled" -> stringResource(R.string.order_status_cancelled)
        else -> status
    }
