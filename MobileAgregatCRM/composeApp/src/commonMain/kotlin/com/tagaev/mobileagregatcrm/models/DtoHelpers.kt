package org.agregatcrm.models

import com.tagaev.mobileagregatcrm.models.EventItemDto
import com.tagaev.mobileagregatcrm.models.UserRowDto

internal fun String?.yesNoBoolean(): Boolean? = when (this?.trim()?.lowercase()) {
    "да", "yes", "y", "true", "1" -> true
    "нет", "no", "n", "false", "0" -> false
    else -> null
}

// Example extension properties
val EventItemDto.isPosted: Boolean? get() = posted.yesNoBoolean()
val EventItemDto.isRead: Boolean? get() = readMark.yesNoBoolean()
val UserRowDto.isResponsible: Boolean? get() = responsible.yesNoBoolean()

private val leadingNoise = Regex("^[\\uFEFF\\u200B\\u200E\\u200F\\u00A0\\s]+")
fun String.cleanJsonStart(): String = replace(leadingNoise, "")