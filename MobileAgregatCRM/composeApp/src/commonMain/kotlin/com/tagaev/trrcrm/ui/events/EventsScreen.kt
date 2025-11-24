package com.tagaev.trrcrm.ui.events

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.tagaev.trrcrm.models.EventItemDto
import com.tagaev.trrcrm.ui.custom.TextC
import com.tagaev.trrcrm.ui.mainscreen.StatusBadge
import com.tagaev.trrcrm.ui.mainscreen.format
import com.tagaev.trrcrm.ui.master_screen.MasterScreen
import com.tagaev.trrcrm.ui.master_screen.RefineScreen
import com.tagaev.trrcrm.ui.master_screen.models.MessageModel
import com.tagaev.trrcrm.utils.formatDDMMYYYY
import kotlinx.coroutines.launch
import kotlinx.datetime.format

@Composable
fun EventsScreen(
    component: IEventsComponent,
    modifier: Modifier = Modifier
) {
    val resource by component.events.collectAsState()
    val refineState by component.refineState.collectAsState()
    val panel by component.masterScreenPanel.collectAsState()
    val selectedId by component.selectedItemGuid.collectAsState()

    val scope = rememberCoroutineScope()
    var isSendingMessage by remember { mutableStateOf(false) }
    var lastSendError by remember { mutableStateOf<String?>(null) }

    MasterScreen(
        title = "События",
        resource = resource,
        emptyText = "События не найдены",
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
            EventDetailsSheet(
                event = ev,
                onBack = onClose,
                onSendMessage = { message, onResult ->
                    val number = ev.number.orEmpty()
                    val date = ev.date?.format(formatDDMMYYYY).orEmpty()
                    scope.launch {
                        val ok = component.sendMessage(number, date, message)
                        if (ok) {
                            component.addLocalMessage(ev.guid.toString(), message = MessageModel(author = "я", text = message))
                        }
                        onResult(ok) // this notifies the sheet
                    }
                },
                isSendingMessage = isSendingMessage,
                lastSendError = lastSendError,
                onErrorDismiss = { lastSendError = null }
            )
        },

        // Full-screen filter screen (not dialog)
        filterScreen = { current, onDismiss, onApply ->
            RefineScreen(
                current = current,
                onBack = onDismiss,
                onApply = { newState ->
                    println(">>>>>> ${newState.toString()}")
                    println(">>>>>> ${newState.searchQueryType.wire}")
                    component.setRefineState(newState)
                    onApply(newState)     // MasterDetailFilterScreen получит обновлённый стейт

                }
            )
        },

        panel = panel,
        onPanelChange = {
            component.changePanel(it)

        },

        selectedItemId = selectedId,
        onSelectedItemChange = { id -> component.selectItemFromList(id) },

        modifier = modifier
    )
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
                    fontWeight = FontWeight.SemiBold
                )
                StatusBadge(ev.state.orEmpty())
            }

            Spacer(Modifier.height(2.dp))

            Text(
                ev.date?.format(format) ?: "",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(4.dp))

            TextC(
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
                Spacer(Modifier.height(4.dp))
                HorizontalDivider(
                    modifier = Modifier.fillMaxWidth(),
                    thickness = 1.dp,
                    color = MaterialTheme.colorScheme.outlineVariant
                )
                Spacer(Modifier.height(4.dp))

                // First row: Эпик + Вид события
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    EventMetaColumn(
                        label = "Эпик",
                        value = ev.baseDocument,
                        modifier = Modifier.weight(1f, fill = true)
                    )
                    EventMetaColumn(
                        label = "Вид события",
                        value = ev.eventType,
                        modifier = Modifier.weight(1f, fill = true)
                    )
                }

                // Second row: Организация + Подразделение
                if (!ev.organization.isNullOrBlank() || !ev.companyDepartment.isNullOrBlank()) {
                    Spacer(Modifier.height(2.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        EventMetaColumn(
                            label = "Организация",
                            value = ev.organization,
                            modifier = Modifier.weight(1f, fill = true)
                        )
                        EventMetaColumn(
                            label = "Подразделение",
                            value = ev.companyDepartment,
                            modifier = Modifier.weight(1f, fill = true)
                        )
                    }
                }

                // Third row: Контрагент
                ev.counterparty?.takeIf { it.isNotBlank() }?.let { counterparty ->
                    Spacer(Modifier.height(2.dp))
                    EventMetaColumn(
                        label = "Контрагент",
                        value = counterparty,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // Fourth row: Автор + Изменено
                if (!ev.author.isNullOrBlank() || !ev.modifiedDate.isNullOrBlank()) {
                    Spacer(Modifier.height(2.dp))
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
                        EventMetaColumn(
                            label = "Изм.",
                            value = ev.modifiedDate,
                            modifier = Modifier.weight(1f, fill = true)
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
        TextC(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}