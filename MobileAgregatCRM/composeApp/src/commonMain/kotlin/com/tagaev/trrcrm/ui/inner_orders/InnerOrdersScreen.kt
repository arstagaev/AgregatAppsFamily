package com.tagaev.trrcrm.ui.inner_orders

import com.tagaev.trrcrm.ui.i18n.s

import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.toMutableStateList
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.max
import androidx.compose.ui.unit.sp
import com.tagaev.trrcrm.data.remote.Resource
import com.tagaev.trrcrm.data.remote.friendlyError
import com.tagaev.trrcrm.domain.OptionChipsScrollingRow
import com.tagaev.trrcrm.domain.Refiner
import com.tagaev.trrcrm.domain.TreeRootDocumentKind
import com.tagaev.trrcrm.domain.TreeRootResolvedDocument
import com.tagaev.trrcrm.domain.complectationSearchTokenFromNomenclatureCharacteristic
import com.tagaev.trrcrm.domain.linkTabCaptionForListRow
import com.tagaev.trrcrm.domain.linkTabLabel
import com.tagaev.trrcrm.models.ImageDocumentType
import com.tagaev.trrcrm.models.DocumentUploadPeriod
import com.tagaev.trrcrm.models.InnerOrderDto
import com.tagaev.trrcrm.models.WorkOrderDto
import com.tagaev.trrcrm.ui.camera.DocumentCameraScreen
import com.tagaev.trrcrm.ui.complectation.ComplectationAddPhotoTopBarAction
import com.tagaev.trrcrm.ui.complectation.ComplectationOpenPhotosButton
import com.tagaev.trrcrm.ui.complectation.DocumentPhotosViewerScreen
import com.tagaev.trrcrm.ui.custom.SearchIconButtonWithIndicator
import com.tagaev.trrcrm.ui.custom.StatusBadge
import com.tagaev.trrcrm.ui.custom.StatusStyle
import com.tagaev.trrcrm.ui.custom.TextC
import com.tagaev.trrcrm.ui.custom.UniversalCardItem
import com.tagaev.trrcrm.ui.master_screen.LinkedDocumentStackTabStrip
import com.tagaev.trrcrm.ui.master_screen.MasterPanel
import com.tagaev.trrcrm.ui.master_screen.MasterScreen
import com.tagaev.trrcrm.ui.master_screen.RefineSection
import com.tagaev.trrcrm.ui.master_screen.RefineScreen
import com.tagaev.trrcrm.ui.master_screen.TreeRootDocumentDetailsSheet
import com.tagaev.trrcrm.ui.master_screen.models.MessageModel
import com.tagaev.trrcrm.ui.root.LocalAppSnackbar
import com.tagaev.trrcrm.ui.style.DefaultColors
import com.tagaev.trrcrm.ui.work_order.WorkOrderDetailsSheet
import com.tagaev.trrcrm.utils.formatRelativeWorkDate
import compose.icons.FeatherIcons
import compose.icons.feathericons.Camera
import compose.icons.feathericons.ChevronsUp
import compose.icons.feathericons.Filter
import compose.icons.feathericons.RefreshCw
import compose.icons.feathericons.Search
import compose.icons.feathericons.X
import kotlinx.coroutines.launch
import org.jetbrains.compose.ui.tooling.preview.Preview

private val INNER_ORDERS_TOPBAR_SEARCH_OPTIONS = listOf(
    Refiner.SearchQueryType.CODE,
    Refiner.SearchQueryType.TOPIC,
    Refiner.SearchQueryType.AUTHOR,
    Refiner.SearchQueryType.COUNTERPARTY,
    Refiner.SearchQueryType.AUTO
)

private fun Refiner.SearchQueryType.innerOrdersSearchLabel(): String = when (this) {
    Refiner.SearchQueryType.CODE -> s("filter_nomer")
    Refiner.SearchQueryType.TOPIC -> s("filter_tema")
    Refiner.SearchQueryType.AUTHOR -> s("events_avtor")
    Refiner.SearchQueryType.COUNTERPARTY -> s("events_kontragent")
    Refiner.SearchQueryType.AUTO -> "Автомобиль"
    Refiner.SearchQueryType.MANAGER -> s("filter_menedzher")
    Refiner.SearchQueryType.MASTER -> s("filter_master")
    Refiner.SearchQueryType.KIT_CHARACTERISTIC -> "Хар. комплекта"
    Refiner.SearchQueryType.LICENSE_PLATE -> "Госномер"
    Refiner.SearchQueryType.VIN_NUMBER -> "VIN"
    Refiner.SearchQueryType.FIX_TYPE -> "Вид ремонта"
    Refiner.SearchQueryType.CLIENT -> "Заказчик"
    Refiner.SearchQueryType.ROUTE -> "Маршрут"
    Refiner.SearchQueryType.CARRIER -> "Перевозчик"
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
fun InnerOrdersScreen(component: InnerOrdersComponent, modifier: Modifier = Modifier) {
    val resource by component.innerOrders.collectAsState()
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
    val cameraUploadPeriod by component.cameraUploadPeriod.collectAsState()
    val documentPhotoCount by component.documentPhotoCount.collectAsState()
    val isDocumentPhotoCountLoading by component.isDocumentPhotoCountLoading.collectAsState()
    val documentPhotoCountLoaded by component.documentPhotoCountLoaded.collectAsState()
    val isDocumentPhotoCountUiLoading = isDocumentPhotoCountLoading || !documentPhotoCountLoaded
    val isPhotosViewerOpen by component.isPhotosViewerOpen.collectAsState()
    val photosViewerDocumentNumber by component.photosViewerDocumentNumber.collectAsState()
    var photosUploadEnabled by androidx.compose.runtime.remember { mutableStateOf(false) }
    var photosDownloadEnabled by androidx.compose.runtime.remember { mutableStateOf(false) }
    val cameraErrorSnackbarHostState = androidx.compose.runtime.remember { SnackbarHostState() }
    var cameraSnackbarIsError by androidx.compose.runtime.remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()
    val showSnackbar = LocalAppSnackbar.current
    val linkedDocuments = androidx.compose.runtime.remember { emptyList<TreeRootResolvedDocument>().toMutableStateList() }
    var characteristicMatches by androidx.compose.runtime.remember { mutableStateOf<List<WorkOrderDto>>(emptyList()) }
    var isResolvingBaseDocument by rememberSaveable { mutableStateOf(false) }
    var isResolvingLinkedByCharacteristic by rememberSaveable { mutableStateOf(false) }

    androidx.compose.runtime.LaunchedEffect(Unit) {
        photosUploadEnabled = component.isPhotosUploadEnabled()
        photosDownloadEnabled = component.isPhotosDownloadEnabled()
    }
    androidx.compose.runtime.LaunchedEffect(cameraPrecheckError) {
        val error = cameraPrecheckError
        if (!error.isNullOrBlank()) {
            cameraSnackbarIsError = true
            cameraErrorSnackbarHostState.showSnackbar(
                com.tagaev.trrcrm.data.remote.userFacingMessage(error, error),
            )
            component.consumeCameraPrecheckError()
        }
    }

    androidx.compose.runtime.LaunchedEffect(refineState.searchQuery, refineState.searchQueryType, isSearchMode) {
        if (!isSearchMode) {
            searchQueryDraft = refineState.searchQuery
            searchTypeDraft = if (refineState.searchQueryType in INNER_ORDERS_TOPBAR_SEARCH_OPTIONS) {
                refineState.searchQueryType
            } else {
                Refiner.SearchQueryType.CODE
            }
        }
    }
    androidx.compose.runtime.LaunchedEffect(selectedId) {
        linkedDocuments.clear()
        characteristicMatches = emptyList()
        isResolvingBaseDocument = false
        isResolvingLinkedByCharacteristic = false
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
        searchTypeDraft = if (refineState.searchQueryType in INNER_ORDERS_TOPBAR_SEARCH_OPTIONS) {
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
        val uploadPeriod = cameraUploadPeriod
        if (number != null && uploadPeriod != null) {
            DocumentCameraScreen(
                documentNumber = number,
                uploadPeriod = uploadPeriod,
                title = s("complectation_kamera_number", number),
                documentType = ImageDocumentType.InnerOrder,
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
                documentType = ImageDocumentType.InnerOrder,
                onBack = component::closePhotosViewer,
            )
        }
        return
    }

    Box(modifier = modifier.fillMaxSize()) {
    MasterScreen(
        title = s("inner_order_vnutrennie_zakazy"),
        resource = resource,
        errorText = s("inner_order_ne_udalos_zagruzit_vnutrennie_zakazy"),
        notFoundText = s("inner_order_vnutrennie_zakazy_ne_naydeny"),
        refineState = refineState,
        onRefresh = { component.fullRefresh() },
        onLoadMore = { component.loadMore() },
        onFilterChanged = { component.setRefineState(it) },

        itemId = { it.guid.toString() },
        isItemChanged = { old, new -> old.messages.size != new.messages.size },

        listItem = { innerOrder, isChanged, onClick ->
            UniversalCardItem(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                onClick = onClick,
                title = innerOrder.number ?: "",
                allowTitleLongPressCopy = true,
                subtitle = "${innerOrder.branch} - ${innerOrder.organization}",

                topRightPrimary = {
                    val status = innerOrder.state
                    if (!status.isNullOrBlank()) {
                        StatusBadge(
                            state = status,
                            styles = mapOf(
                                // Colors are aligned with 1C screenshot palette
                                "Заявка"                      to StatusStyle(DefaultColors.RainbowSkyBg,        DefaultColors.RainbowSkyFg),
                                "В работе"                    to StatusStyle(DefaultColors.RainbowMintBg,       DefaultColors.RainbowMintFg),
                                s("complectation_vypolneno")                   to StatusStyle(DefaultColors.RainbowBrightPurpleBg, DefaultColors.RainbowBrightPurpleFg),
                                "В пути на основной склад"    to StatusStyle(DefaultColors.RainbowSoftAmberBg,  DefaultColors.RainbowSoftAmberFg),
                                s("inner_order_gotovo_k_otgruzke")           to StatusStyle(DefaultColors.RainbowStrongOrangeBg, DefaultColors.RainbowStrongOrangeFg),
                                "Заказано"                    to StatusStyle(DefaultColors.RainbowNeonMagentaBg, DefaultColors.RainbowNeonMagentaFg),
                                "Дефектовка"                  to StatusStyle(DefaultColors.RainbowGreyBg,       DefaultColors.RainbowGreyFg),
                                "Дефектовка (выполнено)"      to StatusStyle(DefaultColors.RainbowOliveBg,      DefaultColors.RainbowOliveFg),
                                "Сборка"                      to StatusStyle(DefaultColors.RainbowNeonGreenBg,  DefaultColors.RainbowNeonGreenFg),
                                "Отправлено получателю"       to StatusStyle(DefaultColors.RainbowAquaBg,       DefaultColors.RainbowAquaFg),
                                "Получено"                    to StatusStyle(DefaultColors.RainbowRoyalBlueBg,  DefaultColors.RainbowRoyalBlueFg),
                                "ОТКАЗ"                       to StatusStyle(DefaultColors.RainbowDarkTealBg,   DefaultColors.RainbowDarkTealFg),
                                "Получено на основном складе" to StatusStyle(DefaultColors.RainbowSkyBg,        DefaultColors.RainbowSkyFg),
                                "Передано в производстве"     to StatusStyle(DefaultColors.RainbowSandBg,       DefaultColors.RainbowSandFg),
                            )
                        )
                    }
                },
                topRightSecondary = {
//                    val priority = innerOrder.priority
//                    if (!priority.isNullOrBlank()) {
//                        StatusBadge(
//                            state = priority,
//                            styles = mapOf(
//                                Pair("Высокая", StatusStyle(background = DefaultColors.RainbowRedBg, foreground =DefaultColors.RainbowRedFg)),
//                                Pair("Средняя", StatusStyle(background = DefaultColors.RainbowOrangeBg, foreground =DefaultColors.RainbowOrangeFg)),
//                                Pair("Низкая", StatusStyle(background = DefaultColors.RainbowGreenBg, foreground =DefaultColors.RainbowGreenFg)),
//                            )
//                        )
//                    }
                },
                // Middle A (big)
                bigText1 = "${innerOrder.operationType} ",
                bigText2 = "${innerOrder.carText}",
                bigText3 = "${innerOrder.documentAmount} ${innerOrder.currency}",

                // Middle B (two medium texts)
//                midBText1 = "Организация: ООО САМАРА АКПП",
//                midBText2 = "Подразделение: Сургут",
//
//                // Middle C (two medium texts)
//                midCText1 = "Ответственный: Голиков Максим",
//                midCText2 = "Источник: Яндекс",
//                complaint.date?.let {
//                    Text(
//                        text = "созд. ${it}",
//                        style = TextStyle(fontSize = 9.sp),
//                        color = MaterialTheme.colorScheme.onSurfaceVariant
//                    )
//                }
//                        complaint.messages.lastOrNull()?.let {
//                    Text(
//                        text = "изм. ${formatRelativeWorkDate(it.workDate)}",
//                        style = TextStyle(fontSize = 9.sp),
//                        color = MaterialTheme.colorScheme.onSurfaceVariant
//                    )
//                }
//                if (complaint.messages.lastOrNull() == null) {
//                    Text(
//                        text = s("work_order_soobscheniy_net"),
//                        style = TextStyle(fontSize = 9.sp),
//                        color = MaterialTheme.colorScheme.onSurfaceVariant
//                    )
//                }
                // Bottom
                bottomLeftText = "созд. ${innerOrder.creationDate}",
                bottomRightText = "изм. ${formatRelativeWorkDate(innerOrder.date)}"
            )
            ////
        },

        // Full-screen details content (not bottom-sheet)
        detailsContent = { complaint, onClose ->
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
            val activeInnerOrderNumber = (currentLinked as? TreeRootResolvedDocument.InnerOrder)?.value?.number
                ?: complaint.number
            androidx.compose.runtime.LaunchedEffect(activeInnerOrderNumber, photosDownloadEnabled) {
                if (photosDownloadEnabled) {
                    activeInnerOrderNumber?.let { component.refreshDocumentPhotoCount(it) }
                }
            }
            val openDocumentPhotos: (() -> Unit)? =
                if (photosDownloadEnabled && !activeInnerOrderNumber.isNullOrBlank()) {
                    { component.requestOpenDocumentPhotos(activeInnerOrderNumber) }
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
                InnerOrderDetailsSheetWithMessages(
                    complaint = complaint,
                    onBack = onNestedBack,
                    onOpenBaseDocument = onOpenBaseDocument,
                    onNomenclatureCharacteristicSearch = onNomenclatureCharacteristicSearch,
                    documentPhotoCount = if (photosDownloadEnabled) documentPhotoCount else 0,
                    isDocumentPhotoCountLoading = photosDownloadEnabled && isDocumentPhotoCountUiLoading,
                    onOpenDocumentPhotos = openDocumentPhotos,
                    onSendMessage = { message, onResult ->
                        val number = complaint.number.orEmpty()
                        val date = complaint.date.orEmpty()
                        scope.launch {
                            val err = component.sendMessage(number, date, message)
                            if (err == null) {
                                component.addLocalMessage(complaint.guid.toString(), message = MessageModel(author = "я", text = message))
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
                        placeholder = { Text(s("inner_order_poisk_vnutrennego_zakaza")) },
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
                val active = resolveActiveInnerOrder(selectedId, resource, linkedDocuments)
                val cameraNumber = active?.number.orEmpty()
                if (active != null && cameraNumber.isNotBlank()) {
                    ComplectationAddPhotoTopBarAction(
                        enabled = !isCameraPrecheckInProgress,
                        onClick = { component.requestOpenCamera(cameraNumber, DocumentUploadPeriod.from(active.creationDate)) },
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
                            searchTypeDraft = if (refineState.searchQueryType in INNER_ORDERS_TOPBAR_SEARCH_OPTIONS) {
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
                val root = rows.firstOrNull { it.guid.toString() == selectedId }
                val rootLabel = root?.let { o ->
                    linkTabCaptionForListRow(o.link, TreeRootDocumentKind.INNER_ORDER, o.number)
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
                InnerOrdersSearchTypeRow(
                    selected = searchTypeDraft,
                    onSelected = { searchTypeDraft = it }
                )
            }
        } else null,
        onDetailsBack = handleDetailsBack,
    )

    if (isResolvingBaseDocument || isResolvingLinkedByCharacteristic) {
        AlertDialog(
            onDismissRequest = {},
            title = { Text(s("events_pozhaluysta_podozhdite")) },
            text = { Text(s("events_ischem_dokument_osnovanie")) },
            confirmButton = {}
        )
    }

    if (characteristicMatches.size >= 2) {
        AlertDialog(
            onDismissRequest = { characteristicMatches = emptyList() },
            title = { Text(s("complectation_naydeno_neskolko_komplektatsiy")) },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(320.dp)
                        .verticalScroll(androidx.compose.foundation.rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
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
                androidx.compose.material3.TextButton(onClick = { characteristicMatches = emptyList() }) {
                    Text(s("settings_otmena"))
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

private fun resolveActiveInnerOrder(
    selectedId: String?,
    resource: Resource<List<InnerOrderDto>>,
    linkedDocuments: List<TreeRootResolvedDocument>,
): InnerOrderDto? {
    linkedDocuments.lastOrNull()?.let { linked ->
        if (linked is TreeRootResolvedDocument.InnerOrder) return linked.value
    }
    val list = (resource as? Resource.Success)?.data.orEmpty()
    return list.firstOrNull { it.guid.toString() == selectedId }
}

@Composable
private fun InnerOrdersSearchTypeRow(
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
                options = INNER_ORDERS_TOPBAR_SEARCH_OPTIONS,
                selected = selected,
                onSelect = onSelected,
                labelFor = { it.innerOrdersSearchLabel() }
            )
        }
    }
}
