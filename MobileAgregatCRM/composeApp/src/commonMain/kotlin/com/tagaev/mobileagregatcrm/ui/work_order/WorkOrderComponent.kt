package com.tagaev.mobileagregatcrm.ui.work_order

import com.tagaev.mobileagregatcrm.data.remote.Resource
import com.tagaev.mobileagregatcrm.models.WorkOrderDto
import kotlinx.coroutines.flow.StateFlow
import com.arkivanov.decompose.ComponentContext
import com.tagaev.mobileagregatcrm.data.AppSettings
import com.tagaev.mobileagregatcrm.data.AppSettingsKeys
import com.tagaev.mobileagregatcrm.data.EventsRepository
import com.tagaev.mobileagregatcrm.feature.FilterByOption
import com.tagaev.mobileagregatcrm.feature.toFilterByOption
import com.tagaev.mobileagregatcrm.utils.formatDDMMYYYY
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.datetime.format
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject


interface IWorkOrdersComponent {
    val workOrders: StateFlow<Resource<List<WorkOrderDto>>>
    val ncount: StateFlow<Int>
    val isLoadingMore: StateFlow<Boolean>
    val currentFilter: StateFlow<FilterByOption>


    fun fullRefresh()
    fun loadMore()

    fun sendMessage(orderNumber: String, orderDate: String, message: String)
    fun setFilter(filter: FilterByOption)
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

    private val _currentFilter = MutableStateFlow(appSettings.getStringOrNull(AppSettingsKeys.FILTER_STATE_WO,).toFilterByOption())
    override val currentFilter: StateFlow<FilterByOption> = _currentFilter

    private val _ncount = MutableStateFlow(0)
    override val ncount: StateFlow<Int> = _ncount

    private val _isLoadingMore = MutableStateFlow(false)
    override val isLoadingMore: StateFlow<Boolean> = _isLoadingMore

    // simple in-memory cache of loaded work orders for future DB caching
    private val loadedOrders = mutableListOf<WorkOrderDto>()

    private val loadedKeys = mutableSetOf<String>()

    private fun WorkOrderDto.key(): String =
        guid ?: (number ?: "") + "|" + (date ?: "")

    init {
        fullRefresh()
    }

    override fun fullRefresh() {
        appScope.launch {
            _workOrders.value = Resource.Loading
            _ncount.value = 0
            loadedOrders.clear()
            loadedKeys.clear()

            val result = repository.loadWorkOrders(_ncount.value, currentFilter.value)
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
            if (_isLoadingMore.value) return@launch
            _isLoadingMore.value = true

            val nextOffset = _ncount.value + PAGE_SIZE
            val result = repository.loadWorkOrders(nextOffset, currentFilter.value)
            if (result is Resource.Success) {
                _ncount.value = nextOffset
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

            _isLoadingMore.value = false
        }
    }
    override fun setFilter(filter: FilterByOption) {
        if (_currentFilter.value == filter) return
        _currentFilter.value = filter
        appSettings.setString(AppSettingsKeys.FILTER_STATE_WO, filter.wire)
        fullRefresh()
    }
    override fun sendMessage(orderNumber: String, orderDate: String, message: String) {
        if (orderNumber.isBlank() || orderDate.isBlank() || message.isBlank()) return
        println("orderDate ${orderDate}  == ${orderDate.substringBefore(' ')}")
        appScope.launch {
            val res = repository.sendMessageToOrder(orderNumber, orderDate.substringBefore(' '), message)
            if (res is Resource.Success) {
                // после успешной отправки перезагружаем, чтобы в messages появился новый коммент
                fullRefresh()
            }
        }
    }
}