package com.tagaev.trrcrm.ui.supplier_order

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.essenty.backhandler.BackCallback
import com.tagaev.trrcrm.data.AppSettings
import com.tagaev.trrcrm.data.AppSettingsKeys
import com.tagaev.trrcrm.data.MainRepository
import com.tagaev.trrcrm.data.remote.EventsApi.Companion.json
import com.tagaev.trrcrm.data.remote.Resource
import com.tagaev.trrcrm.domain.RefineState
import com.tagaev.trrcrm.domain.Refiner
import com.tagaev.trrcrm.domain.TreeRootResolvedDocument
import com.tagaev.trrcrm.models.SupplierOrderDto
import com.tagaev.trrcrm.models.WorkOrderMessageDto
import com.tagaev.trrcrm.ui.master_screen.IListMaster
import com.tagaev.trrcrm.ui.master_screen.MasterPanel
import com.tagaev.trrcrm.ui.master_screen.models.MessageModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

interface ISupplierOrdersComponent : IListMaster {
    val supplierOrders: StateFlow<Resource<List<SupplierOrderDto>>>
}

class SupplierOrdersComponent(
    componentContext: ComponentContext,
    private val onBack: () -> Unit,
) : ISupplierOrdersComponent,
    ComponentContext by componentContext,
    KoinComponent {

    companion object {
        private const val PAGE_SIZE = 30
        private val SUPPLIER_ORDERS_DEFAULT_REFINE = RefineState(
            orderBy = Refiner.OrderBy.DATE,
            orderDir = Refiner.Dir.DESC,
            searchQueryType = Refiner.SearchQueryType.CODE
        )
    }

    private val appScope: CoroutineScope by inject()
    private val repository: MainRepository by inject()
    private val appSettings: AppSettings by inject()

    private val _supplierOrders = MutableStateFlow<Resource<List<SupplierOrderDto>>>(Resource.Loading)
    override val supplierOrders: StateFlow<Resource<List<SupplierOrderDto>>> = _supplierOrders

    private val _refineState = MutableStateFlow(SUPPLIER_ORDERS_DEFAULT_REFINE)
    override val refineState: StateFlow<RefineState> = _refineState

    val _masterScreenPanel = MutableStateFlow(MasterPanel.List)
    override val masterScreenPanel: StateFlow<MasterPanel> = _masterScreenPanel

    private val _ncount = MutableStateFlow(0)
    override val ncount: StateFlow<Int> = _ncount

    private val _selectedGuid = MutableStateFlow<String?>(null)
    override val selectedItemGuid: StateFlow<String?> = _selectedGuid

    private val loadedOrders = mutableListOf<SupplierOrderDto>()
    private val loadedKeys = mutableSetOf<String>()

    val backCallback = BackCallback { _masterScreenPanel.value = MasterPanel.List }

    private fun SupplierOrderDto.key(): String = guid ?: (number ?: "") + "|" + (date ?: "")

    init {
        fullRefresh()
        backHandler.register(backCallback)
    }

    override fun selectItemFromList(guid: String?) {
        _selectedGuid.value = guid
    }

    override fun fullRefresh() {
        appScope.launch {
            _supplierOrders.value = Resource.Success(data = loadedOrders, additionalLoading = true)
            _refineState.value = loadRefineState()
            _ncount.value = 0

            val result = repository.loadSupplierOrders(0, refineState.value)
            if (result is Resource.Success) {
                val newItems = result.data
                loadedOrders.clear()
                loadedKeys.clear()
                for (item in newItems) {
                    val key = item.key()
                    if (loadedKeys.add(key)) loadedOrders.add(item)
                }
                _supplierOrders.value = Resource.Success(loadedOrders.toList(), additionalLoading = false)
            } else {
                _supplierOrders.value = result
            }
        }
    }

    override fun loadMore() {
        appScope.launch {
            _supplierOrders.value = Resource.Success(data = loadedOrders, additionalLoading = true)
            val nextOffset = _ncount.value + PAGE_SIZE
            val result = repository.loadSupplierOrders(nextOffset, refineState.value)
            if (result is Resource.Success) {
                _ncount.value = nextOffset
                val newItems = result.data
                for (item in newItems) {
                    val key = item.key()
                    if (loadedKeys.add(key)) loadedOrders.add(item)
                }
                _supplierOrders.value = Resource.Success(loadedOrders.toList(), additionalLoading = false)
            } else {
                _supplierOrders.value = result
            }
        }
    }

    override fun changePanel(masterDetailPanel: MasterPanel) {
        _masterScreenPanel.value = masterDetailPanel
    }

    override fun setRefineState(newState: RefineState) {
        _refineState.value = newState.copy(searchQuery = newState.searchQuery.trim())
        saveRefineState(_refineState.value)
        _ncount.value = 0
        fullRefresh()
    }

    override suspend fun sendMessage(itemNumber: String, itemDate: String, message: String): String? {
        return "Отправка сообщений для Заказа поставщику недоступна"
    }

    override suspend fun resolveBaseDocument(rawBaseDocument: String): Resource<TreeRootResolvedDocument> {
        return repository.resolveTreeRootDocument(rawBaseDocument)
    }

    override fun addLocalMessage(orderGuid: String?, message: MessageModel) {
        updateOrderLocally(orderGuid) { current ->
            val newMessage = WorkOrderMessageDto(
                author = message.author,
                comment = message.text,
                workDate = message.date
            )
            current.copy(messages = current.messages + newMessage)
        }
    }

    private fun updateOrderLocally(guid: String?, transform: (SupplierOrderDto) -> SupplierOrderDto) {
        if (guid == null) return
        val index = loadedOrders.indexOfFirst { it.guid == guid }
        if (index == -1) return
        loadedOrders[index] = transform(loadedOrders[index])
        val additionalLoading = (_supplierOrders.value as? Resource.Success)?.additionalLoading ?: false
        _supplierOrders.value = Resource.Success(data = loadedOrders.toList(), additionalLoading = additionalLoading)
    }

    private fun loadRefineState(): RefineState {
        val raw = appSettings.getStringOrNull(AppSettingsKeys.SUPPLIER_ORDERS_REFINE_STATE)
        if (raw.isNullOrBlank()) return SUPPLIER_ORDERS_DEFAULT_REFINE
        return runCatching { json.decodeFromString<RefineState>(raw) }.getOrDefault(SUPPLIER_ORDERS_DEFAULT_REFINE)
    }

    private fun saveRefineState(state: RefineState) {
        val raw = runCatching { json.encodeToString(state) }.getOrNull() ?: return
        appSettings.setString(AppSettingsKeys.SUPPLIER_ORDERS_REFINE_STATE, raw)
    }
}

