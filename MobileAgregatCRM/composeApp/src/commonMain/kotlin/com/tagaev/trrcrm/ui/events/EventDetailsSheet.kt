package com.tagaev.trrcrm.ui.events

import com.tagaev.trrcrm.ui.i18n.s

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
import androidx.compose.material3.Card
import androidx.compose.material3.Divider
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import com.tagaev.trrcrm.data.AppSettings
import com.tagaev.trrcrm.data.AppSettingsKeys
import com.tagaev.trrcrm.models.EventItemDto
import com.tagaev.trrcrm.models.MessageDto
import com.tagaev.trrcrm.models.ProductsItem
import com.tagaev.trrcrm.models.TaskDto
import com.tagaev.trrcrm.models.UserRowDto
import com.tagaev.trrcrm.ui.master_screen.DetailsWithMessagesSheet
import com.tagaev.trrcrm.ui.master_screen.models.MessageModel
import com.tagaev.trrcrm.utils.SPACE_RX
import com.tagaev.trrcrm.utils.formatDDMMYYYY
import com.tagaev.trrcrm.utils.roleRank
import kotlinx.datetime.format
import com.tagaev.trrcrm.models.isResponsible
import com.tagaev.trrcrm.ui.custom.TextC
import com.tagaev.trrcrm.ui.custom.TextCLinkPreview
import com.tagaev.trrcrm.ui.work_order.formatProductQuantityWithUnit
import org.koin.compose.koinInject

@Composable
fun EventsHeader(
    event: EventItemDto,
    onOpenBaseDocument: (String) -> Unit = {},
    documentPhotoCount: Int = 0,
    isDocumentPhotoCountLoading: Boolean = false,
    onOpenDocumentPhotos: (() -> Unit)? = null,
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
                text = s("events_sobytie_ne_vybrano"),
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
            add(s("buyer_order_ssylka") to (e.link ?: ""))
            add(s("events_organizatsiya") to (e.organization ?: ""))
            add(s("events_podrazdelenie") to (e.companyDepartment ?: ""))
            add(s("events_vid_sobytiya") to (e.eventType ?: ""))
            add(s("work_order_sostoyanie") to (e.state ?: ""))
            add(s("events_data_sozdaniya") to (e.date?.format(formatDDMMYYYY) ?: ""))
            add(s("events_data_nachala") to (e.startDate ?: ""))
            add(s("events_data_izmeneniya") to (e.modifiedDate ?: ""))
            add(s("events_data_okonchaniya") to (e.endDate ?: ""))
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
                    text = e.subject?.takeIf { it.isNotBlank() } ?: (e.eventType ?: s("events_sobytie_ne_vybrano")),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
//                    TextButton(onClick = onRequestRefresh) { Text(s("menu_obnovit")) }
            }
            Spacer(Modifier.height(8.dp))
            Card {
                EventMainInfoCard(
                    fields = fields,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp)
                )
            }
            e.baseDocument?.takeIf { it.isNotBlank() }?.let { baseDocument ->
                Text(
                    text = s("events_dokument_osnovanie_2"),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                TextC(
                    text = baseDocument,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.clickable { onOpenBaseDocument(baseDocument) },
                    allowLinkTap = false,
                    allowLongPressCopy = false,
                )
            }

            if (onOpenDocumentPhotos != null) {
                com.tagaev.trrcrm.ui.complectation.ComplectationOpenPhotosButton(
                    photoCount = documentPhotoCount,
                    isLoading = isDocumentPhotoCountLoading,
                    onClick = onOpenDocumentPhotos,
                )
            }

            // Пользователи
            Section(
                title = s("events_polzovateli"),
                expanded = usersExpanded,
                onToggle = { usersExpanded = !usersExpanded }
            ) {
                if (e.users.isEmpty()) {
                    MutedText(s("events_net_polzovateley"))
                } else {
                    // Optional: if your model has a boolean like isResponsible / Ответственный == s("settings_da"),
                    // upgrade the role for sorting only:
                    fun effectiveRole(u: UserRowDto): String? =
                        when {
                            (u.isResponsible == true) -> s("events_otvetstvennyy")
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
                title = s("events_zadachi_kol_vo", e.tasks.size),
                expanded = tasksExpanded,
                onToggle = { tasksExpanded = !tasksExpanded },
                trailing = { }
            ) {
                if (e.tasks.isEmpty()) {
                    MutedText(s("events_net_zadach"))
                } else {
                    e.tasks.forEach { TaskItem(it) }
                }
            }
            val total: Double = e.products.sumOf { p ->
                p.sum?.replace(SPACE_RX, "")?.replace(',', '.')?.toDoubleOrNull() ?: 0.0
            }
            Section(
                title = s("events_tovary_sht_summa", e.products.size, total),
                expanded = productsExpanded,
                onToggle = { productsExpanded = !productsExpanded },
                //trailing = { TextButton(onClick = { component.addTask("TEST") }) { Text("+") } }
            ) {
                if (e.products.isEmpty()) {
                    MutedText(s("events_net_tovarov"))
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
private fun EventMainInfoCard(
    fields: List<Pair<String, String>>,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        fields.forEachIndexed { idx, (label, value) ->
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(0.42f)
                )
                Text(
                    text = value,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.End,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(0.58f)
                )
            }
            if (idx != fields.lastIndex) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))
            }
        }
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
    val displayName = (u.user ?: "—") + if (isHighlighted) s("events_ya_suffix") else ""
    Column(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
        Text(displayName, style = MaterialTheme.typography.bodyLarge, fontWeight = nameWeight)
        val details = listOfNotNull(
            u.role?.takeIf { it.isNotBlank() }?.let { s("events_rol_it", it) },
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
        Text(t.document ?: s("events_zadacha"), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
        val line = buildList {
            t.workDate?.takeIf { it.isNotBlank() }?.let { add(it) }
            t.author?.takeIf { it.isNotBlank() }?.let { add(s("events_avtor_it", it)) }
            t.price?.takeIf { it.isNotBlank() }?.let { add(s("events_tsena_it", it)) }
        }.joinToString("  •  ")
        if (line.isNotBlank()) {
            Text(line, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        t.comment?.takeIf { it.isNotBlank() }?.let {
            Spacer(Modifier.height(4.dp)); TextCLinkPreview(it, style = MaterialTheme.typography.bodyLarge)
        }
    }
    Divider()
}

@Composable
private fun ProductItem(t: ProductsItem) {
    Column(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
        TextCLinkPreview(t.itemName ?: s("events_tovar_row", t.rowNo.orEmpty()), style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
        val line = buildList {
            t.itemFeature?.takeIf { it.isNotBlank() }?.let { add(it) }
            formatProductQuantityWithUnit(t.quantity, t.unit)?.let { add(it) }
//            t.unit?.takeIf { it.isNotBlank() }?.let { add("Ед. Измерения: $it") }
            t.price?.takeIf { it.isNotBlank() }?.let { add(s("events_tsena_it", it)) }
            t.sum?.takeIf { it.isNotBlank() }?.let { add(s("events_summa_it", it)) }

//            t.rowNo?.takeIf { it.isNotBlank() }?.let { add(s("events_tsena_it", it)) }
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
            Text(m.author ?: s("events_soobschenie_fallback"), style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
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
                //TextButton(onClick = onToggle) { Text(if (expanded) s("login_skryt") else s("login_pokazat")) }
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
    onSendMessage: (String, (String?) -> Unit) -> Unit,
    onOpenBaseDocument: (String) -> Unit = {},
    initialDraft: String? = null,
    onDraftChanged: (String) -> Unit = {},
    documentPhotoCount: Int = 0,
    isDocumentPhotoCountLoading: Boolean = false,
    onOpenDocumentPhotos: (() -> Unit)? = null,
) {
    DetailsWithMessagesSheet(
        item = event,
        guid = event.guid.toString(),
        messages = event.messages.map { MessageModel(author = it.author ?: "no author", text = it.comment ?: "", date = it.workDate ?: "no date") },
        onBack = onBack,
        onSendMessage = onSendMessage,
        initialDraft = initialDraft,
        onDraftChanged = onDraftChanged,
        isSendEnabled = { draft, wo ->
            draft.isNotBlank() &&
                    !wo.number.isNullOrBlank() &&
                    !wo.date.toString().isNullOrBlank()
        }
    ) { ev ->
        EventsHeader(
            event = ev,
            onOpenBaseDocument = onOpenBaseDocument,
            documentPhotoCount = documentPhotoCount,
            isDocumentPhotoCountLoading = isDocumentPhotoCountLoading,
            onOpenDocumentPhotos = onOpenDocumentPhotos,
        )
    }
}
