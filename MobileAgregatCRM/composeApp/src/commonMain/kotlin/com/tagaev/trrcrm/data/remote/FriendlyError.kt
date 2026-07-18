package com.tagaev.trrcrm.data.remote

import com.tagaev.trrcrm.ui.i18n.tr
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

private fun msgNoInternet() = tr("error_net_soedineniya_s_internetom")
private fun msgTimeout() = tr("error_prevysheno_vremya_ozhidaniya_poprobuyte_esche_raz")
private fun msgHostNotFound() = tr("error_server_nedostupen_proverte_podklyuchenie")
private fun msgCannotConnect() = tr("error_ne_udalos_podklyuchitsya_k_serveru")
private fun msgConnectionLost() = tr("error_soedinenie_poteryano_povtorite_popytku")
private fun msgSsl() = tr("error_oshibka_zaschischennogo_soedineniya")
private fun msgForbidden() = tr("error_dostup_zapreschen_voydite_zanovo")
private fun msgNotFound() = tr("error_ne_naydeno")
private fun msgServerDown() = tr("error_server_vremenno_nedostupen")
private fun msgBadRequest() = tr("error_oshibka_zaprosa_poprobuyte_pozzhe")
private fun msgGeneric() = tr("error_proizoshla_oshibka")

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
private val HOST_PATH_REGEX = Regex(
    """\b[\w.-]+\.(?:ru|com|net|org|io)(?::\d+)?(?:/\S*)?""",
    RegexOption.IGNORE_CASE,
)
private val IPV4_REGEX = Regex("""\b\d{1,3}(?:\.\d{1,3}){3}(?::\d+)?(?:/\S*)?""")
private val TOKEN_REGEX = Regex("""token=[^\s&"']+""", RegexOption.IGNORE_CASE)
private val KTOR_REQUEST_REGEX = Regex(
    """(?i)(client request|server response|request)\s*\([^)]*\)""",
)
private val PROJECT_HOST_MARKERS = listOf(
    "agregatka",
    "trrservice",
    "agrapp",
)

private val TECHNICAL_REMAINS_REGEX = Regex(
    """(?i)(https?://|\b\d{1,3}(?:\.\d{1,3}){3}\b|[\w.-]+\.(?:ru|com|net|org|io)\b|client request|server response)""",
)

/**
 * Strip URLs and tokens from a raw message. Returns "" if the message
 * looks like an iOS NSError dump (we can't safely keep any part of it).
 */
fun sanitizeMessage(raw: String?): String {
    if (raw.isNullOrBlank()) return ""
    val lower = raw.lowercase()
    if (UNSAFE_DUMP_MARKERS.any { it in lower }) return ""
    if (PROJECT_HOST_MARKERS.any { it in lower }) return ""
    val cleaned = stripKnownSensitiveFragments(raw)
    if (cleaned.isBlank()) return ""
    if (looksTechnical(cleaned)) return ""
    return cleaned
}

fun userFacingMessage(raw: String?, fallback: String = msgGeneric()): String {
    val sanitized = sanitizeMessage(raw)
    return sanitized.ifBlank { fallback }
}

private fun stripKnownSensitiveFragments(raw: String): String =
    raw
        .replace(URL_REGEX, "")
        .replace(HOST_PATH_REGEX, "")
        .replace(IPV4_REGEX, "")
        .replace(TOKEN_REGEX, "")
        .replace(KTOR_REQUEST_REGEX, "")
        .replace(Regex("""\s{2,}"""), " ")
        .trim()
        .trim('(', ')', '|', '-', ':', ';', ',')

private fun looksTechnical(value: String): Boolean {
    val lower = value.lowercase()
    if (TECHNICAL_REMAINS_REGEX.containsMatchIn(value)) return true
    if (PROJECT_HOST_MARKERS.any { it in lower }) return true
    if (HTTP_PREFIX_REGEX.containsMatchIn(value)) return true
    return false
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
        is RedirectResponseException -> return msgServerDown()
        is CoreApiException -> {
            val backendMessage = sanitizeMessage(throwable.errorMessage)
            if (backendMessage.isNotBlank()) return backendMessage
            return mapHttpStatus(throwable.statusCode, fallback)
        }
        is ClientRequestException -> return mapHttpStatus(throwable.response.status.value, fallback)
        is ServerResponseException -> return mapHttpStatus(throwable.response.status.value, fallback)
    }

    val raw = throwable.message.orEmpty()
    val lower = raw.lowercase()

    when {
        "-1009" in lower ||
            "appears to be offline" in lower ||
            "not connected to the internet" in lower -> return msgNoInternet()

        "-1001" in lower ||
            "request timed out" in lower ||
            "sockettimeout" in lower ||
            "timed out" in lower ||
            "timeout" in lower -> return msgTimeout()

        "-1003" in lower ||
            "cannot find host" in lower ||
            "unknownhost" in lower ||
            "unresolvedaddress" in lower -> return msgHostNotFound()

        "-1004" in lower ||
            "could not connect" in lower ||
            "cannot connect to host" in lower ||
            "connectexception" in lower ||
            "connection refused" in lower ||
            "failed to connect" in lower -> return msgCannotConnect()

        "-1005" in lower ||
            "network connection was lost" in lower ||
            "network is unreachable" in lower -> return msgConnectionLost()

        "-1200" in lower ||
            "ssl" in lower ||
            "tls" in lower ||
            "certificate" in lower -> return msgSsl()
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
    in 500..599 -> msgServerDown()
    401, 403 -> msgForbidden()
    404 -> msgNotFound()
    in 400..499 -> msgBadRequest()
    in 300..399 -> msgServerDown()
    else -> fallback
}
