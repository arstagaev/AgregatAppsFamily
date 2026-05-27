package com.tagaev.trrcrm.push

import platform.darwin.dispatch_async
import platform.darwin.dispatch_get_main_queue
import platform.UIKit.UIApplication

actual fun applyAppIconBadgeCount(unreadCount: Int) {
    dispatch_async(dispatch_get_main_queue()) {
        UIApplication.sharedApplication.applicationIconBadgeNumber = unreadCount.toLong()
    }
}
