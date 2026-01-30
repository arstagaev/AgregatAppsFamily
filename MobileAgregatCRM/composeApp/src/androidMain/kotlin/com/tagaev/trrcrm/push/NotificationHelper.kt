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

        val intent = Intent(context, MainActivity::class.java).apply {
            // Use SINGLE_TOP to handle clicks when app is already running
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            // Universal deep link parameters
            putExtra("screen", resolvedScreen)
            resolvedDocId?.let { putExtra("docId", it) }
            // Optional diagnostics / future-proofing
            putExtra("docTitle", data["docTitle"] ?: title)
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
        return data["docId"]
            ?: data["doc_id"]
            ?: data["docID"]
            ?: data["docGuid"]
            ?: data["doc_guid"]
            ?: data["guid"]
    }

    private fun resolveScreen(data: Map<String, String>, title: String?, body: String?): String {
        val explicit = data["screen"]
            ?: data["Screen"]
            ?: data["target"]
            ?: data["docType"]
            ?: data["doc_type"]
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

    private fun inferScreenFromText(text: String): String? {
        val t = text.lowercase()

        // Work orders
        if (
            t.contains("work order") ||
            t.contains("workorder") ||
            t.contains("заказ-нар") ||
            t.contains("заказнар")
        ) return "work_orders"

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
}
