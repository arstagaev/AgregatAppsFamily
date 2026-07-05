package com.tagaev.trrcrm.ui.expense_requests

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.tagaev.trrcrm.data.remote.Resource
import com.tagaev.trrcrm.data.remote.friendlyError
import com.tagaev.trrcrm.domain.Refiner
import com.tagaev.trrcrm.domain.TreeRootDocumentKind
import com.tagaev.trrcrm.domain.TreeRootResolvedDocument
import com.tagaev.trrcrm.domain.linkTabCaptionForListRow
import com.tagaev.trrcrm.domain.linkTabLabel
import com.tagaev.trrcrm.models.formattedAmount
import com.tagaev.trrcrm.ui.custom.SearchIconButtonWithIndicator
import com.tagaev.trrcrm.ui.custom.StatusBadge
import com.tagaev.trrcrm.ui.custom.StatusStyle
import com.tagaev.trrcrm.ui.custom.UniversalCardItem
import com.tagaev.trrcrm.ui.master_screen.LinkedDocumentStackTabStrip
import com.tagaev.trrcrm.ui.master_screen.MasterPanel
import com.tagaev.trrcrm.ui.master_screen.MasterScreen
import com.tagaev.trrcrm.ui.master_screen.RefineScreen
import com.tagaev.trrcrm.ui.master_screen.RefineSection
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

private val EXPENSE_REQUEST_STATUS_STYLES = mapOf(
    "Создана" to StatusStyle(DefaultColors.RainbowSkyBg, DefaultColors.RainbowSkyFg),
    "Согласована" to StatusStyle(DefaultColors.RainbowAquaBg, DefaultColors.RainbowAquaFg),
)

private val EXPENSE_TOPBAR_SEARCH_OPTIONS = listOf(
    Refiner.SearchQueryType.CODE,
    Refiner.SearchQueryType.AUTHOR,
    Refiner.SearchQueryType.TOPIC,
)

private enum class ExpenseSearchMode {
    NUMBER,
    AUTHOR,
    TOPIC,
}

private fun ExpenseSearchMode.toRefineType(): Refiner.SearchQueryType = when (this) {
    ExpenseSearchMode.NUMBER -> Refiner.SearchQueryType.CODE
    ExpenseSearchMode.AUTHOR -> Refiner.SearchQueryType.AUTHOR
    ExpenseSearchMode.TOPIC -> Refiner.SearchQueryType.TOPIC
}

private fun refineToExpenseMode(type: Refiner.SearchQueryType): ExpenseSearchMode =
    when (type) {
        Refiner.SearchQueryType.AUTHOR -> ExpenseSearchMode.AUTHOR
        Refiner.SearchQueryType.TOPIC -> ExpenseSearchMode.TOPIC
        else -> ExpenseSearchMode.NUMBER
    }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpenseRequestsScreen(
    component: IExpenseRequestsComponent,
    modifier: Modifier = Modifier,
) {
    val resource by component.expenseRequests.collectAsState()
    val refineState by component.refineState.collectAsState()
    val panel by component.masterScreenPanel.collectAsState()
    val selectedId by component.selectedItemGuid.collectAsState()

    var isSearchMode by rememberSaveable { mutableStateOf(false) }
    var searchQueryDraft by rememberSaveable { mutableStateOf(refineState.searchQuery) }
    var searchModeDraft by rememberSaveable { mutableStateOf(refineToExpenseMode(refineState.searchQueryType)) }
    val isTopBarLoading = resource is Resource.Loading ||
        (resource as? Resource.Success<*>)?.additionalLoading == true

    val scope = rememberCoroutineScope()
    val showSnackbar = LocalAppSnackbar.current
    val linkedDocuments = remember { emptyList<TreeRootResolvedDocument>().toMutableStateList() }
    var isResolvingBaseDocument by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(refineState.searchQuery, refineState.searchQueryType, isSearchMode) {
        if (!isSearchMode) {
            searchQueryDraft = refineState.searchQuery
            searchModeDraft = refineToExpenseMode(refineState.searchQueryType)
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
                searchQueryType = searchModeDraft.toRefineType(),
            ),
        )
    }
    val hideSearchForm: () -> Unit = {
        isSearchMode = false
        searchQueryDraft = refineState.searchQuery
        searchModeDraft = refineToExpenseMode(refineState.searchQueryType)
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

    Box(modifier = modifier.fillMaxSize()) {
        MasterScreen(
            title = "Заявки на расход",
            resource = resource,
            errorText = "Не удалось загрузить заявки на расход",
            notFoundText = "Заявки на расход не найдены",
            refineState = refineState,
            onRefresh = { component.fullRefresh() },
            onLoadMore = { component.loadMore() },
            onFilterChanged = { component.setRefineState(it) },
            itemId = { it.guid },
            isItemChanged = { old, new ->
                old.date != new.date ||
                    old.requestStatus != new.requestStatus ||
                    old.amount != new.amount ||
                    old.topic != new.topic
            },
            listItem = { item, _, onClick ->
                val bottomRight = buildList {
                    item.formattedAmount()?.let { add(it) }
                    item.author?.trim()?.takeIf { it.isNotEmpty() }?.let { add(it) }
                }.joinToString(" · ")

                UniversalCardItem(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                    onClick = onClick,
                    title = item.number.orEmpty(),
                    allowTitleLongPressCopy = true,
                    subtitle = listOfNotNull(item.organization, item.branch)
                        .filter { it.isNotBlank() }
                        .joinToString(" · "),
                    topRightPrimary = {
                        val status = item.requestStatus
                        if (!status.isNullOrBlank()) {
                            StatusBadge(
                                state = status,
                                styles = EXPENSE_REQUEST_STATUS_STYLES,
                            )
                        }
                    },
                    bigText1 = item.topic.orEmpty(),
                    bigText2 = listOfNotNull(item.requestType, item.operation)
                        .filter { it.isNotBlank() }
                        .joinToString(" · "),
                    bottomLeftText = item.date.orEmpty(),
                    bottomRightText = bottomRight,
                )
            },
            detailsContent = { item, onClose ->
                val onOpenBaseDocument: (String) -> Unit = { rawBaseDocument ->
                    scope.launch {
                        isResolvingBaseDocument = true
                        try {
                            when (val resolved = runCatching { component.resolveBaseDocument(rawBaseDocument) }
                                .getOrElse { e -> Resource.Error(causes = friendlyError(e, "Ошибка поиска документа")) }) {
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
                    )
                } else {
                    ExpenseRequestDetailsSheet(
                        item = item,
                        onOpenBaseDocument = onOpenBaseDocument,
                    )
                }
            },
            filterScreen = { current, onDismiss, onApply ->
                RefineScreen(
                    current = current,
                    onBack = onDismiss,
                    messageForUser = "Корректно работает только сортировка по Дате, остальные фильтры пока в разработке",
                    orderByOptions = Refiner.OrderBy.allForUiExceptDateLastModification,
                    sections = setOf(
                        RefineSection.STATUS,
                        RefineSection.FILTER_VAL,
                        RefineSection.ORDER,
                        RefineSection.DIRECTION,
                    ),
                    onApply = { newState ->
                        val applied = newState.copy(
                            searchQuery = refineState.searchQuery,
                            searchQueryType = refineState.searchQueryType,
                        )
                        component.setRefineState(applied)
                        onApply(applied)
                    },
                )
            },
            panel = panel,
            onPanelChange = { component.changePanel(it) },
            selectedItemId = selectedId,
            onSelectedItemChange = { component.selectItemFromList(it) },
            topBarNavigationIcon = if (panel == MasterPanel.List && isSearchMode) {
                {
                    Row {
                        IconButton(onClick = hideSearchForm, enabled = !isTopBarLoading) {
                            Icon(FeatherIcons.ChevronsUp, contentDescription = "Скрыть поиск")
                        }
                        IconButton(onClick = clearSearchAndClose, enabled = !isTopBarLoading) {
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
                            placeholder = { Text(searchModeDraft.placeholder()) },
                            singleLine = true,
                            enabled = !isTopBarLoading,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                            keyboardActions = KeyboardActions(onSearch = { applySearch() }),
                        )
                    }
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
                                strokeWidth = 2.dp,
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
                                searchModeDraft = if (refineState.searchQueryType in EXPENSE_TOPBAR_SEARCH_OPTIONS) {
                                    refineToExpenseMode(refineState.searchQueryType)
                                } else {
                                    ExpenseSearchMode.NUMBER
                                }
                                isSearchMode = true
                            },
                        )
                        if (isLoadingTopBar) {
                            CircularProgressIndicator(
                                modifier = Modifier
                                    .padding(horizontal = 8.dp)
                                    .size(18.dp),
                                strokeWidth = 2.dp,
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
                    val root = rows.firstOrNull { it.guid == selectedId }
                    val rootLabel = root?.let { item ->
                        linkTabCaptionForListRow(item.link, TreeRootDocumentKind.EXPENSE_REQUEST, item.number)
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
            } else {
                null
            },
            topBarBottomContent = if (panel == MasterPanel.List && isSearchMode) {
                {
                    ExpenseRequestsSearchModeRow(
                        selected = searchModeDraft,
                        onSelected = { searchModeDraft = it },
                    )
                }
            } else {
                null
            },
            onDetailsBack = handleDetailsBack,
            modifier = Modifier.fillMaxSize(),
        )

        if (isResolvingBaseDocument) {
            AlertDialog(
                onDismissRequest = {},
                title = { Text("Пожалуйста, подождите") },
                text = { Text("ищем документ основание....") },
                confirmButton = {},
            )
        }
    }
}

@Composable
private fun ExpenseRequestsSearchModeRow(
    selected: ExpenseSearchMode,
    onSelected: (ExpenseSearchMode) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 12.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        FilterChip(
            selected = selected == ExpenseSearchMode.NUMBER,
            onClick = { onSelected(ExpenseSearchMode.NUMBER) },
            label = { Text("Номер", maxLines = 1, overflow = TextOverflow.Ellipsis) },
        )
        FilterChip(
            selected = selected == ExpenseSearchMode.AUTHOR,
            onClick = { onSelected(ExpenseSearchMode.AUTHOR) },
            label = { Text("Автор", maxLines = 1, overflow = TextOverflow.Ellipsis) },
        )
        FilterChip(
            selected = selected == ExpenseSearchMode.TOPIC,
            onClick = { onSelected(ExpenseSearchMode.TOPIC) },
            label = { Text("Тема", maxLines = 1, overflow = TextOverflow.Ellipsis) },
        )
    }
}

private fun ExpenseSearchMode.placeholder(): String = when (this) {
    ExpenseSearchMode.NUMBER -> "Номер заявки…"
    ExpenseSearchMode.AUTHOR -> "Автор…"
    ExpenseSearchMode.TOPIC -> "Тема…"
}
