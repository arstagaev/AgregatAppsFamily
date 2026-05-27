package com.tagaev.trrcrm.ui.menu

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import compose.icons.LineAwesomeIcons
import compose.icons.FeatherIcons
import compose.icons.feathericons.Download
import compose.icons.feathericons.RefreshCw
import compose.icons.lineawesomeicons.AlignJustifySolid
import compose.icons.lineawesomeicons.ToolboxSolid
import com.tagaev.trrcrm.updates.SemVerParser
import com.tagaev.trrcrm.updates.DesktopUpdateUiState

/**
 * Decompose component for this screen.
 * Root component will create this and handle navigation.
 */
//interface MenuComponent {
//    fun openCargo()
//    // add more functions later for other cards
//}

/**
 * Top-level screen composable used by Decompose.
 */
@Composable
fun MenuScreen(
    component: IMenuComponent,
    modifier: Modifier = Modifier
) {
    val updateState by component.desktopUpdateState.collectAsState()
    Column(
        modifier = modifier
            .fillMaxSize()//.snowflakeBackground()
            .padding(16.dp)
    ) {
        Text(
            text = "Меню",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        MenuGridScreen(
            items = remember(updateState.supported) {
                buildList {
                    if (updateState.supported) {
                        add(
                            MenuCardData(
                                id = "check_update",
                                title = "Обновить",
                                iconRes = FeatherIcons.RefreshCw
                            )
                        )
                    }
                    add(
                        MenuCardData(
                            id = "catalog",
                            title = "Каталог",
                            iconRes = LineAwesomeIcons.AlignJustifySolid
                        )
                    )
                    add(
                        MenuCardData(
                            id = "settings",
                            title = "Настройки",
                            iconRes = LineAwesomeIcons.ToolboxSolid
                        )
                    )
                }
            },
            onItemClick = { item ->
                when (item.id) {
//                    "cargo" -> component.openCargo()
                    "check_update" -> component.checkForDesktopUpdate()
                    "catalog" -> component.openCatalog()
                    "settings" -> component.openSettings()
                    // add other when branches for new cards
                }
            },
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        )
        if (updateState.supported) {
            DesktopUpdatePanel(
                state = updateState,
                onCheck = component::checkForDesktopUpdate,
                onInstall = component::installDesktopUpdate,
                onCancelDownload = component::cancelDesktopUpdateDownload,
                onDismiss = component::dismissDesktopUpdate,
                onClearError = component::clearDesktopUpdateError
            )
        }
    }
}

/* --- Internal UI model & composables --- */

data class MenuCardData(
    val id: String,
    val title: String,
    val iconRes: ImageVector // we’ll resolve to painterResource inside
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun MenuGridScreen(
    items: List<MenuCardData>,
    onItemClick: (MenuCardData) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(items) { item ->
            MenuCard(
                item = item,
                onClick = { onItemClick(item) }
            )
        }
    }
}

@Composable
private fun DesktopUpdatePanel(
    state: DesktopUpdateUiState,
    onCheck: () -> Unit,
    onInstall: () -> Unit,
    onCancelDownload: () -> Unit,
    onDismiss: () -> Unit,
    onClearError: () -> Unit
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
            Text("Обновления Desktop", style = MaterialTheme.typography.titleMedium)
            state.currentVersion?.takeIf { it.isNotBlank() }?.let {
                Text("Текущая версия: $it", style = MaterialTheme.typography.bodySmall)
            }
            state.latestVersion?.takeIf { it.isNotBlank() }?.let {
                Text("Последняя версия: $it", style = MaterialTheme.typography.bodySmall)
            }
            if (isUpToDate && state.errorMessage == null && !state.isBusy) {
                Text(
                    "Установлена актуальная версия. Обновление не требуется.",
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
                    Text("Проверить")
                }
                if (canOfferInstall) {
                    Button(onClick = onInstall, enabled = !state.isBusy) {
                        Icon(FeatherIcons.Download, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text("Установить")
                    }
                }
                if (state.canCancelDownload && state.isBusy) {
                    TextButton(onClick = onCancelDownload) { Text("Отменить") }
                }
                if (state.errorMessage != null) {
                    TextButton(onClick = onClearError, enabled = !state.isBusy) { Text("Скрыть") }
                } else if (state.availableRelease?.isMandatory == false) {
                    TextButton(onClick = onDismiss, enabled = !state.isBusy) { Text("Позже") }
                }
            }
        }
    }
}

@Composable
private fun MenuCard(
    item: MenuCardData,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
//    val painter = painterResource(item.iconRes)

    Card(
        modifier = modifier
            .fillMaxWidth()
            // 1:1 ratio for square menu tiles
            .aspectRatio(1f)
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Top 70%: icon / image
            Box(
                modifier = Modifier
                    .weight(0.7f)
                    .fillMaxWidth()
                    .padding(8.dp),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    imageVector = item.iconRes,
                    contentDescription = item.title,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit
                )
            }

            // Bottom 30%: title
            Box(
                modifier = Modifier
                    .weight(0.3f)
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Text(
                    text = item.title,
                    fontSize = 20.sp,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
