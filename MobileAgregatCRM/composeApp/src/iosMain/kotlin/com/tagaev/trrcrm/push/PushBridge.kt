package com.tagaev.trrcrm.push

fun onIosFcmTokenReceived(token: String?) {
    val safeToken = token?.trim().orEmpty()
    if (safeToken.isBlank()) {
        println("PushBridge(iOS): token missing")
        return
    }
    PushRegistrationCoordinator.onTokenReceived(
        token = safeToken,
        preferredPlatform = "ios"
    )
}

