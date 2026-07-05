package com.tagaev.trrcrm.navigation

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import com.tagaev.trrcrm.data.AppSettings

object BottomNavLayoutState {
    private val _layout: MutableState<List<BottomNavLayoutItem>> =
        mutableStateOf(BottomNavLayoutResolver.defaultLayout())
    val state: MutableState<List<BottomNavLayoutItem>> get() = _layout

    fun loadFrom(settings: AppSettings) {
        val saved = settings.loadBottomNavLayout()
        _layout.value = BottomNavLayoutResolver.mergeLayout(saved)
    }

    fun applySaved(items: List<BottomNavLayoutItem>) {
        _layout.value = BottomNavLayoutResolver.mergeLayout(items)
    }

    fun savedLayout(): List<BottomNavLayoutItem> = _layout.value

    fun visibleTabs(): List<BottomNavItemId> =
        BottomNavLayoutResolver.resolveVisibleTabs(_layout.value)
}
