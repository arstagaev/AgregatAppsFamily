package com.tagaev.trrcrm.ui.events

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.toMutableStateList
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.tagaev.trrcrm.data.remote.Resource
import com.tagaev.trrcrm.domain.OptionChipsScrollingRow
import com.tagaev.trrcrm.domain.Refiner
import com.tagaev.trrcrm.models.EventItemDto
import com.tagaev.trrcrm.push.rememberNotificationPermissionRequester
import com.tagaev.trrcrm.domain.displayNameRu
import com.tagaev.trrcrm.domain.TreeRootResolvedDocument
import com.tagaev.trrcrm.ui.custom.SessionTrrImage
import com.tagaev.trrcrm.ui.custom.SearchIconButtonWithIndicator
import com.tagaev.trrcrm.ui.custom.TextC
import com.tagaev.trrcrm.ui.mainscreen.StatusBadge
import com.tagaev.trrcrm.ui.mainscreen.format
import com.tagaev.trrcrm.ui.master_screen.MasterPanel
import com.tagaev.trrcrm.ui.master_screen.MasterScreen
import com.tagaev.trrcrm.ui.master_screen.RefineSection
import com.tagaev.trrcrm.ui.master_screen.RefineScreen
import com.tagaev.trrcrm.ui.master_screen.TreeRootDocumentDetailsSheet
import com.tagaev.trrcrm.ui.master_screen.models.MessageModel
import com.tagaev.trrcrm.ui.root.LocalAppSnackbar
import com.tagaev.trrcrm.utils.formatDDMMYYYY
import compose.icons.FeatherIcons
import compose.icons.feathericons.Filter
import compose.icons.feathericons.RefreshCw
import compose.icons.feathericons.Search
import compose.icons.feathericons.X
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.datetime.format
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource

private const val EVENTS_LOADING_MIN_DELAY_MS = 900L
private var eventsLoadingShownThisSession = false
private val EVENTS_TOPBAR_SEARCH_OPTIONS = listOf(
    Refiner.SearchQueryType.TOPIC,
    Refiner.SearchQueryType.CODE,
    Refiner.SearchQueryType.AUTHOR,
    Refiner.SearchQueryType.COUNTERPARTY,
    Refiner.SearchQueryType.AUTO,
)

private fun Refiner.SearchQueryType.eventsSearchLabel(): String = when (this) {
    Refiner.SearchQueryType.TOPIC -> "Тема"
    Refiner.SearchQueryType.CODE -> "Номер"
    Refiner.SearchQueryType.AUTHOR -> "Автор"
    Refiner.SearchQueryType.COUNTERPARTY -> "Контрагент"
    Refiner.SearchQueryType.AUTO -> "Автомобиль"
    Refiner.SearchQueryType.MANAGER -> "Менеджер"
    Refiner.SearchQueryType.MASTER -> "Мастер"
    Refiner.SearchQueryType.KIT_CHARACTERISTIC -> "Хар. комплекта"
    Refiner.SearchQueryType.LICENSE_PLATE -> "Госномер"
    Refiner.SearchQueryType.VIN_NUMBER -> "VIN"
    Refiner.SearchQueryType.FIX_TYPE -> "Вид ремонта"
    Refiner.SearchQueryType.CLIENT -> "Заказчик"
    Refiner.SearchQueryType.ROUTE -> "Маршрут"
    Refiner.SearchQueryType.CARRIER -> "Перевозчик"
}

@Composable
fun EventsScreen(
    component: IEventsComponent,
    modifier: Modifier = Modifier
) {
    val resource by component.events.collectAsState()
    val refineState by component.refineState.collectAsState()
    val panel by component.masterScreenPanel.collectAsState()
    val selectedId by component.selectedItemGuid.collectAsState()
    var isSearchMode by rememberSaveable { mutableStateOf(false) }
    var searchQueryDraft by rememberSaveable { mutableStateOf(refineState.searchQuery) }
    var searchTypeDraft by rememberSaveable { mutableStateOf(refineState.searchQueryType) }

    val scope = rememberCoroutineScope()
    val showSnackbar = LocalAppSnackbar.current
    val linkedDocuments = remember { emptyList<TreeRootResolvedDocument>().toMutableStateList() }
    var isResolvingBaseDocument by remember { mutableStateOf(false) }
    val requestNotificationPermission =
        rememberNotificationPermissionRequester { granted ->

            if (!granted) {
                showSnackbar("Необходимо разрешение на уведомления")
            }
        }

    LaunchedEffect(selectedId) {
        linkedDocuments.clear()
        isResolvingBaseDocument = false
    }

    LaunchedEffect(Unit) {
        requestNotificationPermission()
    }

    val isEventsLoading = when (val state = resource) {
        is Resource.Loading -> true
        is Resource.Success -> state.additionalLoading
        is Resource.Error -> false
    }
    val isTopBarLoading = resource is Resource.Loading ||
            (resource as? Resource.Success<*>)?.additionalLoading == true

    LaunchedEffect(refineState.searchQuery, refineState.searchQueryType, isSearchMode) {
        if (!isSearchMode) {
            searchQueryDraft = refineState.searchQuery
            searchTypeDraft = if (refineState.searchQueryType in EVENTS_TOPBAR_SEARCH_OPTIONS) {
                refineState.searchQueryType
            } else {
                Refiner.SearchQueryType.TOPIC
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
    val clearSearchAndExit: () -> Unit = {
        searchQueryDraft = ""
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
    val sessionLoadingImage = remember { SessionTrrImage.get() }
    var isLoadingOverlayVisible by remember { mutableStateOf(false) }
    var wasLoading by remember { mutableStateOf(false) }
    var loadingSessionId by remember { mutableStateOf(0) }
    var minDelayPassed by remember { mutableStateOf(true) }
    var shouldShowOverlayForCurrentLoad by remember { mutableStateOf(false) }

    LaunchedEffect(isEventsLoading) {
        if (isEventsLoading && !wasLoading && !eventsLoadingShownThisSession) {
            isLoadingOverlayVisible = true
            minDelayPassed = false
            loadingSessionId += 1
            shouldShowOverlayForCurrentLoad = true
            eventsLoadingShownThisSession = true
        }

        if (!isEventsLoading && wasLoading && shouldShowOverlayForCurrentLoad && minDelayPassed) {
            isLoadingOverlayVisible = false
            shouldShowOverlayForCurrentLoad = false
        }
        wasLoading = isEventsLoading
    }

    LaunchedEffect(loadingSessionId) {
        if (loadingSessionId == 0) return@LaunchedEffect
        delay(EVENTS_LOADING_MIN_DELAY_MS)
        minDelayPassed = true
        if (!isEventsLoading && shouldShowOverlayForCurrentLoad) {
            isLoadingOverlayVisible = false
            shouldShowOverlayForCurrentLoad = false
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        MasterScreen(
            title = "События",
            resource = resource,
            errorText = "Не удалось загрузить события",
            notFoundText = "События не найдены",
            refineState = refineState,
            onRefresh = { component.fullRefresh() },
            onLoadMore = { component.loadMore() },
            onFilterChanged = { component.setRefineState(it) },

            itemId = { it.guid.toString() },
            isItemChanged = { old, new -> old.messages.size != new.messages.size },

            listItem = { order, isChanged, onClick ->
                EventCard(
                    ev = order,
                    onClick = onClick
                )
            },

            // Full-screen details content (not bottom-sheet)
            detailsContent = { ev, onClose ->
                val onOpenBaseDocument: (String) -> Unit = { rawBaseDocument ->
                    scope.launch {
                        isResolvingBaseDocument = true
                        try {
                            when (val resolved = component.resolveBaseDocument(rawBaseDocument)) {
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
                    EventDetailsSheet(
                        event = ev,
                        onBack = onNestedBack,
                        onOpenBaseDocument = onOpenBaseDocument,
                        onSendMessage = { message, onResult ->
                            val number = ev.number.orEmpty()
                            val date = ev.date?.format(formatDDMMYYYY).orEmpty()
                            scope.launch {
                                component.pickedEvent = ev
                                val err = component.sendMessage(itemNumber = number, itemDate = date, message = message)
                                if (err == null) {
                                    component.addLocalMessage(ev.guid.toString(), message = MessageModel(author = "я", text = message))
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
                    sections = setOf(
                        RefineSection.STATUS,
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
                    IconButton(
                        onClick = clearSearchAndExit,
                        enabled = !isTopBarLoading
                    ) {
                        Icon(FeatherIcons.X, contentDescription = "Закрыть поиск")
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
                            placeholder = { Text("Поиск события") },
                            singleLine = true,
                            enabled = !isTopBarLoading,
                            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(imeAction = ImeAction.Search),
                            keyboardActions = androidx.compose.foundation.text.KeyboardActions(onSearch = { applySearch() })
                        )
                    }
                }
                panel == MasterPanel.Details && linkedDocTitle != null -> {
                    { Text("Документ Основание: $linkedDocTitle", modifier = Modifier.padding(horizontal = 10.dp)) }
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
                                searchTypeDraft = if (refineState.searchQueryType in EVENTS_TOPBAR_SEARCH_OPTIONS) {
                                    refineState.searchQueryType
                                } else {
                                    Refiner.SearchQueryType.TOPIC
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
                    EventsSearchTypeRow(
                        selected = searchTypeDraft,
                        onSelected = { searchTypeDraft = it }
                    )
                }
            } else null,
            onDetailsBack = handleDetailsBack,

            modifier = Modifier.fillMaxSize()
        )

        if (isResolvingBaseDocument) {
            AlertDialog(
                onDismissRequest = {},
                title = { Text("Пожалуйста, подождите") },
                text = { Text("ищем документ основание....") },
                confirmButton = {}
            )
        }

        if (isLoadingOverlayVisible) {
            EventsLoadingImageOverlay(
                image = sessionLoadingImage,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

@Composable
private fun EventsLoadingImageOverlay(
    image: DrawableResource,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.background(MaterialTheme.colorScheme.surface),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(image),
            contentDescription = "Загрузка событий",
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
    }
}

@Composable
private fun EventsSearchTypeRow(
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
                options = EVENTS_TOPBAR_SEARCH_OPTIONS,
                selected = selected,
                onSelect = onSelected,
                labelFor = { it.eventsSearchLabel() }
            )
        }
    }
}


@Composable
fun EventCard(
    ev: EventItemDto,
    onClick: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(3.dp)
        ) {
            // Header: number + status at right
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextC(
                    text = ev.number?.let { "№ $it" } ?: "Без номера",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    allowLinkTap = false,
                    allowLongPressCopy = true,
                )
                StatusBadge(ev.state.orEmpty())
            }

            Spacer(Modifier.height(2.dp))

            Text(
                ev.subject ?: (ev.content ?: ""),
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            // Compact meta block
            val hasMeta =
                !ev.baseDocument.isNullOrBlank() ||
                        !ev.eventType.isNullOrBlank() ||
                        !ev.organization.isNullOrBlank() ||
                        !ev.companyDepartment.isNullOrBlank() ||
                        !ev.counterparty.isNullOrBlank() ||
                        !ev.author.isNullOrBlank() ||
                        !ev.modifiedDate.isNullOrBlank()

            if (hasMeta) {
                Spacer(Modifier.height(2.dp))
                HorizontalDivider(
                    modifier = Modifier.fillMaxWidth(),
                    thickness = 1.dp,
                    color = MaterialTheme.colorScheme.outlineVariant
                )
                Spacer(Modifier.height(2.dp))

                // First row: Эпик + Вид события
//                Row(
//                    modifier = Modifier.fillMaxWidth(),
//                    horizontalArrangement = Arrangement.spacedBy(12.dp)
//                ) {
//
//                }

                // Second row: Организация + Подразделение
                if (!ev.organization.isNullOrBlank() || !ev.companyDepartment.isNullOrBlank()) {
                    Spacer(Modifier.height(1.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        EventMetaColumn(
                            label = "Организация",
                            value = ev.organization ?: "undefined",
                            modifier = Modifier.weight(1f, fill = true)
                        )
                        EventMetaColumn(
                            label = "Подразделение",
                            value = ev.companyDepartment?: "undefined",
                            modifier = Modifier.weight(1f, fill = true)
                        )

                        EventMetaColumn(
                            label = "Эпик",
                            value = ev.baseDocument?: "undefined",
                            modifier = Modifier.weight(1f, fill = true)
                        )
                        EventMetaColumn(
                            label = "Вид события",
                            value = ev.eventType?: "undefined",
                            modifier = Modifier.weight(1f, fill = true)
                        )
                    }
                }
                // Fourth row: Автор
                if (!ev.author.isNullOrBlank() && !ev.users.isNullOrEmpty()) {
                    Spacer(Modifier.height(1.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        EventMetaColumn(
                            label = "Автор",
                            value = ev.author,
                            modifier = Modifier.weight(1f, fill = true)
                        )
                        // Контрагент
                        ev.counterparty?.takeIf { it.isNotBlank() }?.let { counterparty ->
//                            Spacer(Modifier.height(1.dp))
                            EventMetaColumn(
                                label = "Контрагент",
                                value = counterparty,
                                modifier = Modifier.weight(1f, fill = true)
                            )
                        }
                        EventMetaColumn(
                            label = "Количество участников",
                            value = "${ev.users.size}",
                            modifier = Modifier.weight(1f, fill = true)
                        )
                    }
                }
            }

            val createdText = ev.date?.format(format)
            val modifiedText = ev.modifiedDate

            if (!createdText.isNullOrBlank() || !modifiedText.isNullOrBlank()) {
                Spacer(Modifier.height(4.dp))
                HorizontalDivider(
                    modifier = Modifier.fillMaxWidth(),
                    thickness = 0.5.dp,
                    color = MaterialTheme.colorScheme.outlineVariant
                )
                Spacer(Modifier.height(2.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    createdText?.let {
                        Text(
                            text = "Создано: $it",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    modifiedText?.takeIf { it.isNotBlank() }?.let {
                        Text(
                            text = "Изм.: $it",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EventMetaColumn(
    label: String,
    value: String?,
    modifier: Modifier = Modifier
) {
    if (value.isNullOrBlank()) return
    Column(modifier = modifier) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.labelSmall,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis
        )
    }
}
