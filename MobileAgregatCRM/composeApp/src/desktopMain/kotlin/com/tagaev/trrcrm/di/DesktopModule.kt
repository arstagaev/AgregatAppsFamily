package com.tagaev.trrcrm.di

import com.russhwolf.settings.Settings
import com.tagaev.trrcrm.data.db.DriverFactory
import com.tagaev.trrcrm.data.remote.ApiConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.koin.core.context.startKoin
import org.koin.dsl.module

private val desktopModule = module {
    single { DriverFactory() }
    single { ApiConfig(token = "NULL") }
    single { CoroutineScope(SupervisorJob() + Dispatchers.Default) }
    single<Settings> { Settings() }
}

private var koinStarted = false

fun initKoinDesktop() {
    if (koinStarted) return
    koinStarted = true
    startKoin { modules(commonModule, desktopModule) }
}
