package com.tagaev.mobileagregatcrm.di

import com.agregat.db.AppDatabase
import com.russhwolf.settings.Settings
import com.tagaev.mobileagregatcrm.data.AppSettings
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

    // Multiplatform Settings (russhwolf) – used directly in UI/logic
//    single { Settings }
//

    // App-wide scope for background jobs from components (e.g., network calls)
    single<CoroutineScope> { CoroutineScope(SupervisorJob() + Dispatchers.Default) }

    // --- HTTP / API layer ----------------------------------------------------
    // Provide API config; platform can override with real values:
    single {
        // If you set named("ApiBaseUrl") / named("ApiToken") elsewhere, they will be picked up here.
//        val baseUrl = getOrNull<String>(named("ApiBaseUrl")) ?: ""
//        val token = getOrNull<String>(named("ApiToken")) ?: ""
        ApiConfig()
    }
    single { EventsApi() }

    // --- Database layer (SQLDelight) ----------------------------------------
    // DriverFactory is expected from platform module (android/ios)
    single { AppDatabase(get<DriverFactory>().createDriver()) }
    single { get<AppDatabase>().favoritesQueries }
    single { FavoritesRepository(get()) }

    single<Json> {
        Json {
            ignoreUnknownKeys = true
            isLenient = true
            explicitNulls = false
        }
    }

    single { AppSettings(get<Settings>(), get<Json>()) }

    single { ThemeController(get()) }  // requires AppSettings in DI
}