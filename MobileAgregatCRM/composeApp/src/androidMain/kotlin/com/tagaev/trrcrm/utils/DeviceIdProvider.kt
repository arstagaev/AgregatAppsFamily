package com.tagaev.trrcrm.utils

import android.content.Context
import java.util.UUID

class DeviceIdProvider(context: Context) {

    private val prefs = context.getSharedPreferences("device_info", Context.MODE_PRIVATE)

    val deviceId: String by lazy {
        val existing = prefs.getString("device_id", null)
        if (existing != null) existing
        else {
            val newId = UUID.randomUUID().toString()
            prefs.edit().putString("device_id", newId).apply()
            newId
        }
    }
}