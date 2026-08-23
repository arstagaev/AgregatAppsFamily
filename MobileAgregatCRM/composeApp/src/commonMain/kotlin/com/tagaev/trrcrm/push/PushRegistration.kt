package com.tagaev.trrcrm.push

import com.tagaev.trrcrm.data.AppSettings
import com.tagaev.trrcrm.pushPlatformId
import com.tagaev.trrcrm.data.AppSettingsKeys
import com.tagaev.trrcrm.getPlatform
import com.tagaev.trrcrm.utils.DeviceIdentity
import com.tagaev.trrcrm.data.MainRepository
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
        onComplete: ((success: Boolean) -> Unit)? = null,
    ): Job {
        if (!::client.isInitialized) {
            println("PUSH_SERVICE: PushRegistration client not configured, skipping register")
            onComplete?.invoke(false)
            return scope.launch { }
        }
        if (fullName.isBlank() || token.isBlank()) {
            onComplete?.invoke(false)
            return scope.launch { }
        }

        return scope.launch {
            var success = false
            try {
                println("PUSH_SERVICE: PushRegistration register_start(platform=$platform, token_len=${token.length})")
                val response: HttpResponse = client.post("$baseUrl/devices/register") {
                    contentType(ContentType.Application.Json)
                    header("X-API-Key", apiKey)
                    setBody(
                        RegisterDeviceCoreRequest(
                            full_name = fullName,
                            platform = platform,
                            device_id = DeviceIdentity.stableDeviceId(),
                            fcm_token = token,
                            device_name = getPlatform().name,
                        )
                    )
                }
                println("PUSH_SERVICE: PushRegistration register_core_status(code=${response.status.value})")
                success = response.status.isSuccess()
            } catch (e: Exception) {
                println("PUSH_SERVICE: PushRegistration core register failed, fallback legacy: $e")
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
                    println("PUSH_SERVICE: PushRegistration register_legacy_status(code=${fallbackResponse.status.value})")
                    success = fallbackResponse.status.isSuccess()
                }.onFailure { fallbackError ->
                    println("PUSH_SERVICE: PushRegistration legacy register failed: $fallbackError")
                }
            } finally {
                onComplete?.invoke(success)
            }
        }
    }

    fun logoutCurrentDevice(
        fullName: String,
        platform: String,
        fcmToken: String? = null,
    ) {
        if (!::client.isInitialized) {
            println("PUSH_SERVICE: PushRegistration client not configured, skipping logout")
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
                println("PUSH_SERVICE: PushRegistration logout failed: $e")
            }
        }
    }
}

object PushRegistrationCoordinator : KoinComponent {
    private val appSettings: AppSettings by inject()
    private val appScope: CoroutineScope by inject()
    private val repository: MainRepository by inject()
    private val coreSession: CoreSessionCoordinator by inject()

    fun registerIfReady(@Suppress("UNUSED_PARAMETER") preferredPlatform: String? = null) {
        appScope.launch {
            repository.refreshPushFeatureToggleIfNeeded(force = false)
        }
        println("PUSH_SERVICE: PushRegistrationCoordinator register_skipped(bootstrap_owns_active_user)")
    }

    fun onTokenReceived(token: String, preferredPlatform: String? = null) {
        if (token.isBlank()) {
            println("PUSH_SERVICE: PushRegistrationCoordinator token_received(blank)")
            return
        }
        val platform = preferredPlatform ?: pushPlatformId()
        println("PUSH_SERVICE: PushRegistrationCoordinator token_received(platform=$platform, token_len=${token.length})")
        val existingToken = appSettings.getStringOrNull(AppSettingsKeys.FCM_TOKEN).orEmpty().trim()
        if (existingToken == token) {
            println("PUSH_SERVICE: PushRegistrationCoordinator token_unchanged")
        } else {
            appSettings.setString(AppSettingsKeys.FCM_TOKEN, token)
            val changeType = if (existingToken.isBlank()) "initial" else "rotated"
            println("PUSH_SERVICE: PushRegistrationCoordinator token_saved(change=$changeType)")
        }
        appScope.launch {
            coreSession.onFcmToken(token)
        }
    }

    suspend fun recoverCoreSessionNow(reason: String, forceRebootstrap: Boolean = false): Boolean {
        println("CoreSession: recover delegate reason=$reason force=$forceRebootstrap")
        return coreSession.recover(reason = reason, forceRebootstrap = forceRebootstrap)
    }
}
