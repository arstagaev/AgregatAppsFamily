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

private var lastTapSignature: String? = null

fun ensureIosDependenciesReady() {
    initKoinIos()
}

fun setIosApnsReady(ready: Boolean) {
    ensureIosDependenciesReady()
    runCatching {
        IosPushBridgeSettings.appSettings.setBool(AppSettingsKeys.IOS_APNS_READY, ready)
        println("PUSH_SERVICE: PushBridge(iOS) apns_ready_set($ready)")
    }.onFailure {
        println("PUSH_SERVICE: PushBridge(iOS) failed to set apns_ready($ready): ${it.message}")
    }
}

fun onIosFcmTokenReceived(token: String?) {
    val safeToken = token?.trim().orEmpty()
    if (safeToken.isBlank()) {
        println("PUSH_SERVICE: PushBridge(iOS) token missing")
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
        println("PUSH_SERVICE: PushBridge(iOS) token forwarded successfully (attempt=$attempt)")
    } catch (t: Throwable) {
        val maxAttempts = 3
        if (attempt >= maxAttempts) {
            println("PUSH_SERVICE: PushBridge(iOS) token forwarding failed after $attempt attempts: ${t.message}")
            return
        }

        val retryDelayMs = 500L * attempt
        println("PUSH_SERVICE: PushBridge(iOS) forwarding failed (attempt=$attempt), retry in ${retryDelayMs}ms: ${t.message}")
        IosPushBridgeRetry.scope.launch {
            delay(retryDelayMs)
            ensureIosDependenciesReady()
            forwardTokenWithRetry(token = token, attempt = attempt + 1)
        }
    }
}

fun onIosNotificationTap(title: String?, screen: String?, docId: String?, messageText: String?) {
    val normalizedScreen = screen?.trim()?.takeIf { it.isNotBlank() }
    val normalizedDocId = docId?.trim()?.takeIf { it.isNotBlank() }
    val normalizedMessage = messageText?.trim().orEmpty()
    val signature = listOf(normalizedScreen.orEmpty(), normalizedDocId.orEmpty(), title.orEmpty(), normalizedMessage).joinToString("|")
    if (signature == lastTapSignature) {
        println("PUSH_SERVICE DEEPLINK: PushBridge(iOS) duplicate tap signature, skip")
        return
    }
    lastTapSignature = signature

    if (normalizedScreen == null && normalizedDocId == null) {
        println("PUSH_SERVICE DEEPLINK: PushBridge(iOS) missing_canonical_push_payload, skip deeplink routing")
        return
    }
    val resolvedScreen = normalizedScreen ?: inferScreenFromTitle(title) ?: "events"
    println(
        "PUSH_SERVICE DEEPLINK: PushBridge(iOS) tap resolved " +
                "screen='$resolvedScreen', docId='${normalizedDocId ?: ""}', title='${title.orEmpty()}'"
    )
    DeepLinkBridge.handle(screen = resolvedScreen, docId = normalizedDocId, messageText = messageText, title = title)
}

private fun inferScreenFromTitle(title: String?): String? {
    val normalized = title?.lowercase() ?: return null
    return when {
        normalized.contains("комплект") -> "complectation"
        normalized.contains("заказ-нар") || normalized.contains("заказнар") -> "work_orders"
        normalized.contains("событ") -> "events"
        else -> null
    }
}
