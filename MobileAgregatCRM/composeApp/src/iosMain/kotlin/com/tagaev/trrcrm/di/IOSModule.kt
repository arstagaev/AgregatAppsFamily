package com.tagaev.trrcrm.di

import com.russhwolf.settings.NSUserDefaultsSettings
import com.russhwolf.settings.Settings
import com.tagaev.trrcrm.data.db.DriverFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.koin.core.KoinApplication
import org.koin.core.context.startKoin
import org.koin.dsl.module
import platform.Foundation.NSUserDefaults

val iosModule = module {
    single { DriverFactory() }
    single { CoroutineScope(SupervisorJob() + Dispatchers.Default) }

    single<Settings> {
        NSUserDefaultsSettings(NSUserDefaults.standardUserDefaults())
    }
}

fun initKoinIos(): KoinApplication = startKoin { modules(commonModule, iosModule) }
