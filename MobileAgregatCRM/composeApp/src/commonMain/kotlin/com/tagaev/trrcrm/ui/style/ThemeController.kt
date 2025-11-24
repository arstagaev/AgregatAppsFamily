package com.tagaev.trrcrm.ui.style

import com.tagaev.trrcrm.data.AppSettings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.Composable

@Composable
expect fun platformColorScheme(mode: ThemeMode): ColorScheme?

enum class ThemeMode { System, Light, Dark }

class ThemeController(private val settings: AppSettings) {
    companion object { private const val KEY_APP_THEME = "APP_THEME" } // system|light|dark

    private fun readMode(): ThemeMode = when (settings.getString(KEY_APP_THEME, "system").lowercase()) {
        "light" -> ThemeMode.Light
        "dark"  -> ThemeMode.Dark
        else    -> ThemeMode.System
    }

    private fun writeMode(mode: ThemeMode) {
        val v = when (mode) {
            ThemeMode.System -> "system"
            ThemeMode.Light  -> "light"
            ThemeMode.Dark   -> "dark"
        }
        settings.setString(KEY_APP_THEME, v)
    }

    private val _mode = MutableStateFlow(readMode())
    val mode: StateFlow<ThemeMode> = _mode

    fun setMode(newMode: ThemeMode) {
        if (_mode.value == newMode) return
        _mode.value = newMode
        writeMode(newMode)
    }
}