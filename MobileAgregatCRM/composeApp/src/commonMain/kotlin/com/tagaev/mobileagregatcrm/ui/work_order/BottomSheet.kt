package com.tagaev.mobileagregatcrm.ui.work_order

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
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
    onClose: () -> Unit
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .verticalScroll(scrollState)
    ) {
        // Header
        Text(
            text = "Заказ-наряд ${order.number ?: ""}",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold
        )
        order.date?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(Modifier.height(8.dp))

        // Vehicle + customer
        order.car?.takeIf { it.isNotBlank() }?.let {
            SectionTitle("Автомобиль")
            Text(it, style = MaterialTheme.typography.bodyMedium)
        }

        order.customer?.takeIf { it.isNotBlank() }?.let {
            SectionTitle("Заказчик")
            Text(it, style = MaterialTheme.typography.bodyMedium)
        }

        Row(
            modifier = Modifier.padding(top = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            order.repairType?.takeIf { it.isNotBlank() }?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Spacer(Modifier.width(8.dp))
            order.status?.let {
                WorkOrderStatusBadge(it)
            }
        }

        // Reason
        order.reason?.takeIf { it.isNotBlank() }?.let {
            SectionTitle("Причина обращения")
            Text(it, style = MaterialTheme.typography.bodyMedium)
        }

        // Error codes (разбиваем по \r)
        order.errorCodes?.takeIf { it.isNotBlank() }?.let { raw ->
            val lines = remember(raw) {
                raw.split("\\r".toRegex())
                    .map { it.trim() }
                    .filter { it.isNotEmpty() }
            }
            if (lines.isNotEmpty()) {
                SectionTitle("Ошибки / коды неисправностей")
                lines.forEach { line ->
                    Text(
                        "• $line",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }

        // Defects
        val defects = order.defects
        if (defects.isNotEmpty()) {
            SectionTitle("Дефектовка")
            defects.forEach { d ->
                Column(modifier = Modifier.padding(vertical = 2.dp)) {
                    Text(
                        text = d.name.orEmpty(),
                        style = MaterialTheme.typography.bodyMedium
                    )
                    if (!d.state.isNullOrBlank()) {
                        Text(
                            text = "Состояние: ${d.state}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (!d.decision.isNullOrBlank()) {
                        Text(
                            text = "Решение: ${d.decision}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (!d.description.isNullOrBlank()) {
                        Text(
                            text = d.description,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        }

        // Jobs
        val jobs = order.jobs
        if (jobs.isNotEmpty()) {
            SectionTitle("Работы")
            jobs.forEach { job ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            text = job.work.orEmpty(),
                            style = MaterialTheme.typography.bodyMedium
                        )
                        job.quantity?.let {
                            Text(
                                text = "Кол-во: $it",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    val price = job.price?.takeIf { it.isNotBlank() }
                    val amount = job.amount?.takeIf { it.isNotBlank() }
                    if (amount != null || price != null) {
                        Column(horizontalAlignment = Alignment.End) {
                            amount?.let {
                                Text(
                                    text = "$it ₽",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            price?.let {
                                Text(
                                    text = "цена: $it",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }

        // Messages / комментарии по работам
        val messages = order.messages
        if (messages.isNotEmpty()) {
            SectionTitle("Комментарий по работам")
            messages.forEach { msg ->
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                ) {
                    msg.comment?.let {
                        Text(
                            text = it,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        msg.author?.takeIf { it.isNotBlank() }?.let {
                            Text(
                                text = it,
                                style = MaterialTheme.typography.bodySmall,
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
                }
            }
        }

        Spacer(Modifier.height(24.dp))

        // Close button (optional, sheet можно и свайпом закрыть)
        OutlinedButton(
            onClick = onClose,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        ) {
            Text("Закрыть")
        }

        Spacer(Modifier.height(12.dp))
    }
}
