package com.tagaev.trrcrm.push

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

object NotificationsUnreadState {
    private val _count = MutableStateFlow(0)
    val count: StateFlow<Int> = _count

    fun setCount(raw: Int) {
        val normalized = raw.coerceAtLeast(0)
        if (_count.value == normalized) return
        _count.value = normalized
        applyAppIconBadgeCount(normalized)
    }
}
