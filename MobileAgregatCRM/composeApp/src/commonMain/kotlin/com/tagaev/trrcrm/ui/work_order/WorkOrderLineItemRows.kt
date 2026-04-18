package com.tagaev.trrcrm.ui.work_order

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tagaev.trrcrm.models.WorkOrderExecutorDto
import com.tagaev.trrcrm.models.WorkOrderJobDto
import com.tagaev.trrcrm.models.WorkOrderProductDto
import com.tagaev.trrcrm.ui.custom.TextC
import androidx.compose.runtime.remember

/** Horizontal padding aligned with [WorkOrderLineItemsExpandableDividerOutdent] for list dividers */
val WorkOrderLineItemsExpandableListPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)

val WorkOrderLineItemsExpandableDividerOutdent = 8.dp

internal fun dashOr(text: String?): String =
    text?.trim()?.takeIf { it.isNotBlank() } ?: "—"

/** `null` если нет ни количества, ни ед. изм. */
internal fun formatProductQuantityWithUnit(quantity: String?, unit: String?): String? {
    val q = quantity?.trim().orEmpty()
    val u = unit?.trim().orEmpty()
    if (q.isEmpty() && u.isEmpty()) return null
    return when {
        q.isNotEmpty() && u.isNotEmpty() -> "$q $u"
        q.isNotEmpty() -> q
        else -> u
    }
}

internal fun formatRubleAmount(raw: String?): String {
    val s = raw?.trim().orEmpty()
    if (s.isBlank()) return "—"
    return if (s.contains('₽')) s else "$s ₽"
}

internal fun productDisplayTitle(p: WorkOrderProductDto): String {
    val line = p.lineNumber?.trim()?.takeIf { it.isNotBlank() }?.let { ln -> "Стр. $ln" }
    return sequenceOf(p.name, p.characteristic, p.note, p.notePrint, p.article, line)
        .mapNotNull { it?.trim()?.takeIf(String::isNotBlank) }
        .firstOrNull()
        ?: "—"
}

internal fun productCharacteristicTitle(p: WorkOrderProductDto): String =
    sequenceOf(p.characteristic, p.note, p.notePrint, p.article)
        .mapNotNull { it?.trim()?.takeIf(String::isNotBlank) }
        .firstOrNull() ?: "—"

internal fun jobDisplayTitle(j: WorkOrderJobDto): String {
    val line = j.lineNumber?.trim()?.takeIf { it.isNotBlank() }?.let { ln -> "Стр. $ln" }
    return sequenceOf(j.work, j.workLine1, j.note, j.notePrint, j.workPackageNumber, line)
        .mapNotNull { it?.trim()?.takeIf(String::isNotBlank) }
        .firstOrNull()
        ?: "—"
}

internal fun jobExecutorTitle(job: WorkOrderJobDto, executors: List<WorkOrderExecutorDto>): String {
    val id = job.workId?.trim().orEmpty()
    if (id.isBlank()) return "—"
    return executors.asSequence()
        .filter { it.workId?.trim() == id }
        .mapNotNull { it.executor?.trim()?.takeIf(String::isNotBlank) }
        .distinct()
        .joinToString(", ")
        .ifBlank { "—" }
}

@Composable
fun WorkOrderProductLineRowCompact(
    product: WorkOrderProductDto,
    onNomenclatureCharacteristicSearch: ((String) -> Unit)? = null,
) {
    val charRaw = product.characteristic?.trim().orEmpty()
    val charDisplay = if (onNomenclatureCharacteristicSearch != null) {
        charRaw.ifEmpty { "—" }
    } else {
        productCharacteristicTitle(product)
    }
    val charMaxLines = if (onNomenclatureCharacteristicSearch != null) 10 else 2
    val onCharClick: (() -> Unit)? =
        onNomenclatureCharacteristicSearch?.takeIf { charRaw.isNotEmpty() }
            ?.let { { it(charRaw) } }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp, horizontal = 2.dp)
    ) {
        TextC(
            text = productDisplayTitle(product),
            style = MaterialTheme.typography.titleSmall.copy(
                fontSize = 14.sp,
                lineHeight = 16.sp,
                fontWeight = FontWeight.SemiBold
            ),
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(Modifier.height(3.dp))
        Spacer(Modifier.height(5.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.Top
        ) {
            WorkOrderProductMetaCell(
                label = "хар-ка",
                value = charDisplay,
                modifier = Modifier.weight(1f),
                emphasize = true,
                onValueClick = onCharClick,
                valueMaxLines = charMaxLines,
                usePrimaryForValue = onCharClick != null
            )
            WorkOrderProductMetaCell(
                label = "кол-во",
                value = formatProductQuantityWithUnit(product.quantity, product.unit) ?: "—",
                modifier = Modifier.weight(1f),
                emphasize = true
            )
            WorkOrderProductMetaCell(
                label = "цена",
                value = dashOr(product.price),
                modifier = Modifier.weight(1f)
            )
            WorkOrderProductMetaCell(
                label = "сумма",
                value = formatRubleAmount(product.amount),
                modifier = Modifier.weight(1f),
                emphasize = true
            )
        }
        product.cell?.takeIf { it.isNotBlank() }?.let { cell ->
            Spacer(Modifier.height(4.dp))
            TextC(
                text = "Ячейка: $cell",
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp, lineHeight = 13.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun WorkOrderProductMetaCell(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    emphasize: Boolean = false,
    onValueClick: (() -> Unit)? = null,
    valueMaxLines: Int = 2,
    usePrimaryForValue: Boolean = false,
) {
    val interaction = remember { MutableInteractionSource() }
    val valueModifier = if (onValueClick != null) {
        Modifier.clickable(
            interactionSource = interaction,
            indication = null,
            onClick = onValueClick
        )
    } else {
        Modifier
    }
    Column(modifier = modifier) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, lineHeight = 10.sp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(Modifier.height(1.dp))
        TextC(
            text = value,
            modifier = valueModifier,
            style = MaterialTheme.typography.bodySmall.copy(
                fontSize = 11.sp,
                lineHeight = 13.sp,
                fontWeight = if (emphasize) FontWeight.SemiBold else FontWeight.Medium
            ),
            color = if (usePrimaryForValue) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.onSurface,
            maxLines = valueMaxLines,
            overflow = if (valueMaxLines > 2) TextOverflow.Clip else TextOverflow.Ellipsis,
            allowLinkTap = false,
            allowLongPressCopy = false
        )
    }
}

@Composable
fun WorkOrderJobLineRowCompact(
    job: WorkOrderJobDto,
    executors: List<WorkOrderExecutorDto>,
) {
    val executorTitle = jobExecutorTitle(job, executors)
    val normWithCoefficient = buildString {
        val qty = job.quantity?.trim().orEmpty()
        val coef = job.coefficient?.trim().orEmpty()
        when {
            qty.isNotBlank() && coef.isNotBlank() -> append(coef)
            qty.isNotBlank() -> append(qty)
            coef.isNotBlank() -> append(coef)
            else -> append("—")
        }
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 6.dp, horizontal = 8.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.Top
            ) {
                Column(
                    modifier = Modifier.weight(1.25f),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = "Работа",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, lineHeight = 11.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = jobDisplayTitle(job),
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontSize = 12.sp,
                            lineHeight = 14.sp,
                            fontWeight = FontWeight.SemiBold
                        ),
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.height(1.dp))
                    Text(
                        text = "Исполнитель",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, lineHeight = 11.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = dashOr(executorTitle),
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontSize = 12.sp,
                            lineHeight = 14.sp,
                            fontWeight = FontWeight.SemiBold
                        ),
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                    horizontalAlignment = Alignment.End
                ) {
                    WorkOrderJobMetaLine(
                        label = "Количество: ",
                        value = dashOr(job.quantity),
                        emphasize = true
                    )
                    WorkOrderJobMetaLine(
                        label = "Норма вр. (ч.): ",
                        value = normWithCoefficient
                    )
                    WorkOrderJobMetaLine(
                        label = "Цена: ",
                        value = dashOr(job.price)
                    )
                    WorkOrderJobMetaLine(
                        label = "Сумма: ",
                        value = formatRubleAmount(job.amount),
                        emphasize = true
                    )
                }
            }
        }
    }
}

@Composable
private fun WorkOrderJobMetaLine(
    label: String,
    value: String,
    emphasize: Boolean = false,
) {
    Row {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, lineHeight = 11.sp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Right
        )
        Spacer(Modifier.width(2.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall.copy(
                fontSize = 12.sp,
                lineHeight = 13.sp,
                fontWeight = if (emphasize) FontWeight.SemiBold else FontWeight.Medium
            ),
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Right
        )
    }
}
