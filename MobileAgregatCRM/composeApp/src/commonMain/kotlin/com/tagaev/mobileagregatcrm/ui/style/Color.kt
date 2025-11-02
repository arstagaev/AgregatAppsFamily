package com.tagaev.mobileagregatcrm.ui.style

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

// Brand: refined indigo/blue with teal accents; neutral, low-contrast surfaces.

fun elegantLightColors(): ColorScheme = lightColorScheme(
    primary = Color(0xFF546FF3),       // brand indigo-blue
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFE5EAFF),
    onPrimaryContainer = Color(0xFF0C163F),

    secondary = Color(0xFF6E8BFF),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFE7EBFF),
    onSecondaryContainer = Color(0xFF0D1B49),

    tertiary = Color(0xFF00BFA6),      // teal accent
    onTertiary = Color(0xFF001A16),
    tertiaryContainer = Color(0xFFCFF8F1),
    onTertiaryContainer = Color(0xFF002923),

    background = Color(0xFFF7F8FC),
    onBackground = Color(0xFF0F1320),
    surface = Color(0xFFF8F9FF),
    onSurface = Color(0xFF12151E),

    surfaceVariant = Color(0xFFE7EAF6),
    onSurfaceVariant = Color(0xFF444B66),
    outline = Color(0xFFB9C1EA),

    error = Color(0xFFBA1A1A),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002)
)

fun elegantDarkColors(): ColorScheme = darkColorScheme(
    primary = Color(0xFF9FB1FF),
    onPrimary = Color(0xFF0A102A),
    primaryContainer = Color(0xFF2A3B86),
    onPrimaryContainer = Color(0xFFE5EAFF),

    secondary = Color(0xFFAEC2FF),
    onSecondary = Color(0xFF0A1436),
    secondaryContainer = Color(0xFF2B3D87),
    onSecondaryContainer = Color(0xFFE7EBFF),

    tertiary = Color(0xFF4DDACB),
    onTertiary = Color(0xFF00201B),
    tertiaryContainer = Color(0xFF005048),
    onTertiaryContainer = Color(0xFFCFF8F1),

    background = Color(0xFF0B0D16),
    onBackground = Color(0xFFE6E9F9),
    surface = Color(0xFF0E101A),
    onSurface = Color(0xFFE4E7F7),

    surfaceVariant = Color(0xFF2C314A),
    onSurfaceVariant = Color(0xFFC3CAE7),
    outline = Color(0xFF495073),

    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6)
)