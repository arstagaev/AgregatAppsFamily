package com.tagaev.mobileagregatcrm.data.remote

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
import com.tagaev.mobileagregatcrm.utils.HumanLogger
import com.tagaev.mobileagregatcrm.utils.Log

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
        loggingEnabled: Boolean = true,
        logBodies: Boolean = false,
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
        if (loggingEnabled) {
            install(Logging) {
                logger = HumanLogger()
                level = LogLevel.ALL // set to LogLevel.NONE if you prefer keeping the plugin installed but silent
            }
            // Status line & timing
            install(ResponseObserver) {
                onResponse { response ->
                    Log.info("HTTP ${response.status.value} ${response.request.url}")
                }
            }
        }

//        // Allow reading body more than once (so we can log it safely)
//        if (logBodies) {
//            install(BodyLoggerPlugin)
//        }
    }

    fun createNoLogs(json: Json = defaultJson): HttpClient =
        create(json = json, loggingEnabled = false, logBodies = false)

    private fun HttpClient.sanitizeHeader(function: Any) {
        TODO("Not yet implemented")
    }

    val defaultJson = Json {
        ignoreUnknownKeys = true
        isLenient = true
        explicitNulls = false
    }
}
