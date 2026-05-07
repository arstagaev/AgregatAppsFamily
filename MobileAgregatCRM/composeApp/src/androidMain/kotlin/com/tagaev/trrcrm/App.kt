package com.tagaev.trrcrm

import android.app.Application
import com.google.firebase.FirebaseApp
import com.google.firebase.messaging.FirebaseMessaging
import com.tagaev.trrcrm.di.androidModule
import com.tagaev.trrcrm.di.commonModule
import com.tagaev.trrcrm.push.NotificationHelper
import com.tagaev.trrcrm.push.PushRegistrationCoordinator
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.GlobalContext.startKoin

class App : Application() {
    override fun onCreate() {
        super.onCreate()
        FirebaseApp.initializeApp(this)
        startKoin {
            androidContext(this@App)
            modules(commonModule, androidModule)
        }
        FirebaseMessaging.getInstance().token
            .addOnCompleteListener { task ->
                if (!task.isSuccessful) {
                    println("PUSH_SERVICE: FCM(Android) proactive token fetch failed: ${task.exception?.message}")
                    return@addOnCompleteListener
                }
                val token = task.result.orEmpty()
                if (token.isBlank()) {
                    println("PUSH_SERVICE: FCM(Android) proactive token fetch returned blank token")
                    return@addOnCompleteListener
                }
                println("PUSH_SERVICE: FCM(Android) proactive token fetch success")
                PushRegistrationCoordinator.onTokenReceived(
                    token = token,
                    preferredPlatform = "android"
                )
            }
//        NotificationHelper.ensureNotificationChannel(this)
    }
}
