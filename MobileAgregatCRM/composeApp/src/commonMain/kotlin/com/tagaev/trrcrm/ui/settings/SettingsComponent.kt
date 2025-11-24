package com.tagaev.trrcrm.ui.settings

import com.arkivanov.decompose.ComponentContext
import com.tagaev.trrcrm.data.AppSettings
import com.tagaev.trrcrm.data.db.EventsCacheStore
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
    private val settings: AppSettings by inject()


    override fun onWriteToDeveloper() {
        onWriteToDeveloperAction()
    }

    override fun onLogout() {
        eventsCacheStore.clearAll()

        settings.clearAll()
//        settings.setString(AppSettingsKeys.WORK_ORDERS_REFINE_STATE,"")
//        settings.setString(AppSettingsKeys.EVENTS_REFINE_STATE,"")

        onLogoutAction.invoke()
    }

    override fun back() = onBack()
}