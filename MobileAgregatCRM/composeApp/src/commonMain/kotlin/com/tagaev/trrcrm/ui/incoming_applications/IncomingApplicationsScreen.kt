package com.tagaev.trrcrm.ui.incoming_applications

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.layout.padding
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
import com.tagaev.trrcrm.ui.custom.StatusBadge
import com.tagaev.trrcrm.ui.custom.StatusStyle
import com.tagaev.trrcrm.ui.custom.UniversalCardItem
import com.tagaev.trrcrm.ui.master_screen.MasterPanel
import com.tagaev.trrcrm.ui.master_screen.MasterScreen
import com.tagaev.trrcrm.ui.master_screen.RefineScreen
import com.tagaev.trrcrm.ui.master_screen.RefineSection
import com.tagaev.trrcrm.ui.style.DefaultColors
import compose.icons.FeatherIcons
import compose.icons.feathericons.ChevronsUp
import compose.icons.feathericons.Filter
import compose.icons.feathericons.RefreshCw
import compose.icons.feathericons.Search
import compose.icons.feathericons.X

private val INCOMING_TOPBAR_SEARCH_OPTIONS = listOf(
    Refiner.SearchQueryType.CODE,
    Refiner.SearchQueryType.AUTHOR,
    Refiner.SearchQueryType.SUBJECT_MATTER,
    Refiner.SearchQueryType.PHONE,
)

private enum class IncomingSearchMode {
    NUMBER,
    AUTHOR,
    SUBJECT,
    PHONE,
}

private fun IncomingSearchMode.toRefineType(): Refiner.SearchQueryType = when (this) {
    IncomingSearchMode.NUMBER -> Refiner.SearchQueryType.CODE
    IncomingSearchMode.AUTHOR -> Refiner.SearchQueryType.AUTHOR
    IncomingSearchMode.SUBJECT -> Refiner.SearchQueryType.SUBJECT_MATTER
    IncomingSearchMode.PHONE -> Refiner.SearchQueryType.PHONE
}

private fun refineToIncomingMode(type: Refiner.SearchQueryType): IncomingSearchMode =
    when (type) {
        Refiner.SearchQueryType.AUTHOR -> IncomingSearchMode.AUTHOR
        Refiner.SearchQueryType.SUBJECT_MATTER -> IncomingSearchMode.SUBJECT
        Refiner.SearchQueryType.PHONE -> IncomingSearchMode.PHONE
        else -> IncomingSearchMode.NUMBER
    }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IncomingApplicationsScreen(
    component: IIncomingApplicationsComponent,
    modifier: Modifier = Modifier,
) {
    val resource by component.incomingApplications.collectAsState()
    val refineState by component.refineState.collectAsState()
    val panel by component.masterScreenPanel.collectAsState()
    val selectedId by component.selectedItemGuid.collectAsState()

    var isSearchMode by rememberSaveable { mutableStateOf(false) }
    var searchQueryDraft by rememberSaveable { mutableStateOf(refineState.searchQuery) }
    var searchModeDraft by rememberSaveable { mutableStateOf(refineToIncomingMode(refineState.searchQueryType)) }
    val isTopBarLoading = resource is Resource.Loading ||
        (resource as? Resource.Success<*>)?.additionalLoading == true

    LaunchedEffect(refineState.searchQuery, refineState.searchQueryType, isSearchMode) {
        if (!isSearchMode) {
            searchQueryDraft = refineState.searchQuery
            searchModeDraft = refineToIncomingMode(refineState.searchQueryType)
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
        searchModeDraft = refineToIncomingMode(refineState.searchQueryType)
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
        title = "Входящие заявки",
        resource = resource,
        errorText = "Не удалось загрузить входящие заявки",
        notFoundText = "Входящие заявки не найдены",
        refineState = refineState,
        onRefresh = { component.fullRefresh() },
        onLoadMore = { component.loadMore() },
        onFilterChanged = { component.setRefineState(it) },
        itemId = { it.guid },
        isItemChanged = { old, new -> old.date != new.date || old.subjectMatter != new.subjectMatter },
        listItem = { item, _, onClick ->
            UniversalCardItem(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                onClick = onClick,
                title = item.number.orEmpty(),
                allowTitleLongPressCopy = true,
                subtitle = listOfNotNull(item.branch, item.organization)
                    .filter { it.isNotBlank() }
                    .joinToString(" · "),
                topRightPrimary = {
                    val t = item.type
                    if (!t.isNullOrBlank()) {
                        StatusBadge(
                            state = t,
                            styles = mapOf(
                                "Лендинг" to StatusStyle(DefaultColors.RainbowSkyBg, DefaultColors.RainbowSkyFg),
                            ),
                        )
                    }
                },
                bigText1 = item.subjectMatter.orEmpty(),
                bigText2 = listOfNotNull(item.model, item.clientData)
                    .filter { it.isNotBlank() }
                    .joinToString(" · "),
                bottomLeftText = item.date.orEmpty(),
                bottomRightText = item.author.orEmpty(),
            )
        },
        detailsContent = { item, onClose ->
            IncomingApplicationDetailsSheet(item = item, onBack = onClose)
        },
        filterScreen = { current, onDismiss, onApply ->
            RefineScreen(
                current = current,
                onBack = onDismiss,
                messageForUser = "Корректно работает только сортировка по Дате, остальные фильтры пока в разработке",
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
                            searchModeDraft = if (refineState.searchQueryType in INCOMING_TOPBAR_SEARCH_OPTIONS) {
                                refineToIncomingMode(refineState.searchQueryType)
                            } else {
                                IncomingSearchMode.NUMBER
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
                IncomingApplicationsSearchModeRow(
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
private fun IncomingApplicationsSearchModeRow(
    selected: IncomingSearchMode,
    onSelected: (IncomingSearchMode) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 12.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        FilterChip(
            selected = selected == IncomingSearchMode.NUMBER,
            onClick = { onSelected(IncomingSearchMode.NUMBER) },
            label = { Text("Номер", maxLines = 1, overflow = TextOverflow.Ellipsis) },
        )
        FilterChip(
            selected = selected == IncomingSearchMode.AUTHOR,
            onClick = { onSelected(IncomingSearchMode.AUTHOR) },
            label = { Text("Автор", maxLines = 1, overflow = TextOverflow.Ellipsis) },
        )
        FilterChip(
            selected = selected == IncomingSearchMode.SUBJECT,
            onClick = { onSelected(IncomingSearchMode.SUBJECT) },
            label = { Text("Суть обращения", maxLines = 1, overflow = TextOverflow.Ellipsis) },
        )
        FilterChip(
            selected = selected == IncomingSearchMode.PHONE,
            onClick = { onSelected(IncomingSearchMode.PHONE) },
            label = { Text("Телефон", maxLines = 1, overflow = TextOverflow.Ellipsis) },
        )
    }
}

private fun IncomingSearchMode.placeholder(): String = when (this) {
    IncomingSearchMode.NUMBER -> "Номер заявки…"
    IncomingSearchMode.AUTHOR -> "Автор…"
    IncomingSearchMode.SUBJECT -> "Суть обращения…"
    IncomingSearchMode.PHONE -> "Телефон…"
}
