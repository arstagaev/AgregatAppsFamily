package com.tagaev.trrcrm.push

import com.tagaev.secrets.Secrets
import com.tagaev.trrcrm.data.AppSettings
import com.tagaev.trrcrm.pushPlatformId
import com.tagaev.trrcrm.data.AppSettingsKeys
import com.tagaev.trrcrm.getPlatform
import com.tagaev.trrcrm.utils.DeviceIdentity
import com.tagaev.trrcrm.data.MainRepository
import com.tagaev.trrcrm.data.remote.Resource
import com.tagaev.trrcrm.models.CoreSessionBootstrapRequest
import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.coroutines.*
import kotlinx.serialization.Serializable
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
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
    private var registerInFlight = false
    private var bootstrapRetryInFlight = false
    private val sessionRecoveryMutex = Mutex()

    fun registerIfReady(preferredPlatform: String? = null) {
        appScope.launch {
            repository.refreshPushFeatureToggleIfNeeded(force = false)
        }

        val pushEnabled = appSettings.getBool(AppSettingsKeys.PUSH_FEATURE_TOGGLE_ENABLED, true)
        if (!pushEnabled) {
            println("PUSH_SERVICE: PushRegistrationCoordinator register_skipped(push_disabled)")
            return
        }

        val platform = preferredPlatform ?: pushPlatformId()
        if (platform == "ios") {
            val apnsReady = appSettings.getBool(AppSettingsKeys.IOS_APNS_READY, false)
            if (!apnsReady) {
                println("PUSH_SERVICE: PushRegistrationCoordinator register_skipped(missing_apns_ready)")
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
            println("PUSH_SERVICE: PushRegistrationCoordinator register_skipped($reason)")
            return
        }

        val fingerprint = "$platform|$fullName|$token|${DeviceIdentity.stableDeviceId()}"
        val lastFingerprint = appSettings.getStringOrNull(AppSettingsKeys.PUSH_REGISTER_LAST_FINGERPRINT).orEmpty()
        if (lastFingerprint == fingerprint) {
            println("PUSH_SERVICE: PushRegistrationCoordinator register_skipped(already_registered_fingerprint)")
            return
        }
        if (lastFingerprint.isNotBlank() && lastFingerprint != fingerprint) {
            println("PUSH_SERVICE: PushRegistrationCoordinator register_rebind(user_or_token_changed)")
        }
        if (registerInFlight) {
            println("PUSH_SERVICE: PushRegistrationCoordinator register_skipped(in_flight)")
            return
        }

        println("PUSH_SERVICE: PushRegistrationCoordinator register_attempt(platform=$platform, token_len=${token.length}, user_len=${fullName.length})")
        registerInFlight = true
        PushRegistration.registerCurrentUserToken(
            fullName = fullName,
            platform = platform,
            token = token
        ) { success ->
            registerInFlight = false
            if (success) {
                appSettings.setString(AppSettingsKeys.PUSH_REGISTER_LAST_FINGERPRINT, fingerprint)
            }
        }
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
        registerIfReady(preferredPlatform = preferredPlatform)
        retryCoreBootstrapAfterTokenIfNeeded(platform = platform, token = token)
    }

    private fun retryCoreBootstrapAfterTokenIfNeeded(platform: String, token: String) {
        val pendingRetry = appSettings.getBool(AppSettingsKeys.CORE_BOOTSTRAP_RETRY_ON_TOKEN, false)
        if (!pendingRetry) return

        val existingSessionId = appSettings.getStringOrNull(AppSettingsKeys.CORE_SESSION_ID).orEmpty().trim()
        if (existingSessionId.isNotBlank()) {
            appSettings.setBool(AppSettingsKeys.CORE_BOOTSTRAP_RETRY_ON_TOKEN, false)
            println("CoreSession: pending token bootstrap retry cleared (session already exists)")
            return
        }

        val fullName = appSettings.getStringOrNull(AppSettingsKeys.PERSONAL_DATA).orEmpty().trim()
        if (fullName.isBlank()) {
            println("CoreSession: pending token bootstrap retry skipped (missing_user)")
            return
        }

        if (bootstrapRetryInFlight) {
            println("CoreSession: pending token bootstrap retry skipped (in_flight)")
            return
        }

        bootstrapRetryInFlight = true
        println("CoreSession: pending token bootstrap retry attempt")
        appScope.launch {
            try {
                val request = CoreSessionBootstrapRequest(
                    full_name = fullName,
                    platform = platform,
                    device_id = DeviceIdentity.stableDeviceId(),
                    fcm_token = token,
                    login = appSettings.getStringOrNull(AppSettingsKeys.EMAIL),
                    department = appSettings.getStringOrNull(AppSettingsKeys.DEPARTMENT),
                    device_name = getPlatform().name,
                    app_version = Secrets.VERSION,
                )
                when (val res = repository.coreSessionBootstrap(request)) {
                    is Resource.Success -> {
                        appSettings.setString(AppSettingsKeys.CORE_SESSION_ID, res.data.sessionId)
                        appSettings.setBool(AppSettingsKeys.CORE_BOOTSTRAP_RETRY_ON_TOKEN, false)
                        println("CoreSession: pending token bootstrap retry success")
                        UnreadCountSync.refreshAsync(reason = "bootstrap_retry_success", force = true)
                    }
                    is Resource.Error -> {
                        println("CoreSession: pending token bootstrap retry failed ${res.causes ?: res.exception?.message}")
                    }
                    is Resource.Loading -> Unit
                }
            } finally {
                bootstrapRetryInFlight = false
            }
        }
    }

    suspend fun recoverCoreSessionNow(reason: String, forceRebootstrap: Boolean = false): Boolean = sessionRecoveryMutex.withLock {
        val existingSession = appSettings.getStringOrNull(AppSettingsKeys.CORE_SESSION_ID).orEmpty().trim()
        if (existingSession.isNotBlank() && !forceRebootstrap) return@withLock true
        if (forceRebootstrap && existingSession.isNotBlank()) {
            appSettings.setString(AppSettingsKeys.CORE_SESSION_ID, "")
            println("CoreSession: recover forcing re-bootstrap reason=$reason")
        }

        val fullName = appSettings.getStringOrNull(AppSettingsKeys.PERSONAL_DATA).orEmpty().trim()
        val token = appSettings.getStringOrNull(AppSettingsKeys.FCM_TOKEN).orEmpty().trim()
        val platform = pushPlatformId()

        if (fullName.isBlank() || token.isBlank()) {
            println("CoreSession: recover skipped reason=$reason missing_user_or_fcm")
            return@withLock false
        }

        println("CoreSession: recover attempt reason=$reason")
        val request = CoreSessionBootstrapRequest(
            full_name = fullName,
            platform = platform,
            device_id = DeviceIdentity.stableDeviceId(),
            fcm_token = token,
            login = appSettings.getStringOrNull(AppSettingsKeys.EMAIL),
            department = appSettings.getStringOrNull(AppSettingsKeys.DEPARTMENT),
            device_name = getPlatform().name,
            app_version = Secrets.VERSION,
        )

        return@withLock when (val res = repository.coreSessionBootstrap(request)) {
            is Resource.Success -> {
                appSettings.setString(AppSettingsKeys.CORE_SESSION_ID, res.data.sessionId)
                appSettings.setBool(AppSettingsKeys.CORE_BOOTSTRAP_RETRY_ON_TOKEN, false)
                println("CoreSession: recover success reason=$reason")
                true
            }
            is Resource.Error -> {
                println("CoreSession: recover failed reason=$reason ${res.causes ?: res.exception?.message}")
                false
            }
            is Resource.Loading -> false
        }
    }
}
