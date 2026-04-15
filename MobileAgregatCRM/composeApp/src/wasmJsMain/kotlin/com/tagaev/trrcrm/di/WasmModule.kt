package com.tagaev.trrcrm.di

import com.russhwolf.settings.Settings
import com.russhwolf.settings.StorageSettings
import com.tagaev.trrcrm.data.db.DriverFactory
import com.tagaev.trrcrm.data.remote.ApiConfig
import org.koin.core.context.startKoin
import org.koin.dsl.module

private val wasmModule = module {
    single<Settings> { StorageSettings() }
    single { DriverFactory() }
    // Use /api (without trailing slash) for better compatibility with front proxies
    // that define only exact "/api" routing.
    single { ApiConfig(baseUrl = "/api", token = "NULL") }
}

private var koinStarted = false

fun initKoinWasm() {
    if (koinStarted) return
    koinStarted = true
    startKoin { modules(commonModule, wasmModule) }
}
