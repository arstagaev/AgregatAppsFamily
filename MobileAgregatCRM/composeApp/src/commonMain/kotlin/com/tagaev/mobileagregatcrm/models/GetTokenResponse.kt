package com.tagaev.mobileagregatcrm.models


import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class GetTokenResponse(
    @SerialName("accessapp")
    val accessapp: String,
    @SerialName("accessmoodle")
    val accessmoodle: String,
    @SerialName("token")
    val token: String,
    @SerialName("username")
    val username: String,
    @SerialName("ДатаРождения")
    val birthDate: String,
    @SerialName("Должность")
    val occupation: String,
    @SerialName("Подразделение")
    val department: String,
    @SerialName("ФИО")
    val fullName: String
)