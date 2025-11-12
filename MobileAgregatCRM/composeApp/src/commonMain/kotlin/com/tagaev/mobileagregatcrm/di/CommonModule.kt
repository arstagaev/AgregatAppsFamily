package com.tagaev.mobileagregatcrm.di

//import com.agregat.db.AppDatabase
import com.agregat.db.Database
import com.russhwolf.settings.Settings
import com.tagaev.mobileagregatcrm.data.AppSettings
import com.tagaev.mobileagregatcrm.data.EventsRepository
import com.tagaev.mobileagregatcrm.data.db.DriverFactory
import com.tagaev.mobileagregatcrm.data.db.EventsCacheStore
import com.tagaev.mobileagregatcrm.data.db.FavoritesStore
import com.tagaev.mobileagregatcrm.data.db.createDatabase
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

//    single { get<Database>().events_cacheQueries }
//    single { EventsCacheStore(get(), get()) } // needs Json too

    // --- Database (SQLDelight) ---
    // Provide Database
    single<Database> { createDatabase(get()) }

    // Provide queries
    factory { get<Database>().events_cacheQueries }
    factory { get<Database>().favoritesQueries }

    // Stores
    single { EventsCacheStore(get(), get()) }
    single { FavoritesStore(get()) }

//    single { AppDatabase(get<DriverFactory>().createDriver()) }
//    single { get<AppDatabase>().favoritesQueries }

//    single { FavoritesRepository(get()) }

    // --- Settings / JSON ---
    single<Json> { Json { ignoreUnknownKeys = true; isLenient = true; explicitNulls = false } }
    single { AppSettings(get<Settings>(), get<Json>()) }

    // --- Theme ---
    single { ThemeController(get()) }
}