package com.tagaev.trrcrm.push

import com.tagaev.secrets.Secrets
import com.tagaev.trrcrm.data.AppSettings
import com.tagaev.trrcrm.data.AppSettingsKeys
import com.tagaev.trrcrm.data.MainRepository
import com.tagaev.trrcrm.data.accounts.AccountSessionStore
import com.tagaev.trrcrm.data.accounts.AccountSlot
import com.tagaev.trrcrm.data.remote.CoreApiErrorKind
import com.tagaev.trrcrm.data.remote.Resource
import com.tagaev.trrcrm.data.remote.toCoreApiError
import com.tagaev.trrcrm.getPlatform
import com.tagaev.trrcrm.models.CoreSessionBootstrapRequest
import com.tagaev.trrcrm.models.CoreSessionBootstrapResponse
import com.tagaev.trrcrm.models.CoreSessionHeartbeatRequest
import com.tagaev.trrcrm.models.CoreSessionHeartbeatResponse
import com.tagaev.trrcrm.models.CoreSessionLogoutRequest
import com.tagaev.trrcrm.models.CoreSessionLogoutResponse
import com.tagaev.trrcrm.pushPlatformId
import com.tagaev.trrcrm.ui.i18n.tr
import com.tagaev.trrcrm.utils.DeviceIdentity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

interface CoreSessionGateway {
    suspend fun bootstrap(request: CoreSessionBootstrapRequest): Resource<CoreSessionBootstrapResponse>
    suspend fun heartbeat(request: CoreSessionHeartbeatRequest): Resource<CoreSessionHeartbeatResponse>
    suspend fun logout(request: CoreSessionLogoutRequest): Resource<CoreSessionLogoutResponse>
}

class MainRepositoryCoreSessionGateway(
    private val repository: MainRepository,
) : CoreSessionGateway {
    override suspend fun bootstrap(request: CoreSessionBootstrapRequest) =
        repository.coreSessionBootstrap(request)

    override suspend fun heartbeat(request: CoreSessionHeartbeatRequest) =
        repository.coreSessionHeartbeat(request)

    override suspend fun logout(request: CoreSessionLogoutRequest) =
        repository.coreSessionLogout(request)
}

data class CoreSessionIdentity(
    val fullName: String,
    val login: String? = null,
    val department: String? = null,
)

sealed interface CoreSessionResult {
    data class Ok(val sessionId: String) : CoreSessionResult
    data class Error(val kind: CoreApiErrorKind, val message: String) : CoreSessionResult
    data object DeferredMissingFcm : CoreSessionResult
    data object SkippedMissingUser : CoreSessionResult
}

class CoreSessionCoordinator(
    private val settings: AppSettings,
    private val gateway: CoreSessionGateway,
    private val accountStore: AccountSessionStore,
    private val appScope: CoroutineScope,
    private val platformId: () -> String = { pushPlatformId() },
    private val deviceId: () -> String = { DeviceIdentity.stableDeviceId() },
    private val deviceName: () -> String = { getPlatform().name },
    private val appVersion: () -> String = { Secrets.VERSION },
    private val delayMs: suspend (Long) -> Unit = { delay(it) },
    private val heartbeatIntervalMs: Long = DEFAULT_HEARTBEAT_INTERVAL_MS,
    private val backoffDelaysMs: List<Long> = DEFAULT_BACKOFF_MS,
    private val onSessionReady: (String) -> Unit = { sessionId ->
        if (sessionId.isNotBlank()) {
            UnreadCountSync.refreshAsync(reason = "bootstrap_success", force = true)
        }
    },
) {
    private val mutex = Mutex()
    private var heartbeatJob: Job? = null

    fun currentSessionId(): String =
        settings.getStringOrNull(AppSettingsKeys.CORE_SESSION_ID).orEmpty().trim()

    fun stopHeartbeat() {
        heartbeatJob?.cancel()
        heartbeatJob = null
    }

    fun isHeartbeatRunning(): Boolean = heartbeatJob?.isActive == true

    suspend fun ensureActiveSession(reason: String = "ensure"): CoreSessionResult = mutex.withLock {
        ensureActiveSessionLocked(reason)
    }

    suspend fun recover(reason: String, forceRebootstrap: Boolean = false): Boolean = mutex.withLock {
        val existing = currentSessionId()
        if (existing.isNotBlank() && !forceRebootstrap) {
            startHeartbeatLocked()
            return@withLock true
        }
        if (forceRebootstrap) {
            clearActiveSessionId()
        }
        ensureActiveSessionLocked(reason) is CoreSessionResult.Ok
    }

    suspend fun bootstrapCurrentUserAfterCrmLogin(): CoreSessionResult = mutex.withLock {
        bootstrapLocked(identityFromSettings(), reason = "crm_login")
    }

    suspend fun commitAndStartHeartbeat(sessionId: String) = mutex.withLock {
        commitSessionLocked(sessionId)
    }

    suspend fun switchTo(target: AccountSlot): CoreSessionResult = mutex.withLock {
        val previous = accountStore.activeAccount()
        if (previous?.id == target.id && currentSessionId().isNotBlank()) {
            startHeartbeatLocked()
            return@withLock CoreSessionResult.Ok(currentSessionId())
        }
        val result = bootstrapLocked(identityFromSlot(target), reason = "switch")
        when (result) {
            is CoreSessionResult.Ok -> {
                accountStore.snapshotActiveFromSettings()
                stopHeartbeat()
                accountStore.activate(target.id)
                commitSessionLocked(result.sessionId)
                result
            }
            CoreSessionResult.DeferredMissingFcm -> {
                accountStore.snapshotActiveFromSettings()
                stopHeartbeat()
                accountStore.activate(target.id)
                settings.setString(AppSettingsKeys.CORE_SESSION_ID, "")
                settings.setBool(AppSettingsKeys.CORE_BOOTSTRAP_RETRY_ON_TOKEN, true)
                result
            }
            else -> result
        }
    }

    suspend fun onFcmToken(token: String) {
        if (token.isBlank()) return
        mutex.withLock {
            val existing = settings.getStringOrNull(AppSettingsKeys.FCM_TOKEN).orEmpty().trim()
            if (existing != token) {
                settings.setString(AppSettingsKeys.FCM_TOKEN, token)
            }
            val sessionId = currentSessionId()
            if (sessionId.isNotBlank()) {
                sendHeartbeatLocked(sessionId)
            } else {
                bootstrapLocked(identityFromSettings(), reason = "fcm_token")
                    .takeIf { it is CoreSessionResult.Ok }
                    ?.let { commitSessionLocked((it as CoreSessionResult.Ok).sessionId) }
            }
        }
    }

    suspend fun logoutActive(deactivateDeviceToken: Boolean) {
        mutex.withLock {
            val sessionId = currentSessionId()
            stopHeartbeat()
            if (sessionId.isNotBlank()) {
                runCatching {
                    gateway.logout(
                        CoreSessionLogoutRequest(
                            sessionId = sessionId,
                            deactivateDeviceToken = deactivateDeviceToken,
                        )
                    )
                }
            }
            clearActiveSessionId()
        }
    }

    suspend fun heartbeatOnce() = mutex.withLock {
        val sessionId = currentSessionId()
        if (sessionId.isBlank()) return@withLock
        sendHeartbeatLocked(sessionId)
    }

    private suspend fun ensureActiveSessionLocked(reason: String): CoreSessionResult {
        val identity = identityFromSettings()
        if (identity.fullName.isBlank()) {
            val slot = accountStore.activeAccount()
            if (slot == null || slot.fullName.isBlank()) {
                println("CoreSession: ensure skipped reason=$reason missing_user")
                return CoreSessionResult.SkippedMissingUser
            }
            val fromSlot = bootstrapLocked(identityFromSlot(slot), reason)
            if (fromSlot is CoreSessionResult.Ok) commitSessionLocked(fromSlot.sessionId)
            return fromSlot
        }
        val sessionId = currentSessionId()
        val fcm = currentFcmToken()
        if (sessionId.isNotBlank() && fcm != null) {
            startHeartbeatLocked()
            return CoreSessionResult.Ok(sessionId)
        }
        val result = bootstrapLocked(identity, reason)
        if (result is CoreSessionResult.Ok) {
            commitSessionLocked(result.sessionId)
        }
        return result
    }

    private suspend fun bootstrapLocked(
        identity: CoreSessionIdentity,
        reason: String,
    ): CoreSessionResult {
        val fullName = identity.fullName.trim()
        if (fullName.isBlank()) {
            println("CoreSession: bootstrap skipped reason=$reason missing_user")
            return CoreSessionResult.SkippedMissingUser
        }
        val fcm = currentFcmToken()
        if (fcm == null) {
            settings.setBool(AppSettingsKeys.CORE_BOOTSTRAP_RETRY_ON_TOKEN, true)
            println("CoreSession: bootstrap deferred reason=$reason missing_fcm_token")
            return CoreSessionResult.DeferredMissingFcm
        }
        var lastError: CoreSessionResult.Error? = null
        val attempts = backoffDelaysMs.size + 1
        repeat(attempts) { attempt ->
            if (attempt > 0) {
                delayMs(backoffDelaysMs[attempt - 1])
            }
            val request = CoreSessionBootstrapRequest(
                full_name = fullName,
                platform = platformId(),
                device_id = deviceId(),
                fcm_token = fcm,
                login = identity.login?.trim()?.takeIf { it.isNotBlank() },
                department = identity.department?.trim()?.takeIf { it.isNotBlank() },
                device_name = deviceName(),
                app_version = appVersion(),
            )
            println("CoreSession: bootstrap attempt=${attempt + 1} reason=$reason")
            when (val res = gateway.bootstrap(request)) {
                is Resource.Success -> {
                    val sessionId = res.data.sessionId.trim()
                    if (sessionId.isBlank()) {
                        println("CoreSession: bootstrap failed reason=$reason empty_session_id")
                        return CoreSessionResult.Error(
                            kind = CoreApiErrorKind.Unknown,
                            message = tr("login_pustoy_token_ot_servera"),
                        )
                    }
                    settings.setBool(AppSettingsKeys.CORE_BOOTSTRAP_RETRY_ON_TOKEN, false)
                    println("CoreSession: bootstrap success reason=$reason")
                    return CoreSessionResult.Ok(sessionId)
                }
                is Resource.Error -> {
                    val mapped = res.exception.toCoreApiError(res.causes ?: "bootstrap failed")
                    lastError = CoreSessionResult.Error(mapped.kind, mapped.message.ifBlank {
                        res.causes ?: tr("accounts_switch_failed")
                    })
                    if (!mapped.kind.shouldRetryBootstrap()) {
                        if (mapped.kind == CoreApiErrorKind.Conflict) {
                            lastError = CoreSessionResult.Error(
                                kind = CoreApiErrorKind.Conflict,
                                message = tr("accounts_session_conflict"),
                            )
                        }
                        println("CoreSession: bootstrap failed reason=$reason kind=${mapped.kind}")
                        return lastError
                    }
                    println("CoreSession: bootstrap retryable failure reason=$reason kind=${mapped.kind}")
                }
                is Resource.Loading -> Unit
            }
        }
        return lastError ?: CoreSessionResult.Error(
            kind = CoreApiErrorKind.Unknown,
            message = tr("accounts_switch_failed"),
        )
    }

    private suspend fun sendHeartbeatLocked(sessionId: String) {
        val heartbeatReq = CoreSessionHeartbeatRequest(
            sessionId = sessionId,
            fcmToken = settings.getStringOrNull(AppSettingsKeys.FCM_TOKEN),
            appVersion = appVersion(),
        )
        when (val hb = gateway.heartbeat(heartbeatReq)) {
            is Resource.Success -> Unit
            is Resource.Error -> {
                val mapped = hb.exception.toCoreApiError(hb.causes ?: "Heartbeat failed")
                if (mapped.kind == CoreApiErrorKind.NotFound) {
                    println("CoreSession: heartbeat 404, recovering selected slot")
                    clearActiveSessionId()
                    val recovered = bootstrapLocked(identityFromSettingsOrSlot(), reason = "heartbeat_404")
                    if (recovered is CoreSessionResult.Ok) {
                        commitSessionLocked(recovered.sessionId)
                    }
                } else {
                    println("CoreSession: heartbeat failed kind=${mapped.kind}")
                }
            }
            is Resource.Loading -> Unit
        }
    }

    private fun commitSessionLocked(sessionId: String) {
        settings.setString(AppSettingsKeys.CORE_SESSION_ID, sessionId)
        settings.setBool(AppSettingsKeys.CORE_BOOTSTRAP_RETRY_ON_TOKEN, false)
        accountStore.updateActiveToken(
            token = settings.getStringOrNull(AppSettingsKeys.TOKEN_KEY).orEmpty(),
            coreSessionId = sessionId,
        )
        startHeartbeatLocked()
        onSessionReady(sessionId)
    }

    private fun startHeartbeatLocked() {
        if (heartbeatJob?.isActive == true) return
        heartbeatJob = appScope.launch {
            while (true) {
                delayMs(heartbeatIntervalMs)
                val sessionId = currentSessionId()
                if (sessionId.isBlank()) continue
                mutex.withLock {
                    val latest = currentSessionId()
                    if (latest.isNotBlank()) {
                        sendHeartbeatLocked(latest)
                    }
                }
            }
        }
    }

    private fun clearActiveSessionId() {
        settings.setString(AppSettingsKeys.CORE_SESSION_ID, "")
    }

    private fun currentFcmToken(): String? =
        settings.getStringOrNull(AppSettingsKeys.FCM_TOKEN)?.trim()?.takeIf { it.isNotBlank() }

    private fun identityFromSettings(): CoreSessionIdentity = CoreSessionIdentity(
        fullName = settings.getStringOrNull(AppSettingsKeys.PERSONAL_DATA).orEmpty().trim(),
        login = settings.getStringOrNull(AppSettingsKeys.ACCOUNT_LOGIN)
            ?.ifBlank { settings.getStringOrNull(AppSettingsKeys.EMAIL) },
        department = settings.getStringOrNull(AppSettingsKeys.DEPARTMENT),
    )

    private fun identityFromSlot(slot: AccountSlot): CoreSessionIdentity = CoreSessionIdentity(
        fullName = slot.fullName.trim().ifBlank { slot.displayName },
        login = slot.login.trim().takeIf { it.isNotBlank() },
        department = slot.department.trim().takeIf { it.isNotBlank() },
    )

    private fun identityFromSettingsOrSlot(): CoreSessionIdentity {
        val fromSettings = identityFromSettings()
        if (fromSettings.fullName.isNotBlank()) return fromSettings
        val slot = accountStore.activeAccount() ?: return fromSettings
        return identityFromSlot(slot)
    }

    companion object {
        const val DEFAULT_HEARTBEAT_INTERVAL_MS = 5 * 60 * 1000L
        val DEFAULT_BACKOFF_MS = listOf(500L, 1_500L, 4_000L)
    }
}

private fun CoreApiErrorKind.shouldRetryBootstrap(): Boolean = when (this) {
    CoreApiErrorKind.Network, CoreApiErrorKind.Timeout, CoreApiErrorKind.Server -> true
    else -> false
}
