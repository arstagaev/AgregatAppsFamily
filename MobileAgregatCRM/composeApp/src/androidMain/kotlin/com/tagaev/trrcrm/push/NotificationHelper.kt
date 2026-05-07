package com.tagaev.trrcrm.push

import android.app.*
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.tagaev.trrcrm.MainActivity
import com.tagaev.trrcrm.R

object NotificationHelper {

    private const val CHANNEL_ID = "thread_messages"

    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val mgr = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Thread messages",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            mgr.createNotificationChannel(channel)
        }
    }

    fun showNotification(
        context: Context,
        title: String,
        body: String,
        data: Map<String, String>,
    ) {
        ensureChannel(context)

        val resolvedScreen = resolveScreen(data = data, title = title, body = body)
        val resolvedDocId = resolveDocId(data)
        val resolvedMessageText = resolveMessageText(data = data, body = body)
        val resolvedTitle = resolveTitle(data = data, fallbackTitle = title)
        val canonicalScreen = firstNonBlank(data, "screen", "Screen", "target", "docType", "doc_type")
        val canonicalDocId = firstNonBlank(data, "docId", "doc_id", "docID", "docGuid", "doc_guid", "guid")
        val canonicalTitle = firstNonBlank(data, "docTitle", "title", "notification_title")
        val hasCanonicalPayload = !canonicalScreen.isNullOrBlank() &&
                (!canonicalDocId.isNullOrBlank() || !canonicalTitle.isNullOrBlank())
        println(
            "PUSH_SERVICE DEEPLINK: Android NotificationHelper resolved " +
                    "screen='$resolvedScreen', docId='${resolvedDocId ?: ""}', " +
                    "title='$resolvedTitle', messageTextLen=${resolvedMessageText.length}, " +
                    "dataKeys=${data.keys.sorted()}, hasCanonicalPayload=$hasCanonicalPayload"
        )
        if (!hasCanonicalPayload) {
            println("PUSH_SERVICE DEEPLINK: missing_canonical_push_payload keys=${data.keys.sorted()}")
        }

        val intent = Intent(context, MainActivity::class.java).apply {
            // Use SINGLE_TOP to handle clicks when app is already running
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            // Universal deep link parameters
            if (hasCanonicalPayload) {
                putExtra("screen", resolvedScreen)
                resolvedDocId?.let { putExtra("docId", it) }
            }
            // Optional diagnostics / future-proofing
            putExtra("docTitle", resolvedTitle)
            putExtra("title", resolvedTitle)
            putExtra("body", resolvedMessageText)
            putExtra("message_text", resolvedMessageText)
            putExtra("screen_raw", canonicalScreen.orEmpty())
            putExtra("doc_id_raw", canonicalDocId.orEmpty())
            putExtra("has_canonical_payload", hasCanonicalPayload)
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            System.currentTimeMillis().toInt(), // unique ID for each notification
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.racecar2) // add one in resources
            .setContentTitle(title)
            .setContentText(body)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(System.currentTimeMillis().toInt(), notification)
    }

    private fun resolveDocId(data: Map<String, String>): String? {
        return firstNonBlank(data, "docId", "doc_id", "docID", "docGuid", "doc_guid", "guid")
    }

    private fun resolveScreen(data: Map<String, String>, title: String?, body: String?): String {
        val explicit = firstNonBlank(data, "screen", "Screen", "target", "docType", "doc_type")
        if (!explicit.isNullOrBlank()) {
            return normalizeScreen(explicit)
        }

        val text = listOfNotNull(
            data["docTitle"],
            title,
            body,
        ).joinToString(" ")

        return inferScreenFromText(text) ?: "events"
    }

    private fun resolveMessageText(data: Map<String, String>, body: String): String {
        return firstNonBlank(data, "message_text", "messageText", "body", "text", "comment")
            ?: body
    }

    private fun resolveTitle(data: Map<String, String>, fallbackTitle: String): String {
        return firstNonBlank(data, "docTitle", "title", "notification_title")
            ?: fallbackTitle
    }

    private fun inferScreenFromText(text: String): String? {
        val t = text.lowercase()

        // Work orders
        if (
            t.contains("work order") ||
            t.contains("workorder") ||
            t.contains("заказ-нар") ||
            t.contains("заказнар")
        ) return "work_orders"

        // Complectation
        if (t.contains("complect") || t.contains("комплект")) return "complectation"

        // Complaints
        if (t.contains("complaint") || t.contains("рекламац")) return "complaints"

        // Inner orders
        if (
            t.contains("inner order") ||
            t.contains("innerorder") ||
            t.contains("внутр") ||
            t.contains("заказ внутрен")
        ) return "inner_orders"

        // Cargo / deliveries
        if (t.contains("cargo") || t.contains("груз") || t.contains("достав")) return "cargo"

        // Events
        if (t.contains("event") || t.contains("событ")) return "events"

        return null
    }

    private fun normalizeScreen(raw: String): String {
        return raw
            .trim()
            .lowercase()
            .replace('-', '_')
            .replace(' ', '_')
    }

    private fun firstNonBlank(data: Map<String, String>, vararg keys: String): String? {
        for (key in keys) {
            val value = data[key]?.trim()
            if (!value.isNullOrBlank()) return value
        }
        return null
    }
}
