package com.tagaev.mobileagregatcrm.ui.style

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color

/**
 * App-wide Material theme that reacts to ThemeController in real time.
 * Pass [monochrome] if you want a grayscale UI (your Settings toggle).
 */
@Composable
fun AppTheme(
    controller: ThemeController,
    monochrome: Boolean = false,
    content: @Composable () -> Unit
) {
    val mode by controller.mode.collectAsState()

    // Prefer platform scheme (Android dynamic colors), otherwise elegant palette
    val base: ColorScheme = platformColorScheme(mode) ?: when (mode) {
        ThemeMode.Light  -> elegantLightColors()
        ThemeMode.Dark   -> elegantDarkColors()
        ThemeMode.System -> elegantLightColors()
    }

    val colors = if (monochrome) base.asMonochrome() else base

    MaterialTheme(
        colorScheme = colors,
        // You can add your custom Typography/Shapes here if you have them
        content = content
    )
}

/* ---- Optional grayscale mapping for your B/W toggle ---- */

private fun gray(c: Color): Color {
    // Luma transform (Rec. 709)
    val y = 0.2126f * c.red + 0.7152f * c.green + 0.0722f * c.blue
    return Color(y, y, y, c.alpha)
}

private fun ColorScheme.asMonochrome(): ColorScheme = lightColorScheme(
    primary              = gray(primary),
    onPrimary            = gray(onPrimary),
    primaryContainer     = gray(primaryContainer),
    onPrimaryContainer   = gray(onPrimaryContainer),

    secondary            = gray(secondary),
    onSecondary          = gray(onSecondary),
    secondaryContainer   = gray(secondaryContainer),
    onSecondaryContainer = gray(onSecondaryContainer),

    tertiary             = gray(tertiary),
    onTertiary           = gray(onTertiary),
    tertiaryContainer    = gray(tertiaryContainer),
    onTertiaryContainer  = gray(onTertiaryContainer),

    background           = gray(background),
    onBackground         = gray(onBackground),
    surface              = gray(surface),
    onSurface            = gray(onSurface),

    surfaceVariant       = gray(surfaceVariant),
    onSurfaceVariant     = gray(onSurfaceVariant),
    outline              = gray(outline),
    outlineVariant       = gray(outlineVariant),

    error                = gray(error),
    onError              = gray(onError),
    errorContainer       = gray(errorContainer),
    onErrorContainer     = gray(onErrorContainer),

    // You can add inverseSurface/primary, scrim, etc. if you use them
)