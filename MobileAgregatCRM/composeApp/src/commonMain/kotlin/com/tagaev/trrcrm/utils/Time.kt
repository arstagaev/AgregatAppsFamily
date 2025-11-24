package com.tagaev.trrcrm.utils

import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.format
import kotlinx.datetime.format.FormatStringsInDatetimeFormats
import kotlinx.datetime.format.byUnicodePattern
import kotlinx.datetime.toLocalDateTime
import kotlin.time.ExperimentalTime
import kotlin.time.Instant


@OptIn(ExperimentalTime::class)
val getTimestamp: Long = kotlin.time.Clock.System.now().toEpochMilliseconds()


@OptIn(ExperimentalTime::class)
fun formatHmsFromEpoch(ms: Long, zone: TimeZone = TimeZone.currentSystemDefault()): String {
    val t = Instant.fromEpochMilliseconds(ms).toLocalDateTime(zone).time
    // "HH:mm:ss"
    return t.format(LocalTime.Formats.ISO)
}

// Pattern-based (Java/Unicode style)
//@OptIn(FormatStringsInDatetimeFormats::class)
//private val HmsFormat = LocalTime.Format { byUnicodePattern("HH:mm:ss") }

@OptIn(ExperimentalTime::class, FormatStringsInDatetimeFormats::class)
fun getTimestampWithFormat(ms: Long, zone: TimeZone = TimeZone.currentSystemDefault(), format: String = "HH:mm:ss"): String {
    val t = Instant.fromEpochMilliseconds(ms).toLocalDateTime(zone).time
    return t.format(LocalTime.Format { byUnicodePattern(format) })
}