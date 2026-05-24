package com.tagaev.trrcrm.push

fun onAppForegroundForUnreadCount() {
    UnreadCountSync.refreshAsync(reason = "app_foreground", force = false)
}
