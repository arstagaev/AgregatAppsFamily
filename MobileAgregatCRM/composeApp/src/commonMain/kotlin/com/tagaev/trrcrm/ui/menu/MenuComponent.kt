package com.tagaev.trrcrm.ui.menu

import com.arkivanov.decompose.ComponentContext
import com.tagaev.trrcrm.updates.DesktopUpdateService
import com.tagaev.trrcrm.updates.DesktopUpdateUiState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject


interface IMenuComponent {
    val desktopUpdateState: StateFlow<DesktopUpdateUiState>
    fun back()
    fun openCargo()
    fun openCatalog()
    fun openSettings()
    fun checkForDesktopUpdate()
    fun installDesktopUpdate()
    fun dismissDesktopUpdate()
    fun clearDesktopUpdateError()
}

class MenuComponent(
    componentContext: ComponentContext,
    private val onBack: () -> Unit,
    private val onCargo: () -> Unit,
    private val onCatalog: () -> Unit,
    private val onSettings: () -> Unit
) : IMenuComponent, ComponentContext by componentContext, KoinComponent {
    private val appScope: CoroutineScope by inject()
    private val desktopUpdateService: DesktopUpdateService by inject()

    override val desktopUpdateState: StateFlow<DesktopUpdateUiState> = desktopUpdateService.state

    override fun back() = onBack()

    override fun openCargo() {
        onCargo.invoke()
    }

    override fun openCatalog() {
        onCatalog.invoke()
    }

    override fun openSettings() {
        onSettings.invoke()
    }

    override fun checkForDesktopUpdate() {
        appScope.launch { desktopUpdateService.checkForUpdates(manual = true) }
    }

    override fun installDesktopUpdate() {
        appScope.launch { desktopUpdateService.installAvailableUpdate() }
    }

    override fun dismissDesktopUpdate() {
        desktopUpdateService.dismissAvailableUpdate()
    }

    override fun clearDesktopUpdateError() {
        desktopUpdateService.clearError()
    }

}
