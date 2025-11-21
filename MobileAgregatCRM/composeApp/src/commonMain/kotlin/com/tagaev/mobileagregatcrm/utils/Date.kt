package com.tagaev.mobileagregatcrm.utils

import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.format.FormatStringsInDatetimeFormats
import kotlinx.datetime.format.byUnicodePattern

@OptIn(FormatStringsInDatetimeFormats::class)
val formatDDMMYYYY = LocalDateTime.Format {
    byUnicodePattern("dd.MM.yyyy")
}