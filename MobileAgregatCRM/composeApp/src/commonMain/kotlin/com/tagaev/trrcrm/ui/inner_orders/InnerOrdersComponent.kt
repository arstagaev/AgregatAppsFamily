package com.tagaev.trrcrm.ui.inner_orders

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.essenty.backhandler.BackCallback
import com.tagaev.trrcrm.data.AppSettings
import com.tagaev.trrcrm.data.AppSettingsKeys
import com.tagaev.trrcrm.data.MainRepository
import com.tagaev.trrcrm.data.remote.EventsApi.Companion.json
import com.tagaev.trrcrm.data.remote.Resource
import com.tagaev.trrcrm.domain.RefineState
import com.tagaev.trrcrm.domain.Refiner
import com.tagaev.trrcrm.models.InnerOrderDto
import com.tagaev.trrcrm.ui.master_screen.MasterPanel
import com.tagaev.trrcrm.ui.master_screen.models.MessageModel
import com.tagaev.trrcrm.ui.work_order.IListMaster
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

interface IInnerOrdersComponent : IListMaster {
    fun back()

    val innerOrders: StateFlow<Resource<List<InnerOrderDto>>>
}

class InnerOrdersComponent(
    componentContext: ComponentContext,
    private val onBack: () -> Unit
) : IInnerOrdersComponent, ComponentContext by componentContext, KoinComponent {
    override fun back() = onBack()
    companion object { private const val PAGE_SIZE = 30 }

    private val appScope: CoroutineScope by inject()
    private val repository: MainRepository by inject()
    private val appSettings: AppSettings by inject()

    private val _innerOrders =
        MutableStateFlow<Resource<List<InnerOrderDto>>>(Resource.Loading)
    override val innerOrders: StateFlow<Resource<List<InnerOrderDto>>> = _innerOrders

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
    private val loadedCargos = mutableListOf<InnerOrderDto>()

    private val loadedKeys = mutableSetOf<String>()

    val backCallback = BackCallback { _masterScreenPanel.value = MasterPanel.List }

    private fun InnerOrderDto.key(): String =
        guid ?: (number ?: "") + "|" + (date ?: "")

    init {
        fullRefresh()
        backHandler.register(backCallback)
    }

    override fun fullRefresh() {
        println(">>> fullRefresh")
        appScope.launch {
            //_workOrders.value = Resource.Loading
            _innerOrders.value = Resource.Success(data = loadedCargos, additionalLoading = true)


            _refineState.value = loadRefineState()

            val result = repository.loadInnerOrders(_ncount.value, refineState.value)
            if (result is Resource.Success) {

                val newItems = result.data ?: emptyList()

                _ncount.value = 0
                loadedCargos.clear()
                loadedKeys.clear()

                for (order in newItems) {
                    val key = order.key()
                    if (loadedKeys.add(key)) {
                        loadedCargos.add(order)
                    }
                }
                _innerOrders.value = Resource.Success(loadedCargos.toList(), additionalLoading = false)
            } else {
                _innerOrders.value = result
            }
        }
    }

    override fun loadMore() {
        println(">>> LOAD More")
        appScope.launch {
//            if ( _workOrders.value is Resource.Loading) return@launch
//            _workOrders.value = Resource.Loading
            _innerOrders.value = Resource.Success(data = loadedCargos, additionalLoading = true)
            val nextOffset = _ncount.value + PAGE_SIZE
            val result = repository.loadInnerOrders(nextOffset, refineState.value)
            if (result is Resource.Success) {
                _ncount.value = nextOffset
                val newItems = result.data ?: emptyList()
                for (order in newItems) {
                    val key = order.key()
                    if (loadedKeys.add(key)) {
                        loadedCargos.add(order)
                    }
                }
                _innerOrders.value = Resource.Success(loadedCargos.toList(), additionalLoading = false)
            } else {
                _innerOrders.value = result
            }
        }
    }

    override fun changePanel(masterDetailPanel: MasterPanel) {
        _masterScreenPanel.value = masterDetailPanel
    }


    override fun setRefineState(newState: RefineState) {
        _refineState.value = newState.copy(searchQuery = newState.searchQuery.trimStart().trimEnd())
        saveRefineState(_refineState.value)
        fullRefresh()
    }

    override suspend fun sendMessage(itemNumber: String, itemDate: String, message: String): Boolean {
        println("orderDate $itemDate  == ${itemDate.substringBefore(' ')}")
        return false
    }
    // ---------- Work Orders refine state ----------

    fun loadRefineState(): RefineState {
        val raw = appSettings.getStringOrNull(AppSettingsKeys.INNERORDER_REFINE_STATE)
        if (raw.isNullOrBlank()) {
            // default state when nothing stored
            return RefineState(
                orderBy = Refiner.OrderBy.DATE
            )
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
        appSettings.setString(AppSettingsKeys.INNERORDER_REFINE_STATE, encoded)
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
        transform: (InnerOrderDto) -> InnerOrderDto
    ) {
        if (guid == null) return

        val index = loadedCargos.indexOfFirst { it.guid == guid }
        if (index == -1) return

        val currentOrder = loadedCargos[index]
        val updatedOrder = transform(currentOrder)

        loadedCargos[index] = updatedOrder

        // preserve current additionalLoading flag if present
        val current = _innerOrders.value
        val additionalLoading = (current as? Resource.Success)?.additionalLoading ?: false

        _innerOrders.value = Resource.Success(
            data = loadedCargos.toList(),
            additionalLoading = additionalLoading
        )
    }
}

enum class CargoStatus(val value: String) {
    PROPOSAL("Заявка"),
    SENT("Отправлено"),
    RECEIVED("Получено"),
    IN_WORK("В работе.Поиск Перевозчика"),
    PROPOSAL_FOR_GET_CARGO("Заявка на забор груза"),
    SENT_TO_MAIN_DEPT("Отправлено в УК"),
    WAIT_FOR_LOAD_CAR_FOUND("Авто Найден.Ожидается Загрузка"),
}
