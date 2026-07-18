package com.tagaev.trrcrm.ui.settings

import com.tagaev.trrcrm.ui.i18n.s

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.unit.dp
import com.tagaev.trrcrm.navigation.BottomNavItemId
import com.tagaev.trrcrm.navigation.BottomNavItemIcon
import com.tagaev.trrcrm.navigation.BottomNavLayoutResolver
import com.tagaev.trrcrm.utils.SessionPermissions
import compose.icons.FeatherIcons
import compose.icons.feathericons.ArrowLeft
import compose.icons.feathericons.ChevronDown
import compose.icons.feathericons.ChevronUp
import compose.icons.feathericons.Eye
import compose.icons.feathericons.EyeOff

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BottomNavLayoutEditorScreen(
    component: BottomNavLayoutEditorHost,
    contextTabId: BottomNavItemId?,
    onBack: () -> Unit,
    onNavigateHome: () -> Unit,
) {
    val draft by component.bottomNavDraft.collectAsState(initial = emptyList())
    val dirty by component.bottomNavDirty.collectAsState(initial = false)
    val saveError by component.bottomNavSaveError.collectAsState(initial = null)
    val permissionMap by SessionPermissions.state

    val editorItems = remember(draft, permissionMap) {
        BottomNavLayoutResolver.resolveEditorItems(draft)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(s("settings_panel_navigatsii")) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(FeatherIcons.ArrowLeft, contentDescription = s("settings_nazad"))
                    }
                },
            )
        },
        bottomBar = {
            Surface(tonalElevation = 3.dp) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                ) {
                    if (saveError != null) {
                        Text(
                            text = saveError.orEmpty(),
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(bottom = 8.dp),
                        )
                    }
                    Button(
                        onClick = { performSave(component, contextTabId, onNavigateHome, onBack) },
                        enabled = dirty,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(s("settings_sohranit"))
                    }
                }
            }
        },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            item {
                Text(
                    text = s("settings_glavnaya_vsegda_pervaya_sobytiya_qr_skaner_i_menyu_n"),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 8.dp),
                )
            }

            itemsIndexed(
                items = editorItems,
                key = { _, item -> item.id },
            ) { index, item ->
                val tabId = BottomNavItemId.fromWire(item.id) ?: return@itemsIndexed

                BottomNavEditorRow(
                    tabId = tabId,
                    visible = item.visible,
                    canHide = tabId.isHideable,
                    canMoveUp = index > 0,
                    canMoveDown = index < editorItems.lastIndex,
                    onMoveUp = { component.moveBottomNavItem(index, index - 1) },
                    onMoveDown = { component.moveBottomNavItem(index, index + 1) },
                    onToggleVisible = { component.toggleBottomNavVisible(item.id) },
                )
            }
        }
    }
}

private fun performSave(
    component: BottomNavLayoutEditorHost,
    contextTabId: BottomNavItemId?,
    onNavigateHome: () -> Unit,
    onBack: () -> Unit,
) {
    when (val result = component.saveBottomNavLayout(contextTabId)) {
        BottomNavSaveResult.Success -> onBack()
        BottomNavSaveResult.SuccessNavigateHome -> {
            onBack()
            onNavigateHome()
        }
        is BottomNavSaveResult.Error -> Unit
    }
}

@Composable
private fun BottomNavEditorRow(
    tabId: BottomNavItemId,
    visible: Boolean,
    canHide: Boolean,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onToggleVisible: () -> Unit,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (canHide) Modifier.clickable(onClick = onToggleVisible) else Modifier
            )
            .alpha(if (visible) 1f else 0.45f),
        tonalElevation = 1.dp,
        shape = MaterialTheme.shapes.medium,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BottomNavItemIcon(tabId)
            Spacer(Modifier.width(12.dp))
            Text(
                text = tabId.label,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.weight(1f),
            )
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                IconButton(
                    onClick = onMoveUp,
                    enabled = canMoveUp,
                    modifier = Modifier.size(36.dp),
                ) {
                    Icon(
                        FeatherIcons.ChevronUp,
                        contentDescription = s("settings_vyshe"),
                        modifier = Modifier.size(20.dp),
                    )
                }
                IconButton(
                    onClick = onMoveDown,
                    enabled = canMoveDown,
                    modifier = Modifier.size(36.dp),
                ) {
                    Icon(
                        FeatherIcons.ChevronDown,
                        contentDescription = s("settings_nizhe"),
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
            if (canHide) {
                IconButton(onClick = onToggleVisible) {
                    Icon(
                        imageVector = if (visible) FeatherIcons.Eye else FeatherIcons.EyeOff,
                        contentDescription = if (visible) s("settings_skryt_iz_menyu") else s("settings_pokazat_v_menyu"),
                    )
                }
            }
        }
    }
}
