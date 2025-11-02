package com.tagaev.mobileagregatcrm.ui.mainscreen

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.layout.Column
import androidx.compose.ui.draw.scale
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import compose.icons.FeatherIcons
import compose.icons.feathericons.Eye
import compose.icons.feathericons.EyeOff
import compose.icons.feathericons.RefreshCw
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.clickable
import androidx.compose.ui.text.style.TextAlign
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.format
import kotlinx.datetime.format.FormatStringsInDatetimeFormats
import kotlinx.datetime.format.byUnicodePattern
import com.tagaev.mobileagregatcrm.data.AppSettings
import com.tagaev.mobileagregatcrm.data.FilterState
import com.tagaev.mobileagregatcrm.data.remote.Resource
import com.tagaev.mobileagregatcrm.utils.CenteredNoPaddingOutlinedField
import com.tagaev.mobileagregatcrm.utils.DefaultConfig
import com.tagaev.mobileagregatcrm.feature.OrderDialog
import com.tagaev.mobileagregatcrm.feature.toOrderByOption
import com.tagaev.mobileagregatcrm.feature.toOrderDirOption
import com.tagaev.mobileagregatcrm.models.EventItemDto
import com.tagaev.mobileagregatcrm.utils.TARGET_EVENT
import org.koin.compose.koinInject


@OptIn(FormatStringsInDatetimeFormats::class)
val format = LocalDateTime.Format {
    byUnicodePattern("dd.MM.yyyy")
}

private fun FilterState.sanitize(): FilterState = copy(
    count = if (count <= 0) 10 else count,
    ncount = if (ncount < 0) 0 else ncount,
    filterBy = if (filterBy?.isBlank() == true) DefaultConfig.FILTER_BY else filterBy,
    filterVal = if (filterVal?.isBlank() == true) DefaultConfig.FILTER_VAL else filterVal,
    orderBy = (orderBy ?: "Дата"),
    orderDir = (orderDir ?: "desc")
)


private var showDialogOrderBy = mutableStateOf(false)
private var showFilter = mutableStateOf(true)
private const val PREF_COMPACT_CARDS = "isCOMPACT_CARDS"
//private const val PREF_BW_THEME = "isBW_THEME"


@Composable
fun EventsScreen(component: ListComponent) {
    val scope = rememberCoroutineScope()
    val appSettings = koinInject<AppSettings>()

    val res by component.resource.collectAsState()

    val listState = rememberLazyListState()
    var eventsCache by remember { mutableStateOf<List<EventItemDto>>(emptyList()) }

    // simple controls state
    var isLoading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var filters by remember { mutableStateOf(appSettings.loadFilters().sanitize()) }

    var compactCards by remember { mutableStateOf(appSettings.getBool(PREF_COMPACT_CARDS, false)) }
//    val isBWTheme by remember { mutableStateOf(appSettings.getBool(PREF_BW_THEME, false)) }

//    LaunchedEffect(Unit) {
//        component.setFiltersAndRefresh(filters)
//    }
    LaunchedEffect(compactCards) {
        appSettings.setBool(PREF_COMPACT_CARDS, compactCards)
    }

    Column(Modifier.fillMaxSize()) {
        // Top controls
        Box(Modifier.fillMaxSize().weight(if (showFilter.value) 5f else 0.7f)) {
            TopControls(
                count = filters.count,
                onCountChange = { filters = filters.copy(count = it) },
                ncount = filters.ncount,
                onNCountChange = { filters = filters.copy(ncount = it) },
                filterBy = filters.filterBy ?: DefaultConfig.FILTER_BY,
                onFilterByChange = { filters = filters.copy(filterBy = it) },
                filterVal = filters.filterVal ?: DefaultConfig.FILTER_VAL,
                onFilterValChange = { filters = filters.copy(filterVal = it) },
                isLoading = isLoading,
                onRefresh = {
                    isLoading = true
                    error = null
                    scope.launch {
                        try {
                            component.setFiltersAndRefresh(filters.sanitize())
                        } catch (t: Throwable) {
                            error = t.message ?: "Unknown error"
                        } finally {
                            isLoading = false
                        }
                    }
                },
                onOpenOrderDialog = { showDialogOrderBy.value = true },
                compact = compactCards,
                onCompactChange = { compactCards = it }
            )
        }

        // Content
        Row(modifier = Modifier.fillMaxSize().weight(10f)) {
            when (val r = res) {
                is Resource.Loading -> {
                    if (eventsCache.isNotEmpty()) {
                        LazyColumn(
                            state = listState,
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(12.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(eventsCache) { ev -> EventCard(ev, component, compact = compactCards) }
                            item {
                                Row(
                                    Modifier.fillMaxWidth().padding(vertical = 8.dp),
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    CircularProgressIndicator()
                                }
                            }
                        }
                    } else {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                CircularProgressIndicator()
                                Spacer(Modifier.height(12.dp))
                                Text("Загрузка...")
                            }
                        }
                    }
                }
                is Resource.Success -> {
                    val events = r.data
                    eventsCache = events
                    if (events.isEmpty()) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("Нет данных", style = MaterialTheme.typography.bodyLarge)
                        }
                    } else {
                        LazyColumn(
                            state = listState,
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(12.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(events) { ev ->
                                EventCard(ev, component, compact = compactCards)
                            }
                            item {
                                Row(
                                    Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Left: go to start (reset pagination)
                                    OutlinedButton(
                                        onClick = {
                                            isLoading = true
                                            error = null
                                            filters = filters.copy(ncount = 0)
                                            scope.launch {
                                                try {
                                                    component.setFiltersAndRefresh(filters.sanitize())
                                                    // Scroll list to the top after refresh
                                                    listState.animateScrollToItem(0)
                                                } catch (t: Throwable) {
                                                    error = t.message ?: "Unknown error"
                                                } finally {
                                                    isLoading = false
                                                }
                                            }
                                        }
                                    ) {
                                        Text("В начало")
                                    }

                                    // Right: load next page (+10)
                                    OutlinedButton(
                                        onClick = { scope.launch { component.loadMore(10) } }
                                    ) {
                                        Text("Загрузить ещё (+10)")
                                    }
                                }
                            }
                        }
                    }
                }
                is Resource.Error -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                r.causes ?: r.exception?.message ?: "Неизвестная ошибка",
                                style = MaterialTheme.typography.bodyLarge
                            )
                            Button(onClick = {
                                scope.launch { component.fullRefresh() }
                            }) {
                                Text("Повторить")
                            }
                        }
                    }
                }
            }
        }

        if (showDialogOrderBy.value) {
            OrderDialog(
                currentBy = (filters.orderBy ?: "Дата").toOrderByOption(),
                currentDir = (filters.orderDir ?: "desc").toOrderDirOption(),
                onDismiss = { showDialogOrderBy.value = false },
                onApply = { by, dir ->
                    filters = filters.copy(orderBy = by.wire, orderDir = dir.wire)
                    isLoading = true
                    error = null
                    scope.launch {
                        try {
                            component.setFiltersAndRefresh(filters.sanitize())
                        } catch (t: Throwable) {
                            error = t.message ?: "Unknown error"
                        } finally {
                            isLoading = false
                            showDialogOrderBy.value = false
                        }
                    }
                }
            )
        }
    }
}

@Composable
private fun TopControls(
    count: Int,
    onCountChange: (Int) -> Unit,
    ncount: Int,
    onNCountChange: (Int) -> Unit,
    filterBy: String,
    onFilterByChange: (String) -> Unit,
    filterVal: String,
    onFilterValChange: (String) -> Unit,
    isLoading: Boolean,
    onRefresh: () -> Unit,
    onOpenOrderDialog: () -> Unit,
    compact: Boolean,
    onCompactChange: (Boolean) -> Unit
) {
    var showFullControlsInternal by remember { showFilter }

    Column(Modifier.fillMaxSize()) {
        AnimatedVisibility(
            visible = showFullControlsInternal
        ) {
            Column {
                Spacer(Modifier.height(8.dp))
                Text("Фильтры и параметры", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(8.dp))
                Row(
                    Modifier.fillMaxWidth().height(50.dp).padding(vertical = 6.dp, horizontal = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    NumberField(
                        label = "count",
                        value = count,
                        onValue = onCountChange,
                        modifier = Modifier.weight(1f)
                    )
                    NumberField(
                        label = "ncount",
                        value = ncount,
                        onValue = onNCountChange,
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(
                    Modifier.fillMaxWidth().height(50.dp).padding(vertical = 6.dp, horizontal = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CenteredNoPaddingOutlinedField(
                        value = filterBy,
                        onValueChange = onFilterByChange,
                        labelText = "filterby",            // shows floating label
                        placeholderText = "filterby",     // optional
                        modifier = Modifier.height(40.dp).weight(1f)                // add your .weight/.height/.fillMaxWidth as needed
                    )
                    CenteredNoPaddingOutlinedField(
                        value = filterVal,
                        onValueChange = onFilterValChange,
                        labelText = "filterval",            // shows floating label
                        placeholderText = "filterval",     // optional
                        modifier = Modifier.height(40.dp).weight(1f)                // add your .weight/.height/.fillMaxWidth as needed
                    )
                }
                Row(
                    Modifier.height(24.dp).padding(horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Компактные карточки", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Switch(
                        checked = compact,
                        onCheckedChange = onCompactChange,
                        enabled = !isLoading,
                        modifier = Modifier.scale(0.85f)
                    )
                }
                // Toggle row (red background) for compact cards

            }
        }
        Row {
            Column(Modifier.padding(bottom = 3.dp)) {
                if (!showFullControlsInternal) {
                    Text(
                        modifier = Modifier.fillMaxWidth(),
                        text = "$filterBy, г.$filterVal",
                        style = MaterialTheme.typography.titleMedium,
                        textAlign = TextAlign.Center
                    )
                }
                Row(
                    Modifier.fillMaxWidth().height(50.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    IconButton(onClick = onRefresh) {
                        Icon(
                            imageVector = FeatherIcons.RefreshCw,
                            contentDescription = "Refresh"
                        )
                    }
                    AssistChip(
                        onClick = onOpenOrderDialog,
                        label = { Text("Сортировка …", maxLines = 1, overflow = TextOverflow.Ellipsis) }
                    )
                    IconButton(
                        onClick = { showFilter.value = !showFilter.value }
                    ) {
                        Icon(
                            imageVector = if (showFullControlsInternal) FeatherIcons.Eye else FeatherIcons.EyeOff,
                            contentDescription = "Show or Hide"
                        )
                    }
                }
            }
        }

    }
}

@Composable
private fun NumberField(
    label: String,
    value: Int,
    onValue: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    var str by remember(value) { mutableStateOf(value.toString()) }

    CenteredNoPaddingOutlinedField(
        value = str,
        onValueChange = {
            str = it
            it.toIntOrNull()?.let(onValue)
        },
        labelText = label,            // shows floating label
        modifier = modifier                // add your .weight/.height/.fillMaxWidth as needed
    )
}

@Composable
fun EventCard(
    ev: EventItemDto,
    component: ListComponent,
    compact: Boolean = false,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                TARGET_EVENT.value = ev
                ev.number?.let { component.openDetails(it, ev) }
            },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(Modifier.fillMaxWidth().padding(12.dp)) {
            // Header: number + status at right
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = ev.number?.let { "№ $it" } ?: "Без номера",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                StatusBadge(ev.state ?: "")
            }

            Spacer(Modifier.height(4.dp))
            Text(
                ev.date?.format(format) ?: "",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(8.dp))
            Text(
                ev.subject ?: (ev.content ?: ""),
                style = MaterialTheme.typography.bodyLarge,
                maxLines = if (compact) 2 else 3,
                overflow = TextOverflow.Ellipsis
            )

            if (!compact) {
                KeyValueRow("Эпик", ev.baseDocument)
                HorizontalDivider(modifier = Modifier.fillMaxWidth(), thickness = 2.dp, color = MaterialTheme.colorScheme.outlineVariant)
                Spacer(Modifier.height(8.dp))
                KeyValueRow("Вид события", ev.eventType)
                KeyValueRow("Организация", ev.organization)
                KeyValueRow("Подразделение", ev.companyDepartment)
                KeyValueRow("Контрагент", ev.counterparty)
                KeyValueRow("Автор", ev.author)
            }
        }
    }
}

@Composable
private fun StatusBadge(state: String) {
    if (state.isBlank()) return
    val lower = state.lowercase()
    val bg = when (lower) {
        "выполнено", "завершено" -> Color(0xFF4CAF50) // green
        "выполняется"            -> Color(0xFFFF9800) // orange
        "запланировано"          -> Color(0xFFFFEB3B) // yellow
        else -> MaterialTheme.colorScheme.secondaryContainer
    }
    val fg = Color.Black
    Surface(color = bg, shape = MaterialTheme.shapes.small) {
        Text(
            state,
            color = fg,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelMedium
        )
    }
}

@Composable
private fun KeyValueRow(key: String, value: String?, textSize: TextUnit = TextUnit.Unspecified) {
    if (value.isNullOrBlank()) return
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "$key:",
            fontWeight = FontWeight.SemiBold,
            maxLines = 2, // Limit the text to 2 lines
            overflow = TextOverflow.Ellipsis, // Apply ellipsis if it overflows
            fontSize = textSize
        )
        Text(
            modifier = Modifier.basicMarquee(),
            text = value,
            maxLines = 1,
            overflow = TextOverflow.Visible,
            fontSize = textSize
        )
    }
}


