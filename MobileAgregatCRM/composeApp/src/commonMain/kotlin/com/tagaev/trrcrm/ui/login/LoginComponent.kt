package com.tagaev.trrcrm.ui.login

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
import com.tagaev.trrcrm.pushPlatformId
import com.tagaev.trrcrm.push.PushRegistrationCoordinator
import com.tagaev.trrcrm.utils.SessionPermissions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
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
    fun back()
}

class LoginComponent(
    componentContext: ComponentContext,
    private val onLoginSuccess: () -> Unit,
    private val onBack: () -> Unit,
) : ILoginComponent, ComponentContext by componentContext, KoinComponent {
    companion object {
        private var startupCheckPassedThisSession = false
        private var coreHeartbeatJob: Job? = null
    }

    private val appSettings: AppSettings by inject()
    private val apiConfig: ApiConfig by inject()
    private val repo: MainRepository by inject()
    private val appScope: CoroutineScope by inject()
    private val mutex = kotlinx.coroutines.sync.Mutex()

    private val backCallback = BackCallback { /* NO HANDLE */ }

    private val _uiState = MutableStateFlow<LoginUiState>(LoginUiState.Idle)
    override val uiState: StateFlow<LoginUiState> = _uiState
    private var tokenRefreshAttemptedThisLogin = false

    init {
        appScope.launch {
            coldStartGateAndContinue()
        }

        backHandler.register(backCallback)
        if (!Secrets.IS_PUBLISH.toBoolean()) {
            println("FCM TOKEN: " +appSettings.getString(AppSettingsKeys.FCM_TOKEN,"NULL"))
        }
    }

    override fun retryStartup() {
        appScope.launch {
            coldStartGateAndContinue()
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
        val hasSavedCredentials = hasSavedCredentials()

        withContext(Dispatchers.Main.immediate) {
            _uiState.value = LoginUiState.Idle
        }

        tokenRefreshAttemptedThisLogin = false
        if (savedToken.isNotBlank()) {
            onLoginWithToken(savedToken)
        } else if (hasSavedCredentials) {
            onLoginWithCredentials(
                user = appSettings.getString(AppSettingsKeys.EMAIL, defaultValue = ""),
                pass = appSettings.getString(AppSettingsKeys.PASS, defaultValue = "")
            )
        } else {
            println("Email or Token is empty")
        }
    }

    private fun hasSavedCredentials(): Boolean {
        return !appSettings.getStringOrNull(AppSettingsKeys.EMAIL).isNullOrEmpty() &&
            !appSettings.getStringOrNull(AppSettingsKeys.PASS).isNullOrEmpty()
    }

    private fun isTokenAuthenticationError(message: String?): Boolean {
        val raw = message?.trim().orEmpty()
        if (raw.isBlank()) return false
        val lower = raw.lowercase()
        return lower.contains("token authentification error") ||
            lower.contains("token authentication error")
    }

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
                println("onLoginWithCredentials> pass${pass}")
                appSettings.setString(AppSettingsKeys.EMAIL, user)
                appSettings.setString(AppSettingsKeys.PASS, passHash)
                println("onLoginWithCredentials> ${passHash}")
                // Call network on IO
                val res = withContext(Dispatchers.Default) {
                    repo.getToken(username = user, password = passHash)
                }

                when (res) {
                    is Resource.Success -> {
                        println("Success! We can LOGIN!")
                        val data = res.data
                        withContext(Dispatchers.Main.immediate) {
                            if (!data.token.isNullOrBlank()) {
                                appSettings.setString(AppSettingsKeys.TOKEN_KEY, data.token)
                                appSettings.setString(AppSettingsKeys.PERSONAL_DATA, "${data.fullName}")
                                appSettings.setString(AppSettingsKeys.DEPARTMENT, "${data.department}")
//                                settings.setString(AppSettingsKeys.FILTER_VAL, data.department)
                                runCatching { apiConfig.token = data.token }

                                SessionPermissions.clear()
                                val permissions = withContext(Dispatchers.Default) {
                                    repo.getPermission()
                                }

                                when (permissions) {
                                    is Resource.Success -> {
                                        SessionPermissions.replaceAll(permissions.data)
                                        completeLogin()
                                    }
                                    is Resource.Loading -> Unit
                                    is Resource.Error -> {
                                        val msg = permissions.causes
                                            ?: permissions.exception?.message
                                            ?: "Не удалось загрузить права доступа"
                                        _uiState.value = LoginUiState.Error(msg)
                                    }
                                }


                            } else {
                                _uiState.value = LoginUiState.Error("Пустой токен от сервера")
                            }
                        }
                    }
                    is Resource.Error -> {
                        val msg = res.causes ?: res.exception?.message ?: "Ошибка авторизации"
                        tokenRefreshAttemptedThisLogin = false
                        withContext(Dispatchers.Main.immediate) {
                            _uiState.value = LoginUiState.Error(msg)
                        }
                    }
                    Resource.Loading -> {
                        // no-op: already set to Loading on MAIN
                    }
                }
            } catch (t: Throwable) {
                tokenRefreshAttemptedThisLogin = false
                withContext(Dispatchers.Main.immediate) {
                    _uiState.value = LoginUiState.Error(t.message ?: "Ошибка авторизации")
                }
            }
        }
    }

    private fun completeLogin() {
        _uiState.value = LoginUiState.Idle
        appScope.launch {
            bootstrapCoreSessionAndStartHeartbeat()
        }
        PushRegistrationCoordinator.registerIfReady(preferredPlatform = pushPlatformId())

        onLoginSuccess() // navigate (must be MAIN)
    }

    private suspend fun bootstrapCoreSessionAndStartHeartbeat() {
        val fullName = appSettings.getStringOrNull(AppSettingsKeys.PERSONAL_DATA).orEmpty().trim()
        val fcmToken = appSettings.getStringOrNull(AppSettingsKeys.FCM_TOKEN).orEmpty().trim()
        if (fullName.isBlank() || fcmToken.isBlank()) {
            println("CoreSession: bootstrap skipped (missing_user_or_fcm)")
            return
        }

        val req = com.tagaev.trrcrm.models.CoreSessionBootstrapRequest(
            full_name = fullName,
            platform = pushPlatformId(),
            device_id = com.tagaev.trrcrm.getPlatform().deviceSpecificInfo,
            fcm_token = fcmToken,
            login = appSettings.getStringOrNull(AppSettingsKeys.EMAIL),
            department = appSettings.getStringOrNull(AppSettingsKeys.DEPARTMENT),
            device_name = com.tagaev.trrcrm.getPlatform().name,
            app_version = Secrets.VERSION,
        )

        when (val res = repo.coreSessionBootstrap(req)) {
            is Resource.Success -> {
                appSettings.setString(AppSettingsKeys.CORE_SESSION_ID, res.data.sessionId)
                startCoreHeartbeatLoop()
            }
            is Resource.Error -> {
                println("CoreSession: bootstrap failed ${res.causes ?: res.exception?.message}")
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
                val heartbeatReq = com.tagaev.trrcrm.models.CoreSessionHeartbeatRequest(
                    sessionId = sessionId,
                    fcmToken = appSettings.getStringOrNull(AppSettingsKeys.FCM_TOKEN),
                    appVersion = Secrets.VERSION
                )
                when (val hb = repo.coreSessionHeartbeat(heartbeatReq)) {
                    is Resource.Success -> Unit
                    is Resource.Error -> println("CoreSession: heartbeat failed ${hb.causes ?: hb.exception?.message}")
                    is Resource.Loading -> Unit
                }
            }
        }
    }

    override fun onLoginWithToken(token: String) {
        if (_uiState.value is LoginUiState.Loading) return
        appScope.launch {
            withContext(Dispatchers.Main.immediate) {
                _uiState.value = LoginUiState.Loading
            }
            try {
                appSettings.setString(AppSettingsKeys.TOKEN_KEY, token)
                runCatching { apiConfig.token = token }
                SessionPermissions.clear()
                val permissions = withContext(Dispatchers.Default) { repo.getPermission() }
                withContext(Dispatchers.Main.immediate) {
                    when (permissions) {
                        is Resource.Success -> {
                            tokenRefreshAttemptedThisLogin = false
                            SessionPermissions.replaceAll(permissions.data)
                            completeLogin()
                        }
                        is Resource.Loading -> Unit
                        is Resource.Error -> {
                            val msg = permissions.causes
                                ?: permissions.exception?.message
                                ?: "Не удалось загрузить права доступа"
                            val shouldFallbackToCredentials = !tokenRefreshAttemptedThisLogin &&
                                isTokenAuthenticationError(msg) &&
                                hasSavedCredentials()
                            if (shouldFallbackToCredentials) {
                                tokenRefreshAttemptedThisLogin = true
                                val savedUser = appSettings.getString(AppSettingsKeys.EMAIL, defaultValue = "")
                                val savedPassHash = appSettings.getString(AppSettingsKeys.PASS, defaultValue = "")
                                println("Saved token rejected, requesting fresh token by credentials")
                                _uiState.value = LoginUiState.Idle
                                onLoginWithCredentials(savedUser, savedPassHash)
                            } else {
                                tokenRefreshAttemptedThisLogin = false
                                _uiState.value = LoginUiState.Error(msg)
                            }
                        }
                    }
                }
            } catch (t: Throwable) {
                withContext(Dispatchers.Main.immediate) {
                    tokenRefreshAttemptedThisLogin = false
                    _uiState.value = LoginUiState.Error(t.message ?: "Ошибка авторизации")
                }
            }
        }
    }


    //TODO test request get token by login and password

    override fun back() = onBack()
}
