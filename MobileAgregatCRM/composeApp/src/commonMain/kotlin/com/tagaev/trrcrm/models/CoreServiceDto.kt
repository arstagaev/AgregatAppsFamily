package com.tagaev.trrcrm.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

@Serializable
data class CoreSessionBootstrapRequest(
    val full_name: String,
    val platform: String,
    val device_id: String,
    val fcm_token: String,
    val login: String? = null,
    val email: String? = null,
    val department: String? = null,
    val department_code: String? = null,
    val role: String? = null,
    val position: String? = null,
    val device_name: String? = null,
    val apns_token: String? = null,
    val app_version: String? = null,
    val corp: String? = null,
)

@Serializable
data class CoreSessionBootstrapResponse(
    @SerialName("session_id")
    val sessionId: String,
    val status: String? = null,
)

@Serializable
data class CoreSessionHeartbeatRequest(
    @SerialName("session_id")
    val sessionId: String,
    @SerialName("fcm_token")
    val fcmToken: String? = null,
    @SerialName("app_version")
    val appVersion: String? = null,
)

@Serializable
data class CoreSessionHeartbeatResponse(
    val status: String? = null,
)

@Serializable
data class CoreSessionLogoutRequest(
    @SerialName("session_id")
    val sessionId: String,
    @SerialName("deactivate_device_token")
    val deactivateDeviceToken: Boolean? = null,
)

@Serializable
data class CoreSessionLogoutResponse(
    val status: String? = null,
)

@Serializable
data class CoreDeviceRegisterRequest(
    val full_name: String,
    val platform: String,
    val device_id: String,
    val fcm_token: String,
    val device_name: String? = null,
    val apns_token: String? = null,
    val app_version: String? = null,
)

@Serializable
data class CoreDeviceRegisterResponse(
    val status: String? = null,
)

@Serializable
data class CoreResolveRecipientsRequest(
    val recipient_names: List<String>,
)

@Serializable
data class CoreResolveRecipientsResponse(
    val status: String? = null,
    val resolved: Int? = null,
    val total: Int? = null,
)

@Serializable
data class CoreNotificationIntentRequest(
    val source_system: String,
    val event_type: String,
    val title: String,
    val body: String,
    val recipient_names: List<String>,
    val dedupe_key: String? = null,
    val actor_full_name: String? = null,
    val payload: JsonObject? = null,
    val exclude_actor: Boolean? = null,
    val send_now: Boolean? = null,
)

@Serializable
data class CoreNotificationIntentResponse(
    val status: String? = null,
    val success: Int? = null,
    val failure: Int? = null,
    val message: String? = null,
)

