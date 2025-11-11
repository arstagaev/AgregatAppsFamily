package com.tagaev.mobileagregatcrm.data.remote

import io.ktor.client.plugins.*
import io.ktor.client.statement.bodyAsText

suspend inline fun <T> resourceify(
    crossinline block: suspend () -> T
): Resource<T> =
    runCatching { block() }.fold(
        onSuccess = { data -> Resource.Success(data) },
        onFailure = { t ->
            when (t) {
                is RedirectResponseException -> {
                    Resource.Error(t, "Redirect error: ${t.response.status}")
                }
                is ClientRequestException -> {
                    val body = runCatching { t.response.bodyAsText().take(2000) }.getOrNull()
                    Resource.Error(t, "Client error ${t.response.status}: ${body ?: t.message}")
                }
                is ServerResponseException -> {
                    val body = runCatching { t.response.bodyAsText().take(2000) }.getOrNull()
                    Resource.Error(t, "Server error ${t.response.status}: ${body ?: t.message}")
                }
                else -> {
                    val ex = (t as? Exception) ?: Exception(t.message)
                    Resource.Error(ex, ex.message ?: "Unexpected error")
                }
            }
        }
    )