package com.tagaev.trrcrm.data.remote

import com.tagaev.data.models.qrscanner.QRResponseTRS
import com.tagaev.trrcrm.data.remote.models.GetRolesResponse
import com.tagaev.trrcrm.domain.Refiner
import com.tagaev.trrcrm.domain.RefineState
import com.tagaev.trrcrm.domain.DocumentTypes
import com.tagaev.trrcrm.domain.FilterByOption
import com.tagaev.trrcrm.models.CargoDto
import com.tagaev.trrcrm.models.ComplaintDto
import io.ktor.client.*
import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.request.*
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.ktor.http.isSuccess
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.contentOrNull
import com.tagaev.trrcrm.models.EventItemDto
import com.tagaev.trrcrm.models.GetTokenResponse
import com.tagaev.trrcrm.models.InnerOrderDto
import com.tagaev.trrcrm.models.SentMessageResponse
import com.tagaev.trrcrm.models.WorkOrderDto
import io.ktor.client.plugins.expectSuccess
import com.tagaev.trrcrm.models.cleanJsonStart

// https://api.aaaaaaaaa.ru/app/getdata.php?token=111111111&task=getitemslist&type=%D0%94%D0%BE%D0%BA%D1%83%D0%BC%D0%B5%D0%BD%D1%82&name=%D0%A1%D0%BE%D0%B1%D1%8B%D1%82%D0%B8%D0%B5&count=5&ncount=50&orderby=%D0%94%D0%B0%D1%82%D0%B0&orderdir=asc
// https://api.agregatka.ru/app/getdata.php?token=234234234&task=getitemslist&type=%D0%94%D0%BE%D0%BA%D1%83%D0%BC%D0%B5%D0%BD%D1%82&name=%D0%A1%D0%BE%D0%B1%D1%8B%D1%82%D0%B8%D0%B5&count=5&ncount=50&orderby=%D0%94%D0%B0%D1%82%D0%B0&orderdir=asc
data class ApiConfig(
//    val baseUrl: String = "http://akpp-1c.ru:86/AA/hs/mycrm/agrapp",
    val baseUrl: String = "https://agrapp.agregatka.ru", //Secrets.BASE_URL, // "https://agrapp.agregatka.ru",//"https://api.agregatka.ru/app/getdata.php",
    var token: String
)

//internal const val TOKEN = "95AA8A6F209270E6BA02F21BEAE4A2BC75B192A815707D42AFF3E0862CD82898"

sealed class Resource<out R> {
    data class Success<out T>(val data: T, var additionalLoading: Boolean = false) : Resource<T>()
    data class Error<T>(val exception: Exception? = null, val causes: String? = null) : Resource<T>()
    object Loading: Resource<Nothing>()
}

class EventsApi(
    private val client: HttpClient
) {
    //
    //https://agrapp.agregatka.ru/?token=92139A5BFBBF4D644BA0E49BC0D35E842EE78F188DE445BF1C01F5BBE069B389&&task=getitemslist&type=Документ&name=Событие&count=50&ncount=0&orderby=Дата&orderdir=desc&filterby=Тема&filterval=Настройка&filtertype=value
    suspend fun getEvents(apiConfig: ApiConfig, ncount: Int = 0, currentRefine: RefineState, city: String): Resource<List<EventItemDto>> = resourceify {
        val url = apiConfig.baseUrl
        val response = client.get(url) {
            url {
                parameters.append("token", apiConfig.token)
                parameters.append("task", "getitemslist")

                parameters.append("type", "Документ")
                parameters.append("name", "Событие")

                parameters.append("count", "30")
                parameters.append("ncount", "$ncount")

                if (currentRefine.orderBy != Refiner.OrderBy.OFF) {
                    parameters.append("orderby", currentRefine.orderBy.wire)
                    parameters.append("orderdir", currentRefine.orderDir.wire)
                }

                if (currentRefine.status == Refiner.Status.ACTIVE) {
                    println("FilterByOption 1")
                    parameters.append("state", FilterByOption.ACTIVE.wire)
                } else if (currentRefine.status == Refiner.Status.DONE) {
                    println("FilterByOption 2")
                    parameters.append("state", FilterByOption.NO_ACTIVE.wire)
                } else {
                    println("FilterByOption 3")
                }


                if (currentRefine.searchQuery.isNotEmpty()) {
                    parameters.append("filterby", currentRefine.searchQueryType.wire)
                    parameters.append("filterval", currentRefine.searchQuery)
//                    parameters.append("filtertype", "list")
                }


                parameters.append("viewtype", "onlymy") //Secrets.VIEW_TYPE)
            }
        }

        if (!response.status.isSuccess()) {
            val body = runCatching { response.bodyAsText().take(2000) }.getOrNull()
            throw ClientRequestException(response, body ?: "HTTP ${response.status}")
        }

        val raw = response.bodyAsText()
        decodeOrWarning<List<EventItemDto>>(json, raw)
    }

    @Deprecated("USE FROM NEW SCREEN")
    suspend fun getEvents(
        api: ApiConfig,
        type: String,
        name: String,
        count: Int,
        ncount: Int,
        orderBy: String,
        orderDir: String,
        filterBy: String,
        filterVal: String
    ): Resource<List<EventItemDto>> = resourceify {
        val url = api.baseUrl
        val response = client.get(url) {
            url {
                parameters.append("token", api.token)
                parameters.append("task", "getitemslist")
                parameters.append("type", type)
                parameters.append("name", name)
                parameters.append("count", "$count")
                parameters.append("ncount", "$ncount")

                parameters.append("orderby", orderBy)
                parameters.append("orderdir", orderDir)
                println("FilterByOption 0 ${filterVal} ~ ${FilterByOption.ACTIVE.wire}  ${FilterByOption.NO_ACTIVE.wire}")

                if (filterVal == FilterByOption.ACTIVE.wire) {
                    println("FilterByOption 1")
                    parameters.append("state", FilterByOption.ACTIVE.wire)
                } else if (filterVal == FilterByOption.NO_ACTIVE.wire) {
                    println("FilterByOption 2")
                    parameters.append("state", FilterByOption.NO_ACTIVE.wire)
                } else {
                    println("FilterByOption 3")
                }

                parameters.append("filtertype", "list")
                parameters.append("viewtype", "onlymy") //Secrets.VIEW_TYPE)
            }
        }

        if (!response.status.isSuccess()) {
            val body = runCatching { response.bodyAsText().take(2000) }.getOrNull()
            throw ClientRequestException(response, body ?: "HTTP ${response.status}")
        }

        val raw = response.bodyAsText()
        decodeOrWarning<List<EventItemDto>>(json, raw)
    }

    suspend fun sendMessage(
        api: ApiConfig,
        number: String,
        date: String,
        //tasklist: String, // optional
        documentType: DocumentTypes,
        message: String,
    ): Resource<SentMessageResponse> = resourceify {
        val url = api.baseUrl
        val response = client.post(url) {
            url {
                parameters.append("token", api.token)
                parameters.append("task", "setmessage")
                parameters.append("type", "Документ")
                parameters.append("name", documentType.requestName)
                parameters.append("number", "${number}")
                parameters.append("date", "${date}")
                parameters.append("message", "${message}")
            }
        }

        if (!response.status.isSuccess()) {
            val body = runCatching { response.bodyAsText().take(2000) }.getOrNull()
            throw ClientRequestException(response, body ?: "HTTP ${response.status}")
        }

        val raw = response.bodyAsText()
        decodeOrWarning<SentMessageResponse>(json, raw)
    }

    // https://agrapp.agregatka.ru/?task=gettoken&user=kolosov.a.a@my.agregatka.ru&pass=
    suspend fun getToken(apiConfig: ApiConfig, username: String, password: String): Resource<GetTokenResponse> = resourceify {

        val response = client.get(apiConfig.baseUrl) {
            expectSuccess = true            // make non-2xx throw ResponseException
            url {
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

    //https://agrapp.agregatka.ru/?token=ED776D6A60113D476D9469A14A724037BE040BC8FB54B9E969D169584AD41D6A&task=getqrcomlectinfo&code=TRS111405064
    suspend fun getTRSData(apiConfig: ApiConfig, decodedCode: String) : Resource<QRResponseTRS> = resourceify {
        val response = client.get(apiConfig.baseUrl) {
            expectSuccess = true            // make non-2xx throw ResponseException
            url {
                parameters.append("token", apiConfig.token)
                parameters.append("task", "getqrcomlectinfo")
                parameters.append("code", decodedCode)
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
        json.decodeFromJsonElement<QRResponseTRS>(obj)
    }

    //
    suspend fun loadWorkOrders(apiConfig: ApiConfig, ncount: Int = 0, currentRefine: RefineState, city: String): List<WorkOrderDto> {
        val response = client.get(apiConfig.baseUrl) {
            url {
                parameters.append("token", apiConfig.token)
                parameters.append("task", "getitemslist")

                parameters.append("type", "Документ")
                parameters.append("name", "ЗаказНаряд")

                parameters.append("ncount", ncount.toString())
                parameters.append("count", "30")

//                if (currentRefine.orderBy != Refiner.OrderBy.OFF) {
//                    parameters.append("orderby", currentRefine.orderBy.wire)
//                    parameters.append("orderdir", currentRefine.orderDir.wire)
//                }
                if (currentRefine.searchQuery.isNotEmpty()) {
                    parameters.append("filterby", currentRefine.searchQueryType.wire)
                    parameters.append("filterval", currentRefine.searchQuery)
//                    parameters.append("filtertype", "list")
                }

                if (!currentRefine.filter.wire.isNullOrEmpty()) {
                    parameters.append("filterby", currentRefine.filter.wire)
                    parameters.append("filterval", city)
                }


                if (currentRefine.status == Refiner.Status.ACTIVE) {
                    println("FilterByOption 1")
                    parameters.append("state", FilterByOption.ACTIVE.wire)
                } else if (currentRefine.status == Refiner.Status.DONE) {
                    println("FilterByOption 2")
                    parameters.append("state", FilterByOption.NO_ACTIVE.wire)
                } else {
                    println("FilterByOption 3")
                }
            }
        }

        if (!response.status.isSuccess()) {
            val errBody = runCatching { response.bodyAsText().take(2000) }.getOrNull()
            throw IllegalStateException(
                "HTTP ${response.status.value} ${response.status.description}" +
                        (if (errBody.isNullOrBlank()) "" else " | $errBody")
            )
        }

        val raw = response.bodyAsText().cleanJsonStart()
        return json.decodeFromString<List<WorkOrderDto>>(raw)
    }

    suspend fun getComplaints(apiConfig: ApiConfig, ncount: Int = 0, currentRefine: RefineState, city: String): Resource<List<ComplaintDto>> = resourceify {
        val url = apiConfig.baseUrl
        val response = client.get(url) {
            url {
                parameters.append("token", apiConfig.token)
                parameters.append("task", "getitemslist")

                parameters.append("type", "Документ")
                parameters.append("name", "Рекламация")

                parameters.append("count", "30")
                parameters.append("ncount", "$ncount")

                if (currentRefine.orderBy != Refiner.OrderBy.OFF) {
                    parameters.append("orderby", currentRefine.orderBy.wire)
                    parameters.append("orderdir", currentRefine.orderDir.wire)
                }

                if (currentRefine.status == Refiner.Status.ACTIVE) {
                    println("FilterByOption 1")
                    parameters.append("state", FilterByOption.ACTIVE.wire)
                } else if (currentRefine.status == Refiner.Status.DONE) {
                    println("FilterByOption 2")
                    parameters.append("state", FilterByOption.NO_ACTIVE.wire)
                } else {
                    println("FilterByOption 3")
                }

                if (currentRefine.filter == Refiner.Filter.DEPARTMENT) {
                    parameters.append("filterby", currentRefine.filter.wire)
                    parameters.append("filterval", currentRefine.filterValue)
                }

                if (currentRefine.searchQuery.isNotEmpty()) {
                    parameters.append("filterby", currentRefine.searchQueryType.wire)
                    parameters.append("filterval", currentRefine.searchQuery)
//                    parameters.append("filtertype", "list")
                }


                parameters.append("viewtype", "onlymy") //Secrets.VIEW_TYPE)
            }
        }

        if (!response.status.isSuccess()) {
            val body = runCatching { response.bodyAsText().take(2000) }.getOrNull()
            throw ClientRequestException(response, body ?: "HTTP ${response.status}")
        }

        val raw = response.bodyAsText()
        decodeOrWarning<List<ComplaintDto>>(json, raw)
    }

    suspend fun getInnerOrders(apiConfig: ApiConfig, ncount: Int = 0, currentRefine: RefineState, city: String): Resource<List<InnerOrderDto>> = resourceify {
        val url = apiConfig.baseUrl
        val response = client.get(url) {
            url {
                parameters.append("token", apiConfig.token)
                parameters.append("task", "getitemslist")

                parameters.append("type", "Документ")
                parameters.append("name", "ЗаказВнутренний")

                parameters.append("count", "30")
                parameters.append("ncount", "$ncount")

                if (currentRefine.orderBy != Refiner.OrderBy.OFF) {
                    parameters.append("orderby", currentRefine.orderBy.wire)
                    parameters.append("orderdir", currentRefine.orderDir.wire)
                }

                if (currentRefine.status == Refiner.Status.ACTIVE) {
                    println("FilterByOption 1")
                    parameters.append("state", FilterByOption.ACTIVE.wire)
                } else if (currentRefine.status == Refiner.Status.DONE) {
                    println("FilterByOption 2")
                    parameters.append("state", FilterByOption.NO_ACTIVE.wire)
                } else {
                    println("FilterByOption 3")
                }

                if (currentRefine.filter == Refiner.Filter.DEPARTMENT) {
                    parameters.append("filterby", currentRefine.filter.wire)
                    parameters.append("filterval", currentRefine.filterValue)
                }

                if (currentRefine.searchQuery.isNotEmpty()) {
                    parameters.append("filterby", currentRefine.searchQueryType.wire)
                    parameters.append("filterval", currentRefine.searchQuery)
//                    parameters.append("filtertype", "list")
                }


                parameters.append("viewtype", "onlymy") //Secrets.VIEW_TYPE)
            }
        }

        if (!response.status.isSuccess()) {
            val body = runCatching { response.bodyAsText().take(2000) }.getOrNull()
            throw ClientRequestException(response, body ?: "HTTP ${response.status}")
        }

        val raw = response.bodyAsText()
        decodeOrWarning<List<InnerOrderDto>>(json, raw)
    }

    suspend fun getCargos(apiConfig: ApiConfig, ncount: Int = 0, currentRefine: RefineState, city: String): Resource<List<CargoDto>> = resourceify {
        val url = apiConfig.baseUrl
        val response = client.get(url) {
            url {
                parameters.append("token", apiConfig.token)
                parameters.append("task", "getitemslist")

                parameters.append("type", "Документ")
                parameters.append("name", "Груз")

                parameters.append("count", "30")
                parameters.append("ncount", "$ncount")

                if (currentRefine.orderBy != Refiner.OrderBy.OFF) {
                    parameters.append("orderby", currentRefine.orderBy.wire)
                    parameters.append("orderdir", currentRefine.orderDir.wire)
                }

                if (currentRefine.status == Refiner.Status.ACTIVE) {
                    println("FilterByOption 1")
                    parameters.append("state", FilterByOption.ACTIVE.wire)
                } else if (currentRefine.status == Refiner.Status.DONE) {
                    println("FilterByOption 2")
                    parameters.append("state", FilterByOption.NO_ACTIVE.wire)
                } else {
                    println("FilterByOption 3")
                }

                if (currentRefine.filter == Refiner.Filter.DEPARTMENT) {
                    parameters.append("filterby", currentRefine.filter.wire)
                    parameters.append("filterval", currentRefine.filterValue)
                }

                if (currentRefine.searchQuery.isNotEmpty()) {
                    parameters.append("filterby", currentRefine.searchQueryType.wire)
                    parameters.append("filterval", currentRefine.searchQuery)
//                    parameters.append("filtertype", "list")
                }


                parameters.append("viewtype", "onlymy") //Secrets.VIEW_TYPE)
            }
        }

        if (!response.status.isSuccess()) {
            val body = runCatching { response.bodyAsText().take(2000) }.getOrNull()
            throw ClientRequestException(response, body ?: "HTTP ${response.status}")
        }

        val raw = response.bodyAsText()
        decodeOrWarning<List<CargoDto>>(json, raw)
    }

    suspend fun getRole(apiConfig: ApiConfig): Resource<GetRolesResponse> = resourceify {

        val response = client.get(apiConfig.baseUrl) {
            expectSuccess = true            // make non-2xx throw ResponseException
            url {
                parameters.append("token", apiConfig.token)
                parameters.append("task", "getroles")
            }
        }

        // If the server sometimes sends a preface you strip off:
        val raw = response.bodyAsText().cleanJsonStart()
        val obj = json.parseToJsonElement(raw).jsonObject
        val err = obj["error"]?.jsonPrimitive?.contentOrNull
        if (err != null) {
            // Map logical 200-OK errors into Resource.Error via resourceify
            throw IllegalStateException(err)
        }
        json.decodeFromJsonElement<GetRolesResponse>(obj)
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