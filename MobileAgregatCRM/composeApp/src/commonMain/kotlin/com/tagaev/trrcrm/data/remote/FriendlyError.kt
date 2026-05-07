package com.tagaev.trrcrm.data.remote

import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.plugins.RedirectResponseException
import io.ktor.client.plugins.ServerResponseException

/**
 * User-visible mapping of low-level network/HTTP errors.
 *
 * Goal: never surface request URLs, tokens, or platform-specific error dumps
 * (e.g. iOS NSURLErrorDomain bodies) to the end user. Always return a short
 * Russian phrase suitable for an alert/snackbar.
 */

private const val MSG_NO_INTERNET = "Нет соединения с интернетом."
private const val MSG_TIMEOUT = "Превышено время ожидания. Попробуйте ещё раз."
private const val MSG_HOST_NOT_FOUND = "Сервер недоступен. Проверьте подключение."
private const val MSG_CANNOT_CONNECT = "Не удалось подключиться к серверу."
private const val MSG_CONNECTION_LOST = "Соединение потеряно. Повторите попытку."
private const val MSG_SSL = "Ошибка защищённого соединения."
private const val MSG_FORBIDDEN = "Доступ запрещён. Войдите заново."
private const val MSG_NOT_FOUND = "Не найдено."
private const val MSG_SERVER_DOWN = "Сервер временно недоступен."
private const val MSG_BAD_REQUEST = "Ошибка запроса. Попробуйте позже."

private val UNSAFE_DUMP_MARKERS = listOf(
    "nsurlerror",
    "nsurldomain",
    "nserror",
    "kcfstreamerror",
    "kcfnetwork",
    "urlsessiontask",
    "exception in http request",
    "userinfo=",
    "nsunderlyingerror",
    "nslocalizeddescription",
)

private val URL_REGEX = Regex("""https?://\S+""")
private val TOKEN_REGEX = Regex("""token=[^\s&"']+""", RegexOption.IGNORE_CASE)

/**
 * Strip URLs and tokens from a raw message. Returns "" if the message
 * looks like an iOS NSError dump (we can't safely keep any part of it).
 */
fun sanitizeMessage(raw: String?): String {
    if (raw.isNullOrBlank()) return ""
    val lower = raw.lowercase()
    if (UNSAFE_DUMP_MARKERS.any { it in lower }) return ""
    return raw
        .replace(URL_REGEX, "")
        .replace(TOKEN_REGEX, "")
        .trim()
}

/**
 * Pick a user-friendly Russian message for the given throwable, falling back
 * to [fallback] when no specific mapping applies. The result NEVER contains
 * URLs, tokens or NSError dumps.
 */
fun friendlyError(throwable: Throwable?, fallback: String): String {
    if (throwable == null) return fallback

    if (throwable is WarningException) {
        val safe = sanitizeMessage(throwable.message)
        return safe.ifBlank { fallback }
    }

    // Server-provided error strings reach us as IllegalStateException via
    // decodeOrWarning (e.g. {"error":"..."} or [{"error":"..."}]).
    // Show them verbatim (after sanitizing tokens/URLs) so messages like
    // "Too many non-authorizeded requests. Waiting 300 seconds" are not
    // misclassified by the generic HTTP-code regex below.
    if (throwable is IllegalStateException) {
        val msg = throwable.message.orEmpty()
        val httpStart = HTTP_PREFIX_REGEX.find(msg)?.groupValues?.getOrNull(1)?.toIntOrNull()
        if (httpStart != null) return mapHttpStatus(httpStart, fallback)
        val safe = sanitizeMessage(msg)
        return safe.ifBlank { fallback }
    }

    when (throwable) {
        is RedirectResponseException -> return MSG_SERVER_DOWN
        is ClientRequestException -> return mapHttpStatus(throwable.response.status.value, fallback)
        is ServerResponseException -> return mapHttpStatus(throwable.response.status.value, fallback)
    }

    val raw = throwable.message.orEmpty()
    val lower = raw.lowercase()

    when {
        "-1009" in lower ||
            "appears to be offline" in lower ||
            "not connected to the internet" in lower -> return MSG_NO_INTERNET

        "-1001" in lower ||
            "request timed out" in lower ||
            "sockettimeout" in lower ||
            "timed out" in lower ||
            "timeout" in lower -> return MSG_TIMEOUT

        "-1003" in lower ||
            "cannot find host" in lower ||
            "unknownhost" in lower ||
            "unresolvedaddress" in lower -> return MSG_HOST_NOT_FOUND

        "-1004" in lower ||
            "could not connect" in lower ||
            "cannot connect to host" in lower ||
            "connectexception" in lower ||
            "connection refused" in lower ||
            "failed to connect" in lower -> return MSG_CANNOT_CONNECT

        "-1005" in lower ||
            "network connection was lost" in lower ||
            "network is unreachable" in lower -> return MSG_CONNECTION_LOST

        "-1200" in lower ||
            "ssl" in lower ||
            "tls" in lower ||
            "certificate" in lower -> return MSG_SSL
    }

    val httpFromMessage = HTTP_CODE_IN_CONTEXT_REGEX.find(raw)
        ?.groupValues
        ?.getOrNull(1)
        ?.toIntOrNull()
    if (httpFromMessage != null) {
        return mapHttpStatus(httpFromMessage, fallback)
    }

    val safe = sanitizeMessage(raw)
    return safe.ifBlank { fallback }
}

private val HTTP_PREFIX_REGEX = Regex("""^\s*HTTP\s+(\d{3})""", RegexOption.IGNORE_CASE)
private val HTTP_CODE_IN_CONTEXT_REGEX = Regex(
    """(?:HTTP|status|code)[\s:=]+([3-5]\d{2})""",
    RegexOption.IGNORE_CASE,
)

private fun mapHttpStatus(code: Int, fallback: String): String = when (code) {
    in 500..599 -> MSG_SERVER_DOWN
    401, 403 -> MSG_FORBIDDEN
    404 -> MSG_NOT_FOUND
    in 400..499 -> MSG_BAD_REQUEST
    in 300..399 -> MSG_SERVER_DOWN
    else -> fallback
}
