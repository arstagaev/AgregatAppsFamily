package com.tagaev.trrcrm.data.remote

import io.ktor.client.plugins.*
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
                    Resource.Error(t, friendlyError(t, "Не удалось выполнить запрос"))
                }
                is RedirectResponseException -> {
                    Resource.Error(t, friendlyError(t, "Сервер временно недоступен"))
                }
                is ClientRequestException -> {
                    Resource.Error(t, friendlyError(t, "Ошибка запроса"))
                }
                is ServerResponseException -> {
                    Resource.Error(t, friendlyError(t, "Сервер временно недоступен"))
                }
                else -> {
                    val ex = (t as? Exception) ?: Exception(t.message)
                    Resource.Error(ex, friendlyError(t, "Не удалось выполнить запрос"))
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

    // Backend may also wrap an error/warning inside a single-element array:
    //   [{"error":"..."}]  or  [{"warning":"..."}]
    val arr = el as? kotlinx.serialization.json.JsonArray
    val singleObj = if (arr != null && arr.size == 1) arr.first() as? kotlinx.serialization.json.JsonObject else null
    val source = obj ?: singleObj

    // Soft error branches first:
    source?.get("warning")?.jsonPrimitive?.contentOrNull
        ?.takeIf { it.isNotBlank() }
        ?.let { warning ->
            // Backend sometimes returns {"warning":"Empty answers"} for empty lists.
            // Treat it as an empty successful response so UI shows notFoundText (not errorText).
            if (warning.equals("Empty answers", ignoreCase = true) && T::class == List::class) {
                @Suppress("UNCHECKED_CAST")
                return emptyList<Any?>() as T
            }
            throw WarningException(warning)
        }

    source?.get("error")?.jsonPrimitive?.contentOrNull
        ?.takeIf { it.isNotBlank() }
        ?.let { throw IllegalStateException(it) }

    // Safe to decode as the expected T now
    return json.decodeFromJsonElement(el)
}
