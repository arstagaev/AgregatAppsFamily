package org.agregatcrm.ui

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import compose.icons.FeatherIcons
import compose.icons.feathericons.Eye
import compose.icons.feathericons.EyeOff
import compose.icons.feathericons.RefreshCw
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.format
import kotlinx.datetime.format.FormatStringsInDatetimeFormats
import kotlinx.datetime.format.byUnicodePattern
import kotlinx.datetime.format.char
import org.agregatcrm.data.local.CMPPrefs
import org.agregatcrm.data.local.SharedprefVariants
import org.agregatcrm.data.remote.Resource
import org.agregatcrm.feature.OrderDialog
import org.agregatcrm.feature.toOrderByOption
import org.agregatcrm.feature.toOrderDirOption
import org.agregatcrm.models.EventItemDto
import org.agregatcrm.utils.CURRENT_SCREEN
import org.agregatcrm.utils.CurrentScreen
import org.agregatcrm.utils.TARGET_EVENT
import org.agregatcrm.utils.requestEventsList
import org.koin.compose.koinInject

@OptIn(FormatStringsInDatetimeFormats::class)
val format = LocalDateTime.Format {
    byUnicodePattern("dd.MM.yyyy")
}

@Composable
fun App(scope: CoroutineScope, controller: EventsController = provideEventsController(scope)) {
    var curScreen by remember { CURRENT_SCREEN }
    MaterialTheme(colorScheme = darkColorScheme(primary = Color(0xff546ff3))) {
        Surface(Modifier.fillMaxSize().safeDrawingPadding()) {
            Scaffold(
                bottomBar = {
                    AppBottomNavBar { selected ->
                        CURRENT_SCREEN.value = selected.screen
                        println("Selected: ${selected.label}")
                    }
                }
            ) { innerPadding ->
                when(curScreen) {
                    CurrentScreen.List -> EventsScreen(controller)
                    CurrentScreen.Details -> EventDetailsScreen(
                        event = TARGET_EVENT.value,
                        onSendMessageClick = { message ->
                            println("message: ${message}")
                            if (TARGET_EVENT.value.date != null && TARGET_EVENT.value.number != null) {
                                controller.sendMessage(
                                    number = TARGET_EVENT.value.number!!,
                                    date = TARGET_EVENT.value.date!!.format(format),
                                    message = message
                                )
                            } else {
                                println("ERROR!!! TARGET_EVENT is null MB ${TARGET_EVENT.value.toString()}")
                            }
//                            TARGET_EVENT.value?.let {
//
//                                controller.sendMessage(
//                                    number = it.number,
//                                    date = TARGET_EVENT.value.date,
//                                    message = message
//                                )
//                            }

                        })
                    CurrentScreen.Favorites -> FavoritesScreen()
                }
            }
        }
    }
}
private var showDialogOrderBy = mutableStateOf(false)
private var showFilter = mutableStateOf(true)

@Composable
fun EventsScreen(controller: EventsController) {
//    val events by controller.resource.collectAsState(initial = emptyList())
    val prefs = koinInject<CMPPrefs>()
    val res by controller.resource.collectAsState()
    val scope = rememberCoroutineScope()

    // simple controls state
    var eventsListRequest by remember { requestEventsList }
    var isLoading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    var showDialogInternals by remember { mutableStateOf(showDialogOrderBy) }

    LaunchedEffect(Unit) {
        showFilter.value = prefs.getBool(SharedprefVariants.isSHOW_TOP_CONTROLS.key,true) ?: true

        requestEventsList.value = requestEventsList.value.copy(
            count = prefs.getInt(SharedprefVariants.FILTER_COUNT.key,20),
            ncount = prefs.getInt(SharedprefVariants.FILTER_N_COUNT.key,0),
            filterBy = prefs.getString(SharedprefVariants.FILTER_BY.key,SharedprefVariants.FILTER_BY.defaultValue.toString()) ?: "",
            filterVal = prefs.getString(SharedprefVariants.FILTER_VAL.key,SharedprefVariants.FILTER_VAL.defaultValue.toString()) ?: "",

            orderBy =  prefs.getString(SharedprefVariants.ORDER_BY.key,SharedprefVariants.ORDER_BY.defaultValue.toString()).toOrderByOption(),
            orderDir = prefs.getString(SharedprefVariants.ORDER_DIR.key,SharedprefVariants.ORDER_DIR.defaultValue.toString()).toOrderDirOption(),
        )
        println(">>>>>>> "+prefs.getString(SharedprefVariants.ORDER_BY.key,SharedprefVariants.ORDER_BY.defaultValue.toString()).toOrderByOption(),)
        println(">>>>>>> "+prefs.getString(SharedprefVariants.ORDER_DIR.key,SharedprefVariants.ORDER_DIR.defaultValue.toString()).toOrderDirOption())
        controller.fullRefresh()
    }

    Column(Modifier.fillMaxSize()) {
//        Row(modifier = Modifier.fillMaxWidth().height(60.dp)) {
//
//        }
        // Top controls
        Box(Modifier.fillMaxSize().weight(if (showFilter.value) 5f else 0.7f)) {
            TopControls(
                count = eventsListRequest.count,
                onCountChange = {
                    requestEventsList.value = requestEventsList.value.copy(count = it)

                },
                ncount = eventsListRequest.ncount,
                onNCountChange = {
                    requestEventsList.value = requestEventsList.value.copy(ncount = it)

                },
                filterBy = eventsListRequest.filterBy,
                onFilterByChange = {
                    requestEventsList.value = requestEventsList.value.copy(filterBy = it)

                },
                filterVal = eventsListRequest.filterVal,
                onFilterValChange = {
                    requestEventsList.value = requestEventsList.value.copy(filterVal = it)

                },
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
                            prefs.put(SharedprefVariants.FILTER_COUNT.key,   requestEventsList.value.count)
                            prefs.put(SharedprefVariants.FILTER_N_COUNT.key, requestEventsList.value.ncount)
                            prefs.put(SharedprefVariants.FILTER_BY.key,      requestEventsList.value.filterBy)
                            prefs.put(SharedprefVariants.FILTER_VAL.key,     requestEventsList.value.filterVal)

                            prefs.put(SharedprefVariants.ORDER_BY.key,     requestEventsList.value.orderBy.wire)
                            prefs.put(SharedprefVariants.ORDER_DIR.key,     requestEventsList.value.orderDir.wire)
                        }
                    }
                }
            )
        }

//        if (error != null) {
//            AssistChipRow("Ошибка: $error")
//        }

        // Content
        Row(modifier = Modifier.fillMaxSize().weight(10f)) {
            when (val r = res) {
                is Resource.Loading -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator()
                            Spacer(Modifier.height(12.dp))
                            Text("Загрузка...")
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
                                scope.launch { controller.fullRefresh() }
                            }) {
                                Text("Повторить")
                            }
                        }
                    }
                }
                is Resource.Success -> {
                    val events = r.data
                    if (events.isEmpty()) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("Нет данных", style = MaterialTheme.typography.bodyLarge)
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(12.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(events) { ev ->
                                EventCard(ev)
                            }
                            item {
                                Row(
                                    Modifier.fillMaxWidth().padding(vertical = 8.dp),
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    OutlinedButton(
                                        onClick = {
                                            // increment ncount by +10 and refresh
                                            val current = requestEventsList.value
                                            requestEventsList.value = current.copy(ncount = current.ncount + 10)
                                            scope.launch { controller.fullRefresh() }
                                        }
                                    ) { Text("Загрузить ещё (+10)") }
                                }
                            }
                        }
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

//                    ConfigRequest.value.orderBy = by
//                    ConfigRequest.value.orderDir = dir
                    requestEventsList.value = requestEventsList.value.copy(orderBy = by, orderDir = dir)

                    prefs.put(SharedprefVariants.ORDER_BY.key,     requestEventsList.value.orderBy.wire)
                    prefs.put(SharedprefVariants.ORDER_DIR.key,     requestEventsList.value.orderDir.wire)
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
                            showDialogInternals.value = false
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
    val prefs = koinInject<CMPPrefs>()

    Column(Modifier.fillMaxSize()) {
        AnimatedVisibility(
            visible = showFullControlsInternal, //modifier = Modifier.fillMaxWidth().height(120.dp)
        ) {
            Column() {
                Spacer(Modifier.height(8.dp))
                Text("Фильтры и параметры", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(8.dp))
                Row(Modifier.fillMaxWidth().height(80.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
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
                Row(Modifier.fillMaxWidth().height(80.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = filterBy,
                        onValueChange = onFilterByChange,
                        label = { Text("Фильтрация по (filterby)") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = filterVal,
                        onValueChange = onFilterValChange,
                        label = { Text("Город (filterval)") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }
            }

        }
        Column {
            if (!showFullControlsInternal) {
                Text(modifier = Modifier.fillMaxWidth(), text = "${filterBy}, г.${filterVal}", style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
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
                    onClick = {
                        showDialogOrderBy.value = !showDialogOrderBy.value
                    },
                    label = { Text("${requestEventsList.value.orderBy.wire}, ${
                        requestEventsList.value.orderDir.label}", maxLines = 1, overflow = TextOverflow.Ellipsis) }
                )

                IconButton(onClick = {
                    showFilter.value = !showFilter.value
                    prefs.put(SharedprefVariants.isSHOW_TOP_CONTROLS.key, showFilter.value)
                }) {
                    Icon(
                        imageVector = if (showFullControlsInternal) FeatherIcons.Eye else FeatherIcons.EyeOff,
                        contentDescription = "Show or Hide"
                    )
                }
            }
            if (!showFullControlsInternal) {
                Spacer(modifier = Modifier.fillMaxWidth().height(10.dp))
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
            Text(ev.date?.format(format) ?: "", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

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
            TextButton(onClick = {
                TARGET_EVENT.value = ev
                CURRENT_SCREEN.value = CurrentScreen.Details
            }) {
                Text("Показать детали")
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