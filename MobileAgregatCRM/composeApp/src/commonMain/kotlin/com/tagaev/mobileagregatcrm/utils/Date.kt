package com.tagaev.mobileagregatcrm.utils

import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.format.FormatStringsInDatetimeFormats
import kotlinx.datetime.format.byUnicodePattern
import kotlinx.datetime.toInstant
import kotlin.time.ExperimentalTime

@OptIn(FormatStringsInDatetimeFormats::class)
val formatDDMMYYYY = LocalDateTime.Format {
    byUnicodePattern("dd.MM.yyyy")
}

@OptIn(ExperimentalTime::class)
fun formatRelativeWorkDate(raw: String?): String {
    if (raw.isNullOrBlank()) return ""

    return runCatching {
        // Expected format: "24.09.2025 15:18:04"
        val parts = raw.trim().split(" ")
        if (parts.size != 2) return raw

        val dateParts = parts[0].split(".")
        val timeParts = parts[1].split(":")

        if (dateParts.size != 3 || timeParts.size != 3) return raw

        val day = dateParts[0].toInt()
        val month = dateParts[1].toInt()
        val year = dateParts[2].toInt()

        val hour = timeParts[0].toInt()
        val minute = timeParts[1].toInt()
        val second = timeParts[2].toInt()

        val messageDateTime = LocalDateTime(year, month, day, hour, minute, second)
        val timeZone = TimeZone.currentSystemDefault()
        val messageInstant = messageDateTime.toInstant(timeZone)

        // both in millis
        val nowMillis = getTimestamp          // Long (millis)
        val messageMillis = messageInstant.toEpochMilliseconds()

        // If the message is from the future, just return the raw date
        if (messageMillis > nowMillis) {
            return raw
        }

        val diffMillis = nowMillis - messageMillis
        val diffSeconds = diffMillis / 1000
        println("${diffSeconds} = ${diffMillis} / 1000")
        if (diffSeconds < 60) return "только что"

        val diffMinutes = diffSeconds / 60
        if (diffMinutes < 60) return "$diffMinutes мин. назад"

        val diffHours = diffMinutes / 60
        if (diffHours < 24) return "$diffHours ч. назад"

        val diffDays = diffHours / 24
        if (diffDays < 30) return "$diffDays дн. назад"

        val diffMonths = diffDays / 30
        if (diffMonths < 12) return "$diffMonths мес. назад"

        val diffYears = diffMonths / 12
        return "$diffYears г. назад"
    }.getOrElse {
        raw
    }
}
