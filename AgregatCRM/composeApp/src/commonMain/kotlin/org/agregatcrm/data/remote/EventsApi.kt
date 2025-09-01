package org.agregatcrm.data.remote

import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.bodyAsText
import io.ktor.client.statement.request
import kotlinx.serialization.json.Json
import org.agregatcrm.ext.urlDecodeUtf8
import org.agregatcrm.models.EventItemDto
import org.agregatcrm.models.cleanJsonStart
import org.agregatcrm.utils.Log

// https://api.aaaaaaaaa.ru/app/getdata.php?token=111111111&task=getitemslist&type=%D0%94%D0%BE%D0%BA%D1%83%D0%BC%D0%B5%D0%BD%D1%82&name=%D0%A1%D0%BE%D0%B1%D1%8B%D1%82%D0%B8%D0%B5&count=5&ncount=50&orderby=%D0%94%D0%B0%D1%82%D0%B0&orderdir=asc
// https://api.agregatka.ru/app/getdata.php?token=234234234&task=getitemslist&type=%D0%94%D0%BE%D0%BA%D1%83%D0%BC%D0%B5%D0%BD%D1%82&name=%D0%A1%D0%BE%D0%B1%D1%8B%D1%82%D0%B8%D0%B5&count=5&ncount=50&orderby=%D0%94%D0%B0%D1%82%D0%B0&orderdir=asc
data class ApiConfig(
//    val baseUrl: String = "http://akpp-1c.ru:86/AA/hs/mycrm/agrapp",
    val baseUrl: String = "https://api.agregatka.ru/app/getdata.php",
    val token: String = TOKEN
)

internal const val TOKEN = "234234234"

class EventsApi(
    private val client: HttpClient = HttpClientFactory.create()
) {
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        explicitNulls = false
    }

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
    ): List<EventItemDto> {
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
        val raw = response.bodyAsText().cleanJsonStart()
//        Log.info(">>>> URL: ${response.request.url.toString()} | ${response.request.url.toString().urlDecodeUtf8()}")
//        println("EventsApi getEvents ${response.bodyAsText()}")
        // Ответ — массив JSON объектов (список событий):
        return json.decodeFromString<List<EventItemDto>>(raw)
    }

    companion object {

    }
}
