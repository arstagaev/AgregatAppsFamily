package com.tagaev.trrcrm.ui.feed

import com.arkivanov.decompose.ComponentContext
import com.tagaev.trrcrm.data.AppSettings
import com.tagaev.trrcrm.data.AppSettingsKeys
import com.tagaev.trrcrm.data.MainRepository
import com.tagaev.trrcrm.data.remote.CoreApiErrorKind
import com.tagaev.trrcrm.data.remote.Resource
import com.tagaev.trrcrm.data.remote.toCoreApiError
import com.tagaev.trrcrm.models.CoreNotificationFeedItem
import com.tagaev.trrcrm.push.NotificationContextParser
import com.tagaev.trrcrm.push.NotificationsUnreadState
import com.tagaev.trrcrm.push.PushRegistrationCoordinator
import com.tagaev.trrcrm.push.UnreadCountSync
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

private const val PAGE_SIZE = 25

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
    val isLoadingFirstPage: StateFlow<Boolean>
    val isRefreshing: StateFlow<Boolean>
    val isLoadingNextPage: StateFlow<Boolean>
    val firstPageError: StateFlow<String?>
    val nextPageError: StateFlow<String?>
    val hasNext: StateFlow<Boolean>
    val currentPage: StateFlow<Int>
    val totalCount: StateFlow<Int>
    val searchQuery: StateFlow<String>
    val statusFilter: StateFlow<FeedStatusFilter>
    val unreadCount: StateFlow<Int>
    val transientMessage: StateFlow<String?>

    fun refresh()
    fun loadMore()
    fun retryFirstPage()
    fun retryNextPage()
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
    private var requestGeneration: Long = 0L
    private var sessionAwaitRefreshInFlight = false

    private val _items = MutableStateFlow<List<CoreNotificationFeedItem>>(emptyList())
    override val items: StateFlow<List<CoreNotificationFeedItem>> = _items

    private val _isLoadingFirstPage = MutableStateFlow(false)
    override val isLoadingFirstPage: StateFlow<Boolean> = _isLoadingFirstPage

    private val _isRefreshing = MutableStateFlow(false)
    override val isRefreshing: StateFlow<Boolean> = _isRefreshing

    private val _isLoadingNextPage = MutableStateFlow(false)
    override val isLoadingNextPage: StateFlow<Boolean> = _isLoadingNextPage

    private val _firstPageError = MutableStateFlow<String?>(null)
    override val firstPageError: StateFlow<String?> = _firstPageError

    private val _nextPageError = MutableStateFlow<String?>(null)
    override val nextPageError: StateFlow<String?> = _nextPageError

    private val _hasNext = MutableStateFlow(false)
    override val hasNext: StateFlow<Boolean> = _hasNext

    private val _currentPage = MutableStateFlow(1)
    override val currentPage: StateFlow<Int> = _currentPage
    private val _nextCursor = MutableStateFlow<String?>(null)

    private val _totalCount = MutableStateFlow(0)
    override val totalCount: StateFlow<Int> = _totalCount

    private val _searchQuery = MutableStateFlow(appSettings.getString(AppSettingsKeys.NOTIFICATIONS_SEARCH_QUERY, ""))
    override val searchQuery: StateFlow<String> = _searchQuery

    private val _statusFilter = MutableStateFlow(
        FeedStatusFilter.fromWire(
            appSettings.getString(AppSettingsKeys.NOTIFICATIONS_STATUS_FILTER, FeedStatusFilter.ALL.wire)
        )
    )
    override val statusFilter: StateFlow<FeedStatusFilter> = _statusFilter

    private val _unreadCount = MutableStateFlow(
        appSettings.getInt(AppSettingsKeys.NOTIFICATIONS_UNREAD_COUNT, 0).coerceAtLeast(0)
    )
    override val unreadCount: StateFlow<Int> = _unreadCount

    private val _transientMessage = MutableStateFlow<String?>(null)
    override val transientMessage: StateFlow<String?> = _transientMessage

    init {
        NotificationsUnreadState.setCount(_unreadCount.value)
        refresh()
    }

    override fun refresh() {
        appScope.launch {
            requestGeneration += 1
            loadPage(targetPage = 1, replace = true, expectedGeneration = requestGeneration)
        }
    }

    override fun loadMore() {
        appScope.launch {
            if (!_hasNext.value) return@launch
            if (_isLoadingNextPage.value || _isRefreshing.value || _isLoadingFirstPage.value) return@launch
            loadPage(
                targetPage = _currentPage.value + 1,
                replace = false,
                expectedGeneration = requestGeneration
            )
        }
    }

    override fun retryFirstPage() {
        refresh()
    }

    override fun retryNextPage() {
        loadMore()
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
            val sessionId = requireSessionId(reason = "read_all") ?: return@launch
            when (val res = repository.coreNotificationsReadAll(sessionId)) {
                is Resource.Success -> {
                    println("PUSH_SERVICE: feed_read_all_success updated=${res.data.updated}")
                    _items.value = _items.value.map { it.copy(status = "read", readAt = it.readAt ?: it.createdAt) }
                    updateUnreadCount(0)
                    UnreadCountSync.refreshAsync(reason = "feed_read_all", force = true)
                    _transientMessage.value = "Все уведомления отмечены как прочитанные"
                }
                is Resource.Error -> {
                    val mapped = res.exception.toCoreApiError(res.causes ?: "Не удалось отметить уведомления")
                    println("PUSH_SERVICE: feed_read_all_failed kind=${mapped.kind} status=${mapped.statusCode} reason=${mapped.message}")
                    if (mapped.kind == CoreApiErrorKind.NotFound && recoverSessionForFeed("read_all")) {
                        val retrySession = requireSessionId(reason = "read_all_retry") ?: return@launch
                        when (val retry = repository.coreNotificationsReadAll(retrySession)) {
                            is Resource.Success -> {
                                println("PUSH_SERVICE: feed_read_all_retry_success updated=${retry.data.updated}")
                                _items.value = _items.value.map { it.copy(status = "read", readAt = it.readAt ?: it.createdAt) }
                                updateUnreadCount(0)
                                UnreadCountSync.refreshAsync(reason = "feed_read_all_retry", force = true)
                                _transientMessage.value = "Все уведомления отмечены как прочитанные"
                            }
                            is Resource.Error -> {
                                val retryMapped = retry.exception.toCoreApiError(retry.causes ?: "Не удалось отметить уведомления")
                                _transientMessage.value = actionableFeedError(retryMapped)
                            }
                            is Resource.Loading -> Unit
                        }
                        return@launch
                    }
                    _transientMessage.value = actionableFeedError(mapped)
                }
                is Resource.Loading -> Unit
            }
        }
    }

    override fun consumeTransientMessage() {
        _transientMessage.value = null
    }

    private suspend fun loadPage(targetPage: Int, replace: Boolean, expectedGeneration: Long) {
        requestMutex.withLock {
            if (replace) {
                _isRefreshing.value = true
                if (_items.value.isEmpty()) {
                    _isLoadingFirstPage.value = true
                }
                _firstPageError.value = null
                _nextPageError.value = null
            } else {
                _isLoadingNextPage.value = true
                _nextPageError.value = null
            }

            try {
                val sessionId = requireSessionId(reason = "load_page_$targetPage") ?: return
                val cursor = if (replace) null else _nextCursor.value
                val response = fetchFeedPage(sessionId, targetPage, cursor)

                when (response) {
                    is Resource.Success -> {
                        if (expectedGeneration != requestGeneration) return
                        val payload = response.data
                        println("PUSH_SERVICE: feed_fetch_success page=${payload.page ?: targetPage} items=${payload.items.size} unread=${payload.unreadCount} next_cursor=${payload.nextCursor}")
                        updateUnreadCount(payload.unreadCount)
                        _nextCursor.value = payload.nextCursor
                        _hasNext.value = payload.hasNext
                            ?: (payload.nextCursor != null || (payload.items.isNotEmpty() && payload.items.size >= PAGE_SIZE))
                        _totalCount.value = payload.totalCount ?: _totalCount.value
                        _currentPage.value = payload.page ?: targetPage
                        _items.value = if (replace) {
                            payload.items
                        } else {
                            mergeUniqueByIdSorted(_items.value, payload.items)
                        }
                    }
                    is Resource.Error -> {
                        if (expectedGeneration != requestGeneration) return
                        val mapped = response.exception.toCoreApiError(response.causes ?: "Ошибка загрузки уведомлений")
                        println("PUSH_SERVICE: feed_fetch_failed page=$targetPage kind=${mapped.kind} status=${mapped.statusCode} reason=${mapped.message}")
                        if (mapped.kind == CoreApiErrorKind.NotFound && recoverSessionForFeed("feed_page_$targetPage")) {
                            val retrySession = requireSessionId(reason = "feed_page_retry_$targetPage") ?: return
                            when (val retry = fetchFeedPage(retrySession, targetPage, cursor)) {
                                is Resource.Success -> {
                                    if (expectedGeneration != requestGeneration) return
                                    val payload = retry.data
                                    println("PUSH_SERVICE: feed_fetch_retry_success page=${payload.page ?: targetPage} items=${payload.items.size} unread=${payload.unreadCount} next_cursor=${payload.nextCursor}")
                                    updateUnreadCount(payload.unreadCount)
                                    _nextCursor.value = payload.nextCursor
                                    _hasNext.value = payload.hasNext
                                        ?: (payload.nextCursor != null || (payload.items.isNotEmpty() && payload.items.size >= PAGE_SIZE))
                                    _totalCount.value = payload.totalCount ?: _totalCount.value
                                    _currentPage.value = payload.page ?: targetPage
                                    _items.value = if (replace) payload.items else mergeUniqueByIdSorted(_items.value, payload.items)
                                    return
                                }
                                is Resource.Error -> {
                                    if (expectedGeneration != requestGeneration) return
                                    val retryMapped = retry.exception.toCoreApiError(retry.causes ?: "Ошибка загрузки уведомлений")
                                    if (replace) {
                                        _firstPageError.value = actionableFeedError(retryMapped)
                                    } else {
                                        _nextPageError.value = actionableFeedError(retryMapped)
                                    }
                                    return
                                }
                                is Resource.Loading -> Unit
                            }
                        }
                        val msg = actionableFeedError(mapped)
                        if (replace) {
                            _firstPageError.value = msg
                        } else {
                            _nextPageError.value = msg
                        }
                    }
                    is Resource.Loading -> Unit
                }
            } finally {
                if (replace) {
                    _isLoadingFirstPage.value = false
                    _isRefreshing.value = false
                } else {
                    _isLoadingNextPage.value = false
                }
            }
        }
    }

    private suspend fun updateItemStatus(notificationId: String, status: String, source: String) {
        val sessionId = requireSessionId(reason = "status_update") ?: return
        val numericNotificationId = notificationId.toLongOrNull()
        if (numericNotificationId == null) {
            _transientMessage.value = "Некорректный ID уведомления: $notificationId"
            return
        }
        when (val result = repository.coreNotificationStatusUpdate(sessionId, numericNotificationId, status, source)) {
            is Resource.Success -> {
                println("PUSH_SERVICE: feed_status_update_success notification_id=$numericNotificationId status=$status source=$source")
                val readAt = result.data.readAt
                _items.value = _items.value.map { item ->
                    if (item.id != notificationId) item
                    else item.copy(status = status, readAt = readAt)
                }
                updateUnreadCount(_items.value.count { !it.status.equals("read", ignoreCase = true) })
                UnreadCountSync.refreshAsync(reason = "feed_status_update", force = false)
            }
            is Resource.Error -> {
                val mapped = result.exception.toCoreApiError(result.causes ?: "Не удалось обновить статус уведомления")
                println("PUSH_SERVICE: feed_status_update_failed notification_id=$numericNotificationId kind=${mapped.kind} status=${mapped.statusCode} reason=${mapped.message}")
                if (mapped.kind == CoreApiErrorKind.NotFound && recoverSessionForFeed("status_update")) {
                    val retrySession = requireSessionId(reason = "status_update_retry") ?: return
                    when (val retry = repository.coreNotificationStatusUpdate(retrySession, numericNotificationId, status, source)) {
                        is Resource.Success -> {
                            println("PUSH_SERVICE: feed_status_update_retry_success notification_id=$numericNotificationId status=$status source=$source")
                            val readAt = retry.data.readAt
                            _items.value = _items.value.map { item ->
                                if (item.id != notificationId) item
                                else item.copy(status = status, readAt = readAt)
                            }
                            updateUnreadCount(_items.value.count { !it.status.equals("read", ignoreCase = true) })
                            UnreadCountSync.refreshAsync(reason = "feed_status_update_retry", force = false)
                            return
                        }
                        is Resource.Error -> {
                            val retryMapped = retry.exception.toCoreApiError(retry.causes ?: "Не удалось обновить статус уведомления")
                            _transientMessage.value = actionableFeedError(retryMapped)
                            return
                        }
                        is Resource.Loading -> Unit
                    }
                }
                _transientMessage.value = actionableFeedError(mapped)
            }
            is Resource.Loading -> Unit
        }
    }

    private fun mergeUniqueByIdSorted(
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

    private fun updateUnreadCount(raw: Int) {
        val normalized = raw.coerceAtLeast(0)
        _unreadCount.value = normalized
        appSettings.setInt(AppSettingsKeys.NOTIFICATIONS_UNREAD_COUNT, normalized)
        NotificationsUnreadState.setCount(normalized)
    }

    private suspend fun requireSessionId(reason: String): String? {
        val sessionId = appSettings.getString(AppSettingsKeys.CORE_SESSION_ID, "").trim()
        if (sessionId.isNotBlank()) {
            if (_firstPageError.value?.contains("Сессия") == true) {
                _firstPageError.value = null
            }
            return sessionId
        }

        val bootstrapPending = appSettings.getBool(AppSettingsKeys.CORE_BOOTSTRAP_RETRY_ON_TOKEN, false)
        if (bootstrapPending) {
            println("PUSH_SERVICE: feed_session_pending reason=$reason waiting_for_bootstrap")
            _firstPageError.value = "Инициализируем сессию уведомлений..."
            scheduleAwaitSessionAndRefresh(reason)
            return null
        }

        val recovered = recoverSessionForFeed(reason = "missing_$reason", forceRebootstrap = false)
        if (recovered) {
            val recoveredSessionId = appSettings.getString(AppSettingsKeys.CORE_SESSION_ID, "").trim()
            if (recoveredSessionId.isNotBlank()) {
                _firstPageError.value = null
                return recoveredSessionId
            }
        }

        _firstPageError.value = "Сессия не инициализирована. Перезайдите в приложение."
        return null
    }

    private suspend fun fetchFeedPage(
        sessionId: String,
        targetPage: Int,
        cursor: String?,
    ): Resource<com.tagaev.trrcrm.models.CoreNotificationsFeedResponse> = repository.coreNotificationsFeed(
        sessionId = sessionId,
        limit = PAGE_SIZE,
        page = targetPage,
        cursor = cursor,
        searchQuery = _searchQuery.value.ifBlank { null },
        statusFilter = _statusFilter.value.wire
    )

    private suspend fun recoverSessionForFeed(reason: String, forceRebootstrap: Boolean = true): Boolean {
        println("PUSH_SERVICE: feed_session_recover_attempt reason=$reason")
        val recovered = PushRegistrationCoordinator.recoverCoreSessionNow(
            reason = "feed_$reason",
            forceRebootstrap = forceRebootstrap
        )
        if (!recovered) {
            _transientMessage.value = "Сессия истекла. Перезайдите в приложение."
        }
        return recovered
    }

    private fun scheduleAwaitSessionAndRefresh(reason: String) {
        if (sessionAwaitRefreshInFlight) return
        sessionAwaitRefreshInFlight = true
        appScope.launch {
            try {
                repeat(6) { attempt ->
                    delay(1_000L)
                    val sessionId = appSettings.getString(AppSettingsKeys.CORE_SESSION_ID, "").trim()
                    if (sessionId.isNotBlank()) {
                        println("PUSH_SERVICE: feed_session_ready_after_wait reason=$reason attempt=${attempt + 1}")
                        _firstPageError.value = null
                        refresh()
                        return@launch
                    }
                }
                println("PUSH_SERVICE: feed_session_wait_timeout reason=$reason")
            } finally {
                sessionAwaitRefreshInFlight = false
            }
        }
    }

    private fun actionableFeedError(error: com.tagaev.trrcrm.data.remote.CoreApiError): String = when (error.kind) {
        CoreApiErrorKind.Unauthorized, CoreApiErrorKind.Forbidden ->
            "Ошибка авторизации. Перезайдите в приложение."
        CoreApiErrorKind.NotFound ->
            "Сессия не найдена или истекла. Перезайдите в приложение."
        CoreApiErrorKind.Validation ->
            "Сервер отклонил запрос. Проверьте параметры и повторите."
        else -> error.message.ifBlank { "Ошибка загрузки уведомлений" }
    }
}
