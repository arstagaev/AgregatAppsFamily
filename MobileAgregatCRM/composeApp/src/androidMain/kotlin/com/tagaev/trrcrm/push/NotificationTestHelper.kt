package com.tagaev.trrcrm.push

import android.content.Context

/**
 * Helper for testing push notifications locally without FCM.
 * Useful for testing deep link functionality during development.
 */
object NotificationTestHelper {
    /**
     * Test notification locally without FCM.
     * Call this from anywhere in your app (e.g., Settings screen or debug menu).
     * 
     * @param context Application context
     * @param screen Target screen name (events, work_orders, cargo, complaints, inner_orders)
     * @param docId Optional document ID to open
     * @param title Notification title
     * @param body Notification body text
     */
    fun testNotification(
        context: Context,
        screen: String = "events",
        docId: String? = null,
        title: String = "Test Notification",
        body: String = "Testing deep link to $screen${docId?.let { " (doc: $it)" } ?: ""}"
    ) {
        val data = mutableMapOf<String, String>(
            "screen" to screen
        )
        docId?.let { data["docId"] = it }
        
        NotificationHelper.showNotification(
            context = context,
            title = title,
            body = body,
            data = data
        )
    }
}
