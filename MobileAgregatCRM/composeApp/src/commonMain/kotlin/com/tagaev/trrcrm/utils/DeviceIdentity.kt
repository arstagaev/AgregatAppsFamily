package com.tagaev.trrcrm.utils

import com.tagaev.trrcrm.data.AppSettings
import com.tagaev.trrcrm.data.AppSettingsKeys
import com.tagaev.trrcrm.getPlatform
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

object DeviceIdentity : KoinComponent {
    private val settings: AppSettings by inject()

    fun stableDeviceId(): String {
        val saved = settings.getStringOrNull(AppSettingsKeys.STABLE_DEVICE_ID).orEmpty().trim()
        if (saved.isNotBlank()) return saved

        val generated = getPlatform().deviceSpecificInfo.trim()
        if (generated.isNotBlank()) {
            settings.setString(AppSettingsKeys.STABLE_DEVICE_ID, generated)
            return generated
        }

        return generated
    }
}
