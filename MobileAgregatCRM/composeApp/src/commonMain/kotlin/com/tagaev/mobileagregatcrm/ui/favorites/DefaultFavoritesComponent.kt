package com.tagaev.mobileagregatcrm.ui.favorites

import com.arkivanov.decompose.ComponentContext


interface FavoritesComponent {
    fun back()
}

class DefaultFavoritesComponent(
    componentContext: ComponentContext,
    private val onBack: () -> Unit
) : FavoritesComponent, ComponentContext by componentContext {
    override fun back() = onBack()
}