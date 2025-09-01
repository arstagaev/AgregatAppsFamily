package org.agregatcrm.repository

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.agregatcrm.data.remote.ApiConfig
import org.agregatcrm.data.remote.EventsApi
import org.agregatcrm.models.EventItemDto

class EventsRepository(
    private val api: EventsApi,
) {
//    private val kv by lazy { KeyValueStoreProvider.get() }

    /**
     * Fetch latest from network; on success, cache raw JSON.
     * If network fails, fallback to cached JSON (if present).
     */
    suspend fun loadEvents(
        apiConfig: ApiConfig,
        count: Int,
        ncount: Int,

        orderBy: String,
        orderDir: String,

        filterBy: String = "ПодразделениеКомпании",
        filterVal: String = "Воронеж"
    ): Result<List<EventItemDto>> = runCatching {
        val items = api.getEvents(
            api = apiConfig,
            count = count,
            ncount = ncount,
            orderBy = orderBy,
            orderDir = orderDir,
            filterBy = filterBy,
            filterVal = filterVal
        )
        println(">>> ${items.joinToString()}<")
        // Cache raw JSON (so Android can inspect later if needed)
//        val raw = json.encodeToString(items)
//        kv.putString(KVKeys.EVENTS_JSON, raw)

        items
    }
//        .getOrElse { err ->
////         fallback to cached
//        val cached = kv.getString(KVKeys.EVENTS_JSON)
//        if (cached != null) {
//            runCatching { json.decodeFromString<List<EventItem>>(cached) }
//                .getOrElse { emptyList() }
//        } else {
//            emptyList()
//        }
//    }
}
