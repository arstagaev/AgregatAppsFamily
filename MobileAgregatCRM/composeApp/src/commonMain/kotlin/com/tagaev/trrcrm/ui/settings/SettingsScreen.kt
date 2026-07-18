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
import com.tagaev.trrcrm.ui.i18n.AppLanguage
import com.tagaev.trrcrm.ui.i18n.LanguageController
import com.tagaev.trrcrm.ui.i18n.s
import com.tagaev.trrcrm.ui.root.LocalAppSnackbar
import com.tagaev.secrets.Secrets
import compose.icons.FeatherIcons
import compose.icons.feathericons.Bell
import compose.icons.feathericons.Code
import compose.icons.feathericons.Image
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
    val languageController = koinInject<LanguageController>()
    val currentTheme by themeController.mode.collectAsState()
    val currentLanguage by languageController.language.collectAsState()
    val showSnackbar = LocalAppSnackbar.current

    LaunchedEffect(Unit) {
        DeveloperModeState.loadFrom(appSettings)
    }

    val developerMode by DeveloperModeState.enabled
    var titleTapCount by rememberSaveable { mutableIntStateOf(0) }
    var showLogoutDialog by rememberSaveable { mutableStateOf(false) }
    var showClearPhotoCacheDialog by rememberSaveable { mutableStateOf(false) }
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
                        text = s("settings_title"),
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
                                        if (enabled) s("settings_developer_rezhim_vklyuchen")
                                        else s("settings_developer_rezhim_vyklyuchen")
                                    )
                                }
                            }
                            .padding(vertical = 12.dp)
                    )
                    Divider()
                }

                item {
                    Column {
                        Text(s("settings_tema_prilozheniya"))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            ThemeMode.values().forEach { mode ->
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(start = 8.dp)) {
                                    RadioButton(
                                        selected = currentTheme == mode,
                                        onClick = { themeController.setMode(mode) }
                                    )
                                    Text(
                                        when (mode) {
                                            ThemeMode.System -> s("theme_system")
                                            ThemeMode.Light -> s("theme_light")
                                            ThemeMode.Dark -> s("theme_dark")
                                        }
                                    )
                                }
                            }
                        }
                    }
                    Divider()
                }

                item {
                    Column {
                        Text(s("settings_language"))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            AppLanguage.entries.forEach { language ->
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(start = 8.dp),
                                ) {
                                    RadioButton(
                                        selected = currentLanguage == language,
                                        onClick = { languageController.setLanguage(language) },
                                    )
                                    Text(
                                        when (language) {
                                            AppLanguage.Russian -> s("settings_language_russian")
                                            AppLanguage.Uzbek -> s("settings_language_uzbek")
                                        }
                                    )
                                }
                            }
                        }
                    }
                    Divider()
                }

                item {
                    ListItem(
                        headlineContent = { Text(s("settings_panel_navigatsii")) },
                        supportingContent = { Text(s("settings_poryadok_i_vidimost_ikonok_v_nizhnem_menyu")) },
                        leadingContent = { Icon(FeatherIcons.Sliders, contentDescription = null) },
                        modifier = Modifier.clickable { component.openBottomNavEditor() },
                    )
                    Divider()
                }

                item {
                    ListItem(
                        headlineContent = { Text(s("settings_uvedomleniya")) },
                        supportingContent = { Text(s("settings_push_uvedomleniya_i_mute_po_tipam_dokumentov")) },
                        leadingContent = { Icon(FeatherIcons.Bell, contentDescription = null) },
                        modifier = Modifier.clickable { component.openNotificationSettings() },
                    )
                    Divider()
                }

                item {
                    ListItem(
                        headlineContent = { Text(s("settings_ochistit_kesh_fotografiy")) },
                        supportingContent = { Text(s("settings_zagruzhennye_foto_dokumentov_na_ustroystve")) },
                        leadingContent = { Icon(FeatherIcons.Image, contentDescription = null) },
                        modifier = Modifier.clickable { showClearPhotoCacheDialog = true },
                    )
                    Divider()
                }

                if (developerMode) {
                    item {
                        ListItem(
                            headlineContent = { Text("Developer") },
                            supportingContent = { Text(s("settings_instrumenty_razrabotchika")) },
                            leadingContent = { Icon(FeatherIcons.Code, contentDescription = null) },
                            modifier = Modifier.clickable { component.openDeveloperMenu() },
                        )
                        Divider()
                    }
                }

                item {
                    ListItem(
                        headlineContent = { Text(s("settings_vyyti")) },
                        supportingContent = { Text(s("settings_zavershit_sessiyu")) },
                        leadingContent = { Icon(FeatherIcons.LogOut, contentDescription = null) },
                        modifier = Modifier.clickable { showLogoutDialog = true }
                    )
                    Divider()
                }
            }

            if (showLogoutDialog) {
                AlertDialog(
                    onDismissRequest = { showLogoutDialog = false },
                    title = { Text(s("settings_vyhod_iz_akkaunta")) },
                    text = { Text(s("settings_vy_uvereny_chto_hotite_vyyti_iz_akkaunta")) },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                showLogoutDialog = false
                                component.onLogout()
                            },
                        ) {
                            Text(s("settings_da"))
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showLogoutDialog = false }) {
                            Text(s("settings_otmena"))
                        }
                    },
                )
            }

            if (showClearPhotoCacheDialog) {
                AlertDialog(
                    onDismissRequest = { showClearPhotoCacheDialog = false },
                    title = { Text(s("settings_ochistit_kesh_fotografiy")) },
                    text = { Text(s("settings_udalit_zagruzhennye_foto_dokumentov_s_ustroystva")) },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                showClearPhotoCacheDialog = false
                                component.clearDocumentPhotoCache(showSnackbar)
                            },
                        ) {
                            Text(s("settings_ochistit"))
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showClearPhotoCacheDialog = false }) {
                            Text(s("settings_otmena"))
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
                        text = s("settings_podrazdelenie_departmentdata", departmentData),
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                Text(
                    text = s("common_version", Secrets.VERSION),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}
