package com.tagaev.mobileagregatcrm.data.remote

import com.tagaev.mobileagregatcrm.utils.CONST
import io.ktor.client.*
import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.plugins.RedirectResponseException
import io.ktor.client.plugins.ServerResponseException
import io.ktor.client.request.*
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.ktor.http.isSuccess
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.contentOrNull
import com.tagaev.mobileagregatcrm.models.EventItemDto
import com.tagaev.mobileagregatcrm.models.GetTokenResponse
import com.tagaev.mobileagregatcrm.models.SentMessageResponse
import com.tagaev.secrets.Secrets
import io.ktor.client.plugins.expectSuccess
import org.agregatcrm.models.cleanJsonStart

// https://api.aaaaaaaaa.ru/app/getdata.php?token=111111111&task=getitemslist&type=%D0%94%D0%BE%D0%BA%D1%83%D0%BC%D0%B5%D0%BD%D1%82&name=%D0%A1%D0%BE%D0%B1%D1%8B%D1%82%D0%B8%D0%B5&count=5&ncount=50&orderby=%D0%94%D0%B0%D1%82%D0%B0&orderdir=asc
// https://api.agregatka.ru/app/getdata.php?token=234234234&task=getitemslist&type=%D0%94%D0%BE%D0%BA%D1%83%D0%BC%D0%B5%D0%BD%D1%82&name=%D0%A1%D0%BE%D0%B1%D1%8B%D1%82%D0%B8%D0%B5&count=5&ncount=50&orderby=%D0%94%D0%B0%D1%82%D0%B0&orderdir=asc
data class ApiConfig(
//    val baseUrl: String = "http://akpp-1c.ru:86/AA/hs/mycrm/agrapp",
    val baseUrl: String = Secrets.BASE_URL, // "https://agrapp.agregatka.ru",//"https://api.agregatka.ru/app/getdata.php",
    var token: String
)

//internal const val TOKEN = "95AA8A6F209270E6BA02F21BEAE4A2BC75B192A815707D42AFF3E0862CD82898"

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
                    parameters.append("viewtype", Secrets.VIEW_TYPE)
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
            println("getEvents raw:${raw.length}")
            val items = json.decodeFromString<List<EventItemDto>>(raw)
            println("getEvents size:${items.size}")

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
            Resource.Error(e, "Unexpected error: ${e.message}")
        }
    }

    suspend fun sendMessage(
        api: ApiConfig,
        number: String,
        date: String,
        //tasklist: String, // optional
        message: String,
    ): Resource<SentMessageResponse> {
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
            val result = json.decodeFromString<SentMessageResponse>(raw)
            Resource.Success(result)
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

    // https://agrapp.agregatka.ru/?task=gettoken&user=kolosov.a.a@my.agregatka.ru&pass=
    suspend fun getToken(apiConfig: ApiConfig, username: String, password: String): Resource<GetTokenResponse> = resourceify {

        val response = client.get(apiConfig.baseUrl) {
            expectSuccess = true            // make non-2xx throw ResponseException
            url {
//                parameters.append("token", apiConfig.token)
                parameters.append("task", "gettoken")
                parameters.append("user", username)
                parameters.append("pass", password)
            }
        }

        // If you already installed ContentNegotiation(json), you can do:
        // return@resourceify response.body<SentMessageResponse>()

        // If the server sometimes sends a preface you strip off:
        val raw = response.bodyAsText().cleanJsonStart()
        val obj = json.parseToJsonElement(raw).jsonObject
        val err = obj["error"]?.jsonPrimitive?.contentOrNull
        if (err != null) {
            // Map logical 200-OK errors into Resource.Error via resourceify
            throw IllegalStateException(err)
        }
        json.decodeFromJsonElement<GetTokenResponse>(obj)
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