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
import com.tagaev.trrcrm.domain.withOrderByMigratedFromDateLastModificationIfNeeded
import com.tagaev.trrcrm.domain.TreeRootResolvedDocument
import com.tagaev.trrcrm.ui.master_screen.IListMaster
import com.tagaev.trrcrm.ui.master_screen.MasterPanel
import com.tagaev.trrcrm.ui.master_screen.models.MessageModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
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
        private const val MAX_QR_LOOKUP_PAGES = 40
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
    private val _isQrScannerOpen = MutableStateFlow(false)
    val isQrScannerOpen: StateFlow<Boolean> = _isQrScannerOpen
    private val _isQrLookupInProgress = MutableStateFlow(false)
    val isQrLookupInProgress: StateFlow<Boolean> = _isQrLookupInProgress
    private val _qrLookupError = MutableStateFlow<String?>(null)
    val qrLookupError: StateFlow<String?> = _qrLookupError

    override fun selectItemFromList(guid: String?) {
        _selectedOrderGuid.value = guid
    }

    fun openQrScanner() {
        _qrLookupError.value = null
        _isQrScannerOpen.value = true
    }

    fun closeQrScanner() {
        _isQrScannerOpen.value = false
    }

    fun consumeQrLookupError() {
        _qrLookupError.value = null
    }

    fun onQrScanned(raw: String) {
        if (_isQrLookupInProgress.value) return

        appScope.launch {
            _isQrLookupInProgress.value = true
            _qrLookupError.value = null
            try {
                val code = extractTRSCode(raw) ?: raw
                val resolvedNumber = when (val res = repository.getTRSData(code)) {
                    is Resource.Success -> res.data.completionNumber.trim()
                    is Resource.Error -> {
                        _qrLookupError.value = res.causes ?: res.exception?.message ?: "Не удалось получить данные QR"
                        ""
                    }
                    is Resource.Loading -> ""
                }

                if (resolvedNumber.isBlank()) {
                    if (_qrLookupError.value.isNullOrBlank()) {
                        _qrLookupError.value = "В QR нет номера комплектации"
                    }
                    return@launch
                }

                val opened = openDetailsByNumber(resolvedNumber)
                if (opened) {
                    _isQrScannerOpen.value = false
                    _qrLookupError.value = null
                } else {
                    _qrLookupError.value = "Комплектация №$resolvedNumber не найдена"
                }
            } finally {
                _isQrLookupInProgress.value = false
            }
        }
    }

    // simple in-memory cache of loaded work orders for future DB caching
    private val loadedOrders = mutableListOf<WorkOrderDto>()

    private val loadedKeys = mutableSetOf<String>()
    private val loadMutex = Mutex()



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
            loadMutex.withLock {
                //_complectations.value = Resource.Loading
                _complectations.value = Resource.Success(data = loadedOrders, additionalLoading = true)

                _refineState.value = loadRefineState()
                _ncount.value = 0

                val result = repository.loadComplectations(0, refineState.value)
                if (result is Resource.Success) {
                    val newItems = result.data

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
    }

    override fun loadMore() {
        appScope.launch {
            loadMutex.withLock {
                _complectations.value = Resource.Success(data = loadedOrders, additionalLoading = true)
                val nextOffset = _ncount.value + PAGE_SIZE
                val result = repository.loadComplectations(nextOffset, refineState.value)
                if (result is Resource.Success) {
                    _ncount.value = nextOffset
                    val newItems = result.data
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
    }

    override fun changePanel(masterDetailPanel: MasterPanel) {
        _masterScreenPanel.value = masterDetailPanel
    }

    suspend fun openDetailsByNumber(number: String): Boolean {
        val target = normalizeDocNumber(number) ?: return false
        val runtimeRefine = refineState.value.copy(
            status = Refiner.Status.OFF,
            searchQueryType = Refiner.SearchQueryType.CODE,
            searchQuery = target
        )

        return loadMutex.withLock {
            var offset = 0

            repeat(MAX_QR_LOOKUP_PAGES) {
                val result = repository.loadComplectations(offset, runtimeRefine)
                if (result !is Resource.Success) {
                    return@withLock false
                }

                val page = result.data.orEmpty()
                if (page.isEmpty()) {
                    return@withLock false
                }

                val found = page.firstOrNull { workOrder ->
                    numbersMatch(workOrder.number, target)
                }
                if (found != null) {
                    loadedOrders.clear()
                    loadedKeys.clear()

                    val foundKey = found.key()
                    loadedKeys.add(foundKey)
                    loadedOrders.add(found)

                    _ncount.value = 0
                    _selectedOrderGuid.value = found.guid.toString()
                    _complectations.value = Resource.Success(loadedOrders.toList(), additionalLoading = false)
                    _masterScreenPanel.value = MasterPanel.Details

                    return@withLock true
                }

                if (page.size < PAGE_SIZE) {
                    return@withLock false
                }
                offset += PAGE_SIZE
            }

            false
        }
    }


    override fun setRefineState(newState: RefineState) {
        _refineState.value = newState.copy(searchQuery = newState.searchQuery.trimStart().trimEnd())
        saveRefineState(_refineState.value)
        _ncount.value = 0
        fullRefresh()
    }

    /**
     * Однократный поиск списка комплектаций по [Refiner.SearchQueryType.KIT_CHARACTERISTIC] (без изменения сохранённого refine и списка на экране).
     */
    suspend fun searchComplectationsByKitCharacteristicToken(token: String): Resource<List<WorkOrderDto>> {
        val trimmed = token.trim()
        if (trimmed.isEmpty()) return Resource.Error(causes = "Пустой запрос")
        val searchState = _refineState.value.copy(
            searchQuery = trimmed,
            searchQueryType = Refiner.SearchQueryType.KIT_CHARACTERISTIC
        )
        return repository.loadComplectations(0, searchState)
    }

    override suspend fun sendMessage(itemNumber: String, itemDate: String, message: String): String? {
        if (itemNumber.isBlank() || itemDate.isBlank() || message.isBlank()) return "Нет номера или даты документа"
        val res = repository.sendMessageToComplectation(
            itemNumber,
            itemDate.substringBefore(' '),
            message
        )
        return when (res) {
            is Resource.Success -> null
            is Resource.Error -> res.causes ?: res.exception?.message ?: "Ошибка отправки сообщения"
            else -> "Ошибка отправки сообщения"
        }
    }

    override suspend fun resolveBaseDocument(rawBaseDocument: String): Resource<TreeRootResolvedDocument> {
        return repository.resolveTreeRootDocument(rawBaseDocument)
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
            return RefineState.Default
        }

        val decoded = runCatching {
            json.decodeFromString<RefineState>(raw)
        }.getOrElse {
            // if schema changed or data corrupted – fail gracefully
            RefineState.Default
        }
        val migrated = decoded.withOrderByMigratedFromDateLastModificationIfNeeded(false)
        if (migrated != decoded) saveRefineState(migrated)
        return migrated
    }

    fun saveRefineState(state: RefineState) {
        val encoded = json.encodeToString(state)
        appSettings.setString(AppSettingsKeys.COMPLECTATION_REFINE_STATE, encoded)
    }

    private fun normalizeDocNumber(value: String?): String? {
        if (value.isNullOrBlank()) return null
        return value
            .replace(Regex("\\s+"), "")
            .trim()
            .takeIf { it.isNotBlank() }
    }

    private fun normalizeWithoutLeadingZeros(value: String): String {
        val noLeadingZeros = value.trimStart('0')
        return if (noLeadingZeros.isBlank()) "0" else noLeadingZeros
    }

    private fun extractQueryParam(input: String, name: String): String? {
        val query = input.substringAfter('?', missingDelimiterValue = input)
            .substringBefore('#')
        if (query.isEmpty()) return null
        val target = name.lowercase()
        for (part in query.split('&')) {
            if (part.isEmpty()) continue
            val eq = part.indexOf('=')
            val key = if (eq >= 0) part.substring(0, eq) else part
            if (key.lowercase() == target) {
                val value = if (eq >= 0) part.substring(eq + 1) else ""
                return value.takeIf { it.isNotBlank() }
            }
        }
        return null
    }

    private fun extractTRSCode(input: String): String? {
        extractQueryParam(input, "code")?.let { return it }
        val match = Regex("""\bTRS[0-9A-Za-z]+\b""").find(input)
        return match?.value
    }

    private fun numbersMatch(candidate: String?, target: String): Boolean {
        val normalizedCandidate = normalizeDocNumber(candidate) ?: return false
        if (normalizedCandidate == target) return true

        return normalizeWithoutLeadingZeros(normalizedCandidate) ==
                normalizeWithoutLeadingZeros(target)
    }
}
