package com.tagaev.trrcrm.push

import com.tagaev.trrcrm.data.AppSettings
import com.tagaev.trrcrm.pushPlatformId
import com.tagaev.trrcrm.data.AppSettingsKeys
import com.tagaev.trrcrm.getPlatform
import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.coroutines.*
import kotlinx.serialization.Serializable
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

@Serializable
data class RegisterDeviceLegacyRequest(
    val full_name: String,
    val platform: String,   // "android" or "ios"
    val fcm_token: String
)

@Serializable
data class RegisterDeviceCoreRequest(
    val full_name: String,
    val platform: String,
    val device_id: String,
    val fcm_token: String,
    val device_name: String? = null,
    val app_version: String? = null,
)

@kotlinx.serialization.Serializable
data class LogoutDeviceRequest(
    val full_name: String,
    val platform: String? = null,
    val fcm_token: String? = null,
)


object PushRegistration {

    private lateinit var client: HttpClient
    private lateinit var baseUrl: String
    private lateinit var apiKey: String

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    fun configure(
        client: HttpClient,
        baseUrl: String,
        apiKey: String,
    ) {
        this.client = client
        this.baseUrl = baseUrl.trimEnd('/')
        this.apiKey = apiKey
    }

    fun registerCurrentUserToken(
        fullName: String,
        platform: String,
        token: String,
    ) {
        if (!::client.isInitialized) {
            println("PushRegistration: client not configured, skipping register")
            return
        }
        if (fullName.isBlank() || token.isBlank()) return

        scope.launch {
            try {
                println("PushRegistration: register_start(platform=$platform, token_len=${token.length})")
                val response: HttpResponse = client.post("$baseUrl/devices/register") {
                    contentType(ContentType.Application.Json)
                    header("X-API-Key", apiKey)
                    setBody(
                        RegisterDeviceCoreRequest(
                            full_name = fullName,
                            platform = platform,
                            device_id = getPlatform().deviceSpecificInfo,
                            fcm_token = token,
                            device_name = getPlatform().name,
                        )
                    )
                }
                println("PushRegistration: register_core_status(code=${response.status.value})")
            } catch (e: Exception) {
                println("PushRegistration: core register failed, fallback legacy: $e")
                runCatching {
                    val fallbackResponse: HttpResponse = client.post("$baseUrl/users/register-device") {
                        contentType(ContentType.Application.Json)
                        header("X-API-Key", apiKey)
                        setBody(
                            RegisterDeviceLegacyRequest(
                                full_name = fullName,
                                platform = platform,
                                fcm_token = token,
                            )
                        )
                    }
                    println("PushRegistration: register_legacy_status(code=${fallbackResponse.status.value})")
                }.onFailure { fallbackError ->
                    println("PushRegistration: legacy register failed: $fallbackError")
                }
            }
        }
    }

    fun logoutCurrentDevice(
        fullName: String,
        platform: String,
        fcmToken: String? = null,
    ) {
        if (!::client.isInitialized) {
            println("PushRegistration: client not configured, skipping logout")
            return
        }
        if (fullName.isBlank()) return

        scope.launch {
            try {
                client.post("$baseUrl/users/logout-device") {
                    contentType(ContentType.Application.Json)
                    header("X-API-Key", apiKey)
                    setBody(
                        LogoutDeviceRequest(
                            full_name = fullName,
                            platform = platform,
                            fcm_token = fcmToken
                        )
                    )
                }
            } catch (e: Exception) {
                println("PushRegistration: logout failed: $e")
            }
        }
    }
}

object PushRegistrationCoordinator : KoinComponent {
    private val appSettings: AppSettings by inject()

    fun registerIfReady(preferredPlatform: String? = null) {
        val platform = preferredPlatform ?: pushPlatformId()
        if (platform == "ios") {
            val apnsReady = appSettings.getBool(AppSettingsKeys.IOS_APNS_READY, false)
            if (!apnsReady) {
                println("PushRegistrationCoordinator: register_skipped(missing_apns_ready)")
                return
            }
        }

        val token = appSettings.getStringOrNull(AppSettingsKeys.FCM_TOKEN).orEmpty()
        val fullName = appSettings.getStringOrNull(AppSettingsKeys.PERSONAL_DATA).orEmpty()

        if (token.isBlank() || fullName.isBlank()) {
            val reason = when {
                token.isBlank() && fullName.isBlank() -> "missing_token_and_user"
                token.isBlank() -> "missing_token"
                else -> "missing_user"
            }
            println("PushRegistrationCoordinator: register_skipped($reason)")
            return
        }

        println("PushRegistrationCoordinator: register_attempt(platform=$platform, token_len=${token.length}, user_len=${fullName.length})")
        PushRegistration.registerCurrentUserToken(fullName = fullName, platform = platform, token = token)
    }

    fun onTokenReceived(token: String, preferredPlatform: String? = null) {
        if (token.isBlank()) {
            println("PushRegistrationCoordinator: token_received(blank)")
            return
        }
        val platform = preferredPlatform ?: pushPlatformId()
        println("PushRegistrationCoordinator: token_received(platform=$platform, token_len=${token.length})")
        appSettings.setString(AppSettingsKeys.FCM_TOKEN, token)
        println("PushRegistrationCoordinator: token_saved")
        registerIfReady(preferredPlatform = preferredPlatform)
    }

}
