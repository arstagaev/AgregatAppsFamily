package com.tagaev.trrcrm.data.remote

import com.tagaev.trrcrm.models.ImageMediatorCanUploadResponse
import com.tagaev.trrcrm.models.MAX_PHOTOS_PER_DOCUMENT_PER_APP_RUN
import com.tagaev.trrcrm.ui.i18n.tr
import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.plugins.ServerResponseException

class ImageMediatorException(
    val statusCode: Int,
    val url: String,
    val responseBody: String,
) : Exception("HTTP $statusCode")

fun imageMediatorErrorMessage(
    statusCode: Int,
    fallback: String = tr("error_ne_udalos_otpravit_foto"),
    folderLimit: Int? = null,
): String =
    when (statusCode) {
        400 -> tr("error_nekorrektnye_dannye_ili_format_izobrazheniya")
        401 -> tr("error_sessiya_istekla_voydite_zanovo")
        404 -> tr("upload_folder_unavailable")
        409 -> tr("upload_idempotency_conflict")
        413 -> tr("error_fayl_slishkom_bolshoy")
        429 -> tr("upload_doc_full", folderLimit ?: 500)
        503 -> tr("error_server_zanyat_povtorite_cherez_neskolko_sekund")
        in 500..599 -> tr("upload_service_unavailable")
        else -> fallback
    }

fun Throwable?.toImageMediatorError(
    fallback: String,
    folderLimit: Int? = null,
): String {
    if (this == null) return fallback
    return when (this) {
        is ImageMediatorException -> imageMediatorErrorMessage(statusCode, fallback, folderLimit)
        is ClientRequestException -> imageMediatorErrorMessage(response.status.value, friendlyError(this, fallback), folderLimit)
        is ServerResponseException -> imageMediatorErrorMessage(response.status.value, friendlyError(this, fallback), folderLimit)
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
