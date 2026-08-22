package com.tagaev.trrcrm.data.remote

import com.tagaev.trrcrm.models.ImageMediatorCanUploadResponse
import com.tagaev.trrcrm.models.MAX_PHOTOS_PER_DOCUMENT_PER_APP_RUN
import com.tagaev.trrcrm.ui.i18n.tr
import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.plugins.ServerResponseException
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive

class ImageMediatorException(
    val statusCode: Int,
    val url: String,
    val responseBody: String,
    val errorCode: String? = parseImageMediatorErrorCode(responseBody),
    val retryAfterSeconds: Long? = null,
) : Exception("HTTP $statusCode")

internal fun parseImageMediatorErrorCode(raw: String): String? {
    val cleaned = raw.cleanJsonStart()
    if (cleaned.isBlank()) return null
    val el = runCatching { HttpClientFactory.defaultJson.parseToJsonElement(cleaned) }.getOrNull() ?: return null
    val obj = el as? JsonObject ?: return null
    obj["code"]?.jsonPrimitive?.contentOrNull?.takeIf { it.isNotBlank() }?.let { return it }
    return when (val detail = obj["detail"]) {
        is JsonPrimitive -> detail.contentOrNull?.takeIf { it.isNotBlank() }
        is JsonObject -> detail["code"]?.jsonPrimitive?.contentOrNull?.takeIf { it.isNotBlank() }
        else -> null
    }
}

fun imageMediatorErrorMessage(
    statusCode: Int,
    fallback: String = tr("error_ne_udalos_otpravit_foto"),
    folderLimit: Int? = null,
    errorCode: String? = null,
): String {
    val code = errorCode?.trim().orEmpty()
    return when {
        code == "unsupported_api_contract" -> tr("upload_unsupported_api_contract")
        code == "ambiguous_document_folder" -> tr("upload_ambiguous_document_folder")
        code == "legacy_document_identity_incomplete" -> tr("upload_legacy_identity_incomplete")
        statusCode == 400 -> tr("error_nekorrektnye_dannye_ili_format_izobrazheniya")
        statusCode == 401 -> tr("error_sessiya_istekla_voydite_zanovo")
        statusCode == 404 -> tr("upload_folder_unavailable")
        statusCode == 409 -> tr("upload_idempotency_conflict")
        statusCode == 413 -> tr("error_fayl_slishkom_bolshoy")
        statusCode == 429 -> tr("upload_rate_limited")
        statusCode == 503 -> tr("error_server_zanyat_povtorite_cherez_neskolko_sekund")
        statusCode in 500..599 -> tr("upload_service_unavailable")
        else -> fallback
    }
}

fun Throwable?.toImageMediatorError(
    fallback: String,
    folderLimit: Int? = null,
): String {
    if (this == null) return fallback
    return when (this) {
        is ImageMediatorException -> imageMediatorErrorMessage(
            statusCode = statusCode,
            fallback = fallback,
            folderLimit = folderLimit,
            errorCode = errorCode,
        )
        is ClientRequestException -> imageMediatorErrorMessage(
            statusCode = response.status.value,
            fallback = friendlyError(this, fallback),
            folderLimit = folderLimit,
        )
        is ServerResponseException -> imageMediatorErrorMessage(
            statusCode = response.status.value,
            fallback = friendlyError(this, fallback),
            folderLimit = folderLimit,
        )
        else -> friendlyError(this, fallback)
    }
}

/**
 * Map can-upload HTTP 200 + allowed=false (or missing folder) to a clear user message.
 * Priority: folder unavailable → document full (remaining == 0) → generic.
 */
fun canUploadBlockedMessage(response: ImageMediatorCanUploadResponse): String {
    if (!response.folderFound) {
        return tr("upload_folder_unavailable")
    }
    val remaining = response.limits?.effectiveRemaining()
    val maxPhotos = response.limits?.effectiveMaxPhotos() ?: 500
    if (!response.allowed) {
        return when {
            remaining != null && remaining <= 0 -> tr("upload_doc_full", maxPhotos)
            else -> tr("upload_unavailable_now")
        }
    }
    if (remaining != null && remaining <= 0) {
        return tr("upload_doc_full", maxPhotos)
    }
    return tr("upload_unavailable_now")
}

fun uploadSessionExhaustedMessage(): String =
    tr("upload_session_exhausted", MAX_PHOTOS_PER_DOCUMENT_PER_APP_RUN)
