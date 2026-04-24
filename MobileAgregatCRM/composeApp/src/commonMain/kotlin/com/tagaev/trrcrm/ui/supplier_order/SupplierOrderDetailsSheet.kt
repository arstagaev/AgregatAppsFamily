package com.tagaev.trrcrm.ui.supplier_order

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
import com.tagaev.trrcrm.models.SupplierOrderDistributionDto
import com.tagaev.trrcrm.models.SupplierOrderDto
import com.tagaev.trrcrm.models.WorkOrderProductDto
import com.tagaev.trrcrm.ui.cargo.ExpandableListSection
import com.tagaev.trrcrm.ui.custom.TextC
import com.tagaev.trrcrm.ui.master_screen.DetailsWithMessagesSheet
import com.tagaev.trrcrm.ui.master_screen.models.MessageModel
import com.tagaev.trrcrm.ui.work_order.buildGoodsTitle
import com.tagaev.trrcrm.ui.work_order.formatProductQuantityWithUnit
import com.tagaev.trrcrm.ui.work_order.parseMoneyAmount

private val SupplierExpandableListPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
private val SupplierExpandableDividerOutdent = 8.dp

@Composable
fun SupplierOrderDetailsSheet(
    order: SupplierOrderDto,
    onBack: () -> Unit,
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
        onSendMessage = { _, onResult -> onResult("Отправка сообщений для Заказа поставщику недоступна") },
        initialDraft = initialDraft,
        onDraftChanged = onDraftChanged,
        historyTitle = "История",
        historyEmptyText = "Записей нет",
        sendingDialogTitle = "Отправка записи",
        showComposer = false,
        headerContent = { wo ->
            SupplierOrderHeaderContent(
                order = wo,
                onOpenBaseDocument = onOpenBaseDocument
            )
        }
    )
}

@Composable
private fun SupplierOrderHeaderContent(
    order: SupplierOrderDto,
    onOpenBaseDocument: (String) -> Unit
) {
    SupplierCompactTitle("Ссылка")
    SupplierCompactValue(order.link)
    Spacer(Modifier.height(4.dp))

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Column(Modifier.weight(1f)) {
            SupplierCompactTitle("Организация")
            SupplierCompactValue(order.organization)
        }
        Column(Modifier.weight(1f)) {
            SupplierCompactTitle("Подразделение")
            SupplierCompactValue(order.branch)
        }
    }
    Spacer(Modifier.height(4.dp))

    SupplierCompactTitle("Контрагент")
    SupplierCompactValue(order.counterparty)
    Spacer(Modifier.height(4.dp))

    SupplierCompactTitle("Состояние")
    SupplierCompactValue(order.status)
    Spacer(Modifier.height(4.dp))

    SupplierCompactTitle("Автор")
    SupplierCompactValue(order.author)
    Spacer(Modifier.height(4.dp))

    SupplierCompactTitle("ДокументОснование")
    val baseDocument = order.baseDocument?.takeIf { it.isNotBlank() }
    if (baseDocument != null) {
        Text(
            text = baseDocument,
            style = MaterialTheme.typography.bodySmall.copy(
                fontSize = 12.sp,
                lineHeight = 14.sp,
                fontWeight = FontWeight.Medium
            ),
            color = MaterialTheme.colorScheme.primary,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.clickable { onOpenBaseDocument(baseDocument) }
        )
    } else {
        SupplierCompactValue(order.baseDocument)
    }
    Spacer(Modifier.height(8.dp))

    val productsTotal = order.products.sumOf { parseMoneyAmount(it.amount) ?: 0.0 }
    ExpandableListSection(
        title = buildGoodsTitle(
            baseTitle = "Товары (список)",
            positionsCount = order.products.size,
            totalAmount = productsTotal
        ),
        items = order.products,
        initiallyExpanded = false,
        listContentPadding = SupplierExpandableListPadding,
        itemSpacing = 0.dp,
        showItemDividers = true,
        dividerHorizontalOutdent = SupplierExpandableDividerOutdent
    ) { product ->
        SupplierProductRow(product)
    }
    Spacer(Modifier.height(6.dp))

    if (order.orderDistribution.isNotEmpty()) {
        ExpandableListSection(
            title = "Распределение заказа (поз. ${order.orderDistribution.size})",
            items = order.orderDistribution,
            initiallyExpanded = false,
            listContentPadding = SupplierExpandableListPadding,
            itemSpacing = 0.dp,
            showItemDividers = true,
            dividerHorizontalOutdent = SupplierExpandableDividerOutdent
        ) { item ->
            SupplierDistributionRow(item)
        }
        Spacer(Modifier.height(6.dp))
    }

    SupplierCompactTitle("Комментарий")
    SupplierCompactValue(order.comment)
}

@Composable
private fun SupplierCompactTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, lineHeight = 11.sp),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis
    )
}

@Composable
private fun SupplierCompactValue(value: String?) {
    Text(
        text = value?.takeIf { it.isNotBlank() } ?: "—",
        style = MaterialTheme.typography.bodySmall.copy(
            fontSize = 12.sp,
            lineHeight = 14.sp,
            fontWeight = FontWeight.Medium
        ),
        color = MaterialTheme.colorScheme.onSurface,
        maxLines = 3,
        overflow = TextOverflow.Ellipsis
    )
}

@Composable
private fun SupplierProductRow(product: WorkOrderProductDto) {
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
        product.note?.takeIf { it.isNotBlank() }?.let {
            TextC(
                text = it,
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, lineHeight = 11.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun SupplierDistributionRow(item: SupplierOrderDistributionDto) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        verticalArrangement = Arrangement.spacedBy(1.dp),
    ) {
        SupplierCompactValue(item.itemName)
        val sub = listOfNotNull(
            item.quantity?.takeIf { it.isNotBlank() }?.let { "Кол-во: $it" },
            item.price?.takeIf { it.isNotBlank() }?.let { "Цена: $it" }
        ).joinToString(" · ")
        if (sub.isNotBlank()) SupplierCompactTitle(sub)
        item.buyerOrder?.takeIf { it.isNotBlank() }?.let { SupplierCompactTitle(it) }
    }
}

