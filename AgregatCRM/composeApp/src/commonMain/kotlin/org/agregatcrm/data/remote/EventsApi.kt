package org.agregatcrm.data.remote

import io.ktor.client.*
import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.plugins.RedirectResponseException
import io.ktor.client.plugins.ServerResponseException
import io.ktor.client.request.*
import io.ktor.client.statement.bodyAsText
import io.ktor.client.statement.request
import io.ktor.http.HttpStatusCode
import io.ktor.http.isSuccess
import kotlinx.serialization.json.Json
import org.agregatcrm.ext.urlDecodeUtf8
import org.agregatcrm.models.EventItemDto
import org.agregatcrm.models.cleanJsonStart
import org.agregatcrm.utils.Log

// https://api.aaaaaaaaa.ru/app/getdata.php?token=111111111&task=getitemslist&type=%D0%94%D0%BE%D0%BA%D1%83%D0%BC%D0%B5%D0%BD%D1%82&name=%D0%A1%D0%BE%D0%B1%D1%8B%D1%82%D0%B8%D0%B5&count=5&ncount=50&orderby=%D0%94%D0%B0%D1%82%D0%B0&orderdir=asc
// https://api.agregatka.ru/app/getdata.php?token=234234234&task=getitemslist&type=%D0%94%D0%BE%D0%BA%D1%83%D0%BC%D0%B5%D0%BD%D1%82&name=%D0%A1%D0%BE%D0%B1%D1%8B%D1%82%D0%B8%D0%B5&count=5&ncount=50&orderby=%D0%94%D0%B0%D1%82%D0%B0&orderdir=asc
data class ApiConfig(
//    val baseUrl: String = "http://akpp-1c.ru:86/AA/hs/mycrm/agrapp",
    val baseUrl: String = "https://agrapp.agregatka.ru",//"https://api.agregatka.ru/app/getdata.php",
    val token: String = TOKEN
)

internal const val TOKEN = "95AA8A6F209270E6BA02F21BEAE4A2BC75B192A815707D42AFF3E0862CD82898"

sealed class Resource<out R> {
    data class Success<out T>(val data: T) : Resource<T>()
    data class Error<T>(val exception: Exception? = null, val causes: String? = null) : Resource<T>()
    object Loading : Resource<Nothing>()
}

class EventsApi(
    private val client: HttpClient = HttpClientFactory.create()
) {
    suspend fun getEvents(
        api: ApiConfig,
        type: String = "Документ",
        name: String = "Событие",
        count: Int = 1,
        ncount: Int = 50,
        orderBy: String = "Дата",
        orderDir: String = "desc",
        filterBy: String = "ПодразделениеКомпании",
        filterVal: String = "Воронеж"
    ): Resource<List<EventItemDto>> {
        return try {
            val url = api.baseUrl
            val response = client.get(url) {
                url {
                    parameters.append("token", api.token)
                    parameters.append("task", "getitemslist")
                    parameters.append("type", "Документ")
                    parameters.append("name", "Событие")
                    parameters.append("count", "${count}")
                    parameters.append("ncount", "${ncount}")
                    parameters.append("orderby", "${orderBy}")
                    parameters.append("orderdir", "${orderDir}")
                    parameters.append("filterby", "${filterBy}")
                    parameters.append("filterval", filterVal)
                }
            }
            if (!response.status.isSuccess()) {
                // read body for diagnostics, but don’t crash if not text
                val errBody = runCatching { response.bodyAsText().take(2000) }.getOrNull()
                return Resource.Error(
                    exception = null,
                    causes = "HTTP ${response.status.value} ${response.status.description}" +
                            (if (errBody.isNullOrBlank()) "" else " | $errBody")
                )
            }

            val raw = response.bodyAsText().cleanJsonStart()
            val items = json.decodeFromString<List<EventItemDto>>(raw)
            Resource.Success(items)
        } catch (e: RedirectResponseException) { // 3xx with body
            Resource.Error(e, "Redirect error: ${e.response.status}")
        } catch (e: ClientRequestException) {     // 4xx
            val body = runCatching { e.response.bodyAsText().take(2000) }.getOrNull()
            Resource.Error(e, "Client error ${e.response.status}: ${body ?: e.message}")
        } catch (e: ServerResponseException) {    // 5xx
            val body = runCatching { e.response.bodyAsText().take(2000) }.getOrNull()
            Resource.Error(e, "Server error ${e.response.status}: ${body ?: e.message}")
        } catch (e: Exception) {
            Resource.Error(e, e.message ?: "Unexpected error")
        }

//        Log.info(">>>> URL: ${response.request.url.toString()} | ${response.request.url.toString().urlDecodeUtf8()}")
//        println("EventsApi getEvents ${response.bodyAsText()}")
        // Ответ — массив JSON объектов (список событий):
//        return json.decodeFromString<List<EventItemDto>>(raw)
    }

    suspend fun sendMessage(
        api: ApiConfig,
        number: String,
        date: String,
        //tasklist: String, // optional
        message: String,
    ): Resource<List<EventItemDto>> {
        return try {
            val url = api.baseUrl
            val response = client.get(url) {
                url {
                    parameters.append("token", api.token)
                    parameters.append("task", "setmessage")
                    parameters.append("type", "Документ")
                    parameters.append("name", "Событие")
                    parameters.append("number", "${number}")
                    parameters.append("date", "${date}")
                    parameters.append("message", "${message}")
                }
            }
            if (!response.status.isSuccess()) {
                // read body for diagnostics, but don’t crash if not text
                val errBody = runCatching { response.bodyAsText().take(2000) }.getOrNull()
                return Resource.Error(
                    exception = null,
                    causes = "HTTP ${response.status.value} ${response.status.description}" +
                            (if (errBody.isNullOrBlank()) "" else " | $errBody")
                )
            }

            val raw = response.bodyAsText().cleanJsonStart()
            val items = json.decodeFromString<List<EventItemDto>>(raw)
            Resource.Success(items)
        } catch (e: RedirectResponseException) { // 3xx with body
            Resource.Error(e, "Redirect error: ${e.response.status}")
        } catch (e: ClientRequestException) {     // 4xx
            val body = runCatching { e.response.bodyAsText().take(2000) }.getOrNull()
            Resource.Error(e, "Client error ${e.response.status}: ${body ?: e.message}")
        } catch (e: ServerResponseException) {    // 5xx
            val body = runCatching { e.response.bodyAsText().take(2000) }.getOrNull()
            Resource.Error(e, "Server error ${e.response.status}: ${body ?: e.message}")
        } catch (e: Exception) {
            Resource.Error(e, e.message ?: "Unexpected error")
        }
    }

    companion object {
        val json = Json {
            ignoreUnknownKeys = true
            isLenient = true
            explicitNulls = false
        }
    }
}


private fun HttpStatusCode.isSuccess(): Boolean = value in 200..299