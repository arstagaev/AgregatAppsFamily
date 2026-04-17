package com.tagaev.trrcrm.ui.custom

import androidx.compose.foundation.layout.size
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import compose.icons.FeatherIcons
import compose.icons.feathericons.Search

@Composable
fun SearchIconButtonWithIndicator(
    showIndicator: Boolean,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    IconButton(
        onClick = onClick,
        enabled = enabled
    ) {
        BadgedBox(
            badge = {
                if (showIndicator) {
                    Badge(
                        modifier = Modifier.size(8.dp),
                        containerColor = MaterialTheme.colorScheme.error,
                    )
                }
            }
        ) {
            Icon(FeatherIcons.Search, contentDescription = "Поиск")
        }
    }
}
