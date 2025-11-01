package org.agregatcrm.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import org.agregatcrm.models.EventItemDto
import org.agregatcrm.models.MessageDto
import org.agregatcrm.models.TaskDto
import org.agregatcrm.models.UserRowDto

@Composable
fun EventDetailsScreen(
    event: EventItemDto,
    onAddTaskClick: () -> Unit = {},
    onSendMessageClick: (String) -> Unit = {}
) {
    var usersExpanded by remember { mutableStateOf(false) }
    var tasksExpanded by remember { mutableStateOf(false) }
    var messagesExpanded by remember { mutableStateOf(false) }
    var messageDraft by remember { mutableStateOf("") }

    val fields = buildList {
        add("Тема" to (event.subject ?: ""))
        add("Ссылка" to (event.link ?: ""))
        add("Дата" to ("${event.date!!.date.month}" ?: ""))
        add("ДатаМод" to (event.modifiedDate ?: ""))
        add("Состояние" to (event.state ?: ""))
        add("ВидСобытия" to (event.eventType ?: ""))
        add("ДатаНачала" to (event.startDate ?: ""))
        add("ДатаОкончания" to (event.endDate ?: ""))
        add("Организация" to (event.organization ?: ""))
        add("Содержание" to (event.content ?: ""))
    }.filter { it.second.isNotBlank() }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Header + compact fields (non-scrolling grid replacement)
        item {
            Text(
                text = event.subject?.takeIf { it.isNotBlank() } ?: (event.eventType ?: "Событие"),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.basicMarquee()
            )
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
                if (event.users.isEmpty()) {
                    MutedText("Нет пользователей")
                } else {
                    event.users.forEach { UserItem(it) }
                }
            }
        }

        // tasks (+)
        item {
            Section(
                title = "tasks",
                expanded = tasksExpanded,
                onToggle = { tasksExpanded = !tasksExpanded },
                trailing = { TextButton(onClick = onAddTaskClick) { Text("+") } }
            ) {
                if (event.tasks.isEmpty()) {
                    MutedText("Нет задач")
                } else {
                    event.tasks.forEach { TaskItem(it) }
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
                if (event.messages.isEmpty()) {
                    MutedText("Нет сообщений")
                } else {
                    event.messages.forEach { MessageItem(it) }
                }
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = messageDraft,
                    onValueChange = { messageDraft = it },
                    label = { Text("Новое сообщение (скоро)") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    OutlinedButton(
                        onClick = { onSendMessageClick(messageDraft) },
                        enabled = messageDraft.isNotBlank()
                    ) { Text("Отправить") }
                }
            }
        }

        // guid footer
        item {
            event.guid?.takeIf { it.isNotBlank() }?.let {
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
    Column(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(m.author ?: "Сообщение", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
            m.workDate?.takeIf { it.isNotBlank() }?.let {
                Text(it, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
    Card {
        Column(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("▸", modifier = Modifier.rotate(arrowRotation), style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.width(8.dp))
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.weight(1f))
                trailing?.invoke()
                TextButton(onClick = onToggle) { Text(if (expanded) "Скрыть" else "Показать") }
            }
            AnimatedVisibility(visible = expanded) {
                Column(Modifier.fillMaxWidth().padding(top = 8.dp)) { content() }
            }
        }
    }
}
