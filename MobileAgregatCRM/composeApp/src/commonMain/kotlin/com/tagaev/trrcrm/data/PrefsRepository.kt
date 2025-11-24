package com.tagaev.trrcrm.data


import com.russhwolf.settings.Settings

class PrefsRepository(private val settings: Settings) {
    fun getBool(key: String, def: Boolean) = settings.getBooleanOrNull(key) ?: def
    fun putBool(key: String, v: Boolean) = settings.putBoolean(key, v)
    fun getInt(key: String, def: Int) = settings.getIntOrNull(key) ?: def
    fun putInt(key: String, v: Int) = settings.putInt(key, v)
    fun getString(key: String, def: String) = settings.getStringOrNull(key) ?: def
    fun putString(key: String, v: String) = settings.putString(key, v)
}