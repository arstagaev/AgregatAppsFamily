package com.tagaev.trrcrm.ui.repair_template_catalog

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.tagaev.trrcrm.models.RepairTemplateCatalogItemDto
import com.tagaev.trrcrm.models.customerRowsForDisplay
import com.tagaev.trrcrm.models.hasCustomersSection
import com.tagaev.trrcrm.models.nonEmptyHeaderPairs
import com.tagaev.trrcrm.ui.custom.TextC
import compose.icons.FeatherIcons
import compose.icons.feathericons.ArrowLeft

@Composable
fun RepairTemplateCatalogDetailsSheet(
    item: RepairTemplateCatalogItemDto,
    onBack: () -> Unit,
) {
    val scroll = rememberScrollState()
    val title = item.name?.trim()?.takeIf { it.isNotEmpty() }
        ?: item.link?.trim()?.takeIf { it.isNotEmpty() }
        ?: item.code?.trim().orEmpty()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scroll)
            .padding(16.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(FeatherIcons.ArrowLeft, contentDescription = "Назад")
            }
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f).padding(end = 8.dp),
            )
        }
        Spacer(Modifier.height(12.dp))

        val headerPairs = item.nonEmptyHeaderPairs()
        headerPairs.chunked(2).forEach { row ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 10.dp),
            ) {
                row.forEachIndexed { index, (label, value) ->
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(end = if (index == 0 && row.size > 1) 8.dp else 0.dp),
                    ) {
                        TextC(
                            text = label,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        TextC(
                            text = value,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }
                if (row.size == 1) {
                    Spacer(Modifier.weight(1f))
                }
            }
        }

        if (item.works.isNotEmpty()) {
            Spacer(Modifier.height(8.dp))
            HorizontalDivider()
            Spacer(Modifier.height(12.dp))
            Text(
                text = "Работы",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.height(8.dp))
            item.works.forEachIndexed { i, line ->
                if (i > 0) Spacer(Modifier.height(8.dp))
                TextC(
                    text = listOfNotNull(
                        line.lineNumber?.takeIf { it.isNotBlank() }?.let { "№ $it" },
                        line.work?.trim()?.takeIf { it.isNotEmpty() },
                        line.normHours?.trim()?.takeIf { it.isNotEmpty() }?.let { "н/ч $it" },
                        line.coefficient?.trim()?.takeIf { it.isNotEmpty() }?.let { "к ×$it" },
                        line.price?.trim()?.takeIf { it.isNotEmpty() }?.let { "цена $it" },
                    ).joinToString(" · "),
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }

        if (item.goods.isNotEmpty()) {
            Spacer(Modifier.height(12.dp))
            HorizontalDivider()
            Spacer(Modifier.height(12.dp))
            Text(
                text = "Товары",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.height(8.dp))
            item.goods.forEachIndexed { i, line ->
                if (i > 0) Spacer(Modifier.height(8.dp))
                TextC(
                    text = listOfNotNull(
                        line.lineNumber?.takeIf { it.isNotBlank() }?.let { "№ $it" },
                        line.nomenclature?.trim()?.takeIf { it.isNotEmpty() },
                        line.quantity?.trim()?.takeIf { it.isNotEmpty() }?.let { "кол-во $it" },
                        line.analog?.trim()?.takeIf { it.isNotEmpty() }?.let { "аналог $it" },
                        line.price?.trim()?.takeIf { it.isNotEmpty() }?.let { "цена $it" },
                    ).joinToString(" · "),
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }

        if (item.hasCustomersSection()) {
            Spacer(Modifier.height(12.dp))
            HorizontalDivider()
            Spacer(Modifier.height(12.dp))
            Text(
                text = "Заказчики",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.height(8.dp))
            item.customerRowsForDisplay().forEachIndexed { i, row ->
                if (i > 0) Spacer(Modifier.height(6.dp))
                TextC(
                    text = row,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
}
