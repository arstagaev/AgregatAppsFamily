package com.tagaev.trrcrm.ui.work_order

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
import com.tagaev.trrcrm.models.WorkOrderDto
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
        Refiner.SearchQueryType.CODE -> "Номер"
        Refiner.SearchQueryType.AUTO -> "Автомобиль"
        Refiner.SearchQueryType.LICENSE_PLATE -> "Госномер"
        Refiner.SearchQueryType.VIN_NUMBER -> "VIN"
        Refiner.SearchQueryType.FIX_TYPE -> "Вид ремонта"
        Refiner.SearchQueryType.CLIENT -> "Заказчик"
        Refiner.SearchQueryType.MASTER -> "Мастер"
        Refiner.SearchQueryType.KIT_CHARACTERISTIC -> "Хар. комплекта"
        Refiner.SearchQueryType.TOPIC -> "Тема"
        Refiner.SearchQueryType.AUTHOR -> "Автор"
        Refiner.SearchQueryType.MANAGER -> "Менеджер"
        Refiner.SearchQueryType.COUNTERPARTY -> "Контрагент"
        Refiner.SearchQueryType.ROUTE -> "Маршрут"
        Refiner.SearchQueryType.CARRIER -> "Перевозчик"
        Refiner.SearchQueryType.SUBJECT_MATTER -> "Суть обращения"
        Refiner.SearchQueryType.PHONE -> "Телефон"
        Refiner.SearchQueryType.REPAIR_TEMPLATE_MODEL,
        Refiner.SearchQueryType.REPAIR_TEMPLATE_NAME,
        Refiner.SearchQueryType.REPAIR_TEMPLATE_CODE,
        Refiner.SearchQueryType.REPAIR_TEMPLATE_YEAR_FROM,
        Refiner.SearchQueryType.REPAIR_TEMPLATE_YEAR_TO,
        Refiner.SearchQueryType.REPAIR_TEMPLATE_TRANSMISSION,
        Refiner.SearchQueryType.REPAIR_TEMPLATE_ENGINE,
        Refiner.SearchQueryType.REPAIR_TEMPLATE_REPAIR_KIND,
        -> "Калькуляция"
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

    val scope = rememberCoroutineScope()
    val showSnackbar = LocalAppSnackbar.current
    val linkedDocuments = remember { emptyList<TreeRootResolvedDocument>().toMutableStateList() }
    var characteristicMatches by remember { mutableStateOf<List<WorkOrderDto>>(emptyList()) }
    var isResolvingBaseDocument by rememberSaveable { mutableStateOf(false) }
    var isResolvingLinkedByCharacteristic by rememberSaveable { mutableStateOf(false) }
    var isSearchMode by rememberSaveable { mutableStateOf(false) }
    var searchQueryDraft by rememberSaveable { mutableStateOf(refineState.searchQuery) }
    var searchTypeDraft by rememberSaveable { mutableStateOf(refineState.searchQueryType) }
    val isTopBarLoading = resource is Resource.Loading ||
            (resource as? Resource.Success<*>)?.additionalLoading == true

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
                                list.size == 1 -> linkedDocuments.add(TreeRootResolvedDocument.Complectation(list.first()))
                                else -> characteristicMatches = list
                            }
                        }
                        is Resource.Error -> showSnackbar(
                            res.causes ?: friendlyError(res.exception, "Ошибка поиска комплектации")
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

    MasterScreen(
        title = "Заказ-наряды",
        resource = resource,
        errorText = "Не удалось загрузить заказ-наряды",
        notFoundText = "Заказ-наряды не найдены",
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

            val currentLinked = linkedDocuments.lastOrNull()
            if (currentLinked != null) {
                TreeRootDocumentDetailsSheet(
                    document = currentLinked,
                    onBack = onNestedBack,
                    onOpenBaseDocument = onOpenBaseDocument,
                    onNomenclatureCharacteristicSearch = onNomenclatureCharacteristicSearch
                )
            } else {
                component.pickedOrder = order
                WorkOrderDetailsSheet(
                    order = order,
                    onBack = onNestedBack,
                    onNomenclatureCharacteristicSearch = onNomenclatureCharacteristicSearch,
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
        topBarTitleContent = if (panel == MasterPanel.List && isSearchMode) {
            {
                OutlinedTextField(
                    value = searchQueryDraft,
                    onValueChange = { searchQueryDraft = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 6.dp),
                    placeholder = { Text("Поиск заказ-наряда") },
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
                    IconButton(onClick = { component.changePanel(MasterPanel.Filter) }) {
                        Icon(FeatherIcons.Filter, contentDescription = "Фильтр")
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
                            Icon(FeatherIcons.RefreshCw, contentDescription = "Обновить")
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

        modifier = modifier
    )

    if (isResolvingBaseDocument || isResolvingLinkedByCharacteristic) {
        AlertDialog(
            onDismissRequest = {},
            title = { Text("Открытие документа...") },
            text = {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                    Text("Подождите, выполняем поиск.")
                }
            },
            confirmButton = {}
        )
    }

    if (characteristicMatches.size >= 2) {
        AlertDialog(
            onDismissRequest = { characteristicMatches = emptyList() },
            title = { Text("Найдено ${characteristicMatches.size} комплектаций") },
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
                            ?: "Без номера"
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
                    Text("Закрыть")
                }
            }
        )
    }
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
                text = "Поиск по:",
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
                        text = order.number?.let { "№ $it" } ?: "Без номера",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 6,
                        overflow = TextOverflow.Clip,
                        allowLinkTap = false,
                        allowLongPressCopy = true,
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
                        text = "созд. ${it}",
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
                        text = "изм. ${formatRelativeWorkDate(it.workDate)}",
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
