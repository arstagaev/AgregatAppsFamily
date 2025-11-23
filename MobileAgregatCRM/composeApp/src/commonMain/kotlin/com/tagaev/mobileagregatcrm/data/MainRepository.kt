package com.tagaev.mobileagregatcrm.data

import com.tagaev.data.models.qrscanner.QRResponseTRS
import com.tagaev.mobileagregatcrm.data.remote.ApiConfig
import com.tagaev.mobileagregatcrm.data.remote.EventsApi
import com.tagaev.mobileagregatcrm.data.remote.Resource
import com.tagaev.mobileagregatcrm.domain.RefineState
import com.tagaev.mobileagregatcrm.feature.DocumentTypes
import com.tagaev.mobileagregatcrm.models.EventItemDto
import com.tagaev.mobileagregatcrm.models.GetTokenResponse
import com.tagaev.mobileagregatcrm.models.SentMessageResponse
import com.tagaev.mobileagregatcrm.models.WorkOrderDto
import com.tagaev.mobileagregatcrm.utils.DefaultValuesConst
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

    suspend fun sendMessageEvent(
        number: String,
        date: String,
        message: String
    ): Resource<SentMessageResponse> = api.sendMessage(api = cfg, documentType = DocumentTypes.EVENT, number = number, date = date, message = message)


    suspend fun sendMessageToWorkOrder(
        number: String,
        date: String,
        message: String
    ): Resource<SentMessageResponse> = api.sendMessage(api = cfg, documentType = DocumentTypes.WORK_ORDER, number = number, date = date, message = message)
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
