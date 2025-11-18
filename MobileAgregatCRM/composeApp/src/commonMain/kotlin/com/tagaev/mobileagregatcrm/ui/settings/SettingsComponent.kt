package com.tagaev.mobileagregatcrm.ui.settings

import com.arkivanov.decompose.ComponentContext
import com.tagaev.mobileagregatcrm.data.db.EventsCacheStore
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

interface ISettingsComponent {
    fun onWriteToDeveloper()
    fun onLogout()
    fun back()
}

class SettingsComponent(
    componentContext: ComponentContext,
    private val onBack: () -> Unit,
    private val onWriteToDeveloperAction: () -> Unit = {},
    private val onLogoutAction: () -> Unit
) : ISettingsComponent, KoinComponent, ComponentContext by componentContext {
    private val eventsCacheStore: EventsCacheStore by inject()

    override fun onWriteToDeveloper() {
        onWriteToDeveloperAction()
    }

    override fun onLogout() {
        eventsCacheStore.clearAll()
        onLogoutAction.invoke()
    }

    override fun back() = onBack()
}