package com.tagaev.trrcrm.ui.i18n

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ProvidedValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.text.intl.Locale

actual object LocalAppLocale {
    private val localAppLocale = staticCompositionLocalOf { Locale.current.toString() }

    actual val current: String
        @Composable get() = localAppLocale.current

    @Composable
    actual infix fun provides(value: String?): ProvidedValue<*> {
        // wasm target is currently disabled; keep a no-op-compatible provider.
        return localAppLocale.provides(value ?: Locale.current.toString())
    }
}
