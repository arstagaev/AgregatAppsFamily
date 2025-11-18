package com.tagaev.mobileagregatcrm.ui.work_order

import com.tagaev.mobileagregatcrm.data.remote.Resource
import com.tagaev.mobileagregatcrm.models.WorkOrderDto
import kotlinx.coroutines.flow.StateFlow
import com.arkivanov.decompose.ComponentContext
import com.tagaev.mobileagregatcrm.data.EventsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject


interface IWorkOrdersComponent {
    val workOrders: StateFlow<Resource<List<WorkOrderDto>>>

    fun fullRefresh()
}



class WorkOrdersComponent(
    componentContext: ComponentContext,
    private val onBack: () -> Unit,
) : IWorkOrdersComponent,
    ComponentContext by componentContext,
    KoinComponent {

    private val appScope: CoroutineScope by inject()
    private val repository: EventsRepository by inject()

    private val _workOrders =
        MutableStateFlow<Resource<List<WorkOrderDto>>>(Resource.Loading)
    override val workOrders: StateFlow<Resource<List<WorkOrderDto>>> = _workOrders

    init {
        fullRefresh()
    }

    override fun fullRefresh() {
        appScope.launch {
            _workOrders.value = Resource.Loading
            _workOrders.value = repository.loadWorkOrders()
        }
    }
}