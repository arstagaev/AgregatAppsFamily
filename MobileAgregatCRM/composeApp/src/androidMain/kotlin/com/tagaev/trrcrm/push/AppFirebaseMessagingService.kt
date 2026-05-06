package com.tagaev.trrcrm.push

import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.tagaev.trrcrm.push.PushRegistrationCoordinator

class AppFirebaseMessagingService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        PushRegistrationCoordinator.onTokenReceived(token, preferredPlatform = "android")
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)

        val title = remoteMessage.notification?.title
            ?: remoteMessage.data["docTitle"]
            ?: "New message"

        val body = remoteMessage.notification?.body
            ?: remoteMessage.data["messageText"]
            ?: ""

        val data = remoteMessage.data // contains docId, authorName, etc.

        NotificationHelper.showNotification(
            context = applicationContext,
            title = title,
            body = body,
            data = data
        )
    }
}

