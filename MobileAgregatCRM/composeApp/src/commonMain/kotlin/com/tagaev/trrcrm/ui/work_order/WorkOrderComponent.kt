package com.tagaev.trrcrm.ui.work_order

import com.tagaev.trrcrm.data.remote.Resource
import com.tagaev.trrcrm.data.remote.friendlyError
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
import com.tagaev.trrcrm.domain.TreeRootResolvedDocument
import com.tagaev.trrcrm.ui.master_screen.IListMaster
import com.tagaev.trrcrm.ui.master_screen.MasterPanel
import com.tagaev.trrcrm.ui.master_screen.models.MessageModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

interface IWorkOrdersComponent: IListMaster {
    val workOrders: StateFlow<Resource<List<WorkOrderDto>>>
    var pickedOrder: WorkOrderDto?
    suspend fun searchComplectationsByKitCharacteristicToken(token: String): Resource<List<WorkOrderDto>>
}

class WorkOrdersComponent(
    componentContext: ComponentContext,
    private val onBack: () -> Unit,
) : IWorkOrdersComponent,
    ComponentContext by componentContext,
    KoinComponent {

    companion object {
        private const val PAGE_SIZE = 30
    }

    private val appScope: CoroutineScope by inject()
    private val repository: MainRepository by inject()
    private val appSettings: AppSettings by inject()

    private val _workOrders =
        MutableStateFlow<Resource<List<WorkOrderDto>>>(Resource.Loading)
    override val workOrders: StateFlow<Resource<List<WorkOrderDto>>> = _workOrders

    private val _refineState = MutableStateFlow(RefineState.EventsLikeDefault)
    override val refineState: StateFlow<RefineState> = _refineState

    val _masterScreenPanel = MutableStateFlow(MasterPanel.List)
    override val masterScreenPanel: StateFlow<MasterPanel> = _masterScreenPanel

    private val _ncount = MutableStateFlow(0)
    override val ncount: StateFlow<Int> = _ncount

    private val _selectedOrderGuid = MutableStateFlow<String?>(null)
    override val selectedItemGuid: StateFlow<String?> = _selectedOrderGuid
    override var pickedOrder: WorkOrderDto? = null

    override fun selectItemFromList(guid: String?) {
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

//    fun insertNewMessage() {
//        _workOrders
//    }

    override fun fullRefresh() {
        appScope.launch {
            //_workOrders.value = Resource.Loading
            _workOrders.value = Resource.Success(data = loadedOrders, additionalLoading = true)


            _refineState.value = loadRefineState()
            _ncount.value = 0

            val result = repository.loadWorkOrders(0, refineState.value)
            if (result is Resource.Success) {

                val newItems = result.data ?: emptyList()

                loadedOrders.clear()
                loadedKeys.clear()

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


    override fun setRefineState(newState: RefineState) {
        _refineState.value = newState.copy(searchQuery = newState.searchQuery.trimStart().trimEnd())
        saveRefineState(_refineState.value)
        _ncount.value = 0
        fullRefresh()
    }

    override suspend fun sendMessage(itemNumber: String, itemDate: String, message: String): String? {
        if (itemNumber.isBlank() || itemDate.isBlank() || message.isBlank()) return "Нет номера или даты документа"
        val res = repository.sendMessageToWorkOrder(
            itemNumber,
            itemDate.substringBefore(' '),
            message
        )
        val wo = pickedOrder
        val users = buildWorkOrderRecipients(wo)
        val author = appSettings.getStringOrNull(AppSettingsKeys.PERSONAL_DATA)
        if (!users.isNullOrEmpty() && !author.isNullOrBlank()) {
            repository.sendMessageEventPUSH(
                docId = wo?.guid ?: wo?.number ?: itemNumber,
                docTitle = "Заказ-Наряд ${wo?.number ?: itemNumber} (${wo?.branch.orEmpty()})",
                authorName = author,
                recipientNames = users,
                message = "${author}:\n${message}",
                screen = "work_orders",
                rawMessage = message
            )
        }
        return when (res) {
            is Resource.Success -> null
            is Resource.Error -> res.causes ?: friendlyError(res.exception, "Ошибка отправки сообщения")
            else -> "Ошибка отправки сообщения"
        }
    }

    override suspend fun resolveBaseDocument(rawBaseDocument: String): Resource<TreeRootResolvedDocument> {
        return repository.resolveTreeRootDocument(rawBaseDocument)
    }

    override suspend fun searchComplectationsByKitCharacteristicToken(token: String): Resource<List<WorkOrderDto>> {
        val trimmed = token.trim()
        if (trimmed.isEmpty()) return Resource.Error(causes = "Пустой запрос")
        val searchState = _refineState.value.copy(
            searchQuery = trimmed,
            searchQueryType = Refiner.SearchQueryType.KIT_CHARACTERISTIC
        )
        return repository.loadComplectations(0, searchState)
    }

    override fun findAndSelectByNotification(identifier: String, messageHint: String?): Boolean {
        val normalizedIdentifier = normalizeIdentifier(identifier) ?: return false
        val matches = loadedOrders.filter { order -> order.matchesNormalizedIdentifier(normalizedIdentifier) }
        val picked = pickWorkOrderMatch(matches, messageHint) ?: return false
        selectItemFromList(picked.guid ?: picked.number)
        return true
    }

    override suspend fun resolveNotificationTarget(identifier: String, messageHint: String?): String? {
        val normalizedIdentifier = normalizeIdentifier(identifier) ?: return null
        val localPicked = pickWorkOrderMatch(
            loadedOrders.filter { it.matchesNormalizedIdentifier(normalizedIdentifier) },
            messageHint
        )
        if (localPicked != null) return localPicked.guid

        val searchState = _refineState.value.copy(
            searchQuery = identifier.trim(),
            searchQueryType = Refiner.SearchQueryType.CODE
        )
        val remoteMatches = (repository.loadWorkOrders(0, searchState) as? Resource.Success)?.data.orEmpty()
            .filter { it.matchesNormalizedIdentifier(normalizedIdentifier) }
        return pickWorkOrderMatch(remoteMatches, messageHint)?.guid
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
     * After updating, this method emits a new Resource.Success to [_workOrders]
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
        val current = _workOrders.value
        val additionalLoading = (current as? Resource.Success)?.additionalLoading ?: false

        _workOrders.value = Resource.Success(
            data = loadedOrders.toList(),
            additionalLoading = additionalLoading
        )
    }
    // ---------- Work Orders refine state ----------

    fun loadRefineState(): RefineState {
        val primaryRaw = appSettings.getStringOrNull(AppSettingsKeys.WORK_ORDERS_REFINE_STATE)
        val legacyRaw = appSettings.getStringOrNull(AppSettingsKeys.CARGO_REFINE_STATE)
        val usingLegacyValue = primaryRaw.isNullOrBlank() && !legacyRaw.isNullOrBlank()
        val raw = if (usingLegacyValue) legacyRaw else primaryRaw

        if (raw.isNullOrBlank()) {
            // default state when nothing stored
            return RefineState.EventsLikeDefault
        }

        val decoded = runCatching {
            json.decodeFromString<RefineState>(raw)
        }.getOrElse {
            // if schema changed or data corrupted – fail gracefully
            RefineState.EventsLikeDefault
        }

        // One-time migration path from old key to isolated work-order key.
        if (usingLegacyValue) {
            saveRefineState(decoded)
        }

        return decoded
    }

    fun saveRefineState(state: RefineState) {
        val encoded = json.encodeToString(state)
        appSettings.setString(AppSettingsKeys.WORK_ORDERS_REFINE_STATE, encoded)
    }

    private fun buildWorkOrderRecipients(order: WorkOrderDto?): List<String> {
        if (order == null) return emptyList()
        val rawCandidates = buildList {
            add(order.author)
            add(order.manager)
            add(order.dispatcher)
            add(order.master)
            order.messages.forEach { add(it.author) }
        }
        return uniqueNormalizedNames(rawCandidates)
    }

    private fun uniqueNormalizedNames(candidates: List<String?>): List<String> {
        val result = LinkedHashSet<String>()
        candidates.forEach { value ->
            val normalized = value?.trim()?.replace(Regex("\\s+"), " ").orEmpty()
            if (normalized.isNotBlank()) result.add(normalized)
        }
        return result.toList()
    }

    private fun normalizeIdentifier(value: String?): String? {
        if (value.isNullOrBlank()) return null
        return value.replace(Regex("\\s+"), "").trim().trimStart('0').ifBlank { "0" }
    }

    private fun pickWorkOrderMatch(candidates: List<WorkOrderDto>, messageHint: String?): WorkOrderDto? {
        if (candidates.isEmpty()) return null
        if (candidates.size == 1) return candidates.first()
        val hint = messageHint?.trim().orEmpty()
        if (hint.isBlank()) return candidates.first()
        return candidates.firstOrNull { order ->
            order.messages.orEmpty().any { msg ->
                msg.comment?.contains(hint, ignoreCase = true) == true
            }
        } ?: candidates.first()
    }

    private fun WorkOrderDto.matchesNormalizedIdentifier(normalizedIdentifier: String): Boolean {
        val guidNormalized = normalizeIdentifier(guid)
        val numberNormalized = normalizeIdentifier(number)
        return guidNormalized == normalizedIdentifier || numberNormalized == normalizedIdentifier
    }
}
