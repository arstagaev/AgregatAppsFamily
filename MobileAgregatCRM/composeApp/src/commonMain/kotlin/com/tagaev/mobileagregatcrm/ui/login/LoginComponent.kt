package com.tagaev.mobileagregatcrm.ui.login

import com.arkivanov.decompose.ComponentContext
import com.tagaev.mobileagregatcrm.data.AppSettings
import com.tagaev.mobileagregatcrm.data.remote.ApiConfig
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

/**
 * Contract used by LoginScreen.
 * Implemented by a Decompose component that saves creds/token and navigates onward.
 */
interface ILoginComponent {
    fun onLoginWithCredentials(user: String, pass: String)
    fun onLoginWithToken(token: String)
    fun back()
}

class LoginComponent(
    componentContext: ComponentContext,
    private val onLoginSuccess: () -> Unit,
    private val onBack: () -> Unit
) : ILoginComponent, ComponentContext by componentContext, KoinComponent {

    private val settings: AppSettings by inject()
    private val apiConfig: ApiConfig by inject()

    override fun onLoginWithCredentials(user: String, pass: String) {
        // Persist credentials if your backend needs them (optional)
        settings.setString("API_LOGIN", user)
        settings.setString("API_PASSWORD", pass)
        // If your flow exchanges creds for a token, do it here.
        // For now, proceed to main after persisting.
        onLoginSuccess()
    }

    override fun onLoginWithToken(token: String) {
        // Persist and update runtime config so API starts using it immediately
        settings.setString("API_TOKEN", token)
        runCatching { apiConfig.token = token }
        onLoginSuccess()
    }

    //TODO test request get token by login and password

    override fun back() = onBack()
}