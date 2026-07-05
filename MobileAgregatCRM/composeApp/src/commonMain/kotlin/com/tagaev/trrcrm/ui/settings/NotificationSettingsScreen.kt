package com.tagaev.trrcrm.ui.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import compose.icons.FeatherIcons
import compose.icons.feathericons.ArrowLeft
import compose.icons.feathericons.RefreshCw

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationSettingsScreen(
    component: ISettingsComponent,
    onBack: () -> Unit,
) {
    val muteAll by component.muteAll.collectAsState()
    val mutedDocTypes by component.mutedDocTypes.collectAsState()
    val muteLoading by component.muteLoading.collectAsState()
    val muteError by component.muteErrorMessage.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Уведомления") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(FeatherIcons.ArrowLeft, contentDescription = "Назад")
                    }
                },
            )
        },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(vertical = 12.dp),
        ) {
            item {
                ListItem(
                    headlineContent = { Text("Отключить все уведомления на этом устройстве") },
                    supportingContent = { Text("Блокирует все push-уведомления для текущего устройства") },
                    trailingContent = {
                        Switch(
                            checked = muteAll,
                            onCheckedChange = { component.setMuteAll(it) },
                            enabled = !muteLoading,
                        )
                    },
                )
                Divider()
            }

            item {
                ListItem(
                    headlineContent = { Text("Откл. Событие") },
                    trailingContent = {
                        Switch(
                            checked = DeviceMuteDocType.EVENT in mutedDocTypes,
                            onCheckedChange = { component.setDocumentTypeMuted(DeviceMuteDocType.EVENT, it) },
                            enabled = !muteLoading && !muteAll,
                        )
                    },
                )
                Divider()
            }

            item {
                ListItem(
                    headlineContent = { Text("Откл. Заказ-Наряд") },
                    trailingContent = {
                        Switch(
                            checked = DeviceMuteDocType.WORK_ORDER in mutedDocTypes,
                            onCheckedChange = { component.setDocumentTypeMuted(DeviceMuteDocType.WORK_ORDER, it) },
                            enabled = !muteLoading && !muteAll,
                        )
                    },
                )
                Divider()
            }

            item {
                ListItem(
                    headlineContent = { Text("Откл. Комплектация") },
                    trailingContent = {
                        Switch(
                            checked = DeviceMuteDocType.COMPLECTATION in mutedDocTypes,
                            onCheckedChange = { component.setDocumentTypeMuted(DeviceMuteDocType.COMPLECTATION, it) },
                            enabled = !muteLoading && !muteAll,
                        )
                    },
                )
                Divider()
            }

            item {
                Column(modifier = Modifier.fillMaxWidth()) {
                    TextButton(
                        onClick = component::refreshMuteState,
                        enabled = !muteLoading,
                    ) {
                        Icon(FeatherIcons.RefreshCw, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text(if (muteLoading) "Обновляем..." else "Обновить настройки уведомлений")
                    }
                    if (!muteError.isNullOrBlank()) {
                        Text(
                            text = muteError.orEmpty(),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                        )
                    }
                }
                Divider()
            }
        }
    }
}
