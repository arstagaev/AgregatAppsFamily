package com.tagaev.trrcrm

import android.app.Application
import com.google.firebase.FirebaseApp
import com.google.firebase.installations.FirebaseInstallations
import com.google.firebase.messaging.FirebaseMessaging
import com.tagaev.trrcrm.di.androidModule
import com.tagaev.trrcrm.di.commonModule
import com.tagaev.trrcrm.push.NotificationHelper
import com.tagaev.trrcrm.push.PushRegistrationCoordinator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.GlobalContext.startKoin

class App : Application() {
    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var proactiveFetchAttempts = 0
    private var fisResetAttempted = false

    private val retryDelaysMs = longArrayOf(
        5_000L,
        15_000L,
        30_000L,
        60_000L,
        120_000L
    )

    override fun onCreate() {
        super.onCreate()
        val firebaseApp = FirebaseApp.initializeApp(this)
        if (firebaseApp == null) {
            println("PUSH_SERVICE: FCM(Android) FirebaseApp initialization failed, skip token fetch")
        } else {
            println(
                "PUSH_SERVICE: FCM(Android) Firebase initialized " +
                    "projectId=${firebaseApp.options.projectId}, appId=${firebaseApp.options.applicationId}"
            )
        }
        startKoin {
            androidContext(this@App)
            modules(commonModule, androidModule)
        }
        fetchFcmTokenProactively(reason = "app_start")
//        NotificationHelper.ensureNotificationChannel(this)
    }

    private fun fetchFcmTokenProactively(reason: String) {
        proactiveFetchAttempts += 1
        val attempt = proactiveFetchAttempts
        FirebaseMessaging.getInstance().token
            .addOnCompleteListener { task ->
                if (!task.isSuccessful) {
                    val error = task.exception
                    val message = error?.message.orEmpty()
                    println(
                        "PUSH_SERVICE: FCM(Android) proactive token fetch failed " +
                            "attempt=$attempt reason=$reason error=${error?.javaClass?.simpleName}:$message"
                    )

                    if (!fisResetAttempted && isHardFisAuthError(error, message)) {
                        fisResetAttempted = true
                        println("PUSH_SERVICE: FCM(Android) hard FIS auth error detected, deleting FIS installation and retrying")
                        FirebaseInstallations.getInstance().delete()
                            .addOnCompleteListener { deleteTask ->
                                if (!deleteTask.isSuccessful) {
                                    println(
                                        "PUSH_SERVICE: FCM(Android) FIS installation delete failed: " +
                                            "${deleteTask.exception?.message}"
                                    )
                                } else {
                                    println("PUSH_SERVICE: FCM(Android) FIS installation deleted")
                                }
                                scheduleProactiveRetry("after_fis_delete")
                            }
                        return@addOnCompleteListener
                    }

                    scheduleProactiveRetry(reason = "fetch_failed")
                    return@addOnCompleteListener
                }

                val token = task.result.orEmpty()
                if (token.isBlank()) {
                    println("PUSH_SERVICE: FCM(Android) proactive token fetch returned blank token attempt=$attempt")
                    scheduleProactiveRetry(reason = "blank_token")
                    return@addOnCompleteListener
                }

                println("PUSH_SERVICE: FCM(Android) proactive token fetch success attempt=$attempt")
                PushRegistrationCoordinator.onTokenReceived(
                    token = token,
                    preferredPlatform = "android"
                )
            }
    }

    private fun scheduleProactiveRetry(reason: String) {
        val index = proactiveFetchAttempts - 1
        if (index >= retryDelaysMs.size) {
            println("PUSH_SERVICE: FCM(Android) proactive token fetch retries exhausted; waiting for Firebase onNewToken callback")
            return
        }
        val delayMs = retryDelaysMs[index]
        println("PUSH_SERVICE: FCM(Android) scheduling token fetch retry in ${delayMs}ms reason=$reason")
        appScope.launch {
            delay(delayMs)
            fetchFcmTokenProactively(reason = "retry_$reason")
        }
    }

    private fun isHardFisAuthError(error: Throwable?, message: String): Boolean {
        val lowerMessage = message.lowercase()
        if (lowerMessage.contains("fis_auth_error")) return true
        if (error?.cause?.message?.contains("FIS_AUTH_ERROR", ignoreCase = true) == true) return true
        return false
    }
}
