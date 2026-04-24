package com.tagaev.trrcrm.ui.buyer_order

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tagaev.trrcrm.models.BuyerOrderDto
import com.tagaev.trrcrm.models.TaskDto
import com.tagaev.trrcrm.models.UserRowDto
import com.tagaev.trrcrm.models.WorkOrderProductDto
import com.tagaev.trrcrm.ui.cargo.ExpandableListSection
import com.tagaev.trrcrm.ui.custom.TextC
import com.tagaev.trrcrm.ui.master_screen.DetailsWithMessagesSheet
import com.tagaev.trrcrm.ui.master_screen.models.MessageModel
import com.tagaev.trrcrm.ui.work_order.buildGoodsTitle
import com.tagaev.trrcrm.ui.work_order.formatProductQuantityWithUnit
import com.tagaev.trrcrm.ui.work_order.parseMoneyAmount

private val BuyerOrderExpandableListPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
private val BuyerOrderExpandableDividerOutdent = 8.dp

@Composable
fun BuyerOrderDetailsSheet(
    order: BuyerOrderDto,
    onBack: () -> Unit,
    onSendMessage: (String, (String?) -> Unit) -> Unit,
    onOpenBaseDocument: (String) -> Unit = {},
    initialDraft: String? = null,
    onDraftChanged: (String) -> Unit = {},
) {
    DetailsWithMessagesSheet(
        item = order,
        guid = order.guid.toString(),
        messages = order.messages.map {
            MessageModel(
                author = it.author ?: "no author",
                text = it.comment ?: "",
                date = it.workDate ?: "no date"
            )
        },
        onBack = onBack,
        onSendMessage = onSendMessage,
        initialDraft = initialDraft,
        onDraftChanged = onDraftChanged,
        isSendEnabled = { draft, wo ->
            draft.isNotBlank() && !wo.number.isNullOrBlank() && !wo.date.isNullOrBlank()
        },
        historyTitle = "История",
        historyEmptyText = "Записей нет",
        addCommentTitle = "Добавить запись",
        composerPlaceholder = "Текст записи…",
        sendingDialogTitle = "Отправка записи",
        headerContent = { wo ->
            BuyerOrderHeaderContent(
                order = wo,
                onOpenBaseDocument = onOpenBaseDocument
            )
        }
    )
}

@Composable
private fun BuyerOrderHeaderContent(
    order: BuyerOrderDto,
    onOpenBaseDocument: (String) -> Unit
) {
    BuyerOrderCompactTitle("Ссылка")
    BuyerOrderCompactValue(order.link)
    Spacer(Modifier.height(4.dp))

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Column(Modifier.weight(1f)) {
            BuyerOrderCompactTitle("Организация")
            BuyerOrderCompactValue(order.organization)
        }
        Column(Modifier.weight(1f)) {
            BuyerOrderCompactTitle("Подразделение")
            BuyerOrderCompactValue(order.branch)
        }
    }
    Spacer(Modifier.height(4.dp))

    BuyerOrderCompactTitle("Автомобиль")
    BuyerOrderCompactValue(order.car ?: order.carText)
    Spacer(Modifier.height(4.dp))

    BuyerOrderCompactTitle("Состояние")
    BuyerOrderCompactValue(order.status)
    Spacer(Modifier.height(4.dp))

    BuyerOrderCompactTitle("Автор")
    BuyerOrderCompactValue(order.author)
    Spacer(Modifier.height(4.dp))

    BuyerOrderCompactTitle("Менеджер")
    BuyerOrderCompactValue(order.manager)
    Spacer(Modifier.height(4.dp))

    BuyerOrderCompactTitle("ДокументОснование")
    val baseDocument = order.baseDocument?.takeIf { it.isNotBlank() }
    if (baseDocument != null) {
        Text(
            text = baseDocument,
            style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp, lineHeight = 14.sp, fontWeight = FontWeight.Medium),
            color = MaterialTheme.colorScheme.primary,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.clickable { onOpenBaseDocument(baseDocument) }
        )
    } else {
        BuyerOrderCompactValue(order.baseDocument)
    }
    order.workOrderRef?.takeIf { it.isNotBlank() }?.let { workOrderRef ->
        Spacer(Modifier.height(4.dp))
        BuyerOrderCompactTitle("НомерЗН")
        Text(
            text = workOrderRef,
            style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp, lineHeight = 14.sp, fontWeight = FontWeight.Medium),
            color = MaterialTheme.colorScheme.primary,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.clickable { onOpenBaseDocument(workOrderRef) }
        )
    }
    Spacer(Modifier.height(8.dp))

    ExpandableListSection(
        title = "Пользователи (список) (поз. ${order.users.size})",
        items = order.users,
        initiallyExpanded = false,
        listContentPadding = BuyerOrderExpandableListPadding,
        itemSpacing = 0.dp,
        showItemDividers = true,
        dividerHorizontalOutdent = BuyerOrderExpandableDividerOutdent
    ) { user ->
        BuyerOrderUserRow(user)
    }
    Spacer(Modifier.height(6.dp))

    BuyerOrderCompactTitle("Комментарий")
    BuyerOrderCompactValue(order.comment)
    Spacer(Modifier.height(8.dp))

    val tasks = if (order.tasks.isNotEmpty()) order.tasks else order.tasksRu
    ExpandableListSection(
        title = "Задания (список) (поз. ${tasks.size})",
        items = tasks,
        initiallyExpanded = false,
        listContentPadding = BuyerOrderExpandableListPadding,
        itemSpacing = 0.dp,
        showItemDividers = true,
        dividerHorizontalOutdent = BuyerOrderExpandableDividerOutdent
    ) { task ->
        BuyerOrderTaskRow(task)
    }
    Spacer(Modifier.height(6.dp))

    val products = if (order.products.isNotEmpty()) order.products else order.spareParts
    val productsTotal = products.sumOf { parseMoneyAmount(it.amount) ?: 0.0 }
    ExpandableListSection(
        title = buildGoodsTitle(
            baseTitle = "Товары (Вып работ)",
            positionsCount = products.size,
            totalAmount = productsTotal
        ),
        items = products,
        initiallyExpanded = false,
        listContentPadding = BuyerOrderExpandableListPadding,
        itemSpacing = 0.dp,
        showItemDividers = true,
        dividerHorizontalOutdent = BuyerOrderExpandableDividerOutdent
    ) { product ->
        BuyerOrderProductRow(product)
    }
}

@Composable
private fun BuyerOrderCompactTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, lineHeight = 11.sp),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis
    )
}

@Composable
private fun BuyerOrderCompactValue(value: String?) {
    Text(
        text = value?.takeIf { it.isNotBlank() } ?: "—",
        style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp, lineHeight = 14.sp, fontWeight = FontWeight.Medium),
        color = MaterialTheme.colorScheme.onSurface,
        maxLines = 3,
        overflow = TextOverflow.Ellipsis
    )
}

@Composable
private fun BuyerOrderUserRow(user: UserRowDto) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.Top
    ) {
        Column(Modifier.weight(1f)) {
            BuyerOrderCompactValue(user.user)
            val subtitle = listOfNotNull(
                user.role?.takeIf { it.isNotBlank() },
                user.responsible?.takeIf { it.isNotBlank() }?.let { "Ответственный: $it" }
            ).joinToString(" · ")
            if (subtitle.isNotBlank()) {
                BuyerOrderCompactTitle(subtitle)
            }
        }
    }
}

@Composable
private fun BuyerOrderTaskRow(task: TaskDto) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp)
    ) {
        BuyerOrderCompactValue(task.work ?: task.work1)
        val sub = listOfNotNull(
            task.executor?.takeIf { it.isNotBlank() },
            task.author?.takeIf { it.isNotBlank() }
        ).joinToString(" · ")
        if (sub.isNotBlank()) BuyerOrderCompactTitle(sub)
        task.comment?.takeIf { it.isNotBlank() }?.let { BuyerOrderCompactTitle(it) }
    }
}

@Composable
private fun BuyerOrderProductRow(product: WorkOrderProductDto) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp)
    ) {
        TextC(
            text = product.name?.takeIf { it.isNotBlank() } ?: "—",
            style = MaterialTheme.typography.bodySmall.copy(
                fontSize = 12.sp,
                lineHeight = 14.sp,
                fontWeight = FontWeight.Medium
            ),
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis
        )
        val sub = listOfNotNull(
            formatProductQuantityWithUnit(product.quantity, product.unit)?.let { "кол-во: $it" },
            product.price?.takeIf { it.isNotBlank() }?.let { "цена: $it" },
            product.amount?.takeIf { it.isNotBlank() }?.let { "сумма: $it" }
        ).joinToString(" · ")
        if (sub.isNotBlank()) {
            TextC(
                text = sub,
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, lineHeight = 11.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

