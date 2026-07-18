package com.tagaev.trrcrm.ui.i18n

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject

/**
 * Process-wide language snapshot for non-Compose callers ([tr]/[s]).
 * Updated by [LanguageController]; UI also recomposes via [AppLocaleProvider].
 */
object AppLanguageHolder {
    var current: AppLanguage = AppLanguage.Russian
}

/**
 * Resolve a catalog string by key. Safe from Compose and non-Compose code.
 * UI still refreshes on language change via [AppLocaleProvider] `key(language)`.
 */
fun s(key: String, vararg args: Any): String = tr(key, *args)

@Composable
fun appString(resource: StringResource): String = stringResource(resource)

@Composable
fun appString(resource: StringResource, vararg formatArgs: Any): String =
    stringResource(resource, *formatArgs)

/** Non-composable lookup by resource key name. */
fun tr(key: String, vararg args: Any): String =
    StringCatalog.get(key, AppLanguageHolder.current, *args)

@Composable
fun rememberAppLanguage(): AppLanguage {
    val controller = koinInject<LanguageController>()
    val language by controller.language.collectAsState()
    return language
}
