package com.tagaev.trrcrm.ui.master_screen

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Alignment
import com.tagaev.trrcrm.domain.Refiner
import com.tagaev.trrcrm.domain.RefineState
import com.tagaev.trrcrm.domain.OptionChipsRow
import compose.icons.LineAwesomeIcons
import compose.icons.lineawesomeicons.UndoSolid
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.text.style.TextOverflow
import com.tagaev.trrcrm.data.AppSettings
import com.tagaev.trrcrm.data.AppSettingsKeys
import com.tagaev.trrcrm.data.remote.models.UserRole
import com.tagaev.trrcrm.ui.custom.ScreenWithDismissableKeyboard
import com.tagaev.trrcrm.ui.style.DefaultColors.RainbowRedFg
import com.tagaev.trrcrm.ui.style.ThemeController
import com.tagaev.trrcrm.utils.AVAILABLE_ROLES
import compose.icons.FeatherIcons
import compose.icons.feathericons.AlertTriangle
import org.koin.compose.koinInject
import kotlin.collections.contains

enum class RefineSection {
    STATUS,
    ORDER,
    DIRECTION,
    SEARCH,
    FILTER_VAL
}

/**
 * Полноэкранный экран фильтрации/сортировки заказ-нарядов.
 *
 * current* — текущие значения, приходят из компонента/вью-модели.
 * onApply — возвращаем итоговые значения наружу.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RefineScreen(
    current: RefineState,
    onBack: () -> Unit,
    onApply: (RefineState) -> Unit,
    messageForUser: String? = null,
    sections: Set<RefineSection> = RefineSection.values().toSet()
) {
    val appSettings = koinInject<AppSettings>()

    val personalData = remember { appSettings.getString(AppSettingsKeys.PERSONAL_DATA, "") }
    val departmentData = remember { appSettings.getString(AppSettingsKeys.DEPARTMENT,"NO DEFINED") }

    var selOrderBy by remember { mutableStateOf(current.orderBy) }
    var selOrderDir by remember { mutableStateOf(current.orderDir) }
    var setStatus by remember { mutableStateOf(current.status) }
    var setFilter by remember { mutableStateOf(current.filter) }
    var setFilterValue by remember { mutableStateOf(current.filterValue) }
    var searchQuery by remember { mutableStateOf(current.searchQuery) }
    var searchQueryType by remember { mutableStateOf(current.searchQueryType) }

    val scrollState = rememberScrollState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Фильтр") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(LineAwesomeIcons.UndoSolid, contentDescription = "Назад")
                    }
                },
                actions = {
                    TextButton(
                        onClick = {
                            searchQuery = ""
                            searchQueryType = Refiner.SearchQueryType.TOPIC
                            selOrderBy = Refiner.OrderBy.DATE_LAST_MODIFICATION
                            selOrderDir = Refiner.Dir.DESC
                            setStatus = Refiner.Status.OFF
                            setFilter = Refiner.Filter.OFF
                        }
                    ) {
                        Text("Сброс")
                    }
                }
            )
        }
    ) { innerPadding ->
        ScreenWithDismissableKeyboard {
            Column(
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (messageForUser != null) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp),
                        shape = RoundedCornerShape(8.dp),
                        color = RainbowRedFg,
                        tonalElevation = 2.dp
                    ) {
                        Row(
                            modifier = Modifier
                                .padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(FeatherIcons.AlertTriangle, contentDescription = "Alert")
                            Spacer(Modifier.width(5.dp))
                            Text(
                                text = messageForUser,
                                style = MaterialTheme.typography.bodySmall,
                                maxLines = 3,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                if (RefineSection.STATUS in sections) {
                    // Статус
                    Text("Статус", style = MaterialTheme.typography.titleSmall)
                    OptionChipsRow(
                        options = Refiner.Status.values().toList(),
                        selected = setStatus,
                        onSelect = { setStatus = it },
                        labelFor = { it.label }
                    )
                }

                if (RefineSection.FILTER_VAL in sections && (AVAILABLE_ROLES.contains(UserRole.FULL_ACCESS.shortTitle))) {
                    // Фильтр
                    Text("Фильтр по подразделению", style = MaterialTheme.typography.titleSmall)
                    OptionChipsRow(
                        options = Refiner.Filter.values().toList(),
                        selected = setFilter,
                        onSelect = {
                            setFilter = it
                            if (setFilter == Refiner.Filter.DEPARTMENT) {

                                setFilterValue = departmentData
                            }
                        },
                        labelFor = { it.label }
                    )
                }

                if (RefineSection.ORDER in sections) {
                    // Поле сортировки
                    Text("Сортировать по", style = MaterialTheme.typography.titleSmall)
                    OptionChipsRow(
                        options = Refiner.OrderBy.values().toList(),
                        selected = selOrderBy,
                        onSelect = { selOrderBy = it },
                        labelFor = { it.label }
                    )
                }

                if (RefineSection.DIRECTION in sections) {
                    // Направление
                    Text("Направление", style = MaterialTheme.typography.titleSmall)
                    OptionChipsRow(
                        options = Refiner.Dir.values().toList(),
                        selected = selOrderDir,
                        onSelect = { selOrderDir = it },
                        labelFor = { it.label }
                    )
                }

                if (RefineSection.SEARCH in sections) {
                    // Поиск
                    Text("Поиск", style = MaterialTheme.typography.titleSmall)
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        label = { Text("Поиск по ключевым словам") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                    )
                    OptionChipsRow(
                        options = Refiner.SearchQueryType.values().toList(),
                        selected = searchQueryType,
                        onSelect = { searchQueryType = it },
                        labelFor = { it.label }
                    )
                }



                ///////////////
                // .  SETTING REFINERS!!
                //////////////
                Spacer(Modifier.height(16.dp))

                Button(
                    onClick = {
                        onApply(
                            current.copy(
                                orderBy = selOrderBy,
                                orderDir = selOrderDir,
                                status = setStatus,
                                filter = setFilter,
                                filterValue = setFilterValue,
                                searchQueryType = searchQueryType,
                                searchQuery = searchQuery
                            )
                        )
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Применить")
                }
            }
        }

    }
}