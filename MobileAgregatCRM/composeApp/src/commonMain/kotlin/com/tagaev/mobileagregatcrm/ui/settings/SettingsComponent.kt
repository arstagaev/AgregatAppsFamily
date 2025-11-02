package com.tagaev.mobileagregatcrm.ui.settings

import com.arkivanov.decompose.ComponentContext

interface ISettingsComponent {
    fun onWriteToDeveloper()
    fun onLogout()
    fun back()
}

class SettingsComponent(
    componentContext: ComponentContext,
    private val onBack: () -> Unit,
//    private val onWriteToDeveloperAction: () -> Unit,
    private val onLogoutAction: () -> Unit
) : ISettingsComponent, ComponentContext by componentContext {

    override fun onWriteToDeveloper() {

    }

    override fun onLogout() = onLogoutAction()

    override fun back() = onBack()
}