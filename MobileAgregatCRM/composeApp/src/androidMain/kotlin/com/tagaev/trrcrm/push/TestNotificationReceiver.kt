package com.tagaev.trrcrm.push

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * Test BroadcastReceiver for triggering notifications via ADB.
 * 
 * Usage via ADB:
 * adb shell am broadcast -a com.tagaev.trrcrm.TEST_NOTIFICATION \
 *   --es screen events --es docId "EVENT-123" \
 *   --es title "Test Title" --es body "Test Body"
 */
class TestNotificationReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {

        val screen = intent.getStringExtra("screen") ?: "events"
        val docId = intent.getStringExtra("docId")
        val title = intent.getStringExtra("title") ?: "Test Notification"
        val body = intent.getStringExtra("body") ?: "Testing deep link to $screen${docId?.let { " (doc: $it)" } ?: ""}"

        println("TestNotificationReceiver>>> INCOMING INTENT ${title}")

        val data = mutableMapOf<String, String>("screen" to screen)
        docId?.let { data["docId"] = it }

        NotificationHelper.showNotification(
            context = context,
            title = title,
            body = body,
            data = data
        )
    }
}
