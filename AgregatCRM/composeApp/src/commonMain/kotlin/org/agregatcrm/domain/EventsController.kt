package org.agregatcrm.domain
// commonMain/kotlin/com/example/events/logic/EventsController.kt
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.agregatcrm.data.remote.ApiConfig
import org.agregatcrm.data.remote.EventsApi
import org.agregatcrm.models.EventItemDto
import org.agregatcrm.repository.EventsRepository
import org.agregatcrm.utils.requestEventsList
import org.agregatcrm.utils.Log

class EventsController(
    private val scope: CoroutineScope,
    private val repo: EventsRepository
) {
    private val _state = MutableStateFlow<List<EventItemDto>>(emptyList())
    val state = _state.asStateFlow()
    
    fun addNewEvents(
        count: Int = requestEventsList.value.count,
        ncount: Int = requestEventsList.value.ncount,
    ) {
        Log.info("EventsController refresh")
        scope.launch(Dispatchers.Default) {
            val items = repo.loadEvents(
                apiConfig = ApiConfig(),
                count = count,
                ncount = ncount,

                orderDir = requestEventsList.value.orderBy.wire,
                orderBy  = requestEventsList.value.orderDir.wire,

                filterBy  = requestEventsList.value.filterBy,
                filterVal = requestEventsList.value.filterVal
            )

            Log.info("repo.loadEvents ${items.getOrNull()}")
            _state.value = items.getOrNull() ?: listOf()
        }
    }
    fun fullRefresh(
        count: Int = requestEventsList.value.count,
        ncount: Int = requestEventsList.value.ncount,
        // Sort/Order:
        orderBy: String =  requestEventsList.value.orderBy.wire,
        orderDir: String = requestEventsList.value.orderDir.wire,
        // FILTER:
        filterBy: String =  requestEventsList.value.filterBy,
        filterVal: String = requestEventsList.value.filterVal
    ) {
        Log.info("EventsController refresh")
        scope.launch(Dispatchers.Default) {
            val items = repo.loadEvents(
                apiConfig = ApiConfig(),
                count = count,
                ncount = ncount,
                orderDir = orderDir,
                orderBy = orderBy,
                filterBy = filterBy,
                filterVal = filterVal
            )
            Log.info("repo.loadEvents ${items.getOrNull()}")
            _state.value = items.getOrNull() ?: listOf()
        }
    }
}

// helper to quickly wire
fun provideEventsController(scope: CoroutineScope): EventsController {
    val api = EventsApi()
    val repo = EventsRepository(api)
    return EventsController(scope, repo)
}
