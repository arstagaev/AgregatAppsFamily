package com.tagaev.trrcrm.ui.i18n

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ProvidedValue
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import org.koin.compose.koinInject

/**
 * In-app locale override for Compose Multiplatform resources.
 * See: https://kotlinlang.org/docs/multiplatform/compose-resource-environment.html
 */
expect object LocalAppLocale {
    val current: String
        @Composable get

    @Composable
    infix fun provides(value: String?): ProvidedValue<*>
}

@Composable
fun AppLocaleProvider(
    controller: LanguageController = koinInject(),
    content: @Composable () -> Unit,
) {
    val language by controller.language.collectAsState()
    CompositionLocalProvider(
        LocalAppLocale provides language.code,
    ) {
        key(language.code) {
            content()
        }
    }
}
