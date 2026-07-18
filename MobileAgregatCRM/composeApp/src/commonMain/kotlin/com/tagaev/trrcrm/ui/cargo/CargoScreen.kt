package com.tagaev.trrcrm.ui.cargo

import com.tagaev.trrcrm.ui.i18n.s

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.AlertDialog
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.tagaev.trrcrm.data.remote.Resource
import com.tagaev.trrcrm.data.remote.friendlyError
import com.tagaev.trrcrm.domain.OptionChipsScrollingRow
import com.tagaev.trrcrm.domain.Refiner
import com.tagaev.trrcrm.domain.TreeRootDocumentKind
import com.tagaev.trrcrm.domain.TreeRootResolvedDocument
import com.tagaev.trrcrm.domain.linkTabCaptionForListRow
import com.tagaev.trrcrm.domain.linkTabLabel
import com.tagaev.trrcrm.models.CargoDto
import com.tagaev.trrcrm.models.ImageDocumentType
import com.tagaev.trrcrm.ui.camera.DocumentCameraScreen
import com.tagaev.trrcrm.ui.complectation.ComplectationAddPhotoTopBarAction
import com.tagaev.trrcrm.ui.complectation.DocumentPhotosViewerScreen
import com.tagaev.trrcrm.ui.custom.SearchIconButtonWithIndicator
import com.tagaev.trrcrm.ui.custom.StatusBadge
import com.tagaev.trrcrm.ui.custom.StatusStyle
import com.tagaev.trrcrm.ui.custom.TextC
import com.tagaev.trrcrm.ui.custom.withEscapedNewlines
import com.tagaev.trrcrm.ui.master_screen.LinkedDocumentStackTabStrip
import com.tagaev.trrcrm.ui.master_screen.MasterPanel
import com.tagaev.trrcrm.ui.master_screen.MasterScreen
import com.tagaev.trrcrm.ui.master_screen.RefineSection
import com.tagaev.trrcrm.ui.master_screen.RefineScreen
import com.tagaev.trrcrm.ui.master_screen.TreeRootDocumentDetailsSheet
import com.tagaev.trrcrm.ui.root.LocalAppSnackbar
import com.tagaev.trrcrm.ui.style.DefaultColors
import compose.icons.FeatherIcons
import compose.icons.feathericons.Camera
import compose.icons.feathericons.ChevronsUp
import compose.icons.feathericons.Filter
import compose.icons.feathericons.RefreshCw
import compose.icons.feathericons.Search
import compose.icons.feathericons.X
import kotlinx.coroutines.launch

private val CARGO_TOPBAR_SEARCH_OPTIONS = listOf(
    Refiner.SearchQueryType.CODE,
    Refiner.SearchQueryType.ROUTE,
    Refiner.SearchQueryType.CARRIER,
    Refiner.SearchQueryType.AUTHOR,
)

private fun Refiner.SearchQueryType.cargoSearchLabel(): String = when (this) {
    Refiner.SearchQueryType.CODE -> s("filter_nomer")
    Refiner.SearchQueryType.ROUTE -> "Маршрут"
    Refiner.SearchQueryType.CARRIER -> "Перевозчик"
    Refiner.SearchQueryType.AUTHOR -> s("events_avtor")
    Refiner.SearchQueryType.TOPIC -> s("filter_tema")
    Refiner.SearchQueryType.MANAGER -> s("filter_menedzher")
    Refiner.SearchQueryType.COUNTERPARTY -> s("events_kontragent")
    Refiner.SearchQueryType.MASTER -> s("filter_master")
    Refiner.SearchQueryType.KIT_CHARACTERISTIC -> "Хар. комплекта"
    Refiner.SearchQueryType.AUTO -> "Автомобиль"
    Refiner.SearchQueryType.LICENSE_PLATE -> "Госномер"
    Refiner.SearchQueryType.VIN_NUMBER -> "VIN"
    Refiner.SearchQueryType.FIX_TYPE -> "Вид ремонта"
    Refiner.SearchQueryType.CLIENT -> "Заказчик"
    Refiner.SearchQueryType.SUBJECT_MATTER -> s("incoming_sut_obrascheniya")
    Refiner.SearchQueryType.PHONE -> "Телефон"
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
fun CargoScreen(component: CargoComponent, modifier: Modifier = Modifier) {
    val resource by component.cargos.collectAsState()
    val refineState by component.refineState.collectAsState()
    val panel by component.masterScreenPanel.collectAsState()
    val selectedId by component.selectedItemGuid.collectAsState()
    var isSearchMode by rememberSaveable { mutableStateOf(false) }
    var searchQueryDraft by rememberSaveable { mutableStateOf(refineState.searchQuery) }
    var searchTypeDraft by rememberSaveable { mutableStateOf(refineState.searchQueryType) }
    val isTopBarLoading = resource is Resource.Loading ||
            (resource as? Resource.Success<*>)?.additionalLoading == true

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
    var isResolvingBaseDocument by rememberSaveable { mutableStateOf(false) }

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
            searchTypeDraft = if (refineState.searchQueryType in CARGO_TOPBAR_SEARCH_OPTIONS) {
                refineState.searchQueryType
            } else {
                Refiner.SearchQueryType.CODE
            }
        }
    }
    LaunchedEffect(selectedId) {
        linkedDocuments.clear()
        isResolvingBaseDocument = false
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
        searchTypeDraft = if (refineState.searchQueryType in CARGO_TOPBAR_SEARCH_OPTIONS) {
            refineState.searchQueryType
        } else {
            Refiner.SearchQueryType.CODE
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

    if (isCameraOpen) {
        val number = cameraDocumentNumber
        if (number != null) {
            DocumentCameraScreen(
                documentNumber = number,
                title = s("complectation_kamera_number", number),
                documentType = ImageDocumentType.Delivery,
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
                documentType = ImageDocumentType.Delivery,
                onBack = component::closePhotosViewer,
            )
        }
        return
    }

    Box(modifier = modifier.fillMaxSize()) {
    MasterScreen(
        title = s("nav_dostavki"),
        resource = resource,
        errorText = s("cargo_ne_udalos_zagruzit_dostavki"),
        notFoundText = s("cargo_dostavki_ne_naydeny"),
        refineState = refineState,
        onRefresh = { component.fullRefresh() },
        onLoadMore = { component.loadMore() },
        onFilterChanged = { component.setRefineState(it) },

        itemId = { it.guid.toString() },
        isItemChanged = { old, new -> old.orders.size != new.orders.size },

        listItem = { cargo, _, onClick ->
            CargoListItem(
                cargo = cargo,
                onClick = onClick
            )
        },

        // Full-screen details content (not bottom-sheet)
        detailsContent = { cargo, onClose ->
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
            val activeCargoNumber = (currentLinked as? TreeRootResolvedDocument.Cargo)?.value?.number
                ?: cargo.number
            LaunchedEffect(activeCargoNumber, photosDownloadEnabled) {
                if (photosDownloadEnabled) {
                    activeCargoNumber.takeIf { it.isNotBlank() }?.let {
                        component.refreshDocumentPhotoCount(it)
                    }
                }
            }
            val openDocumentPhotos: (() -> Unit)? =
                if (photosDownloadEnabled && activeCargoNumber.isNotBlank()) {
                    { component.requestOpenDocumentPhotos(activeCargoNumber) }
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
                CargoDetailsSheet(
                    cargo = cargo,
                    onClose = onNestedBack,
                    onOpenBaseDocument = onOpenBaseDocument,
                    documentPhotoCount = if (photosDownloadEnabled) documentPhotoCount else 0,
                    isDocumentPhotoCountLoading = photosDownloadEnabled && isDocumentPhotoCountUiLoading,
                    onOpenDocumentPhotos = openDocumentPhotos,
                )
            }
        },

        // Full-screen filter screen (not dialog)
        filterScreen = { current, onDismiss, onApply ->
            RefineScreen(
                current = current,
                onBack = onDismiss,
                messageForUser = s("complaints_korrektno_rabotaet_tolko_sortirovka_po_date_ostalnye"),
                orderByOptions = Refiner.OrderBy.allForUiExceptDateLastModification,
                sections = setOf(
                    RefineSection.STATUS,
                    RefineSection.FILTER_VAL,
                    RefineSection.ORDER,
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
                        placeholder = { Text(s("cargo_poisk_dostavki")) },
                        singleLine = true,
                        enabled = !isTopBarLoading,
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = androidx.compose.foundation.text.KeyboardActions(onSearch = { applySearch() })
                    )
                }
            }
            panel == MasterPanel.Details && linkedDocuments.isNotEmpty() -> {
                { Text(linkedDocuments.last().linkTabLabel()) }
            }
            else -> null
        },
        topBarActionsContent = { isLoadingTopBar ->
            if (panel == MasterPanel.Details && photosUploadEnabled) {
                val active = resolveActiveCargo(selectedId, resource, linkedDocuments)
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
                            searchTypeDraft = if (refineState.searchQueryType in CARGO_TOPBAR_SEARCH_OPTIONS) {
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
        topBarTopContent = if (panel == MasterPanel.Details && selectedId != null && linkedDocuments.isNotEmpty()) {
            {
                val rows = (resource as? Resource.Success)?.data.orEmpty()
                val rootCargo = rows.firstOrNull { it.guid.toString() == selectedId }
                val rootLabel = rootCargo?.let { c ->
                    linkTabCaptionForListRow(c.link, TreeRootDocumentKind.CARGO, c.number)
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
                CargoSearchTypeRow(
                    selected = searchTypeDraft,
                    onSelected = { searchTypeDraft = it }
                )
            }
        } else null,
        onDetailsBack = handleDetailsBack,
    )

    if (isResolvingBaseDocument) {
        AlertDialog(
            onDismissRequest = {},
            title = { Text(s("events_pozhaluysta_podozhdite")) },
            text = { Text(s("events_ischem_dokument_osnovanie")) },
            confirmButton = {}
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

private fun resolveActiveCargo(
    selectedId: String?,
    resource: Resource<List<CargoDto>>,
    linkedDocuments: List<TreeRootResolvedDocument>,
): CargoDto? {
    linkedDocuments.lastOrNull()?.let { linked ->
        if (linked is TreeRootResolvedDocument.Cargo) return linked.value
    }
    val list = (resource as? Resource.Success)?.data.orEmpty()
    return list.firstOrNull { it.guid.toString() == selectedId }
}


@Composable
private fun CargoSearchTypeRow(
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
                options = CARGO_TOPBAR_SEARCH_OPTIONS,
                selected = selected,
                onSelect = onSelected,
                labelFor = { it.cargoSearchLabel() }
            )
        }
    }
}

@Composable
private fun CargoListItem(
    cargo: CargoDto,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp)
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Верхняя строка: номер + (опционально) маркер изменений + статус
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    TextC(
                        text = cargo.number,
                        style = MaterialTheme.typography.titleMedium,
                        allowLinkTap = false,
                        allowLongPressCopy = true,
                    )
//                    if (isChanged) {
//                        Text(
//                            text = "новое",
//                            style = MaterialTheme.typography.labelSmall,
//                            color = NeumoColors.RainbowGreenBg
//                        )
//                    }
                }

                StatusBadge(
                    state = cargo.status,
                    styles = mapOf(
                        Pair(CargoStatus.PROPOSAL.value, StatusStyle(DefaultColors.RainbowYellowFg, Color.Black)),
                        Pair(CargoStatus.RECEIVED.value, StatusStyle(DefaultColors.RainbowBlueBg, Color.Black)),
                        Pair(CargoStatus.IN_WORK.value, StatusStyle(DefaultColors.RainbowGreenBg, Color.Black)),
                        Pair(CargoStatus.PROPOSAL_FOR_GET_CARGO.value, StatusStyle(DefaultColors.RainbowRedFg, Color.Black)),
                        Pair(CargoStatus.SENT_TO_MAIN_DEPT.value, StatusStyle(DefaultColors.RainbowVioletBg, Color.Black)),
                        Pair(CargoStatus.WAIT_FOR_LOAD_CAR_FOUND.value, StatusStyle(DefaultColors.RainbowRedBg, Color.Black)),
                    )
                )
            }

            // Основные сведения
            Column(
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                if (cargo.route.isNotBlank()) {
                    TextC(
                        text = "${cargo.route}",
                        style = MaterialTheme.typography.bodyMedium,
                        allowLinkTap = false,
                        allowLongPressCopy = false,
                    )
                }

                Text(
                    text = "Организация: ${cargo.organization}\nПодразделение: ${cargo.department}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                val counters = buildList {
                    if (cargo.orders.isNotEmpty()) add("Заказов: ${cargo.orders.size}")
                    if (cargo.products.isNotEmpty()) add("Товаров: ${cargo.products.size}")
                }.joinToString(" · ")

                if (counters.isNotEmpty()) {
                    Text(
                        text = counters,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (!cargo.comment.isNullOrBlank()) {
                    HorizontalDivider(Modifier.fillMaxWidth(), thickness = 1.dp, color = DefaultColors.NeumoHighlight)
                    TextC(
                        text = "${cargo.comment.withEscapedNewlines()}",
                        maxLines = 3,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        allowLinkTap = false,
                        allowLongPressCopy = false,
                    )
                }
            }

            // Дата в правом нижнем углу
            Text(
                modifier = Modifier.align(Alignment.End),
                text = cargo.date,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
