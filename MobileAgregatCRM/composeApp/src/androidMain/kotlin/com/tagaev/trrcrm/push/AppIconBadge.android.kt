package com.tagaev.trrcrm.push

actual fun applyAppIconBadgeCount(unreadCount: Int) {
    // Android launcher badges are OEM/launcher-specific and typically driven by posted notifications.
    // Keep this as a safe no-op to avoid unreliable side effects.
}
