package com.tagaev.trrcrm.data.remote

import io.ktor.client.statement.request
import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.plugins.DefaultRequest
import io.ktor.client.plugins.HttpRequestRetry
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.*
import io.ktor.client.plugins.observer.ResponseObserver
import io.ktor.client.request.header
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import com.tagaev.trrcrm.utils.HumanLogger
import com.tagaev.trrcrm.utils.Log
import io.ktor.http.HttpStatusCode

/**
 * Make a client with:
 * - Logging (requests + responses + bodies)
 * - ResponseObserver for status line
 * - DoubleReceive to safely read body for logs
 * - Timeouts, retries, JSON
 */

// Common flag: actual implementations live in androidMain / iosMain
expect val isPublish: Boolean

object HttpClientFactory {

    fun create(
        engine: HttpClientEngine? = null,
        json: Json = defaultJson,
        loggingEnabled: Boolean = !isPublish,
        logBodies: Boolean = false,
    ): HttpClient {
        val config: HttpClientConfig<*>.() -> Unit = {
        // JSON
        install(ContentNegotiation) { json(json) }

        // Retries: idempotent requests (non-POST) for transient failures.
        install(HttpRequestRetry) {
            maxRetries = 2
            retryIf { request, response ->
                request.method != HttpMethod.Post &&
                    (response.status.value >= 500 ||
                        response.status == HttpStatusCode.RequestTimeout ||
                        response.status == HttpStatusCode.TooManyRequests)
            }
            retryOnExceptionIf { request, cause ->
                request.method != HttpMethod.Post && isTransientNetworkFailure(cause)
            }
            exponentialDelay()
        }

        // Timeouts
        install(HttpTimeout) {
            requestTimeoutMillis = 60_000
            connectTimeoutMillis = 30_000
            socketTimeoutMillis  = 60_000
        }

        // Default headers
        install(DefaultRequest) {
            // Keep browser requests "simple" (no forced Content-Type/User-Agent),
            // otherwise wasm/js can fail with CORS/preflight errors.
            header(HttpHeaders.Accept, "application/json")
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
        return if (engine != null) {
            HttpClient(engine, config)
        } else {
            HttpClient(config)
        }
    }

    fun createNoLogs(json: Json = defaultJson): HttpClient =
        create(json = json, loggingEnabled = false, logBodies = false)

    private fun HttpClient.sanitizeHeader(function: Any) {
        TODO("Not yet implemented")
    }

    private fun isTransientNetworkFailure(cause: Throwable): Boolean {
        val lower = cause.message.orEmpty().lowercase()
        val name = cause::class.simpleName.orEmpty().lowercase()
        return "timeout" in lower ||
            "timed out" in lower ||
            "connection reset" in lower ||
            "connection aborted" in lower ||
            "connection refused" in lower ||
            "network is unreachable" in lower ||
            "unresolvedaddress" in lower ||
            "unknownhost" in lower ||
            "connectexception" in lower ||
            "connecttimeoutexception" in name ||
            "sockettimeoutexception" in name
    }

    val defaultJson = Json {
        ignoreUnknownKeys = true
        isLenient = true
        explicitNulls = false
    }
}
