package com.pnzgu.electronix.data.local

data class NotificationSnapshot(
    val notifyOrderStatus: Boolean = true,
    val notifySupportReply: Boolean = true,
    val notifyReviewModeration: Boolean = true,
    val notifySupportQueue: Boolean = true,
    val lastSeenOrdersMs: Long = 0L,
    val lastSeenSupportMyMs: Long = 0L,
    val lastSeenSupportQueueMs: Long = 0L,
    val lastSeenModReviewsMs: Long = 0L,
)
