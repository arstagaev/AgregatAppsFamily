package com.tagaev.mobileagregatcrm.ui.events

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tagaev.mobileagregatcrm.data.AppSettings
import com.tagaev.mobileagregatcrm.data.AppSettingsKeys
import com.tagaev.mobileagregatcrm.models.EventItemDto
import com.tagaev.mobileagregatcrm.models.MessageDto
import com.tagaev.mobileagregatcrm.models.ProductsItem
import com.tagaev.mobileagregatcrm.models.TaskDto
import com.tagaev.mobileagregatcrm.models.UserRowDto
import com.tagaev.mobileagregatcrm.ui.custom.TextC
import com.tagaev.mobileagregatcrm.ui.details.DetailsComponent
import com.tagaev.mobileagregatcrm.ui.details.MessageEvent
import com.tagaev.mobileagregatcrm.ui.details.SendMessageUiState
import com.tagaev.mobileagregatcrm.ui.master_screen.DetailsWithMessagesSheet
import com.tagaev.mobileagregatcrm.ui.master_screen.models.MessageModel
import com.tagaev.mobileagregatcrm.utils.DefaultValuesConst.MESSAGE_MAX_CHARS
import com.tagaev.mobileagregatcrm.utils.SPACE_RX
import com.tagaev.mobileagregatcrm.utils.TARGET_EVENT
import com.tagaev.mobileagregatcrm.utils.formatDDMMYYYY
import com.tagaev.mobileagregatcrm.utils.roleRank
import kotlinx.datetime.format
import org.agregatcrm.models.isResponsible
import org.koin.compose.koinInject

@Composable
fun EventsHeader(
    event: EventItemDto
) {
    val appSettings = koinInject<AppSettings>()
    val personalData = remember { appSettings.getString(AppSettingsKeys.PERSONAL_DATA, "") }

//    var event by TARGET_EVENT
    var usersExpanded by rememberSaveable("details_users_expanded") { mutableStateOf(false) }
    var tasksExpanded by rememberSaveable("details_tasks_expanded") { mutableStateOf(false) }
    var productsExpanded by rememberSaveable("products_expanded") { mutableStateOf(false) }
    var messagesExpanded by rememberSaveable("details_messages_expanded") { mutableStateOf(false) }
    var messageDraft by remember { mutableStateOf("") }

    // Make a stable non-null snapshot and short-circuit UI when nothing is selected
    val e = event
    if (e == null) {
        Box(Modifier.fillMaxSize()) {
            Text(
                modifier = Modifier.align(Alignment.Center),
                text = "Событие не выбрано",
                fontSize = 44.sp,
                textAlign = TextAlign.Center
            )
        }
    } else {
        // If a new message arrives (TARGET_EVENT updated by component), auto-expand the section
        var lastMessagesCount by rememberSaveable("details_messages_last_count") { mutableStateOf(e.messages.size) }
        LaunchedEffect(e.messages.size) {
            if (e.messages.size > lastMessagesCount) {
                messagesExpanded = true
                lastMessagesCount = e.messages.size
            }
        }

        val fields = buildList {
            add("Тема" to (e.subject ?: ""))
            add("Ссылка" to (e.link ?: ""))
            add("Дата" to (e.date?.format(formatDDMMYYYY) ?: ""))
            add("Дата изменения" to (e.modifiedDate ?: ""))
            add("Состояние" to (e.state ?: ""))
            add("ВидСобытия" to (e.eventType ?: ""))
            add("ДатаНачала" to (e.startDate ?: ""))
            add("ДатаОкончания" to (e.endDate ?: ""))
            add("Организация" to (e.organization ?: ""))
            add("Подразделение" to (e.companyDepartment ?: ""))
        }.filter { it.second.isNotBlank() }

        Column(
            modifier = Modifier.fillMaxSize().padding(PaddingValues(3.dp)),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = e.subject?.takeIf { it.isNotBlank() } ?: (e.eventType ?: "Событие не выбрано"),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f).basicMarquee()
                )
//                    TextButton(onClick = onRequestRefresh) { Text("Обновить") }
            }
            Spacer(Modifier.height(8.dp))
            Card {
                FieldsTwoColumn(
                    fields = fields,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp)
                )
            }

            // Пользователи
            Section(
                title = "Пользователи",
                expanded = usersExpanded,
                onToggle = { usersExpanded = !usersExpanded }
            ) {
                if (e.users.isEmpty()) {
                    MutedText("Нет пользователей")
                } else {
                    // Optional: if your model has a boolean like isResponsible / Ответственный == "Да",
                    // upgrade the role for sorting only:
                    fun effectiveRole(u: UserRowDto): String? =
                        when {
                            (u.isResponsible == true) -> "ответственный"
                            else -> u.role // e.g. "Делаю", "Помогаю", "Наблюдаю"
                        }

                    val sortedUsers = remember(e.users) {
                        e.users.sortedWith(
                            compareBy<UserRowDto>(
                                { roleRank(effectiveRole(it)) }              // 1) by role order
                            ).thenBy { it.user?.lowercase() ?: "" }      // 2) tie-breaker by name
                        )
                    }

                    sortedUsers.forEach { UserItem(it, highlightFullName = personalData) }
                }
            }

            // tasks (+)
            Section(
                title = "Задачи",
                expanded = tasksExpanded,
                onToggle = { tasksExpanded = !tasksExpanded },
                trailing = { }
            ) {
                if (e.tasks.isEmpty()) {
                    MutedText("Нет задач")
                } else {
                    e.tasks.forEach { TaskItem(it) }
                }
            }
            val total: Double = e.products.sumOf { p ->
                p.sum?.replace(SPACE_RX, "")?.replace(',', '.')?.toDoubleOrNull() ?: 0.0
            }
            Section(
                title = "Товары (Сумма: ${total} руб.)",
                expanded = productsExpanded,
                onToggle = { productsExpanded = !productsExpanded },
                //trailing = { TextButton(onClick = { component.addTask("TEST") }) { Text("+") } }
            ) {
                if (e.products.isEmpty()) {
                    MutedText("Нет товаров")
                } else {
                    e.products.forEach { ProductItem(it) }
                }
            }
            // guid footer
//            e.guid?.takeIf { it.isNotBlank() }?.let {
//                Divider()
//                Text(
//                    text = "guid: $it",
//                    style = MaterialTheme.typography.labelSmall,
//                    modifier = Modifier.padding(top = 6.dp).basicMarquee(),
//                    color = MaterialTheme.colorScheme.onSurfaceVariant
//                )
//            }
        }
    }
}

/** Non-scrolling “grid” to avoid nested scrollables. */
@Composable
private fun FieldsTwoColumn(
    fields: List<Pair<String, String>>,
    modifier: Modifier = Modifier
) {
    Column(modifier) {
        fields.chunked(2).forEachIndexed { idx, row ->
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                FieldCell(row[0].first, row[0].second, Modifier.weight(1f))
                if (row.size == 2) {
                    FieldCell(row[1].first, row[1].second, Modifier.weight(1f))
                } else {
                    Spacer(Modifier.weight(1f))
                }
            }
            if (idx != fields.lastIndex / 2) Spacer(Modifier.height(10.dp))
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun FieldCell(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.basicMarquee()
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.basicMarquee()
        )
    }
}

@Composable
private fun MutedText(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

@Composable
private fun UserItem(u: UserRowDto, highlightFullName: String? = null) {
    val isHighlighted = remember(u.user, highlightFullName) {
        val name = u.user?.trim().orEmpty()
        val target = highlightFullName?.trim().orEmpty()
        name.isNotEmpty() && target.isNotEmpty() && name.equals(target, ignoreCase = true)
    }
    val nameWeight = if (isHighlighted) FontWeight.Bold else FontWeight.SemiBold
    val displayName = (u.user ?: "—") + if (isHighlighted) " (я)" else ""
    Column(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
        Text(displayName, style = MaterialTheme.typography.bodyLarge, fontWeight = nameWeight)
        val details = listOfNotNull(
            u.role?.takeIf { it.isNotBlank() }?.let { "Роль: $it" },
            //u.responsible?.takeIf { it.isNotBlank() }?.let { "Ответственный: $it" }
        ).joinToString("  •  ")
        if (details.isNotBlank()) {
            Text(details, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
    Divider()
}

@Composable
private fun TaskItem(t: TaskDto) {
    Column(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
        Text(t.document ?: "Задача", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
        val line = buildList {
            t.workDate?.takeIf { it.isNotBlank() }?.let { add(it) }
            t.author?.takeIf { it.isNotBlank() }?.let { add("Автор: $it") }
            t.price?.takeIf { it.isNotBlank() }?.let { add("Цена: $it") }
        }.joinToString("  •  ")
        if (line.isNotBlank()) {
            Text(line, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        t.comment?.takeIf { it.isNotBlank() }?.let {
            Spacer(Modifier.height(4.dp)); Text(it, style = MaterialTheme.typography.bodyMedium)
        }
    }
    Divider()
}

@Composable
private fun ProductItem(t: ProductsItem) {
    Column(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
        Text(t.itemName ?: "Товар ${t.rowNo}", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
        val line = buildList {
            t.itemFeature?.takeIf { it.isNotBlank() }?.let { add(it) }
            t.quantity?.takeIf { it.isNotBlank() }?.let { add("$it ${t.unit ?: ""}") }
//            t.unit?.takeIf { it.isNotBlank() }?.let { add("Ед. Измерения: $it") }
            t.price?.takeIf { it.isNotBlank() }?.let { add("Цена: $it") }
            t.sum?.takeIf { it.isNotBlank() }?.let { add("Сумма: $it") }

//            t.rowNo?.takeIf { it.isNotBlank() }?.let { add("Цена: $it") }
        }.joinToString("  •  ")
        if (line.isNotBlank()) {
            Text(line, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
//        t.comment?.takeIf { it.isNotBlank() }?.let {
//            Spacer(Modifier.height(4.dp)); Text(it, style = MaterialTheme.typography.bodyMedium)
//        }
    }
    Divider()
}

@Composable
private fun MessageItem(m: MessageDto) {
    Column(Modifier.fillMaxWidth().padding(vertical = 6.dp, horizontal = 4.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(m.author ?: "Сообщение", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
            m.workDate?.takeIf { it.isNotBlank() }?.let {
                Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        m.comment?.takeIf { it.isNotBlank() }?.let {
            Spacer(Modifier.height(4.dp)); Text(it, style = MaterialTheme.typography.bodyLarge)
        }
    }
    Divider()
}

@Composable
private fun Section(
    title: String,
    expanded: Boolean,
    onToggle: () -> Unit,
    trailing: (@Composable () -> Unit)? = null,
    content: @Composable () -> Unit
) {
    val arrowRotation by animateFloatAsState(if (expanded) 90f else 0f)
    Card(Modifier) {
        Column(Modifier.fillMaxWidth().clickable { onToggle.invoke() }) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("▸", modifier = Modifier.rotate(arrowRotation), style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.width(8.dp))
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.weight(1f))
                trailing?.invoke()
                //TextButton(onClick = onToggle) { Text(if (expanded) "Скрыть" else "Показать") }
            }
            AnimatedVisibility(visible = expanded) {
                Column(Modifier.fillMaxWidth().padding(top = 8.dp, start = 8.dp, end = 8.dp)) { content() }
            }
        }
    }
}

@Composable
fun EventDetailsSheet(
    event: EventItemDto,
    onBack: () -> Unit,
    onSendMessage: (String, (Boolean) -> Unit) -> Unit,
    isSendingMessage: Boolean = false,
    lastSendError: String? = null,
    onErrorDismiss: () -> Unit = {},
    initialDraft: String? = null,
    onDraftChanged: (String) -> Unit = {}
) {
    DetailsWithMessagesSheet(
        item = event,
        guid = event.guid.toString(),
        messages = event.messages.map { MessageModel(author = it.author ?: "no author", text = it.comment ?: "", date = it.workDate ?: "no date") },
        onBack = onBack,
        onSendMessage = onSendMessage,
        //isSendingMessage = isSendingMessage,
        lastSendError = lastSendError,
        onErrorDismiss = onErrorDismiss,
        initialDraft = initialDraft,
        onDraftChanged = onDraftChanged,
        isSendEnabled = { draft, wo ->
            draft.isNotBlank() &&
                    !wo.number.isNullOrBlank() &&
                    !wo.date.toString().isNullOrBlank()
        }
    ) { ev ->
        EventsHeader(ev)
    }
}