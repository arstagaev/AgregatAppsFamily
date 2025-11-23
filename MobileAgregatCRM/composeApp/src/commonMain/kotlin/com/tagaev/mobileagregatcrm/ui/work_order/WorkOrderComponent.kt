package com.tagaev.mobileagregatcrm.ui.work_order

import com.tagaev.mobileagregatcrm.data.remote.Resource
import com.tagaev.mobileagregatcrm.models.WorkOrderDto
import kotlinx.coroutines.flow.StateFlow
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.essenty.backhandler.BackCallback
import com.tagaev.mobileagregatcrm.data.AppSettings
import com.tagaev.mobileagregatcrm.data.AppSettingsKeys
import com.tagaev.mobileagregatcrm.data.EventsRepository
import com.tagaev.mobileagregatcrm.data.remote.EventsApi.Companion.json
import com.tagaev.mobileagregatcrm.domain.WorkOrderRefineState
import com.tagaev.mobileagregatcrm.ui.master_screen.MasterPanel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject


interface IWorkOrdersComponent {
    val workOrders: StateFlow<Resource<List<WorkOrderDto>>>
    val ncount: StateFlow<Int>
//    val isLoadingMore: StateFlow<Boolean>
//    val currentFilter: StateFlow<FilterByOption>
    val refineState: StateFlow<WorkOrderRefineState>
    val masterScreenPanel: StateFlow<MasterPanel>
    val selectedOrderGuid: StateFlow<String?>


    fun setRefineState(newState: WorkOrderRefineState)
    fun selectOrder(guid: String?)
    fun fullRefresh()
    fun loadMore()
    fun changePanel(masterDetailPanel: MasterPanel)
    fun sendMessage(orderNumber: String, orderDate: String, message: String)
//    fun setFilter(filter: FilterByOption)
}



class WorkOrdersComponent(
    componentContext: ComponentContext,
    private val onBack: () -> Unit,
) : IWorkOrdersComponent,
    ComponentContext by componentContext,
    KoinComponent {

    companion object {
        private const val PAGE_SIZE = 12
    }

    private val appScope: CoroutineScope by inject()
    private val repository: EventsRepository by inject()
    private val appSettings: AppSettings by inject()

    private val _workOrders =
        MutableStateFlow<Resource<List<WorkOrderDto>>>(Resource.Loading)
    override val workOrders: StateFlow<Resource<List<WorkOrderDto>>> = _workOrders

    private val _refineState = MutableStateFlow(WorkOrderRefineState.Default)
    override val refineState: StateFlow<WorkOrderRefineState> = _refineState

    private val _masterScreenPanel = MutableStateFlow(MasterPanel.List)
    override val masterScreenPanel: StateFlow<MasterPanel> = _masterScreenPanel

    private val _ncount = MutableStateFlow(0)
    override val ncount: StateFlow<Int> = _ncount

    private val _selectedOrderGuid = MutableStateFlow<String?>(null)
    override val selectedOrderGuid: StateFlow<String?> = _selectedOrderGuid

    override fun selectOrder(guid: String?) {
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

    override fun fullRefresh() {
        appScope.launch {
            _workOrders.value = Resource.Loading
            _ncount.value = 0
            loadedOrders.clear()
            loadedKeys.clear()

            _refineState.value = loadRefineState()

            val result = repository.loadWorkOrders(_ncount.value, refineState.value)
            if (result is Resource.Success) {
                val newItems = result.data ?: emptyList()
                for (order in newItems) {
                    val key = order.key()
                    if (loadedKeys.add(key)) {
                        loadedOrders.add(order)
                    }
                }
                _workOrders.value = Resource.Success(loadedOrders.toList())
            } else {
                _workOrders.value = result
            }
        }
    }

    override fun loadMore() {
        appScope.launch {
//            if ( _workOrders.value is Resource.Loading) return@launch
//            _workOrders.value = Resource.Loading
            _workOrders.value = Resource.Success(data = loadedOrders, additionalLoading = true)
            val nextOffset = _ncount.value + PAGE_SIZE
            val result = repository.loadWorkOrders(nextOffset, refineState.value)
            if (result is Resource.Success) {
                _ncount.value = nextOffset
                val newItems = result.data ?: emptyList()
                for (order in newItems) {
                    val key = order.key()
                    if (loadedKeys.add(key)) {
                        loadedOrders.add(order)
                    }
                }
                _workOrders.value = Resource.Success(loadedOrders.toList(), additionalLoading = false)
            } else {
                _workOrders.value = result
            }

//            _isLoadingMore.value = false
        }
    }

    override fun changePanel(masterDetailPanel: MasterPanel) {
        _masterScreenPanel.value = masterDetailPanel
    }


    override fun setRefineState(newState: WorkOrderRefineState) {
        _refineState.value = newState
        fullRefresh()
        saveRefineState(newState)
    }

    override fun sendMessage(orderNumber: String, orderDate: String, message: String) {
        if (orderNumber.isBlank() || orderDate.isBlank() || message.isBlank()) return
        println("orderDate ${orderDate}  == ${orderDate.substringBefore(' ')}")
        appScope.launch {
            val res = repository.sendMessageToWorkOrder(orderNumber, orderDate.substringBefore(' '), message)
            if (res is Resource.Success) {
                // после успешной отправки перезагружаем, чтобы в messages появился новый коммент
                fullRefresh()
            }
        }
    }


    // ---------- Work Orders refine state ----------

    fun loadRefineState(): WorkOrderRefineState {
        val raw = appSettings.getStringOrNull(AppSettingsKeys.WORK_ORDERS_REFINE_STATE)
        if (raw.isNullOrBlank()) {
            // default state when nothing stored
            return WorkOrderRefineState()
        }

        return runCatching {
            json.decodeFromString<WorkOrderRefineState>(raw)
        }.getOrElse {
            // if schema changed or data corrupted – fail gracefully
            WorkOrderRefineState()
        }
    }

    fun saveRefineState(state: WorkOrderRefineState) {
        val encoded = json.encodeToString(state)
        appSettings.setString(AppSettingsKeys.WORK_ORDERS_REFINE_STATE, encoded)
    }
}