package com.tagaev.mobileagregatcrm.ui.details

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.format
import kotlinx.datetime.format.FormatStringsInDatetimeFormats
import kotlinx.datetime.format.byUnicodePattern
import org.agregatcrm.models.MessageDto
import org.agregatcrm.models.TaskDto
import org.agregatcrm.models.UserRowDto
import com.tagaev.mobileagregatcrm.utils.TARGET_EVENT

@Composable
fun DetailsScreen(
    component: DetailsComponent,
    onRequestRefresh: () -> Unit = {}
) {

    var event by remember { TARGET_EVENT }
    var usersExpanded by rememberSaveable("details_users_expanded") { mutableStateOf(false) }
    var tasksExpanded by rememberSaveable("details_tasks_expanded") { mutableStateOf(false) }
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
//        return
    } else {
        val fields = buildList {
            add("Тема" to (e.subject ?: ""))
            add("Ссылка" to (e.link ?: ""))
            add("Дата" to (e.date?.format(format) ?: ""))
            add("ДатаМод" to (e.modifiedDate ?: ""))
            add("Состояние" to (e.state ?: ""))
            add("ВидСобытия" to (e.eventType ?: ""))
            add("ДатаНачала" to (e.startDate ?: ""))
            add("ДатаОкончания" to (e.endDate ?: ""))
            add("Организация" to (e.organization ?: ""))
            add("Содержание" to (e.content ?: ""))
        }.filter { it.second.isNotBlank() }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(3.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header + compact fields (non-scrolling grid replacement)
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = e.subject?.takeIf { it.isNotBlank() } ?: (e.eventType ?: "Событие"),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.weight(1f).basicMarquee()
                    )
                    TextButton(onClick = onRequestRefresh) { Text("Обновить") }
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
            }

            // Пользователи
            item {
                Section(
                    title = "Пользователи",
                    expanded = usersExpanded,
                    onToggle = { usersExpanded = !usersExpanded }
                ) {
                    if (e.users.isEmpty()) {
                        MutedText("Нет пользователей")
                    } else {
                        e.users.forEach { UserItem(it) }
                    }
                }
            }

            // tasks (+)
            item {
                Section(
                    title = "Задачи",
                    expanded = tasksExpanded,
                    onToggle = { tasksExpanded = !tasksExpanded },
                    trailing = { TextButton(onClick = { component.addTask("TEST") }) { Text("+") } }
                ) {
                    if (e.tasks.isEmpty()) {
                        MutedText("Нет задач")
                    } else {
                        e.tasks.forEach { TaskItem(it) }
                    }
                }
            }

            // messages + input
            item {
                Section(
                    title = "messages",
                    expanded = messagesExpanded,
                    onToggle = { messagesExpanded = !messagesExpanded }
                ) {
                    if (e.messages.isEmpty()) {
                        MutedText("Нет сообщений")
                    } else {
                        e.messages.forEach { MessageItem(it) }
                    }
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = messageDraft,
                        onValueChange = { messageDraft = it },
                        label = { Text("Новое сообщение") },
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp),
                    )
                    Spacer(Modifier.height(8.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        OutlinedButton(
                            modifier = Modifier.padding(horizontal = 10.dp),
                            onClick = {
                                val num = e.number ?: return@OutlinedButton
                                val dateStr = e.date?.format(format) ?: return@OutlinedButton
                                component.sendMessage(
                                    number = num,
                                    date = dateStr,
                                    message = messageDraft
                                )
                                messageDraft = ""
                            },
                            enabled = messageDraft.isNotBlank() && e.number != null && e.date != null
                        ) { Text("Отправить") }
                    }
                }
            }

            // guid footer
            item {
                e.guid?.takeIf { it.isNotBlank() }?.let {
                    Divider()
                    Text(
                        text = "guid: $it",
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(top = 6.dp).basicMarquee(),
                        color = Color(0xFF262626)
                    )
                }
            }
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
private fun UserItem(u: UserRowDto) {
    Column(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
        Text(u.user ?: "—", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
        val details = listOfNotNull(
            u.role?.takeIf { it.isNotBlank() }?.let { "Роль: $it" },
            u.responsible?.takeIf { it.isNotBlank() }?.let { "Ответственный: $it" }
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

@OptIn(FormatStringsInDatetimeFormats::class)
val format = LocalDateTime.Format {
    byUnicodePattern("dd.MM.yyyy")
}
