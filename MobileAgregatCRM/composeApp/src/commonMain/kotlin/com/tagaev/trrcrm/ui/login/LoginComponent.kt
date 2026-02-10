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
import com.tagaev.trrcrm.getPlatform
import com.tagaev.trrcrm.push.PushRegistration
import com.tagaev.trrcrm.utils.AVAILABLE_ROLES
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import okio.ByteString.Companion.encodeUtf8
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
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
    }

    private val appSettings: AppSettings by inject()
    private val apiConfig: ApiConfig by inject()
    private val repo: MainRepository by inject()
    private val appScope: CoroutineScope by inject()
    private val mutex = kotlinx.coroutines.sync.Mutex()

    private val backCallback = BackCallback { /* NO HANDLE */ }

    private val _uiState = MutableStateFlow<LoginUiState>(LoginUiState.Idle)
    override val uiState: StateFlow<LoginUiState> = _uiState

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
            val probe = withContext(Dispatchers.IO) { repo.probeStartup() }
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
        val hasSavedCredentials =
            !appSettings.getStringOrNull(AppSettingsKeys.EMAIL).isNullOrEmpty() &&
                    !appSettings.getStringOrNull(AppSettingsKeys.PASS).isNullOrEmpty()

        withContext(Dispatchers.Main.immediate) {
            _uiState.value = LoginUiState.Idle
        }

        if (hasSavedCredentials) {
            onLoginWithCredentials(
                user = appSettings.getString(AppSettingsKeys.EMAIL, defaultValue = ""),
                pass = appSettings.getString(AppSettingsKeys.PASS, defaultValue = "")
            )
        } else {
            println("Email or Token is empty")
        }
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
                val res = withContext(Dispatchers.IO) {
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

                                AVAILABLE_ROLES.clear()
                                val roles = repo.getRole()

                                when (roles) {
                                    is Resource.Success -> {
                                        AVAILABLE_ROLES = roles.data.roles.toMutableList()
                                        println("Current roles: ${AVAILABLE_ROLES.joinToString()}")
                                        completeLogin()
                                    }
                                    is Resource.Loading -> {

                                    }
                                    is Resource.Error -> {
                                        completeLogin()
                                    }
                                }


                            } else {
                                _uiState.value = LoginUiState.Error("Пустой токен от сервера")
                            }
                        }
                    }
                    is Resource.Error -> {
                        val msg = res.causes ?: res.exception?.message ?: "Ошибка авторизации"
                        withContext(Dispatchers.Main.immediate) {
                            _uiState.value = LoginUiState.Error(msg)
                        }
                    }
                    Resource.Loading -> {
                        // no-op: already set to Loading on MAIN
                    }
                }
            } catch (t: Throwable) {
                withContext(Dispatchers.Main.immediate) {
                    _uiState.value = LoginUiState.Error(t.message ?: "Ошибка авторизации")
                }
            }
        }
    }

    private fun completeLogin() {
        _uiState.value = LoginUiState.Idle
        val token = appSettings.getStringOrNull(AppSettingsKeys.FCM_TOKEN)
        val personalData = appSettings.getStringOrNull(AppSettingsKeys.PERSONAL_DATA)

        if (token.isNullOrBlank() || personalData.isNullOrBlank()) {
            println("FCM token or personalData is empty, skip register $token / $personalData")

        } else {
            println("Register FCM token for user $personalData")
            PushRegistration.registerCurrentUserToken(
                fullName = personalData,
                platform = "${getPlatform()}",
                token = token
            )
        }

        onLoginSuccess() // navigate (must be MAIN)
    }

    override fun onLoginWithToken(token: String) {
        // Persist and update runtime config so API starts using it immediately
        appSettings.setString("API_TOKEN", token)
        runCatching { apiConfig.token = token }
        onLoginSuccess()
    }


    //TODO test request get token by login and password

    override fun back() = onBack()
}
