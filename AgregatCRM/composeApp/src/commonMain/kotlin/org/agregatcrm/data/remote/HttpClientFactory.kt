package org.agregatcrm.data.remote

import io.ktor.client.statement.request
import io.ktor.client.HttpClient
import io.ktor.client.plugins.DefaultRequest
import io.ktor.client.plugins.HttpRequestRetry
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.*
import io.ktor.client.plugins.observer.ResponseObserver
import io.ktor.client.request.header
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import io.ktor.client.plugins.logging.Logger
import org.agregatcrm.utils.HumanLogger

/**
 * Make a client with:
 * - Logging (requests + responses + bodies)
 * - ResponseObserver for status line
 * - DoubleReceive to safely read body for logs
 * - Timeouts, retries, JSON
 */
object HttpClientFactory {

    fun create(
        json: Json = defaultJson,
        logBodies: Boolean = true,
        // If you already choose engines per platform in each source set,
        // just call this without passing an engine here.
    ): HttpClient = HttpClient {
        // JSON
        install(ContentNegotiation) { json(json) }

        // Timeouts
        install(HttpTimeout) {
            requestTimeoutMillis = 60_000
            connectTimeoutMillis = 30_000
            socketTimeoutMillis  = 60_000
        }

        // Retries (idempotent GETs; backoff)
        install(HttpRequestRetry) {
            maxRetries = 2
            retryIf { request, response ->
                // retry on 5xx
                response.status.value >= 500
            }
            exponentialDelay()
        }

        // Default headers
        install(DefaultRequest) {
            header(HttpHeaders.Accept, ContentType.Application.Json)
            header(HttpHeaders.ContentType, ContentType.Application.Json)
            header(HttpHeaders.UserAgent, "KMP-CRM/1.0 (+ktor)")
        }
        install(Logging) {
            logger = HumanLogger()
            level = LogLevel.ALL
//            sanitizeHeader { header -> header.equals(HttpHeaders.Authorization, ignoreCase = true) }
        }

        // Status line & timing
        install(ResponseObserver) {
            onResponse { response ->
                org.agregatcrm.utils.Log.info("HTTP ${response.status.value} ${response.request.url}")
            }
        }

//        // Allow reading body more than once (so we can log it safely)
//        if (logBodies) {
//            install(BodyLoggerPlugin)
//        }
    }

    private fun HttpClient.sanitizeHeader(function: Any) {
        TODO("Not yet implemented")
    }

    val defaultJson = Json {
        ignoreUnknownKeys = true
        isLenient = true
        explicitNulls = false
    }
}
