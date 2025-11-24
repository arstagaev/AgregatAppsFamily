package com.tagaev.trrcrm.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SentMessageResponse(
    // common flags/ids
    @SerialName("status") val status: String,
    @SerialName("document") val document: String? = null
)