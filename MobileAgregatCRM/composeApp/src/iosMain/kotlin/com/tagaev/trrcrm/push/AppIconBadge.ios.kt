package com.tagaev.trrcrm.push

import platform.UIKit.UIApplication

actual fun applyAppIconBadgeCount(unreadCount: Int) {
    UIApplication.sharedApplication.applicationIconBadgeNumber = unreadCount.toLong()
}
