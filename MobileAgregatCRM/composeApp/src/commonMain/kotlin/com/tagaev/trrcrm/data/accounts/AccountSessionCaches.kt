package com.tagaev.trrcrm.data.accounts

import com.tagaev.trrcrm.data.AppSettings
import com.tagaev.trrcrm.data.AppSettingsKeys
import com.tagaev.trrcrm.data.db.EventsCacheStore
import com.tagaev.trrcrm.push.NotificationsUnreadState
import com.tagaev.trrcrm.utils.SessionPermissions

object AccountSessionCaches {
    fun clearCrmUserCaches(settings: AppSettings, eventsCacheStore: EventsCacheStore) {
        eventsCacheStore.clearAll()
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
        ).forEach { key -> settings.setString(key, "") }
        settings.setString(AppSettingsKeys.CORE_SESSION_ID, "")
        settings.setString(AppSettingsKeys.PUSH_REGISTER_LAST_FINGERPRINT, "")
    }
}
