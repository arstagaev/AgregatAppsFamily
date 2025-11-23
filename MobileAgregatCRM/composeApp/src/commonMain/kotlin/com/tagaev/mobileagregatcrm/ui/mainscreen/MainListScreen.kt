package com.tagaev.mobileagregatcrm.ui.mainscreen

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
import compose.icons.feathericons.RefreshCw
import androidx.compose.runtime.saveable.rememberSaveable
import compose.icons.feathericons.AlertCircle
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.clickable
import androidx.compose.ui.text.style.TextAlign
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.format
import kotlinx.datetime.format.FormatStringsInDatetimeFormats
import kotlinx.datetime.format.byUnicodePattern
import com.tagaev.mobileagregatcrm.data.AppSettings
import com.tagaev.mobileagregatcrm.data.AppSettingsKeys
import com.tagaev.mobileagregatcrm.data.FilterState
import com.tagaev.mobileagregatcrm.data.remote.Resource
import com.tagaev.mobileagregatcrm.utils.DefaultValuesConst
import com.tagaev.mobileagregatcrm.feature.OrderDialog
import com.tagaev.mobileagregatcrm.feature.toFilterByOption
import com.tagaev.mobileagregatcrm.feature.toOrderByOption
import com.tagaev.mobileagregatcrm.feature.toOrderDirOption
import com.tagaev.mobileagregatcrm.models.EventItemDto
import com.tagaev.mobileagregatcrm.ui.custom.TextC
import com.tagaev.mobileagregatcrm.utils.TARGET_EVENT
import org.koin.compose.koinInject


@OptIn(FormatStringsInDatetimeFormats::class)
val format = LocalDateTime.Format {
    byUnicodePattern("dd.MM.yyyy")
}

private fun FilterState.sanitize(): FilterState = copy(
    count = if (count <= 0) DefaultValuesConst.COUNT else count,
    ncount = if (ncount < 0) 0 else ncount,
    filterBy = if (filterBy?.isBlank() == true) DefaultValuesConst.FILTER_BY else filterBy,
    filterVal = if (filterVal?.isBlank() == true) DefaultValuesConst.FILTER_VAL else filterVal?.trim(),
    orderBy = (orderBy ?: DefaultValuesConst.ORDER_BY),
    orderDir = (orderDir ?: DefaultValuesConst.ORDER_DIR),
    filtertype = (filtertype ?: DefaultValuesConst.FILTER_TYPE)
)

private var showDialogOrderBy = mutableStateOf(false)
private var showFilter = mutableStateOf(false)
private const val PREF_COMPACT_CARDS = "isCOMPACT_CARDS"

@Composable
fun MainListScreen(component: ListComponent) {
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
    var showErrorDialog by rememberSaveable { mutableStateOf(false) }
    var errorText by rememberSaveable { mutableStateOf("") }
    var showControlsDialog by rememberSaveable { mutableStateOf(false) }
//    val isBWTheme by remember { mutableStateOf(appSettings.getBool(PREF_BW_THEME, false)) }

    LaunchedEffect(compactCards) {
        appSettings.setBool(PREF_COMPACT_CARDS, compactCards)
    }

    LaunchedEffect(res) {
        when (val r = res) {
            is Resource.Error -> {
                errorText = r.causes ?: r.exception?.message ?: "Неизвестная ошибка"
                showErrorDialog = true
            }
            else -> Unit
        }
    }

    val onRefresh: () -> Unit = {
        isLoading = true
        error = null
        scope.launch {
            val gate = launch { kotlinx.coroutines.delay(1000) }
            try {
                component.setFiltersAndRefresh(filters.sanitize())
            } catch (t: Throwable) {
                error = t.message ?: "Unknown error"
                errorText = error ?: "Unknown error"
                showErrorDialog = true
            } finally {
                gate.join()
                isLoading = false
            }
        }
    }

    Column(Modifier.fillMaxSize()) {
        // Header (department + quick actions)
        run {
//            val cityLabel = remember(filters.filterVal) {
//                CITY_OPTIONS.find { it.value == (filters.filterVal ?: "") }?.label ?: (filters.filterVal ?: "")
//            }
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                TextC(
                    modifier = Modifier.fillMaxWidth(),
                    text = "Подразделение ${component.dept}",
                    style = MaterialTheme.typography.titleMedium,
                    textAlign = TextAlign.Center
                )
                Row(
                    Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    IconButton(onClick = {
                        if (res != Resource.Loading) {
                            onRefresh.invoke()
                        }
                    }, enabled = !isLoading) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                strokeWidth = 2.dp,
                                modifier = Modifier.size(18.dp)
                            )
                        } else {
                            Icon(
                                imageVector = FeatherIcons.RefreshCw,
                                contentDescription = "Refresh"
                            )
                        }
                    }
                    AssistChip(
                        onClick = { showDialogOrderBy.value = true },
                        label = { Text("Настройка показа", maxLines = 1, overflow = TextOverflow.Ellipsis) }
                    )
                    AssistChip(
                        onClick = { showControlsDialog = true },
                        label = { Text("Параметры") }
                    )
                }
            }
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
                                        modifier = Modifier.scale(0.8f),
                                        onClick = {
                                            isLoading = true
                                            error = null
                                            filters = filters.copy(ncount = 0)
                                            scope.launch {
                                                val gate = launch { kotlinx.coroutines.delay(1000) }
                                                try {
                                                    component.setFiltersAndRefresh(filters.sanitize())
                                                    // Scroll list to the top after refresh
                                                    listState.animateScrollToItem(0)
                                                } catch (t: Throwable) {
                                                    error = t.message ?: "Unknown error"
                                                    errorText = error ?: "Unknown error"
                                                    showErrorDialog = true
                                                } finally {
                                                    gate.join()
                                                    isLoading = false
                                                }
                                            }
                                        }
                                    ) {
                                        Text("В начало")
                                    }
                                    Text(
                                        modifier = Modifier,
                                        text = "Загружено: ${events.size}\n${appSettings.getString(AppSettingsKeys.LAST_UPDATE,"N/A")}",
                                        style = MaterialTheme.typography.bodySmall,
                                        textAlign = TextAlign.Center
                                    )
                                    // Right: load next page (+10)
                                    OutlinedButton(
                                        modifier = Modifier.scale(0.8f),
                                        onClick = { scope.launch { component.loadMore(DefaultValuesConst.COUNT) } }
                                    ) {
                                        Text("Загрузить ещё")
                                    }
                                }
                            }
                        }
                    }
                }
                is Resource.Error -> {
//                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
//                        Column(
//                            horizontalAlignment = Alignment.CenterHorizontally,
//                            verticalArrangement = Arrangement.spacedBy(12.dp)
//                        ) {
//                            Text(
//                                r.causes ?: r.exception?.message ?: "Неизвестная ошибка",
//                                style = MaterialTheme.typography.bodyLarge
//                            )
//                            Button(onClick = {
//                                scope.launch { component.fullRefresh() }
//                            }) {
//                                Text("Повторить")
//                            }
//                        }
//                    }
                }
            }
        }

        if (showControlsDialog) {
            ControlsDialog(
                compact = compactCards,
                onCompactChange = { compactCards = it },
                isLoading = isLoading,
                onOpenOrderDialog = { showDialogOrderBy.value = true },
                onDismiss = { showControlsDialog = false },
                onApply = {
                    showControlsDialog = false
                    onRefresh()
                }
            )
        }

        if (showDialogOrderBy.value) {
            OrderDialog(
                orderByOption = (filters.orderBy ?: DefaultValuesConst.ORDER_BY).toOrderByOption(),
                currentDir = (filters.orderDir ?: DefaultValuesConst.ORDER_DIR).toOrderDirOption(),
                currentFilterVal = (filters.filterVal ?: "Состояние").toFilterByOption(),
                onDismiss = { showDialogOrderBy.value = false },
                onApply = { orderBy, dir, filterVal ->
                    filters = filters.copy(orderBy = orderBy.wire, orderDir = dir.wire, filterVal = filterVal.wire)
                    isLoading = true
                    error = null
                    scope.launch {
                        val gate = launch { kotlinx.coroutines.delay(1000) }
                        try {
                            component.setFiltersAndRefresh(filters.sanitize())
                        } catch (t: Throwable) {
                            error = t.message ?: "Unknown error"
                            errorText = error ?: "Unknown error"
                            showErrorDialog = true
                        } finally {
                            gate.join()
                            isLoading = false
                            showDialogOrderBy.value = false
                        }
                    }
                }
            )
        }

        if (showErrorDialog) {
            AlertDialog(
                onDismissRequest = { showErrorDialog = false },
                icon = { Icon(FeatherIcons.AlertCircle, contentDescription = null) },
                title = { Text("Ошибка запроса") },
                text = { Text(errorText.ifBlank { "Неизвестная ошибка" }) },
                confirmButton = {
                    TextButton(onClick = { showErrorDialog = false }) { Text("OK") }
                }
            )
        }
    }
}

@Composable
private fun ControlsDialog(
    compact: Boolean,
    onCompactChange: (Boolean) -> Unit,
    isLoading: Boolean,
    onOpenOrderDialog: () -> Unit,
    onDismiss: () -> Unit,
    onApply: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Фильтры и параметры") },
        text = {
            Column(Modifier.fillMaxWidth()) {
                Spacer(Modifier.height(4.dp))
                Row(
                    Modifier
                        .fillMaxWidth()
                        .height(24.dp)
                        .padding(horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        "Компактные карточки",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Switch(
                        checked = compact,
                        onCheckedChange = onCompactChange,
                        enabled = !isLoading,
                        modifier = Modifier.scale(0.45f)
                    )
                }
                Spacer(Modifier.height(8.dp))
                AssistChip(
                    onClick = onOpenOrderDialog,
                    label = { Text("Настройка показа") }
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onApply, enabled = !isLoading) { Text("Применить") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Отмена") }
        }
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
                TextC(
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
            TextC(
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


