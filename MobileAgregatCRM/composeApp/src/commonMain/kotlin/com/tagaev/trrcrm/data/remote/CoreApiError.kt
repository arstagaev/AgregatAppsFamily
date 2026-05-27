package com.tagaev.trrcrm.data.remote

import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.plugins.RedirectResponseException
import io.ktor.client.plugins.ServerResponseException

enum class CoreApiErrorKind {
    Unauthorized,
    Forbidden,
    NotFound,
    Validation,
    Server,
    Timeout,
    Network,
    Redirect,
    Unknown,
}

data class CoreApiFieldError(
    val field: String? = null,
    val message: String? = null,
    val code: String? = null,
)

data class CoreApiError(
    val statusCode: Int? = null,
    val kind: CoreApiErrorKind = CoreApiErrorKind.Unknown,
    val message: String = "",
)

class CoreApiException(
    val statusCode: Int,
    val url: String,
    val responseBody: String,
    val errorCode: String? = null,
    val errorMessage: String? = null,
    val fields: List<CoreApiFieldError> = emptyList(),
) : Exception("HTTP $statusCode $url | ${responseBody.take(500)}")

fun CoreApiException.normalizedErrorCode(): String {
    return errorCode
        ?: when (statusCode) {
            401 -> "unauthorized"
            403 -> "forbidden"
            404 -> "not_found"
            408 -> "timeout"
            422 -> "validation_error"
            in 500..599 -> "server_error"
            else -> "http_$statusCode"
        }
}

fun Throwable?.toCoreApiError(fallback: String): CoreApiError {
    if (this == null) return CoreApiError(message = fallback)
    val message = friendlyError(this, fallback)

    return when (this) {
        is CoreApiException -> CoreApiError(
            statusCode = statusCode,
            kind = statusToKind(statusCode),
            message = errorMessage?.ifBlank { message } ?: message
        )
        is RedirectResponseException -> CoreApiError(
            statusCode = this.response.status.value,
            kind = CoreApiErrorKind.Redirect,
            message = message
        )
        is ClientRequestException -> {
            val code = this.response.status.value
            CoreApiError(
                statusCode = code,
                kind = statusToKind(code),
                message = message
            )
        }
        is ServerResponseException -> {
            val code = this.response.status.value
            CoreApiError(
                statusCode = code,
                kind = statusToKind(code),
                message = message
            )
        }
        else -> {
            val lower = this.message.orEmpty().lowercase()
            val kind = when {
                "timeout" in lower || "timed out" in lower -> CoreApiErrorKind.Timeout
                "unknownhost" in lower || "unresolvedaddress" in lower || "failed to connect" in lower ||
                    "network" in lower -> CoreApiErrorKind.Network
                else -> CoreApiErrorKind.Unknown
            }
            CoreApiError(kind = kind, message = message)
        }
    }
}

private fun statusToKind(code: Int): CoreApiErrorKind = when (code) {
    401 -> CoreApiErrorKind.Unauthorized
    403 -> CoreApiErrorKind.Forbidden
    404 -> CoreApiErrorKind.NotFound
    422 -> CoreApiErrorKind.Validation
    in 500..599 -> CoreApiErrorKind.Server
    else -> CoreApiErrorKind.Unknown
}
