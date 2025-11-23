package com.tagaev.mobileagregatcrm.data.remote.models


import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class GetRolesResponse(
    @SerialName("roles")
    val roles: List<String>,
    @SerialName("Пользователь")
    val пользователь: String? = null,
    @SerialName("ПользовательGUID")
    val пользовательGUID: String? = null,
    @SerialName("Сотрудник")
    val сотрудник: String? = null,
    @SerialName("СотрудникGUID")
    val сотрудникGUID: String? = null
)