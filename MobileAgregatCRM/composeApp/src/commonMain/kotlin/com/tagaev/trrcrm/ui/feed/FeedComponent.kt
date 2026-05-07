package com.tagaev.trrcrm.ui.feed

import com.arkivanov.decompose.ComponentContext
import com.tagaev.trrcrm.data.AppSettings
import com.tagaev.trrcrm.data.AppSettingsKeys
import com.tagaev.trrcrm.data.MainRepository
import com.tagaev.trrcrm.data.remote.Resource
import com.tagaev.trrcrm.models.CoreNotificationFeedItem
import com.tagaev.trrcrm.push.NotificationContextParser
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

enum class FeedStatusFilter(val wire: String, val label: String) {
    ALL("all", "Все"),
    UNREAD("unread", "Непрочитанные"),
    READ("read", "Прочитанные");

    companion object {
        fun fromWire(raw: String?): FeedStatusFilter {
            return entries.firstOrNull { it.wire == raw } ?: ALL
        }
    }
}

interface IFeedComponent {
    val items: StateFlow<List<CoreNotificationFeedItem>>
    val isLoading: StateFlow<Boolean>
    val isRefreshing: StateFlow<Boolean>
    val isLoadingMore: StateFlow<Boolean>
    val error: StateFlow<String?>
    val nextCursor: StateFlow<String?>
    val searchQuery: StateFlow<String>
    val statusFilter: StateFlow<FeedStatusFilter>
    val unreadCount: StateFlow<Int>
    val transientMessage: StateFlow<String?>

    fun refresh()
    fun loadMore()
    fun setSearchQuery(query: String)
    fun setStatusFilter(filter: FeedStatusFilter)
    fun openNotification(item: CoreNotificationFeedItem)
    fun toggleRead(item: CoreNotificationFeedItem)
    fun markAllRead()
    fun consumeTransientMessage()
}

class FeedComponent(
    componentContext: ComponentContext,
    private val onOpenNotification: (screen: String, identifier: String?, messageText: String?, title: String?) -> Unit,
) : IFeedComponent, ComponentContext by componentContext, KoinComponent {

    private val appScope: CoroutineScope by inject()
    private val repository: MainRepository by inject()
    private val appSettings: AppSettings by inject()
    private val requestMutex = Mutex()

    private val _items = MutableStateFlow<List<CoreNotificationFeedItem>>(emptyList())
    override val items: StateFlow<List<CoreNotificationFeedItem>> = _items

    private val _isLoading = MutableStateFlow(false)
    override val isLoading: StateFlow<Boolean> = _isLoading

    private val _isRefreshing = MutableStateFlow(false)
    override val isRefreshing: StateFlow<Boolean> = _isRefreshing

    private val _isLoadingMore = MutableStateFlow(false)
    override val isLoadingMore: StateFlow<Boolean> = _isLoadingMore

    private val _error = MutableStateFlow<String?>(null)
    override val error: StateFlow<String?> = _error

    private val _nextCursor = MutableStateFlow<String?>(null)
    override val nextCursor: StateFlow<String?> = _nextCursor

    private val _searchQuery = MutableStateFlow(appSettings.getString(AppSettingsKeys.NOTIFICATIONS_SEARCH_QUERY, ""))
    override val searchQuery: StateFlow<String> = _searchQuery

    private val _statusFilter = MutableStateFlow(
        FeedStatusFilter.fromWire(
            appSettings.getString(AppSettingsKeys.NOTIFICATIONS_STATUS_FILTER, FeedStatusFilter.ALL.wire)
        )
    )
    override val statusFilter: StateFlow<FeedStatusFilter> = _statusFilter

    private val _unreadCount = MutableStateFlow(0)
    override val unreadCount: StateFlow<Int> = _unreadCount

    private val _transientMessage = MutableStateFlow<String?>(null)
    override val transientMessage: StateFlow<String?> = _transientMessage

    init {
        refresh()
    }

    override fun refresh() {
        appScope.launch {
            loadPage(reset = true)
        }
    }

    override fun loadMore() {
        appScope.launch {
            if (_nextCursor.value.isNullOrBlank()) return@launch
            if (_isLoading.value || _isRefreshing.value || _isLoadingMore.value) return@launch
            loadPage(reset = false)
        }
    }

    override fun setSearchQuery(query: String) {
        val normalized = query.trim()
        if (_searchQuery.value == normalized) return
        _searchQuery.value = normalized
        appSettings.setString(AppSettingsKeys.NOTIFICATIONS_SEARCH_QUERY, normalized)
        refresh()
    }

    override fun setStatusFilter(filter: FeedStatusFilter) {
        if (_statusFilter.value == filter) return
        _statusFilter.value = filter
        appSettings.setString(AppSettingsKeys.NOTIFICATIONS_STATUS_FILTER, filter.wire)
        refresh()
    }

    override fun openNotification(item: CoreNotificationFeedItem) {
        appScope.launch {
            if (!item.status.equals("read", ignoreCase = true)) {
                updateItemStatus(item.id, "read", "open")
            }

            val parsed = NotificationContextParser.parse(
                title = item.docTitle ?: item.title,
                screen = item.screen,
                docId = item.searchKey ?: item.docNumber,
                messageText = item.messageText
            )
            val resolvedScreen = parsed.screen
                ?: item.screen?.trim()?.lowercase()?.replace('-', '_')?.replace(' ', '_')
                ?: "events"
            val resolvedIdentifier = parsed.primaryKey

            onOpenNotification(
                resolvedScreen,
                resolvedIdentifier,
                parsed.messageHint ?: item.messageText,
                item.docTitle ?: item.title
            )
        }
    }

    override fun toggleRead(item: CoreNotificationFeedItem) {
        appScope.launch {
            val current = item.status.lowercase()
            val nextStatus = if (current == "read") "unread" else "read"
            updateItemStatus(item.id, nextStatus, "manual")
        }
    }

    override fun markAllRead() {
        appScope.launch {
            val sessionId = requireSessionId() ?: return@launch
            when (val res = repository.coreNotificationsReadAll(sessionId)) {
                is Resource.Success -> {
                    _items.value = _items.value.map { it.copy(status = "read", readAt = it.readAt ?: it.createdAt) }
                    _unreadCount.value = 0
                    _transientMessage.value = "Все уведомления отмечены как прочитанные"
                }
                is Resource.Error -> {
                    _transientMessage.value = res.causes ?: "Не удалось отметить уведомления"
                }
                is Resource.Loading -> Unit
            }
        }
    }

    override fun consumeTransientMessage() {
        _transientMessage.value = null
    }

    private suspend fun loadPage(reset: Boolean) {
        requestMutex.withLock {
            if (reset) {
                _isRefreshing.value = true
                _error.value = null
                if (_items.value.isEmpty()) {
                    _isLoading.value = true
                }
            } else {
                _isLoadingMore.value = true
            }

            try {
                val sessionId = requireSessionId() ?: return
                val cursor = if (reset) null else _nextCursor.value
                val response = repository.coreNotificationsFeed(
                    sessionId = sessionId,
                    limit = 30,
                    cursor = cursor,
                    searchQuery = _searchQuery.value.ifBlank { null },
                    statusFilter = _statusFilter.value.wire
                )

                when (response) {
                    is Resource.Success -> {
                        val payload = response.data
                        _unreadCount.value = payload.unreadCount
                        _nextCursor.value = payload.nextCursor
                        _items.value = if (reset) {
                            payload.items
                        } else {
                            mergeUniqueById(_items.value, payload.items)
                        }
                    }
                    is Resource.Error -> {
                        _error.value = response.causes ?: "Ошибка загрузки уведомлений"
                    }
                    is Resource.Loading -> Unit
                }
            } finally {
                _isLoading.value = false
                _isRefreshing.value = false
                _isLoadingMore.value = false
            }
        }
    }

    private suspend fun updateItemStatus(notificationId: String, status: String, source: String) {
        val sessionId = requireSessionId() ?: return
        val numericNotificationId = notificationId.toLongOrNull()
        if (numericNotificationId == null) {
            _transientMessage.value = "Некорректный ID уведомления: $notificationId"
            return
        }
        when (val result = repository.coreNotificationStatusUpdate(sessionId, numericNotificationId, status, source)) {
            is Resource.Success -> {
                val readAt = result.data.readAt
                _items.value = _items.value.map { item ->
                    if (item.id != notificationId) item
                    else item.copy(status = status, readAt = readAt)
                }
                _unreadCount.value = _items.value.count { !it.status.equals("read", ignoreCase = true) }
            }
            is Resource.Error -> {
                _transientMessage.value = result.causes ?: "Не удалось обновить статус уведомления"
            }
            is Resource.Loading -> Unit
        }
    }

    private fun mergeUniqueById(
        existing: List<CoreNotificationFeedItem>,
        incoming: List<CoreNotificationFeedItem>,
    ): List<CoreNotificationFeedItem> {
        val knownIds = existing.mapTo(mutableSetOf()) { it.id }
        val merged = existing.toMutableList()
        for (item in incoming) {
            if (knownIds.add(item.id)) {
                merged.add(item)
            }
        }
        return merged
    }

    private fun requireSessionId(): String? {
        val sessionId = appSettings.getString(AppSettingsKeys.CORE_SESSION_ID, "").trim()
        if (sessionId.isBlank()) {
            _error.value = "Сессия не инициализирована. Перезайдите в приложение."
            return null
        }
        return sessionId
    }
}
