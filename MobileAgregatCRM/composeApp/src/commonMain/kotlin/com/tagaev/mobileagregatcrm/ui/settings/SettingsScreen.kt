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

private const val KEY_BW_THEME = "isBW_THEME"

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

    // BW theme toggle (persisted)
    var bwTheme by remember { mutableStateOf(appSettings.getBool(KEY_BW_THEME, false)) }

    // Simple future settings list; add real items later
    val futureItems = remember {
        listOf(
            "Notifications (soon)",
            "Sync over Wi‑Fi only (soon)",
            "Auto-refresh on open (soon)"
        )
    }

    Scaffold(
        topBar = {
            MediumTopAppBar(
                title = {
                    Text(
                        text = "Настройки",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            )
        }
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
                    Column {
                        ListItem(
                            headlineContent = { Text("Тема приложения") },
                            supportingContent = { Text("") },
                            trailingContent = {

                            }
                        )
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

                // future placeholders
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
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Version: if you want a real value, write it into AppSettings under APP_VERSION at startup
                Text(
                    text = "Версия: ${CONST.VERSION}",
                    style = MaterialTheme.typography.bodyMedium
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    OutlinedButton(onClick = { component.onWriteToDeveloper() }) {
                        Text("Написать разработчику")
                    }
                    Button(onClick = { component.onLogout() }) {
                        Text("Выйти")
                    }
                }
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
