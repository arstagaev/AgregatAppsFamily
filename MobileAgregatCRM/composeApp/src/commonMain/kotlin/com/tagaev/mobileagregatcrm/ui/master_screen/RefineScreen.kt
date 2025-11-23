package com.tagaev.mobileagregatcrm.ui.master_screen

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.tagaev.mobileagregatcrm.domain.Refiner
import com.tagaev.mobileagregatcrm.domain.WorkOrderRefineState
import com.tagaev.mobileagregatcrm.feature.OptionChipsRow
import compose.icons.FeatherIcons
import compose.icons.feathericons.SkipBack

/**
 * Полноэкранный экран фильтрации/сортировки заказ-нарядов.
 *
 * current* — текущие значения, приходят из компонента/вью-модели.
 * onApply — возвращаем итоговые значения наружу.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RefineScreen(
    current: WorkOrderRefineState,
    onBack: () -> Unit,
    onApply: (WorkOrderRefineState) -> Unit
) {
    var selOrderBy by remember { mutableStateOf(current.orderBy) }
    var selOrderDir by remember { mutableStateOf(current.orderDir) }
    var selFilter by remember { mutableStateOf(current.filter) }
    var searchQuery by remember { mutableStateOf(current.searchQuery) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Фильтр заказ-нарядов") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(FeatherIcons.SkipBack, contentDescription = "Назад")
                    }
                },
                actions = {
                    TextButton(
                        onClick = {
                            searchQuery = ""
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
                .padding(16.dp)
                .fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            // Статус
            Text("Статус", style = MaterialTheme.typography.titleSmall)
            OptionChipsRow(
                options = Refiner.Filter.values().toList(),
                selected = selFilter,
                onSelect = { selFilter = it },
                labelFor = { it.label }
            )

            // Поле сортировки
            Text("Сортировать по", style = MaterialTheme.typography.titleSmall)
            OptionChipsRow(
                options = Refiner.OrderBy.values().toList(),
                selected = selOrderBy,
                onSelect = { selOrderBy = it },
                labelFor = { it.label }
            )

            // Направление
            Text("Направление", style = MaterialTheme.typography.titleSmall)
            OptionChipsRow(
                options = Refiner.Dir.values().toList(),
                selected = selOrderDir,
                onSelect = { selOrderDir = it },
                labelFor = { it.label }
            )

            // Поиск
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                label = { Text("Поиск по ключевым словам") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )

            Spacer(Modifier.weight(1f))

            Button(
                onClick = {
                    onApply(
                        current.copy(
                            orderBy = selOrderBy,
                            orderDir = selOrderDir,
                            filter = selFilter,
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