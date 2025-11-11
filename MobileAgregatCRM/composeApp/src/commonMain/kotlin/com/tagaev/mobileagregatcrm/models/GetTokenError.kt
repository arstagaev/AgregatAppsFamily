package com.tagaev.mobileagregatcrm.models


import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class GetTokenError(
    @SerialName("error")
    val error: String
)