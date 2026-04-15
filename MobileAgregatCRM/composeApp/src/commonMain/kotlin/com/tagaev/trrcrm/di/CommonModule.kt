package com.tagaev.trrcrm.di

//import com.agregat.db.AppDatabase
import com.agregat.db.Database
import com.russhwolf.settings.Settings
import com.tagaev.secrets.Secrets
import com.tagaev.secrets.Secrets.IS_PUBLISH
import com.tagaev.trrcrm.data.AppSettings
import com.tagaev.trrcrm.data.MainRepository
import com.tagaev.trrcrm.data.db.EventsCacheStore
import com.tagaev.trrcrm.data.db.FavoritesStore
import com.tagaev.trrcrm.data.db.createDatabase
import com.tagaev.trrcrm.data.db.isSqlDriverAvailable
import com.tagaev.trrcrm.data.remote.EventsApi
import com.tagaev.trrcrm.data.remote.HttpClientFactory
import com.tagaev.trrcrm.getPlatform
import com.tagaev.trrcrm.push.PushRegistration
import com.tagaev.trrcrm.ui.style.ThemeController
import com.tagaev.trrcrm.utils.DefaultValuesConst.GLOBAL_PUSH_URL
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
    single<EventsApi> {
        EventsApi(
            client = get()   // get<HttpClient>()
        )
    }

//    single { provideHttpClient() } // you already have this

    // configure push registration at startup
    single(createdAtStart = true) {
        val client: HttpClient = get()
        PushRegistration.configure(
            client = client,
            baseUrl = GLOBAL_PUSH_URL,
            apiKey = Secrets.PUSH_API_KEY,
        )
        PushRegistration
    }

    // --- Repositories ---
    single { MainRepository(api = get(), cfg = get()) }

    // --- Database/cache layer ---
    if (isSqlDriverAvailable) {
        single<Database> { createDatabase(get()) }
        factory { get<Database>().events_cacheQueries }
        factory { get<Database>().favoritesQueries }
        single { EventsCacheStore(get(), get()) }
        single { FavoritesStore(get()) }
    } else {
        // Web fallback (no SQLDelight driver): session-only in-memory stores.
        single { EventsCacheStore(json = get()) }
        single { FavoritesStore() }
    }

    // --- Settings / JSON ---
    single<Json> { Json { ignoreUnknownKeys = true; isLenient = true; explicitNulls = false } }
    single { AppSettings(get<Settings>(), get<Json>()) }

    // --- Theme ---
    single { ThemeController(get()) }
}
