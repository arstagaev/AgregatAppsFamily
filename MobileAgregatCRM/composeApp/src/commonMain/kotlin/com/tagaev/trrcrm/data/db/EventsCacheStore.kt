package com.tagaev.trrcrm.data.db

import com.agregatcrm.db.Events_cacheQueries
import com.tagaev.trrcrm.models.EventItemDto
import kotlinx.serialization.json.Json
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

class EventsCacheStore(
    private val queries: Events_cacheQueries,
    private val json: Json
) {
    private companion object { const val KEY = "last_list" }

    @OptIn(ExperimentalTime::class)
    fun save(list: List<EventItemDto>) {
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
        // selectByKey returns String? (the json column), not a row object
        val payload: String? = queries.selectByKey(KEY).executeAsOneOrNull()
        if (payload.isNullOrBlank()) return emptyList()
        return runCatching { json.decodeFromString<List<EventItemDto>>(payload) }
            .getOrElse { emptyList() } // avoids T inference issues
    }

    fun clearAll() {
        queries.clearEventsCache()
    }

}
