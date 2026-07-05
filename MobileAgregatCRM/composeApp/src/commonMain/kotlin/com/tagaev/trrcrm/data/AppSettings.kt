package com.tagaev.trrcrm.data

// AppSettings.kt (common)
import com.russhwolf.settings.Settings
import com.russhwolf.settings.serialization.removeValue
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import com.tagaev.trrcrm.models.EventItemDto
import com.tagaev.trrcrm.navigation.BottomNavLayoutItem



object AppSettingsKeys {
    const val EVENTS_CACHE = "events_cache_v2"

    const val FILTER_COUNT = "FILTER_COUNT"
    const val FILTER_NCOUNT = "FILTER_NCOUNT"
    const val FILTER_BY = "FILTER_BY"
    const val FILTER_VAL = "FILTER_VAL"
    const val ORDER_BY = "ORDER_BY"
    const val ORDER_DIR = "ORDER_DIR"
    const val SHOW_TOP_CONTROLS = "SHOW_TOP_CONTROLS"
    const val FILTER_TYPE = "FILTER_TYPE"
    const val DEPARTMENT = "DEPARTMENT"

    // OrderBy Dir Status SearchQueryType Filter
//    const val CARGO_ORDER_BY = "CARGO_ORDER_BY"
//    const val CARGO_DIR = "CARGO_DIR"
//    const val CARGO_STATUS = "CARGO_STATUS"
//    const val CARGO_SEARCH_QUERY_TYPE = "CARGO_SEARCH_QUERY_TYPE"
//    const val CARGO_FILTER = "CARGO_FILTER"


    const val FILTER_STATE_WO = "FILTER_STATE_WO"

    // Details screen
    const val LAST_EVENT_NUMBER = "LAST_EVENT_NUMBER"
    const val DETAILS_USERS_OPEN = "DETAILS_USERS_OPEN"
    const val DETAILS_TASKS_OPEN = "DETAILS_TASKS_OPEN"
    const val DETAILS_MSGS_OPEN = "DETAILS_MSGS_OPEN"
    const val DETAILS_MSG_DRAFT = "DETAILS_MSG_DRAFT"


    const val ERROR_LOGS = "ERROR_LOGS"
    //FCM Token Cant registrate .         ;
    //0 - not registered / 1 - registered ;

    const val EMAIL     = "EMAIL"
    const val PASS      = "PASS"
    const val TOKEN_KEY = "TOKEN_KEY"

    const val LAST_UPDATE = "LAST_UPDATE"

    const val PERSONAL_DATA = "PERSONAL_DATA"
    const val CORE_SESSION_ID = "CORE_SESSION_ID"
    const val CORE_BOOTSTRAP_RETRY_ON_TOKEN = "CORE_BOOTSTRAP_RETRY_ON_TOKEN"
    const val IOS_APNS_READY = "IOS_APNS_READY"
    const val STABLE_DEVICE_ID = "STABLE_DEVICE_ID"
    const val PUSH_REGISTER_LAST_FINGERPRINT = "PUSH_REGISTER_LAST_FINGERPRINT"
    const val PUSH_FEATURE_TOGGLE_ENABLED = "PUSH_FEATURE_TOGGLE_ENABLED"
    const val PUSH_FEATURE_TOGGLE_UPDATED_AT_MS = "PUSH_FEATURE_TOGGLE_UPDATED_AT_MS"
    const val DEVICE_MUTE_ALL = "DEVICE_MUTE_ALL"
    const val DEVICE_MUTE_EVENT = "DEVICE_MUTE_EVENT"
    const val DEVICE_MUTE_WORK_ORDER = "DEVICE_MUTE_WORK_ORDER"
    const val DEVICE_MUTE_COMPLECTATION = "DEVICE_MUTE_COMPLECTATION"
    const val DESKTOP_UPDATER_LAST_CHECK_AT_MS = "DESKTOP_UPDATER_LAST_CHECK_AT_MS"
    const val DESKTOP_UPDATER_SKIPPED_VERSION = "DESKTOP_UPDATER_SKIPPED_VERSION"

    const val FCM_TOKEN = "NULL"

    const val VERSION_CODE = "v2"
    const val EVENTS_REFINE_STATE = "EVENTS_REFINE_STATE_"+VERSION_CODE
    const val WORK_ORDERS_REFINE_STATE = "WORK_ORDERS_REFINE_STATE_"+VERSION_CODE
    const val BUYER_ORDERS_REFINE_STATE = "BUYER_ORDERS_REFINE_STATE_"+VERSION_CODE
    const val SUPPLIER_ORDERS_REFINE_STATE = "SUPPLIER_ORDERS_REFINE_STATE_"+VERSION_CODE
    const val COMPLECTATION_REFINE_STATE = "COMPLECTATION_REFINE_STATE_"+VERSION_CODE
    const val CARGO_REFINE_STATE = "CARGO_REFINE_STATE_"+VERSION_CODE
    /** Экран доставки (Cargo); не путать с CARGO_REFINE_STATE — там legacy для миграции заказ-нарядов. */
    const val CARGO_MASTER_REFINE_STATE = "CARGO_MASTER_REFINE_STATE_"+VERSION_CODE
    const val COMPLAINTS_REFINE_STATE = "COMPLAINTS_REFINE_STATE_"+VERSION_CODE
    const val INNERORDER_REFINE_STATE = "INNERORDER_REFINE_STATE_"+VERSION_CODE
    const val INCOMING_APPLICATIONS_REFINE_STATE = "INCOMING_APPLICATIONS_REFINE_STATE_"+VERSION_CODE
    const val REPAIR_TEMPLATE_CATALOG_REFINE_STATE = "REPAIR_TEMPLATE_CATALOG_REFINE_STATE_"+VERSION_CODE
    const val EXPENSE_REQUESTS_REFINE_STATE = "EXPENSE_REQUESTS_REFINE_STATE_"+VERSION_CODE
    const val NOTIFICATIONS_SEARCH_QUERY = "NOTIFICATIONS_SEARCH_QUERY_"+VERSION_CODE
    const val NOTIFICATIONS_STATUS_FILTER = "NOTIFICATIONS_STATUS_FILTER_"+VERSION_CODE
    const val NOTIFICATIONS_UNREAD_COUNT = "NOTIFICATIONS_UNREAD_COUNT_"+VERSION_CODE
    const val BOTTOM_NAV_LAYOUT = "BOTTOM_NAV_LAYOUT_" + VERSION_CODE
    const val DEVELOPER_MODE_ENABLED = "DEVELOPER_MODE_ENABLED_" + VERSION_CODE

//    const val EVENTS_REFINE_STATE = "EVENTS_REFINE_STATE"
//    const val WORK_ORDERS_REFINE_STATE = "WORK_ORDERS_REFINE_STATE"
//    const val CARGO_REFINE_STATE = "CARGO_REFINE_STATE"
//    const val COMPLAINTS_REFINE_STATE = "COMPLAINTS_REFINE_STATE"
//    const val INNERORDER_REFINE_STATE = "COMPLAINTS_REFINE_STATE"
}

//object PrefDef {
//    const val FILTER_COUNT: Int = 5
//    const val FILTER_N_COUNT: Int = 0
//    const val FILTER_BY: String = "ПодразделениеКомпании"
//    const val FILTER_VAL: String = "Казань"
//    const val ORDER_BY: String = "Дата"
//    const val ORDER_DIR: String = "desc"
//    const val SHOW_TOP: Boolean = true
//"username": "Ivan.a.a@my.Ivan.ru",
//"ФИО": "Ivan Ivan Ivan",
//"Должность": "Веб-программист",
//    const val VERSION_CODE = "v2"
//
//    const val EVENTS_REFINE_STATE = "EVENTS_REFINE_STATE_"+VERSION_CODE
//    const val WORK_ORDERS_REFINE_STATE = "WORK_ORDERS_REFINE_STATE_"+VERSION_CODE
//    const val CARGO_REFINE_STATE = "CARGO_REFINE_STATE_"+VERSION_CODE
//    const val COMPLAINTS_REFINE_STATE = "COMPLAINTS_REFINE_STATE_"+VERSION_CODE
//    const val INNERORDER_REFINE_STATE = "COMPLAINTS_REFINE_STATE_"+VERSION_CODE

////////////////////////

data class FilterState(
    val count: Int,
    val ncount: Int,
    val filterBy: String?,
    val filterVal: String?,
    val orderBy: String?,
    val orderDir: String?,
    val filtertype: String?,
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
//    fun saveEventsCache(events: List<EventItemDto>) {
//        // Option A (recommended): Serialization module (stores as structured keys)
//        settings.encodeValue(
//            serializer = ListSerializer(EventItemDto.serializer()),
//            key = AppSettingsKeys.EVENTS_CACHE,
//            value = events
//        ) // encodeValue/deserialize APIs, delegates: :contentReference[oaicite:4]{index=4}
//    }
//
//    fun loadEventsCache(): List<EventItemDto> {
//        return settings.decodeValueOrNull(
//            serializer = ListSerializer(EventItemDto.serializer()),
//            key = AppSettingsKeys.EVENTS_CACHE
//        ) ?: emptyList()
//    }

    fun clearEventsCache() {
        settings.removeValue(ListSerializer(EventItemDto.serializer()), AppSettingsKeys.EVENTS_CACHE)
    }

    fun getBool(key: String, defaultValue: Boolean) = settings.getBoolean(key = key, defaultValue = defaultValue)
    fun setBool(key: String, newValue: Boolean) = settings.putBoolean(key = key, value = newValue)
    fun getLong(key: String, defaultValue: Long) = settings.getLong(key = key, defaultValue = defaultValue)
    fun setLong(key: String, newValue: Long) = settings.putLong(key = key, value = newValue)
    fun getInt(key: String, defaultValue: Int) = settings.getInt(key = key, defaultValue = defaultValue)
    fun setInt(key: String, newValue: Int) = settings.putInt(key = key, value = newValue)

    fun getStringOrNull(key: String) = settings.getStringOrNull(key = key)
    fun getString(key: String, defaultValue: String) = settings.getString(key = key, defaultValue = defaultValue)
    fun setString(key: String, newValue: String) = settings.putString(key = key, value = newValue)

    // ---------- Filters ----------
    fun loadFilters(): FilterState = FilterState(
        count = settings.getInt(AppSettingsKeys.FILTER_COUNT, 0),
        ncount = settings.getInt(AppSettingsKeys.FILTER_NCOUNT, 10),
        filterBy = settings.getStringOrNull(AppSettingsKeys.FILTER_BY),
        filterVal = settings.getStringOrNull(AppSettingsKeys.FILTER_VAL),
        orderBy = settings.getStringOrNull(AppSettingsKeys.ORDER_BY),
        orderDir = settings.getStringOrNull(AppSettingsKeys.ORDER_DIR),
        showTopControls = settings.getBoolean(AppSettingsKeys.SHOW_TOP_CONTROLS, true),
        filtertype = settings.getStringOrNull(AppSettingsKeys.FILTER_TYPE)
    )

    fun saveFilters(s: FilterState) {
        settings.putInt(AppSettingsKeys.FILTER_COUNT, s.count)
        settings.putInt(AppSettingsKeys.FILTER_NCOUNT, s.ncount)
        s.filterBy?.let { settings.putString(AppSettingsKeys.FILTER_BY, it) } ?: settings.remove(AppSettingsKeys.FILTER_BY)
        s.filterVal?.let { settings.putString(AppSettingsKeys.FILTER_VAL, it) } ?: settings.remove(AppSettingsKeys.FILTER_VAL)
        s.orderBy?.let { settings.putString(AppSettingsKeys.ORDER_BY, it) } ?: settings.remove(AppSettingsKeys.ORDER_BY)
        s.orderDir?.let { settings.putString(AppSettingsKeys.ORDER_DIR, it) } ?: settings.remove(AppSettingsKeys.ORDER_DIR)
        settings.putBoolean(AppSettingsKeys.SHOW_TOP_CONTROLS, s.showTopControls)
        s.filtertype?.let { settings.putString(AppSettingsKeys.FILTER_TYPE, it) } ?: settings.remove(AppSettingsKeys.FILTER_TYPE)
    }

    // ---------- Details UI state ----------
    fun loadDetailsUi(): DetailsUiState = DetailsUiState(
        lastEventNumber = settings.getStringOrNull(AppSettingsKeys.LAST_EVENT_NUMBER),
        usersOpen = settings.getBoolean(AppSettingsKeys.DETAILS_USERS_OPEN, true),
        tasksOpen = settings.getBoolean(AppSettingsKeys.DETAILS_TASKS_OPEN, true),
        messagesOpen = settings.getBoolean(AppSettingsKeys.DETAILS_MSGS_OPEN, true),
        messageDraft = settings.getString(AppSettingsKeys.DETAILS_MSG_DRAFT, "")
    )

    fun saveDetailsUi(s: DetailsUiState) {
        s.lastEventNumber?.let { settings.putString(AppSettingsKeys.LAST_EVENT_NUMBER, it) }
            ?: settings.remove(AppSettingsKeys.LAST_EVENT_NUMBER)
        settings.putBoolean(AppSettingsKeys.DETAILS_USERS_OPEN, s.usersOpen)
        settings.putBoolean(AppSettingsKeys.DETAILS_TASKS_OPEN, s.tasksOpen)
        settings.putBoolean(AppSettingsKeys.DETAILS_MSGS_OPEN, s.messagesOpen)
        settings.putString(AppSettingsKeys.DETAILS_MSG_DRAFT, s.messageDraft)
    }

    fun loadBottomNavLayout(): List<BottomNavLayoutItem>? {
        val raw = getStringOrNull(AppSettingsKeys.BOTTOM_NAV_LAYOUT) ?: return null
        return runCatching {
            json.decodeFromString(ListSerializer(BottomNavLayoutItem.serializer()), raw)
        }.getOrNull()
    }

    fun saveBottomNavLayout(items: List<BottomNavLayoutItem>) {
        setString(
            AppSettingsKeys.BOTTOM_NAV_LAYOUT,
            json.encodeToString(ListSerializer(BottomNavLayoutItem.serializer()), items)
        )
    }

    fun clearAll() {
        settings.clear()
    }

    /**
     * Clears user/session-scoped state while preserving install-level push identity.
     * This keeps rebind reliability across account switches on the same device install.
     */
    fun clearForLogoutPreservingInstallIdentity() {
        val preservedFcmToken = getStringOrNull(AppSettingsKeys.FCM_TOKEN)
        val preservedStableDeviceId = getStringOrNull(AppSettingsKeys.STABLE_DEVICE_ID)
        val preservedIosApnsReady = getBool(AppSettingsKeys.IOS_APNS_READY, false)
        val preservedPushToggleEnabled = getBool(AppSettingsKeys.PUSH_FEATURE_TOGGLE_ENABLED, true)
        val preservedPushToggleUpdatedAt = getLong(AppSettingsKeys.PUSH_FEATURE_TOGGLE_UPDATED_AT_MS, 0L)

        clearAll()

        preservedFcmToken?.let { setString(AppSettingsKeys.FCM_TOKEN, it) }
        preservedStableDeviceId?.let { setString(AppSettingsKeys.STABLE_DEVICE_ID, it) }
        setBool(AppSettingsKeys.IOS_APNS_READY, preservedIosApnsReady)
        setBool(AppSettingsKeys.PUSH_FEATURE_TOGGLE_ENABLED, preservedPushToggleEnabled)
        if (preservedPushToggleUpdatedAt > 0L) {
            setLong(AppSettingsKeys.PUSH_FEATURE_TOGGLE_UPDATED_AT_MS, preservedPushToggleUpdatedAt)
        }
    }
}
