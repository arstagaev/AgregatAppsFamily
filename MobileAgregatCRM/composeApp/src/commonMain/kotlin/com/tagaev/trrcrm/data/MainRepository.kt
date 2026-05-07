package com.tagaev.trrcrm.data

import com.tagaev.data.models.qrscanner.QRResponseTRS
import com.tagaev.trrcrm.data.remote.ApiConfig
import com.tagaev.trrcrm.data.remote.EventsApi
import com.tagaev.trrcrm.data.remote.Resource
import com.tagaev.trrcrm.models.UserPermissionEntryDto
import com.tagaev.trrcrm.domain.RefineState
import com.tagaev.trrcrm.domain.DocumentTypes
import com.tagaev.trrcrm.domain.TreeRootDocument
import com.tagaev.trrcrm.domain.TreeRootDocumentKind
import com.tagaev.trrcrm.domain.TreeRootResolvedDocument
import com.tagaev.trrcrm.models.CargoDto
import com.tagaev.trrcrm.models.BuyerOrderDto
import com.tagaev.trrcrm.models.ComplaintDto
import com.tagaev.trrcrm.models.CoreDeviceRegisterRequest
import com.tagaev.trrcrm.models.CoreDeviceRegisterResponse
import com.tagaev.trrcrm.models.CoreNotificationIntentRequest
import com.tagaev.trrcrm.models.CoreNotificationIntentResponse
import com.tagaev.trrcrm.models.CoreResolveRecipientsRequest
import com.tagaev.trrcrm.models.CoreResolveRecipientsResponse
import com.tagaev.trrcrm.models.CoreSessionBootstrapRequest
import com.tagaev.trrcrm.models.CoreSessionBootstrapResponse
import com.tagaev.trrcrm.models.CoreSessionHeartbeatRequest
import com.tagaev.trrcrm.models.CoreSessionHeartbeatResponse
import com.tagaev.trrcrm.models.CoreSessionLogoutRequest
import com.tagaev.trrcrm.models.CoreSessionLogoutResponse
import com.tagaev.trrcrm.models.EventItemDto
import com.tagaev.trrcrm.models.IncomingApplicationDto
import com.tagaev.trrcrm.models.RepairTemplateCatalogItemDto
import com.tagaev.trrcrm.models.GetTokenResponse
import com.tagaev.trrcrm.models.InnerOrderDto
import com.tagaev.trrcrm.models.SentMessageResponse
import com.tagaev.trrcrm.models.SupplierOrderDto
import com.tagaev.trrcrm.models.ThreadMessageResponse
import com.tagaev.trrcrm.models.WorkOrderDto
import com.tagaev.trrcrm.utils.DefaultValuesConst
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import kotlin.getValue

//import org.agregatcrm.utils.requestEventsList

// Repository that adapts EventsApi to our app needs
class MainRepository(
    private val api: EventsApi,
    private val cfg: ApiConfig,
): KoinComponent {
    private val settings: AppSettings by inject()

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

    suspend fun loadRepairTemplateCatalog(ncount: Int, currentRefine: RefineState): Resource<List<RepairTemplateCatalogItemDto>> =
        api.getRepairTemplateCatalog(cfg, ncount, currentRefine, settings.getString(AppSettingsKeys.DEPARTMENT, ""))

    suspend fun loadWorkOrders(ncount: Int, currentRefine: RefineState): Resource<List<WorkOrderDto>> =
        runCatching { api.loadWorkOrders(cfg, ncount, currentRefine, settings.getString(AppSettingsKeys.DEPARTMENT, "")) }
            .fold(
                onSuccess = { Resource.Success(it) },
                onFailure = {
                    Resource.Error(
                        exception = it as Exception?,
                        causes = it.message ?: "Ошибка загрузки заказ-нарядов"
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
                        causes = it.message ?: "Ошибка загрузки комплектаций"
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
        message: String
    ): Resource<ThreadMessageResponse> {
        val resolveOnlyIntent = CoreNotificationIntentRequest(
            source_system = "mobile_agregatcrm",
            event_type = "thread_message",
            title = docTitle,
            body = message,
            recipient_names = recipientNames,
            actor_full_name = authorName,
            send_now = false
        )
        val coreRequest = CoreNotificationIntentRequest(
            source_system = "mobile_agregatcrm",
            event_type = "thread_message",
            title = docTitle,
            body = message,
            recipient_names = recipientNames,
            actor_full_name = authorName,
            send_now = true
        )
        // Resolve-only pass keeps intent semantics explicit and helps diagnostics.
        api.coreNotificationIntent(resolveOnlyIntent)
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
