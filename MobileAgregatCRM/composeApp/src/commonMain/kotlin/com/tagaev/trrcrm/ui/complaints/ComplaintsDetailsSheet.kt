package com.tagaev.trrcrm.ui.complaints

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.tagaev.trrcrm.domain.UserRow
import com.tagaev.trrcrm.domain.sortedByRolePriority
import com.tagaev.trrcrm.ext.toIntSafe
import com.tagaev.trrcrm.models.ComplaintDto
import com.tagaev.trrcrm.models.ComplaintWorkDto
import com.tagaev.trrcrm.ui.cargo.ExpandableListSection
import com.tagaev.trrcrm.ui.custom.TextC
import com.tagaev.trrcrm.ui.master_screen.DetailsWithMessagesSheet
import com.tagaev.trrcrm.ui.master_screen.SectionTitle
import com.tagaev.trrcrm.ui.master_screen.models.MessageModel
import kotlin.collections.map

@Composable
fun ComplaintDetailsSheetTopPart(
    complaint: ComplaintDto,
    onBack: () -> Unit,
) {
    // If you have a generic "details sheet" wrapper like DetailsWithMessagesSheet
    // but you don't want messages here – you can wrap this content into that later.
    // For now we expose just the detailed content.

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 2.dp)
    ) {
        // 0. Header: link / number / date
        val header = complaint.link
            ?: buildString {
                if (!complaint.number.isNullOrBlank()) {
                    append(complaint.number)
                }
                if (!complaint.date.isNullOrBlank()) {
                    if (isNotEmpty()) append(" от ")
                    append(complaint.date)
                }
            }

        if (header.isNotBlank()) {
            DetailLargeTitleRow(
                text = header
            )
            Spacer(Modifier.height(2.dp))
        }

        // 1. Организация + Подразделение
        if (!complaint.organization.isNullOrBlank() || !complaint.branch.isNullOrBlank()) {
            DetailTwoColumnRow(
                firstTitle = "Организация:",
                firstValue = complaint.organization,
                secondTitle = "Подразделение:",
                secondValue = complaint.branch
            )
            Spacer(Modifier.height(2.dp))
        }

        // 2. Автомобиль
        complaint.car?.takeIf { it.isNotBlank() }?.let {
            SectionTitle("Автомобиль:")
            TextC(
                text = it,
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(Modifier.height(2.dp))
        }

        // 3. Характеристики: Тип КПП / Тип двигателя / Приоритет
        if (!complaint.transmissionType.isNullOrBlank() ||
            !complaint.engineType.isNullOrBlank() ||
            !complaint.priority.isNullOrBlank()
        ) {
            SectionTitle("Характеристики:")
            DetailThreeColumnRow(
                firstTitle = "Тип КПП:",
                firstValue = complaint.transmissionType,
                secondTitle = "Тип двигателя:",
                secondValue = complaint.engineType,
                thirdTitle = "Приоритет:",
                thirdValue = complaint.priority
            )
            Spacer(Modifier.height(2.dp))
        }

        // 4. Состояние, Вид события, Источник информации
        if (!complaint.state.isNullOrBlank() ||
            !complaint.eventType.isNullOrBlank() ||
            !complaint.infoSource.isNullOrBlank()
        ) {
            SectionTitle("Статус обращения:")
            DetailThreeColumnRow(
                firstTitle = "Состояние:",
                firstValue = complaint.state,
                secondTitle = "Вид события:",
                secondValue = complaint.eventType,
                thirdTitle = "Источник:",
                thirdValue = complaint.infoSource
            )
            Spacer(Modifier.height(2.dp))
        }

        // 5. Тема + Содержание (основной текст обращения)
        if (!complaint.topic.isNullOrBlank()) {
            SectionTitle("Тема:")
            TextC(
                text = complaint.topic.orEmpty(),
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(Modifier.height(2.dp))
        }

        if (!complaint.content.isNullOrBlank()) {
            SectionTitle("Содержание обращения:")
            TextC(
                text = complaint.content.orEmpty(),
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(Modifier.height(2.dp))
        }

        // 2. Ошибка (диагностические коды / текст)
        if (!complaint.error.isNullOrBlank()) {
            SectionTitle("Ошибка / диагностическая информация:")
            // Здесь, как с кодами ошибок в WorkOrder, можно разбивать по \r при желании
            val lines = complaint.error
                ?.split("\\r".toRegex())
                ?.map { it.trim() }
                ?.filter { it.isNotEmpty() }
                .orEmpty()

            if (lines.isNotEmpty()) {
                lines.forEach { line ->
                    Text(
                        text = "• $line",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            } else {
                TextC(
                    text = complaint.error.orEmpty(),
                    style = MaterialTheme.typography.bodySmall
                )
            }
            Spacer(Modifier.height(2.dp))
        }

        // 7. Ответственный / Автор
        if (!complaint.responsible.isNullOrBlank() || !complaint.author.isNullOrBlank()) {
            DetailTwoColumnRow(
                firstTitle = "Ответственный:",
                firstValue = complaint.responsible,
                secondTitle = "Автор обращения:",
                secondValue = complaint.author
            )
            Spacer(Modifier.height(2.dp))
        }

        // 8. Участники (Пользователи)
        val users = complaint.users
        ExpandableListSection(
            title = "Участники (${users.size})",
            items = users.map { UserRow(name = it.user ?: "Не определено", role = it.role ?: "Не определено", isResponsible = it.responsible.toBoolean() ?: false) }.sortedByRolePriority()
        ) { u ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    TextC(
                        text = u.name.orEmpty(),
                        style = MaterialTheme.typography.bodyMedium
                    )
                    val role = u.role?.takeIf { it.isNotBlank() }
//                    val resp = u.isResponsible?.takeIf { it.isNotBlank() }
                    if (role != null) {
                        Text(
                            text = buildString {
                                "(${role?.let { append(it) }})"
//                                if (resp != null) {
//                                    if (isNotEmpty()) append(" • ")
//                                    append("Ответственный: $resp")
//                                }
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
        Spacer(Modifier.height(2.dp))

        // 9. Работы (expandable)
        val works = complaint.works
        val valSumWorks = works.sumOf { (it.totalAmount?.toIntSafe() ?: 0) }
        ExpandableListSection(
            title = "Работы (поз. ${works.size})",
            items = works
        ) { work ->
            ComplaintWorkItemRow(work = work)
        }

        Spacer(Modifier.height(2.dp))

        // 10. Товары (expandable)
        val goods = complaint.goods
        val valSum = goods.sumOf { (it.totalAmount?.toIntSafe() ?: 0) }
        goods.forEach {
            println("|>>>>>> ${it.totalAmount} ${it.unit}  ${it.qty}")
            println("|>>>>>> ${it.totalAmount?.toIntSafe()} ${it.unit?.toIntSafe()}  ${it.qty?.toIntSafe()}")
        }

        ExpandableListSection(
            title = "Товары (поз. ${goods.size})",
            items = goods
        ) { good ->
            ComplaintGoodsItemRow(goods = good)
        }

        Spacer(Modifier.height(3.dp))

        // 11. Дефекты (ДефектТаб, expandable)
        val defects = complaint.defects
        if (defects.isNotEmpty()) {
            ExpandableListSection(
                title = "Дефекты (поз. ${defects.size})",
                items = defects
            ) { d ->
                ComplaintDefectItemRow(defect = d)
            }
            Spacer(Modifier.height(3.dp))
        }

        // 12. Задачи (tasks, expandable)
        val tasks = complaint.tasks
        if (tasks.isNotEmpty()) {
            ExpandableListSection(
                title = "Задачи / комментарии по работам (поз. ${tasks.size})",
                items = tasks
            ) { t ->
                ComplaintTaskItemRow(task = t)
            }
        }
    }
}


@Composable
fun DetailLargeTitleRow(
    text: String,
    modifier: Modifier = Modifier
) {
    TextC(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold,
        modifier = modifier
    )
}

@Composable
fun DetailMediumTitleRow(
    text: String,
    modifier: Modifier = Modifier
) {
    TextC(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.SemiBold,
        modifier = modifier
    )
}

@Composable
fun DetailTwoColumnRow(
    firstTitle: String?,
    firstValue: String?,
    secondTitle: String?,
    secondValue: String?,
    modifier: Modifier = Modifier
) {
    if ((firstValue.isNullOrBlank()) && (secondValue.isNullOrBlank())) return

    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        if (!firstValue.isNullOrBlank() && !firstTitle.isNullOrBlank()) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = firstTitle,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                TextC(
                    text = firstValue,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        } else {
            Spacer(Modifier.weight(1f))
        }

        if (!secondValue.isNullOrBlank() && !secondTitle.isNullOrBlank()) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = secondTitle,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                TextC(
                    text = secondValue,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

@Composable
fun DetailThreeColumnRow(
    firstTitle: String?,
    firstValue: String?,
    secondTitle: String?,
    secondValue: String?,
    thirdTitle: String?,
    thirdValue: String?,
    modifier: Modifier = Modifier
) {
    if ((firstValue.isNullOrBlank()) && (secondValue.isNullOrBlank()) && (thirdValue.isNullOrBlank())) return

    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        if (!firstValue.isNullOrBlank() && !firstTitle.isNullOrBlank()) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = firstTitle,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                TextC(
                    text = firstValue,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }

        if (!secondValue.isNullOrBlank() && !secondTitle.isNullOrBlank()) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = secondTitle,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                TextC(
                    text = secondValue,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }

        if (!thirdValue.isNullOrBlank() && !thirdTitle.isNullOrBlank()) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = thirdTitle,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                TextC(
                    text = thirdValue,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

@Composable
fun DetailFourColumnRow(
    firstTitle: String?,
    firstValue: String?,
    secondTitle: String?,
    secondValue: String?,
    thirdTitle: String?,
    thirdValue: String?,
    fourthTitle: String?,
    fourthValue: String?,
    modifier: Modifier = Modifier
) {
    if ((firstValue.isNullOrBlank())
        && (secondValue.isNullOrBlank())
        && (thirdValue.isNullOrBlank())
        && (fourthValue.isNullOrBlank())
    ) return

    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        if (!firstValue.isNullOrBlank() && !firstTitle.isNullOrBlank()) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = firstTitle,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                TextC(
                    text = firstValue,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }

        if (!secondValue.isNullOrBlank() && !secondTitle.isNullOrBlank()) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = secondTitle,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                TextC(
                    text = secondValue,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }

        if (!thirdValue.isNullOrBlank() && !thirdTitle.isNullOrBlank()) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = thirdTitle,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                TextC(
                    text = thirdValue,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }

        if (!fourthValue.isNullOrBlank() && !fourthTitle.isNullOrBlank()) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = fourthTitle,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                TextC(
                    text = fourthValue,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

@Composable
fun ExpandableSection(
    title: String,
    count: Int? = null,
    initiallyExpanded: Boolean = true,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    androidx.compose.runtime.LaunchedEffect(Unit) { } // no-op to keep import hints happy if needed

    androidx.compose.runtime.key(title) {
        var expanded = androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(initiallyExpanded) }

        Column(modifier = modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded.value = !expanded.value }
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )

                count?.let { c ->
                    Spacer(Modifier.width(2.dp))
                    Text(
                        text = "($c)",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(Modifier.weight(1f))

                Text(
                    text = if (expanded.value) "▾" else "▸",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (expanded.value) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 2.dp)
                ) {
                    content()
                }
            }
        }
    }
}

@Composable
fun ComplaintWorkItemRow(
    work: ComplaintWorkDto,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            TextC(
                text = work.workName.orEmpty(),
                style = MaterialTheme.typography.bodyMedium
            )
            val qty = work.qty?.takeIf { it.isNotBlank() }
            if (qty != null) {
                Text(
                    text = "Кол-во: $qty",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Column(horizontalAlignment = Alignment.End) {
            work.totalAmount?.takeIf { it.isNotBlank() }?.let {
                Text(
                    text = "$it ₽",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }
            work.price?.takeIf { it.isNotBlank() }?.let {
                Text(
                    text = "Цена: $it",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun ComplaintGoodsItemRow(
    goods: com.tagaev.trrcrm.models.ComplaintGoodsDto,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            TextC(
                text = goods.itemName.orEmpty(),
                style = MaterialTheme.typography.bodyMedium
            )
            val qty = goods.qty?.takeIf { it.isNotBlank() }
            val unit = goods.unit?.takeIf { it.isNotBlank() }
            if (qty != null) {
                Text(
                    text = buildString {
                        append("Кол-во: ")
                        append(qty)
                        if (unit != null) {
                            append(" ")
                            append(unit)
                        }
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Column(horizontalAlignment = Alignment.End) {
            goods.totalAmount?.takeIf { it.isNotBlank() }?.let {
                Text(
                    text = "$it ₽",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }
            goods.price?.takeIf { it.isNotBlank() }?.let {
                Text(
                    text = "Цена: $it",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun ComplaintDefectItemRow(
    defect: com.tagaev.trrcrm.models.ComplaintDefectDto,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        TextC(
            text = defect.name.orEmpty(),
            style = MaterialTheme.typography.bodyMedium
        )
        val state = defect.state?.takeIf { it.isNotBlank() }
        val resolution = defect.resolution?.takeIf { it.isNotBlank() }
        if (state != null || resolution != null) {
            Text(
                text = buildString {
                    state?.let { append("Состояние: $it") }
                    if (resolution != null) {
                        if (isNotEmpty()) append(" • ")
                        append("Решение: $resolution")
                    }
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        defect.explanation?.takeIf { it.isNotBlank() }?.let { expl ->
            Text(
                text = expl,
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@Composable
fun ComplaintTaskItemRow(
    task: com.tagaev.trrcrm.models.ComplaintTaskDto,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        TextC(
            text = task.comment.orEmpty(),
            style = MaterialTheme.typography.bodyMedium
        )
        Text(
            text = buildString {
                task.workDate?.takeIf { it.isNotBlank() }?.let {
                    append(it)
                }
                task.author?.takeIf { it.isNotBlank() }?.let { a ->
                    if (isNotEmpty()) append(" • ")
                    append(a)
                }
            },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}


@Composable
fun ComplaintDetailsSheetWithMessages(
    complaint: ComplaintDto,
    onBack: () -> Unit,
    onSendMessage: (String, (Boolean) -> Unit) -> Unit,
    isSendingMessage: Boolean = false,
    lastSendError: String? = null,
    onErrorDismiss: () -> Unit = {},
    initialDraft: String? = null,
    onDraftChanged: (String) -> Unit = {}
) {
    DetailsWithMessagesSheet(
        item = complaint,
        guid = complaint.guid.toString(),
        messages = complaint.messages.map { MessageModel(author = it.author ?: "no author", text = it.comment ?: "", date = it.workDate ?: "no date") },
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
        ComplaintDetailsSheetTopPart(
            ev,
            onBack = onBack
        )
    }
}