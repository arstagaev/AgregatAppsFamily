package com.tagaev.trrcrm.push

import platform.darwin.dispatch_async
import platform.darwin.dispatch_get_main_queue
import platform.UIKit.UIApplication
import platform.UserNotifications.UNUserNotificationCenter

actual fun disablePushDeliveryForLoggedOutUser() {
    dispatch_async(dispatch_get_main_queue()) {
        val app = UIApplication.sharedApplication
        app.applicationIconBadgeNumber = 0
        UNUserNotificationCenter.currentNotificationCenter().removeAllDeliveredNotifications()
        UNUserNotificationCenter.currentNotificationCenter().removeAllPendingNotificationRequests()
    }
}
