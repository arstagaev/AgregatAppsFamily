package com.tagaev.trrcrm.developer

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import com.tagaev.trrcrm.data.AppSettings
import com.tagaev.trrcrm.data.AppSettingsKeys

object DeveloperModeState {
    private val _enabled: MutableState<Boolean> = mutableStateOf(false)
    val enabled: MutableState<Boolean> get() = _enabled

    fun loadFrom(settings: AppSettings) {
        _enabled.value = settings.getBool(AppSettingsKeys.DEVELOPER_MODE_ENABLED, false)
    }

    fun toggle(settings: AppSettings): Boolean {
        val next = !_enabled.value
        _enabled.value = next
        settings.setBool(AppSettingsKeys.DEVELOPER_MODE_ENABLED, next)
        return next
    }
}
