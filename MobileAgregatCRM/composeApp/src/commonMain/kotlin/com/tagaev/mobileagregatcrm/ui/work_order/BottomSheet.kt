package com.tagaev.mobileagregatcrm.ui.work_order

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.tagaev.mobileagregatcrm.models.WorkOrderDto

// same file: WorkOrdersScreen.kt
@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(top = 16.dp, bottom = 4.dp)
    )
}

@Composable
fun WorkOrderDetailsSheet(
    order: WorkOrderDto,
    onClose: () -> Unit,
    onSendMessage: (String) -> Unit
) {
    val scrollState = rememberScrollState()
    var messageDraft by remember(order.guid) { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .verticalScroll(scrollState)
    ) {
        // 1. Организация + подразделение (две колонки)
        if (!order.organization.isNullOrBlank() || !order.branch.isNullOrBlank()) {

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                if (!order.organization.isNullOrBlank()) {
                    Column(modifier = Modifier.weight(1f)) {
                        SectionTitle("Организация:")
                        Text(
                            text = order.organization.orEmpty(),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                } else {
                    Spacer(Modifier.weight(1f))
                }
                if (!order.branch.isNullOrBlank()) {
                    Column(modifier = Modifier.weight(1f)) {
                        SectionTitle("Подразделение:")
//                        Text(
//                            text = "Подразделение:",
//                            style = MaterialTheme.typography.bodySmall,
//                            fontWeight = FontWeight.SemiBold,
//                            color = MaterialTheme.colorScheme.onSurfaceVariant
//                        )
                        Text(
                            text = order.branch.orEmpty(),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        }

        // 2. Документ + заказчик (две колонки)
        if (!order.link.isNullOrBlank() || !order.customer.isNullOrBlank()) {

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                if (!order.link.isNullOrBlank()) {
                    Column(modifier = Modifier.weight(1f)) {
//                        Text(
//                            text = "Документ:",
//                            style = MaterialTheme.typography.bodySmall,
//                            fontWeight = FontWeight.SemiBold,
//                            color = MaterialTheme.colorScheme.onSurfaceVariant
//                        )
                        SectionTitle("Документ:")
                        Text(
                            text = order.link.orEmpty(),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                } else {
                    Spacer(Modifier.weight(1f))
                }
                if (!order.customer.isNullOrBlank()) {
                    Column(modifier = Modifier.weight(1f)) {
                        SectionTitle("Заказчик:")
                        Text(
                            text = order.customer.orEmpty(),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        }

        // 4. Автомобиль
        order.car?.takeIf { it.isNotBlank() }?.let {
            SectionTitle("Автомобиль:")
            Text(
                text = it,
                style = MaterialTheme.typography.bodyMedium
            )
        }

        // 5. Тип КПП / Тип двигателя / Пробег
        if (!order.gearboxType.isNullOrBlank() ||
            !order.engineType.isNullOrBlank() ||
            !order.mileage.isNullOrBlank()
        ) {
            SectionTitle("Хар-ки автомобиля:")
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                if (!order.gearboxType.isNullOrBlank()) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Тип КПП:",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = order.gearboxType.orEmpty(),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
                if (!order.engineType.isNullOrBlank()) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Тип ДВС:",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = order.engineType.orEmpty(),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
                if (!order.mileage.isNullOrBlank()) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Пробег:",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = order.mileage.orEmpty(),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
                if (!order.carAge.isNullOrBlank()) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Год выпуска:",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = order.carAge.orEmpty(),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        }

        // 6. Состояние + Вид ремонта (две колонки)
        if (!order.status.isNullOrBlank() || !order.repairType.isNullOrBlank()) {
//            SectionTitle("Состояние и вид ремонта:")
            Row(
                verticalAlignment = Alignment.Top,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (!order.status.isNullOrBlank()) {
                    Column(modifier = Modifier.weight(1f)) {
                        SectionTitle("Состояние")

                        Spacer(Modifier.height(4.dp))
                        WorkOrderStatusBadge(order.status.orEmpty())
                    }
                }
                if (!order.repairType.isNullOrBlank()) {
                    Column(modifier = Modifier.weight(1f)) {
                        SectionTitle("Вид ремонта:")

                        Text(
                            text = order.repairType.orEmpty(),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }

        // 7. Ошибка + Причина обращения (две колонки)
        if (!order.errorCodes.isNullOrBlank() || !order.reason.isNullOrBlank()) {
//            SectionTitle("Ошибки:")
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                if (!order.errorCodes.isNullOrBlank()) {
                    Column(modifier = Modifier.weight(1f)) {
//                        Text(
//                            text = "Коды ошибок:",
//                            style = MaterialTheme.typography.bodySmall,
//                            fontWeight = FontWeight.SemiBold
//                        )
                        SectionTitle("Коды ошибок:")
                        val lines = remember(order.errorCodes) {
                            order.errorCodes!!
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
                                text = order.errorCodes ?: "Нету",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
                if (!order.reason.isNullOrBlank()) {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(start = 8.dp)
                    ) {
                        SectionTitle("Причина обращения:")

                        Text(
                            text = order.reason.orEmpty(),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        }

        // 8. Выполненные работы по заказ-наряду (используем только "Работы")
        val jobs = order.jobs
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
                        Text(
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
        val products = order.products
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
                        Text(
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
                            Text(
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

        // 10. Сообщения + форма отправки (внизу)
        val messages = order.messages
        if (!messages.isNullOrEmpty()) {
            SectionTitle("Комментарии:")
            messages.forEach { msg ->
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        msg.author?.takeIf { it.isNotBlank() }?.let {
                            Text(
                                text = it,
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(Modifier.width(8.dp))
                        msg.workDate?.takeIf { it.isNotBlank() }?.let {
                            Text(
                                text = it,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    msg.comment?.takeIf { it.isNotBlank() }?.let {
                        Text(
                            text = it,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }

                }
            }
        }

        SectionTitle("Добавить комментарий")
        OutlinedTextField(
            value = messageDraft,
            onValueChange = { messageDraft = it },
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 80.dp),
            placeholder = { Text("Комментарий по работам…") }
        )

        Spacer(Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedButton(
                onClick = onClose,
                modifier = Modifier.weight(1f)
            ) {
                Text("Закрыть")
            }

            Button(
                onClick = {
                    val trimmed = messageDraft.trim()
                    if (trimmed.isNotEmpty()) {
                        onSendMessage(trimmed)
                        messageDraft = ""
                    }
                },
                modifier = Modifier.weight(1f),
                enabled = messageDraft.isNotBlank() &&
                        !order.number.isNullOrBlank() &&
                        !order.date.isNullOrBlank()
            ) {
                Text("Отправить")
            }
        }

        Spacer(Modifier.height(12.dp))
    }
}