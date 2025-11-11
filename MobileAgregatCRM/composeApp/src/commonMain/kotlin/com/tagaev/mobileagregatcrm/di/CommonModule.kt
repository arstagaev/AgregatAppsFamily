package com.tagaev.mobileagregatcrm.di

import com.agregat.db.AppDatabase
import com.russhwolf.settings.Settings
import com.tagaev.mobileagregatcrm.data.AppSettings
import com.tagaev.mobileagregatcrm.data.EventsRepository
import com.tagaev.mobileagregatcrm.data.FavoritesRepository
import com.tagaev.mobileagregatcrm.data.db.DriverFactory
import com.tagaev.mobileagregatcrm.data.remote.ApiConfig
import com.tagaev.mobileagregatcrm.data.remote.EventsApi
import com.tagaev.mobileagregatcrm.ui.style.ThemeController
import org.koin.dsl.module
import org.koin.core.qualifier.named
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.serialization.json.Json

/**
 * Common Koin module (shared across Android & iOS).
 *
 * Platform modules must also provide:
 *  - DriverFactory (Android/iOS actuals)
 *  - Optionally override ApiConfig (baseUrl/token)
 */
val commonModule = module {

    // App scope
    single<CoroutineScope> { CoroutineScope(SupervisorJob() + Dispatchers.Default) }

    // --- HTTP / API layer ---
    single { ApiConfig(token = "NULL") }
    single { EventsApi() }

    // --- Repositories ---
    single { EventsRepository(api = get(), cfg = get()) }

    // --- Database (SQLDelight) ---
    single { AppDatabase(get<DriverFactory>().createDriver()) }
    single { get<AppDatabase>().favoritesQueries }
    single { FavoritesRepository(get()) }

    // --- Settings / JSON ---
    single<Json> { Json { ignoreUnknownKeys = true; isLenient = true; explicitNulls = false } }
    single { AppSettings(get<Settings>(), get<Json>()) }

    // --- Theme ---
    single { ThemeController(get()) }
}