package com.tagaev.mobileagregatcrm.ui.style

import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.Composable

@Composable
actual fun platformColorScheme(mode: ThemeMode): ColorScheme? = when (mode) {
    ThemeMode.Light  -> elegantLightColors()
    ThemeMode.Dark   -> elegantDarkColors()
    ThemeMode.System -> null // let common fallback decide
}