package com.tagaev.trrcrm.ui.cargo

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import com.tagaev.trrcrm.domain.OptionChipsScrollingRow
import com.tagaev.trrcrm.domain.Refiner
import com.tagaev.trrcrm.domain.TreeRootResolvedDocument
import com.tagaev.trrcrm.domain.displayNameRu
import com.tagaev.trrcrm.models.CargoDto
import com.tagaev.trrcrm.ui.custom.SearchIconButtonWithIndicator
import com.tagaev.trrcrm.ui.custom.StatusBadge
import com.tagaev.trrcrm.ui.custom.StatusStyle
import com.tagaev.trrcrm.ui.custom.TextC
import com.tagaev.trrcrm.ui.custom.withEscapedNewlines
import com.tagaev.trrcrm.ui.master_screen.MasterPanel
import com.tagaev.trrcrm.ui.master_screen.MasterScreen
import com.tagaev.trrcrm.ui.master_screen.RefineSection
import com.tagaev.trrcrm.ui.master_screen.RefineScreen
import com.tagaev.trrcrm.ui.master_screen.TreeRootDocumentDetailsSheet
import com.tagaev.trrcrm.ui.root.LocalAppSnackbar
import com.tagaev.trrcrm.ui.style.DefaultColors
import compose.icons.FeatherIcons
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
    Refiner.SearchQueryType.CODE -> "Номер"
    Refiner.SearchQueryType.ROUTE -> "Маршрут"
    Refiner.SearchQueryType.CARRIER -> "Перевозчик"
    Refiner.SearchQueryType.AUTHOR -> "Автор"
    Refiner.SearchQueryType.TOPIC -> "Тема"
    Refiner.SearchQueryType.MANAGER -> "Менеджер"
    Refiner.SearchQueryType.COUNTERPARTY -> "Контрагент"
    Refiner.SearchQueryType.MASTER -> "Мастер"
    Refiner.SearchQueryType.KIT_CHARACTERISTIC -> "Хар. комплекта"
    Refiner.SearchQueryType.AUTO -> "Автомобиль"
    Refiner.SearchQueryType.LICENSE_PLATE -> "Госномер"
    Refiner.SearchQueryType.VIN_NUMBER -> "VIN"
    Refiner.SearchQueryType.FIX_TYPE -> "Вид ремонта"
    Refiner.SearchQueryType.CLIENT -> "Заказчик"
}

@Composable
fun CargoScreen(component: ICargoComponent) {
    val resource by component.cargos.collectAsState()
    val refineState by component.refineState.collectAsState()
    val panel by component.masterScreenPanel.collectAsState()
    val selectedId by component.selectedItemGuid.collectAsState()
    var isSearchMode by rememberSaveable { mutableStateOf(false) }
    var searchQueryDraft by rememberSaveable { mutableStateOf(refineState.searchQuery) }
    var searchTypeDraft by rememberSaveable { mutableStateOf(refineState.searchQueryType) }
    val isTopBarLoading = resource is Resource.Loading ||
            (resource as? Resource.Success<*>)?.additionalLoading == true
    val scope = rememberCoroutineScope()
    val showSnackbar = LocalAppSnackbar.current
    val linkedDocuments = remember { emptyList<TreeRootResolvedDocument>().toMutableStateList() }
    var isResolvingBaseDocument by rememberSaveable { mutableStateOf(false) }

//    var isSendingMessage by remember { mutableStateOf(false) }
//    var lastSendError by remember { mutableStateOf<String?>(null) }

    androidx.compose.runtime.LaunchedEffect(refineState.searchQuery, refineState.searchQueryType, isSearchMode) {
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
    val linkedDocTitle = linkedDocuments.lastOrNull()?.kind?.displayNameRu()

    MasterScreen(
        title = "Доставки",
        resource = resource,
        errorText = "Не удалось загрузить доставки",
        notFoundText = "Доставки не найдены",
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
                    onOpenBaseDocument = onOpenBaseDocument
                )
            } else {
                CargoDetailsSheet(
                    cargo = cargo,
                    onClose = onNestedBack,
                    onOpenBaseDocument = onOpenBaseDocument
                )
            }
        },

        // Full-screen filter screen (not dialog)
        filterScreen = { current, onDismiss, onApply ->
            RefineScreen(
                current = current,
                onBack = onDismiss,
                messageForUser = "Корректно работает только сортировка по Дате, остальные фильтры пока в разработке",
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
                        placeholder = { Text("Поиск доставки") },
                        singleLine = true,
                        enabled = !isTopBarLoading,
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = androidx.compose.foundation.text.KeyboardActions(onSearch = { applySearch() })
                    )
                }
            }
            panel == MasterPanel.Details && linkedDocTitle != null -> {
                { Text("Документ Основание: $linkedDocTitle") }
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
                    IconButton(onClick = { component.changePanel(MasterPanel.Filter) }) {
                        Icon(FeatherIcons.Filter, contentDescription = "Фильтр")
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
                            Icon(FeatherIcons.RefreshCw, contentDescription = "Обновить")
                        }
                    }
                }
            }
        },
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
            title = { Text("Пожалуйста, подождите") },
            text = { Text("ищем документ основание....") },
            confirmButton = {}
        )
    }
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
                text = "Поиск по:",
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
