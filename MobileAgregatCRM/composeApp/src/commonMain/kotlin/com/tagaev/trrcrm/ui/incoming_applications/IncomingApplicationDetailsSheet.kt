package com.tagaev.trrcrm.ui.incoming_applications

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
import com.tagaev.trrcrm.models.IncomingApplicationDto
import com.tagaev.trrcrm.models.nonEmptyDisplayRows
import com.tagaev.trrcrm.ui.custom.TextC
import compose.icons.FeatherIcons
import compose.icons.feathericons.ArrowLeft

@Composable
fun IncomingApplicationDetailsSheet(
    item: IncomingApplicationDto,
    onBack: () -> Unit,
) {
    val scroll = rememberScrollState()
    val title = item.link?.trim()?.takeIf { it.isNotEmpty() }
        ?: listOfNotNull(item.number, item.date).joinToString(" · ")

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

        val rows = item.nonEmptyDisplayRows()
        rows.forEachIndexed { index, (label, value) ->
            if (index > 0) {
                Spacer(Modifier.height(8.dp))
                HorizontalDivider()
                Spacer(Modifier.height(8.dp))
            }
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
}
