package com.tagaev.trrcrm.ui.login

import com.tagaev.trrcrm.ui.i18n.tr

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.essenty.backhandler.BackCallback
import com.tagaev.secrets.Secrets
import com.tagaev.trrcrm.data.AppSettings
import com.tagaev.trrcrm.data.remote.ApiConfig
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import com.tagaev.trrcrm.data.MainRepository
import com.tagaev.trrcrm.data.AppSettingsKeys
import com.tagaev.trrcrm.data.remote.Resource
import com.tagaev.trrcrm.data.remote.CoreApiErrorKind
import com.tagaev.trrcrm.data.remote.friendlyError
import com.tagaev.trrcrm.data.remote.toCoreApiError
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import okio.ByteString.Companion.encodeUtf8
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.plugins.RedirectResponseException
import io.ktor.client.plugins.ServerResponseException

sealed interface LoginUiState {
    data object Idle : LoginUiState
    data object Loading : LoginUiState
    data class Error(val message: String) : LoginUiState
    /** Saved token was explicitly rejected; show the credentials form after acknowledgement. */
    data object ReauthenticationRequired : LoginUiState
    data class StartupBlocked(val reason: StartupBlockReason) : LoginUiState
}

enum class StartupBlockReason {
    NoInternet,
    ServerError
}

/**
 * Contract used by LoginScreen.
 * Implemented by a Decompose component that saves creds/token and navigates onward.
 */
interface ILoginComponent {
    val uiState: StateFlow<LoginUiState>

    fun onLoginWithCredentials(user: String, pass: String)
    fun onLoginWithToken(token: String)
    fun retryStartup()
    fun dismissError()
    fun back()
}

class LoginComponent(
    componentContext: ComponentContext,
    private val onLoginSuccess: () -> Unit,
    private val onNoSavedAuth: () -> Unit = {},
    private val onBack: () -> Unit,
) : ILoginComponent, ComponentContext by componentContext, KoinComponent {
    companion object {
        private var startupCheckPassedThisSession = false
    }

    private val appSettings: AppSettings by inject()
    private val apiConfig: ApiConfig by inject()
    private val repo: MainRepository by inject()
    private val appScope: CoroutineScope by inject()
    private val mutex = kotlinx.coroutines.sync.Mutex()

    private val backCallback = BackCallback { /* NO HANDLE */ }

    private val _uiState = MutableStateFlow<LoginUiState>(LoginUiState.Idle)
    override val uiState: StateFlow<LoginUiState> = _uiState
    private val heartbeatRecoveryMutex = kotlinx.coroutines.sync.Mutex()

    init {
        appScope.launch {
            coldStartGateAndContinue()
        }

        backHandler.register(backCallback)
        if (!Secrets.IS_PUBLISH.toBoolean()) {
            println("PUSH_SERVICE: FCM TOKEN=" + appSettings.getString(AppSettingsKeys.FCM_TOKEN, "NULL"))
        }
    }

    override fun retryStartup() {
        appScope.launch {
            coldStartGateAndContinue()
        }
    }

    override fun dismissError() {
        if (_uiState.value is LoginUiState.Error ||
            _uiState.value is LoginUiState.ReauthenticationRequired
        ) {
            _uiState.value = LoginUiState.Idle
        }
    }

    private suspend fun coldStartGateAndContinue() {
        if (!startupCheckPassedThisSession) {
            withContext(Dispatchers.Main.immediate) {
                _uiState.value = LoginUiState.Loading
            }
            val probe = withContext(Dispatchers.Default) { repo.probeStartup() }
            val blockReason = (probe as? Resource.Error)?.let {
                classifyStartupBlock(it) ?: StartupBlockReason.NoInternet
            }

            if (blockReason != null) {
                withContext(Dispatchers.Main.immediate) {
                    _uiState.value = LoginUiState.StartupBlocked(blockReason)
                }
                return
            }
            startupCheckPassedThisSession = true
        }

        continueAsUsual()
    }

    private suspend fun continueAsUsual() {
        val savedToken = appSettings.getStringOrNull(AppSettingsKeys.TOKEN_KEY).orEmpty()
        val hasLegacyCredentials = hasLegacyCredentials()
        val needsLegacyMigration = hasLegacyCredentials &&
            !appSettings.getBool(AppSettingsKeys.TOKEN_ONLY_AUTH_MIGRATION_COMPLETED, false)

        withContext(Dispatchers.Main.immediate) {
            _uiState.value = LoginUiState.Idle
        }

        if (savedToken.isNotBlank()) {
            startTokenLogin(savedToken, allowLegacyCredentialsMigration = needsLegacyMigration)
        } else if (needsLegacyMigration) {
            // One-time compatibility path for users who update with old saved credentials.
            startCredentialsLogin(
                user = appSettings.getString(AppSettingsKeys.EMAIL, defaultValue = ""),
                pass = appSettings.getString(AppSettingsKeys.PASS, defaultValue = ""),
                isLegacyMigration = true
            )
        } else {
            println("Login: no saved auth context, opening demo zone")
            onNoSavedAuth()
        }
    }

    private fun hasLegacyCredentials(): Boolean =
        !appSettings.getStringOrNull(AppSettingsKeys.EMAIL).isNullOrBlank() &&
            !appSettings.getStringOrNull(AppSettingsKeys.PASS).isNullOrBlank()

    private fun completeLegacyMigration() {
        appSettings.setString(AppSettingsKeys.EMAIL, "")
        appSettings.setString(AppSettingsKeys.PASS, "")
        appSettings.setBool(AppSettingsKeys.TOKEN_ONLY_AUTH_MIGRATION_COMPLETED, true)
    }

    private fun isDefinitiveLegacyCredentialFailure(error: Resource.Error<*>): Boolean =
        error.exception is IllegalStateException

    private fun classifyStartupBlock(error: Resource.Error<*>): StartupBlockReason? {
        val ex = error.exception
        val message = (error.causes ?: ex?.message).orEmpty()
        val lower = message.lowercase()
        val codeFromMessage = Regex("""\b([3-5]\d{2})\b""")
            .find(message)
            ?.groupValues
            ?.getOrNull(1)
            ?.toIntOrNull()

        val serverStatusByType =
            ex is RedirectResponseException ||
                    ex is ClientRequestException ||
                    ex is ServerResponseException

        if (serverStatusByType || (codeFromMessage != null && codeFromMessage in 300..599)) {
            return StartupBlockReason.ServerError
        }

        val noInternetHints = listOf(
            "unresolvedaddress",
            "unknownhost",
            "connectexception",
            "sockettimeout",
            "network is unreachable",
            "failed to connect",
            "connection refused",
            "timed out"
        )
        if (noInternetHints.any { lower.contains(it) }) {
            return StartupBlockReason.NoInternet
        }

        // Any other probe failure should still block cold start as connection problem.
        if (ex != null || message.isNotBlank()) {
            return StartupBlockReason.NoInternet
        }

        return null
    }

    override fun onLoginWithCredentials(user: String, pass: String) {
        startCredentialsLogin(user, pass, isLegacyMigration = false)
    }

    private fun startCredentialsLogin(user: String, pass: String, isLegacyMigration: Boolean) {
        // Prevent concurrent attempts
        if (_uiState.value is LoginUiState.Loading) return

        appScope.launch {
            // Set loading on the MAIN thread (Compose/Decompose safe)
            withContext(Dispatchers.Main.immediate) {
                _uiState.value = LoginUiState.Loading
            }

            try {
                // Hash on any thread
                val passHash = if (pass.length == 64) {
                    pass
                } else {
                    pass.encodeUtf8().sha256().hex()
                }
                val res = withContext(Dispatchers.Default) {
                    CrmAuthUseCase.loginWithCredentials(user = user, pass = passHash)
                }

                when (res) {
                    is Resource.Success -> {
                        println("Success! We can LOGIN!")
                        withContext(Dispatchers.Main.immediate) {
                            if (isLegacyMigration) completeLegacyMigration()
                            completeLogin()
                        }
                    }
                    is Resource.Error -> {
                        val msg = res.causes ?: friendlyError(res.exception, tr("login_oshibka_avtorizatsii"))
                        withContext(Dispatchers.Main.immediate) {
                            // Do not retry bad legacy credentials on every future launch.
                            // Transport failures deliberately keep them for a later migration retry.
                            if (isLegacyMigration && isDefinitiveLegacyCredentialFailure(res)) {
                                completeLegacyMigration()
                            }
                            _uiState.value = LoginUiState.Error(msg)
                        }
                    }
                    Resource.Loading -> {
                        // no-op: already set to Loading on MAIN
                    }
                }
            } catch (t: Throwable) {
                withContext(Dispatchers.Main.immediate) {
                    _uiState.value = LoginUiState.Error(friendlyError(t, tr("login_oshibka_avtorizatsii")))
                }
            }
        }
    }

    private fun completeLogin() {
        _uiState.value = LoginUiState.Idle
        onLoginSuccess() // navigate (must be MAIN)
    }

    override fun onLoginWithToken(token: String) {
        startTokenLogin(token, allowLegacyCredentialsMigration = false)
    }

    private fun startTokenLogin(token: String, allowLegacyCredentialsMigration: Boolean) {
        if (_uiState.value is LoginUiState.Loading) return
        appScope.launch {
            withContext(Dispatchers.Main.immediate) {
                _uiState.value = LoginUiState.Loading
            }
            try {
                runCatching { apiConfig.token = token }
                val permissions = withContext(Dispatchers.Default) { CrmAuthUseCase.loginWithToken(token) }
                withContext(Dispatchers.Main.immediate) {
                    when (permissions) {
                        is Resource.Success -> {
                            if (allowLegacyCredentialsMigration) completeLegacyMigration()
                            completeLogin()
                        }
                        is Resource.Loading -> Unit
                        is Resource.Error -> {
                            val msg = permissions.causes
                                ?: friendlyError(permissions.exception, tr("login_ne_udalos_zagruzit_prava_dostupa"))
                            if (allowLegacyCredentialsMigration && isTokenAuthenticationError(msg)) {
                                _uiState.value = LoginUiState.Idle
                                startCredentialsLogin(
                                    user = appSettings.getString(AppSettingsKeys.EMAIL, defaultValue = ""),
                                    pass = appSettings.getString(AppSettingsKeys.PASS, defaultValue = ""),
                                    isLegacyMigration = true
                                )
                                return@withContext
                            }
                            // Keep the saved token intact. It is cleared only by explicit logout.
                            _uiState.value = if (isTokenAuthenticationError(msg)) {
                                LoginUiState.ReauthenticationRequired
                            } else {
                                LoginUiState.Error(msg)
                            }
                        }
                    }
                }
            } catch (t: Throwable) {
                withContext(Dispatchers.Main.immediate) {
                    _uiState.value = LoginUiState.Error(friendlyError(t, tr("login_oshibka_avtorizatsii")))
                }
            }
        }
    }


    //TODO test request get token by login and password

    override fun back() = onBack()
}
