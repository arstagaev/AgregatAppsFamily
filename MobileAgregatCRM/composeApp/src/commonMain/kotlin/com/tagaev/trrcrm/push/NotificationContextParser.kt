package com.tagaev.trrcrm.push

data class NotificationContext(
    val screen: String?,
    val docTypeLabel: String?,
    val primaryKey: String?,
    val messageHint: String?,
)

object NotificationContextParser {
    fun parse(
        title: String?,
        screen: String?,
        docId: String?,
        messageText: String?,
    ): NotificationContext {
        val normalizedTitle = title.orEmpty().trim()
        val resolvedScreen = inferScreen(normalizedTitle, screen)
        val keyFromTitle = extractPrimaryKeyFromTitle(normalizedTitle, resolvedScreen)
        val label = inferDocTypeLabel(normalizedTitle, resolvedScreen)
        val normalizedDocId = normalizeKey(docId)
        return NotificationContext(
            screen = resolvedScreen,
            docTypeLabel = label,
            primaryKey = keyFromTitle ?: normalizedDocId,
            messageHint = normalizeMessageHint(messageText),
        )
    }

    fun normalizeKey(raw: String?): String? {
        if (raw.isNullOrBlank()) return null
        return raw
            .replace('\u00A0', ' ')
            .replace(Regex("(?iu)\\bс\\s*/?\\s*н\\b"), "")
            .replace("№", "")
            .replace(Regex("\\s+"), "")
            .trim()
            .ifBlank { null }
    }

    private fun extractPrimaryKeyFromTitle(title: String, screen: String?): String? {
        if (title.isBlank()) return null
        return when (screen) {
            "complectation" -> {
                val complectation = Regex("(?iu)с\\s*/?\\s*н\\s*([\\p{L}0-9\\-]+)")
                    .find(title)
                    ?.groupValues
                    ?.getOrNull(1)
                normalizeKey(complectation)
            }
            "work_orders" -> {
                val number = Regex("№\\s*([\\p{L}0-9\\-]+)")
                    .find(title)
                    ?.groupValues
                    ?.getOrNull(1)
                if (!number.isNullOrBlank()) return normalizeKey(number)

                val plainNumber = Regex("(?u)\\b\\d{6,}\\b")
                    .find(title)
                    ?.value
                normalizeKey(plainNumber)
            }
            "events" -> {
                val eventLike = Regex("(?iu)\\b[\\p{L}]{2,6}\\s*\\d{3,}\\b")
                    .find(title)
                    ?.value
                if (!eventLike.isNullOrBlank()) return normalizeKey(eventLike)

                val number = Regex("№\\s*([\\p{L}0-9\\-]+)")
                    .find(title)
                    ?.groupValues
                    ?.getOrNull(1)
                normalizeKey(number)
            }
            else -> {
                val complectation = Regex("(?iu)с\\s*/?\\s*н\\s*([\\p{L}0-9\\-]+)")
                    .find(title)
                    ?.groupValues
                    ?.getOrNull(1)
                if (!complectation.isNullOrBlank()) return normalizeKey(complectation)
                val number = Regex("№\\s*([\\p{L}0-9\\-]+)")
                    .find(title)
                    ?.groupValues
                    ?.getOrNull(1)
                if (!number.isNullOrBlank()) return normalizeKey(number)
                val eventLike = Regex("(?iu)\\b[\\p{L}]{2,6}\\s*\\d{3,}\\b")
                    .find(title)
                    ?.value
                normalizeKey(eventLike)
            }
        }
    }

    private fun normalizeMessageHint(message: String?): String? {
        val raw = message?.trim().orEmpty()
        if (raw.isBlank()) return null
        val stripped = raw.substringAfter(":\n", raw).trim()
        return stripped.ifBlank { null }
    }

    private fun inferScreen(title: String, explicitScreen: String?): String? {
        val explicit = explicitScreen?.trim()?.lowercase()?.replace('-', '_')?.replace(' ', '_')
        if (!explicit.isNullOrBlank()) return explicit
        val t = title.lowercase()
        return when {
            t.contains("комплект") -> "complectation"
            t.contains("заказ-нар") || t.contains("заказнар") -> "work_orders"
            t.contains("событ") -> "events"
            else -> null
        }
    }

    private fun inferDocTypeLabel(title: String, screen: String?): String? {
        val t = title.lowercase()
        return when {
            t.contains("комплект") || screen == "complectation" -> "Комплектация"
            t.contains("заказ-нар") || t.contains("заказнар") || screen == "work_orders" -> "Заказ-наряд"
            t.contains("событ") || screen == "events" -> "Событие"
            else -> null
        }
    }
}
