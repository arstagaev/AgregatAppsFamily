package com.tagaev.mobileagregatcrm.data

// AppSettings.kt (common)
import com.russhwolf.settings.Settings
import com.russhwolf.settings.serialization.decodeValueOrNull
import com.russhwolf.settings.serialization.encodeValue
import com.russhwolf.settings.serialization.removeValue
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import com.tagaev.mobileagregatcrm.models.EventItemDto



object PrefKeys {
    const val EVENTS_CACHE = "events_cache_v1"

    const val FILTER_COUNT = "FILTER_COUNT"
    const val FILTER_NCOUNT = "FILTER_NCOUNT"
    const val FILTER_BY = "FILTER_BY"
    const val FILTER_VAL = "FILTER_VAL"
    const val ORDER_BY = "ORDER_BY"
    const val ORDER_DIR = "ORDER_DIR"
    const val SHOW_TOP_CONTROLS = "SHOW_TOP_CONTROLS"

    // Details screen
    const val LAST_EVENT_NUMBER = "LAST_EVENT_NUMBER"
    const val DETAILS_USERS_OPEN = "DETAILS_USERS_OPEN"
    const val DETAILS_TASKS_OPEN = "DETAILS_TASKS_OPEN"
    const val DETAILS_MSGS_OPEN = "DETAILS_MSGS_OPEN"
    const val DETAILS_MSG_DRAFT = "DETAILS_MSG_DRAFT"
}

//object PrefDef {
//    const val FILTER_COUNT: Int = 5
//    const val FILTER_N_COUNT: Int = 0
//    const val FILTER_BY: String = "ПодразделениеКомпании"
//    const val FILTER_VAL: String = "Казань"
//    const val ORDER_BY: String = "Дата"
//    const val ORDER_DIR: String = "desc"
//    const val SHOW_TOP: Boolean = true
//}

////////////////////////

data class FilterState(
    val count: Int,
    val ncount: Int,
    val filterBy: String?,
    val filterVal: String?,
    val orderBy: String?,
    val orderDir: String?,
    val showTopControls: Boolean
)

data class DetailsUiState(
    val lastEventNumber: String? = null,
    val usersOpen: Boolean = true,
    val tasksOpen: Boolean = true,
    val messagesOpen: Boolean = true,
    val messageDraft: String = ""
)

class AppSettings(
    private val settings: Settings,
    private val json: Json
) {
    // ---------- Events cache (list) ----------
    fun saveEventsCache(events: List<EventItemDto>) {
        // Option A (recommended): Serialization module (stores as structured keys)
        settings.encodeValue(
            serializer = ListSerializer(EventItemDto.serializer()),
            key = PrefKeys.EVENTS_CACHE,
            value = events
        ) // encodeValue/deserialize APIs, delegates: :contentReference[oaicite:4]{index=4}
    }

    fun loadEventsCache(): List<EventItemDto> {
        return settings.decodeValueOrNull(
            serializer = ListSerializer(EventItemDto.serializer()),
            key = PrefKeys.EVENTS_CACHE
        ) ?: emptyList()
    }

    fun clearEventsCache() {
        settings.removeValue(ListSerializer(EventItemDto.serializer()), PrefKeys.EVENTS_CACHE)
    }

    fun getBool(key: String, defaultValue: Boolean) = settings.getBoolean(key = key, defaultValue = defaultValue)
    fun setBool(key: String, newValue: Boolean) = settings.putBoolean(key = key, value = newValue)

    fun getStringOrNull(key: String) = settings.getStringOrNull(key = key)
    fun getString(key: String, defaultValue: String) = settings.getString(key = key, defaultValue = defaultValue)
    fun setString(key: String, newValue: String) = settings.putString(key = key, value = newValue)

    // ---------- Filters ----------
    fun loadFilters(): FilterState = FilterState(
        count = settings.getInt(PrefKeys.FILTER_COUNT, 0),
        ncount = settings.getInt(PrefKeys.FILTER_NCOUNT, 10),
        filterBy = settings.getStringOrNull(PrefKeys.FILTER_BY),
        filterVal = settings.getStringOrNull(PrefKeys.FILTER_VAL),
        orderBy = settings.getStringOrNull(PrefKeys.ORDER_BY),
        orderDir = settings.getStringOrNull(PrefKeys.ORDER_DIR),
        showTopControls = settings.getBoolean(PrefKeys.SHOW_TOP_CONTROLS, true)
    )

    fun saveFilters(s: FilterState) {
        settings.putInt(PrefKeys.FILTER_COUNT, s.count)
        settings.putInt(PrefKeys.FILTER_NCOUNT, s.ncount)
        s.filterBy?.let { settings.putString(PrefKeys.FILTER_BY, it) } ?: settings.remove(PrefKeys.FILTER_BY)
        s.filterVal?.let { settings.putString(PrefKeys.FILTER_VAL, it) } ?: settings.remove(PrefKeys.FILTER_VAL)
        s.orderBy?.let { settings.putString(PrefKeys.ORDER_BY, it) } ?: settings.remove(PrefKeys.ORDER_BY)
        s.orderDir?.let { settings.putString(PrefKeys.ORDER_DIR, it) } ?: settings.remove(PrefKeys.ORDER_DIR)
        settings.putBoolean(PrefKeys.SHOW_TOP_CONTROLS, s.showTopControls)
    }

    // ---------- Details UI state ----------
    fun loadDetailsUi(): DetailsUiState = DetailsUiState(
        lastEventNumber = settings.getStringOrNull(PrefKeys.LAST_EVENT_NUMBER),
        usersOpen = settings.getBoolean(PrefKeys.DETAILS_USERS_OPEN, true),
        tasksOpen = settings.getBoolean(PrefKeys.DETAILS_TASKS_OPEN, true),
        messagesOpen = settings.getBoolean(PrefKeys.DETAILS_MSGS_OPEN, true),
        messageDraft = settings.getString(PrefKeys.DETAILS_MSG_DRAFT, "")
    )

    fun saveDetailsUi(s: DetailsUiState) {
        s.lastEventNumber?.let { settings.putString(PrefKeys.LAST_EVENT_NUMBER, it) }
            ?: settings.remove(PrefKeys.LAST_EVENT_NUMBER)
        settings.putBoolean(PrefKeys.DETAILS_USERS_OPEN, s.usersOpen)
        settings.putBoolean(PrefKeys.DETAILS_TASKS_OPEN, s.tasksOpen)
        settings.putBoolean(PrefKeys.DETAILS_MSGS_OPEN, s.messagesOpen)
        settings.putString(PrefKeys.DETAILS_MSG_DRAFT, s.messageDraft)
    }
}
