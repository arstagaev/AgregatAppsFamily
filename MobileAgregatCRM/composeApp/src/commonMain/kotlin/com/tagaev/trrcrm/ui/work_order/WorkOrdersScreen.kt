package com.tagaev.trrcrm.ui.work_order

import com.tagaev.trrcrm.ui.i18n.s

import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.sp
import com.tagaev.trrcrm.data.remote.Resource
import com.tagaev.trrcrm.data.remote.friendlyError
import com.tagaev.trrcrm.domain.complectationSearchTokenFromNomenclatureCharacteristic
import com.tagaev.trrcrm.domain.OptionChipsScrollingRow
import com.tagaev.trrcrm.domain.Refiner
import com.tagaev.trrcrm.domain.TreeRootResolvedDocument
import com.tagaev.trrcrm.models.ImageDocumentType
import com.tagaev.trrcrm.models.DocumentUploadPeriod
import com.tagaev.trrcrm.models.WorkOrderDto
import com.tagaev.trrcrm.ui.camera.DocumentCameraScreen
import com.tagaev.trrcrm.ui.complectation.ComplectationAddPhotoTopBarAction
import com.tagaev.trrcrm.ui.complectation.ComplectationOpenPhotosButton
import com.tagaev.trrcrm.ui.complectation.DocumentPhotosViewerScreen
import com.tagaev.trrcrm.ui.custom.SearchIconButtonWithIndicator
import com.tagaev.trrcrm.ui.custom.TextC
import com.tagaev.trrcrm.ui.master_screen.MasterPanel
import com.tagaev.trrcrm.ui.master_screen.MasterScreen
import com.tagaev.trrcrm.ui.master_screen.RefineSection
import com.tagaev.trrcrm.ui.master_screen.RefineScreen
import com.tagaev.trrcrm.ui.master_screen.TreeRootDocumentDetailsSheet
import com.tagaev.trrcrm.ui.master_screen.models.MessageModel
import com.tagaev.trrcrm.ui.root.LocalAppSnackbar
import com.tagaev.trrcrm.utils.formatRelativeWorkDate
import compose.icons.FeatherIcons
import compose.icons.feathericons.Camera
import compose.icons.feathericons.ChevronsUp
import compose.icons.feathericons.Filter
import compose.icons.feathericons.RefreshCw
import compose.icons.feathericons.Search
import compose.icons.feathericons.X
import kotlinx.coroutines.launch

private val WORK_ORDER_TOPBAR_SEARCH_OPTIONS = listOf(
    Refiner.SearchQueryType.CODE,
    Refiner.SearchQueryType.AUTO,
    Refiner.SearchQueryType.LICENSE_PLATE,
    Refiner.SearchQueryType.VIN_NUMBER,
    Refiner.SearchQueryType.FIX_TYPE,
    Refiner.SearchQueryType.CLIENT,
    Refiner.SearchQueryType.MASTER,
    Refiner.SearchQueryType.KIT_CHARACTERISTIC
)

private const val WORK_ORDER_REPAIR_FILTER_WARNING = """
Внимание! Сервер фильтрует ВидРемонта по токену без пробелов, поэтому возможны неточные совпадения:
• Бесплатная диагностика ↔ Диагностика
• Гарантийный ремонт ↔ Гарантийный ремонт (Сеть)
• Гарантия (TRS) ↔ Гарантия (TRS) (СЕТЬ)
• Замена жидкости (ПОЛНАЯ) ↔ Замена жидкости (ЧАСТИЧНАЯ)
"""

private fun Refiner.SearchQueryType.workOrderSearchLabel(): String {
    return when (this) {
        Refiner.SearchQueryType.CODE -> s("filter_nomer")
        Refiner.SearchQueryType.AUTO -> s("search_label_avtomobil")
        Refiner.SearchQueryType.LICENSE_PLATE -> s("search_label_gosnomer")
        Refiner.SearchQueryType.VIN_NUMBER -> "VIN"
        Refiner.SearchQueryType.FIX_TYPE -> s("search_label_vid_remonta")
        Refiner.SearchQueryType.CLIENT -> s("search_label_zakazchik")
        Refiner.SearchQueryType.MASTER -> s("filter_master")
        Refiner.SearchQueryType.KIT_CHARACTERISTIC -> s("search_label_har_komplekta")
        Refiner.SearchQueryType.TOPIC -> s("filter_tema")
        Refiner.SearchQueryType.AUTHOR -> s("events_avtor")
        Refiner.SearchQueryType.MANAGER -> s("filter_menedzher")
        Refiner.SearchQueryType.COUNTERPARTY -> s("events_kontragent")
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
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkOrdersScreen(
    component: WorkOrdersComponent,
    modifier: Modifier = Modifier
) {
    val resource by component.workOrders.collectAsState()
    val refineState by component.refineState.collectAsState()
    val panel by component.masterScreenPanel.collectAsState()
    val selectedId by component.selectedItemGuid.collectAsState()
    val transientWarning by component.transientWarning.collectAsState()
    val isCameraOpen by component.isCameraOpen.collectAsState()
    val isCameraPrecheckInProgress by component.isCameraPrecheckInProgress.collectAsState()
    val cameraDocumentNumber by component.cameraDocumentNumber.collectAsState()
    val cameraPrecheckError by component.cameraPrecheckError.collectAsState()
    val cameraUploadQuota by component.cameraUploadQuota.collectAsState()
    val cameraUploadPeriod by component.cameraUploadPeriod.collectAsState()
    val documentPhotoCount by component.documentPhotoCount.collectAsState()
    val isDocumentPhotoCountLoading by component.isDocumentPhotoCountLoading.collectAsState()
    val documentPhotoCountLoaded by component.documentPhotoCountLoaded.collectAsState()
    val isDocumentPhotoCountUiLoading = isDocumentPhotoCountLoading || !documentPhotoCountLoaded
    val isPhotosViewerOpen by component.isPhotosViewerOpen.collectAsState()
    val photosViewerDocumentNumber by component.photosViewerDocumentNumber.collectAsState()
    val photosViewerUploadPeriod by component.photosViewerUploadPeriod.collectAsState()
    var photosUploadEnabled by remember { mutableStateOf(false) }
    var photosDownloadEnabled by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()
    val showSnackbar = LocalAppSnackbar.current
    val cameraErrorSnackbarHostState = remember { SnackbarHostState() }
    var cameraSnackbarIsError by remember { mutableStateOf(false) }
    val linkedDocuments = remember { emptyList<TreeRootResolvedDocument>().toMutableStateList() }
    var characteristicMatches by remember { mutableStateOf<List<WorkOrderDto>>(emptyList()) }
    var isResolvingBaseDocument by rememberSaveable { mutableStateOf(false) }
    var isResolvingLinkedByCharacteristic by rememberSaveable { mutableStateOf(false) }
    var isSearchMode by rememberSaveable { mutableStateOf(false) }
    var searchQueryDraft by rememberSaveable { mutableStateOf(refineState.searchQuery) }
    var searchTypeDraft by rememberSaveable { mutableStateOf(refineState.searchQueryType) }
    val isTopBarLoading = resource is Resource.Loading ||
            (resource as? Resource.Success<*>)?.additionalLoading == true

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
    LaunchedEffect(refineState.searchQuery, refineState.searchQueryType, isSearchMode) {
        if (!isSearchMode) {
            searchQueryDraft = refineState.searchQuery
            searchTypeDraft = if (refineState.searchQueryType in WORK_ORDER_TOPBAR_SEARCH_OPTIONS) {
                refineState.searchQueryType
            } else {
                Refiner.SearchQueryType.CODE
            }
        }
    }
    LaunchedEffect(selectedId) {
        linkedDocuments.clear()
        characteristicMatches = emptyList()
        isResolvingBaseDocument = false
        isResolvingLinkedByCharacteristic = false
    }
    LaunchedEffect(transientWarning) {
        val warning = transientWarning
        if (!warning.isNullOrBlank()) {
            showSnackbar(warning)
            component.consumeTransientWarning()
        }
    }

    val onNomenclatureCharacteristicSearch: (String) -> Unit = { rawCharacteristic ->
        val token = complectationSearchTokenFromNomenclatureCharacteristic(rawCharacteristic)
        if (token.isBlank()) {
            showSnackbar(s("work_order_ukazhite_drugoe_znachenie_harakteristiki_dlya_poiska"))
        } else {
            scope.launch {
                isResolvingLinkedByCharacteristic = true
                try {
                    when (val res = component.searchComplectationsByKitCharacteristicToken(token)) {
                        is Resource.Success -> {
                            val list = res.data.orEmpty()
                            when {
                                list.isEmpty() -> showSnackbar(s("work_order_komplektatsii_ne_naydeny"))
                                list.size == 1 -> linkedDocuments.add(TreeRootResolvedDocument.Complectation(list.first()))
                                else -> characteristicMatches = list
                            }
                        }
                        is Resource.Error -> showSnackbar(
                            res.causes ?: friendlyError(res.exception, s("work_order_oshibka_poiska_komplektatsii"))
                        )
                        is Resource.Loading -> Unit
                    }
                } finally {
                    isResolvingLinkedByCharacteristic = false
                }
            }
        }
    }

    val applySearch: () -> Unit = {
        val normalizedQuery = searchQueryDraft.trim()
        component.setRefineState(
            refineState.copy(
                searchQuery = normalizedQuery,
                searchQueryType = searchTypeDraft
            )
        )
    }

    val hideSearchForm: () -> Unit = {
        isSearchMode = false
        searchQueryDraft = refineState.searchQuery
        searchTypeDraft = if (refineState.searchQueryType in WORK_ORDER_TOPBAR_SEARCH_OPTIONS) {
            refineState.searchQueryType
        } else {
            Refiner.SearchQueryType.CODE
        }
    }
    val clearSearchAndClose: () -> Unit = {
        isSearchMode = false
        component.setRefineState(refineState.copy(searchQuery = ""))
    }

    if (isCameraOpen) {
        val number = cameraDocumentNumber
        val uploadPeriod = cameraUploadPeriod
        if (number != null && uploadPeriod != null) {
            DocumentCameraScreen(
                documentNumber = number,
                uploadPeriod = uploadPeriod,
                title = s("complectation_kamera_number", number),
                documentType = ImageDocumentType.WorkOrder,
                initialQuota = cameraUploadQuota,
                showUploadStatusBlock = false,
                onBack = component::closeCamera,
            )
        }
        return
    }
    if (isPhotosViewerOpen) {
        val number = photosViewerDocumentNumber
        val uploadPeriod = photosViewerUploadPeriod
        if (number != null && uploadPeriod != null) {
            DocumentPhotosViewerScreen(
                documentNumber = number,
                documentType = ImageDocumentType.WorkOrder,
                uploadPeriod = uploadPeriod,
                onBack = component::closePhotosViewer,
            )
        }
        return
    }

    Box(modifier = modifier.fillMaxSize()) {
    MasterScreen(
        title = s("work_order_zakaz_naryady"),
        resource = resource,
        errorText = s("work_order_ne_udalos_zagruzit_zakaz_naryady"),
        notFoundText = s("work_order_zakaz_naryady_ne_naydeny"),
        refineState = refineState,
        onRefresh = { component.fullRefresh() },
        onLoadMore = { component.loadMore() },
        onFilterChanged = { component.setRefineState(it) },

        itemId = { it.guid.toString() },
        isItemChanged = { old, new -> old.messages.size != new.messages.size },

        listItem = { order, isChanged, onClick ->
            WorkOrderCard(
                order = order,
                isChanged = isChanged,
                onClick = onClick
            )
        },

        // Full-screen details content (not bottom-sheet)
        detailsContent = { order, onClose ->
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
            val activeWorkOrder = (currentLinked as? TreeRootResolvedDocument.WorkOrder)?.value
            val activeOrderNumber = activeWorkOrder?.number ?: order.number
            val activeUploadPeriod = DocumentUploadPeriod.from(activeWorkOrder?.date ?: order.date)
            LaunchedEffect(activeOrderNumber, photosDownloadEnabled, activeUploadPeriod) {
                if (photosDownloadEnabled) {
                    activeOrderNumber?.let { component.refreshDocumentPhotoCount(it, activeUploadPeriod) }
                }
            }
            val openDocumentPhotos: (() -> Unit)? =
                if (photosDownloadEnabled && !activeOrderNumber.isNullOrBlank()) {
                    { component.requestOpenDocumentPhotos(activeOrderNumber, activeUploadPeriod) }
                } else {
                    null
                }
            if (currentLinked != null) {
                TreeRootDocumentDetailsSheet(
                    document = currentLinked,
                    onBack = onNestedBack,
                    onOpenBaseDocument = onOpenBaseDocument,
                    onNomenclatureCharacteristicSearch = onNomenclatureCharacteristicSearch,
                    documentPhotoCount = if (photosDownloadEnabled) documentPhotoCount else 0,
                    isDocumentPhotoCountLoading = photosDownloadEnabled && isDocumentPhotoCountUiLoading,
                    onOpenDocumentPhotos = openDocumentPhotos,
                )
            } else {
                component.pickedOrder = order
                WorkOrderDetailsSheet(
                    order = order,
                    onBack = onNestedBack,
                    onNomenclatureCharacteristicSearch = onNomenclatureCharacteristicSearch,
                    documentPhotoCount = if (photosDownloadEnabled) documentPhotoCount else 0,
                    isDocumentPhotoCountLoading = photosDownloadEnabled && isDocumentPhotoCountUiLoading,
                    onOpenDocumentPhotos = openDocumentPhotos,
                    onSendMessage = { message, onResult ->
                        val number = order.number.orEmpty()
                        val date = order.date.orEmpty()
                        scope.launch {
                            val err = component.sendMessage(number, date, message)
                            if (err == null) {
                                component.addLocalMessage(order.guid.toString(), message = MessageModel(author = s("events_ya"), text = message))
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
                messageForUser = WORK_ORDER_REPAIR_FILTER_WARNING.trimIndent(),
                orderByOptions = Refiner.OrderBy.allForUi,
                sections = setOf(
                    RefineSection.STATUS,
                    RefineSection.FILTER_VAL,
                    RefineSection.REPAIR_TYPE,
                    RefineSection.ORDER,
                    RefineSection.DIRECTION
                ),
                onApply = { newState ->
                    onApply(
                        newState.copy(
                            searchQuery = refineState.searchQuery,
                            searchQueryType = refineState.searchQueryType
                        )
                    )
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
        } else {
            null
        },
        topBarTitleContent = if (panel == MasterPanel.List && isSearchMode) {
            {
                OutlinedTextField(
                    value = searchQueryDraft,
                    onValueChange = { searchQueryDraft = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 6.dp),
                    placeholder = { Text(s("work_order_poisk_zakaz_naryada")) },
                    singleLine = true,
                    enabled = !isTopBarLoading,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { applySearch() })
                )
            }
        } else {
            null
        },
        topBarActionsContent = { isLoadingTopBar ->
            if (panel == MasterPanel.Details && photosUploadEnabled) {
                val active = resolveActiveWorkOrder(selectedId, resource, linkedDocuments)
                val cameraNumber = active?.number.orEmpty()
                if (active != null && cameraNumber.isNotBlank()) {
                    ComplectationAddPhotoTopBarAction(
                        enabled = !isCameraPrecheckInProgress,
                        onClick = { component.requestOpenCamera(cameraNumber, DocumentUploadPeriod.from(active.date)) },
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
                            searchTypeDraft = if (refineState.searchQueryType in WORK_ORDER_TOPBAR_SEARCH_OPTIONS) {
                                refineState.searchQueryType
                            } else {
                                Refiner.SearchQueryType.CODE
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
        topBarBottomContent = if (panel == MasterPanel.List && isSearchMode) {
            {
                WorkOrderSearchTypeRow(
                    selected = searchTypeDraft,
                    onSelected = { searchTypeDraft = it }
                )
            }
        } else {
            null
        },

        modifier = Modifier.fillMaxSize()
    )

    if (isResolvingBaseDocument || isResolvingLinkedByCharacteristic) {
        AlertDialog(
            onDismissRequest = {},
            title = { Text(s("work_order_otkrytie_dokumenta")) },
            text = {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                    Text(s("work_order_please_wait_search"))
                }
            },
            confirmButton = {}
        )
    }

    if (characteristicMatches.size >= 2) {
        AlertDialog(
            onDismissRequest = { characteristicMatches = emptyList() },
            title = { Text(s("work_order_naydeno_characteristicmatches_size_komplektatsiy", characteristicMatches.size)) },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 320.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    characteristicMatches.forEach { item ->
                        val title = item.link?.takeIf { it.isNotBlank() }
                            ?: item.number?.takeIf { it.isNotBlank() }
                            ?: s("events_bez_nomera")
                        val subtitle = item.complectationCharacteristic?.takeIf { it.isNotBlank() } ?: "—"
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    linkedDocuments.add(TreeRootResolvedDocument.Complectation(item))
                                    characteristicMatches = emptyList()
                                },
                            tonalElevation = 2.dp,
                            shape = MaterialTheme.shapes.small
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 10.dp, vertical = 8.dp)
                            ) {
                                Text(
                                    text = title,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = subtitle,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { characteristicMatches = emptyList() }) {
                    Text(s("camera_zakryt"))
                }
            }
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

private fun resolveActiveWorkOrder(
    selectedId: String?,
    resource: Resource<List<WorkOrderDto>>,
    linkedDocuments: List<TreeRootResolvedDocument>,
): WorkOrderDto? {
    linkedDocuments.lastOrNull()?.let { linked ->
        if (linked is TreeRootResolvedDocument.WorkOrder) return linked.value
    }
    val list = (resource as? Resource.Success)?.data.orEmpty()
    return list.firstOrNull { it.guid?.toString() == selectedId }
}

@Composable
private fun WorkOrderSearchTypeRow(
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
                options = WORK_ORDER_TOPBAR_SEARCH_OPTIONS,
                selected = selected,
                onSelect = onSelected,
                labelFor = { it.workOrderSearchLabel() }
            )
        }
    }
}


@Composable
private fun WorkOrderCard(
    order: WorkOrderDto,
    isChanged: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(4.dp)
            .clip(MaterialTheme.shapes.medium)
            .clickable(onClick = onClick),
        shape = MaterialTheme.shapes.medium,
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 12.dp, vertical = 10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 8.dp)
                ) {
                    TextC(
                        text = order.number?.let { "№ $it" } ?: s("events_bez_nomera"),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 6,
                        overflow = TextOverflow.Clip,
                        allowLinkTap = false,
                        allowLongPressCopy = true,
                        onTap = onClick,
                    )
                    order.branch?.takeIf { it.isNotBlank() }?.let { branch ->
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = branch,
                            modifier = Modifier.fillMaxWidth(),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 8,
                            overflow = TextOverflow.Clip,
                            softWrap = true
                        )
                    }
                }

                Row(verticalAlignment = Alignment.Top) {
//                    if (isChanged) {
//                        CustomCircle()
//                    }
                    WorkOrderStatusBadge(order.status)
                }

//                Spacer(Modifier.weight(1f))

            }

            order.car?.takeIf { it.isNotBlank() }?.let {
                Spacer(Modifier.height(4.dp))
                Text(
                    text = it,
                    modifier = Modifier.fillMaxWidth(),
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 12,
                    overflow = TextOverflow.Clip,
                    softWrap = true
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                order.customer?.takeIf { it.isNotBlank() }?.let {
                    Text(
                        text = it,
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 10,
                        overflow = TextOverflow.Clip,
                        softWrap = true
                    )
                }
                order.repairType?.takeIf { it.isNotBlank() }?.let {
                    Text(
                        text = it,
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 10,
                        overflow = TextOverflow.Clip,
                        softWrap = true
                    )
                }
            }

            order.reason?.takeIf { it.isNotBlank() }?.let {
                Spacer(Modifier.height(6.dp))
                Text(
                    text = it,
                    modifier = Modifier.fillMaxWidth(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 16,
                    overflow = TextOverflow.Clip,
                    softWrap = true
                )
            }
            Spacer(Modifier.fillMaxWidth().height(5.dp))
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                order.date?.let {
                    Text(
                        text = s("work_order_sozd_it", it),
                        modifier = Modifier.fillMaxWidth(),
                        style = TextStyle(fontSize = 9.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 4,
                        overflow = TextOverflow.Clip,
                        softWrap = true
                    )
                }
                order.messages.lastOrNull()?.let {
                    Text(
                        text = s("work_order_izm_formatrelativeworkdate_it_workdate", formatRelativeWorkDate(it.workDate)),
                        modifier = Modifier.fillMaxWidth(),
                        style = TextStyle(fontSize = 9.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 6,
                        overflow = TextOverflow.Clip,
                        softWrap = true
                    )
                }
                if (order.messages.lastOrNull() == null) {
                    Text(
                        text = s("work_order_soobscheniy_net"),
                        modifier = Modifier.fillMaxWidth(),
                        style = TextStyle(fontSize = 9.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 3,
                        overflow = TextOverflow.Clip,
                        softWrap = true
                    )
                }
            }
        }
    }
}

@Composable
fun WorkOrderStatusBadge(status: String?) {
    if (status.isNullOrBlank()) return

    val (bg, fg) = when (status) {
        "Закрыт" -> MaterialTheme.colorScheme.tertiaryContainer to MaterialTheme.colorScheme.onTertiaryContainer
        "В работе" -> MaterialTheme.colorScheme.primaryContainer to MaterialTheme.colorScheme.onPrimaryContainer
        else -> MaterialTheme.colorScheme.secondaryContainer to MaterialTheme.colorScheme.onSecondaryContainer
    }

    Surface(
        color = bg,
        contentColor = fg,
        shape = MaterialTheme.shapes.large
    ) {
        Text(
            text = status,
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
            maxLines = 4,
            overflow = TextOverflow.Clip,
            softWrap = true
        )
    }
}
