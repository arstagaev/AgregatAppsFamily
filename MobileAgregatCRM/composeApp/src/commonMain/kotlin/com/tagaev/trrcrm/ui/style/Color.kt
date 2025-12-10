package com.tagaev.trrcrm.ui.style

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

/**
 * Dark / black neumorphism-oriented palette.
 * Elegant, low-saturation accents + soft contrast.
 */
object DefaultColors {

    // === BASE / BACKGROUND ===

    // App background (behind everything)
    val Background = Color(0xFF050509)      // almost-black, slightly bluish

    // Surfaces (cards, panels, sheets)
    val SurfaceLowest = Color(0xFF06070B)   // for very flat areas
    val SurfaceLow    = Color(0xFF080A10)   // default background for cards
    val Surface       = Color(0xFF0C0F17)   // slightly raised
    val SurfaceHigh   = Color(0xFF111521)   // more contrast / top bars

    // Borders for outlines / dividers
    val BorderSubtle  = Color(0xFF171C28)
    val BorderStrong  = Color(0xFF222837)

    // === TEXT ===

    val TextPrimary   = Color(0xFFE8ECF8)   // main text
    val TextSecondary = Color(0xFFB0B6C6)   // secondary labels
    val TextMuted     = Color(0xFF70788A)   // hints, disabled, helper

    // === NEUMORPHIC LIGHT / SHADOW (for inner/outer shadows) ===

    // Light highlight for top-left edges
    val NeumoHighlight = Color(0x26FFFFFF) // ~15% white

    // Dark shadow for bottom-right edges
    val NeumoShadow    = Color(0x80000000) // 50% black

    // You can use these in shadow/elevation implementations.

    // === ACCENT COLORS (for primary actions, focus, sliders, etc.) ===

    // Main accent (buttons, primary chips)
    val AccentPrimary   = Color(0xFF4C8DFF) // deep muted blue

    // Secondary accent (toggles, links)
    val AccentSecondary = Color(0xFF7B5CFF) // violet / indigo

    // Tertiary accent (small highlights, indicators)
    val AccentTertiary  = Color(0xFFFF9E64) // warm amber/orange

    // For special UI bits (charts, tags)
    val AccentTeal      = Color(0xFF36C5B3)
    val AccentPink      = Color(0xFFFF6FA3)

    // === STATUS / BADGE COLORS ===

    // Success / Done
    val StatusSuccessBg = Color(0xFF1F8F5C)
    val StatusSuccessFg = Color(0xFFE3F7ED)

    // Warning / In progress / Risk
    val StatusWarningBg = Color(0xFFB97A1F)
    val StatusWarningFg = Color(0xFFFFF4DD)

    // Error / Problem
    val StatusErrorBg   = Color(0xFFB63A3A)
    val StatusErrorFg   = Color(0xFFFDECEC)

    // Info / Neutral
    val StatusInfoBg    = Color(0xFF27628F)
    val StatusInfoFg    = Color(0xFFE3F1FD)

    // Muted / Low importance / On hold
    val StatusMutedBg   = Color(0xFF262B38)
    val StatusMutedFg   = Color(0xFFB0B6C6)


    /// RAINBOW

    // Red – deep crimson
    val RainbowRedBg = Color(0xFFB4363C)
    val RainbowRedFg = Color(0xFFFF8D95)

    // Orange – warm but not neon
    val RainbowOrangeBg = Color(0xFFCB7A2A)
    val RainbowOrangeFg = Color(0xFFFFF3E1)

    // Yellow – golden / amber, softened
    val RainbowYellowBg = Color(0xFFD2A52A)
    val RainbowYellowFg = Color(0xFFFFF7D9)

    // Green – emerald / teal-ish, not acidic
    val RainbowGreenBg = Color(0xFF2C9B61)
    val RainbowGreenFg = Color(0xFFE3F7EE)

    // Blue – rich, slightly desaturated
    val RainbowBlueBg = Color(0xFF3472C9)
    val RainbowBlueFg = Color(0xFFE3F0FF)

    // Indigo – deep blue-violet
    val RainbowIndigoBg = Color(0xFF4642C5)
    val RainbowIndigoFg = Color(0xFFE7E5FF)

    // Violet – softer magenta-violet accent
    val RainbowVioletBg = Color(0xFF9C4CCB)
    val RainbowVioletFg = Color(0xFFF7E6FF)
}
