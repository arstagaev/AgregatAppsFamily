package com.tagaev.trrcrm.ui.login

import com.tagaev.trrcrm.ui.i18n.tr

import com.tagaev.trrcrm.data.AppSettings
import com.tagaev.trrcrm.data.AppSettingsKeys
import com.tagaev.trrcrm.data.MainRepository
import com.tagaev.trrcrm.data.remote.ApiConfig
import com.tagaev.trrcrm.data.remote.Resource
import com.tagaev.trrcrm.data.remote.friendlyError
import com.tagaev.trrcrm.data.accounts.AccountSlot
import com.tagaev.trrcrm.data.accounts.AccountSessionStore
import com.tagaev.trrcrm.data.accounts.AddAccountResult
import com.tagaev.trrcrm.data.featureflags.MobileFeatureFlagsSync
import com.tagaev.trrcrm.push.CoreSessionCoordinator
import com.tagaev.trrcrm.push.CoreSessionResult
import com.tagaev.trrcrm.push.triggerPostLoginPushPermissionCheck
import com.tagaev.trrcrm.utils.SessionPermissions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import okio.ByteString.Companion.encodeUtf8

object CrmAuthUseCase : KoinComponent {
    private val appSettings: AppSettings by inject()
    private val apiConfig: ApiConfig by inject()
    private val repo: MainRepository by inject()
    private val appScope: CoroutineScope by inject()
    private val accountStore: AccountSessionStore by inject()
    private val coreSession: CoreSessionCoordinator by inject()

    fun stopSessionLoops() {
        coreSession.stopHeartbeat()
    }

    suspend fun loginWithCredentials(user: String, pass: String): Resource<Unit> {
        val passHash = if (pass.length == 64) pass else pass.encodeUtf8().sha256().hex()
        return SessionExpiryBridge.suppressing {
            when (val tokenRes = repo.getToken(username = user, password = passHash)) {
                is Resource.Success -> {
                    val data = tokenRes.data
                    val token = data.token.orEmpty()
                    if (token.isBlank()) {
                        Resource.Error(causes = tr("login_pustoy_token_ot_servera"))
                    } else {
                        val pendingAdd = accountStore.snapshot().pendingCreateNewSlot
                        val previous = if (pendingAdd) accountStore.activeAccount() else null
                        rejectIfCannotBind(
                            fullName = data.fullName.orEmpty(),
                            login = user.trim(),
                            previous = previous,
                        )?.let { return@suppressing it }
                        appSettings.setString(AppSettingsKeys.EMAIL, "")
                        appSettings.setString(AppSettingsKeys.PASS, "")
                        appSettings.setString(AppSettingsKeys.TOKEN_KEY, token)
                        appSettings.setString(AppSettingsKeys.PERSONAL_DATA, data.fullName.orEmpty())
                        appSettings.setString(AppSettingsKeys.DEPARTMENT, data.department.orEmpty())
                        appSettings.setString(AppSettingsKeys.ACCOUNT_LOGIN, user.trim())
                        runCatching { apiConfig.token = token }
                        val result = authenticateWithTokenAndFinalize(forceBootstrap = true)
                        if (result is Resource.Error && previous != null) {
                            restorePreviousSlot(previous)
                        }
                        result
                    }
                }
                is Resource.Error -> {
                    Resource.Error(tokenRes.exception, tokenRes.causes ?: friendlyError(tokenRes.exception, tr("login_oshibka_avtorizatsii")))
                }
                is Resource.Loading -> Resource.Loading
            }
        }
    }

    suspend fun loginWithToken(token: String): Resource<Unit> {
        if (token.isBlank()) return Resource.Error(causes = tr("login_pustoy_token"))
        rejectIfCannotBind(
            fullName = appSettings.getStringOrNull(AppSettingsKeys.PERSONAL_DATA).orEmpty(),
            login = appSettings.getStringOrNull(AppSettingsKeys.ACCOUNT_LOGIN).orEmpty(),
            previous = if (accountStore.snapshot().pendingCreateNewSlot) accountStore.activeAccount() else null,
        )?.let { return it }
        runCatching { apiConfig.token = token }
        return SessionExpiryBridge.suppressing {
            authenticateWithTokenAndFinalize(
                onPermissionsGranted = { appSettings.setString(AppSettingsKeys.TOKEN_KEY, token) },
                forceBootstrap = false,
            )
        }
    }

    private suspend fun authenticateWithTokenAndFinalize(
        onPermissionsGranted: () -> Unit = {},
        forceBootstrap: Boolean,
    ): Resource<Unit> {
        SessionPermissions.clear()
        return when (val permissions = repo.getPermission()) {
            is Resource.Success -> {
                onPermissionsGranted()
                SessionPermissions.replaceAll(permissions.data)
                finalizeAuthenticatedSession(forceBootstrap = forceBootstrap)
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

    private suspend fun finalizeAuthenticatedSession(forceBootstrap: Boolean): Resource<Unit> {
        val pendingAdd = accountStore.snapshot().pendingCreateNewSlot
        val previous = if (pendingAdd) accountStore.activeAccount() else null
        rejectIfCannotBind(
            fullName = appSettings.getStringOrNull(AppSettingsKeys.PERSONAL_DATA).orEmpty(),
            login = appSettings.getStringOrNull(AppSettingsKeys.ACCOUNT_LOGIN).orEmpty(),
            previous = previous,
        )?.let { return it }
        val bindResult = accountStore.bindSuccessfulLogin(
            appSettings.getStringOrNull(AppSettingsKeys.ACCOUNT_LOGIN).orEmpty()
        )
        when (bindResult) {
            AddAccountResult.DuplicateIdentity -> {
                restoreAfterRejectedBind(previous)
                return Resource.Error(causes = tr("accounts_duplicate_identity"))
            }
            AddAccountResult.LimitReached -> {
                restoreAfterRejectedBind(previous)
                return Resource.Error(causes = tr("accounts_limit"))
            }
            else -> Unit
        }
        val createdNewSlot = bindResult is AddAccountResult.Created

        triggerPostLoginPushPermissionCheck()
        appScope.launch {
            repo.refreshPushFeatureToggleIfNeeded(force = true)
            MobileFeatureFlagsSync.refreshNow(reason = "login", force = true)
        }
        val bootstrap = if (forceBootstrap) {
            coreSession.bootstrapCurrentUserAfterCrmLogin()
        } else {
            coreSession.ensureActiveSession(reason = "login_token")
        }
        return when (bootstrap) {
            is CoreSessionResult.Ok -> {
                coreSession.commitAndStartHeartbeat(bootstrap.sessionId)
                accountStore.snapshotActiveFromSettings()
                Resource.Success(Unit)
            }
            CoreSessionResult.DeferredMissingFcm -> {
                if (pendingAdd) {
                    coreSession.stopHeartbeat()
                    appSettings.setString(AppSettingsKeys.CORE_SESSION_ID, "")
                }
                accountStore.snapshotActiveFromSettings()
                Resource.Success(Unit)
            }
            CoreSessionResult.SkippedMissingUser -> {
                accountStore.snapshotActiveFromSettings()
                Resource.Success(Unit)
            }
            is CoreSessionResult.Error -> {
                if (previous != null) {
                    rollbackCreatedSlot(previous, createdNewSlot)
                    Resource.Error(causes = bootstrap.message)
                } else {
                    Resource.Success(Unit)
                }
            }
        }
    }

    private fun rejectIfCannotBind(
        fullName: String,
        login: String,
        previous: AccountSlot?,
    ): Resource<Unit>? {
        return when (accountStore.preflightLoginBind(fullName, login)) {
            AddAccountResult.DuplicateIdentity -> {
                restoreAfterRejectedBind(previous)
                Resource.Error(causes = tr("accounts_duplicate_identity"))
            }
            AddAccountResult.LimitReached -> {
                restoreAfterRejectedBind(previous)
                Resource.Error(causes = tr("accounts_limit"))
            }
            else -> null
        }
    }

    private fun rollbackCreatedSlot(previous: AccountSlot, createdNewSlot: Boolean) {
        if (createdNewSlot) {
            val createdId = accountStore.activeAccount()?.id
            if (createdId != null && createdId != previous.id) {
                accountStore.removeAccount(createdId)
            }
        }
        accountStore.activate(previous.id)
        restorePreviousSlot(previous)
    }

    private fun restoreAfterRejectedBind(previous: AccountSlot?) {
        accountStore.clearPendingCreateNewSlot()
        val restore = previous ?: accountStore.activeAccount() ?: return
        restorePreviousSlot(restore)
    }

    private fun restorePreviousSlot(previous: AccountSlot) {
        accountStore.clearPendingCreateNewSlot()
        accountStore.applySlotToSettings(previous)
        runCatching { apiConfig.token = previous.token }
    }
}
