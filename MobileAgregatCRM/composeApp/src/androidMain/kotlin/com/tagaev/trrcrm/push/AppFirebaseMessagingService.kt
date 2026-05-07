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

        val data = remoteMessage.data // contains docId, authorName, etc.
        val title = remoteMessage.notification?.title
            ?: remoteMessage.data["docTitle"]
            ?: remoteMessage.data["title"]
            ?: "New message"

        val body = remoteMessage.notification?.body
            ?: remoteMessage.data["message_text"]
            ?: remoteMessage.data["messageText"]
            ?: remoteMessage.data["body"]
            ?: ""

        println(
            "PUSH_SERVICE: Android onMessageReceived " +
                    "hasNotification=${remoteMessage.notification != null}, " +
                    "dataKeys=${data.keys.sorted()}, " +
                    "title='$title', bodyLen=${body.length}"
        )

        NotificationHelper.showNotification(
            context = applicationContext,
            title = title,
            body = body,
            data = data
        )
    }
}

