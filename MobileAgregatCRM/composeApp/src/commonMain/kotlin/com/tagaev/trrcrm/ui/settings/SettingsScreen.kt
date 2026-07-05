package com.tagaev.trrcrm.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.koin.compose.koinInject
import com.tagaev.trrcrm.data.AppSettings
import com.tagaev.trrcrm.ui.style.ThemeController
import com.tagaev.trrcrm.ui.style.ThemeMode

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.material3.Icon
import com.tagaev.trrcrm.navigation.BottomNavItemId
import com.tagaev.trrcrm.data.AppSettingsKeys
import com.tagaev.trrcrm.developer.DeveloperModeState
import com.tagaev.trrcrm.ui.custom.TextC
import com.tagaev.trrcrm.ui.root.LocalAppSnackbar
import com.tagaev.secrets.Secrets
import compose.icons.FeatherIcons
import compose.icons.feathericons.Bell
import compose.icons.feathericons.Code
import compose.icons.feathericons.LogOut
import compose.icons.feathericons.Sliders

/**
 * Minimal settings screen scaffold.
 *
 * Shows a title, a list of (future) parameters including a B/W theme toggle,
 * and a footer with version and actions (contact developer / logout).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    component: ISettingsComponent,
    contextTabId: BottomNavItemId? = null,
    onNavigateHome: () -> Unit = {},
) {
    val showBottomNavEditor by component.showBottomNavEditor.collectAsState()
    val showNotificationSettings by component.showNotificationSettings.collectAsState()
    val showDeveloperMenu by component.showDeveloperMenu.collectAsState()
    val showCameraFixator by component.showCameraFixator.collectAsState()

    when {
        showBottomNavEditor -> {
            BottomNavLayoutEditorScreen(
                component = component,
                contextTabId = contextTabId,
                onBack = component::closeBottomNavEditor,
                onNavigateHome = onNavigateHome,
            )
        }
        showNotificationSettings -> {
            NotificationSettingsScreen(
                component = component,
                onBack = component::closeNotificationSettings,
            )
        }
        showCameraFixator -> {
            CameraFixatorScreen(
                onBack = component::closeCameraFixator,
            )
        }
        showDeveloperMenu -> {
            DeveloperMenuScreen(
                onBack = component::closeDeveloperMenu,
                onOpenCameraFixator = component::openCameraFixator,
            )
        }
        else -> {
            SettingsMainScreen(component = component)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsMainScreen(
    component: ISettingsComponent,
) {
    val appSettings = koinInject<AppSettings>()
    val themeController = koinInject<ThemeController>()
    val currentTheme by themeController.mode.collectAsState()
    val showSnackbar = LocalAppSnackbar.current

    LaunchedEffect(Unit) {
        DeveloperModeState.loadFrom(appSettings)
    }

    val developerMode by DeveloperModeState.enabled
    var titleTapCount by rememberSaveable { mutableIntStateOf(0) }
    var showLogoutDialog by rememberSaveable { mutableStateOf(false) }
    val titleInteractionSource = remember { MutableInteractionSource() }

    val personalData = remember { appSettings.getString(AppSettingsKeys.PERSONAL_DATA, "") }
    val departmentData = remember { appSettings.getString(AppSettingsKeys.DEPARTMENT,"NO DEFINED") }

    Scaffold(
        topBar = { }
    ) { inner ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(inner)
                .padding(horizontal = 16.dp)
        ) {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp),
                contentPadding = PaddingValues(vertical = 12.dp)
            ) {
                item {
                    Text(
                        text = "Настройки",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(
                                interactionSource = titleInteractionSource,
                                indication = null,
                            ) {
                                titleTapCount++
                                if (titleTapCount >= 5) {
                                    titleTapCount = 0
                                    val enabled = DeveloperModeState.toggle(appSettings)
                                    showSnackbar(
                                        if (enabled) "Developer режим включён"
                                        else "Developer режим выключен"
                                    )
                                }
                            }
                            .padding(vertical = 12.dp)
                    )
                    Divider()
                }

                item {
                    Column {
                        Text("Тема приложения")
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            ThemeMode.values().forEach { mode ->
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(start = 8.dp)) {
                                    RadioButton(
                                        selected = currentTheme == mode,
                                        onClick = { themeController.setMode(mode) }
                                    )
                                    Text(mode.name)
                                }
                            }
                        }
                    }
                    Divider()
                }

                item {
                    ListItem(
                        headlineContent = { Text("Панель навигации") },
                        supportingContent = { Text("Порядок и видимость иконок в нижнем меню") },
                        leadingContent = { Icon(FeatherIcons.Sliders, contentDescription = null) },
                        modifier = Modifier.clickable { component.openBottomNavEditor() },
                    )
                    Divider()
                }

                item {
                    ListItem(
                        headlineContent = { Text("Уведомления") },
                        supportingContent = { Text("Push-уведомления и mute по типам документов") },
                        leadingContent = { Icon(FeatherIcons.Bell, contentDescription = null) },
                        modifier = Modifier.clickable { component.openNotificationSettings() },
                    )
                    Divider()
                }

                if (developerMode) {
                    item {
                        ListItem(
                            headlineContent = { Text("Developer") },
                            supportingContent = { Text("Инструменты разработчика") },
                            leadingContent = { Icon(FeatherIcons.Code, contentDescription = null) },
                            modifier = Modifier.clickable { component.openDeveloperMenu() },
                        )
                        Divider()
                    }
                }

                item {
                    ListItem(
                        headlineContent = { Text("Выйти") },
                        supportingContent = { Text("Завершить сессию") },
                        leadingContent = { Icon(FeatherIcons.LogOut, contentDescription = null) },
                        modifier = Modifier.clickable { showLogoutDialog = true }
                    )
                    Divider()
                }
            }

            if (showLogoutDialog) {
                AlertDialog(
                    onDismissRequest = { showLogoutDialog = false },
                    title = { Text("Выход из аккаунта") },
                    text = { Text("Вы уверены, что хотите выйти из аккаунта?") },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                showLogoutDialog = false
                                component.onLogout()
                            },
                        ) {
                            Text("Да")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showLogoutDialog = false }) {
                            Text("Отмена")
                        }
                    },
                )
            }

            Divider()

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (personalData.isNotBlank()) {
                    TextC(
                        text = personalData,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                if (departmentData.isNotBlank()) {
                    TextC(
                        text = "Подразделение: ${departmentData}",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                Text(
                    text = "Версия: ${Secrets.VERSION}",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}
