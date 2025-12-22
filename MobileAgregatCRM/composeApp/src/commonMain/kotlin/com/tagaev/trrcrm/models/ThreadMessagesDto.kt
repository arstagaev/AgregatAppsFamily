package com.tagaev.trrcrm.models

import kotlinx.serialization.Serializable

@Serializable
data class ThreadMessageRequest(
    val doc_id: String,
    val doc_title: String,
    val author_name: String,
    val message_text: String,
    val recipient_names: List<String>,
)

@Serializable
data class ThreadMessageResponse(
    val status: String,
    val success: Int,
    val failure: Int,
    val recipients: Int? = null,
)
