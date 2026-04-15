package com.tagaev.trrcrm.ui.inner_orders

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
import com.tagaev.trrcrm.models.ComplaintWorkDto
import com.tagaev.trrcrm.models.InnerOrderDto
import com.tagaev.trrcrm.ui.cargo.ExpandableListSection
import com.tagaev.trrcrm.ui.custom.TextC
import com.tagaev.trrcrm.ui.master_screen.DetailsWithMessagesSheet
import com.tagaev.trrcrm.ui.master_screen.SectionTitle
import com.tagaev.trrcrm.ui.master_screen.models.MessageModel
import kotlin.collections.map

@Composable
fun InnerOrderDetailsSheetTopPart(
    innerOrder: InnerOrderDto,
    onBack: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 2.dp)
    ) {
        // 0. Header: Ссылка или "Номер от Дата"
        val header = innerOrder.link
            ?: buildString {
                if (!innerOrder.number.isNullOrBlank()) {
                    append(innerOrder.number)
                }
                if (!innerOrder.date.isNullOrBlank()) {
                    if (isNotEmpty()) append(" от ")
                    append(innerOrder.date)
                }
            }

        if (header.isNotBlank()) {
            DetailLargeTitleRow(text = header)
            Spacer(Modifier.height(4.dp))
        }

        // 1. Статус документа
        if (!innerOrder.state.isNullOrBlank() ||
            !innerOrder.operationType.isNullOrBlank() ||
            !innerOrder.posted.isNullOrBlank()
        ) {
            SectionTitle("Статус документа:")
            DetailThreeColumnRow(
                firstTitle = "Состояние:",
                firstValue = innerOrder.state,
                secondTitle = "Операция:",
                secondValue = innerOrder.operationType,
                thirdTitle = "Проведен:",
                thirdValue = innerOrder.posted
            )
            Spacer(Modifier.height(4.dp))
        }

        // 2. Организация + Подразделение
        if (!innerOrder.organization.isNullOrBlank() || !innerOrder.branch.isNullOrBlank()) {
            DetailTwoColumnRow(
                firstTitle = "Организация:",
                firstValue = innerOrder.organization,
                secondTitle = "Подразделение:",
                secondValue = innerOrder.branch
            )
            Spacer(Modifier.height(2.dp))
        }

        // 3. Склад / получатель
        if (!innerOrder.companyWarehouse.isNullOrBlank() || !innerOrder.receiverBranch.isNullOrBlank()) {
            SectionTitle("Склад и получатель:")
            DetailTwoColumnRow(
                firstTitle = "Склад компании:",
                firstValue = innerOrder.companyWarehouse,
                secondTitle = "Подразделение-получатель:",
                secondValue = innerOrder.receiverBranch
            )
            Spacer(Modifier.height(2.dp))
        }

        // 4. Ответственные лица
        if (!innerOrder.manager.isNullOrBlank() ||
            !innerOrder.author.isNullOrBlank() ||
            !innerOrder.myRole.isNullOrBlank()
        ) {
            SectionTitle("Ответственные:")
            DetailThreeColumnRow(
                firstTitle = "Менеджер:",
                firstValue = innerOrder.manager,
                secondTitle = "Автор:",
                secondValue = innerOrder.author,
                thirdTitle = "Моя роль:",
                thirdValue = innerOrder.myRole
            )
            Spacer(Modifier.height(4.dp))
        }

        // 5. Документы-основания
        if (!innerOrder.workOrderNumber.isNullOrBlank() || !innerOrder.baseDocument.isNullOrBlank()) {
            SectionTitle("Документы-основания:")
            DetailTwoColumnRow(
                firstTitle = "Комплектация / ЗН:",
                firstValue = innerOrder.workOrderNumber,
                secondTitle = "Документ-основание:",
                secondValue = innerOrder.baseDocument
            )
            Spacer(Modifier.height(4.dp))
        }

        // 6. Финансы
        if (!innerOrder.documentAmount.isNullOrBlank() ||
            !innerOrder.currency.isNullOrBlank() ||
            !innerOrder.rate.isNullOrBlank() ||
            !innerOrder.managementCurrencyRate.isNullOrBlank()
        ) {
            SectionTitle("Финансы:")
            DetailFourColumnRow(
                firstTitle = "Сумма:",
                firstValue = innerOrder.documentAmount,
                secondTitle = "Валюта:",
                secondValue = innerOrder.currency,
                thirdTitle = "Курс:",
                thirdValue = innerOrder.rate,
                fourthTitle = "Курс упр.:",
                fourthValue = innerOrder.managementCurrencyRate
            )
            Spacer(Modifier.height(4.dp))
        }

        // 7. Даты
        if (!innerOrder.creationDate.isNullOrBlank() ||
            !innerOrder.operationDate.isNullOrBlank() ||
            !innerOrder.date.isNullOrBlank()
        ) {
            SectionTitle("Даты:")
            DetailThreeColumnRow(
                firstTitle = "Создан:",
                firstValue = innerOrder.creationDate,
                secondTitle = "Дата операции:",
                secondValue = innerOrder.operationDate,
                thirdTitle = "Дата документа:",
                thirdValue = innerOrder.date
            )
            Spacer(Modifier.height(4.dp))
        }

        // 8. Текст по машине
        innerOrder.carText?.takeIf { it.isNotBlank() }?.let {
            SectionTitle("Машина / агрегат:")
            TextC(
                text = it,
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(Modifier.height(4.dp))
        }

        // 9. Текст заказа
        innerOrder.orderText?.takeIf { it.isNotBlank() }?.let {
            SectionTitle("Текст заказа:")
            TextC(
                text = it,
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(Modifier.height(4.dp))
        }

        // 10. Комментарий
        innerOrder.comment?.takeIf { it.isNotBlank() }?.let {
            SectionTitle("Комментарий:")
            TextC(
                text = it,
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(Modifier.height(4.dp))
        }

        // 11. Участники (Пользователи)
        val users = innerOrder.users
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

        // 12. Товары (expandable)
        val goods = innerOrder.goods
        if (goods.isNotEmpty()) {
            ExpandableListSection(
                title = "Товары",
                items = goods
            ) { g ->
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                ) {
                    TextC(
                        text = g.itemName.orEmpty(),
                        style = MaterialTheme.typography.bodyMedium
                    )

                    // Кол-во / Цена
                    if (!g.quantity.isNullOrBlank() || !g.price.isNullOrBlank()) {
                        val qtyWithUnit = buildString {
                            if (!g.quantity.isNullOrBlank()) {
                                append(g.quantity)
                                if (!g.unit.isNullOrBlank()) {
                                    append(" ")
                                    append(g.unit)
                                }
                            }
                        }.ifBlank { null }

                        DetailTwoColumnRow(
                            firstTitle = "Кол-во:",
                            firstValue = qtyWithUnit,
                            secondTitle = "Цена:",
                            secondValue = g.price
                        )
                    }

                    // Сумма / Всего
                    if (!g.amount.isNullOrBlank() || !g.totalAmount.isNullOrBlank()) {
                        DetailTwoColumnRow(
                            firstTitle = "Сумма:",
                            firstValue = g.amount,
                            secondTitle = "Всего:",
                            secondValue = g.totalAmount
                        )
                    }

                    // Доп. сведения
                    if (!g.itemCharacteristic.isNullOrBlank() ||
                        !g.storageCell.isNullOrBlank() ||
                        !g.comment.isNullOrBlank()
                    ) {
                        Spacer(Modifier.height(2.dp))
                        g.itemCharacteristic?.takeIf { it.isNotBlank() }?.let { ch ->
                            Text(
                                text = "Хар-ка: $ch",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        g.storageCell?.takeIf { it.isNotBlank() }?.let { cell ->
                            Text(
                                text = "Ячейка: $cell",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        g.comment?.takeIf { it.isNotBlank() }?.let { c ->
                            Text(
                                text = c,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            }
            Spacer(Modifier.height(4.dp))
        }
        Spacer(Modifier.height(4.dp))
        // 13. Задачи (tasks, expandable)
        val tasks = innerOrder.tasks
        if (tasks.isNotEmpty()) {
            ExpandableListSection(
                title = "Задачи / комментарии (${tasks.size})",
                items = tasks
            ) { t ->
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                ) {
                    val mainText = when {
                        !t.comment.isNullOrBlank() -> t.comment
                        !t.work.isNullOrBlank() -> t.work
                        else -> null
                    }

                    mainText?.let {
                        TextC(
                            text = it,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }

                    val metaLine = buildString {
                        t.workDate?.takeIf { it.isNotBlank() }?.let { d ->
                            append(d)
                        }
                        t.author?.takeIf { it.isNotBlank() }?.let { a ->
                            if (isNotEmpty()) append(" • ")
                            append(a)
                        }
                        t.document?.takeIf { it.isNotBlank() }?.let { doc ->
                            if (isNotEmpty()) append(" • ")
                            append(doc)
                        }
                    }

                    if (metaLine.isNotBlank()) {
                        Text(
                            text = metaLine,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
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
fun InnerOrderDetailsSheetWithMessages(
    complaint: InnerOrderDto,
    onBack: () -> Unit,
    onSendMessage: (String, (String?) -> Unit) -> Unit,
    initialDraft: String? = null,
    onDraftChanged: (String) -> Unit = {}
) {
    DetailsWithMessagesSheet(
        item = complaint,
        guid = complaint.guid.toString(),
        messages = complaint.messages.map { MessageModel(author = it.author ?: "no author", text = it.comment ?: "", date = it.workDate ?: "no date") },
        onBack = onBack,
        onSendMessage = onSendMessage,
        initialDraft = initialDraft,
        onDraftChanged = onDraftChanged,
        isSendEnabled = { draft, wo ->
            draft.isNotBlank() &&
                    !wo.number.isNullOrBlank() &&
                    !wo.date.toString().isNullOrBlank()
        }
    ) { io ->
        InnerOrderDetailsSheetTopPart(
            io,
            onBack = onBack
        )
    }
}