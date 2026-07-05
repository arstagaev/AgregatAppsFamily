package com.tagaev.trrcrm.data

import com.tagaev.data.models.qrscanner.QRResponseTRS
import com.tagaev.trrcrm.data.remote.ApiConfig
import com.tagaev.trrcrm.data.remote.CoreApiErrorKind
import com.tagaev.trrcrm.data.remote.EventsApi
import com.tagaev.trrcrm.data.remote.Resource
import com.tagaev.trrcrm.data.remote.toCoreApiError
import com.tagaev.trrcrm.data.remote.friendlyError
import com.tagaev.trrcrm.models.UserPermissionEntryDto
import com.tagaev.trrcrm.domain.RefineState
import com.tagaev.trrcrm.domain.DocumentTypes
import com.tagaev.trrcrm.domain.TreeRootDocument
import com.tagaev.trrcrm.domain.TreeRootDocumentKind
import com.tagaev.trrcrm.domain.TreeRootResolvedDocument
import com.tagaev.trrcrm.models.CargoDto
import com.tagaev.trrcrm.models.BuyerOrderDto
import com.tagaev.trrcrm.models.ComplaintDto
import com.tagaev.trrcrm.models.CatalogProductRequest
import com.tagaev.trrcrm.models.CatalogProductRequestContact
import com.tagaev.trrcrm.models.CatalogProductRequestItem
import com.tagaev.trrcrm.models.CatalogProductRequestResponse
import com.tagaev.trrcrm.models.CatalogProductsResponse
import com.tagaev.trrcrm.models.CatalogSignupRequest
import com.tagaev.trrcrm.models.CatalogSignupResponse
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
import com.tagaev.trrcrm.models.EventItemDto
import com.tagaev.trrcrm.models.ExpenseRequestDto
import com.tagaev.trrcrm.models.IncomingApplicationDto
import com.tagaev.trrcrm.models.RepairTemplateCatalogItemDto
import com.tagaev.trrcrm.models.GetTokenResponse
import com.tagaev.trrcrm.models.InnerOrderDto
import com.tagaev.trrcrm.models.SentMessageResponse
import com.tagaev.trrcrm.models.SupplierOrderDto
import com.tagaev.trrcrm.models.ThreadMessageResponse
import com.tagaev.trrcrm.models.WorkOrderDto
import com.tagaev.trrcrm.utils.DefaultValuesConst
import com.tagaev.trrcrm.data.AppSettingsKeys
import com.tagaev.trrcrm.data.remote.ImageMediatorApi
import com.tagaev.trrcrm.data.remote.canUploadBlockedMessage
import com.tagaev.trrcrm.data.remote.toImageMediatorError
import com.tagaev.trrcrm.domain.exceedsHardMax
import com.tagaev.trrcrm.domain.isValidDocumentNumber
import com.tagaev.trrcrm.domain.normalizeDocumentNumber
import com.tagaev.trrcrm.models.ImageMediatorUploadResult
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import kotlin.getValue
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

//import org.agregatcrm.utils.requestEventsList

// Repository that adapts EventsApi to our app needs
class MainRepository(
    private val api: EventsApi,
    private val cfg: ApiConfig,
): KoinComponent {
    companion object {
        private const val INTENT_TITLE_MAX = 40
        private const val INTENT_SUBTITLE_MAX = 50
        private const val INTENT_BODY_MAX = 120
        private const val PUSH_TOGGLE_CACHE_TTL_MS = 5 * 60 * 1000L
    }

    private val settings: AppSettings by inject()
    private val imageMediatorApi: ImageMediatorApi by inject()

    suspend fun loadEvents(
        type: String? = null,
        name: String? = null,
        count: Int?,
        ncount: Int?,
        orderBy: String?,
        orderDir: String?,
        filterBy: String?,
        filterVal: String?,
    ): Resource<List<EventItemDto>> {
        return api.getEvents(
            api = cfg.copy(token = settings.getString(AppSettingsKeys.TOKEN_KEY, defaultValue = "NULL")),
            type = type ?: DefaultValuesConst.TYPE,
            name = name ?: DefaultValuesConst.NAME,
            count = count ?: DefaultValuesConst.COUNT,
            ncount = ncount ?: DefaultValuesConst.NCOUNT,
            orderBy = orderBy ?: DefaultValuesConst.ORDER_BY,
            orderDir = orderDir ?: DefaultValuesConst.ORDER_DIR,
            filterBy = filterBy ?: DefaultValuesConst.FILTER_BY,
            filterVal = filterVal ?: DefaultValuesConst.FILTER_VAL
        )
    }

    suspend fun getToken(username: String, password: String): Resource<GetTokenResponse> = api.getToken(cfg, username, password)

    suspend fun probeStartup(): Resource<Unit> = api.probeStartup(cfg)

    suspend fun getPermission(): Resource<List<UserPermissionEntryDto>> = api.getPermission(cfg)

    suspend fun sendMessage(number: String, date: String, message: String): Resource<SentMessageResponse> =
        api.sendMessage(api = cfg, documentType = DocumentTypes.EVENT, number = number, date = date, message = message)

    suspend fun getTRSData(decodedCode: String): Resource<QRResponseTRS> = api.getTRSData(apiConfig = cfg, decodedCode = decodedCode)

    suspend fun loadEvents(ncount: Int, currentRefine: RefineState): Resource<List<EventItemDto>> =
        api.getEvents(cfg, ncount, currentRefine, settings.getString(AppSettingsKeys.DEPARTMENT, ""))
//        runCatching {  }
//            .fold(
//                onSuccess = { Resource.Success(it) },
//                onFailure = {
//                    Resource.Error(
//                        exception = it as Exception?,
//                        causes = it.message ?: "Ошибка загрузки заказ-нарядов"
//                    )
//                }
//            )

    suspend fun loadCargos(ncount: Int, currentRefine: RefineState): Resource<List<CargoDto>> =
        api.getCargos(cfg, ncount, currentRefine, settings.getString(AppSettingsKeys.DEPARTMENT, ""))

    suspend fun loadComplaints(ncount: Int, currentRefine: RefineState): Resource<List<ComplaintDto>> =
        api.getComplaints(cfg, ncount, currentRefine, settings.getString(AppSettingsKeys.DEPARTMENT, ""))

    suspend fun loadInnerOrders(ncount: Int, currentRefine: RefineState): Resource<List<InnerOrderDto>> =
        api.getInnerOrders(cfg, ncount, currentRefine, settings.getString(AppSettingsKeys.DEPARTMENT, ""))

    suspend fun loadBuyerOrders(ncount: Int, currentRefine: RefineState): Resource<List<BuyerOrderDto>> =
        api.getBuyerOrders(cfg, ncount, currentRefine, settings.getString(AppSettingsKeys.DEPARTMENT, ""))

    suspend fun loadSupplierOrders(ncount: Int, currentRefine: RefineState): Resource<List<SupplierOrderDto>> =
        api.getSupplierOrders(cfg, ncount, currentRefine, settings.getString(AppSettingsKeys.DEPARTMENT, ""))

    suspend fun loadIncomingApplications(ncount: Int, currentRefine: RefineState): Resource<List<IncomingApplicationDto>> =
        api.getIncomingApplications(cfg, ncount, currentRefine, settings.getString(AppSettingsKeys.DEPARTMENT, ""))

    suspend fun loadExpenseRequests(ncount: Int, currentRefine: RefineState): Resource<List<ExpenseRequestDto>> =
        api.getExpenseRequests(cfg, ncount, currentRefine, settings.getString(AppSettingsKeys.DEPARTMENT, ""))

    suspend fun loadRepairTemplateCatalog(ncount: Int, currentRefine: RefineState): Resource<List<RepairTemplateCatalogItemDto>> =
        api.getRepairTemplateCatalog(cfg, ncount, currentRefine, settings.getString(AppSettingsKeys.DEPARTMENT, ""))

    suspend fun loadWorkOrders(ncount: Int, currentRefine: RefineState): Resource<List<WorkOrderDto>> =
        runCatching { api.loadWorkOrders(cfg, ncount, currentRefine, settings.getString(AppSettingsKeys.DEPARTMENT, "")) }
            .fold(
                onSuccess = { Resource.Success(it) },
                onFailure = {
                    Resource.Error(
                        exception = it as Exception?,
                        causes = friendlyError(it, "Ошибка загрузки заказ-нарядов")
                    )
                }
            )

    suspend fun loadComplectations(ncount: Int, currentRefine: RefineState): Resource<List<WorkOrderDto>> =
        runCatching { api.loadComplectations(cfg, ncount, currentRefine, settings.getString(AppSettingsKeys.DEPARTMENT, "")) }
            .fold(
                onSuccess = { Resource.Success(it) },
                onFailure = {
                    Resource.Error(
                        exception = it as Exception?,
                        causes = friendlyError(it, "Ошибка загрузки комплектаций")
                    )
                }
            )

    suspend fun resolveTreeRootDocument(rawBaseDocument: String): Resource<TreeRootResolvedDocument> {
        val parsed = TreeRootDocument.parse(rawBaseDocument)
            ?: return Resource.Error(causes = "Не удалось определить документ-основание")
        val apiCfg = cfg.copy(token = settings.getString(AppSettingsKeys.TOKEN_KEY, defaultValue = "NULL"))

        return when (parsed.kind) {
            TreeRootDocumentKind.EVENT -> {
                when (val result = api.findDocumentsByNumber<EventItemDto>(apiCfg, parsed.requestName, parsed.documentNumber)) {
                    is Resource.Success -> {
                        val item = result.data.firstOrNull()
                        if (item == null) Resource.Error(causes = "Документ-основание не найден")
                        else Resource.Success(TreeRootResolvedDocument.Event(item))
                    }
                    is Resource.Error -> Resource.Error(result.exception, result.causes)
                    is Resource.Loading -> Resource.Loading
                }
            }
            TreeRootDocumentKind.WORK_ORDER -> {
                when (val result = api.findDocumentsByNumber<WorkOrderDto>(apiCfg, parsed.requestName, parsed.documentNumber)) {
                    is Resource.Success -> {
                        val item = result.data.firstOrNull()
                        if (item == null) Resource.Error(causes = "Документ-основание не найден")
                        else Resource.Success(TreeRootResolvedDocument.WorkOrder(item))
                    }
                    is Resource.Error -> Resource.Error(result.exception, result.causes)
                    is Resource.Loading -> Resource.Loading
                }
            }
            TreeRootDocumentKind.COMPLECTATION -> {
                when (val result = api.findDocumentsByNumber<WorkOrderDto>(apiCfg, parsed.requestName, parsed.documentNumber)) {
                    is Resource.Success -> {
                        val item = result.data.firstOrNull()
                        if (item == null) Resource.Error(causes = "Документ-основание не найден")
                        else Resource.Success(TreeRootResolvedDocument.Complectation(item))
                    }
                    is Resource.Error -> Resource.Error(result.exception, result.causes)
                    is Resource.Loading -> Resource.Loading
                }
            }
            TreeRootDocumentKind.COMPLAINT -> {
                when (val result = api.findDocumentsByNumber<ComplaintDto>(apiCfg, parsed.requestName, parsed.documentNumber)) {
                    is Resource.Success -> {
                        val item = result.data.firstOrNull()
                        if (item == null) Resource.Error(causes = "Документ-основание не найден")
                        else Resource.Success(TreeRootResolvedDocument.Complaint(item))
                    }
                    is Resource.Error -> Resource.Error(result.exception, result.causes)
                    is Resource.Loading -> Resource.Loading
                }
            }
            TreeRootDocumentKind.INNER_ORDER -> {
                when (val result = api.findDocumentsByNumber<InnerOrderDto>(apiCfg, parsed.requestName, parsed.documentNumber)) {
                    is Resource.Success -> {
                        val item = result.data.firstOrNull()
                        if (item == null) Resource.Error(causes = "Документ-основание не найден")
                        else Resource.Success(TreeRootResolvedDocument.InnerOrder(item))
                    }
                    is Resource.Error -> Resource.Error(result.exception, result.causes)
                    is Resource.Loading -> Resource.Loading
                }
            }
            TreeRootDocumentKind.BUYER_ORDER -> {
                when (val result = api.findDocumentsByNumber<BuyerOrderDto>(apiCfg, parsed.requestName, parsed.documentNumber)) {
                    is Resource.Success -> {
                        val item = result.data.firstOrNull()
                        if (item == null) Resource.Error(causes = "Документ-основание не найден")
                        else Resource.Success(TreeRootResolvedDocument.BuyerOrder(item))
                    }
                    is Resource.Error -> Resource.Error(result.exception, result.causes)
                    is Resource.Loading -> Resource.Loading
                }
            }
            TreeRootDocumentKind.SUPPLIER_ORDER -> {
                when (val result = api.findDocumentsByNumber<SupplierOrderDto>(apiCfg, parsed.requestName, parsed.documentNumber)) {
                    is Resource.Success -> {
                        val item = result.data.firstOrNull()
                        if (item == null) Resource.Error(causes = "Документ-основание не найден")
                        else Resource.Success(TreeRootResolvedDocument.SupplierOrder(item))
                    }
                    is Resource.Error -> Resource.Error(result.exception, result.causes)
                    is Resource.Loading -> Resource.Loading
                }
            }
            TreeRootDocumentKind.CARGO -> {
                when (val result = api.findDocumentsByNumber<CargoDto>(apiCfg, parsed.requestName, parsed.documentNumber)) {
                    is Resource.Success -> {
                        val item = result.data.firstOrNull()
                        if (item == null) Resource.Error(causes = "Документ-основание не найден")
                        else Resource.Success(TreeRootResolvedDocument.Cargo(item))
                    }
                    is Resource.Error -> Resource.Error(result.exception, result.causes)
                    is Resource.Loading -> Resource.Loading
                }
            }
            TreeRootDocumentKind.EXPENSE_REQUEST -> {
                when (val result = api.findDocumentsByNumber<ExpenseRequestDto>(apiCfg, parsed.requestName, parsed.documentNumber)) {
                    is Resource.Success -> {
                        val item = result.data.firstOrNull()
                        if (item == null) Resource.Error(causes = "Документ-основание не найден")
                        else Resource.Success(TreeRootResolvedDocument.ExpenseRequest(item))
                    }
                    is Resource.Error -> Resource.Error(result.exception, result.causes)
                    is Resource.Loading -> Resource.Loading
                }
            }
        }
    }

    /// MESSAGES /////
    suspend fun sendMessageEvent(
        number: String,
        date: String,
        message: String
    ): Resource<SentMessageResponse> = api.sendMessage(api = cfg, documentType = DocumentTypes.EVENT, number = number, date = date, message = message)

    suspend fun sendMessageInnerOrder(
        number: String,
        date: String,
        message: String
    ): Resource<SentMessageResponse> = api.sendMessage(api = cfg, documentType = DocumentTypes.INNER_ORDER, number = number, date = date, message = message)

    suspend fun sendMessageComplaint(
        number: String,
        date: String,
        message: String
    ): Resource<SentMessageResponse> = api.sendMessage(api = cfg, documentType = DocumentTypes.COMPLAINT, number = number, date = date, message = message)

    suspend fun sendMessageToWorkOrder(
        number: String,
        date: String,
        message: String
    ): Resource<SentMessageResponse> = api.sendMessage(api = cfg, documentType = DocumentTypes.WORK_ORDER, number = number, date = date, message = message)

    suspend fun sendMessageToComplectation(
        number: String,
        date: String,
        message: String
    ): Resource<SentMessageResponse> = api.sendMessage(api = cfg, documentType = DocumentTypes.COMPLECTATION, number = number, date = date, message = message)

    suspend fun sendMessageBuyerOrder(
        number: String,
        date: String,
        message: String
    ): Resource<SentMessageResponse> = api.sendMessage(api = cfg, documentType = DocumentTypes.BUYER_ORDER, number = number, date = date, message = message)

    suspend fun sendMessageEventPUSH(
        docId: String,
        docTitle: String,
        authorName: String,
        recipientNames: List<String>,
        message: String,
        screen: String,
        subtitle: String? = null,
        rawMessage: String = message
    ): Resource<ThreadMessageResponse> {
        if (!isPushFeatureEnabled()) {
            println("PUSH_SERVICE: push feature disabled, skipping intent send")
            return Resource.Success(
                ThreadMessageResponse(
                    status = "skipped",
                    success = 0,
                    failure = 0,
                    recipients = recipientNames.size
                )
            )
        }

        val normalizedScreen = screen.trim().lowercase().replace('-', '_').replace(' ', '_')
        val boundedTitle = clampIntentText(docTitle, INTENT_TITLE_MAX)
        val boundedSubtitle = subtitle?.let { clampIntentText(it, INTENT_SUBTITLE_MAX) }?.ifBlank { null }
        val boundedBody = clampIntentText(message, INTENT_BODY_MAX)
        val searchType = when (normalizedScreen) {
            "events", "event", "work_orders", "workorders", "work_order", "workorder" -> "CODE"
            "complectation", "complectations", "complectation_orders" -> "KIT_CHARACTERISTIC"
            else -> null
        }
        val payload = buildJsonObject {
            put("screen", normalizedScreen)
            put("doc_title", docTitle)
            put("search_key", docId)
            if (searchType != null) {
                put("search_query_type", searchType)
            }
            put("message_text", rawMessage)
        }
        val coreRequest = CoreNotificationIntentRequest(
            source_system = "mobile_agregatcrm",
            event_type = "thread_message",
            title = boundedTitle,
            subtitle = boundedSubtitle,
            body = boundedBody,
            recipient_names = recipientNames,
            actor_full_name = authorName,
            payload = payload,
            send_now = true
        )

        // Optional preflight: keep sending flow unchanged unless backend explicitly rejects recipient count.
        val resolveRes = api.coreResolveRecipients(CoreResolveRecipientsRequest(recipient_names = recipientNames))
        if (resolveRes is Resource.Error) {
            val mapped = resolveRes.exception.toCoreApiError("Не удалось проверить получателей")
            val rawMessage = resolveRes.causes.orEmpty().lowercase()
            if (mapped.kind == CoreApiErrorKind.Validation && "too many recipients" in rawMessage) {
                return Resource.Error(causes = "Слишком много получателей для уведомления")
            }
            println("PUSH_SERVICE: resolve recipients preflight failed, continue intent send: ${resolveRes.causes ?: resolveRes.exception?.message}")
        }

        val coreRes = api.coreNotificationIntent(coreRequest)
        return when (coreRes) {
            is Resource.Success -> {
                val status = coreRes.data.status ?: "sent"
                val success = coreRes.data.success ?: recipientNames.size
                val failure = coreRes.data.failure ?: 0
                Resource.Success(
                    ThreadMessageResponse(
                        status = status,
                        success = success,
                        failure = failure,
                        recipients = recipientNames.size
                    )
                )
            }
            is Resource.Error -> {
                if (shouldFallbackToLegacyPush(coreRes.causes ?: coreRes.exception?.message)) {
                    api.sendThreadMessage(
                        api = cfg,
                        docId = docId,
                        docTitle = docTitle,
                        authorName = authorName,
                        recipientNames = recipientNames,
                        messageText = message
                    )
                } else {
                    Resource.Error(coreRes.exception, coreRes.causes)
                }
            }
            is Resource.Loading -> Resource.Loading
        }
    }

    suspend fun coreSessionBootstrap(request: CoreSessionBootstrapRequest): Resource<CoreSessionBootstrapResponse> =
        api.coreSessionBootstrap(request)

    suspend fun coreSessionHeartbeat(request: CoreSessionHeartbeatRequest): Resource<CoreSessionHeartbeatResponse> =
        api.coreSessionHeartbeat(request)

    suspend fun coreSessionLogout(request: CoreSessionLogoutRequest): Resource<CoreSessionLogoutResponse> =
        api.coreSessionLogout(request)

    suspend fun coreDeviceRegister(request: CoreDeviceRegisterRequest): Resource<CoreDeviceRegisterResponse> =
        api.coreDeviceRegister(request)

    suspend fun coreResolveRecipients(recipientNames: List<String>): Resource<CoreResolveRecipientsResponse> =
        api.coreResolveRecipients(CoreResolveRecipientsRequest(recipient_names = recipientNames))

    suspend fun coreNotificationIntent(request: CoreNotificationIntentRequest): Resource<CoreNotificationIntentResponse> =
        api.coreNotificationIntent(request)

    suspend fun coreNotificationsFeed(
        sessionId: String,
        limit: Int = 25,
        page: Int = 1,
        cursor: String? = null,
        searchQuery: String? = null,
        statusFilter: String = "all",
    ): Resource<CoreNotificationsFeedResponse> = api.coreNotificationsFeed(
        CoreNotificationsFeedRequest(
            sessionId = sessionId,
            limit = limit,
            page = page,
            cursor = cursor,
            searchQuery = searchQuery,
            statusFilter = statusFilter
        )
    )

    suspend fun coreNotificationsUnreadCount(sessionId: String): Resource<CoreNotificationsUnreadCountResponse> =
        api.coreNotificationsUnreadCount(CoreNotificationsUnreadCountRequest(sessionId = sessionId))

    suspend fun coreNotificationStatusUpdate(
        sessionId: String,
        notificationId: Long,
        status: String,
        source: String? = null,
    ): Resource<CoreNotificationStatusUpdateResponse> = api.coreNotificationStatusUpdate(
        CoreNotificationStatusUpdateRequest(
            sessionId = sessionId,
            notificationId = notificationId,
            status = status,
            source = source
        )
    )

    suspend fun coreNotificationsReadAll(sessionId: String): Resource<CoreNotificationsReadAllResponse> =
        api.coreNotificationsReadAll(CoreNotificationsReadAllRequest(sessionId = sessionId))

    suspend fun coreHealth(): Resource<HealthResponse> = api.coreHealth()

    suspend fun corePushFeatureToggleGet(): Resource<PushFeatureToggleGetResponse> = api.corePushFeatureToggleGet()

    suspend fun corePushFeatureToggleSet(enabled: Boolean): Resource<PushFeatureToggleSetResponse> =
        api.corePushFeatureToggleSet(PushFeatureToggleSetRequest(enabled = enabled))

    suspend fun coreDeviceMuteState(sessionId: String): Resource<CoreDeviceMuteStateResponse> =
        api.coreDeviceMuteState(CoreDeviceMuteStateRequest(sessionId = sessionId))

    suspend fun coreDeviceMuteSetAll(sessionId: String, muteAll: Boolean): Resource<CoreDeviceMuteStateResponse> =
        api.coreDeviceMuteUpdate(
            CoreDeviceMuteUpdateRequest(
                sessionId = sessionId,
                muteAll = muteAll
            )
        )

    suspend fun coreDeviceMuteSetType(
        sessionId: String,
        documentType: String,
        muted: Boolean,
    ): Resource<CoreDeviceMuteStateResponse> = api.coreDeviceMuteUpdate(
        CoreDeviceMuteUpdateRequest(
            sessionId = sessionId,
            documentType = documentType,
            muted = muted
        )
    )

    suspend fun refreshPushFeatureToggleIfNeeded(force: Boolean = false): Boolean {
        val now = currentTimeMillis()
        val lastSync = settings.getLong(AppSettingsKeys.PUSH_FEATURE_TOGGLE_UPDATED_AT_MS, 0L)
        val cached = settings.getBool(AppSettingsKeys.PUSH_FEATURE_TOGGLE_ENABLED, true)

        if (!force && now - lastSync < PUSH_TOGGLE_CACHE_TTL_MS) {
            return cached
        }

        return when (val res = corePushFeatureToggleGet()) {
            is Resource.Success -> {
                val enabled = res.data.enabled ?: true
                settings.setBool(AppSettingsKeys.PUSH_FEATURE_TOGGLE_ENABLED, enabled)
                settings.setLong(AppSettingsKeys.PUSH_FEATURE_TOGGLE_UPDATED_AT_MS, now)
                enabled
            }
            is Resource.Error -> {
                println("PUSH_SERVICE: push feature toggle fetch failed, fallback cached=$cached reason=${res.causes ?: res.exception?.message}")
                cached
            }
            is Resource.Loading -> cached
        }
    }

    suspend fun isPushFeatureEnabled(): Boolean = refreshPushFeatureToggleIfNeeded(force = false)

    suspend fun catalogProducts(
        search: String? = null,
        brand: String? = null,
        model: String? = null,
        categoryId: String? = null,
        page: Int = 1,
        limit: Int = 20,
    ): Resource<CatalogProductsResponse> = api.catalogProducts(
        search = search,
        brand = brand,
        model = model,
        categoryId = categoryId,
        page = page,
        limit = limit,
    )

    suspend fun catalogSignupRequest(
        name: String,
        phone: String?,
        email: String,
    ): Resource<CatalogSignupResponse> = api.catalogSignupRequest(
        CatalogSignupRequest(
            name = name.trim(),
            phone = phone?.trim().takeUnless { it.isNullOrBlank() },
            email = email.trim(),
            source = "mobile_app",
            crmSessionId = settings.getStringOrNull(AppSettingsKeys.CORE_SESSION_ID),
            crmLogin = settings.getStringOrNull(AppSettingsKeys.EMAIL),
        )
    )

    suspend fun catalogProductRequest(
        contactName: String,
        contactEmail: String,
        contactPhone: String?,
        contactCompany: String?,
        contactAddress: String?,
        items: List<CatalogProductRequestItem>,
    ): Resource<CatalogProductRequestResponse> = api.catalogProductRequest(
        CatalogProductRequest(
            contact = CatalogProductRequestContact(
                name = contactName.trim(),
                email = contactEmail.trim(),
                phone = contactPhone?.trim().takeUnless { it.isNullOrBlank() },
                company = contactCompany?.trim().takeUnless { it.isNullOrBlank() },
                address = contactAddress?.trim().takeUnless { it.isNullOrBlank() },
            ),
            items = items,
            source = "mobile_app",
            crmSessionId = settings.getStringOrNull(AppSettingsKeys.CORE_SESSION_ID),
            crmLogin = settings.getStringOrNull(AppSettingsKeys.EMAIL),
            crmFullName = settings.getStringOrNull(AppSettingsKeys.PERSONAL_DATA),
        )
    )

    private fun shouldFallbackToLegacyPush(message: String?): Boolean {
        val raw = message?.lowercase().orEmpty()
        if (raw.isBlank()) return true
        if ("client error 401" in raw || "client error 403" in raw || "client error 422" in raw) return false
        return "server error" in raw ||
            "timeout" in raw ||
            "unresolvedaddress" in raw ||
            "unknownhost" in raw ||
            "network" in raw ||
            "failed to connect" in raw
    }

    private fun clampIntentText(text: String, maxLen: Int): String {
        val normalized = text.trim()
        if (normalized.length <= maxLen) return normalized
        return normalized.take(maxLen)
    }

    @OptIn(ExperimentalTime::class)
    private fun currentTimeMillis(): Long = Clock.System.now().toEpochMilliseconds()

    suspend fun checkCanUploadFixatorPhotos(
        documentNumber: String,
        documentName: String = "Комплектация",
    ): Resource<Unit> {
        val token = settings.getString(AppSettingsKeys.TOKEN_KEY, "").trim()
        if (token.isBlank()) {
            return Resource.Error(causes = "Нет токена авторизации. Войдите заново.")
        }

        val normalizedNumber = normalizeDocumentNumber(documentNumber)
        if (!isValidDocumentNumber(normalizedNumber)) {
            return Resource.Error(causes = "Некорректный номер документа")
        }

        return when (val precheck = imageMediatorApi.canUpload(token, normalizedNumber, documentName)) {
            is Resource.Success -> {
                val data = precheck.data
                if (!data.folderFound || !data.allowed) {
                    Resource.Error(causes = canUploadBlockedMessage(data))
                } else {
                    Resource.Success(Unit)
                }
            }
            is Resource.Error -> Resource.Error(
                exception = precheck.exception,
                causes = precheck.causes ?: precheck.exception.toImageMediatorError(
                    "Не удалось проверить возможность загрузки",
                ),
            )
            is Resource.Loading -> Resource.Loading
        }
    }

    suspend fun uploadFixatorPhotos(
        documentNumber: String,
        photos: List<ByteArray>,
        documentName: String = "Camera fixator",
    ): Resource<ImageMediatorUploadResult> {
        val token = settings.getString(AppSettingsKeys.TOKEN_KEY, "").trim()
        if (token.isBlank()) {
            return Resource.Error(causes = "Нет токена авторизации. Войдите заново.")
        }
        if (photos.isEmpty()) {
            return Resource.Error(causes = "Нет фотографий для отправки")
        }

        val normalizedNumber = normalizeDocumentNumber(documentNumber)
        if (!isValidDocumentNumber(normalizedNumber)) {
            return Resource.Error(causes = "Номер документа: 6–12 цифр")
        }

        val oversized = photos.withIndex().firstOrNull { (_, bytes) -> exceedsHardMax(bytes.size) }
        if (oversized != null) {
            return Resource.Error(
                causes = "Фото ${oversized.index + 1} превышает 5 МБ. Переснимите или удалите его.",
            )
        }

        when (val precheck = imageMediatorApi.canUpload(token, normalizedNumber, documentName)) {
            is Resource.Success -> {
                val data = precheck.data
                if (!data.folderFound || !data.allowed) {
                    return Resource.Error(causes = canUploadBlockedMessage(data))
                }
            }
            is Resource.Error -> {
                return Resource.Error(
                    exception = precheck.exception,
                    causes = precheck.causes ?: precheck.exception.toImageMediatorError("Не удалось проверить возможность загрузки"),
                )
            }
            is Resource.Loading -> Unit
        }

        return when (val upload = imageMediatorApi.uploadPhotos(token, normalizedNumber, photos, documentName)) {
            is Resource.Success -> {
                val data = upload.data
                Resource.Success(
                    ImageMediatorUploadResult(
                        documentNumber = data.documentNumber,
                        storedFilenames = data.uploadedFiles.mapNotNull { it.storedFilename },
                        ftpFolderPath = data.ftpFolderPath,
                        resolvedYear = data.resolvedYear,
                        resolvedMonth = data.resolvedMonth,
                    )
                )
            }
            is Resource.Error -> Resource.Error(
                exception = upload.exception,
                causes = upload.causes ?: upload.exception.toImageMediatorError("Не удалось отправить фото"),
            )
            is Resource.Loading -> Resource.Loading
        }
    }


    //          api.sendMessage(api = cfg, number = number, date = date, message = message)
//        runCatching { api.sendMessage(api = cfg, number = number,date = date, message = message) }
//            .fold(
//                onSuccess = { Resource.Success(it) },
//                onFailure = {
//                    Resource.Error(
//                        exception = it,
//                        causes = it.message ?: "Ошибка отправки комментария"
//                    )
//                }
//            )

//    suspend fun fetchTRS(decoded: String): Result<QRResponseTRS>
}
