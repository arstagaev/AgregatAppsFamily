package com.tagaev.trrcrm.ui.work_order

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.tagaev.trrcrm.models.WorkOrderDto
import com.tagaev.trrcrm.ui.custom.TextC
import com.tagaev.trrcrm.ui.master_screen.DetailsWithMessagesSheet
import com.tagaev.trrcrm.ui.master_screen.SectionTitle
import com.tagaev.trrcrm.ui.master_screen.models.MessageModel



@Composable
fun WorkOrderDetailsSheet(
    order: WorkOrderDto,
    onBack: () -> Unit,
    onSendMessage: (String, (Boolean) -> Unit) -> Unit,
    isSendingMessage: Boolean = false,
    lastSendError: String? = null,
    onErrorDismiss: () -> Unit = {},
    initialDraft: String? = null,
    onDraftChanged: (String) -> Unit = {}
) {
    DetailsWithMessagesSheet(
        item = order,
        guid = order.guid.toString(),
        messages = order.messages.map { MessageModel(author = it.author ?: "no author", text = it.comment ?: "", date = it.workDate ?: "no date") },
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
                    !wo.date.isNullOrBlank()
        }
    ) { wo ->
        // 1. Организация + подразделение (две колонки)
        if (!wo.organization.isNullOrBlank() || !wo.branch.isNullOrBlank()) {

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                if (!wo.organization.isNullOrBlank()) {
                    Column(modifier = Modifier.weight(1f)) {
                        SectionTitle("Организация:")
                        TextC(
                            text = wo.organization.orEmpty(),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                } else {
                    Spacer(Modifier.weight(1f))
                }
                if (!wo.branch.isNullOrBlank()) {
                    Column(modifier = Modifier.weight(1f)) {
                        SectionTitle("Подразделение:")
                        TextC(
                            text = wo.branch.orEmpty(),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        }

        // 2. Документ + заказчик (две колонки)
        if (!wo.link.isNullOrBlank() || !wo.customer.isNullOrBlank()) {

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                if (!wo.link.isNullOrBlank()) {
                    Column(modifier = Modifier.weight(1f)) {
                        SectionTitle("Документ:")
                        TextC(
                            text = wo.link.orEmpty(),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                } else {
                    Spacer(Modifier.weight(1f))
                }
                if (!wo.customer.isNullOrBlank()) {
                    Column(modifier = Modifier.weight(1f)) {
                        SectionTitle("Заказчик:")
                        TextC(
                            text = wo.customer.orEmpty(),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        }

        // 4. Автомобиль
        wo.car?.takeIf { it.isNotBlank() }?.let {
            SectionTitle("Автомобиль:")
            TextC(
                text = it,
                style = MaterialTheme.typography.bodyMedium
            )
        }

        // 5. Тип КПП / Тип двигателя / Пробег / Год
        if (!wo.gearboxType.isNullOrBlank() ||
            !wo.engineType.isNullOrBlank() ||
            !wo.mileage.isNullOrBlank() ||
            !wo.carAge.isNullOrBlank()
        ) {
            SectionTitle("Хар-ки автомобиля:")
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                if (!wo.gearboxType.isNullOrBlank()) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Тип КПП:",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        TextC(
                            text = wo.gearboxType.orEmpty(),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
                if (!wo.engineType.isNullOrBlank()) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Тип ДВС:",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        TextC(
                            text = wo.engineType.orEmpty(),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
                if (!wo.mileage.isNullOrBlank()) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Пробег:",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        TextC(
                            text = wo.mileage.orEmpty(),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
                if (!wo.carAge.isNullOrBlank()) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Год выпуска:",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        TextC(
                            text = wo.carAge.orEmpty(),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        }

        // 6. Состояние + Вид ремонта (две колонки)
        if (!wo.status.isNullOrBlank() || !wo.repairType.isNullOrBlank()) {
            Row(
                verticalAlignment = Alignment.Top,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (!wo.status.isNullOrBlank()) {
                    Column(modifier = Modifier.weight(1f)) {
                        SectionTitle("Состояние")

                        Spacer(Modifier.height(4.dp))
                        WorkOrderStatusBadge(wo.status.orEmpty())
                    }
                }
                if (!wo.repairType.isNullOrBlank()) {
                    Column(modifier = Modifier.weight(1f)) {
                        SectionTitle("Вид ремонта:")

                        Text(
                            text = wo.repairType.orEmpty(),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }

        // 7. Ошибка + Причина обращения (две колонки)
        if (!wo.errorCodes.isNullOrBlank() || !wo.reason.isNullOrBlank()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                if (!wo.errorCodes.isNullOrBlank()) {
                    Column(modifier = Modifier.weight(1f)) {
                        SectionTitle("Коды ошибок:")
                        val lines = remember(wo.errorCodes) {
                            wo.errorCodes!!
                                .split("\\r".toRegex())
                                .map { it.trim() }
                                .filter { it.isNotEmpty() }
                        }
                        if (lines.isNotEmpty()) {
                            lines.forEach { line ->
                                Text(
                                    text = "• $line",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        } else {
                            Text(
                                text = wo.errorCodes ?: "Нету",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
                if (!wo.reason.isNullOrBlank()) {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(start = 8.dp)
                    ) {
                        SectionTitle("Причина обращения:")

                        Text(
                            text = wo.reason.orEmpty(),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        }

        // 8. Выполненные работы по заказ-наряду (используем только "Работы")
        val jobs = wo.jobs
        SectionTitle("Выполненные работы по заказ-наряду:")
        if (jobs.isNullOrEmpty()) {
            Text(
                text = "Работы не добавлены",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            jobs.forEach { job ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        TextC(
                            text = job.work.orEmpty(),
                            style = MaterialTheme.typography.bodyMedium
                        )
                        val qty = job.quantity?.takeIf { it.isNotBlank() }
                        if (qty != null) {
                            Text(
                                text = "Кол-во: $qty",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        job.amount?.takeIf { it.isNotBlank() }?.let {
                            Text(
                                text = "$it ₽",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        job.price?.takeIf { it.isNotBlank() }?.let {
                            Text(
                                text = "Цена: $it",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        // 9. Товары по заказ-наряду (используем только "Товары")
        val products = wo.products
        SectionTitle("Товары по заказ-наряду:")
        if (products.isNullOrEmpty()) {
            Text(
                text = "Товары не добавлены",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            products.forEach { product ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        TextC(
                            text = product.name.orEmpty(),
                            style = MaterialTheme.typography.bodyMedium
                        )
                        val qty = product.quantity?.takeIf { it.isNotBlank() }
                        val unit = product.unit?.takeIf { it.isNotBlank() }
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
                        product.article?.takeIf { it.isNotBlank() }?.let { article ->
                            TextC(
                                text = "Артикул: $article",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        product.amount?.takeIf { it.isNotBlank() }?.let {
                            Text(
                                text = "$it ₽",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        product.price?.takeIf { it.isNotBlank() }?.let {
                            Text(
                                text = "Цена: $it",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun LimitedOutlinedTextField(
    value: String,
    onValueChange: (String) -> Unit,
    maxChars: Int = 500,
    modifier: Modifier = Modifier,
    placeholder: String = ""
) {
    Column(modifier = modifier) {
        OutlinedTextField(
            value = value,
            onValueChange = { raw ->
                val clipped = if (raw.length > maxChars) raw.substring(0, maxChars) else raw
                if (clipped != value) {
                    onValueChange(clipped)
                } else if (raw == value) {
                    // no changes
                } else {
                    // length unchanged but content changed (e.g. replacement)
                    onValueChange(clipped)
                }
            },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { if (placeholder.isNotEmpty()) Text(placeholder) },
            singleLine = false,
            maxLines = Int.MAX_VALUE
        )
        Spacer(Modifier.height(4.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            Text(
                text = "${value.length}/$maxChars",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}