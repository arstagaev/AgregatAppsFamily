package com.tagaev.trrcrm.ui.login

import com.tagaev.trrcrm.ui.i18n.tr

import com.tagaev.secrets.Secrets
import com.tagaev.trrcrm.data.AppSettings
import com.tagaev.trrcrm.data.AppSettingsKeys
import com.tagaev.trrcrm.data.MainRepository
import com.tagaev.trrcrm.data.remote.ApiConfig
import com.tagaev.trrcrm.data.remote.CoreApiErrorKind
import com.tagaev.trrcrm.data.remote.Resource
import com.tagaev.trrcrm.data.remote.friendlyError
import com.tagaev.trrcrm.data.remote.toCoreApiError
import com.tagaev.trrcrm.getPlatform
import com.tagaev.trrcrm.models.CoreSessionBootstrapRequest
import com.tagaev.trrcrm.models.CoreSessionHeartbeatRequest
import com.tagaev.trrcrm.data.featureflags.MobileFeatureFlagsSync
import com.tagaev.trrcrm.push.PushRegistrationCoordinator
import com.tagaev.trrcrm.push.UnreadCountSync
import com.tagaev.trrcrm.push.triggerPostLoginPushPermissionCheck
import com.tagaev.trrcrm.pushPlatformId
import com.tagaev.trrcrm.utils.DeviceIdentity
import com.tagaev.trrcrm.utils.SessionPermissions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import okio.ByteString.Companion.encodeUtf8

object CrmAuthUseCase : KoinComponent {
    private val appSettings: AppSettings by inject()
    private val apiConfig: ApiConfig by inject()
    private val repo: MainRepository by inject()
    private val appScope: CoroutineScope by inject()

    private var coreHeartbeatJob: Job? = null
    private val heartbeatRecoveryMutex = Mutex()

    suspend fun loginWithCredentials(user: String, pass: String): Resource<Unit> {
        val passHash = if (pass.length == 64) pass else pass.encodeUtf8().sha256().hex()
        return when (val tokenRes = repo.getToken(username = user, password = passHash)) {
            is Resource.Success -> {
                val data = tokenRes.data
                val token = data.token.orEmpty()
                if (token.isBlank()) {
                    Resource.Error(causes = tr("login_pustoy_token_ot_servera"))
                } else {
                    // Do not persist login or password hash. A token is enough to restore a session.
                    appSettings.setString(AppSettingsKeys.EMAIL, "")
                    appSettings.setString(AppSettingsKeys.PASS, "")
                    appSettings.setString(AppSettingsKeys.TOKEN_KEY, token)
                    appSettings.setString(AppSettingsKeys.PERSONAL_DATA, data.fullName.orEmpty())
                    appSettings.setString(AppSettingsKeys.DEPARTMENT, data.department.orEmpty())
                    runCatching { apiConfig.token = token }
                    authenticateWithTokenAndFinalize()
                }
            }
            is Resource.Error -> {
                Resource.Error(tokenRes.exception, tokenRes.causes ?: friendlyError(tokenRes.exception, tr("login_oshibka_avtorizatsii")))
            }
            is Resource.Loading -> Resource.Loading
        }
    }

    suspend fun loginWithToken(token: String): Resource<Unit> {
        if (token.isBlank()) return Resource.Error(causes = tr("login_pustoy_token"))
        runCatching { apiConfig.token = token }
        return authenticateWithTokenAndFinalize(
            onPermissionsGranted = { appSettings.setString(AppSettingsKeys.TOKEN_KEY, token) }
        )
    }

    private suspend fun authenticateWithTokenAndFinalize(
        onPermissionsGranted: () -> Unit = {}
    ): Resource<Unit> {
        SessionPermissions.clear()
        return when (val permissions = repo.getPermission()) {
            is Resource.Success -> {
                onPermissionsGranted()
                SessionPermissions.replaceAll(permissions.data)
                completeLoginSideEffects()
                Resource.Success(Unit)
            }
            is Resource.Error -> {
                Resource.Error(
                    permissions.exception,
                    permissions.causes ?: friendlyError(permissions.exception, tr("login_ne_udalos_zagruzit_prava_dostupa"))
                )
            }
            is Resource.Loading -> Resource.Loading
        }
    }

    private fun completeLoginSideEffects() {
        startCoreHeartbeatLoop()
        triggerPostLoginPushPermissionCheck()
        appScope.launch {
            repo.refreshPushFeatureToggleIfNeeded(force = true)
            MobileFeatureFlagsSync.refreshNow(reason = "login", force = true)
            bootstrapCoreSessionAndStartHeartbeat()
        }
        PushRegistrationCoordinator.registerIfReady(preferredPlatform = pushPlatformId())
    }

    private suspend fun bootstrapCoreSessionAndStartHeartbeat() {
        val fullName = appSettings.getStringOrNull(AppSettingsKeys.PERSONAL_DATA).orEmpty().trim()
        val fcmToken = appSettings.getStringOrNull(AppSettingsKeys.FCM_TOKEN)?.trim()?.takeIf { it.isNotBlank() }
        if (fullName.isBlank()) {
            println("CoreSession: bootstrap skipped (missing_user)")
            return
        }
        if (fcmToken == null) {
            appSettings.setBool(AppSettingsKeys.CORE_BOOTSTRAP_RETRY_ON_TOKEN, true)
            println("CoreSession: bootstrap deferred (missing_fcm_token); waiting token to retry")
            return
        }
        val req = CoreSessionBootstrapRequest(
            full_name = fullName,
            platform = pushPlatformId(),
            device_id = DeviceIdentity.stableDeviceId(),
            fcm_token = fcmToken,
            login = appSettings.getStringOrNull(AppSettingsKeys.EMAIL),
            department = appSettings.getStringOrNull(AppSettingsKeys.DEPARTMENT),
            device_name = getPlatform().name,
            app_version = Secrets.VERSION,
        )

        when (val res = repo.coreSessionBootstrap(req)) {
            is Resource.Success -> {
                appSettings.setString(AppSettingsKeys.CORE_SESSION_ID, res.data.sessionId)
                appSettings.setBool(AppSettingsKeys.CORE_BOOTSTRAP_RETRY_ON_TOKEN, false)
                println("CoreSession: bootstrap success mode=with_fcm")
                UnreadCountSync.refreshAsync(reason = "bootstrap_success", force = true)
                startCoreHeartbeatLoop()
            }
            is Resource.Error -> {
                val mapped = res.exception.toCoreApiError(res.causes ?: "bootstrap failed")
                if (mapped.kind == CoreApiErrorKind.Validation) {
                    appSettings.setBool(AppSettingsKeys.CORE_BOOTSTRAP_RETRY_ON_TOKEN, true)
                }
                println("CoreSession: bootstrap failed mode=with_fcm ${res.causes ?: res.exception?.message}")
            }
            is Resource.Loading -> Unit
        }
    }

    private fun startCoreHeartbeatLoop() {
        if (coreHeartbeatJob?.isActive == true) return
        coreHeartbeatJob = appScope.launch {
            while (true) {
                delay(5 * 60 * 1000L)
                val sessionId = appSettings.getStringOrNull(AppSettingsKeys.CORE_SESSION_ID).orEmpty()
                if (sessionId.isBlank()) continue
                val heartbeatReq = CoreSessionHeartbeatRequest(
                    sessionId = sessionId,
                    fcmToken = appSettings.getStringOrNull(AppSettingsKeys.FCM_TOKEN),
                    appVersion = Secrets.VERSION
                )
                when (val hb = repo.coreSessionHeartbeat(heartbeatReq)) {
                    is Resource.Success -> Unit
                    is Resource.Error -> {
                        val mapped = hb.exception.toCoreApiError(hb.causes ?: "Heartbeat failed")
                        if (mapped.kind == CoreApiErrorKind.NotFound) {
                            recoverCoreSessionAfterHeartbeat404()
                        } else {
                            println("CoreSession: heartbeat failed ${hb.causes ?: hb.exception?.message}")
                        }
                    }
                    is Resource.Loading -> Unit
                }
            }
        }
    }

    private suspend fun recoverCoreSessionAfterHeartbeat404() {
        heartbeatRecoveryMutex.withLock {
            println("CoreSession: heartbeat returned 404, attempting transparent re-bootstrap")
            bootstrapCoreSessionAndStartHeartbeat()
        }
    }
}
