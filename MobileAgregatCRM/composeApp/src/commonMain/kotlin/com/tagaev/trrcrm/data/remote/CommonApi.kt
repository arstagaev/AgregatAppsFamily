package com.tagaev.trrcrm.data.remote

import com.tagaev.data.models.qrscanner.QRResponseTRS
import com.tagaev.secrets.Secrets
import com.tagaev.trrcrm.models.UserPermissionEntryDto
import com.tagaev.trrcrm.domain.Refiner
import com.tagaev.trrcrm.domain.RefineState
import com.tagaev.trrcrm.domain.DocumentTypes
import com.tagaev.trrcrm.domain.FilterByOption
import com.tagaev.trrcrm.getPlatform
import com.tagaev.trrcrm.models.CargoDto
import com.tagaev.trrcrm.models.BuyerOrderDto
import com.tagaev.trrcrm.models.ComplaintDto
import com.tagaev.trrcrm.models.CoreDeviceRegisterRequest
import com.tagaev.trrcrm.models.CoreDeviceRegisterResponse
import com.tagaev.trrcrm.models.CoreNotificationIntentRequest
import com.tagaev.trrcrm.models.CoreNotificationIntentResponse
import com.tagaev.trrcrm.models.CoreNotificationsFeedRequest
import com.tagaev.trrcrm.models.CoreNotificationsFeedResponse
import com.tagaev.trrcrm.models.CoreNotificationsUnreadCountRequest
import com.tagaev.trrcrm.models.CoreNotificationsUnreadCountResponse
import com.tagaev.trrcrm.models.CoreNotificationsReadAllRequest
import com.tagaev.trrcrm.models.CoreNotificationsReadAllResponse
import com.tagaev.trrcrm.models.CoreResolveRecipientsRequest
import com.tagaev.trrcrm.models.CoreResolveRecipientsResponse
import com.tagaev.trrcrm.models.CoreSessionBootstrapRequest
import com.tagaev.trrcrm.models.CoreSessionBootstrapResponse
import com.tagaev.trrcrm.models.CoreSessionHeartbeatRequest
import com.tagaev.trrcrm.models.CoreSessionHeartbeatResponse
import com.tagaev.trrcrm.models.CoreSessionLogoutRequest
import com.tagaev.trrcrm.models.CoreSessionLogoutResponse
import com.tagaev.trrcrm.models.HealthResponse
import com.tagaev.trrcrm.models.CoreNotificationStatusUpdateRequest
import com.tagaev.trrcrm.models.CoreNotificationStatusUpdateResponse
import com.tagaev.trrcrm.models.CoreDeviceMuteStateRequest
import com.tagaev.trrcrm.models.CoreDeviceMuteUpdateRequest
import com.tagaev.trrcrm.models.CoreDeviceMuteStateResponse
import com.tagaev.trrcrm.models.PushFeatureToggleGetResponse
import com.tagaev.trrcrm.models.PushFeatureToggleSetRequest
import com.tagaev.trrcrm.models.PushFeatureToggleSetResponse
import com.tagaev.trrcrm.models.CatalogProductRequest
import com.tagaev.trrcrm.models.CatalogProductRequestResponse
import com.tagaev.trrcrm.models.CatalogProductsResponse
import com.tagaev.trrcrm.models.CatalogSignupRequest
import com.tagaev.trrcrm.models.CatalogSignupResponse
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
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.JsonElement
import com.tagaev.trrcrm.models.EventItemDto
import com.tagaev.trrcrm.models.GetTokenResponse
import com.tagaev.trrcrm.models.ExpenseRequestDto
import com.tagaev.trrcrm.models.IncomingApplicationDto
import com.tagaev.trrcrm.models.RepairTemplateCatalogItemDto
import com.tagaev.trrcrm.models.InnerOrderDto
import com.tagaev.trrcrm.models.SentMessageResponse
import com.tagaev.trrcrm.models.SupplierOrderDto
import com.tagaev.trrcrm.models.ThreadMessageRequest
import com.tagaev.trrcrm.models.ThreadMessageResponse
import com.tagaev.trrcrm.models.WorkOrderDto
import io.ktor.client.plugins.expectSuccess
import io.ktor.client.plugins.timeout
import com.tagaev.trrcrm.models.cleanJsonStart
import com.tagaev.trrcrm.utils.DefaultValuesConst.CORE_API_KEY
import com.tagaev.trrcrm.utils.DefaultValuesConst.GLOBAL_CATALOG_URL
import com.tagaev.trrcrm.utils.DefaultValuesConst.GLOBAL_CORE_URL
import com.tagaev.trrcrm.utils.DefaultValuesConst.GLOBAL_PUSH_URL
import io.ktor.http.ContentType
import io.ktor.http.HeadersBuilder
import io.ktor.http.contentType
import kotlin.random.Random
import kotlin.time.TimeSource

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

fun Resource.Error<*>.userMessage(fallback: String): String =
    userFacingMessage(causes, friendlyError(exception, fallback))

class EventsApi(
    private val client: HttpClient
) {
    private suspend inline fun <reified T> corePost(
        baseUrl: String = GLOBAL_CORE_URL,
        path: String,
        body: Any? = null,
        includeApiKey: Boolean = true,
        operationName: String = "core_post:$path",
        extraHeaders: Map<String, String> = emptyMap(),
    ): T {
        val marker = TimeSource.Monotonic.markNow()
        println("CATALOG_CORE_API: request_start op=$operationName method=POST path=$path")
        try {
            val response = client.post("${baseUrl.trimEnd('/')}$path") {
                expectSuccess = false
                if (includeApiKey) header("X-API-Key", CORE_API_KEY)
                extraHeaders.forEach { (key, value) -> header(key, value) }
                contentType(ContentType.Application.Json)
                if (body != null) setBody(body)
            }
            val raw = response.bodyAsText().cleanJsonStart()
            val latencyMs = marker.elapsedNow().inWholeMilliseconds
            println("CATALOG_CORE_API: request_end op=$operationName status=${response.status.value} latency_ms=$latencyMs")
            if (!response.status.isSuccess()) {
                val parsed = parseCoreErrorPayload(raw)
                throw CoreApiException(
                    statusCode = response.status.value,
                    url = response.call.request.url.toString(),
                    responseBody = raw,
                    errorCode = parsed?.code,
                    errorMessage = parsed?.message,
                    fields = parsed?.fields.orEmpty()
                )
            }
            return decodeOrWarning(json, raw)
        } catch (t: Throwable) {
            val latencyMs = marker.elapsedNow().inWholeMilliseconds
            val normalized = when (t) {
                is CoreApiException -> t.normalizedErrorCode()
                else -> "transport_error"
            }
            println("CATALOG_CORE_API: request_fail op=$operationName latency_ms=$latencyMs error_code=$normalized reason=${t.message}")
            throw t
        }
    }

    private suspend inline fun <reified T> coreGet(
        baseUrl: String = GLOBAL_CORE_URL,
        path: String,
        includeApiKey: Boolean = false,
        operationName: String = "core_get:$path",
        queryParams: Map<String, String?> = emptyMap(),
    ): T {
        val marker = TimeSource.Monotonic.markNow()
        println("CATALOG_CORE_API: request_start op=$operationName method=GET path=$path")
        try {
            val response = client.get("${baseUrl.trimEnd('/')}$path") {
                expectSuccess = false
                if (includeApiKey) header("X-API-Key", CORE_API_KEY)
                contentType(ContentType.Application.Json)
                if (queryParams.isNotEmpty()) {
                    url {
                        queryParams.forEach { (key, value) ->
                            if (!value.isNullOrBlank()) parameters.append(key, value)
                        }
                    }
                }
            }
            val raw = response.bodyAsText().cleanJsonStart()
            val latencyMs = marker.elapsedNow().inWholeMilliseconds
            println("CATALOG_CORE_API: request_end op=$operationName status=${response.status.value} latency_ms=$latencyMs")
            if (!response.status.isSuccess()) {
                val parsed = parseCoreErrorPayload(raw)
                throw CoreApiException(
                    statusCode = response.status.value,
                    url = response.call.request.url.toString(),
                    responseBody = raw,
                    errorCode = parsed?.code,
                    errorMessage = parsed?.message,
                    fields = parsed?.fields.orEmpty()
                )
            }
            return decodeOrWarning(json, raw)
        } catch (t: Throwable) {
            val latencyMs = marker.elapsedNow().inWholeMilliseconds
            val normalized = when (t) {
                is CoreApiException -> t.normalizedErrorCode()
                else -> "transport_error"
            }
            println("CATALOG_CORE_API: request_fail op=$operationName latency_ms=$latencyMs error_code=$normalized reason=${t.message}")
            throw t
        }
    }

    internal suspend inline fun <reified T> findDocumentsByNumber(
        apiConfig: ApiConfig,
        documentName: String,
        documentNumber: String,
        count: Int = 30,
        ncount: Int = 0
    ): Resource<List<T>> = resourceify {
        val url = apiConfig.baseUrl
        val response = client.get(url) {
            url {
                parameters.append("token", apiConfig.token)
                parameters.append("task", "getitemslist")
                parameters.append("type", "Документ")
                parameters.append("name", documentName)
                parameters.append("count", count.toString())
                parameters.append("ncount", ncount.toString())
                parameters.append("filterby", "Номер")
                parameters.append("filterval", documentNumber)
                parameters.append("viewtype", "onlymy")
            }
        }

        if (!response.status.isSuccess()) {
            val body = runCatching { response.bodyAsText().take(2000) }.getOrNull()
            throw ClientRequestException(response, body ?: "HTTP ${response.status}")
        }

        val raw = response.bodyAsText()
        decodeOrWarning<List<T>>(json, raw)
    }

    suspend fun probeStartup(apiConfig: ApiConfig): Resource<Unit> = resourceify {
        // Lightweight cold-start probe:
        // - hit API root without fake credentials (avoids backend-side 500 on invalid gettoken probes)
        // - still classify 3xx..5xx as server-side startup block
        val response = client.get(apiConfig.baseUrl) {
            expectSuccess = false
        }

        when (val code = response.status.value) {
            in 200..299 -> Unit
            in 300..399 -> throw RedirectResponseException(response, "HTTP $code")
            in 400..499 -> throw ClientRequestException(response, "HTTP $code")
            in 500..599 -> throw ServerResponseException(response, "HTTP $code")
            else -> throw IllegalStateException("Unexpected HTTP status: $code")
        }
    }

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

                // ЗаказПокупателя: backend reliably supports sorting by Дата.
                parameters.append("orderby", Refiner.OrderBy.DATE.wire)
                parameters.append("orderdir", currentRefine.orderDir.wire)

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
            timeout {
                requestTimeoutMillis = 15_000
                socketTimeoutMillis = 15_000
            }
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

    suspend fun sendThreadMessage(
        api: ApiConfig,
        docId: String,
        docTitle: String,
        authorName: String,
        messageText: String,
        recipientNames: List<String>,
    ): Resource<ThreadMessageResponse> = resourceify {
        val url = GLOBAL_PUSH_URL.trimEnd('/') + "/push/thread-message"
        println("sendThreadMessage -> docId ${docId} recipientNames ${recipientNames} | ${messageText}")
        val response = client.post(url) {
            header("X-API-Key", Secrets.PUSH_API_KEY)
            contentType(ContentType.Application.Json)
            setBody(
                ThreadMessageRequest(
                    doc_id = docId,
                    doc_title = docTitle,
                    author_name = authorName,
                    message_text = messageText,
                    recipient_names = recipientNames,
                )
            )
        }

        if (!response.status.isSuccess()) {
            val body = runCatching { response.bodyAsText().take(2000) }.getOrNull()
            throw ClientRequestException(response, body ?: "HTTP ${response.status}")
        }

        val raw = response.bodyAsText()
        decodeOrWarning<ThreadMessageResponse>(json, raw)
    }

    suspend fun coreSessionBootstrap(request: CoreSessionBootstrapRequest): Resource<CoreSessionBootstrapResponse> = resourceify {
        corePost<CoreSessionBootstrapResponse>(
            path = "/core/session/bootstrap",
            body = request,
            includeApiKey = true
        )
    }

    suspend fun coreSessionHeartbeat(request: CoreSessionHeartbeatRequest): Resource<CoreSessionHeartbeatResponse> = resourceify {
        corePost<CoreSessionHeartbeatResponse>(
            path = "/core/session/heartbeat",
            body = request,
            includeApiKey = true
        )
    }

    suspend fun coreSessionLogout(request: CoreSessionLogoutRequest): Resource<CoreSessionLogoutResponse> = resourceify {
        corePost<CoreSessionLogoutResponse>(
            path = "/core/session/logout",
            body = request,
            includeApiKey = true
        )
    }

    suspend fun coreDeviceRegister(request: CoreDeviceRegisterRequest): Resource<CoreDeviceRegisterResponse> = resourceify {
        corePost<CoreDeviceRegisterResponse>(
            path = "/devices/register",
            body = request,
            includeApiKey = true
        )
    }

    suspend fun coreResolveRecipients(request: CoreResolveRecipientsRequest): Resource<CoreResolveRecipientsResponse> = resourceify {
        corePost<CoreResolveRecipientsResponse>(
            path = "/notifications/resolve-recipients",
            body = request,
            includeApiKey = true
        )
    }

    suspend fun coreNotificationIntent(request: CoreNotificationIntentRequest): Resource<CoreNotificationIntentResponse> = resourceify {
        corePost<CoreNotificationIntentResponse>(
            path = "/notifications/intents",
            body = request,
            includeApiKey = true
        )
    }

    suspend fun coreNotificationsFeed(request: CoreNotificationsFeedRequest): Resource<CoreNotificationsFeedResponse> = resourceify {
        corePost<CoreNotificationsFeedResponse>(
            path = "/notifications/feed",
            body = request,
            includeApiKey = true
        )
    }

    suspend fun coreNotificationsUnreadCount(request: CoreNotificationsUnreadCountRequest): Resource<CoreNotificationsUnreadCountResponse> = resourceify {
        corePost<CoreNotificationsUnreadCountResponse>(
            path = "/notifications/unread-count",
            body = request,
            includeApiKey = true
        )
    }

    suspend fun coreNotificationStatusUpdate(request: CoreNotificationStatusUpdateRequest): Resource<CoreNotificationStatusUpdateResponse> = resourceify {
        corePost<CoreNotificationStatusUpdateResponse>(
            path = "/notifications/status/update",
            body = request,
            includeApiKey = true
        )
    }

    suspend fun coreNotificationsReadAll(request: CoreNotificationsReadAllRequest): Resource<CoreNotificationsReadAllResponse> = resourceify {
        corePost<CoreNotificationsReadAllResponse>(
            path = "/notifications/status/read-all",
            body = request,
            includeApiKey = true
        )
    }

    suspend fun coreHealth(): Resource<HealthResponse> = resourceify {
        coreGet<HealthResponse>(
            path = "/health",
            includeApiKey = false
        )
    }

    suspend fun corePushFeatureToggleGet(): Resource<PushFeatureToggleGetResponse> = resourceify {
        coreGet<PushFeatureToggleGetResponse>(
            path = "/feature-toggles/push-notifications",
            includeApiKey = true
        )
    }

    suspend fun corePushFeatureToggleSet(request: PushFeatureToggleSetRequest): Resource<PushFeatureToggleSetResponse> = resourceify {
        corePost<PushFeatureToggleSetResponse>(
            path = "/feature-toggles/push-notifications",
            body = request,
            includeApiKey = true
        )
    }

    suspend fun coreDeviceMuteState(request: CoreDeviceMuteStateRequest): Resource<CoreDeviceMuteStateResponse> = resourceify {
        corePost<CoreDeviceMuteStateResponse>(
            path = "/devices/mute/state",
            body = request,
            includeApiKey = true
        )
    }

    suspend fun coreDeviceMuteUpdate(request: CoreDeviceMuteUpdateRequest): Resource<CoreDeviceMuteStateResponse> = resourceify {
        corePost<CoreDeviceMuteStateResponse>(
            path = "/devices/mute",
            body = request,
            includeApiKey = true
        )
    }

    suspend fun catalogProducts(
        search: String? = null,
        brand: String? = null,
        model: String? = null,
        categoryId: String? = null,
        page: Int = 1,
        limit: Int = 20,
    ): Resource<CatalogProductsResponse> = resourceify {
        coreGet<CatalogProductsResponse>(
            baseUrl = GLOBAL_CATALOG_URL,
            path = "/catalog/products",
            includeApiKey = false,
            operationName = "catalog_products?q=${search.orEmpty()}&brand=${brand.orEmpty()}&model=${model.orEmpty()}&category=${categoryId.orEmpty()}&page=$page&limit=$limit",
            queryParams = mapOf(
                "search" to search?.trim(),
                "brand" to brand?.trim(),
                "model" to model?.trim(),
                "category_id" to categoryId?.trim(),
                "page" to page.toString(),
                "limit" to limit.toString(),
            )
        )
    }

    suspend fun catalogSignupRequest(request: CatalogSignupRequest): Resource<CatalogSignupResponse> = resourceify {
        corePost<CatalogSignupResponse>(
            baseUrl = GLOBAL_CATALOG_URL,
            path = "/catalog/signup-request",
            body = request,
            includeApiKey = true,
            operationName = "catalog_signup_request",
            extraHeaders = mapOf("Idempotency-Key" to generateIdempotencyKey())
        )
    }

    suspend fun catalogProductRequest(request: CatalogProductRequest): Resource<CatalogProductRequestResponse> = resourceify {
        corePost<CatalogProductRequestResponse>(
            baseUrl = GLOBAL_CATALOG_URL,
            path = "/catalog/product-request",
            body = request,
            includeApiKey = true,
            operationName = "catalog_product_request",
            extraHeaders = mapOf("Idempotency-Key" to generateIdempotencyKey())
        )
    }

    private fun generateIdempotencyKey(): String {
        val bytes = Random.Default.nextBytes(16)
        val hex = bytes.joinToString(separator = "") { b ->
            (b.toInt() and 0xFF).toString(16).padStart(2, '0')
        }
        return buildString {
            append(hex.substring(0, 8))
            append('-')
            append(hex.substring(8, 12))
            append('-')
            append(hex.substring(12, 16))
            append('-')
            append(hex.substring(16, 20))
            append('-')
            append(hex.substring(20, 32))
        }
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
                } else if (currentRefine.repairType != Refiner.WorkOrderRepairType.OFF) {
                    parameters.append("filterby", Refiner.WorkOrderRepairType.API_FIELD)
                    parameters.append("filterval", currentRefine.repairType.wire)
                } else if (currentRefine.filter.wire.isNotEmpty()) {
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

    suspend fun loadComplectations(apiConfig: ApiConfig, ncount: Int = 0, currentRefine: RefineState, city: String): List<WorkOrderDto> {
        val response = client.get(apiConfig.baseUrl) {
            url {
                parameters.append("token", apiConfig.token)
                parameters.append("task", "getitemslist")

                parameters.append("type", "Документ")
                parameters.append("name", "Комплектация")

                parameters.append("ncount", ncount.toString())
                parameters.append("count", "30")

                if (currentRefine.searchQuery.isNotEmpty()) {
                    val searchFilterBy = when (currentRefine.searchQueryType) {
                        Refiner.SearchQueryType.CODE -> Refiner.SearchQueryType.CODE.wire
                        Refiner.SearchQueryType.MASTER -> Refiner.SearchQueryType.MASTER.wire
                        Refiner.SearchQueryType.KIT_CHARACTERISTIC -> Refiner.SearchQueryType.KIT_CHARACTERISTIC.wire
                        else -> "Комплект"
                    }
                    parameters.append("filterby", searchFilterBy)
                    parameters.append("filterval", currentRefine.searchQuery)
                } else if (!currentRefine.filter.wire.isNullOrEmpty()) {
                    parameters.append("filterby", currentRefine.filter.wire)
                    parameters.append("filterval", city)
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

                if (currentRefine.searchQuery.isNotEmpty()) {
                    parameters.append("filterby", currentRefine.searchQueryType.wire)
                    parameters.append("filterval", currentRefine.searchQuery)
//                    parameters.append("filtertype", "list")
                } else if (currentRefine.filter == Refiner.Filter.DEPARTMENT) {
                    parameters.append("filterby", currentRefine.filter.wire)
                    parameters.append("filterval", currentRefine.filterValue)
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

                if (currentRefine.searchQuery.isNotEmpty()) {
                    parameters.append("filterby", currentRefine.searchQueryType.wire)
                    parameters.append("filterval", currentRefine.searchQuery)
//                    parameters.append("filtertype", "list")
                } else if (currentRefine.filter == Refiner.Filter.DEPARTMENT) {
                    parameters.append("filterby", currentRefine.filter.wire)
                    parameters.append("filterval", currentRefine.filterValue)
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

    suspend fun getBuyerOrders(apiConfig: ApiConfig, ncount: Int = 0, currentRefine: RefineState, city: String): Resource<List<BuyerOrderDto>> = resourceify {
        val url = apiConfig.baseUrl
        val response = client.get(url) {
            url {
                parameters.append("token", apiConfig.token)
                parameters.append("task", "getitemslist")

                parameters.append("type", "Документ")
                parameters.append("name", "ЗаказПокупателя")

                parameters.append("count", "30")
                parameters.append("ncount", "$ncount")

                if (currentRefine.orderBy != Refiner.OrderBy.OFF) {
                    parameters.append("orderby", currentRefine.orderBy.wire)
                    parameters.append("orderdir", currentRefine.orderDir.wire)
                }

                if (currentRefine.status == Refiner.Status.ACTIVE) {
                    parameters.append("state", FilterByOption.ACTIVE.wire)
                } else if (currentRefine.status == Refiner.Status.DONE) {
                    parameters.append("state", FilterByOption.NO_ACTIVE.wire)
                }

                if (currentRefine.searchQuery.isNotEmpty()) {
                    parameters.append("filterby", currentRefine.searchQueryType.wire)
                    parameters.append("filterval", currentRefine.searchQuery)
                } else if (currentRefine.filter == Refiner.Filter.DEPARTMENT) {
                    parameters.append("filterby", currentRefine.filter.wire)
                    parameters.append("filterval", currentRefine.filterValue.ifBlank { city })
                }

                parameters.append("viewtype", "onlymy")
            }
        }

        if (!response.status.isSuccess()) {
            val body = runCatching { response.bodyAsText().take(2000) }.getOrNull()
            throw ClientRequestException(response, body ?: "HTTP ${response.status}")
        }

        val raw = response.bodyAsText()
        decodeOrWarning<List<BuyerOrderDto>>(json, raw)
    }

    suspend fun getSupplierOrders(apiConfig: ApiConfig, ncount: Int = 0, currentRefine: RefineState, city: String): Resource<List<SupplierOrderDto>> = resourceify {
        val url = apiConfig.baseUrl
        val response = client.get(url) {
            url {
                parameters.append("token", apiConfig.token)
                parameters.append("task", "getitemslist")

                parameters.append("type", "Документ")
                parameters.append("name", "ЗаказПоставщику")

                parameters.append("count", "30")
                parameters.append("ncount", "$ncount")

                if (currentRefine.orderBy != Refiner.OrderBy.OFF) {
                    parameters.append("orderby", currentRefine.orderBy.wire)
                    parameters.append("orderdir", currentRefine.orderDir.wire)
                } else {
                    parameters.append("orderdir", Refiner.Dir.DESC.wire)
                }

                if (currentRefine.status == Refiner.Status.ACTIVE) {
                    parameters.append("state", FilterByOption.ACTIVE.wire)
                } else if (currentRefine.status == Refiner.Status.DONE) {
                    parameters.append("state", FilterByOption.NO_ACTIVE.wire)
                }

                if (currentRefine.searchQuery.isNotEmpty()) {
                    parameters.append("filterby", currentRefine.searchQueryType.wire)
                    parameters.append("filterval", currentRefine.searchQuery)
                } else if (currentRefine.filter == Refiner.Filter.DEPARTMENT) {
                    parameters.append("filterby", currentRefine.filter.wire)
                    parameters.append("filterval", currentRefine.filterValue.ifBlank { city })
                }

                parameters.append("viewtype", "onlymy1")
            }
        }

        if (!response.status.isSuccess()) {
            val body = runCatching { response.bodyAsText().take(2000) }.getOrNull()
            throw ClientRequestException(response, body ?: "HTTP ${response.status}")
        }

        val raw = response.bodyAsText()
        decodeOrWarning<List<SupplierOrderDto>>(json, raw)
    }

    suspend fun getIncomingApplications(
        apiConfig: ApiConfig,
        ncount: Int = 0,
        currentRefine: RefineState,
        city: String,
    ): Resource<List<IncomingApplicationDto>> = resourceify {
        val url = apiConfig.baseUrl
        val response = client.get(url) {
            url {
                parameters.append("token", apiConfig.token)
                parameters.append("task", "getitemslist")
                parameters.append("type", "Документ")
                parameters.append("name", "ВходящиеЗаявки")
                parameters.append("count", "30")
                parameters.append("ncount", "$ncount")
                if (currentRefine.orderBy != Refiner.OrderBy.OFF) {
                    parameters.append("orderby", currentRefine.orderBy.wire)
                    parameters.append("orderdir", currentRefine.orderDir.wire)
                } else {
                    parameters.append("orderdir", Refiner.Dir.DESC.wire)
                }
                if (currentRefine.status == Refiner.Status.ACTIVE) {
                    parameters.append("state", FilterByOption.ACTIVE.wire)
                } else if (currentRefine.status == Refiner.Status.DONE) {
                    parameters.append("state", FilterByOption.NO_ACTIVE.wire)
                }
                if (currentRefine.searchQuery.isNotEmpty()) {
                    parameters.append("filterby", currentRefine.searchQueryType.wire)
                    parameters.append("filterval", currentRefine.searchQuery)
                } else if (currentRefine.filter == Refiner.Filter.DEPARTMENT) {
                    parameters.append("filterby", currentRefine.filter.wire)
                    parameters.append("filterval", currentRefine.filterValue.ifBlank { city })
                }
                parameters.append("viewtype", "onlymy1")
            }
        }
        if (!response.status.isSuccess()) {
            val body = runCatching { response.bodyAsText().take(2000) }.getOrNull()
            throw ClientRequestException(response, body ?: "HTTP ${response.status}")
        }
        val raw = response.bodyAsText()
        decodeOrWarning<List<IncomingApplicationDto>>(json, raw)
    }

    suspend fun getExpenseRequests(
        apiConfig: ApiConfig,
        ncount: Int = 0,
        currentRefine: RefineState,
        city: String,
    ): Resource<List<ExpenseRequestDto>> = resourceify {
        val url = apiConfig.baseUrl
        val response = client.get(url) {
            url {
                parameters.append("token", apiConfig.token)
                parameters.append("task", "getitemslist")
                parameters.append("type", "Документ")
                parameters.append("name", "ЗаявкаНаРасход")
                parameters.append("count", "30")
                parameters.append("ncount", "$ncount")
                if (currentRefine.orderBy != Refiner.OrderBy.OFF) {
                    parameters.append("orderby", currentRefine.orderBy.wire)
                    parameters.append("orderdir", currentRefine.orderDir.wire)
                } else {
                    parameters.append("orderdir", Refiner.Dir.DESC.wire)
                }
                if (currentRefine.status == Refiner.Status.ACTIVE) {
                    parameters.append("state", FilterByOption.ACTIVE.wire)
                } else if (currentRefine.status == Refiner.Status.DONE) {
                    parameters.append("state", FilterByOption.NO_ACTIVE.wire)
                }
                if (currentRefine.searchQuery.isNotEmpty()) {
                    parameters.append("filterby", currentRefine.searchQueryType.wire)
                    parameters.append("filterval", currentRefine.searchQuery)
                } else if (currentRefine.filter == Refiner.Filter.DEPARTMENT) {
                    parameters.append("filterby", currentRefine.filter.wire)
                    parameters.append("filterval", currentRefine.filterValue.ifBlank { city })
                }
                parameters.append("viewtype", "onlymy1")
            }
        }
        if (!response.status.isSuccess()) {
            val body = runCatching { response.bodyAsText().take(2000) }.getOrNull()
            throw ClientRequestException(response, body ?: "HTTP ${response.status}")
        }
        val raw = response.bodyAsText()
        decodeOrWarning<List<ExpenseRequestDto>>(json, raw)
    }

    suspend fun getRepairTemplateCatalog(
        apiConfig: ApiConfig,
        ncount: Int = 0,
        currentRefine: RefineState,
        city: String,
    ): Resource<List<RepairTemplateCatalogItemDto>> = resourceify {
        val url = apiConfig.baseUrl
        val response = client.get(url) {
            url {
                parameters.append("token", apiConfig.token)
                parameters.append("task", "getitemslist")
                parameters.append("type", "Справочник")
                parameters.append("name", "ШаблоныРемонта")
                parameters.append("count", "30")
                parameters.append("ncount", "$ncount")
                parameters.append("orderby", "Наименование")
                parameters.append("orderdir", currentRefine.orderDir.wire)
                if (currentRefine.searchQuery.isNotEmpty()) {
                    val filterBy =
                        if (currentRefine.searchQueryType in Refiner.SearchQueryType.repairTemplateCatalogSearchTypes) {
                            currentRefine.searchQueryType.wire
                        } else {
                            Refiner.SearchQueryType.REPAIR_TEMPLATE_MODEL.wire
                        }
                    parameters.append("filterby", filterBy)
                    parameters.append("filterval", currentRefine.searchQuery)
                } else if (currentRefine.filter == Refiner.Filter.DEPARTMENT) {
                    parameters.append("filterby", currentRefine.filter.wire)
                    parameters.append("filterval", currentRefine.filterValue.ifBlank { city })
                }
            }
        }
        if (!response.status.isSuccess()) {
            val body = runCatching { response.bodyAsText().take(2000) }.getOrNull()
            throw ClientRequestException(response, body ?: "HTTP ${response.status}")
        }
        val raw = response.bodyAsText()
        decodeOrWarning<List<RepairTemplateCatalogItemDto>>(json, raw)
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

                if (currentRefine.searchQuery.isNotEmpty()) {
                    parameters.append("filterby", currentRefine.searchQueryType.wire)
                    parameters.append("filterval", currentRefine.searchQuery)
//                    parameters.append("filtertype", "list")
                } else if (currentRefine.filter == Refiner.Filter.DEPARTMENT) {
                    parameters.append("filterby", currentRefine.filter.wire)
                    parameters.append("filterval", currentRefine.filterValue)
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

    /** Список пар `permission` / `value` из `task=getpermission` (корень ответа — JSON-массив). */
    suspend fun getPermission(apiConfig: ApiConfig): Resource<List<UserPermissionEntryDto>> = resourceify {
        val response = client.get(apiConfig.baseUrl) {
            expectSuccess = true
            url {
                parameters.append("token", apiConfig.token)
                parameters.append("task", "getpermission")
            }
        }
        val raw = response.bodyAsText()
        decodeOrWarning<List<UserPermissionEntryDto>>(json, raw)
    }

    companion object {
        val json = Json {
            ignoreUnknownKeys = true
            isLenient = true
            explicitNulls = false
        }
    }
}

private data class ParsedCoreErrorPayload(
    val code: String? = null,
    val message: String? = null,
    val fields: List<CoreApiFieldError> = emptyList(),
)

private fun parseCoreErrorPayload(raw: String): ParsedCoreErrorPayload? {
    val text = raw.cleanJsonStart()
    if (text.isBlank()) return null
    return runCatching {
        val root = EventsApi.json.parseToJsonElement(text).jsonObject
        val unified = root["error"]?.jsonObject
        if (unified != null) {
            val fields = parseCoreFieldErrors(unified["fields"])
            val message = unified["message"]?.jsonPrimitive?.contentOrNull
                ?: fields.firstNotNullOfOrNull { it.message }
            ParsedCoreErrorPayload(
                code = unified["code"]?.jsonPrimitive?.contentOrNull,
                message = message,
                fields = fields
            )
        } else {
            val fields = parseCoreFieldErrors(root["detail"])
            val detailMessage = root["detail"]?.let { detail ->
                runCatching { detail.jsonPrimitive.contentOrNull }.getOrNull()
            }
            val message = root["message"]?.jsonPrimitive?.contentOrNull
                ?: detailMessage
                ?: fields.firstNotNullOfOrNull { it.message }
            ParsedCoreErrorPayload(
                code = root["code"]?.jsonPrimitive?.contentOrNull,
                message = message,
                fields = fields
            )
        }
    }.getOrNull()
}

private fun parseCoreFieldErrors(source: JsonElement?): List<CoreApiFieldError> {
    val array = runCatching { source?.jsonArray }.getOrNull() ?: return emptyList()
    return array.mapNotNull { element ->
        val obj = runCatching { element.jsonObject }.getOrNull()
            ?: return@mapNotNull CoreApiFieldError(
                message = element.jsonPrimitive.contentOrNull
            )
        if (obj.isEmpty()) return@mapNotNull null

        val locField = obj["loc"]?.jsonArray
            ?.mapNotNull { it.jsonPrimitive.contentOrNull }
            ?.lastOrNull()

        val field = obj["field"]?.jsonPrimitive?.contentOrNull
            ?: obj["name"]?.jsonPrimitive?.contentOrNull
            ?: locField
        val message = obj["message"]?.jsonPrimitive?.contentOrNull
            ?: obj["msg"]?.jsonPrimitive?.contentOrNull
            ?: obj["error"]?.jsonPrimitive?.contentOrNull
        val code = obj["code"]?.jsonPrimitive?.contentOrNull
            ?: obj["type"]?.jsonPrimitive?.contentOrNull

        if (field == null && message == null && code == null) null
        else CoreApiFieldError(field = field, message = message, code = code)
    }
}


private fun HttpStatusCode.isSuccess(): Boolean = value in 200..299
