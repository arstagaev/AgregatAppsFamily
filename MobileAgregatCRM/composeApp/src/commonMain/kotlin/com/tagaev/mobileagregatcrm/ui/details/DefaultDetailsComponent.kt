package com.tagaev.mobileagregatcrm.ui.details

import com.arkivanov.decompose.ComponentContext
import com.tagaev.mobileagregatcrm.data.EventsRepository
import com.tagaev.mobileagregatcrm.data.remote.ApiConfig
import com.tagaev.mobileagregatcrm.data.remote.EventsApi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.agregatcrm.models.EventItemDto
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

interface DetailsComponent {
//    val number: String
//    val snapshot: EventItemDto?
    fun sendMessage(number: String, date: String, message: String)
    fun addTask(taskMessage: String)
    fun back()
}

class DefaultDetailsComponent(
    componentContext: ComponentContext,
//    override val number: String,
//    override val snapshot: EventItemDto?,
    private val onBack: () -> Unit
) : DetailsComponent, ComponentContext by componentContext, KoinComponent {

    private val appScope: CoroutineScope by inject()
    private val api: EventsApi by inject()
    private val apiConfig: ApiConfig by inject()
    private val repo by lazy { EventsRepository(api, apiConfig) }


    override fun sendMessage(number: String, date: String, message: String) {
        appScope.launch { repo.sendMessage(number = number, date = date, message = message) }
    }

    override fun addTask(taskMessage: String) {
        println("NOT YET IMPLEMENTED")
    }

    override fun back() = onBack()
}