package com.tagaev.mobileagregatcrm.ui.style

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

@Composable
actual fun platformColorScheme(mode: ThemeMode): ColorScheme? {
    val ctx = LocalContext.current
    val isDark = when (mode) {
        ThemeMode.Light  -> false
        ThemeMode.Dark   -> true
        ThemeMode.System -> isSystemInDarkTheme()
    }
    val dynamic = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    return when {
        dynamic && isDark  -> dynamicDarkColorScheme(ctx)
        dynamic && !isDark -> dynamicLightColorScheme(ctx)
        isDark             -> elegantDarkColors()
        else               -> elegantLightColors()
    }
}