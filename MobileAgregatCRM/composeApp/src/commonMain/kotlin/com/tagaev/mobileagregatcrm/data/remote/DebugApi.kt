package com.tagaev.mobileagregatcrm.data.remote

import io.ktor.client.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json
import org.agregatcrm.models.EventItemDto
import org.agregatcrm.models.cleanJsonStart

object DebugApi {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        explicitNulls = false
    }

    private val client = HttpClient {
        install(ContentNegotiation) { json(json) }
        install(Logging) {
            level = LogLevel.ALL
            logger = Logger.DEFAULT
        }
    }

    suspend fun fetchEvents(
        token: String,
        baseUrl: String = "http://akpp-1c.ru:86/AA/hs/mycrm/agrapp"
    ): List<EventItemDto> {
        return try {
            val response: HttpResponse = client.get(baseUrl) {
                url {
                    parameters.append("token", token)
                    parameters.append("task", "getitemslist")
                    parameters.append("type", "Документ")
                    parameters.append("name", "Событие")
                    parameters.append("count", "3")
                    parameters.append("ncount", "50")
                    parameters.append("orderby", "Дата")
                    parameters.append("orderdir", "desc")
                    parameters.append("filterby", "ПодразделениеКомпании")
                    parameters.append("filterval", "Воронеж")
                }
//                println("request >>>>>>>[${url.buildString()}]  [${url.toString()}]")
            }
            // Read as text (to clean BOM), then decode into English-named DTO
            val raw = response.bodyAsText().cleanJsonStart()
            println("=== RAW RESPONSE ===")
//            println(text.take(2000)) // cap for debug
            println("====================")

            runCatching { json.decodeFromString<List<EventItemDto>>(raw) }
                .onFailure { println("Parse error: ${it.message}") }
                .getOrDefault(emptyList())

        } catch (e: Exception) {
            println("Request failed: ${e.message}")
            emptyList()
        }
    }
}