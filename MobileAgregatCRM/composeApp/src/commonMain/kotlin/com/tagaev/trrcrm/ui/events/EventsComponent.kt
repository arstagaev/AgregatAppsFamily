package com.tagaev.trrcrm.ui.events

import com.tagaev.trrcrm.ui.i18n.tr

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.essenty.backhandler.BackCallback
import com.tagaev.secrets.Secrets
import com.tagaev.trrcrm.data.AppSettings
import com.tagaev.trrcrm.data.AppSettingsKeys
import com.tagaev.trrcrm.data.MainRepository
import com.tagaev.trrcrm.data.remote.EventsApi.Companion.json
import com.tagaev.trrcrm.data.remote.Resource
import com.tagaev.trrcrm.data.remote.friendlyError
import com.tagaev.trrcrm.domain.RefineState
import com.tagaev.trrcrm.domain.Refiner
import com.tagaev.trrcrm.domain.TreeRootResolvedDocument
import com.tagaev.trrcrm.domain.messages.normalizedPushRecipients
import com.tagaev.trrcrm.models.EventItemDto
import com.tagaev.trrcrm.models.MessageDto
import com.tagaev.trrcrm.ui.master_screen.DeepLinkOpenResult
import com.tagaev.trrcrm.ui.master_screen.IListMaster
import com.tagaev.trrcrm.ui.master_screen.MasterPanel
import com.tagaev.trrcrm.ui.master_screen.models.MessageModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import kotlin.collections.orEmpty
import kotlin.collections.plus
import kotlin.getValue

interface IEventsComponent : IListMaster {
    val events: StateFlow<Resource<List<EventItemDto>>>
    var pickedEvent: EventItemDto?
}

class EventsComponent(
    componentContext: ComponentContext,
    private val onBack: () -> Unit
) : IEventsComponent, ComponentContext by componentContext, KoinComponent {


    companion object {
        private const val PAGE_SIZE = 30
    }

    private val appScope: CoroutineScope by inject()
    private val repository: MainRepository by inject()
    private val appSettings: AppSettings by inject()
    private val mobileFeatureFlags: com.tagaev.trrcrm.data.featureflags.MobileFeatureFlagsStore by inject()

    private val photoSession = com.tagaev.trrcrm.ui.photos.DocumentPhotoSession(
        documentType = com.tagaev.trrcrm.models.ImageDocumentType.Event,
        repository = repository,
        featureFlags = mobileFeatureFlags,
        appScope = appScope,
    )

    val isCameraOpen = photoSession.isCameraOpen
    val isCameraPrecheckInProgress = photoSession.isCameraPrecheckInProgress
    val cameraDocumentNumber = photoSession.cameraDocumentNumber
    val cameraPrecheckError = photoSession.cameraPrecheckError
    val cameraUploadQuota = photoSession.cameraUploadQuota
    val cameraUploadPeriod = photoSession.cameraUploadPeriod
    val documentPhotoCount = photoSession.documentPhotoCount
    val isDocumentPhotoCountLoading = photoSession.isDocumentPhotoCountLoading
    val documentPhotoCountLoaded = photoSession.documentPhotoCountLoaded
    val isPhotosViewerOpen = photoSession.isPhotosViewerOpen
    val photosViewerDocumentNumber = photoSession.photosViewerDocumentNumber
    val photosViewerUploadPeriod = photoSession.photosViewerUploadPeriod

    fun requestOpenCamera(rawNumber: String, uploadPeriod: com.tagaev.trrcrm.models.DocumentUploadPeriod?) =
        photoSession.requestOpenCamera(rawNumber, uploadPeriod)
    fun closeCamera() = photoSession.closeCamera()
    fun consumeCameraPrecheckError() = photoSession.consumeCameraPrecheckError()
    fun refreshDocumentPhotoCount(documentNumber: String, uploadPeriod: com.tagaev.trrcrm.models.DocumentUploadPeriod?) =
        photoSession.refreshDocumentPhotoCount(documentNumber, uploadPeriod)
    fun requestOpenDocumentPhotos(documentNumber: String, uploadPeriod: com.tagaev.trrcrm.models.DocumentUploadPeriod?) =
        photoSession.requestOpenDocumentPhotos(documentNumber, uploadPeriod)
    fun closePhotosViewer() = photoSession.closePhotosViewer()
    suspend fun isPhotosUploadEnabled() = photoSession.isUploadEnabled()
    suspend fun isPhotosDownloadEnabled() = photoSession.isDownloadEnabled()

    private val _events =
        MutableStateFlow<Resource<List<EventItemDto>>>(Resource.Loading)
    override val events: StateFlow<Resource<List<EventItemDto>>> = _events

    private val _refineState = MutableStateFlow(RefineState.EventsLikeDefault)
    override val refineState: StateFlow<RefineState> = _refineState

    val _masterScreenPanel = MutableStateFlow(MasterPanel.List)
    override val masterScreenPanel: StateFlow<MasterPanel> = _masterScreenPanel

    private val _ncount = MutableStateFlow(0)
    override val ncount: StateFlow<Int> = _ncount

    private val _selectedOrderGuid = MutableStateFlow<String?>(null)
    override val selectedItemGuid: StateFlow<String?> = _selectedOrderGuid

    override var pickedEvent: EventItemDto? = null

    override fun selectItemFromList(guid: String?) {
        _selectedOrderGuid.value = guid
    }

    // simple in-memory cache of loaded work orders for future DB caching
    private val loadedEvents = mutableListOf<EventItemDto>()

    private val loadedKeys = mutableSetOf<String>()
    private var deepLinkSnapshot: DeepLinkSnapshot? = null

    private data class DeepLinkSnapshot(
        val events: List<EventItemDto>,
        val keys: Set<String>,
        val refineState: RefineState,
        val ncount: Int,
        val selectedGuid: String?,
        val panel: MasterPanel,
        val pickedEvent: EventItemDto?,
    )

    val backCallback = BackCallback { _masterScreenPanel.value = MasterPanel.List }

    private fun EventItemDto.key(): String =
        guid ?: (number ?: "") + "|" + (date ?: "")

    init {
        fullRefresh()
        backHandler.register(backCallback)
    }

    override fun fullRefresh() {
        println(">>> fullRefresh")
        appScope.launch {
            val capturedGeneration = com.tagaev.trrcrm.data.accounts.AccountSessionCaches.listGeneration()
            loadedEvents.clear()
            loadedKeys.clear()
            _events.value = Resource.Loading

            _refineState.value = loadRefineState()
            _ncount.value = 0

            val result = repository.loadEvents(0, refineState.value)
            if (!com.tagaev.trrcrm.data.accounts.AccountSessionCaches.isCurrentListGeneration(capturedGeneration)) return@launch
            if (result is Resource.Success) {

                val newItems = result.data ?: emptyList()

                loadedEvents.clear()
                loadedKeys.clear()

                for (order in newItems) {
                    val key = order.key()
                    if (loadedKeys.add(key)) {
                        loadedEvents.add(order)
                    }
                }
                _events.value = Resource.Success(loadedEvents.toList(), additionalLoading = false)
            } else {
                _events.value = result
            }
        }
    }

    override fun loadMore() {
        println(">>> LOAD More")
        appScope.launch {
//            if ( _workOrders.value is Resource.Loading) return@launch
//            _workOrders.value = Resource.Loading
            _events.value = Resource.Success(data = loadedEvents, additionalLoading = true)
            val nextOffset = _ncount.value + PAGE_SIZE
            val result = repository.loadEvents(nextOffset, refineState.value)
            if (result is Resource.Success) {
                _ncount.value = nextOffset
                val newItems = result.data ?: emptyList()
                for (order in newItems) {
                    val key = order.key()
                    if (loadedKeys.add(key)) {
                        loadedEvents.add(order)
                    }
                }
                _events.value = Resource.Success(loadedEvents.toList(), additionalLoading = false)
            } else {
                _events.value = result
            }
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
        if (itemNumber.isBlank() || itemDate.isBlank() || message.isBlank()) return tr("events_net_nomera_ili_daty_dokumenta")
        val res = repository.sendMessageEvent(
            itemNumber,
            itemDate.substringBefore(' '),
            message
        )

        return when (res) {
            is Resource.Success -> {
                val author = appSettings.getStringOrNull(AppSettingsKeys.PERSONAL_DATA)?.trim()
                val users = buildEventRecipients(pickedEvent, currentUser = author)
                if (!users.isNullOrEmpty() && !author.isNullOrBlank()) {
                    println("PUSH_SERVICE: recipients_resolved doc_type=events recipient_count=${users.size} recipients=$users")
                    when (val pushRes = repository.sendMessageEventPUSH(
                        docId = pickedEvent?.guid ?: pickedEvent?.number ?: itemNumber,
                        docTitle = tr("events_sobytie_pickedevent_number_pickedevent_companydepart", pickedEvent?.number.orEmpty(), pickedEvent?.companyDepartment.orEmpty()),
                        authorName = author,
                        recipientNames = users,
                        message = "${author}:\n${message}",
                        screen = "events",
                        rawMessage = message
                    )) {
                        is Resource.Error -> {
                            val reason = pushRes.causes ?: friendlyError(pushRes.exception, tr("events_ne_udalos_otpravit_uvedomlenie"))
                            println("PUSH_SERVICE: Events push intent failed after message save: $reason")
                        }
                        else -> Unit
                    }
                }
                null
            }
            is Resource.Error -> res.causes ?: friendlyError(res.exception, tr("events_oshibka_otpravki_soobscheniya"))
            else -> tr("events_oshibka_otpravki_soobscheniya")
        }
    }

    override suspend fun resolveBaseDocument(rawBaseDocument: String): Resource<TreeRootResolvedDocument> {
        return repository.resolveTreeRootDocument(rawBaseDocument)
    }
    // ---------- Work Orders refine state ----------

    fun loadRefineState(): RefineState {
        val raw = appSettings.getStringOrNull(AppSettingsKeys.EVENTS_REFINE_STATE)
        if (raw.isNullOrBlank()) {
            // default state when nothing stored
            return RefineState.EventsLikeDefault
        }

        return runCatching {
            json.decodeFromString<RefineState>(raw)
        }.getOrElse {
            // if schema changed or data corrupted – fail gracefully
            RefineState.EventsLikeDefault
        }
    }

    fun saveRefineState(state: RefineState) {
        val encoded = json.encodeToString(state)
        appSettings.setString(AppSettingsKeys.EVENTS_REFINE_STATE, encoded)
    }

    private fun buildEventRecipients(event: EventItemDto?, currentUser: String?): List<String> {
        if (event == null) return emptyList()
        val rawCandidates = buildList {
            event.users.forEach { add(it.user) }
            event.messages.forEach { add(it.author) }
        }
        return normalizedPushRecipients(rawCandidates, currentUser = currentUser)
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
            val newMessage = MessageDto(
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
        transform: (EventItemDto) -> EventItemDto
    ) {
        if (guid == null) return

        val index = loadedEvents.indexOfFirst { it.guid == guid }
        if (index == -1) return

        val currentOrder = loadedEvents[index]
        val updatedOrder = transform(currentOrder)

        loadedEvents[index] = updatedOrder

        // preserve current additionalLoading flag if present
        val current = _events.value
        val additionalLoading = (current as? Resource.Success)?.additionalLoading ?: false

        _events.value = Resource.Success(
            data = loadedEvents.toList(),
            additionalLoading = additionalLoading
        )
    }

    override fun findAndSelectByNotification(identifier: String, messageHint: String?): Boolean {
        val normalizedIdentifier = normalizeIdentifier(identifier) ?: return false
        val matches = loadedEvents.filter { event -> event.matchesNormalizedIdentifier(normalizedIdentifier) }
        val picked = pickEventMatch(matches, messageHint) ?: return false
        selectItemFromList(picked.guid ?: picked.number)
        return true
    }

    override suspend fun resolveNotificationTarget(identifier: String, messageHint: String?): String? {
        val normalizedIdentifier = normalizeIdentifier(identifier) ?: return null
        val localPicked = pickEventMatch(
            loadedEvents.filter { it.matchesNormalizedIdentifier(normalizedIdentifier) },
            messageHint
        )
        if (localPicked != null) return localPicked.guid

        val searchState = _refineState.value.copy(
            searchQuery = identifier.trim(),
            searchQueryType = Refiner.SearchQueryType.CODE
        )
        val remoteMatches = (repository.loadEvents(0, searchState) as? Resource.Success)?.data.orEmpty()
            .filter { it.matchesNormalizedIdentifier(normalizedIdentifier) }
        return pickEventMatch(remoteMatches, messageHint)?.guid
    }

    override fun enterDeepLinkMode() {
        deepLinkSnapshot = DeepLinkSnapshot(
            events = loadedEvents.toList(),
            keys = loadedKeys.toSet(),
            refineState = _refineState.value,
            ncount = _ncount.value,
            selectedGuid = _selectedOrderGuid.value,
            panel = _masterScreenPanel.value,
            pickedEvent = pickedEvent,
        )
    }

    override fun restoreAfterDeepLinkIfNeeded() {
        val snapshot = deepLinkSnapshot ?: return
        deepLinkSnapshot = null
        loadedEvents.clear()
        loadedEvents.addAll(snapshot.events)
        loadedKeys.clear()
        loadedKeys.addAll(snapshot.keys)
        _refineState.value = snapshot.refineState
        _ncount.value = snapshot.ncount
        _selectedOrderGuid.value = snapshot.selectedGuid
        _masterScreenPanel.value = snapshot.panel
        pickedEvent = snapshot.pickedEvent
        _events.value = Resource.Success(loadedEvents.toList(), additionalLoading = false)
    }

    override suspend fun resolveAndOpenDeepLink(
        identifier: String,
        messageHint: String?,
        preferredSearchType: Refiner.SearchQueryType?,
    ): DeepLinkOpenResult {
        val normalizedIdentifier = normalizeIdentifier(identifier)
            ?: return DeepLinkOpenResult.NotFound
        val searchState = _refineState.value.copy(
            searchQuery = identifier.trim(),
            searchQueryType = preferredSearchType ?: Refiner.SearchQueryType.CODE
        )
        return when (val remote = repository.loadEvents(0, searchState)) {
            is Resource.Success -> {
                val matches = remote.data.orEmpty()
                    .filter { it.matchesNormalizedIdentifier(normalizedIdentifier) }
                    .sortedWith(
                        compareBy<EventItemDto> { normalizeIdentifier(it.number).orEmpty() }
                            .thenBy { normalizeIdentifier(it.guid).orEmpty() }
                            .thenBy { it.date?.toString().orEmpty() }
                    )
                when {
                    matches.isEmpty() -> DeepLinkOpenResult.NotFound
                    matches.size == 1 -> {
                        val target = matches.first()
                        applyDeepLinkCandidates(matches, selectedGuid = target.guid?.toString(), openDetails = true)
                        pickedEvent = target
                        DeepLinkOpenResult.OpenedDetails(target.guid?.toString().orEmpty())
                    }
                    else -> {
                        applyDeepLinkCandidates(matches, selectedGuid = null, openDetails = false)
                        DeepLinkOpenResult.OpenedResultsList(matches.size)
                    }
                }
            }
            is Resource.Error -> DeepLinkOpenResult.Failed(
                remote.causes ?: friendlyError(remote.exception, tr("events_oshibka_poiska_sobytiya"))
            )
            is Resource.Loading -> DeepLinkOpenResult.Failed(tr("events_poisk_sobytiya_ne_zavershen"))
        }
    }

    private fun applyDeepLinkCandidates(
        candidates: List<EventItemDto>,
        selectedGuid: String?,
        openDetails: Boolean,
    ) {
        loadedEvents.clear()
        loadedKeys.clear()
        candidates.forEach { item ->
            val key = item.key()
            if (loadedKeys.add(key)) {
                loadedEvents.add(item)
            }
        }
        _ncount.value = 0
        _selectedOrderGuid.value = selectedGuid
        _events.value = Resource.Success(loadedEvents.toList(), additionalLoading = false)
        _masterScreenPanel.value = if (openDetails) MasterPanel.Details else MasterPanel.List
    }

    private fun pickEventMatch(candidates: List<EventItemDto>, messageHint: String?): EventItemDto? {
        if (candidates.isEmpty()) return null
        val stableCandidates = candidates.sortedWith(
            compareBy<EventItemDto> { normalizeIdentifier(it.number).orEmpty() }
                .thenBy { normalizeIdentifier(it.guid).orEmpty() }
                .thenBy { it.date?.toString().orEmpty() }
        )
        if (stableCandidates.size == 1) return stableCandidates.first()
        val hint = messageHint?.trim().orEmpty()
        if (hint.isBlank()) return stableCandidates.first()
        return stableCandidates.firstOrNull { event ->
            event.messages.orEmpty().any { msg ->
                msg.comment?.contains(hint, ignoreCase = true) == true
            }
        } ?: stableCandidates.first()
    }

    private fun EventItemDto.matchesNormalizedIdentifier(normalizedIdentifier: String): Boolean {
        val guidNormalized = normalizeIdentifier(guid)
        val numberNormalized = normalizeIdentifier(number)
        return guidNormalized == normalizedIdentifier || numberNormalized == normalizedIdentifier
    }

    private fun normalizeIdentifier(value: String?): String? {
        if (value.isNullOrBlank()) return null
        return value.replace(Regex("\\s+"), "").trim().trimStart('0').ifBlank { "0" }
    }
}
