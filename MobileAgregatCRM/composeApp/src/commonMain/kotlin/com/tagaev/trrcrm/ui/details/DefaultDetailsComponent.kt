package com.tagaev.trrcrm.ui.details

import com.arkivanov.decompose.ComponentContext
import com.tagaev.trrcrm.data.MainRepository
import com.tagaev.trrcrm.data.remote.ApiConfig
import com.tagaev.trrcrm.data.remote.EventsApi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

import com.tagaev.trrcrm.data.remote.Resource
import com.tagaev.trrcrm.data.remote.friendlyError
import com.tagaev.trrcrm.models.MessageDto
import com.tagaev.trrcrm.utils.TARGET_EVENT
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow

sealed interface SendMessageUiState {
    data object Idle : SendMessageUiState
    data object Sending : SendMessageUiState
    data class Error(val message: String) : SendMessageUiState
}

sealed interface MessageEvent {
    data object Sent : MessageEvent
}

interface DetailsComponent {
    val sendState: StateFlow<SendMessageUiState>
    val events: SharedFlow<MessageEvent>

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
    private val repo by lazy { MainRepository(api, apiConfig) }

    private val _sendState = MutableStateFlow<SendMessageUiState>(SendMessageUiState.Idle)
    override val sendState: StateFlow<SendMessageUiState> = _sendState

    private val _events = MutableSharedFlow<MessageEvent>(extraBufferCapacity = 1)
    override val events: SharedFlow<MessageEvent> = _events


    override fun sendMessage(number: String, date: String, message: String) {
        // Prevent double sending while already in progress
        if (_sendState.value is SendMessageUiState.Sending) return

        appScope.launch {
            _sendState.value = SendMessageUiState.Sending

            when (val res = repo.sendMessage(number = number, date = date, message = message)) {
                is Resource.Success -> {
                    // Optimistically append the new message to the currently opened event
                    val cur = TARGET_EVENT.value
                    val appended = try {
                        val newMsg = MessageDto(
                            author = "Вы",
                            workDate = date,
                            comment = message
                        )
                        cur.copy(messages = cur.messages + newMsg)
                    } catch (t: Throwable) {
                        cur
                    }
                    TARGET_EVENT.value = appended

                    _sendState.value = SendMessageUiState.Idle
                    _events.tryEmit(MessageEvent.Sent)
                }
                is Resource.Error -> {
                    val msg = res.causes ?: friendlyError(res.exception, "Ошибка отправки")
                    _sendState.value = SendMessageUiState.Error(msg)
                }
                Resource.Loading -> Unit
            }
        }
    }

    override fun addTask(taskMessage: String) {
        println("NOT YET IMPLEMENTED")
    }

    override fun back() = onBack()
}