package org.agregatcrm

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

import kotlinx.coroutines.CoroutineScope
import org.agregatcrm.domain.EventsController
import org.agregatcrm.domain.provideEventsController

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import compose.icons.FeatherIcons
import compose.icons.feathericons.Eye
import compose.icons.feathericons.EyeOff
import compose.icons.feathericons.RefreshCw
import kotlinx.coroutines.launch
import org.agregatcrm.feature.OrderDialog
import org.agregatcrm.models.EventItemDto
import org.agregatcrm.utils.requestEventsList

@Composable
fun App(scope: CoroutineScope, controller: EventsController = provideEventsController(scope)) {
    MaterialTheme(colorScheme = darkColorScheme()) {
        Surface(Modifier.fillMaxSize().safeDrawingPadding()) {
            EventsScreen(controller)
        }
    }
}
private var showDialogOrderBy = mutableStateOf(false)
private var showFilter = mutableStateOf(true)

@Composable
fun EventsScreen(controller: EventsController) {
    val events by controller.state.collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()

    // simple controls state
    var eventsListRequest by remember { requestEventsList }
//    var count by remember { mutableStateOf(5) }
//    var ncount by remember { mutableStateOf(50) }
//    var filterBy by remember { mutableStateOf("ПодразделениеКомпании") }
//    var filterVal by remember { mutableStateOf("Воронеж") }
    var isLoading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    var showDialogInternals by remember { mutableStateOf(showDialogOrderBy) }

    Column(Modifier.fillMaxSize()) {
//        Row(modifier = Modifier.fillMaxWidth().height(60.dp)) {
//
//        }
        // Top controls
        Box(Modifier.fillMaxSize().weight(if (showFilter.value) 5f else 1f)) {
            TopControls(
                count = eventsListRequest.count,
                onCountChange = { requestEventsList.value = requestEventsList.value.copy(count = it) },
                ncount = eventsListRequest.ncount,
                onNCountChange = { requestEventsList.value = requestEventsList.value.copy(ncount = it) },
                filterBy = eventsListRequest.filterBy,
                onFilterByChange = { requestEventsList.value = requestEventsList.value.copy(filterBy = it) },
                filterVal = eventsListRequest.filterVal,
                onFilterValChange = { requestEventsList.value = requestEventsList.value.copy(filterVal = it) },
                isLoading = isLoading,
                onRefresh = {
                    isLoading = true
                    error = null
                    scope.launch {
                        try {
                            controller.fullRefresh()
                        } catch (t: Throwable) {
                            error = t.message ?: "Unknown error"
                        } finally {
                            isLoading = false
                        }
                    }
                }
            )
        }

        if (error != null) {
            AssistChipRow("Ошибка: $error")
        }

        // Content
        if (events.isEmpty() && !isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Нет данных. Нажмите Обновить.", style = MaterialTheme.typography.bodyLarge)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().weight(10f),
                contentPadding = PaddingValues(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(events) { ev ->
                    EventCard(ev)
                }

                item {
                    // Load more by increasing ncount or count
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        OutlinedButton(
                            enabled = !isLoading,
                            onClick = {
                                requestEventsList.value = requestEventsList.value.copy(ncount = requestEventsList.value.ncount.also { it+10 })
                                scope.launch {
                                    isLoading = true
                                    controller.fullRefresh()
                                    isLoading = false
                                }
                            }
                        ) { Text("Загрузить ещё (+10)") }
                    }
                }
            }
        }

        if (showDialogInternals.value) {
            OrderDialog(
                currentBy = requestEventsList.value.orderBy,
                currentDir = requestEventsList.value.orderDir,
                onDismiss = { showDialogInternals.value = false },
                onApply = { by, dir ->
                    showDialogInternals.value = false
//                    ConfigRequest.value.orderBy = by
//                    ConfigRequest.value.orderDir = dir
                    requestEventsList.value = requestEventsList.value.copy(orderBy = by, orderDir = dir)
                    // optionally auto-refresh:
//                    onRefresh()
//                    onApply(by, dir)
                    isLoading = true
                    error = null
                    scope.launch {
                        try {
                            controller.fullRefresh()
                        } catch (t: Throwable) {
                            error = t.message ?: "Unknown error"
                        } finally {
                            isLoading = false
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
    onRefresh: () -> Unit
) {
    var showFullControlsInternal by remember { showFilter }
    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.SpaceBetween) {
        AnimatedVisibility(visible = showFullControlsInternal, modifier = Modifier.fillMaxSize().weight(4f)) {
            Column {
                Text("Фильтры и параметры", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(8.dp))
                Row(Modifier.fillMaxWidth().height(90.dp).background(Color.Red), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
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
                Row(Modifier.fillMaxWidth().height(90.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = filterBy,
                        onValueChange = onFilterByChange,
                        label = { Text("filterby") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = filterVal,
                        onValueChange = onFilterValChange,
                        label = { Text("filterval") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }
            }

        }

        Row(
            Modifier.fillMaxWidth().weight(1f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(onClick = onRefresh) {
                Icon(
                    imageVector = FeatherIcons.RefreshCw,
                    contentDescription = "Refresh"
                )
            }
            Column {
                AssistChipRow("сорт.:${requestEventsList.value.orderBy.wire}, по:${
                    requestEventsList.value.orderDir.label}")
                Text(text = "", fontSize = 8.sp, color = Color.DarkGray)
            }


            IconButton(onClick = { showFilter.value = !showFilter.value }) {
                Icon(
                    imageVector = if (showFullControlsInternal) FeatherIcons.Eye else FeatherIcons.EyeOff,
                    contentDescription = "Show or Hide"
                )
            }
        }
    }
    Divider(Modifier.padding(top = 12.dp))
}

@Composable
private fun AssistChipRow(text: String) {
    AssistChip(
        onClick = {
            showDialogOrderBy.value = !showDialogOrderBy.value
        },
        label = { Text(text, maxLines = 1, overflow = TextOverflow.Ellipsis) }
    )
}

@Composable
private fun NumberField(
    label: String,
    value: Int,
    onValue: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    var str by remember(value) { mutableStateOf(value.toString()) }
    OutlinedTextField(
        value = str,
        onValueChange = {
            str = it
            it.toIntOrNull()?.let(onValue)
        },
        label = { Text(label) },
        singleLine = true,
        modifier = modifier
    )
}

@Composable
fun EventCard(ev: EventItemDto) {
    var expanded by remember { mutableStateOf(false) }
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(Modifier.fillMaxWidth().padding(12.dp)) {
            // Header line: Номер, ВидСобытия, Состояние
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(
                    text = ev.number?.let { "№ $it" } ?: "Без номера",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                StatusBadge(ev.state ?: "")
            }

            Spacer(Modifier.height(4.dp))
            Text(ev.date ?: "", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

            Spacer(Modifier.height(8.dp))
            Text(
                ev.subject ?: (ev.content ?: ""),
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )
            KeyValueRow("Эпик", ev.baseDocument)
            HorizontalDivider(modifier = Modifier.fillMaxWidth(), thickness = 2.dp, color = Color.Gray)

            Spacer(Modifier.height(8.dp))

            KeyValueRow("Вид события", ev.eventType)
            KeyValueRow("Организация", ev.organization)
            KeyValueRow("Подразделение", ev.companyDepartment)
            KeyValueRow("Контрагент", ev.counterparty)


            KeyValueRow("Автор", ev.author)

            Spacer(Modifier.height(8.dp))
            TextButton(onClick = { expanded = !expanded }) {
                Text(if (expanded) "Скрыть детали" else "Показать детали")
            }

            if (expanded) {
                Spacer(Modifier.height(8.dp))
                EventDetails(ev)
            }
        }
    }
}

//private fun EventItemDto.пodразделениеКомпанииSafe(): String? = this.пodразделениеCompat()
//
//// helper because original field name is "ПодразделениеКомпании"
//private fun EventItemDto.пodразделениеCompat(): String? = this.подразделениеКомпании

@Composable
private fun StatusBadge(state: String) {
    if (state.isBlank()) return
    val bg = when (state.lowercase()) {
        "выполнено" -> MaterialTheme.colorScheme.primaryContainer
        "запланировано" -> MaterialTheme.colorScheme.tertiaryContainer
        else -> MaterialTheme.colorScheme.secondaryContainer
    }
    Surface(color = bg, shape = MaterialTheme.shapes.small) {
        Text(
            state,
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

@Composable
private fun EventDetails(ev: EventItemDto) {
    Column(
        Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (ev.users.isNotEmpty()) {
            Text("Пользователи", style = MaterialTheme.typography.titleSmall)
            ev.users.forEach {
                KeyValueRow("— ${it.user ?: "Неизвестно"}", it.role?.takeIf { r -> r.isNotBlank() } ?: it.responsible)
            }
        }
        if (ev.externalUsers.isNotEmpty()) {
            Text("Сторонние лица", style = MaterialTheme.typography.titleSmall)
            ev.externalUsers.forEach {
                KeyValueRow("— ${it.counterparty ?: ""}", it.contactInfo)
            }
        }
        if (ev.products.isNotEmpty()) {
            Text("Товары", style = MaterialTheme.typography.titleSmall)
            ev.products.forEach {
                KeyValueRow("— ${it.itemName ?: ""}", "Кол-во: ${it.quantity ?: "?"} ${it.unit ?: ""}")
            }
        }
//        if (ev.tasks.isNotEmpty()) {
//            Text("Задачи", style = MaterialTheme.typography.titleSmall)
//            ev.tasks.forEach {
//                KeyValueRow("— ${it.документ ?: ""}", it.комментарий)
//            }
//        }
//        if (ev.messages.isNotEmpty()) {
//            Text("Сообщения", style = MaterialTheme.typography.titleSmall)
//            ev.messages.forEach {
//                KeyValueRow("— ${it.автор ?: ""}", it.комментарий)
//            }
//        }
//        KeyValueRow("Документ основание", ev.документОснование)
//        KeyValueRow("Путь к файлам", ev.путьКФайлам)
        KeyValueRow("GUID", ev.guid)
    }
}

//@Preview
//@Composable
//private fun PreviewEventCard() {
//    MaterialTheme(colorScheme = darkColorScheme()) {
//        EventCard(
//            ev = EventItemDto(
//                number = "ВРН0000386",
//                date = "27.08.2025 18:17:34",
//                видСобытия = "Возврат",
//                тема = "ЗН № 0000477568, 01.01.2012 0:00:00, Двиг 1.8 бензин, КПП 6T40 (MH8), VIN ...",
//                состояние = "Запланировано",
//                author = "Черных Анастасия Андреевна",
//                контрагент = "ВРН",
//                организация = "ООО РЕМОНТ АКПП36"
//            )
//        )
//    }
//}