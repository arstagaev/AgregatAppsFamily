package com.tagaev.trrcrm.push

import com.tagaev.trrcrm.data.AppSettings
import com.tagaev.trrcrm.data.AppSettingsKeys
import com.tagaev.trrcrm.di.initKoinIos
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

private object IosPushBridgeRetry {
    val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
}

private object IosPushBridgeSettings : KoinComponent {
    val appSettings: AppSettings by inject()
}

fun ensureIosDependenciesReady() {
    initKoinIos()
}

fun setIosApnsReady(ready: Boolean) {
    ensureIosDependenciesReady()
    runCatching {
        IosPushBridgeSettings.appSettings.setBool(AppSettingsKeys.IOS_APNS_READY, ready)
        println("PushBridge(iOS): apns_ready_set($ready)")
    }.onFailure {
        println("PushBridge(iOS): failed to set apns_ready($ready): ${it.message}")
    }
}

fun onIosFcmTokenReceived(token: String?) {
    val safeToken = token?.trim().orEmpty()
    if (safeToken.isBlank()) {
        println("PushBridge(iOS): token missing")
        return
    }

    ensureIosDependenciesReady()
    forwardTokenWithRetry(token = safeToken, attempt = 1)
}

private fun forwardTokenWithRetry(token: String, attempt: Int) {
    try {
        PushRegistrationCoordinator.onTokenReceived(
            token = token,
            preferredPlatform = "ios"
        )
        println("PushBridge(iOS): token forwarded successfully (attempt=$attempt)")
    } catch (t: Throwable) {
        val maxAttempts = 3
        if (attempt >= maxAttempts) {
            println("PushBridge(iOS): token forwarding failed after $attempt attempts: ${t.message}")
            return
        }

        val retryDelayMs = 500L * attempt
        println("PushBridge(iOS): forwarding failed (attempt=$attempt), retry in ${retryDelayMs}ms: ${t.message}")
        IosPushBridgeRetry.scope.launch {
            delay(retryDelayMs)
            ensureIosDependenciesReady()
            forwardTokenWithRetry(token = token, attempt = attempt + 1)
        }
    }
}
