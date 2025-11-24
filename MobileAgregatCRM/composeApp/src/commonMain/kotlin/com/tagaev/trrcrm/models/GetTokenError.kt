package com.tagaev.trrcrm.models


import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class GetTokenError(
    @SerialName("error")
    val error: String
)