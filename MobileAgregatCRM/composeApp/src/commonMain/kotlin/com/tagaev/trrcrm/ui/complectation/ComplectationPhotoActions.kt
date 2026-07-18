package com.tagaev.trrcrm.ui.complectation

import com.tagaev.trrcrm.ui.i18n.s

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import compose.icons.FeatherIcons
import compose.icons.feathericons.Camera
import compose.icons.feathericons.Image

@Composable
fun ComplectationOpenPhotosButton(
    photoCount: Int,
    isLoading: Boolean = false,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    val isClickable = enabled && !isLoading && photoCount > 0
    val label = when {
        isLoading -> s("complectation_otkryt_fotografii")
        photoCount > 0 -> s("complectation_otkryt_fotografii_photocount", photoCount)
        else -> s("complectation_fotografiy_ne_zagruzheno")
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .alpha(if (isClickable) 1f else 0.45f)
            .clip(MaterialTheme.shapes.small)
            .clickable(enabled = isClickable, onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium,
                    fontSize = 13.sp,
                    maxLines = 1,
                )
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                    )
                }
            }
            if (!isLoading) {
                Icon(
                    FeatherIcons.Image,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                )
            }
        }
    }
}

@Composable
fun ComplectationAddPhotoTopBarAction(
    enabled: Boolean,
    onClick: () -> Unit,
) {
    Card(
        modifier = Modifier
            .padding(end = 4.dp)
            .alpha(if (enabled) 1f else 0.45f)
            .clip(MaterialTheme.shapes.small)
            .clickable(enabled = enabled, onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = s("complectation_dobavit_foto"),
                style = MaterialTheme.typography.labelMedium,
                fontSize = 12.sp,
                maxLines = 1,
            )
            Icon(
                FeatherIcons.Camera,
                contentDescription = null,
                modifier = Modifier.size(14.dp),
            )
        }
    }
}
