package com.tagaev.trrcrm.push

import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.tagaev.trrcrm.data.AppSettings
import com.tagaev.trrcrm.data.AppSettingsKeys
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import kotlin.getValue

class AppFirebaseMessagingService : FirebaseMessagingService(), KoinComponent {
    private val appSettings: AppSettings by inject()

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        println("FCM token (Android): $token")

        val fullName = appSettings.getStringOrNull(AppSettingsKeys.PERSONAL_DATA) // adapt to your storage
        val lastSavedFCMToken = appSettings.getStringOrNull(AppSettingsKeys.FCM_TOKEN) // adapt to your storage

        if (lastSavedFCMToken == token) {
            println("FCM: token unchanged, skip register")
            return
        }

        // save token locally
        appSettings.setString(AppSettingsKeys.FCM_TOKEN, token)

        if (fullName.isNullOrBlank() || token.isBlank()) {
            repeat(5) {
                println("FCM: no user fullName yet, skip register")
            }
            return
        }

        PushRegistration.registerCurrentUserToken(
            fullName = fullName,
            platform = "android",
            token = token
        )
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

