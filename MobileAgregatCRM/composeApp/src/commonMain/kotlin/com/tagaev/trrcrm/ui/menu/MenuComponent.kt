package com.tagaev.trrcrm.ui.menu

import com.arkivanov.decompose.ComponentContext


interface IMenuComponent {
    fun back()
    fun openCargo()
    fun openSettings()
}

class MenuComponent(
    componentContext: ComponentContext,
    private val onBack: () -> Unit,
    private val onCargo: () -> Unit,
    private val onSettings: () -> Unit
) : IMenuComponent, ComponentContext by componentContext {

    override fun back() = onBack()

    override fun openCargo() {
        onCargo.invoke()
    }

    override fun openSettings() {
        onSettings.invoke()
    }

}
