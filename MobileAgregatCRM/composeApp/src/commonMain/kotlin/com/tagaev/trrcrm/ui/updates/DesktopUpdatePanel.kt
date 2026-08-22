package com.tagaev.trrcrm.ui.updates

import com.tagaev.trrcrm.ui.i18n.s
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.tagaev.trrcrm.updates.DesktopUpdateUiState
import com.tagaev.trrcrm.updates.SemVerParser
import compose.icons.FeatherIcons
import compose.icons.feathericons.Download
import compose.icons.feathericons.RefreshCw

@Composable
fun DesktopUpdatePanel(
    state: DesktopUpdateUiState,
    onCheck: () -> Unit,
    onInstall: () -> Unit,
    onCancelDownload: () -> Unit,
    onDismiss: () -> Unit,
    onClearError: () -> Unit,
) {
    val isUpToDate = remember(state.currentVersion, state.latestVersion, state.availableRelease) {
        val current = SemVerParser.parseOrNull(state.currentVersion.orEmpty())
        val latest = SemVerParser.parseOrNull(state.latestVersion.orEmpty())
        val release = SemVerParser.parseOrNull(state.availableRelease?.version.orEmpty())
        when {
            current == null -> false
            latest != null -> current >= latest
            release != null -> current >= release
            else -> state.availableRelease == null
        }
    }
    val canOfferInstall = state.availableRelease != null && !isUpToDate

    ElevatedCard(
        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(s("menu_obnovleniya_desktop"), style = MaterialTheme.typography.titleMedium)
            state.currentVersion?.takeIf { it.isNotBlank() }?.let {
                Text("Текущая версия: $it", style = MaterialTheme.typography.bodySmall)
            }
            state.latestVersion?.takeIf { it.isNotBlank() }?.let {
                Text("Последняя версия: $it", style = MaterialTheme.typography.bodySmall)
            }
            if (isUpToDate && state.errorMessage == null && !state.isBusy) {
                Text(
                    s("menu_ustanovlena_aktualnaya_versiya_obnovlenie_ne_trebuet"),
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            if (state.statusMessage.isNotBlank()) {
                Text(state.statusMessage, style = MaterialTheme.typography.bodyMedium)
            }
            state.progress?.let { progress ->
                LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth())
            }
            state.availableRelease?.let { release ->
                val color = if (release.isMandatory) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                Text(
                    text = "Доступна версия ${release.version}" + if (release.isMandatory) " (обязательное)" else "",
                    color = color,
                    style = MaterialTheme.typography.bodyMedium
                )
                release.fileSize?.takeIf { it > 0 }?.let { size ->
                    val sizeMb = size / (1024.0 * 1024.0)
                    val roundedSizeMb = (sizeMb * 10.0).toInt() / 10.0
                    Text("Размер: ${roundedSizeMb} МБ", style = MaterialTheme.typography.bodySmall)
                }
                if (release.changelog.isNotBlank()) {
                    Text(release.changelog, style = MaterialTheme.typography.bodySmall, maxLines = 4, overflow = TextOverflow.Ellipsis)
                }
            }
            state.errorMessage?.let {
                Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onCheck, enabled = !state.isBusy) {
                    Icon(FeatherIcons.RefreshCw, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text(s("menu_proverit"))
                }
                if (canOfferInstall) {
                    Button(onClick = onInstall, enabled = !state.isBusy) {
                        Icon(FeatherIcons.Download, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text(s("menu_ustanovit"))
                    }
                }
                if (state.canCancelDownload && state.isBusy) {
                    TextButton(onClick = onCancelDownload) { Text(s("menu_otmenit")) }
                }
                if (state.errorMessage != null) {
                    TextButton(onClick = onClearError, enabled = !state.isBusy) { Text(s("login_skryt")) }
                } else if (state.availableRelease?.isMandatory == false) {
                    TextButton(onClick = onDismiss, enabled = !state.isBusy) { Text(s("menu_pozzhe")) }
                }
            }
        }
    }
}
