package com.tagaev.trrcrm.ui.events

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.essenty.backhandler.BackCallback
import com.tagaev.secrets.Secrets
import com.tagaev.trrcrm.data.AppSettings
import com.tagaev.trrcrm.data.AppSettingsKeys
import com.tagaev.trrcrm.data.MainRepository
import com.tagaev.trrcrm.data.remote.EventsApi.Companion.json
import com.tagaev.trrcrm.data.remote.Resource
import com.tagaev.trrcrm.domain.RefineState
import com.tagaev.trrcrm.models.EventItemDto
import com.tagaev.trrcrm.models.MessageDto
import com.tagaev.trrcrm.ui.master_screen.IListMaster
import com.tagaev.trrcrm.ui.master_screen.MasterPanel
import com.tagaev.trrcrm.ui.master_screen.models.MessageModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import kotlin.collections.orEmpty
import kotlin.collections.plus
import kotlin.getValue

interface IEventsComponent : IListMaster {
    val events: StateFlow<Resource<List<EventItemDto>>>
    var pickedEvent: EventItemDto?
}

class EventsComponent(
    componentContext: ComponentContext,
    private val onBack: () -> Unit
) : IEventsComponent, ComponentContext by componentContext, KoinComponent {


    companion object {
        private const val PAGE_SIZE = 30
    }

    private val appScope: CoroutineScope by inject()
    private val repository: MainRepository by inject()
    private val appSettings: AppSettings by inject()

    private val _events =
        MutableStateFlow<Resource<List<EventItemDto>>>(Resource.Loading)
    override val events: StateFlow<Resource<List<EventItemDto>>> = _events

    private val _refineState = MutableStateFlow(RefineState.Default)
    override val refineState: StateFlow<RefineState> = _refineState

    val _masterScreenPanel = MutableStateFlow(MasterPanel.List)
    override val masterScreenPanel: StateFlow<MasterPanel> = _masterScreenPanel

    private val _ncount = MutableStateFlow(0)
    override val ncount: StateFlow<Int> = _ncount

    private val _selectedOrderGuid = MutableStateFlow<String?>(null)
    override val selectedItemGuid: StateFlow<String?> = _selectedOrderGuid

    override var pickedEvent: EventItemDto? = null

    override fun selectItemFromList(guid: String?) {
        _selectedOrderGuid.value = guid
    }

    // simple in-memory cache of loaded work orders for future DB caching
    private val loadedEvents = mutableListOf<EventItemDto>()

    private val loadedKeys = mutableSetOf<String>()

    val backCallback = BackCallback { _masterScreenPanel.value = MasterPanel.List }

    private fun EventItemDto.key(): String =
        guid ?: (number ?: "") + "|" + (date ?: "")

    init {
        fullRefresh()
        backHandler.register(backCallback)
    }

    override fun fullRefresh() {
        println(">>> fullRefresh")
        appScope.launch {
            //_workOrders.value = Resource.Loading
            _events.value = Resource.Success(data = loadedEvents, additionalLoading = true)


            _refineState.value = loadRefineState()
            _ncount.value = 0

            val result = repository.loadEvents(0, refineState.value)
            if (result is Resource.Success) {

                val newItems = result.data ?: emptyList()

                loadedEvents.clear()
                loadedKeys.clear()

                for (order in newItems) {
                    val key = order.key()
                    if (loadedKeys.add(key)) {
                        loadedEvents.add(order)
                    }
                }
                _events.value = Resource.Success(loadedEvents.toList(), additionalLoading = false)
            } else {
                _events.value = result
            }
        }
    }

    override fun loadMore() {
        println(">>> LOAD More")
        appScope.launch {
//            if ( _workOrders.value is Resource.Loading) return@launch
//            _workOrders.value = Resource.Loading
            _events.value = Resource.Success(data = loadedEvents, additionalLoading = true)
            val nextOffset = _ncount.value + PAGE_SIZE
            val result = repository.loadEvents(nextOffset, refineState.value)
            if (result is Resource.Success) {
                _ncount.value = nextOffset
                val newItems = result.data ?: emptyList()
                for (order in newItems) {
                    val key = order.key()
                    if (loadedKeys.add(key)) {
                        loadedEvents.add(order)
                    }
                }
                _events.value = Resource.Success(loadedEvents.toList(), additionalLoading = false)
            } else {
                _events.value = result
            }
        }
    }

    override fun changePanel(masterDetailPanel: MasterPanel) {
        _masterScreenPanel.value = masterDetailPanel
    }


    override fun setRefineState(newState: RefineState) {
        _refineState.value = newState.copy(searchQuery = newState.searchQuery.trimStart().trimEnd())
        saveRefineState(_refineState.value)
        _ncount.value = 0
        fullRefresh()
    }

    override suspend fun sendMessage(itemNumber: String, itemDate: String, message: String): String? {
        if (itemNumber.isBlank() || itemDate.isBlank() || message.isBlank()) return "Нет номера или даты документа"
        val res = repository.sendMessageEvent(
            itemNumber,
            itemDate.substringBefore(' '),
            message
        )

        val users = pickedEvent?.users?.mapNotNull { it.user }
        val author = appSettings.getStringOrNull(AppSettingsKeys.PERSONAL_DATA)
        if (!users.isNullOrEmpty() && !author.isNullOrBlank()) {
            repository.sendMessageEventPUSH(
                docId = "${pickedEvent?.number}",
                docTitle = "Событие ${pickedEvent?.number} (${pickedEvent?.companyDepartment})",
                authorName = appSettings.getString(AppSettingsKeys.PERSONAL_DATA, "NO Name"),
                recipientNames = users,
                message = "${author}:\n${message}"
            )
        }

        return when (res) {
            is Resource.Success -> null
            is Resource.Error -> res.causes ?: res.exception?.message ?: "Ошибка отправки сообщения"
            else -> "Ошибка отправки сообщения"
        }
    }
    // ---------- Work Orders refine state ----------

    fun loadRefineState(): RefineState {
        val raw = appSettings.getStringOrNull(AppSettingsKeys.EVENTS_REFINE_STATE)
        if (raw.isNullOrBlank()) {
            // default state when nothing stored
            return RefineState()
        }

        return runCatching {
            json.decodeFromString<RefineState>(raw)
        }.getOrElse {
            // if schema changed or data corrupted – fail gracefully
            RefineState()
        }
    }

    fun saveRefineState(state: RefineState) {
        val encoded = json.encodeToString(state)
        appSettings.setString(AppSettingsKeys.EVENTS_REFINE_STATE, encoded)
    }
    /**
     * Convenience helper: applies a local "add message" operation to a WorkOrder
     * identified by [orderGuid], using a simple MessageModel from the UI.
     * It constructs a WorkOrderMessageDto and appends it to the existing list
     * of messages, then emits the updated list to observers.
     */
    override fun addLocalMessage(
        orderGuid: String?,
        message: MessageModel
    ) {
        updateOrderLocally(orderGuid) { currentOrder ->
            val newMessage = MessageDto(
                author = message.author,
                comment = message.text,
                workDate = message.date
            )

            val updatedMessages = currentOrder.messages.orEmpty() + newMessage

            currentOrder.copy(messages = updatedMessages)
        }
    }
    /**
     * Applies a local update to a single WorkOrder in the in-memory cache,
     * identified by its guid. The provided [transform] receives the current
     * WorkOrderDto and must return an updated copy (e.g. with a new message
     * appended to its messages list).
     *
     * After updating, this method emits a new Resource.Success to [_workOrders]
     * so the UI can immediately reflect the change without a fullRefresh().
     */
    private fun updateOrderLocally(
        guid: String?,
        transform: (EventItemDto) -> EventItemDto
    ) {
        if (guid == null) return

        val index = loadedEvents.indexOfFirst { it.guid == guid }
        if (index == -1) return

        val currentOrder = loadedEvents[index]
        val updatedOrder = transform(currentOrder)

        loadedEvents[index] = updatedOrder

        // preserve current additionalLoading flag if present
        val current = _events.value
        val additionalLoading = (current as? Resource.Success)?.additionalLoading ?: false

        _events.value = Resource.Success(
            data = loadedEvents.toList(),
            additionalLoading = additionalLoading
        )
    }
}
