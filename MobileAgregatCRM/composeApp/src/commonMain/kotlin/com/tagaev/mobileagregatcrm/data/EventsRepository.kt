package com.tagaev.mobileagregatcrm.data

import com.tagaev.data.models.qrscanner.QRResponseTRS
import com.tagaev.mobileagregatcrm.data.remote.ApiConfig
import com.tagaev.mobileagregatcrm.data.remote.EventsApi
import com.tagaev.mobileagregatcrm.data.remote.Resource
import com.tagaev.mobileagregatcrm.models.EventItemDto
import com.tagaev.mobileagregatcrm.models.GetTokenResponse
import com.tagaev.mobileagregatcrm.models.SentMessageResponse
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import kotlin.getValue

//import org.agregatcrm.utils.requestEventsList

// Repository that adapts EventsApi to our app needs
class EventsRepository(
    private val api: EventsApi,
    private val cfg: ApiConfig,
): KoinComponent {
    private val settings: AppSettings by inject()

    suspend fun loadEvents(
        type: String = "Документ",
        name: String = "Событие",
        count: Int = 1,
        ncount: Int = 50,
        orderBy: String = "Дата",
        orderDir: String = "desc",
        filterBy: String = "ПодразделениеКомпании",
        filterVal: String,
    ): Resource<List<EventItemDto>> {
//        val req = requestEventsList.value
        return api.getEvents(
            api = cfg.copy(token = settings.getString(AppSettingsKeys.TOKEN_KEY, defaultValue = "NULL")),
            type = type,
            name = name,
            count = count,
            ncount = ncount,
            orderBy = orderBy,
            orderDir = orderDir,
            filterBy = filterBy,
            filterVal = filterVal
        )
    }

    suspend fun getToken(username: String, password: String): Resource<GetTokenResponse> = api.getToken(cfg, username, password)

    suspend fun sendMessage(number: String, date: String, message: String): Resource<SentMessageResponse> =
        api.sendMessage(api = cfg, number = number, date = date, message = message)

    suspend fun getTRSData(decodedCode: String): Resource<QRResponseTRS> = api.getTRSData(apiConfig = cfg, decodedCode = decodedCode)
//    suspend fun fetchTRS(decoded: String): Result<QRResponseTRS>
}
