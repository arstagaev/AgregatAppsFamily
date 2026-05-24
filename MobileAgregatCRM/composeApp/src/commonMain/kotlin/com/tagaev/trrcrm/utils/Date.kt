package com.tagaev.trrcrm.utils

import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.format.FormatStringsInDatetimeFormats
import kotlinx.datetime.format.byUnicodePattern
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import kotlin.time.ExperimentalTime

@OptIn(FormatStringsInDatetimeFormats::class)
val formatDDMMYYYY = LocalDateTime.Format {
    byUnicodePattern("dd.MM.yyyy")
}

/**
 * Formats a server timestamp into "yyyy.MM.dd HH:mm:ss".
 * Accepts:
 *  - "dd.MM.yyyy HH:mm:ss"
 *  - ISO-8601 with offset/Z (e.g. "2026-05-07T15:18:04Z", "2026-05-07T15:18:04+03:00")
 *  - "yyyy-MM-dd HH:mm:ss" / "yyyy-MM-ddTHH:mm:ss"
 * Returns the original string if none matches.
 */
@OptIn(ExperimentalTime::class)
fun formatFeedTimestamp(raw: String?): String {
    if (raw.isNullOrBlank()) return ""
    val trimmed = raw.trim()

    runCatching {
        val parts = trimmed.split(" ")
        if (parts.size == 2) {
            val date = parts[0].split(".")
            val time = parts[1].split(":")
            if (date.size == 3 && time.size in 2..3) {
                val day = date[0].toInt()
                val month = date[1].toInt()
                val year = date[2].toInt()
                val hour = time[0].toInt()
                val minute = time[1].toInt()
                val second = time.getOrNull(2)?.toInt() ?: 0
                return formatYmdHms(year, month, day, hour, minute, second)
            }
        }
    }

    runCatching {
        val instant = Instant.parse(trimmed)
        val ldt = instant.toLocalDateTime(TimeZone.currentSystemDefault())
        return formatYmdHms(ldt.year, ldt.monthNumber, ldt.dayOfMonth, ldt.hour, ldt.minute, ldt.second)
    }

    runCatching {
        val ldt = LocalDateTime.parse(trimmed.replace(' ', 'T'))
        return formatYmdHms(ldt.year, ldt.monthNumber, ldt.dayOfMonth, ldt.hour, ldt.minute, ldt.second)
    }

    return trimmed
}

private fun formatYmdHms(year: Int, month: Int, day: Int, hour: Int, minute: Int, second: Int): String {
    val y = year.toString().padStart(4, '0')
    val mo = month.toString().padStart(2, '0')
    val d = day.toString().padStart(2, '0')
    val h = hour.toString().padStart(2, '0')
    val mi = minute.toString().padStart(2, '0')
    val s = second.toString().padStart(2, '0')
    return "$y.$mo.$d $h:$mi:$s"
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
