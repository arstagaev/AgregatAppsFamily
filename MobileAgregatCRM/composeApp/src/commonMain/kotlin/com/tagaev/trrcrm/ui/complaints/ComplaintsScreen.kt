package com.tagaev.trrcrm.ui.complaints

import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.max
import androidx.compose.ui.unit.sp
import com.tagaev.trrcrm.models.ComplaintDto
import com.tagaev.trrcrm.ui.custom.StatusBadge
import com.tagaev.trrcrm.ui.custom.StatusStyle
import com.tagaev.trrcrm.ui.custom.TextC
import com.tagaev.trrcrm.ui.custom.UniversalCardItem
import com.tagaev.trrcrm.ui.master_screen.MasterScreen
import com.tagaev.trrcrm.ui.master_screen.RefineScreen
import com.tagaev.trrcrm.ui.master_screen.models.MessageModel
import com.tagaev.trrcrm.ui.style.DefaultColors
import com.tagaev.trrcrm.ui.work_order.WorkOrderDetailsSheet
import com.tagaev.trrcrm.utils.formatRelativeWorkDate
import kotlinx.coroutines.launch
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
fun ComplaintsScreen(component: IComplaintsComponent) {
    val resource by component.complaints.collectAsState()
    val refineState by component.refineState.collectAsState()
    val panel by component.masterScreenPanel.collectAsState()
    val selectedId by component.selectedItemGuid.collectAsState()

    val scope = rememberCoroutineScope()
    var isSendingMessage by remember { mutableStateOf(false) }
    var lastSendError by remember { mutableStateOf<String?>(null) }

    MasterScreen(
        title = "Рекламации",
        resource = resource,
        errorText = "Не удалось загрузить рекламации",
        notFoundText = "Рекламации не найдены",
        refineState = refineState,
        onRefresh = { component.fullRefresh() },
        onLoadMore = { component.loadMore() },
        onFilterChanged = { component.setRefineState(it) },

        itemId = { it.guid.toString() },
        isItemChanged = { old, new -> old.messages.size != new.messages.size },

        listItem = { complaint, isChanged, onClick ->
//            ComplaintCard(
//                complaint = complaint,
//                onClick = onClick
//            )
            ////
            UniversalCardItem(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                onClick = onClick,
                title = complaint.number ?: "",
                subtitle = "${complaint.branch} - ${complaint.organization}",

                topRightPrimary = {
                    val status = complaint.state
                    if (!status.isNullOrBlank()) {
                        StatusBadge(
                            state = status,
                            styles = mapOf(
                                "Выполнен"       to StatusStyle(DefaultColors.RainbowGreenBg,   DefaultColors.RainbowGreenFg),
                                "Закрыт"         to StatusStyle(DefaultColors.StatusMutedBg,   DefaultColors.StatusMutedFg),
                                "Запланировано"  to StatusStyle(DefaultColors.RainbowIndigoBg, DefaultColors.RainbowIndigoFg),
                                "Выполняется"    to StatusStyle(DefaultColors.RainbowBlueFg,   DefaultColors.RainbowBlueBg),
                                "Начать работу"  to StatusStyle(DefaultColors.RainbowVioletBg, DefaultColors.RainbowVioletFg)
                            )
                        )
                    }
                },
                topRightSecondary = {
                    val priority = complaint.priority
                    if (!priority.isNullOrBlank()) {
                        StatusBadge(
                            state = priority,
                            styles = mapOf(
                                Pair("Высокая", StatusStyle(background = DefaultColors.RainbowRedBg, foreground =DefaultColors.RainbowRedFg)),
                                Pair("Средняя", StatusStyle(background = DefaultColors.RainbowOrangeBg, foreground =DefaultColors.RainbowOrangeFg)),
                                Pair("Низкая", StatusStyle(background = DefaultColors.RainbowGreenBg, foreground =DefaultColors.RainbowGreenFg)),
                            )
                        )
                    }
                },
                // Middle A (big)
                bigText1 = "${complaint.topic} ",
                bigText2 = "${complaint.car} (${complaint.transmissionType}/${complaint.engineType})",
//                bigText3 = "${complaint.error}",

                // Middle B (two medium texts)
//                midBText1 = "Организация: ООО САМАРА АКПП",
//                midBText2 = "Подразделение: Сургут",
//
//                // Middle C (two medium texts)
//                midCText1 = "Ответственный: Голиков Максим",
//                midCText2 = "Источник: Яндекс",
//                complaint.date?.let {
//                    Text(
//                        text = "созд. ${it}",
//                        style = TextStyle(fontSize = 9.sp),
//                        color = MaterialTheme.colorScheme.onSurfaceVariant
//                    )
//                }
//                        complaint.messages.lastOrNull()?.let {
//                    Text(
//                        text = "изм. ${formatRelativeWorkDate(it.workDate)}",
//                        style = TextStyle(fontSize = 9.sp),
//                        color = MaterialTheme.colorScheme.onSurfaceVariant
//                    )
//                }
//                if (complaint.messages.lastOrNull() == null) {
//                    Text(
//                        text = "Сообщений нет",
//                        style = TextStyle(fontSize = 9.sp),
//                        color = MaterialTheme.colorScheme.onSurfaceVariant
//                    )
//                }
                // Bottom
                bottomLeftText = "созд. ${complaint.startDate}",
                bottomRightText = "изм. ${formatRelativeWorkDate(complaint.date)}"
            )
            ////
        },

        // Full-screen details content (not bottom-sheet)
        detailsContent = { complaint, onClose ->
            ComplaintDetailsSheetWithMessages(
                complaint = complaint,
                onBack = onClose,
                onSendMessage = { message, onResult ->
                    val number = complaint.number.orEmpty()
                    val date = complaint.date.orEmpty()
                    scope.launch {
                        val ok = component.sendMessage(number, date, message)
                        if (ok) {
                            component.addLocalMessage(complaint.guid.toString(), message = MessageModel(author = "я", text = message))
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
                messageForUser = "Корректно работает только сортировка по Дате, остальные фильтры пока в разработке",
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
    )
}


@Composable
fun ComplaintListItem(
    complaint: ComplaintDto,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {}
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .clip(MaterialTheme.shapes.medium)
            .clickable(onClick = onClick)
            ,
        shape = MaterialTheme.shapes.medium,
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
//        colors = CardDefaults.cardColors(
//            containerColor = MaterialTheme.colorScheme.onSurfaceVariant,
//            contentColor = MaterialTheme.colorScheme.onSurface
//        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp)
        ) {
            // Top row: number + status
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val number = complaint.number ?: ""
                if (number.isNotBlank()) {
                    Text(
                        text = number,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Spacer(Modifier.weight(1f))
                Row {
                    val priority = complaint.priority
                    if (!priority.isNullOrBlank()) {
                        StatusBadge(
                            state = priority,
                            styles = mapOf(
                                Pair("Высокая", StatusStyle(background = DefaultColors.RainbowRedBg, foreground =DefaultColors.RainbowRedFg)),
                                Pair("Средняя", StatusStyle(background = DefaultColors.RainbowOrangeBg, foreground =DefaultColors.RainbowOrangeFg)),
                                Pair("Низкая", StatusStyle(background = DefaultColors.RainbowGreenBg, foreground =DefaultColors.RainbowGreenFg)),
                            )
                        )
                    }

                    val status = complaint.state
                    if (!status.isNullOrBlank()) {
                        ComplaintStatusChip(status = status)
                    }
                }

            }

            // Middle: topic / content
            val title = when {
                !complaint.topic.isNullOrBlank() -> complaint.topic
                !complaint.content.isNullOrBlank() -> complaint.content
                else -> null
            }

            if (!title.isNullOrBlank()) {
                Spacer(Modifier.height(4.dp))
                TextC(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium,
                    // slightly accent the title
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            // Car line
            complaint.car?.takeIf { it.isNotBlank() }?.let { car ->
                Spacer(Modifier.height(4.dp))
                TextC(
                    text = "Авто: $car",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Bottom row: date + source / branch / priority
            val date = complaint.date?.takeIf { it.isNotBlank() }
            val source = complaint.infoSource?.takeIf { it.isNotBlank() }
            val branch = complaint.branch?.takeIf { it.isNotBlank() }
            val priority = complaint.priority?.takeIf { it.isNotBlank() }

            if (date != null || source != null || branch != null || priority != null) {
                Spacer(Modifier.height(1.dp))
                Row(
                    modifier = Modifier.fillMaxWidth().height(60.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Right: source / branch / priority (short summary)
                    val rightParts = buildList {
                        branch?.let { add(it) }
                        source?.let { add(it) }
//                        priority?.let { add("Приор.: $it") }
                    }

                    if (rightParts.isNotEmpty()) {
                        Text(
                            modifier = Modifier.fillMaxSize().weight(1f).basicMarquee(),
                            text = rightParts.joinToString(" • "),
                            maxLines = 1,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

//                    Spacer(Modifier.weight(1f))

                    if (date != null) {
                        Text(
                            modifier = Modifier.fillMaxSize().weight(1f),
                            text = date,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ComplaintCard(
    complaint: ComplaintDto,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(100.dp)
            .padding(4.dp)
            .clip(MaterialTheme.shapes.medium)
            .clickable(onClick = onClick),
        shape = MaterialTheme.shapes.medium,
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 12.dp, vertical = 10.dp)
        ) {
            Row(Modifier.fillMaxSize().weight(1f)) {
                Row(modifier = Modifier.fillMaxWidth().weight(1f),
                    verticalAlignment = Alignment.CenterVertically,) {
                    TextC(
                        text = complaint.number?.let { "$it" } ?: "Без номера",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(Modifier.width(2.dp))
                    complaint.branch?.let {
                        Text(
                            text = it,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Row(modifier = Modifier.fillMaxWidth().weight(1f),
                    verticalAlignment = Alignment.CenterVertically,) {


                    val priority = complaint.priority
                    if (!priority.isNullOrBlank()) {
                        StatusBadge(
                            state = priority,
                            styles = mapOf(
                                Pair("Высокая", StatusStyle(background = DefaultColors.RainbowRedBg, foreground =DefaultColors.RainbowRedFg)),
                                Pair("Средняя", StatusStyle(background = DefaultColors.RainbowOrangeBg, foreground =DefaultColors.RainbowOrangeFg)),
                                Pair("Низкая", StatusStyle(background = DefaultColors.RainbowGreenBg, foreground =DefaultColors.RainbowGreenFg)),
                            )
                        )
                    }
                    val status = complaint.state
                    if (!status.isNullOrBlank()) {
                        ComplaintStatusChip(status = status)
                    }
                }
            }
            Column(Modifier.fillMaxSize().weight(1f)) {
                // Middle: topic / content
                val title = when {
                    !complaint.topic.isNullOrBlank() -> complaint.topic
                    !complaint.content.isNullOrBlank() -> complaint.content
                    else -> null
                }

                if (!title.isNullOrBlank()) {
                    Spacer(Modifier.height(4.dp))
                    TextC(
                        text = title,
                        style = MaterialTheme.typography.bodyMedium,
                        // slightly accent the title
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                // Car line
                complaint.car?.takeIf { it.isNotBlank() }?.let { car ->
                    Spacer(Modifier.height(4.dp))
                    TextC(
                        text = "Авто: $car",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Bottom
                Spacer(Modifier.fillMaxWidth().height(5.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    complaint.date?.let {
                        Text(
                            text = "созд. ${it}",
                            style = TextStyle(fontSize = 9.sp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    complaint.messages.lastOrNull()?.let {
                        Text(
                            text = "изм. ${formatRelativeWorkDate(it.workDate)}",
                            style = TextStyle(fontSize = 9.sp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (complaint.messages.lastOrNull() == null) {
                        Text(
                            text = "Сообщений нет",
                            style = TextStyle(fontSize = 9.sp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            Row(Modifier.fillMaxSize().weight(1f)) {

            }

//            Row(
//                modifier = Modifier.fillMaxWidth(),
//                verticalAlignment = Alignment.CenterVertically,
//                horizontalArrangement = Arrangement.SpaceBetween
//            ) {
//                Row(modifier = Modifier.fillMaxWidth().weight(1f),
//                    verticalAlignment = Alignment.CenterVertically,) {
//                    TextC(
//                        text = complaint.number?.let { "$it" } ?: "Без номера",
//                        style = MaterialTheme.typography.titleMedium,
//                        fontWeight = FontWeight.SemiBold
//                    )
//                    Spacer(Modifier.width(2.dp))
//                    complaint.branch?.let {
//                        Text(
//                            text = it,
//                            style = MaterialTheme.typography.bodySmall,
//                            color = MaterialTheme.colorScheme.onSurfaceVariant
//                        )
//                    }
//                }
//
//                Row(verticalAlignment = Alignment.CenterVertically,) {
//                    Column {
//                        val status = complaint.state
//                        if (!status.isNullOrBlank()) {
//                            ComplaintStatusChip(status = status)
//                        }
//
//                        val priority = complaint.priority
//                        if (!priority.isNullOrBlank()) {
//                            StatusBadge(
//                                state = priority,
//                                styles = mapOf(
//                                    Pair("Высокая", StatusStyle(background = DefaultColors.RainbowRedBg, foreground =DefaultColors.RainbowRedFg)),
//                                    Pair("Средняя", StatusStyle(background = DefaultColors.RainbowOrangeBg, foreground =DefaultColors.RainbowOrangeFg)),
//                                    Pair("Низкая", StatusStyle(background = DefaultColors.RainbowGreenBg, foreground =DefaultColors.RainbowGreenFg)),
//                                )
//                            )
//                        }
//                    }
//                }
//
////                Spacer(Modifier.weight(1f))
//
//            }
//
//            // Middle: topic / content
//            val title = when {
//                !complaint.topic.isNullOrBlank() -> complaint.topic
//                !complaint.content.isNullOrBlank() -> complaint.content
//                else -> null
//            }
//
//            if (!title.isNullOrBlank()) {
//                Spacer(Modifier.height(4.dp))
//                TextC(
//                    text = title,
//                    style = MaterialTheme.typography.bodyMedium,
//                    // slightly accent the title
//                    color = MaterialTheme.colorScheme.onSurface
//                )
//            }
//
//            // Car line
//            complaint.car?.takeIf { it.isNotBlank() }?.let { car ->
//                Spacer(Modifier.height(4.dp))
//                TextC(
//                    text = "Авто: $car",
//                    style = MaterialTheme.typography.bodySmall,
//                    color = MaterialTheme.colorScheme.onSurfaceVariant
//                )
//            }
//
//            // Bottom
//            Spacer(Modifier.fillMaxWidth().height(5.dp))
//            Row(
//                modifier = Modifier.fillMaxWidth(),
//                verticalAlignment = Alignment.CenterVertically,
//                horizontalArrangement = Arrangement.SpaceBetween
//            ) {
//                complaint.date?.let {
//                    Text(
//                        text = "созд. ${it}",
//                        style = TextStyle(fontSize = 9.sp),
//                        color = MaterialTheme.colorScheme.onSurfaceVariant
//                    )
//                }
//                complaint.messages.lastOrNull()?.let {
//                    Text(
//                        text = "изм. ${formatRelativeWorkDate(it.workDate)}",
//                        style = TextStyle(fontSize = 9.sp),
//                        color = MaterialTheme.colorScheme.onSurfaceVariant
//                    )
//                }
//                if (complaint.messages.lastOrNull() == null) {
//                    Text(
//                        text = "Сообщений нет",
//                        style = TextStyle(fontSize = 9.sp),
//                        color = MaterialTheme.colorScheme.onSurfaceVariant
//                    )
//                }
//            }
        }
    }
}



@Composable
private fun ComplaintStatusChip(
    status: String,
    modifier: Modifier = Modifier
) {
    val (bg, fg) = when {
        status.contains("Выполн", ignoreCase = true) -> // Выполнен, Выполнено…
            MaterialTheme.colorScheme.tertiaryContainer to MaterialTheme.colorScheme.onTertiaryContainer
        status.contains("Новый", ignoreCase = true) ||
                status.contains("Открыт", ignoreCase = true) ->
            MaterialTheme.colorScheme.primaryContainer to MaterialTheme.colorScheme.onPrimaryContainer
        status.contains("Отмен", ignoreCase = true) ||
                status.contains("Закрыт", ignoreCase = true) ->
            MaterialTheme.colorScheme.surfaceVariant to MaterialTheme.colorScheme.onSurfaceVariant
        else ->
            MaterialTheme.colorScheme.secondaryContainer to MaterialTheme.colorScheme.onSecondaryContainer
    }

    Surface(
        modifier = modifier,
        color = bg,
        contentColor = fg,
        shape = MaterialTheme.shapes.small,
        tonalElevation = 0.dp
    ) {
        Text(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            text = status,
            style = MaterialTheme.typography.labelSmall,
            maxLines = 1
        )
    }
}
