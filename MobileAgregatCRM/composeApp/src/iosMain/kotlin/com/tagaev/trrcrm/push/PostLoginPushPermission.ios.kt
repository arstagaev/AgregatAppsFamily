package com.tagaev.trrcrm.push

import platform.Foundation.NSNotificationCenter

private const val POST_LOGIN_PUSH_PERMISSION_CHECK_NOTIFICATION = "TRRCRM_POST_LOGIN_PUSH_PERMISSION_CHECK"

actual fun triggerPostLoginPushPermissionCheck() {
    NSNotificationCenter.defaultCenter.postNotificationName(
        POST_LOGIN_PUSH_PERMISSION_CHECK_NOTIFICATION,
        null
    )
    println("PUSH_SERVICE: PushBridge(iOS) post-login permission check requested")
}
