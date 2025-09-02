package org.agregatcrm.data.mappers

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import org.agregatcrm.data.remote.ApiConfig
import org.agregatcrm.data.remote.EventsApi
import org.agregatcrm.data.remote.Resource
import org.agregatcrm.models.EventItemDto

fun EventsApi.eventsFlow(
    api: ApiConfig,
    type: String = "Документ",
    name: String = "Событие",
    count: Int = 5,
    ncount: Int = 50,
    orderBy: String = "Дата",
    orderDir: String = "desc",
    filterBy: String = "ПодразделениеКомпании",
    filterVal: String = "Воронеж"
): Flow<Resource<List<EventItemDto>>> = flow {
//    emit(Resource.Loading)
//    when (val result = ApigetEventsResource(api, type, name, count, ncount, orderBy, orderDir, filterBy, filterVal)) {
//        is Resource.Success -> emit(result)
//        is Resource.Error -> emit(result)
//        Resource.Loading -> {} // not expected here
//    }
}