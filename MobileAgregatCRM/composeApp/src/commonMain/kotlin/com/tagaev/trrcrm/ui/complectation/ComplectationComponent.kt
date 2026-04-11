package com.tagaev.trrcrm.ui.complectation

import com.tagaev.trrcrm.data.remote.Resource
import com.tagaev.trrcrm.models.WorkOrderDto
import com.tagaev.trrcrm.models.WorkOrderMessageDto
import kotlinx.coroutines.flow.StateFlow
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.essenty.backhandler.BackCallback
import com.tagaev.trrcrm.data.AppSettings
import com.tagaev.trrcrm.data.AppSettingsKeys
import com.tagaev.trrcrm.data.MainRepository
import com.tagaev.trrcrm.data.remote.EventsApi.Companion.json
import com.tagaev.trrcrm.domain.RefineState
import com.tagaev.trrcrm.domain.Refiner
import com.tagaev.trrcrm.ui.master_screen.IListMaster
import com.tagaev.trrcrm.ui.master_screen.MasterPanel
import com.tagaev.trrcrm.ui.master_screen.models.MessageModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

interface IComplectationComponent: IListMaster {
    val complectations: StateFlow<Resource<List<WorkOrderDto>>>
}

class ComplectationComponent(
    componentContext: ComponentContext,
    private val onBack: () -> Unit,
) : IComplectationComponent,
    ComponentContext by componentContext,
    KoinComponent {

    companion object {
        private const val PAGE_SIZE = 30
    }

    private val appScope: CoroutineScope by inject()
    private val repository: MainRepository by inject()
    private val appSettings: AppSettings by inject()

    private val _complectations =
        MutableStateFlow<Resource<List<WorkOrderDto>>>(Resource.Loading)
    override val complectations: StateFlow<Resource<List<WorkOrderDto>>> = _complectations

    private val _refineState = MutableStateFlow(RefineState.Default)
    override val refineState: StateFlow<RefineState> = _refineState

    val _masterScreenPanel = MutableStateFlow(MasterPanel.List)
    override val masterScreenPanel: StateFlow<MasterPanel> = _masterScreenPanel

    private val _ncount = MutableStateFlow(0)
    override val ncount: StateFlow<Int> = _ncount

    private val _selectedOrderGuid = MutableStateFlow<String?>(null)
    override val selectedItemGuid: StateFlow<String?> = _selectedOrderGuid

    override fun selectItemFromList(guid: String?) {
        _selectedOrderGuid.value = guid
    }

    // simple in-memory cache of loaded work orders for future DB caching
    private val loadedOrders = mutableListOf<WorkOrderDto>()

    private val loadedKeys = mutableSetOf<String>()



    val backCallback = BackCallback { _masterScreenPanel.value = MasterPanel.List }

    private fun WorkOrderDto.key(): String =
        guid ?: (number ?: "") + "|" + (date ?: "")

    init {
        fullRefresh()
        backHandler.register(backCallback)
    }

//    fun insertNewMessage() {
//        _complectations
//    }

    override fun fullRefresh() {
        appScope.launch {
            //_complectations.value = Resource.Loading
            _complectations.value = Resource.Success(data = loadedOrders, additionalLoading = true)


            _refineState.value = loadRefineState()
            _ncount.value = 0

            val result = repository.loadComplectations(0, refineState.value)
            if (result is Resource.Success) {

                val newItems = result.data ?: emptyList()

                loadedOrders.clear()
                loadedKeys.clear()

                for (order in newItems) {
                    val key = order.key()
                    if (loadedKeys.add(key)) {
                        loadedOrders.add(order)
                    }
                }
                _complectations.value = Resource.Success(loadedOrders.toList(), additionalLoading = false)
            } else {
                _complectations.value = result
            }
        }
    }

    override fun loadMore() {
        appScope.launch {
//            if ( _complectations.value is Resource.Loading) return@launch
//            _complectations.value = Resource.Loading
            _complectations.value = Resource.Success(data = loadedOrders, additionalLoading = true)
            val nextOffset = _ncount.value + PAGE_SIZE
            val result = repository.loadComplectations(nextOffset, refineState.value)
            if (result is Resource.Success) {
                _ncount.value = nextOffset
                val newItems = result.data ?: emptyList()
                for (order in newItems) {
                    val key = order.key()
                    if (loadedKeys.add(key)) {
                        loadedOrders.add(order)
                    }
                }
                _complectations.value = Resource.Success(loadedOrders.toList(), additionalLoading = false)
            } else {
                _complectations.value = result
            }

//            _isLoadingMore.value = false
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

    override suspend fun sendMessage(itemNumber: String, itemDate: String, message: String): Boolean {
        println("orderDate $itemDate  == ${itemDate.substringBefore(' ')}")
        if (itemNumber.isBlank() || itemDate.isBlank() || message.isBlank()) return false
        // call repository directly (suspend) – no extra launch here
        val res = repository.sendMessageToComplectation(
            itemNumber,
            itemDate.substringBefore(' '),
            message
        )

        return if (res is Resource.Success) {
            // after successful send, refresh list so messages include new comment
            //fullRefresh()   // still runs on appScope internally

            true
        } else {
            false
        }
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
            val newMessage = WorkOrderMessageDto(
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
     * After updating, this method emits a new Resource.Success to [_complectations]
     * so the UI can immediately reflect the change without a fullRefresh().
     */
    private fun updateOrderLocally(
        guid: String?,
        transform: (WorkOrderDto) -> WorkOrderDto
    ) {
        if (guid == null) return

        val index = loadedOrders.indexOfFirst { it.guid == guid }
        if (index == -1) return

        val currentOrder = loadedOrders[index]
        val updatedOrder = transform(currentOrder)

        loadedOrders[index] = updatedOrder

        // preserve current additionalLoading flag if present
        val current = _complectations.value
        val additionalLoading = (current as? Resource.Success)?.additionalLoading ?: false

        _complectations.value = Resource.Success(
            data = loadedOrders.toList(),
            additionalLoading = additionalLoading
        )
    }
    // ---------- Work Orders refine state ----------

    fun loadRefineState(): RefineState {
        val raw = appSettings.getStringOrNull(AppSettingsKeys.COMPLECTATION_REFINE_STATE)
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
        appSettings.setString(AppSettingsKeys.COMPLECTATION_REFINE_STATE, encoded)
    }
}
