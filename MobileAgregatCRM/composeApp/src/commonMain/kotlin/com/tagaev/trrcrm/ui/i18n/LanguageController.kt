package com.tagaev.trrcrm.ui.i18n

import com.tagaev.trrcrm.data.AppSettings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class AppLanguage(val code: String, val displayName: String) {
    Russian("ru", "Русский"),
    Uzbek("uz", "Oʻzbekcha"),
    ;

    companion object {
        fun fromCode(code: String?): AppLanguage =
            entries.firstOrNull { it.code.equals(code, ignoreCase = true) } ?: Russian
    }
}

class LanguageController(private val settings: AppSettings) {
    companion object {
        const val KEY_APP_LANGUAGE = "APP_LANGUAGE"
    }

    private fun readLanguage(): AppLanguage =
        AppLanguage.fromCode(settings.getString(KEY_APP_LANGUAGE, AppLanguage.Russian.code))

    private fun writeLanguage(language: AppLanguage) {
        settings.setString(KEY_APP_LANGUAGE, language.code)
    }

    private val _language = MutableStateFlow(readLanguage().also { AppLanguageHolder.current = it })
    val language: StateFlow<AppLanguage> = _language.asStateFlow()

    fun setLanguage(newLanguage: AppLanguage) {
        if (_language.value == newLanguage) return
        AppLanguageHolder.current = newLanguage
        _language.value = newLanguage
        writeLanguage(newLanguage)
    }
}
