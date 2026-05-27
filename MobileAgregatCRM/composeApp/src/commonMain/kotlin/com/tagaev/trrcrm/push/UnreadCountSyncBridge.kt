package com.tagaev.trrcrm.push

fun onAppForegroundForUnreadCount() {
    runCatching {
        UnreadCountSync.refreshAsync(reason = "app_foreground", force = false)
    }.onFailure {
        println("PUSH_SERVICE: unread_count foreground trigger failed: ${it.message}")
    }
}
