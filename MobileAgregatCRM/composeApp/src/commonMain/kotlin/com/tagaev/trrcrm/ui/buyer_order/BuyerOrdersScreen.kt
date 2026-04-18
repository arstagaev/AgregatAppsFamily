package com.tagaev.trrcrm.ui.buyer_order

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
import com.tagaev.trrcrm.domain.TreeRootResolvedDocument
import com.tagaev.trrcrm.domain.displayNameRu
import com.tagaev.trrcrm.models.BuyerOrderDto
import com.tagaev.trrcrm.ui.custom.SearchIconButtonWithIndicator
import com.tagaev.trrcrm.ui.master_screen.MasterPanel
import com.tagaev.trrcrm.ui.master_screen.MasterScreen
import com.tagaev.trrcrm.ui.master_screen.RefineSection
import com.tagaev.trrcrm.ui.master_screen.RefineScreen
import com.tagaev.trrcrm.ui.master_screen.TreeRootDocumentDetailsSheet
import com.tagaev.trrcrm.ui.master_screen.models.MessageModel
import com.tagaev.trrcrm.ui.root.LocalAppSnackbar
import compose.icons.FeatherIcons
import compose.icons.feathericons.ChevronsUp
import compose.icons.feathericons.Filter
import compose.icons.feathericons.RefreshCw
import compose.icons.feathericons.Search
import compose.icons.feathericons.X
import kotlinx.coroutines.launch

private enum class BuyerOrderSearchMode {
    NUMBER,
    MANAGER,
    AUTHOR,
}

private fun BuyerOrderSearchMode.toRefineType(): Refiner.SearchQueryType =
    when (this) {
        BuyerOrderSearchMode.NUMBER -> Refiner.SearchQueryType.CODE
        BuyerOrderSearchMode.MANAGER -> Refiner.SearchQueryType.MANAGER
        BuyerOrderSearchMode.AUTHOR -> Refiner.SearchQueryType.AUTHOR
    }

private fun refineToBuyerMode(type: Refiner.SearchQueryType): BuyerOrderSearchMode =
    when (type) {
        Refiner.SearchQueryType.MANAGER -> BuyerOrderSearchMode.MANAGER
        Refiner.SearchQueryType.AUTHOR -> BuyerOrderSearchMode.AUTHOR
        else -> BuyerOrderSearchMode.NUMBER
    }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BuyerOrdersScreen(
    component: BuyerOrdersComponent,
    modifier: Modifier = Modifier,
) {
    val resource by component.buyerOrders.collectAsState()
    val refineState by component.refineState.collectAsState()
    val panel by component.masterScreenPanel.collectAsState()
    val selectedId by component.selectedItemGuid.collectAsState()

    val scope = rememberCoroutineScope()
    val showSnackbar = LocalAppSnackbar.current
    val linkedDocuments = androidx.compose.runtime.remember { emptyList<TreeRootResolvedDocument>().toMutableStateList() }
    var isResolvingBaseDocument by rememberSaveable { mutableStateOf(false) }
    var isSearchMode by rememberSaveable { mutableStateOf(false) }
    var searchQueryDraft by rememberSaveable { mutableStateOf(refineState.searchQuery) }
    var searchModeDraft by rememberSaveable { mutableStateOf(refineToBuyerMode(refineState.searchQueryType)) }
    val isTopBarLoading = resource is Resource.Loading || (resource as? Resource.Success<*>)?.additionalLoading == true

    LaunchedEffect(refineState.searchQuery, refineState.searchQueryType, isSearchMode) {
        if (!isSearchMode) {
            searchQueryDraft = refineState.searchQuery
            searchModeDraft = refineToBuyerMode(refineState.searchQueryType)
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
        searchModeDraft = refineToBuyerMode(refineState.searchQueryType)
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
        title = "Заказ покупателя",
        resource = resource,
        errorText = "Не удалось загрузить заказы покупателя",
        notFoundText = "Заказы покупателя не найдены",
        refineState = refineState,
        onRefresh = { component.fullRefresh() },
        onLoadMore = { component.loadMore() },
        onFilterChanged = { component.setRefineState(it) },
        itemId = { it.guid.toString() },
        isItemChanged = { old, new -> old.messages.size != new.messages.size },
        listItem = { order, _, onClick ->
            BuyerOrderCard(order = order, onClick = onClick)
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
                BuyerOrderDetailsSheet(
                    order = order,
                    onBack = onNestedBack,
                    onOpenBaseDocument = onOpenBaseDocument,
                    onSendMessage = { message, onResult ->
                        scope.launch {
                            val err = component.sendMessage(order.number.orEmpty(), order.date.orEmpty(), message)
                            if (err == null) {
                                component.addLocalMessage(
                                    order.guid.toString(),
                                    message = MessageModel(author = "я", text = message)
                                )
                            }
                            onResult(err)
                        }
                    }
                )
            }
        },
        filterScreen = { current, onDismiss, onApply ->
            RefineScreen(
                current = current,
                onBack = onDismiss,
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
                        placeholder = { Text("Поиск заказа покупателя") },
                        singleLine = true,
                        enabled = !isTopBarLoading,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(onSearch = { applySearch() })
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
                            searchModeDraft = refineToBuyerMode(refineState.searchQueryType)
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
                BuyerOrderSearchRow(
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
private fun BuyerOrderSearchRow(
    selected: BuyerOrderSearchMode,
    onSelect: (BuyerOrderSearchMode) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        FilterChip(
            selected = selected == BuyerOrderSearchMode.NUMBER,
            onClick = { onSelect(BuyerOrderSearchMode.NUMBER) },
            label = { Text("По номеру") }
        )
        FilterChip(
            selected = selected == BuyerOrderSearchMode.MANAGER,
            onClick = { onSelect(BuyerOrderSearchMode.MANAGER) },
            label = { Text("По менеджеру") }
        )
        FilterChip(
            selected = selected == BuyerOrderSearchMode.AUTHOR,
            onClick = { onSelect(BuyerOrderSearchMode.AUTHOR) },
            label = { Text("По автору") }
        )
    }
}

@Composable
private fun BuyerOrderCard(
    order: BuyerOrderDto,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val number = order.number.orEmpty().ifBlank { "Без номера" }
    val status = order.status.orEmpty().ifBlank { "—" }
    val manager = order.manager.orEmpty().ifBlank { "—" }
    val author = order.author.orEmpty().ifBlank { "—" }
    val car = (order.car ?: order.carText).orEmpty().ifBlank { "—" }
    val baseDoc = order.baseDocument.orEmpty().ifBlank { "—" }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 6.dp, vertical = 3.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp)) {
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
                text = "Менеджер: $manager",
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
                text = "Основание: $baseDoc",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = car,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

