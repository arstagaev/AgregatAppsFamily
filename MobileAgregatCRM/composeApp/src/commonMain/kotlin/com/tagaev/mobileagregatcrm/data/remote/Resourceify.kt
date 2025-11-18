package com.tagaev.mobileagregatcrm.data.remote

import io.ktor.client.plugins.*
import io.ktor.client.statement.bodyAsText
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.jsonPrimitive

class WarningException(message: String) : Exception(message)

suspend inline fun <T> resourceify(
    crossinline block: suspend () -> T
): Resource<T> =
    runCatching { block() }.fold(
        onSuccess = { data -> Resource.Success(data) },
        onFailure = { t ->
            when (t) {
                is WarningException -> {
                    // soft-fail; UI can treat this differently
                    Resource.Error(t, t.message ?: "Warning")
                }
                is RedirectResponseException -> {
                    Resource.Error(t, "Redirect error: ${t.response.status}")
                }
                is ClientRequestException -> {
                    val body = runCatching { t.response.bodyAsText().take(12) }.getOrNull()
                    Resource.Error(t, "Client error ${t.response.status}: ${body ?: t.message}")
                }
                is ServerResponseException -> {
                    val body = runCatching { t.response.bodyAsText().take(12) }.getOrNull()
                    Resource.Error(t, "Server error ${t.response.status}: ${body ?: t.message}")
                }
                else -> {
                    val ex = (t as? Exception) ?: Exception(t.message)
                    Resource.Error(ex, ex.message ?: "Unexpected error")
                }
            }
        }
    )


// commonMain
//class WarningException(message: String) : Exception(message)

inline fun String.cleanJsonStart(): String =
    this.trimStart('\uFEFF', ' ', '\n', '\r', '\t')

inline fun <reified T> decodeOrWarning(json: Json, raw: String): T {
    val cleaned = raw.cleanJsonStart()
    val el = json.parseToJsonElement(cleaned) // never assumes array/object
    val obj = el as? kotlinx.serialization.json.JsonObject

    // Soft error branches first:
    obj?.get("warning")?.jsonPrimitive?.contentOrNull
        ?.takeIf { it.isNotBlank() }
        ?.let { throw WarningException(it) }

    obj?.get("error")?.jsonPrimitive?.contentOrNull
        ?.takeIf { it.isNotBlank() }
        ?.let { throw IllegalStateException(it) }

    // Safe to decode as the expected T now
    return json.decodeFromJsonElement(el)
}
