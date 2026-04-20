package com.tagaev.trrcrm.ui.repair_template_catalog

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.tagaev.trrcrm.data.remote.Resource
import com.tagaev.trrcrm.domain.Refiner
import com.tagaev.trrcrm.ui.custom.SearchIconButtonWithIndicator
import com.tagaev.trrcrm.ui.custom.UniversalCardItem
import com.tagaev.trrcrm.ui.master_screen.MasterPanel
import com.tagaev.trrcrm.ui.master_screen.MasterScreen
import com.tagaev.trrcrm.ui.master_screen.RefineScreen
import com.tagaev.trrcrm.ui.master_screen.RefineSection
import compose.icons.FeatherIcons
import compose.icons.feathericons.ChevronsUp
import compose.icons.feathericons.Filter
import compose.icons.feathericons.RefreshCw
import compose.icons.feathericons.Search
import compose.icons.feathericons.X

private enum class RepairTemplateSearchMode {
    MODEL,
    YEAR_FROM,
    YEAR_TO,
    TRANSMISSION,
    ENGINE,
    REPAIR_KIND,
    NAME,
    CODE,
}

private fun RepairTemplateSearchMode.toRefineType(): Refiner.SearchQueryType = when (this) {
    RepairTemplateSearchMode.MODEL -> Refiner.SearchQueryType.REPAIR_TEMPLATE_MODEL
    RepairTemplateSearchMode.YEAR_FROM -> Refiner.SearchQueryType.REPAIR_TEMPLATE_YEAR_FROM
    RepairTemplateSearchMode.YEAR_TO -> Refiner.SearchQueryType.REPAIR_TEMPLATE_YEAR_TO
    RepairTemplateSearchMode.TRANSMISSION -> Refiner.SearchQueryType.REPAIR_TEMPLATE_TRANSMISSION
    RepairTemplateSearchMode.ENGINE -> Refiner.SearchQueryType.REPAIR_TEMPLATE_ENGINE
    RepairTemplateSearchMode.REPAIR_KIND -> Refiner.SearchQueryType.REPAIR_TEMPLATE_REPAIR_KIND
    RepairTemplateSearchMode.NAME -> Refiner.SearchQueryType.REPAIR_TEMPLATE_NAME
    RepairTemplateSearchMode.CODE -> Refiner.SearchQueryType.REPAIR_TEMPLATE_CODE
}

private fun refineToSearchMode(type: Refiner.SearchQueryType): RepairTemplateSearchMode =
    when (type) {
        Refiner.SearchQueryType.REPAIR_TEMPLATE_YEAR_FROM -> RepairTemplateSearchMode.YEAR_FROM
        Refiner.SearchQueryType.REPAIR_TEMPLATE_YEAR_TO -> RepairTemplateSearchMode.YEAR_TO
        Refiner.SearchQueryType.REPAIR_TEMPLATE_TRANSMISSION -> RepairTemplateSearchMode.TRANSMISSION
        Refiner.SearchQueryType.REPAIR_TEMPLATE_ENGINE -> RepairTemplateSearchMode.ENGINE
        Refiner.SearchQueryType.REPAIR_TEMPLATE_REPAIR_KIND -> RepairTemplateSearchMode.REPAIR_KIND
        Refiner.SearchQueryType.REPAIR_TEMPLATE_NAME -> RepairTemplateSearchMode.NAME
        Refiner.SearchQueryType.REPAIR_TEMPLATE_CODE -> RepairTemplateSearchMode.CODE
        Refiner.SearchQueryType.REPAIR_TEMPLATE_MODEL -> RepairTemplateSearchMode.MODEL
        else -> RepairTemplateSearchMode.MODEL
    }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RepairTemplateCatalogScreen(
    component: IRepairTemplateCatalogComponent,
    modifier: Modifier = Modifier,
) {
    val resource by component.items.collectAsState()
    val refineState by component.refineState.collectAsState()
    val panel by component.masterScreenPanel.collectAsState()
    val selectedId by component.selectedItemGuid.collectAsState()

    var isSearchMode by rememberSaveable { mutableStateOf(false) }
    var searchQueryDraft by rememberSaveable { mutableStateOf(refineState.searchQuery) }
    var searchModeDraft by rememberSaveable { mutableStateOf(refineToSearchMode(refineState.searchQueryType)) }
    val isTopBarLoading = resource is Resource.Loading ||
        (resource as? Resource.Success<*>)?.additionalLoading == true

    LaunchedEffect(refineState.searchQuery, refineState.searchQueryType, isSearchMode) {
        if (!isSearchMode) {
            searchQueryDraft = refineState.searchQuery
            searchModeDraft = refineToSearchMode(refineState.searchQueryType)
        }
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
        searchModeDraft = refineToSearchMode(refineState.searchQueryType)
    }
    val clearSearchAndClose: () -> Unit = {
        isSearchMode = false
        component.setRefineState(refineState.copy(searchQuery = ""))
    }
    val handleDetailsBack: () -> Unit = {
        component.selectItemFromList(null)
        component.changePanel(MasterPanel.List)
    }

    MasterScreen(
        title = "Калькуляция",
        resource = resource,
        errorText = "Не удалось загрузить шаблоны ремонта",
        notFoundText = "Шаблоны не найдены",
        refineState = refineState,
        onRefresh = { component.fullRefresh() },
        onLoadMore = { component.loadMore() },
        onFilterChanged = { component.setRefineState(it) },
        itemId = {
            it.guid.trim().ifBlank { it.code?.trim().orEmpty() }
                .ifBlank { it.link?.trim().orEmpty() }
                .ifBlank { it.name?.trim().orEmpty() }
        },
        isItemChanged = { old, new ->
            old.name != new.name || old.model != new.model || old.recommendedPrice != new.recommendedPrice
        },
        listItem = { item, _, onClick ->
            val titleText = item.name?.trim()?.takeIf { it.isNotEmpty() }
                ?: item.link?.trim().orEmpty()
            val prices = listOfNotNull(
                item.recommendedPrice?.trim()?.takeIf { it.isNotEmpty() },
                item.retailPrice?.trim()?.takeIf { it.isNotEmpty() },
            ).joinToString(" · ")
            UniversalCardItem(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                onClick = onClick,
                title = titleText,
                allowTitleLongPressCopy = true,
                subtitle = listOfNotNull(item.model, item.transmissionType)
                    .map { it.trim() }
                    .filter { it.isNotEmpty() }
                    .joinToString(" · "),
                bigText1 = prices,
                bigText2 = item.code?.trim().orEmpty(),
                bottomLeftText = item.repairKind?.trim().orEmpty(),
                bottomRightText = "",
            )
        },
        detailsContent = { item, onClose ->
            RepairTemplateCatalogDetailsSheet(item = item, onBack = onClose)
        },
        filterScreen = { current, onDismiss, onApply ->
            RefineScreen(
                current = current,
                onBack = onDismiss,
                messageForUser = "Сортировка справочника — по наименованию; меняется только направление (и подразделение при доступе).",
                sections = setOf(
                    RefineSection.FILTER_VAL,
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
                            searchModeDraft =
                                if (refineState.searchQueryType in Refiner.SearchQueryType.repairTemplateCatalogSearchTypes) {
                                    refineToSearchMode(refineState.searchQueryType)
                                } else {
                                    RepairTemplateSearchMode.MODEL
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
        topBarBottomContent = if (panel == MasterPanel.List && isSearchMode) {
            {
                RepairTemplateSearchModeRow(
                    selected = searchModeDraft,
                    onSelected = { searchModeDraft = it },
                )
            }
        } else {
            null
        },
        onDetailsBack = handleDetailsBack,
        modifier = modifier,
    )
}

@Composable
private fun RepairTemplateSearchModeRow(
    selected: RepairTemplateSearchMode,
    onSelected: (RepairTemplateSearchMode) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 12.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        FilterChip(
            selected = selected == RepairTemplateSearchMode.MODEL,
            onClick = { onSelected(RepairTemplateSearchMode.MODEL) },
            label = { Text("Модель", maxLines = 1, overflow = TextOverflow.Ellipsis) },
        )
        FilterChip(
            selected = selected == RepairTemplateSearchMode.YEAR_FROM,
            onClick = { onSelected(RepairTemplateSearchMode.YEAR_FROM) },
            label = { Text("Год от", maxLines = 1, overflow = TextOverflow.Ellipsis) },
        )
        FilterChip(
            selected = selected == RepairTemplateSearchMode.YEAR_TO,
            onClick = { onSelected(RepairTemplateSearchMode.YEAR_TO) },
            label = { Text("Год до", maxLines = 1, overflow = TextOverflow.Ellipsis) },
        )
        FilterChip(
            selected = selected == RepairTemplateSearchMode.TRANSMISSION,
            onClick = { onSelected(RepairTemplateSearchMode.TRANSMISSION) },
            label = { Text("Тип КПП", maxLines = 1, overflow = TextOverflow.Ellipsis) },
        )
        FilterChip(
            selected = selected == RepairTemplateSearchMode.ENGINE,
            onClick = { onSelected(RepairTemplateSearchMode.ENGINE) },
            label = { Text("ДВС", maxLines = 1, overflow = TextOverflow.Ellipsis) },
        )
        FilterChip(
            selected = selected == RepairTemplateSearchMode.REPAIR_KIND,
            onClick = { onSelected(RepairTemplateSearchMode.REPAIR_KIND) },
            label = { Text("Вид ремонта", maxLines = 1, overflow = TextOverflow.Ellipsis) },
        )
        FilterChip(
            selected = selected == RepairTemplateSearchMode.NAME,
            onClick = { onSelected(RepairTemplateSearchMode.NAME) },
            label = { Text("Наименование", maxLines = 1, overflow = TextOverflow.Ellipsis) },
        )
        FilterChip(
            selected = selected == RepairTemplateSearchMode.CODE,
            onClick = { onSelected(RepairTemplateSearchMode.CODE) },
            label = { Text("Код", maxLines = 1, overflow = TextOverflow.Ellipsis) },
        )
    }
}

private fun RepairTemplateSearchMode.placeholder(): String = when (this) {
    RepairTemplateSearchMode.MODEL -> "Модель…"
    RepairTemplateSearchMode.YEAR_FROM -> "Год от…"
    RepairTemplateSearchMode.YEAR_TO -> "Год до…"
    RepairTemplateSearchMode.TRANSMISSION -> "Тип КПП…"
    RepairTemplateSearchMode.ENGINE -> "ДВС…"
    RepairTemplateSearchMode.REPAIR_KIND -> "Вид ремонта…"
    RepairTemplateSearchMode.NAME -> "Наименование…"
    RepairTemplateSearchMode.CODE -> "Код…"
}
