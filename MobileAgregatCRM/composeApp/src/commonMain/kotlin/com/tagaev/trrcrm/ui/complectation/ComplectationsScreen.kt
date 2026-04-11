package com.tagaev.trrcrm.ui.complectation

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.sp
import com.tagaev.trrcrm.models.WorkOrderDto
import com.tagaev.trrcrm.ui.master_screen.MasterScreen
import com.tagaev.trrcrm.ui.master_screen.RefineScreen
import com.tagaev.trrcrm.ui.master_screen.models.MessageModel
import com.tagaev.trrcrm.utils.formatRelativeWorkDate
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ComplectationsScreen(
    component: ComplectationComponent,
    modifier: Modifier = Modifier
) {
    val resource by component.complectations.collectAsState()
    val refineState by component.refineState.collectAsState()
    val panel by component.masterScreenPanel.collectAsState()
    val selectedId by component.selectedItemGuid.collectAsState()

    val scope = rememberCoroutineScope()
    var isSendingMessage by remember { mutableStateOf(false) }
    var lastSendError by remember { mutableStateOf<String?>(null) }

    MasterScreen(
        title = "Комплектация",
        resource = resource,
        errorText = "Не удалось загрузить комплектации",
        notFoundText = "Комплектации не найдены",
        refineState = refineState,
        onRefresh = { component.fullRefresh() },
        onLoadMore = { component.loadMore() },
        onFilterChanged = { component.setRefineState(it) },

        itemId = { it.guid.toString() },
        isItemChanged = { old, new -> old.messages.size != new.messages.size },

        listItem = { order, isChanged, onClick ->
            ComplectationCard(
                order = order,
                isChanged = isChanged,
                onClick = onClick
            )
        },

        // Full-screen details content (not bottom-sheet)
        detailsContent = { order, onClose ->
            ComplectationDetailsSheet(
                order = order,
                onBack = onClose,
                onSendMessage = { message, onResult ->
                    val number = order.number.orEmpty()
                    val date = order.date.orEmpty()
                    scope.launch {
                        val ok = component.sendMessage(number, date, message)
                        if (ok) {
                            component.addLocalMessage(order.guid.toString(), message = MessageModel(author = "я", text = message))
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
//                sections = setOf(
//                    RefineSection.STATUS,
//                    RefineSection.FILTER_VAL,
//                    RefineSection.ORDER,
//                    RefineSection.DIRECTION,
//                ),
                onApply = { newState ->
                    println(">>>> ${newState.toString()}")
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
private fun ComplectationCard(
    order: WorkOrderDto,
    isChanged: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val numberText = normalizeSingleLine(order.number).orEmpty().ifBlank { "Без номера" }
    val branchText = normalizeSingleLine(order.branch).orEmpty()
    val statusText = normalizeSingleLine(order.status)
    val documentAmountText = formatAmountInK(order.documentAmount)
    val kitText = normalizeSingleLine(order.complectationKit).orEmpty().ifBlank { "—" }
    val createdText = normalizeSingleLine(order.date)?.let { "созд. $it" }
    val lastMessageText = order.messages.lastOrNull()?.let { "изм. ${formatRelativeWorkDate(it.workDate)}" }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(4.dp)
            .clip(MaterialTheme.shapes.medium)
            .clickable(onClick = onClick),
        shape = MaterialTheme.shapes.medium,
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 10.dp, vertical = 8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "№ $numberText",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (branchText.isNotBlank()) {
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = branchText,
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Spacer(Modifier.width(8.dp))
                ComplectationStatusBadge(statusText)
            }

            Spacer(Modifier.height(4.dp))
            ComplectationMetaRow(
                label = "Состояние",
                value = statusText.orEmpty().ifBlank { "—" }
            )
            ComplectationMetaRow(label = "СуммаДокумента", value = documentAmountText)
            ComplectationMetaRow(label = "Комплект", value = kitText)

            Spacer(Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                createdText?.let {
                    Text(
                        text = it,
                        style = TextStyle(fontSize = 9.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                lastMessageText?.let {
                    Text(
                        text = it,
                        style = TextStyle(fontSize = 9.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                if (order.messages.lastOrNull() == null) {
                    Text(
                        text = "Сообщений нет",
                        style = TextStyle(fontSize = 9.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
private fun ComplectationMetaRow(
    label: String,
    value: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 1.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1
        )
        Text(
            text = normalizeSingleLine(value).orEmpty().ifBlank { "—" },
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(start = 8.dp)
        )
    }
}

private fun normalizeSingleLine(value: String?): String? {
    if (value == null) return null
    return value
        .replace('\r', ' ')
        .replace('\n', ' ')
        .replace(Regex("\\s+"), " ")
        .trim()
}

private fun formatAmountInK(raw: String?): String {
    val amount = parseAmount(raw) ?: return "—"
    val amountK = amount / 1000.0
    val rounded = (amountK * 10).roundToInt() / 10.0
    val text = if (rounded % 1.0 == 0.0) rounded.toInt().toString() else rounded.toString()
    return "${text}K"
}

private fun parseAmount(raw: String?): Double? {
    if (raw.isNullOrBlank()) return null
    val normalized = raw
        .replace('\u00A0', ' ')
        .replace(" ", "")
        .filter { it.isDigit() || it == ',' || it == '.' || it == '-' }
        .replace(',', '.')
    if (normalized.isBlank()) return null
    return normalized.toDoubleOrNull()
}

@Composable
fun ComplectationStatusBadge(status: String?) {
    if (status.isNullOrBlank()) return

    val (bg, fg) = when (status) {
        "Закрыт" -> MaterialTheme.colorScheme.tertiaryContainer to MaterialTheme.colorScheme.onTertiaryContainer
        "В работе" -> MaterialTheme.colorScheme.primaryContainer to MaterialTheme.colorScheme.onPrimaryContainer
        else -> MaterialTheme.colorScheme.secondaryContainer to MaterialTheme.colorScheme.onSecondaryContainer
    }

    Surface(
        color = bg,
        contentColor = fg,
        shape = MaterialTheme.shapes.large
    ) {
        Text(
            text = status,
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
        )
    }
}
