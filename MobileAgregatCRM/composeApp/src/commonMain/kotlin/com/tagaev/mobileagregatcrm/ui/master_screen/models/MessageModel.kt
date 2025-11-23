package com.tagaev.mobileagregatcrm.ui.master_screen.models

import com.tagaev.mobileagregatcrm.utils.getTimestamp
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.format
import kotlinx.datetime.format.byUnicodePattern
import kotlinx.datetime.toLocalDateTime
import kotlin.time.ExperimentalTime

// Your shared formatter (example)
val WorkOrderDateTimeFormat = LocalDateTime.Format {
    // Same style as your existing one, just an example
    byUnicodePattern("dd.MM.yyyy HH:mm:ss")
}

// 1) Convert Long -> LocalDateTime (in system time zone)
@OptIn(ExperimentalTime::class)
fun Long.toLocalDateTime(
    timeZone: TimeZone = TimeZone.currentSystemDefault()
): LocalDateTime {
    val instant = Instant.fromEpochMilliseconds(this)
    return instant.toLocalDateTime(timeZone)
}

// 2) Convert Long -> formatted String with your special LocalDateTime.Format
fun Long.toFormattedWorkOrderDateTime(
    timeZone: TimeZone = TimeZone.currentSystemDefault()
): String = this
    .toLocalDateTime(timeZone)
    .format(WorkOrderDateTimeFormat)

fun Long.toDateLabel(timeZone: TimeZone = TimeZone.currentSystemDefault()): String =
    this.toLocalDateTime(timeZone).format(WorkOrderDateTimeFormat)

data class MessageModel(
    val author: String,
    val text: String,
    val date: String = getTimestamp.toDateLabel()
)
