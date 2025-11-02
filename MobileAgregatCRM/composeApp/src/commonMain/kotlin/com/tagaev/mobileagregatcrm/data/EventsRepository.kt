package com.tagaev.mobileagregatcrm.data

import com.tagaev.mobileagregatcrm.data.remote.ApiConfig
import com.tagaev.mobileagregatcrm.data.remote.EventsApi
import com.tagaev.mobileagregatcrm.data.remote.Resource
import com.tagaev.mobileagregatcrm.models.EventItemDto
import com.tagaev.mobileagregatcrm.models.SentMessageResponse

//import org.agregatcrm.utils.requestEventsList

// Repository that adapts EventsApi to our app needs
class EventsRepository(
    private val api: EventsApi,
    private val cfg: ApiConfig,
) {
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
            api = cfg,
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

    suspend fun sendMessage(number: String, date: String, message: String): Resource<SentMessageResponse> =
        api.sendMessage(api = cfg, number = number, date = date, message = message)
}
