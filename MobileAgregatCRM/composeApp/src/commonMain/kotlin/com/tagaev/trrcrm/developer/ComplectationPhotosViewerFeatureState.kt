package com.tagaev.trrcrm.developer

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import com.tagaev.trrcrm.data.AppSettings
import com.tagaev.trrcrm.data.AppSettingsKeys

object ComplectationPhotosViewerFeatureState {
    private val _enabled: MutableState<Boolean> = mutableStateOf(false)
    val enabled: MutableState<Boolean> get() = _enabled

    fun loadFrom(settings: AppSettings) {
        _enabled.value = settings.getBool(AppSettingsKeys.DEV_COMPLECTATION_PHOTOS_VIEWER_ENABLED, false)
    }

    fun setEnabled(settings: AppSettings, value: Boolean) {
        _enabled.value = value
        settings.setBool(AppSettingsKeys.DEV_COMPLECTATION_PHOTOS_VIEWER_ENABLED, value)
    }

    fun isEnabled(settings: AppSettings): Boolean =
        settings.getBool(AppSettingsKeys.DEV_COMPLECTATION_PHOTOS_VIEWER_ENABLED, false)
}
