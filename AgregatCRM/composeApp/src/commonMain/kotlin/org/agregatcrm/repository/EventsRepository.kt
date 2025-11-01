package org.agregatcrm.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.agregatcrm.data.local.FavoritesStore
import org.agregatcrm.data.remote.ApiConfig
import org.agregatcrm.data.remote.EventsApi
import org.agregatcrm.data.remote.Resource
import org.agregatcrm.models.EventItemDto

class EventsRepository(
    private val api: EventsApi,
//    private val store: FavoritesStore
) {
//    private val _favorites = MutableStateFlow(store.getFavorites())
//    val favorites: StateFlow<Set<String>> get() = _favorites
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
    ): Resource<List<EventItemDto>> = api.getEvents(
        api = apiConfig,
        count = count,
        ncount = ncount,
        orderBy = orderBy,
        orderDir = orderDir,
        filterBy = filterBy,
        filterVal = filterVal
    )

    suspend fun sendMessage(
        apiConfig: ApiConfig,
        number: String,
        date: String,
        message: String,
    ): Resource<List<EventItemDto>> = api.sendMessage(
        api = apiConfig,
        number = number,
        date = date,
        message = message
    )

//    fun favoritesFlow(): Flow<Set<String>> =
//        queries.selectAll().asFlow().mapToList(io).map { rows ->
//            rows.map { it.number }.toSet()
//        }

//    fun toggle(number: String) {
//        val current = _favorites.value.toMutableSet()
//        if (current.contains(number)) {
//            current.remove(number)
//        } else {
//            current.add(number)
//        }
//        store.saveFavorites(current)
//        _favorites.value = current
//    }
//
//    fun isFavorite(number: String): Boolean = _favorites.value.contains(number)


//    suspend fun loadEvents(
//        apiConfig: ApiConfig,
//        count: Int,
//        ncount: Int,
//
//        orderBy: String,
//        orderDir: String,
//
//        filterBy: String = "ПодразделениеКомпании",
//        filterVal: String = "Воронеж"
//    ): Resource<List<EventItemDto>> = runCatching {
//        val items = api.getEvents(
//            api = apiConfig,
//            count = count,
//            ncount = ncount,
//            orderBy = orderBy,
//            orderDir = orderDir,
//            filterBy = filterBy,
//            filterVal = filterVal
//        )
//        println(">>> ${items.joinToString()}<")
//        // Cache raw JSON (so Android can inspect later if needed)
////        val raw = json.encodeToString(items)
////        kv.putString(KVKeys.EVENTS_JSON, raw)
//
//        items
//    }
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
