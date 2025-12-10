package com.tagaev.trrcrm.di

//import com.agregat.db.AppDatabase
import com.agregat.db.Database
import com.russhwolf.settings.Settings
import com.tagaev.trrcrm.data.AppSettings
import com.tagaev.trrcrm.data.MainRepository
import com.tagaev.trrcrm.data.db.EventsCacheStore
import com.tagaev.trrcrm.data.db.FavoritesStore
import com.tagaev.trrcrm.data.db.createDatabase
import com.tagaev.trrcrm.data.remote.ApiConfig
import com.tagaev.trrcrm.data.remote.EventsApi
import com.tagaev.trrcrm.data.remote.HttpClientFactory
import com.tagaev.trrcrm.ui.style.ThemeController
import io.ktor.client.HttpClient
import org.koin.dsl.module
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
    single<HttpClient> {
        HttpClientFactory.create()  // your existing factory
    }
    // --- HTTP / API layer ---
    single { ApiConfig(token = "NULL") }
    single<EventsApi> {
        EventsApi(
            client = get()   // get<HttpClient>()
        )
    }
//    single { EventsApi() }

    // --- Repositories ---
    single { MainRepository(api = get(), cfg = get()) }

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