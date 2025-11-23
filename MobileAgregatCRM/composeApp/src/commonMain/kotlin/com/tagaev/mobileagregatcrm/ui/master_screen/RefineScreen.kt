package com.tagaev.mobileagregatcrm.ui.master_screen

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.tagaev.mobileagregatcrm.domain.Refiner
import com.tagaev.mobileagregatcrm.domain.RefineState
import com.tagaev.mobileagregatcrm.feature.OptionChipsRow
import compose.icons.FeatherIcons
import compose.icons.LineAwesomeIcons
import compose.icons.feathericons.SkipBack
import compose.icons.lineawesomeicons.UndoSolid
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll

enum class RefineSection {
    STATUS,
    ORDER,
    DIRECTION,
    SEARCH
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
    sections: Set<RefineSection> = RefineSection.values().toSet()
) {
    var selOrderBy by remember { mutableStateOf(current.orderBy) }
    var selOrderDir by remember { mutableStateOf(current.orderDir) }
    var selFilter by remember { mutableStateOf(current.filter) }
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
                            selFilter = Refiner.Filter.OFF
                        }
                    ) {
                        Text("Сброс")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {

            if (RefineSection.STATUS in sections) {
                // Статус
                Text("Статус", style = MaterialTheme.typography.titleSmall)
                OptionChipsRow(
                    options = Refiner.Filter.values().toList(),
                    selected = selFilter,
                    onSelect = { selFilter = it },
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

            Spacer(Modifier.height(16.dp))

            Button(
                onClick = {
                    onApply(
                        current.copy(
                            orderBy = selOrderBy,
                            orderDir = selOrderDir,
                            filter = selFilter,
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