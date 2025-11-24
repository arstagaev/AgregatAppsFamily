package com.tagaev.trrcrm.di

import android.content.Context
import com.russhwolf.settings.Settings
import com.russhwolf.settings.SharedPreferencesSettings
import com.tagaev.trrcrm.data.db.DriverFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val androidModule = module {
    single { DriverFactory(androidContext()) }
    single { CoroutineScope(SupervisorJob() + Dispatchers.Default) } // for provideEventsController

    single<Settings> {
        val ctx = androidContext()
        val prefs = ctx.getSharedPreferences("app_settings", Context.MODE_PRIVATE)
        SharedPreferencesSettings(prefs, commit = false)
    }
}