package com.tagaev.trrcrm.data.accounts

import com.tagaev.trrcrm.data.AppSettings
import com.tagaev.trrcrm.data.AppSettingsKeys
import com.tagaev.trrcrm.data.db.EventsCacheStore
import com.tagaev.trrcrm.data.db.FavoritesStore
import com.tagaev.trrcrm.push.NotificationsUnreadState
import com.tagaev.trrcrm.utils.SessionPermissions

object AccountSessionCaches {
    @Volatile
    private var listGeneration: Int = 0

    fun listGeneration(): Int = listGeneration

    fun isCurrentListGeneration(captured: Int): Boolean = captured == listGeneration

    fun bumpListGeneration() {
        listGeneration += 1
    }

    fun currentListOwner(
        settings: AppSettings,
        accountStore: AccountSessionStore? = null,
    ): String {
        return accountStore?.activeAccount()?.id?.takeIf { it.isNotBlank() }
            ?: settings.getStringOrNull(AppSettingsKeys.TOKEN_KEY).orEmpty()
    }

    fun canCommitListCache(startedOwner: String, currentOwner: String): Boolean {
        return startedOwner.isNotBlank() && startedOwner == currentOwner
    }

    fun clearCrmUserCaches(
        settings: AppSettings,
        eventsCacheStore: EventsCacheStore,
        favoritesStore: FavoritesStore,
    ) {
        bumpListGeneration()
        eventsCacheStore.clearAll()
        favoritesStore.clearAll()
        settings.clearEventsCache()
        SessionPermissions.clear()
        settings.setInt(AppSettingsKeys.NOTIFICATIONS_UNREAD_COUNT, 0)
        NotificationsUnreadState.setCount(0)
        listOf(
            AppSettingsKeys.EVENTS_REFINE_STATE,
            AppSettingsKeys.WORK_ORDERS_REFINE_STATE,
            AppSettingsKeys.BUYER_ORDERS_REFINE_STATE,
            AppSettingsKeys.SUPPLIER_ORDERS_REFINE_STATE,
            AppSettingsKeys.COMPLECTATION_REFINE_STATE,
            AppSettingsKeys.CARGO_REFINE_STATE,
            AppSettingsKeys.CARGO_MASTER_REFINE_STATE,
            AppSettingsKeys.COMPLAINTS_REFINE_STATE,
            AppSettingsKeys.INNERORDER_REFINE_STATE,
            AppSettingsKeys.INCOMING_APPLICATIONS_REFINE_STATE,
            AppSettingsKeys.REPAIR_TEMPLATE_CATALOG_REFINE_STATE,
            AppSettingsKeys.EXPENSE_REQUESTS_REFINE_STATE,
            AppSettingsKeys.NOTIFICATIONS_SEARCH_QUERY,
            AppSettingsKeys.NOTIFICATIONS_STATUS_FILTER,
            AppSettingsKeys.LAST_EVENT_NUMBER,
            AppSettingsKeys.DETAILS_MSG_DRAFT,
            AppSettingsKeys.FILTER_COUNT,
            AppSettingsKeys.FILTER_NCOUNT,
            AppSettingsKeys.FILTER_BY,
            AppSettingsKeys.FILTER_VAL,
            AppSettingsKeys.ORDER_BY,
            AppSettingsKeys.ORDER_DIR,
            AppSettingsKeys.FILTER_TYPE,
            AppSettingsKeys.FILTER_STATE_WO,
        ).forEach { key -> settings.remove(key) }
        settings.setString(AppSettingsKeys.PUSH_REGISTER_LAST_FINGERPRINT, "")
    }
}
