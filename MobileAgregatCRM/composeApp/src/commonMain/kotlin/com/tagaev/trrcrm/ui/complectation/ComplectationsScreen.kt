package com.tagaev.trrcrm.ui.complectation

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.toMutableStateList
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.sp
import com.tagaev.trrcrm.getPlatform
import com.tagaev.trrcrm.data.remote.Resource
import com.tagaev.trrcrm.domain.complectationSearchTokenFromNomenclatureCharacteristic
import com.tagaev.trrcrm.domain.displayNameRu
import com.tagaev.trrcrm.domain.Refiner
import com.tagaev.trrcrm.domain.TreeRootDocumentKind
import com.tagaev.trrcrm.domain.TreeRootResolvedDocument
import com.tagaev.trrcrm.domain.linkTabCaptionForListRow
import com.tagaev.trrcrm.domain.linkTabLabel
import com.tagaev.trrcrm.domain.stableStateKey
import com.tagaev.trrcrm.models.WorkOrderDto
import com.tagaev.trrcrm.ui.custom.SearchIconButtonWithIndicator
import com.tagaev.trrcrm.ui.master_screen.LinkedDocumentStackTabStrip
import com.tagaev.trrcrm.ui.master_screen.MasterPanel
import com.tagaev.trrcrm.ui.master_screen.MasterScreen
import com.tagaev.trrcrm.ui.master_screen.RefineSection
import com.tagaev.trrcrm.ui.master_screen.RefineScreen
import com.tagaev.trrcrm.ui.master_screen.TreeRootDocumentDetailsSheet
import com.tagaev.trrcrm.ui.master_screen.models.MessageModel
import com.tagaev.trrcrm.ui.root.LocalAppSnackbar
import com.tagaev.trrcrm.ui.permissions.CameraPermissionGate
import com.tagaev.trrcrm.ui.permissions.CameraView
import com.tagaev.trrcrm.utils.formatRelativeWorkDate
import compose.icons.FeatherIcons
import compose.icons.LineAwesomeIcons
import compose.icons.feathericons.ArrowLeft
import compose.icons.feathericons.Camera
import compose.icons.feathericons.ChevronsUp
import compose.icons.feathericons.Filter
import compose.icons.feathericons.RefreshCw
import compose.icons.feathericons.Search
import compose.icons.feathericons.X
import compose.icons.lineawesomeicons.QrcodeSolid
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

private enum class ComplectationSearchModeType {
    NAME,
    NUMBER,
    MASTER,
    KIT_CHARACTERISTIC,
}

private fun ComplectationSearchModeType.toRefineSearchType(): Refiner.SearchQueryType {
    return when (this) {
        ComplectationSearchModeType.NAME -> Refiner.SearchQueryType.TOPIC
        ComplectationSearchModeType.NUMBER -> Refiner.SearchQueryType.CODE
        ComplectationSearchModeType.MASTER -> Refiner.SearchQueryType.MASTER
        ComplectationSearchModeType.KIT_CHARACTERISTIC -> Refiner.SearchQueryType.KIT_CHARACTERISTIC
    }
}

private fun refineToComplectationSearchModeType(type: Refiner.SearchQueryType): ComplectationSearchModeType {
    return when (type) {
        Refiner.SearchQueryType.CODE -> ComplectationSearchModeType.NUMBER
        Refiner.SearchQueryType.MASTER -> ComplectationSearchModeType.MASTER
        Refiner.SearchQueryType.KIT_CHARACTERISTIC -> ComplectationSearchModeType.KIT_CHARACTERISTIC
        else -> ComplectationSearchModeType.NAME
    }
}

private fun ComplectationSearchModeType.searchFieldPlaceholder(): String =
    when (this) {
        ComplectationSearchModeType.NAME -> "Название комплекта…"
        ComplectationSearchModeType.NUMBER -> "Номер документа…"
        ComplectationSearchModeType.MASTER -> "Мастер…"
        ComplectationSearchModeType.KIT_CHARACTERISTIC -> "С/Н…"
    }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ComplectationsScreen(
    component: ComplectationComponent,
    modifier: Modifier = Modifier
) {
    val resource by component.complectations.collectAsState()
    val refineState by component.refineState.collectAsState()
    val panel by component.masterScreenPanel.collectAsState()
    val selectedId by component.selectedItemGuid.collectAsState()
    val isQrScannerOpen by component.isQrScannerOpen.collectAsState()
    val isQrLookupInProgress by component.isQrLookupInProgress.collectAsState()
    val qrLookupError by component.qrLookupError.collectAsState()

    val scope = rememberCoroutineScope()
    val showSnackbar = LocalAppSnackbar.current
    val linkedDocuments = remember { emptyList<TreeRootResolvedDocument>().toMutableStateList() }
    val stackedDetails = remember { mutableStateMapOf<String, StackedDocumentDetailsSnapshot>() }
    var isResolvingBaseDocument by rememberSaveable { mutableStateOf(false) }
    var isResolvingLinkedByCharacteristic by rememberSaveable { mutableStateOf(false) }
    var characteristicMatches by remember { mutableStateOf<List<WorkOrderDto>>(emptyList()) }
    var isSearchMode by rememberSaveable { mutableStateOf(false) }
    var searchQueryDraft by rememberSaveable { mutableStateOf(refineState.searchQuery) }
    var searchTypeDraft by rememberSaveable {
        mutableStateOf(refineToComplectationSearchModeType(refineState.searchQueryType))
    }
    val isTopBarLoading = resource is Resource.Loading ||
            (resource as? Resource.Success<*>)?.additionalLoading == true

    LaunchedEffect(refineState.searchQuery, refineState.searchQueryType, isSearchMode) {
        if (!isSearchMode) {
            searchQueryDraft = refineState.searchQuery
            searchTypeDraft = refineToComplectationSearchModeType(refineState.searchQueryType)
        }
    }
    LaunchedEffect(selectedId) {
        linkedDocuments.clear()
        isResolvingBaseDocument = false
        isResolvingLinkedByCharacteristic = false
        characteristicMatches = emptyList()
        stackedDetails.clear()
    }

    val onNomenclatureCharacteristicSearch: (String) -> Unit = { rawCharacteristic ->
        val token = complectationSearchTokenFromNomenclatureCharacteristic(rawCharacteristic)
        if (token.isBlank()) {
            showSnackbar("Укажите другое значение характеристики — для поиска нет сырого кода (например ЦБ153214)")
        } else {
            scope.launch {
                isResolvingLinkedByCharacteristic = true
                try {
                    when (val res = component.searchComplectationsByKitCharacteristicToken(token)) {
                        is Resource.Success -> {
                            val list = res.data.orEmpty()
                            when {
                                list.isEmpty() -> showSnackbar("Комплектации не найдены")
                                list.size == 1 -> linkedDocuments.add(
                                    TreeRootResolvedDocument.Complectation(list.first())
                                )
                                else -> {
                                    characteristicMatches = list
                                }
                            }
                        }
                        is Resource.Error -> showSnackbar(
                            res.causes ?: res.exception?.message
                            ?: "Ошибка поиска комплектации"
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
                searchQueryType = searchTypeDraft.toRefineSearchType()
            )
        )
    }
    val hideSearchForm: () -> Unit = {
        isSearchMode = false
        searchQueryDraft = refineState.searchQuery
        searchTypeDraft = refineToComplectationSearchModeType(refineState.searchQueryType)
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
    if (isQrScannerOpen) {
        ComplectationQrScannerView(
            isResolving = isQrLookupInProgress,
            errorText = qrLookupError,
            onBack = { component.closeQrScanner() },
            onScanned = { component.onQrScanned(it) },
            onErrorConsumed = { component.consumeQrLookupError() }
        )
        return
    }

    MasterScreen(
        title = "Комплектация",
        resource = resource,
        errorText = "Не удалось загрузить комплектации",
        notFoundText = "Комплектации не найдены",
        refineState = refineState,
        onRefresh = { component.fullRefresh() },
        onLoadMore = { component.loadMore() },
        onFilterChanged = { component.setRefineState(it) },

        itemId = { it.guid.toString() },
        isItemChanged = { old, new -> old.messages.size != new.messages.size },

        listItem = { order, isChanged, onClick ->
            ComplectationCard(
                order = order,
                isChanged = isChanged,
                onClick = onClick
            )
        },

        // Full-screen details content (not bottom-sheet)
        detailsContent = { order, onClose ->
            val currentLinked = linkedDocuments.lastOrNull()
            val detailsKey: String = if (currentLinked != null) {
                currentLinked.stableStateKey()
            } else {
                complectationRootListStateKey(order)
            }
            val snapshot: StackedDocumentDetailsSnapshot = stackedDetails[detailsKey] ?: StackedDocumentDetailsSnapshot()
            val detailsScroll: ScrollState = remember(detailsKey) {
                ScrollState(snapshot.scroll)
            }
            LaunchedEffect(detailsKey) {
                snapshotFlow { detailsScroll.value }
                    .distinctUntilChanged()
                    .collect { v ->
                        val prev = stackedDetails[detailsKey] ?: StackedDocumentDetailsSnapshot()
                        if (v != prev.scroll) {
                            stackedDetails[detailsKey] = prev.withScroll(v)
                        }
                    }
            }
            val onStackedSnapshot: (StackedDocumentDetailsSnapshot) -> Unit = { s ->
                stackedDetails[detailsKey] = s.withScroll(detailsScroll.value)
            }
            val complectationStackedUi: ComplectationTreeStackedUi = ComplectationTreeStackedUi(
                detailsScroll = detailsScroll,
                detailsSnapshot = stackedDetails[detailsKey] ?: StackedDocumentDetailsSnapshot(),
                onDetailsSnapshot = onStackedSnapshot
            )
            val onOpenBaseDocument: (String) -> Unit = { rawBaseDocument ->
                scope.launch {
                    isResolvingBaseDocument = true
                    try {
                        when (val resolved = runCatching { component.resolveBaseDocument(rawBaseDocument) }
                            .getOrElse { e -> Resource.Error(causes = e.message ?: "Ошибка поиска документа") }) {
                            is Resource.Success -> linkedDocuments.add(resolved.data)
                            is Resource.Error -> showSnackbar(resolved.causes ?: "Документ-основание не найден")
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
            if (currentLinked != null) {
                TreeRootDocumentDetailsSheet(
                    document = currentLinked,
                    onBack = onNestedBack,
                    onOpenBaseDocument = onOpenBaseDocument,
                    complectationStacked = complectationStackedUi,
                    onNomenclatureCharacteristicSearch = onNomenclatureCharacteristicSearch
                )
            } else {
                ComplectationDetailsSheet(
                    order = order,
                    onBack = onNestedBack,
                    stackedDetailsSnapshot = complectationStackedUi.detailsSnapshot,
                    onStackedDetailsSnapshotChange = complectationStackedUi.onDetailsSnapshot,
                    detailsScrollState = complectationStackedUi.detailsScroll,
                    onOpenBaseDocument = onOpenBaseDocument,
                    onSendMessage = { message, onResult ->
                        val number = order.number.orEmpty()
                        val date = order.date.orEmpty()
                        scope.launch {
                            val err = component.sendMessage(number, date, message)
                            if (err == null) {
                                component.addLocalMessage(order.guid.toString(), message = MessageModel(author = "я", text = message))
                            }
                            onResult(err)
                        }
                    },
                    onNomenclatureCharacteristicSearch = onNomenclatureCharacteristicSearch
                )
            }
        },

        // Full-screen filter screen (not dialog)
        filterScreen = { current, onDismiss, onApply ->
            RefineScreen(
                current = current,
                onBack = onDismiss,
                sections = setOf(RefineSection.FILTER_VAL),
                onApply = { newState ->
                    onApply(
                        newState.copy(
                            status = Refiner.Status.OFF,
                            orderBy = refineState.orderBy,
                            orderDir = refineState.orderDir,
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
                        Icon(FeatherIcons.ChevronsUp, contentDescription = "Скрыть поиск")
                    }
                    IconButton(
                        onClick = clearSearchAndClose,
                        enabled = !isTopBarLoading
                    ) {
                        Icon(FeatherIcons.X, contentDescription = "Очистить и закрыть поиск")
                    }
                }
            }
        } else {
            null
        },
        topBarTitleContent = when {
            panel == MasterPanel.List && isSearchMode -> {
                {
                    OutlinedTextField(
                        value = searchQueryDraft,
                        onValueChange = { searchQueryDraft = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 6.dp),
                        placeholder = { Text(searchTypeDraft.searchFieldPlaceholder()) },
                        singleLine = true,
                        enabled = !isTopBarLoading,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(onSearch = { applySearch() })
                    )
                }
            }
            panel == MasterPanel.Details && linkedDocuments.isNotEmpty() -> {
                { Text(linkedDocuments.last().linkTabLabel()) }
            }
            else -> null
        },
        topBarActionsContent = { isLoadingTopBar ->
            if (panel == MasterPanel.List) {
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
                            Icon(FeatherIcons.Search, contentDescription = "Искать")
                        }
                    }
                } else {
                    IconButton(onClick = { component.openQrScanner() }) {
                        Icon(LineAwesomeIcons.QrcodeSolid, contentDescription = "Сканировать QR")
                    }
                    IconButton(onClick = { component.changePanel(MasterPanel.Filter) }) {
                        Icon(FeatherIcons.Filter, contentDescription = "Фильтр")
                    }
                    SearchIconButtonWithIndicator(
                        showIndicator = refineState.searchQuery.isNotBlank(),
                        enabled = !isLoadingTopBar,
                        onClick = {
                            searchQueryDraft = refineState.searchQuery
                            searchTypeDraft = refineToComplectationSearchModeType(refineState.searchQueryType)
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
                            Icon(FeatherIcons.RefreshCw, contentDescription = "Обновить")
                        }
                    }
                }
            }
        },
        topBarTopContent = if (panel == MasterPanel.Details && selectedId != null && linkedDocuments.isNotEmpty()) {
            {
                val rows = (resource as? Resource.Success)?.data.orEmpty()
                val rootOrder = rows.firstOrNull { it.guid.toString() == selectedId }
                val rootLabel = rootOrder?.let { o ->
                    linkTabCaptionForListRow(o.link, TreeRootDocumentKind.COMPLECTATION, o.number)
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
                ComplectationSearchTypeRow(
                    selected = searchTypeDraft,
                    onSelected = { searchTypeDraft = it }
                )
            }
        } else {
            null
        },
        onDetailsBack = handleDetailsBack,
        modifier = modifier
    )

    if (isResolvingBaseDocument || isResolvingLinkedByCharacteristic) {
        AlertDialog(
            onDismissRequest = {},
            title = { Text("Пожалуйста, подождите") },
            text = { Text("Поиск документа…") },
            confirmButton = {}
        )
    }

    if (characteristicMatches.size >= 2) {
        AlertDialog(
            onDismissRequest = { characteristicMatches = emptyList() },
            title = { Text("Найдено несколько комплектаций") },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 320.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    characteristicMatches.forEach { item ->
                        val title = item.link?.takeIf { it.isNotBlank() }
                            ?: item.number?.takeIf { it.isNotBlank() }
                            ?: "Без номера"
                        val subtitle = item.complectationCharacteristic?.takeIf { it.isNotBlank() }
                            ?: "—"
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
                    Text("Отмена")
                }
            }
        )
    }
}

@Composable
private fun ComplectationSearchTypeRow(
    selected: ComplectationSearchModeType,
    onSelected: (ComplectationSearchModeType) -> Unit
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
                text = "Поиск по:",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                FilterChip(
                    selected = selected == ComplectationSearchModeType.KIT_CHARACTERISTIC,
                    onClick = { onSelected(ComplectationSearchModeType.KIT_CHARACTERISTIC) },
                    label = {
                        Text(
                            "По с/н",
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                )
                FilterChip(
                    selected = selected == ComplectationSearchModeType.NAME,
                    onClick = { onSelected(ComplectationSearchModeType.NAME) },
                    label = { Text("По названию") }
                )
                FilterChip(
                    selected = selected == ComplectationSearchModeType.NUMBER,
                    onClick = { onSelected(ComplectationSearchModeType.NUMBER) },
                    label = { Text("По номеру") }
                )
                FilterChip(
                    selected = selected == ComplectationSearchModeType.MASTER,
                    onClick = { onSelected(ComplectationSearchModeType.MASTER) },
                    label = { Text("По мастеру") }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ComplectationQrScannerView(
    isResolving: Boolean,
    errorText: String?,
    onBack: () -> Unit,
    onScanned: (String) -> Unit,
    onErrorConsumed: () -> Unit
) {
    val isDesktopTarget = remember { getPlatform().name.startsWith("Desktop") }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(errorText) {
        if (!errorText.isNullOrBlank()) {
            snackbarHostState.showSnackbar(errorText)
            onErrorConsumed()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(FeatherIcons.ArrowLeft, contentDescription = "Назад")
                    }
                },
                title = { Text("Сканер комплектации") }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(Color.Black)
            ) {
                if (isDesktopTarget) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "QR-сканер пока недоступен на desktop",
                            style = MaterialTheme.typography.bodyLarge,
                            color = Color.White
                        )
                    }
                } else {
                    CameraPermissionGate(rationaleText = "Для сканирования нужен доступ к камере.") {
                        CameraView(
                            decodedString = { decodedString ->
                                if (!isResolving) onScanned(decodedString)
                            },
                            autoStart = true
                        )
                    }
                }
            }

            if (isResolving) {
                Surface(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(16.dp),
                    shape = MaterialTheme.shapes.medium,
                    tonalElevation = 6.dp
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp
                        )
                        Spacer(Modifier.width(10.dp))
                        Text("Поиск комплектации...")
                    }
                }
            }
        }
    }
}


@Composable
private fun ComplectationCard(
    order: WorkOrderDto,
    isChanged: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val numberText = normalizeSingleLine(order.number).orEmpty().ifBlank { "Без номера" }
    val branchText = normalizeSingleLine(order.branch).orEmpty()
    val characteristicText = normalizeSingleLine(order.complectationCharacteristic).orEmpty().ifBlank { "—" }
    val statusText = normalizeSingleLine(order.status)
    val documentAmountText =
        normalizeSingleLine(order.documentAmount)?.trim().orEmpty().ifBlank { "—" }
    val kitText = normalizeSingleLine(order.complectationKit).orEmpty().ifBlank { "—" }
    val createdText = normalizeSingleLine(order.date)?.let { "созд. $it" }
    val lastMessageText = order.messages.lastOrNull()?.let { "изм. ${formatRelativeWorkDate(it.workDate)}" }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 3.dp)
            .clip(MaterialTheme.shapes.medium)
            .clickable(onClick = onClick),
        shape = MaterialTheme.shapes.medium,
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 8.dp, vertical = 6.dp)
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
                    Text(
                        text = "№ $numberText   $characteristicText",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 3,
                        overflow = TextOverflow.Clip,
                        softWrap = true
                    )
                    Spacer(Modifier.height(1.dp))
//                    Text(
//                        text = "ХарактеристикаКомплекта: $characteristicText",
//                        modifier = Modifier.fillMaxWidth(),
//                        style = MaterialTheme.typography.labelSmall,
//                        color = MaterialTheme.colorScheme.onSurfaceVariant,
//                        maxLines = 2,
//                        overflow = TextOverflow.Ellipsis,
//                        softWrap = true
//                    )
                    if (branchText.isNotBlank()) {
                        Spacer(Modifier.height(1.dp))
                        Text(
                            text = branchText,
                            modifier = Modifier.fillMaxWidth(),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2,
                            overflow = TextOverflow.Clip,
                            softWrap = true
                        )
                    }
                }

                ComplectationStatusBadge(statusText)
            }

            Spacer(Modifier.height(3.dp))
            ComplectationMetaRow(
                label = "Состояние",
                value = statusText.orEmpty().ifBlank { "—" }
            )
            ComplectationMetaRow(label = "Сумма документа", value = documentAmountText)
            ComplectationMetaRow(label = "Комплект", value = kitText)
            ComplectationMetaRow(
                label = "Комментарий",
                value = normalizeSingleLine(order.comment).orEmpty().ifBlank { "—" }
            )

            Spacer(Modifier.height(3.dp))
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(1.dp)
            ) {
                createdText?.let {
                    Text(
                        text = it,
                        modifier = Modifier.fillMaxWidth(),
                        style = TextStyle(fontSize = 9.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 6,
                        overflow = TextOverflow.Clip,
                        softWrap = true
                    )
                }
                lastMessageText?.let {
                    Text(
                        text = it,
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
                        text = "Сообщений нет",
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
private fun ComplectationMetaRow(
    label: String,
    value: String
) {
    val display = normalizeSingleLine(value).orEmpty().ifBlank { "—" }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 1.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .padding(end = 6.dp, top = 1.dp)
                .widthIn(max = 118.dp),
            maxLines = 4,
            overflow = TextOverflow.Clip,
            softWrap = true
        )
        Text(
            text = display,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium,
            modifier = Modifier
                .weight(1f)
                .padding(start = 6.dp),
            textAlign = TextAlign.Start,
            maxLines = 8,
            overflow = TextOverflow.Clip,
            softWrap = true
        )
    }
}

private fun normalizeSingleLine(value: String?): String? {
    if (value == null) return null
    return value
        .replace('\r', ' ')
        .replace('\n', ' ')
        .replace(Regex("\\s+"), " ")
        .trim()
}

@Composable
fun ComplectationStatusBadge(status: String?) {
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
