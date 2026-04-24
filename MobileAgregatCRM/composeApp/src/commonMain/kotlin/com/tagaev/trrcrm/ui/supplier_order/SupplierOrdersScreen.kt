package com.tagaev.trrcrm.ui.supplier_order

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.tagaev.trrcrm.data.remote.Resource
import com.tagaev.trrcrm.domain.Refiner
import com.tagaev.trrcrm.domain.TreeRootDocumentKind
import com.tagaev.trrcrm.domain.TreeRootResolvedDocument
import com.tagaev.trrcrm.domain.linkTabCaptionForListRow
import com.tagaev.trrcrm.domain.linkTabLabel
import com.tagaev.trrcrm.models.SupplierOrderDto
import com.tagaev.trrcrm.ui.custom.SearchIconButtonWithIndicator
import com.tagaev.trrcrm.ui.master_screen.LinkedDocumentStackTabStrip
import com.tagaev.trrcrm.ui.master_screen.MasterPanel
import com.tagaev.trrcrm.ui.master_screen.MasterScreen
import com.tagaev.trrcrm.ui.master_screen.RefineScreen
import com.tagaev.trrcrm.ui.master_screen.RefineSection
import com.tagaev.trrcrm.ui.master_screen.TreeRootDocumentDetailsSheet
import com.tagaev.trrcrm.ui.root.LocalAppSnackbar
import compose.icons.FeatherIcons
import compose.icons.feathericons.ChevronsUp
import compose.icons.feathericons.Filter
import compose.icons.feathericons.RefreshCw
import compose.icons.feathericons.Search
import compose.icons.feathericons.X
import kotlinx.coroutines.launch

private enum class SupplierOrderSearchMode {
    NUMBER,
    COUNTERPARTY,
    AUTHOR,
    MANAGER,
}

private fun SupplierOrderSearchMode.toRefineType(): Refiner.SearchQueryType =
    when (this) {
        SupplierOrderSearchMode.NUMBER -> Refiner.SearchQueryType.CODE
        SupplierOrderSearchMode.COUNTERPARTY -> Refiner.SearchQueryType.COUNTERPARTY
        SupplierOrderSearchMode.AUTHOR -> Refiner.SearchQueryType.AUTHOR
        SupplierOrderSearchMode.MANAGER -> Refiner.SearchQueryType.MANAGER
    }

private fun refineToSupplierMode(type: Refiner.SearchQueryType): SupplierOrderSearchMode =
    when (type) {
        Refiner.SearchQueryType.COUNTERPARTY -> SupplierOrderSearchMode.COUNTERPARTY
        Refiner.SearchQueryType.AUTHOR -> SupplierOrderSearchMode.AUTHOR
        Refiner.SearchQueryType.MANAGER -> SupplierOrderSearchMode.MANAGER
        else -> SupplierOrderSearchMode.NUMBER
    }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SupplierOrdersScreen(
    component: SupplierOrdersComponent,
    modifier: Modifier = Modifier,
) {
    val resource by component.supplierOrders.collectAsState()
    val refineState by component.refineState.collectAsState()
    val panel by component.masterScreenPanel.collectAsState()
    val selectedId by component.selectedItemGuid.collectAsState()
    val scope = rememberCoroutineScope()

    var isSearchMode by rememberSaveable { mutableStateOf(false) }
    var searchQueryDraft by rememberSaveable { mutableStateOf(refineState.searchQuery) }
    var searchModeDraft by rememberSaveable { mutableStateOf(refineToSupplierMode(refineState.searchQueryType)) }
    val isTopBarLoading = resource is Resource.Loading || (resource as? Resource.Success<*>)?.additionalLoading == true
    val showSnackbar = LocalAppSnackbar.current
    val linkedDocuments = androidx.compose.runtime.remember { emptyList<TreeRootResolvedDocument>().toMutableStateList() }
    var isResolvingBaseDocument by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(refineState.searchQuery, refineState.searchQueryType, isSearchMode) {
        if (!isSearchMode) {
            searchQueryDraft = refineState.searchQuery
            searchModeDraft = refineToSupplierMode(refineState.searchQueryType)
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
                searchQueryType = searchModeDraft.toRefineType()
            )
        )
    }

    val hideSearchForm: () -> Unit = {
        isSearchMode = false
        searchQueryDraft = refineState.searchQuery
        searchModeDraft = refineToSupplierMode(refineState.searchQueryType)
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
    MasterScreen(
        title = "Заказ поставщику",
        resource = resource,
        errorText = "Не удалось загрузить заказы поставщику",
        notFoundText = "Заказы поставщику не найдены",
        refineState = refineState,
        onRefresh = { component.fullRefresh() },
        onLoadMore = { component.loadMore() },
        onFilterChanged = { component.setRefineState(it) },
        itemId = { it.guid.toString() },
        isItemChanged = { old, new -> old.messages.size != new.messages.size },
        listItem = { order, _, onClick ->
            SupplierOrderCard(order = order, onClick = onClick)
        },
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
                    onOpenBaseDocument = onOpenBaseDocument
                )
            } else {
                SupplierOrderDetailsSheet(
                    order = order,
                    onBack = onNestedBack,
                    onOpenBaseDocument = onOpenBaseDocument
                )
            }
        },
        filterScreen = { current, onDismiss, onApply ->
            RefineScreen(
                current = current,
                onBack = onDismiss,
                orderByOptions = Refiner.OrderBy.allForUiExceptDateLastModification,
                sections = setOf(
                    RefineSection.STATUS,
                    RefineSection.FILTER_VAL,
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
                        placeholder = { Text("Поиск заказа поставщику") },
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
                    IconButton(onClick = { component.changePanel(MasterPanel.Filter) }) {
                        Icon(FeatherIcons.Filter, contentDescription = "Фильтр")
                    }
                    SearchIconButtonWithIndicator(
                        showIndicator = refineState.searchQuery.isNotBlank(),
                        enabled = !isLoadingTopBar,
                        onClick = {
                            searchQueryDraft = refineState.searchQuery
                            searchModeDraft = refineToSupplierMode(refineState.searchQueryType)
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
                val root = rows.firstOrNull { it.guid.toString() == selectedId }
                val rootLabel = root?.let { o ->
                    linkTabCaptionForListRow(o.link, TreeRootDocumentKind.SUPPLIER_ORDER, o.number)
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
                SupplierOrderSearchRow(
                    selected = searchModeDraft,
                    onSelect = { searchModeDraft = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 4.dp)
                )
            }
        } else {
            null
        },
        onDetailsBack = handleDetailsBack,
        modifier = modifier
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
private fun SupplierOrderSearchRow(
    selected: SupplierOrderSearchMode,
    onSelect: (SupplierOrderSearchMode) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        FilterChip(
            selected = selected == SupplierOrderSearchMode.NUMBER,
            onClick = { onSelect(SupplierOrderSearchMode.NUMBER) },
            label = { Text("По номеру") }
        )
        FilterChip(
            selected = selected == SupplierOrderSearchMode.COUNTERPARTY,
            onClick = { onSelect(SupplierOrderSearchMode.COUNTERPARTY) },
            label = { Text("По контрагенту") }
        )
        FilterChip(
            selected = selected == SupplierOrderSearchMode.AUTHOR,
            onClick = { onSelect(SupplierOrderSearchMode.AUTHOR) },
            label = { Text("По автору") }
        )
        FilterChip(
            selected = selected == SupplierOrderSearchMode.MANAGER,
            onClick = { onSelect(SupplierOrderSearchMode.MANAGER) },
            label = { Text("По менеджеру") }
        )
    }
}

@Composable
private fun SupplierOrderCard(
    order: SupplierOrderDto,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val number = order.number.orEmpty().ifBlank { "Без номера" }
    val status = order.status.orEmpty().ifBlank { "—" }
    val counterparty = order.counterparty.orEmpty().ifBlank { "—" }
    val manager = order.manager.orEmpty().ifBlank { "—" }
    val author = order.author.orEmpty().ifBlank { "—" }
    val baseDoc = order.baseDocument.orEmpty().ifBlank { "—" }
    val amount = order.documentAmount.orEmpty().ifBlank { "—" }
    val date = order.date.orEmpty().ifBlank { "—" }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 6.dp, vertical = 2.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.Top
            ) {
                Text(
                    text = "№ $number",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = status,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(Modifier.height(3.dp))
            Text(
                text = "Контрагент: $counterparty",
                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "Автор: $author",
                style = MaterialTheme.typography.bodySmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "Менеджер: $manager",
                style = MaterialTheme.typography.bodySmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "Основание: $baseDoc",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "сумма: $amount",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = "дата: $date",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
            }
            Spacer(Modifier.height(1.dp))
            Text(
                text = "товары: ${order.products.size}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

