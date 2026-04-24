package com.tagaev.trrcrm.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Элемент ответа `task=getpermission` (массив пар). */
@Serializable
data class UserPermissionEntryDto(
    @SerialName("permission") val permission: String,
    @SerialName("value") val value: String,
)
