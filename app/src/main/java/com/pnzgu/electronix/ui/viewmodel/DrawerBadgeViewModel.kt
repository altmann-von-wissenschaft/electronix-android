package com.pnzgu.electronix.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.pnzgu.electronix.AppContainer
import com.pnzgu.electronix.data.dto.UserDto
import com.pnzgu.electronix.domain.isManager
import com.pnzgu.electronix.domain.isModerator
import com.pnzgu.electronix.util.parseApiDateTimeToMillis
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class DrawerBadges(
    val orders: Int = 0,
    val supportMy: Int = 0,
    val supportQueue: Int = 0,
    val modReviews: Int = 0,
)

class DrawerBadgeViewModel(
    private val container: AppContainer,
) : ViewModel() {
    private val _badges = MutableStateFlow(DrawerBadges())
    val badges: StateFlow<DrawerBadges> = _badges.asStateFlow()

    fun refresh(user: UserDto?) {
        viewModelScope.launch {
            if (user == null) {
                _badges.value = DrawerBadges()
                return@launch
            }
            val snap = container.preferences.readNotificationSnapshot()
            coroutineScope {
                val ordersDef = async {
                    if (!snap.notifyOrderStatus) null
                    else runCatching { container.api.myOrders() }.getOrNull()
                }
                val myQDef = async {
                    if (!snap.notifySupportReply) null
                    else runCatching { container.api.myQuestions(1, 50) }.getOrNull()
                }
                val queueDef = async {
                    if (!snap.notifySupportQueue || user.isManager() != true) {
                        null
                    } else {
                        runCatching { container.api.unansweredQuestions(1, 50) }.getOrNull()
                    }
                }
                val pendingDef = async {
                    if (!snap.notifyReviewModeration || user.isModerator() != true) {
                        null
                    } else {
                        runCatching { container.api.reviewsPending(1, 50) }.getOrNull()
                    }
                }

                val orders = ordersDef.await()
                val myQ = myQDef.await()
                val queue = queueDef.await()
                val pending = pendingDef.await()

                val thresholdOrders = snap.lastSeenOrdersMs
                val ordersBadge = orders?.count { parseApiDateTimeToMillis(it.updatedAt) > thresholdOrders } ?: 0

                val thresholdMy = snap.lastSeenSupportMyMs
                val myBadge = myQ?.data?.count { q ->
                    val ans = q.answer ?: return@count false
                    parseApiDateTimeToMillis(ans.createdAt) > thresholdMy
                } ?: 0

                val thresholdQueue = snap.lastSeenSupportQueueMs
                val queueBadge = queue?.data?.count { q ->
                    !q.isAnswered && parseApiDateTimeToMillis(q.createdAt) > thresholdQueue
                } ?: 0

                val thresholdMod = snap.lastSeenModReviewsMs
                val modBadge = pending?.data?.count { r ->
                    parseApiDateTimeToMillis(r.createdAt) > thresholdMod
                } ?: 0

                _badges.value = DrawerBadges(
                    orders = ordersBadge,
                    supportMy = myBadge,
                    supportQueue = queueBadge,
                    modReviews = modBadge,
                )
            }
        }
    }

    companion object {
        fun factory(container: AppContainer): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    DrawerBadgeViewModel(container) as T
            }
    }
}
