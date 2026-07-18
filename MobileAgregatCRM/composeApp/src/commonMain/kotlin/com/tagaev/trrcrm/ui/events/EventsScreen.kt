package com.tagaev.trrcrm.ui.events

import com.tagaev.trrcrm.ui.i18n.s

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.toMutableStateList
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.tagaev.trrcrm.data.remote.Resource
import com.tagaev.trrcrm.data.remote.friendlyError
import com.tagaev.trrcrm.domain.OptionChipsScrollingRow
import com.tagaev.trrcrm.domain.Refiner
import com.tagaev.trrcrm.models.EventItemDto
import com.tagaev.trrcrm.models.ImageDocumentType
import com.tagaev.trrcrm.push.rememberNotificationPermissionRequester
import com.tagaev.trrcrm.domain.TreeRootDocumentKind
import com.tagaev.trrcrm.domain.TreeRootResolvedDocument
import com.tagaev.trrcrm.domain.linkTabCaptionForListRow
import com.tagaev.trrcrm.domain.linkTabLabel
import com.tagaev.trrcrm.ui.camera.DocumentCameraScreen
import com.tagaev.trrcrm.ui.complectation.ComplectationAddPhotoTopBarAction
import com.tagaev.trrcrm.ui.complectation.DocumentPhotosViewerScreen
import com.tagaev.trrcrm.ui.custom.SessionTrrImage
import com.tagaev.trrcrm.ui.custom.SearchIconButtonWithIndicator
import com.tagaev.trrcrm.ui.custom.TextC
import com.tagaev.trrcrm.ui.mainscreen.StatusBadge
import com.tagaev.trrcrm.ui.mainscreen.format
import com.tagaev.trrcrm.ui.master_screen.LinkedDocumentStackTabStrip
import com.tagaev.trrcrm.ui.master_screen.MasterPanel
import com.tagaev.trrcrm.ui.master_screen.MasterScreen
import com.tagaev.trrcrm.ui.master_screen.RefineSection
import com.tagaev.trrcrm.ui.master_screen.RefineScreen
import com.tagaev.trrcrm.ui.master_screen.TreeRootDocumentDetailsSheet
import com.tagaev.trrcrm.ui.master_screen.models.MessageModel
import com.tagaev.trrcrm.ui.root.LocalAppSnackbar
import com.tagaev.trrcrm.utils.formatDDMMYYYY
import compose.icons.FeatherIcons
import compose.icons.feathericons.Camera
import compose.icons.feathericons.ChevronsUp
import compose.icons.feathericons.Filter
import compose.icons.feathericons.RefreshCw
import compose.icons.feathericons.Search
import compose.icons.feathericons.X
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.datetime.format
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource

private const val EVENTS_LOADING_MIN_DELAY_MS = 900L
private var eventsLoadingShownThisSession = false
private val EVENTS_TOPBAR_SEARCH_OPTIONS = listOf(
    Refiner.SearchQueryType.TOPIC,
    Refiner.SearchQueryType.CODE,
    Refiner.SearchQueryType.AUTHOR,
    Refiner.SearchQueryType.COUNTERPARTY,
    Refiner.SearchQueryType.AUTO,
)

private fun Refiner.SearchQueryType.eventsSearchLabel(): String = when (this) {
    Refiner.SearchQueryType.TOPIC -> s("filter_tema")
    Refiner.SearchQueryType.CODE -> s("filter_nomer")
    Refiner.SearchQueryType.AUTHOR -> s("events_avtor")
    Refiner.SearchQueryType.COUNTERPARTY -> s("events_kontragent")
    Refiner.SearchQueryType.AUTO -> s("search_label_avtomobil")
    Refiner.SearchQueryType.MANAGER -> s("filter_menedzher")
    Refiner.SearchQueryType.MASTER -> s("filter_master")
    Refiner.SearchQueryType.KIT_CHARACTERISTIC -> s("search_label_har_komplekta")
    Refiner.SearchQueryType.LICENSE_PLATE -> s("search_label_gosnomer")
    Refiner.SearchQueryType.VIN_NUMBER -> "VIN"
    Refiner.SearchQueryType.FIX_TYPE -> s("search_label_vid_remonta")
    Refiner.SearchQueryType.CLIENT -> s("search_label_zakazchik")
    Refiner.SearchQueryType.ROUTE -> s("search_label_marshrut")
    Refiner.SearchQueryType.CARRIER -> s("search_label_perevozchik")
    Refiner.SearchQueryType.SUBJECT_MATTER -> s("incoming_sut_obrascheniya")
    Refiner.SearchQueryType.PHONE -> s("search_label_telefon")
    Refiner.SearchQueryType.REPAIR_TEMPLATE_MODEL,
    Refiner.SearchQueryType.REPAIR_TEMPLATE_NAME,
    Refiner.SearchQueryType.REPAIR_TEMPLATE_CODE,
    Refiner.SearchQueryType.REPAIR_TEMPLATE_YEAR_FROM,
    Refiner.SearchQueryType.REPAIR_TEMPLATE_YEAR_TO,
    Refiner.SearchQueryType.REPAIR_TEMPLATE_TRANSMISSION,
    Refiner.SearchQueryType.REPAIR_TEMPLATE_ENGINE,
    Refiner.SearchQueryType.REPAIR_TEMPLATE_REPAIR_KIND,
    Refiner.SearchQueryType.PURPOSE,
    -> s("nav_kalkulyatsiya")
}

@Composable
fun EventsScreen(
    component: EventsComponent,
    modifier: Modifier = Modifier
) {
    val resource by component.events.collectAsState()
    val refineState by component.refineState.collectAsState()
    val panel by component.masterScreenPanel.collectAsState()
    val selectedId by component.selectedItemGuid.collectAsState()
    var isSearchMode by rememberSaveable { mutableStateOf(false) }
    var searchQueryDraft by rememberSaveable { mutableStateOf(refineState.searchQuery) }
    var searchTypeDraft by rememberSaveable { mutableStateOf(refineState.searchQueryType) }

    val isCameraOpen by component.isCameraOpen.collectAsState()
    val isCameraPrecheckInProgress by component.isCameraPrecheckInProgress.collectAsState()
    val cameraDocumentNumber by component.cameraDocumentNumber.collectAsState()
    val cameraPrecheckError by component.cameraPrecheckError.collectAsState()
    val cameraUploadQuota by component.cameraUploadQuota.collectAsState()
    val documentPhotoCount by component.documentPhotoCount.collectAsState()
    val isDocumentPhotoCountLoading by component.isDocumentPhotoCountLoading.collectAsState()
    val documentPhotoCountLoaded by component.documentPhotoCountLoaded.collectAsState()
    val isDocumentPhotoCountUiLoading = isDocumentPhotoCountLoading || !documentPhotoCountLoaded
    val isPhotosViewerOpen by component.isPhotosViewerOpen.collectAsState()
    val photosViewerDocumentNumber by component.photosViewerDocumentNumber.collectAsState()
    var photosUploadEnabled by remember { mutableStateOf(true) }
    var photosDownloadEnabled by remember { mutableStateOf(true) }
    val cameraErrorSnackbarHostState = remember { SnackbarHostState() }
    var cameraSnackbarIsError by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()
    val showSnackbar = LocalAppSnackbar.current
    val linkedDocuments = remember { emptyList<TreeRootResolvedDocument>().toMutableStateList() }
    var isResolvingBaseDocument by remember { mutableStateOf(false) }
    val requestNotificationPermission =
        rememberNotificationPermissionRequester { granted ->

            if (!granted) {
                showSnackbar(s("events_neobhodimo_razreshenie_na_uvedomleniya"))
            }
        }

    LaunchedEffect(Unit) {
        photosUploadEnabled = component.isPhotosUploadEnabled()
        photosDownloadEnabled = component.isPhotosDownloadEnabled()
    }
    LaunchedEffect(cameraPrecheckError) {
        val error = cameraPrecheckError
        if (!error.isNullOrBlank()) {
            cameraSnackbarIsError = true
            cameraErrorSnackbarHostState.showSnackbar(
                com.tagaev.trrcrm.data.remote.userFacingMessage(error, error),
            )
            component.consumeCameraPrecheckError()
        }
    }

    LaunchedEffect(selectedId) {
        linkedDocuments.clear()
        isResolvingBaseDocument = false
    }

    LaunchedEffect(Unit) {
        requestNotificationPermission()
    }

    val isEventsLoading = when (val state = resource) {
        is Resource.Loading -> true
        is Resource.Success -> state.additionalLoading
        is Resource.Error -> false
    }
    val currentIsEventsLoading by rememberUpdatedState(isEventsLoading)
    val isTopBarLoading = resource is Resource.Loading ||
            (resource as? Resource.Success<*>)?.additionalLoading == true

    LaunchedEffect(refineState.searchQuery, refineState.searchQueryType, isSearchMode) {
        if (!isSearchMode) {
            searchQueryDraft = refineState.searchQuery
            searchTypeDraft = if (refineState.searchQueryType in EVENTS_TOPBAR_SEARCH_OPTIONS) {
                refineState.searchQueryType
            } else {
                Refiner.SearchQueryType.TOPIC
            }
        }
    }

    val applySearch: () -> Unit = {
        component.setRefineState(
            refineState.copy(
                searchQuery = searchQueryDraft.trim(),
                searchQueryType = searchTypeDraft
            )
        )
    }
    val hideSearchForm: () -> Unit = {
        isSearchMode = false
        searchQueryDraft = refineState.searchQuery
        searchTypeDraft = if (refineState.searchQueryType in EVENTS_TOPBAR_SEARCH_OPTIONS) {
            refineState.searchQueryType
        } else {
            Refiner.SearchQueryType.TOPIC
        }
    }
    val clearSearchAndClose: () -> Unit = {
        isSearchMode = false
        component.setRefineState(refineState.copy(searchQuery = ""))
    }
    val handleDetailsBack: () -> Unit = {
        if (linkedDocuments.isNotEmpty()) {
            linkedDocuments.removeAt(linkedDocuments.lastIndex)
        } else {
            component.selectItemFromList(null)
            component.changePanel(MasterPanel.List)
        }
    }
    val sessionLoadingImage = remember { SessionTrrImage.get() }
    var isLoadingOverlayVisible by remember { mutableStateOf(false) }
    var wasLoading by remember { mutableStateOf(false) }
    var loadingSessionId by remember { mutableStateOf(0) }
    var minDelayPassed by remember { mutableStateOf(true) }
    var shouldShowOverlayForCurrentLoad by remember { mutableStateOf(false) }

    LaunchedEffect(isEventsLoading) {
        if (isEventsLoading && !wasLoading && !eventsLoadingShownThisSession) {
            isLoadingOverlayVisible = true
            minDelayPassed = false
            loadingSessionId += 1
            shouldShowOverlayForCurrentLoad = true
            eventsLoadingShownThisSession = true
        }

        if (!isEventsLoading && wasLoading && shouldShowOverlayForCurrentLoad && minDelayPassed) {
            isLoadingOverlayVisible = false
            shouldShowOverlayForCurrentLoad = false
        }
        wasLoading = isEventsLoading
    }

    LaunchedEffect(loadingSessionId) {
        if (loadingSessionId == 0) return@LaunchedEffect
        delay(EVENTS_LOADING_MIN_DELAY_MS)
        minDelayPassed = true
        if (!currentIsEventsLoading && shouldShowOverlayForCurrentLoad) {
            isLoadingOverlayVisible = false
            shouldShowOverlayForCurrentLoad = false
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            isLoadingOverlayVisible = false
            shouldShowOverlayForCurrentLoad = false
        }
    }

    if (isCameraOpen) {
        val number = cameraDocumentNumber
        if (number != null) {
            DocumentCameraScreen(
                documentNumber = number,
                title = s("complectation_kamera_number", number),
                documentType = ImageDocumentType.Event,
                initialQuota = cameraUploadQuota,
                showUploadStatusBlock = false,
                onBack = component::closeCamera,
            )
        }
        return
    }
    if (isPhotosViewerOpen) {
        val number = photosViewerDocumentNumber
        if (number != null) {
            DocumentPhotosViewerScreen(
                documentNumber = number,
                documentType = ImageDocumentType.Event,
                onBack = component::closePhotosViewer,
            )
        }
        return
    }

    Box(modifier = modifier.fillMaxSize()) {
        MasterScreen(
            title = s("nav_sobytiya"),
            resource = resource,
            errorText = s("events_ne_udalos_zagruzit_sobytiya"),
            notFoundText = s("events_sobytiya_ne_naydeny"),
            refineState = refineState,
            onRefresh = { component.fullRefresh() },
            onLoadMore = { component.loadMore() },
            onFilterChanged = { component.setRefineState(it) },

            itemId = { it.guid.toString() },
            isItemChanged = { old, new -> old.messages.size != new.messages.size },

            listItem = { order, isChanged, onClick ->
                EventCard(
                    ev = order,
                    onClick = onClick
                )
            },

            // Full-screen details content (not bottom-sheet)
            detailsContent = { ev, onClose ->
                val onOpenBaseDocument: (String) -> Unit = { rawBaseDocument ->
                    scope.launch {
                        isResolvingBaseDocument = true
                        try {
                            when (val resolved = runCatching { component.resolveBaseDocument(rawBaseDocument) }
                                .getOrElse { e -> Resource.Error(causes = friendlyError(e, s("events_oshibka_poiska_dokumenta"))) }) {
                                is Resource.Success -> linkedDocuments.add(resolved.data)
                                is Resource.Error -> showSnackbar(resolved.causes ?: s("events_dokument_osnovanie_ne_nayden"))
                                is Resource.Loading -> Unit
                            }
                        } finally {
                            isResolvingBaseDocument = false
                        }
                    }
                }
                val onNestedBack: () -> Unit = {
                    if (linkedDocuments.isNotEmpty()) linkedDocuments.removeAt(linkedDocuments.lastIndex)
                    else onClose()
                }

                val currentLinked = linkedDocuments.lastOrNull()
                val activeEventNumber = (currentLinked as? TreeRootResolvedDocument.Event)?.value?.number
                    ?: ev.number
                LaunchedEffect(activeEventNumber, photosDownloadEnabled) {
                    if (photosDownloadEnabled) {
                        activeEventNumber?.let { component.refreshDocumentPhotoCount(it) }
                    }
                }
                val openDocumentPhotos: (() -> Unit)? =
                    if (photosDownloadEnabled && !activeEventNumber.isNullOrBlank()) {
                        { component.requestOpenDocumentPhotos(activeEventNumber) }
                    } else {
                        null
                    }
                if (currentLinked != null) {
                    TreeRootDocumentDetailsSheet(
                        document = currentLinked,
                        onBack = onNestedBack,
                        onOpenBaseDocument = onOpenBaseDocument,
                        documentPhotoCount = if (photosDownloadEnabled) documentPhotoCount else 0,
                        isDocumentPhotoCountLoading = photosDownloadEnabled && isDocumentPhotoCountUiLoading,
                        onOpenDocumentPhotos = openDocumentPhotos,
                    )
                } else {
                    EventDetailsSheet(
                        event = ev,
                        onBack = onNestedBack,
                        onOpenBaseDocument = onOpenBaseDocument,
                        documentPhotoCount = if (photosDownloadEnabled) documentPhotoCount else 0,
                        isDocumentPhotoCountLoading = photosDownloadEnabled && isDocumentPhotoCountUiLoading,
                        onOpenDocumentPhotos = openDocumentPhotos,
                        onSendMessage = { message, onResult ->
                            val number = ev.number.orEmpty()
                            val date = ev.date?.format(formatDDMMYYYY).orEmpty()
                            scope.launch {
                                component.pickedEvent = ev
                                val err = component.sendMessage(itemNumber = number, itemDate = date, message = message)
                                if (err == null) {
                                    component.addLocalMessage(ev.guid.toString(), message = MessageModel(author = s("events_ya"), text = message))
                                }
                                onResult(err)
                            }
                        }
                    )
                }
            },

            // Full-screen filter screen (not dialog)
            filterScreen = { current, onDismiss, onApply ->
                RefineScreen(
                    current = current,
                    onBack = onDismiss,
                    sections = setOf(
                        RefineSection.STATUS,
                        RefineSection.DIRECTION
                    ),
                    onApply = { newState ->
                        val applied = newState.copy(
                            searchQuery = refineState.searchQuery,
                            searchQueryType = refineState.searchQueryType
                        )
                        component.setRefineState(applied)
                        onApply(applied)
                    }
                )
            },

            panel = panel,
            onPanelChange = {
                component.changePanel(it)

            },

            selectedItemId = selectedId,
            onSelectedItemChange = { id -> component.selectItemFromList(id) },
            topBarNavigationIcon = if (panel == MasterPanel.List && isSearchMode) {
                {
                    Row {
                        IconButton(
                            onClick = hideSearchForm,
                            enabled = !isTopBarLoading
                        ) {
                            Icon(FeatherIcons.ChevronsUp, contentDescription = s("events_skryt_poisk"))
                        }
                        IconButton(
                            onClick = clearSearchAndClose,
                            enabled = !isTopBarLoading
                        ) {
                            Icon(FeatherIcons.X, contentDescription = s("events_ochistit_i_zakryt_poisk"))
                        }
                    }
                }
            } else null,
            topBarTitleContent = when {
                panel == MasterPanel.List && isSearchMode -> {
                    {
                        OutlinedTextField(
                            value = searchQueryDraft,
                            onValueChange = { searchQueryDraft = it },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 6.dp),
                            placeholder = { Text(s("events_poisk_sobytiya")) },
                            singleLine = true,
                            enabled = !isTopBarLoading,
                            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(imeAction = ImeAction.Search),
                            keyboardActions = androidx.compose.foundation.text.KeyboardActions(onSearch = { applySearch() })
                        )
                    }
                }
                panel == MasterPanel.Details && linkedDocuments.isNotEmpty() -> {
                    { Text(linkedDocuments.last().linkTabLabel(), modifier = Modifier.padding(horizontal = 10.dp)) }
                }
                else -> null
            },
            topBarActionsContent = { isLoadingTopBar ->
                if (panel == MasterPanel.Details && photosUploadEnabled) {
                    val active = resolveActiveEvent(selectedId, resource, linkedDocuments)
                    val cameraNumber = active?.number.orEmpty()
                    if (active != null && cameraNumber.isNotBlank()) {
                        ComplectationAddPhotoTopBarAction(
                            enabled = !isCameraPrecheckInProgress,
                            onClick = { component.requestOpenCamera(cameraNumber) },
                        )
                    }
                } else if (panel == MasterPanel.List) {
                    if (isSearchMode) {
                        if (isLoadingTopBar) {
                            CircularProgressIndicator(
                                modifier = Modifier
                                    .padding(horizontal = 8.dp)
                                    .size(18.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            IconButton(onClick = applySearch) {
                                Icon(FeatherIcons.Search, contentDescription = s("events_iskat"))
                            }
                        }
                    } else {
                        IconButton(onClick = { component.changePanel(MasterPanel.Filter) }) {
                            Icon(FeatherIcons.Filter, contentDescription = s("events_filtr"))
                        }
                        SearchIconButtonWithIndicator(
                            showIndicator = refineState.searchQuery.isNotBlank(),
                            enabled = !isLoadingTopBar,
                            onClick = {
                                searchQueryDraft = refineState.searchQuery
                                searchTypeDraft = if (refineState.searchQueryType in EVENTS_TOPBAR_SEARCH_OPTIONS) {
                                    refineState.searchQueryType
                                } else {
                                    Refiner.SearchQueryType.TOPIC
                                }
                                isSearchMode = true
                            }
                        )
                        if (isLoadingTopBar) {
                            CircularProgressIndicator(
                                modifier = Modifier
                                    .padding(horizontal = 8.dp)
                                    .size(18.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            IconButton(onClick = { component.fullRefresh() }) {
                                Icon(FeatherIcons.RefreshCw, contentDescription = s("menu_obnovit"))
                            }
                        }
                    }
                }
            },
            topBarTopContent = if (panel == MasterPanel.Details && selectedId != null && linkedDocuments.isNotEmpty()) {
                {
                    val rows = (resource as? Resource.Success)?.data.orEmpty()
                    val rootEv = rows.firstOrNull { it.guid.toString() == selectedId }
                    val rootLabel = rootEv?.let { ev ->
                        linkTabCaptionForListRow(ev.link, TreeRootDocumentKind.EVENT, ev.number)
                    }.orEmpty()
                    LinkedDocumentStackTabStrip(
                        tabLabels = listOf(rootLabel) + linkedDocuments.map { it.linkTabLabel() },
                        selectedIndex = linkedDocuments.size,
                        onTabClick = { idx ->
                            if (idx == 0) linkedDocuments.clear()
                            else while (linkedDocuments.size > idx) {
                                linkedDocuments.removeAt(linkedDocuments.lastIndex)
                            }
                        },
                    )
                }
            } else null,
            topBarBottomContent = if (panel == MasterPanel.List && isSearchMode) {
                {
                    EventsSearchTypeRow(
                        selected = searchTypeDraft,
                        onSelected = { searchTypeDraft = it }
                    )
                }
            } else null,
            onDetailsBack = handleDetailsBack,

            modifier = Modifier.fillMaxSize()
        )

        if (isResolvingBaseDocument) {
            AlertDialog(
                onDismissRequest = {},
                title = { Text(s("events_pozhaluysta_podozhdite")) },
                text = { Text(s("events_ischem_dokument_osnovanie")) },
                confirmButton = {}
            )
        }

        if (isLoadingOverlayVisible) {
            EventsLoadingImageOverlay(
                image = sessionLoadingImage,
                modifier = Modifier.fillMaxSize()
            )
        }

        SnackbarHost(
            hostState = cameraErrorSnackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter),
        ) { data ->
            Snackbar(
                snackbarData = data,
                containerColor = if (cameraSnackbarIsError) {
                    MaterialTheme.colorScheme.errorContainer
                } else {
                    MaterialTheme.colorScheme.inverseSurface
                },
                contentColor = if (cameraSnackbarIsError) {
                    MaterialTheme.colorScheme.onErrorContainer
                } else {
                    MaterialTheme.colorScheme.inverseOnSurface
                },
            )
        }
    }
}

private fun resolveActiveEvent(
    selectedId: String?,
    resource: Resource<List<EventItemDto>>,
    linkedDocuments: List<TreeRootResolvedDocument>,
): EventItemDto? {
    linkedDocuments.lastOrNull()?.let { linked ->
        if (linked is TreeRootResolvedDocument.Event) return linked.value
    }
    val list = (resource as? Resource.Success)?.data.orEmpty()
    return list.firstOrNull { it.guid.toString() == selectedId }
}

@Composable
private fun EventsLoadingImageOverlay(
    image: DrawableResource,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.background(MaterialTheme.colorScheme.surface),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(image),
            contentDescription = s("events_zagruzka_sobytiy"),
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
    }
}

@Composable
private fun EventsSearchTypeRow(
    selected: Refiner.SearchQueryType,
    onSelected: (Refiner.SearchQueryType) -> Unit
) {
    Surface(
        tonalElevation = 2.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = s("events_poisk_po"),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            OptionChipsScrollingRow(
                options = EVENTS_TOPBAR_SEARCH_OPTIONS,
                selected = selected,
                onSelect = onSelected,
                labelFor = { it.eventsSearchLabel() }
            )
        }
    }
}


@Composable
fun EventCard(
    ev: EventItemDto,
    onClick: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(3.dp)
        ) {
            // Header: number + status at right
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextC(
                    text = ev.number?.let { "№ $it" } ?: s("events_bez_nomera"),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    allowLinkTap = false,
                    allowLongPressCopy = true,
                )
                StatusBadge(ev.state.orEmpty())
            }

            Spacer(Modifier.height(2.dp))

            Text(
                ev.subject ?: (ev.content ?: ""),
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            // Compact meta block
            val hasMeta =
                !ev.baseDocument.isNullOrBlank() ||
                        !ev.eventType.isNullOrBlank() ||
                        !ev.organization.isNullOrBlank() ||
                        !ev.companyDepartment.isNullOrBlank() ||
                        !ev.counterparty.isNullOrBlank() ||
                        !ev.author.isNullOrBlank() ||
                        !ev.modifiedDate.isNullOrBlank()

            if (hasMeta) {
                Spacer(Modifier.height(2.dp))
                HorizontalDivider(
                    modifier = Modifier.fillMaxWidth(),
                    thickness = 1.dp,
                    color = MaterialTheme.colorScheme.outlineVariant
                )
                Spacer(Modifier.height(2.dp))

                // First row: Эпик + Вид события
//                Row(
//                    modifier = Modifier.fillMaxWidth(),
//                    horizontalArrangement = Arrangement.spacedBy(12.dp)
//                ) {
//
//                }

                // Second row: Организация + Подразделение
                if (!ev.organization.isNullOrBlank() || !ev.companyDepartment.isNullOrBlank()) {
                    Spacer(Modifier.height(1.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        EventMetaColumn(
                            label = s("events_organizatsiya"),
                            value = ev.organization ?: "undefined",
                            modifier = Modifier.weight(1f, fill = true)
                        )
                        EventMetaColumn(
                            label = s("events_podrazdelenie"),
                            value = ev.companyDepartment?: "undefined",
                            modifier = Modifier.weight(1f, fill = true)
                        )

                        EventMetaColumn(
                            label = s("events_dokument_osnovanie"),
                            value = ev.baseDocument?: "undefined",
                            modifier = Modifier.weight(1f, fill = true)
                        )
                        EventMetaColumn(
                            label = s("events_vid_sobytiya"),
                            value = ev.eventType?: "undefined",
                            modifier = Modifier.weight(1f, fill = true)
                        )
                    }
                }
                // Fourth row: Автор
                if (!ev.author.isNullOrBlank() && !ev.users.isNullOrEmpty()) {
                    Spacer(Modifier.height(1.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        EventMetaColumn(
                            label = s("events_avtor"),
                            value = ev.author,
                            modifier = Modifier.weight(1f, fill = true)
                        )
                        // Контрагент
                        ev.counterparty?.takeIf { it.isNotBlank() }?.let { counterparty ->
//                            Spacer(Modifier.height(1.dp))
                            EventMetaColumn(
                                label = s("events_kontragent"),
                                value = counterparty,
                                modifier = Modifier.weight(1f, fill = true)
                            )
                        }
                        EventMetaColumn(
                            label = s("events_kolichestvo_uchastnikov"),
                            value = "${ev.users.size}",
                            modifier = Modifier.weight(1f, fill = true)
                        )
                    }
                }
            }

            val createdText = ev.date?.format(format)
            val modifiedText = ev.modifiedDate

            if (!createdText.isNullOrBlank() || !modifiedText.isNullOrBlank()) {
                Spacer(Modifier.height(4.dp))
                HorizontalDivider(
                    modifier = Modifier.fillMaxWidth(),
                    thickness = 0.5.dp,
                    color = MaterialTheme.colorScheme.outlineVariant
                )
                Spacer(Modifier.height(2.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    createdText?.let {
                        Text(
                            text = s("events_sozdano_it", it),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    modifiedText?.takeIf { it.isNotBlank() }?.let {
                        Text(
                            text = s("events_izm_it", it),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EventMetaColumn(
    label: String,
    value: String?,
    modifier: Modifier = Modifier
) {
    if (value.isNullOrBlank()) return
    Column(modifier = modifier) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.labelSmall,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis
        )
    }
}
