package com.tagaev.trrcrm.ui.expense_requests

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.tagaev.trrcrm.domain.TreeRootDocument
import com.tagaev.trrcrm.models.ExpenseRequestDto
import com.tagaev.trrcrm.models.ExpenseRequestSignatoryDto
import com.tagaev.trrcrm.models.meaningfulSignatories
import com.tagaev.trrcrm.models.primaryDetailGridRows
import com.tagaev.trrcrm.models.supplementaryDetailGridRows
import com.tagaev.trrcrm.ui.cargo.ExpandableListSection
import com.tagaev.trrcrm.ui.custom.TextC

@Composable
fun ExpenseRequestDetailsSheet(
    item: ExpenseRequestDto,
    onOpenBaseDocument: (String) -> Unit = {},
) {
    val scroll = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scroll)
            .padding(horizontal = 4.dp, vertical = 2.dp),
    ) {
        ExpenseRequestDetailsGrid(
            rows = item.primaryDetailGridRows(),
            onOpenBaseDocument = onOpenBaseDocument,
        )

        val supplementary = item.supplementaryDetailGridRows()
        if (supplementary.isNotEmpty()) {
            Spacer(Modifier.height(12.dp))
            Text(
                text = "Дополнительно",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(bottom = 8.dp),
            )
            ExpenseRequestDetailsGrid(
                rows = supplementary,
                onOpenBaseDocument = onOpenBaseDocument,
            )
        }

        val signatories = item.meaningfulSignatories()
        if (signatories.isNotEmpty()) {
            Spacer(Modifier.height(12.dp))
            ExpandableListSection(
                title = "Подписанты (${signatories.size})",
                items = signatories,
            ) { signatory ->
                SignatoryRow(signatory)
            }
        }
    }
}

@Composable
private fun ExpenseRequestDetailsGrid(
    rows: List<Pair<Pair<String, String>, Pair<String, String>?>>,
    onOpenBaseDocument: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val borderColor = MaterialTheme.colorScheme.outlineVariant

    Column(
        modifier = modifier
            .fillMaxWidth()
            .border(width = 1.dp, color = borderColor),
    ) {
        rows.forEachIndexed { index, (left, right) ->
            if (index > 0) {
                HorizontalDivider(color = borderColor, thickness = 1.dp)
            }
            ExpenseRequestGridRow(
                left = left,
                right = right,
                borderColor = borderColor,
                onOpenBaseDocument = onOpenBaseDocument,
            )
        }
    }
}

@Composable
private fun ExpenseRequestGridRow(
    left: Pair<String, String>,
    right: Pair<String, String>?,
    borderColor: androidx.compose.ui.graphics.Color,
    onOpenBaseDocument: (String) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min),
    ) {
        ExpenseRequestGridCell(
            label = left.first,
            value = left.second,
            modifier = Modifier.weight(1f),
            onOpenBaseDocument = onOpenBaseDocument,
        )
        if (right != null) {
            VerticalDivider(
                modifier = Modifier.fillMaxHeight(),
                color = borderColor,
                thickness = 1.dp,
            )
            ExpenseRequestGridCell(
                label = right.first,
                value = right.second,
                modifier = Modifier.weight(1f),
                onOpenBaseDocument = onOpenBaseDocument,
            )
        }
    }
}

@Composable
private fun ExpenseRequestGridCell(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    onOpenBaseDocument: (String) -> Unit = {},
) {
    val isNavigableBaseDocument = remember(label, value) {
        label == "Документ-основание" &&
            value != "—" &&
            TreeRootDocument.parse(value) != null
    }

    Column(
        modifier = modifier.padding(horizontal = 10.dp, vertical = 8.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(4.dp))
        if (isNavigableBaseDocument) {
            TextC(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.clickable { onOpenBaseDocument(value) },
                allowLinkTap = false,
                allowLongPressCopy = false,
            )
        } else {
            TextC(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

@Composable
private fun SignatoryRow(signatory: ExpenseRequestSignatoryDto) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
    ) {
        TextC(
            text = signatory.user?.trim().orEmpty().ifBlank { "—" },
            style = MaterialTheme.typography.bodyMedium,
        )
        val status = signatory.status?.trim().orEmpty()
        val changeDate = signatory.changeDate?.trim().orEmpty()
        val details = buildList {
            if (status.isNotEmpty() && status != "01.01.0001 0:00:00") add(status)
            if (changeDate.isNotEmpty() && changeDate != "01.01.0001 0:00:00") add(changeDate)
        }
        if (details.isNotEmpty()) {
            TextC(
                text = details.joinToString(" · "),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
