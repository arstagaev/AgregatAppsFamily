package com.tagaev.trrcrm.data.accounts

import com.russhwolf.settings.Settings
import com.tagaev.trrcrm.data.AppSettings
import com.tagaev.trrcrm.data.AppSettingsKeys
import com.tagaev.trrcrm.data.db.EventsCacheStore
import com.tagaev.trrcrm.data.db.FavoritesStore
import com.tagaev.trrcrm.models.EventItemDto
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.serialization.json.Json

private class CacheTestMemorySettings : Settings {
    private val data = mutableMapOf<String, Any?>()
    override val keys: Set<String> get() = data.keys
    override val size: Int get() = data.size
    override fun clear() = data.clear()
    override fun remove(key: String) { data.remove(key) }
    override fun hasKey(key: String): Boolean = data.containsKey(key)
    override fun putInt(key: String, value: Int) { data[key] = value }
    override fun getInt(key: String, defaultValue: Int): Int = data[key] as? Int ?: defaultValue
    override fun getIntOrNull(key: String): Int? = data[key] as? Int
    override fun putLong(key: String, value: Long) { data[key] = value }
    override fun getLong(key: String, defaultValue: Long): Long = data[key] as? Long ?: defaultValue
    override fun getLongOrNull(key: String): Long? = data[key] as? Long
    override fun putString(key: String, value: String) { data[key] = value }
    override fun getString(key: String, defaultValue: String): String = data[key] as? String ?: defaultValue
    override fun getStringOrNull(key: String): String? = data[key] as? String
    override fun putFloat(key: String, value: Float) { data[key] = value }
    override fun getFloat(key: String, defaultValue: Float): Float = data[key] as? Float ?: defaultValue
    override fun getFloatOrNull(key: String): Float? = data[key] as? Float
    override fun putDouble(key: String, value: Double) { data[key] = value }
    override fun getDouble(key: String, defaultValue: Double): Double = data[key] as? Double ?: defaultValue
    override fun getDoubleOrNull(key: String): Double? = data[key] as? Double
    override fun putBoolean(key: String, value: Boolean) { data[key] = value }
    override fun getBoolean(key: String, defaultValue: Boolean): Boolean = data[key] as? Boolean ?: defaultValue
    override fun getBooleanOrNull(key: String): Boolean? = data[key] as? Boolean
}

class AccountSessionCachesTest {
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    private fun settings(seed: AppSettings.() -> Unit = {}): AppSettings =
        AppSettings(CacheTestMemorySettings(), json).apply(seed)

    @Test
    fun clearCrmUserCachesWipesEventsFavoritesFiltersAndLastEvent() {
        val settings = settings {
            setString(AppSettingsKeys.EVENTS_REFINE_STATE, "refine-a")
            setString(AppSettingsKeys.WORK_ORDERS_REFINE_STATE, "wo-refine")
            setString(AppSettingsKeys.LAST_EVENT_NUMBER, "EVT-100")
            setString(AppSettingsKeys.DETAILS_MSG_DRAFT, "draft")
            setInt(AppSettingsKeys.FILTER_COUNT, 30)
            setInt(AppSettingsKeys.FILTER_NCOUNT, 40)
            setString(AppSettingsKeys.FILTER_BY, "ПодразделениеКомпании")
            setString(AppSettingsKeys.FILTER_VAL, "Казань")
            setString(AppSettingsKeys.ORDER_BY, "Дата")
            setString(AppSettingsKeys.ORDER_DIR, "desc")
            setString(AppSettingsKeys.FILTER_TYPE, "event")
            setString(AppSettingsKeys.FILTER_STATE_WO, "open")
            setString(AppSettingsKeys.FCM_TOKEN, "keep-fcm")
        }
        val eventsCache = EventsCacheStore(json = json)
        eventsCache.save(listOf(EventItemDto(number = "EVT-100", subject = "User A event")))
        val favorites = FavoritesStore()
        favorites.add("EVT-100")

        val generationBeforeClear = AccountSessionCaches.listGeneration()
        AccountSessionCaches.clearCrmUserCaches(settings, eventsCache, favorites)

        assertTrue(eventsCache.load().isEmpty())
        assertFalse(favorites.isFavorite("EVT-100"))
        assertNull(settings.getStringOrNull(AppSettingsKeys.EVENTS_REFINE_STATE))
        assertNull(settings.getStringOrNull(AppSettingsKeys.WORK_ORDERS_REFINE_STATE))
        assertNull(settings.getStringOrNull(AppSettingsKeys.LAST_EVENT_NUMBER))
        assertNull(settings.getStringOrNull(AppSettingsKeys.DETAILS_MSG_DRAFT))
        assertEquals(0, settings.getInt(AppSettingsKeys.FILTER_COUNT, 0))
        assertEquals(10, settings.loadFilters().ncount)
        assertNull(settings.getStringOrNull(AppSettingsKeys.FILTER_BY))
        assertNull(settings.getStringOrNull(AppSettingsKeys.FILTER_VAL))
        assertNull(settings.getStringOrNull(AppSettingsKeys.ORDER_BY))
        assertNull(settings.getStringOrNull(AppSettingsKeys.ORDER_DIR))
        assertNull(settings.getStringOrNull(AppSettingsKeys.FILTER_TYPE))
        assertNull(settings.getStringOrNull(AppSettingsKeys.FILTER_STATE_WO))
        assertEquals("keep-fcm", settings.getStringOrNull(AppSettingsKeys.FCM_TOKEN))
        assertFalse(AccountSessionCaches.isCurrentListGeneration(generationBeforeClear))
        assertTrue(AccountSessionCaches.isCurrentListGeneration(AccountSessionCaches.listGeneration()))
    }

    @Test
    fun saveAfterActiveAccountChangeDoesNotRestoreOldList() {
        val settings = settings {
            setString(AppSettingsKeys.TOKEN_KEY, "tok-a")
            setString(AppSettingsKeys.PERSONAL_DATA, "A")
            setString(AppSettingsKeys.ACCOUNT_LOGIN, "a")
        }
        val store = AccountSessionStore(settings, json)
        store.ensureMigrated()
        settings.setString(AppSettingsKeys.TOKEN_KEY, "tok-b")
        settings.setString(AppSettingsKeys.PERSONAL_DATA, "B")
        settings.setString(AppSettingsKeys.ACCOUNT_LOGIN, "b")
        store.markPendingCreateNewSlot()
        store.bindSuccessfulLogin("b")

        val eventsCache = EventsCacheStore(json = json)
        val favorites = FavoritesStore()
        val startedOwner = AccountSessionCaches.currentListOwner(settings, store)
        eventsCache.save(listOf(EventItemDto(number = "A-1")))

        AccountSessionCaches.clearCrmUserCaches(settings, eventsCache, favorites)
        val accountA = store.accounts().first { it.login == "a" }
        store.activate(accountA.id)

        val stale = listOf(EventItemDto(number = "A-1", subject = "stale"))
        if (AccountSessionCaches.canCommitListCache(startedOwner, AccountSessionCaches.currentListOwner(settings, store))) {
            eventsCache.save(stale)
        }

        assertTrue(eventsCache.load().isEmpty())
        assertFalse(AccountSessionCaches.canCommitListCache(startedOwner, AccountSessionCaches.currentListOwner(settings, store)))
    }

    @Test
    fun canCommitListCacheWhenSameOwner() {
        assertTrue(AccountSessionCaches.canCommitListCache("acc-1", "acc-1"))
        assertFalse(AccountSessionCaches.canCommitListCache("acc-1", "acc-2"))
        assertFalse(AccountSessionCaches.canCommitListCache("", "acc-1"))
    }
}
