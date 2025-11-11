package com.tagaev.mobileagregatcrm.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.koin.compose.koinInject
import com.tagaev.mobileagregatcrm.data.AppSettings
import com.tagaev.mobileagregatcrm.ui.style.ThemeController
import com.tagaev.mobileagregatcrm.ui.style.ThemeMode
import com.tagaev.mobileagregatcrm.utils.CONST

import androidx.compose.foundation.clickable
import androidx.compose.material3.Icon
import com.tagaev.mobileagregatcrm.data.AppSettingsKeys
import compose.icons.FeatherIcons
import compose.icons.feathericons.Mail
import compose.icons.feathericons.LogOut

/**
 * Minimal settings screen scaffold.
 *
 * Shows a title, a list of (future) parameters including a B/W theme toggle,
 * and a footer with version and actions (contact developer / logout).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(component: ISettingsComponent) {
    val appSettings = koinInject<AppSettings>()
    val themeController = koinInject<ThemeController>()
    val currentTheme by themeController.mode.collectAsState()

    val personalData = remember { appSettings.getString(AppSettingsKeys.PERSONAL_DATA, "") }

    // Simple future settings list; add real items later
    val futureItems = remember {
        listOf(
            "Notifications (soon)",
            "Sync over Wi‑Fi only (soon)",
            "Auto-refresh on open (soon)"
        )
    }

    Scaffold(
        topBar = { }
    ) { inner ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(inner)
                .padding(horizontal = 16.dp)
        ) {
            // Settings list
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
                            .padding(vertical = 12.dp)
                    )
                    Divider()
                }

                item {
                    Column {
//                        ListItem(
//                            headlineContent = {  },
//                            supportingContent = { Text("") },
//                            trailingContent = {
//
//                            }
//                        )
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

                }

//                item {
//                    ListItem(
//                        headlineContent = { Text("Написать разработчику") },
//                        supportingContent = { Text("Email письмо") },
//                        leadingContent = { Icon(FeatherIcons.Mail, contentDescription = null) },
//                        modifier = Modifier.clickable { component.onWriteToDeveloper() }
//                    )
//                    Divider()
//                }

                item {
                    ListItem(
                        headlineContent = { Text("Выйти") },
                        supportingContent = { Text("Завершить сессию") },
                        leadingContent = { Icon(FeatherIcons.LogOut, contentDescription = null) },
                        modifier = Modifier.clickable { component.onLogout() }
                    )
                    Divider()
                }

//                // future placeholders
//                items(futureItems) { label ->
//                    ListItem(
//                        headlineContent = { Text(label) },
//                        supportingContent = { Text("Configure later in upcoming versions") }
//                    )
//                    Divider()
//                }
            }

            Divider()

            // Footer: version + actions
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (personalData.isNotBlank()) {
                    Text(
                        text = personalData,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                Text(
                    text = "Версия: ${CONST.VERSION}",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

///**
// * Component contract used by SettingsScreen.
// * Provide platform-specific behavior for contacting developer and logout.
// */
//interface ISettingsComponent {
//    fun onWriteToDeveloper()
//    fun onLogout()
//}
