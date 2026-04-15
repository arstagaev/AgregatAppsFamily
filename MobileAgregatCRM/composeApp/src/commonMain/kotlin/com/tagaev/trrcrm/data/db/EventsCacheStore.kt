package com.tagaev.trrcrm.data.db

import com.agregatcrm.db.Events_cacheQueries
import com.tagaev.trrcrm.models.EventItemDto
import kotlinx.serialization.json.Json
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

class EventsCacheStore(
    private val queries: Events_cacheQueries? = null,
    private val json: Json? = null
) {
    private companion object { const val KEY = "last_list" }
    private var memoryCache: List<EventItemDto> = emptyList()

    @OptIn(ExperimentalTime::class)
    fun save(list: List<EventItemDto>) {
        if (queries == null || json == null) {
            // Web fallback: keep cache in memory for current session only.
            memoryCache = list.toList()
            return
        }
        val payload = json.encodeToString(list)
        // Safety guard against absurdly large payloads (adjust threshold to your needs)
        if (payload.length > 500_000) return
        queries.insertOrReplace(
            KEY,
            payload,
            Clock.System.now().toEpochMilliseconds() // simple & reliable on Android
        )
    }

    fun load(): List<EventItemDto> {
        val q = queries
        val parser = json
        if (q == null || parser == null) {
            return memoryCache
        }
        // selectByKey returns String? (the json column), not a row object
        val payload: String? = q.selectByKey(KEY).executeAsOneOrNull()
        if (payload.isNullOrBlank()) return emptyList()
        return runCatching { parser.decodeFromString<List<EventItemDto>>(payload) }
            .getOrElse { emptyList() } // avoids T inference issues
    }

    fun clearAll() {
        if (queries == null) {
            memoryCache = emptyList()
            return
        }
        queries.clearEventsCache()
    }

}
