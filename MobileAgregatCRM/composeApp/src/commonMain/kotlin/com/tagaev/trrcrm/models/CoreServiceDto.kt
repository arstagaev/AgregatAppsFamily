package com.tagaev.trrcrm.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

@Serializable
data class CoreSessionBootstrapRequest(
    val full_name: String,
    val platform: String,
    val device_id: String,
    val fcm_token: String? = null,
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
    val config: CoreSessionConfig? = null,
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
    val config: CoreSessionConfig? = null,
)

@Serializable
data class CoreSessionConfig(
    val flags: Map<String, Boolean>? = null,
    /** Opaque string from server — do not parse as date/number. */
    val revision: String? = null,
)

@Serializable
data class MobileFeatureTogglesResponse(
    val status: String? = null,
    val flags: Map<String, Boolean>? = null,
    /** Opaque string from server — do not parse as date/number. */
    val revision: String? = null,
    @SerialName("server_time")
    val serverTime: String? = null,
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
    val subtitle: String? = null,
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

@Serializable
data class CoreNotificationsFeedRequest(
    @SerialName("session_id")
    val sessionId: String,
    val limit: Int = 25,
    val page: Int? = 1,
    val cursor: String? = null,
    @SerialName("search_query")
    val searchQuery: String? = null,
    @SerialName("status_filter")
    val statusFilter: String = "all",
)

@Serializable
data class CoreNotificationsUnreadCountRequest(
    @SerialName("session_id")
    val sessionId: String,
)

@Serializable
data class CoreNotificationsUnreadCountResponse(
    val status: String? = null,
    @SerialName("unread_count")
    val unreadCount: Int = 0,
    @SerialName("server_time")
    val serverTime: String? = null,
)

@Serializable
data class CoreNotificationFeedItem(
    val id: String,
    @SerialName("created_at")
    val createdAt: String,
    @SerialName("expires_at")
    val expiresAt: String? = null,
    val title: String,
    @SerialName("message_text")
    val messageText: String? = null,
    val screen: String? = null,
    @SerialName("search_key")
    val searchKey: String? = null,
    @SerialName("search_query_type")
    val searchQueryType: String? = null,
    @SerialName("doc_number")
    val docNumber: String? = null,
    @SerialName("doc_title")
    val docTitle: String? = null,
    val status: String = "unread",
    @SerialName("read_at")
    val readAt: String? = null,
)

@Serializable
data class CoreNotificationsFeedResponse(
    val status: String? = null,
    val items: List<CoreNotificationFeedItem> = emptyList(),
    @SerialName("next_cursor")
    val nextCursor: String? = null,
    val page: Int? = null,
    val limit: Int? = null,
    @SerialName("total_count")
    val totalCount: Int? = null,
    @SerialName("has_next")
    val hasNext: Boolean? = null,
    @SerialName("unread_count")
    val unreadCount: Int = 0,
)

@Serializable
data class CoreNotificationStatusUpdateRequest(
    @SerialName("session_id")
    val sessionId: String,
    @SerialName("notification_id")
    val notificationId: Long,
    val status: String,
    val source: String? = null,
)

@Serializable
data class CoreNotificationStatusUpdateResponse(
    val status: String? = null,
    @SerialName("notification_id")
    val notificationId: String? = null,
    @SerialName("new_status")
    val newStatus: String? = null,
    @SerialName("read_at")
    val readAt: String? = null,
)

@Serializable
data class CoreNotificationsReadAllRequest(
    @SerialName("session_id")
    val sessionId: String,
)

@Serializable
data class CoreNotificationsReadAllResponse(
    val status: String? = null,
    val updated: Int = 0,
)

@Serializable
data class HealthResponse(
    val status: String? = null,
    val service: String? = null,
)

@Serializable
data class PushFeatureToggleGetResponse(
    val status: String? = null,
    val enabled: Boolean? = null,
)

@Serializable
data class PushFeatureToggleSetRequest(
    val enabled: Boolean,
)

@Serializable
data class PushFeatureToggleSetResponse(
    val status: String? = null,
    val enabled: Boolean? = null,
)

@Serializable
data class CoreDeviceMuteStateRequest(
    @SerialName("session_id")
    val sessionId: String,
)

@Serializable
data class CoreDeviceMuteUpdateRequest(
    @SerialName("session_id")
    val sessionId: String,
    @SerialName("mute_all")
    val muteAll: Boolean? = null,
    @SerialName("document_type")
    val documentType: String? = null,
    val muted: Boolean? = null,
)

@Serializable
data class CoreDeviceMuteStateResponse(
    val status: String? = null,
    @SerialName("session_id")
    val sessionId: String? = null,
    @SerialName("device_token_id")
    val deviceTokenId: Long? = null,
    val platform: String? = null,
    @SerialName("device_id")
    val deviceId: String? = null,
    @SerialName("mute_all")
    val muteAll: Boolean = false,
    @SerialName("muted_document_types")
    val mutedDocumentTypes: List<String> = emptyList(),
    @SerialName("updated_at")
    val updatedAt: String? = null,
)
