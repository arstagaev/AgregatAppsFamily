package com.tagaev.mobileagregatcrm.feature

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.style.TextOverflow

enum class DocumentTypes(val requestName: String) {
    EVENT("Событие"),
    WORK_ORDER("ЗаказНаряд")
}

// Display label (RU) + wire value (what you send to API)
enum class OrderByOption(val label: String, val wire: String) {
    DATE("Дата", "Дата"),
    DATE_LAST_MODIFICATION("ДатаМод", "ДатаМод"),
    KIND("ВидСобытия", "ВидСобытия"),
    STATE("Состояние", "Состояние"),
    NUMBER("Номер", "Номер"),
}

/**
 * Параметры фильтрации:
 * type - тип сущности (документ/справочник)
 * name - имя сущности
 *
 * count - количество запрашиваемых элементов (max 100 пока)
 * ncount = стартовая позиция для запроса количества
 *
 * orderby - поле сортировки
 * orderdir - направление сортировки (asc, desc)
 *
 * filterby - реквизит для фильтрации (Состояние, Номер, и тд)
 * filtertype - тип фильтрации (value - поиск по подстроке, code - поиск по коду справочника, list - по перечислению, состояния и т.д.)
 *
 * filterval - значение фильтра
 * guid - поле для точного поиска по номеру документов с периодической нумерацией
 */
enum class FilterByOption(val label: String, val wire: String) {
    ACTIVE("Активные","активно"),
    NO_ACTIVE("Не Активные","неактивно"),
    ALL("Все","Все");
//    STATE("Состояние","Состояние"),
//    NUMBER("Номер","Номер"),
//    KIND("ВидСобытия", "ВидСобытия"),
//    DEPARTMENT("ПодразделениеКомпании", "ПодразделениеКомпании")
}

enum class FilterByOptionWorkOrders(val label: String, val wire: String) {
    STATE("Состояние","Состояние"),
//    KIND("ВидСобытия", "ВидСобытия"),
    DEPARTMENT("ПодразделениеКомпании", "ПодразделениеКомпании")
}

enum class OrderDirOption(val label: String, val wire: String) {
    ASC("По возрастанию", "asc"),
    DESC("По убыванию", "desc");
}

@Composable
fun OrderDialog(
    orderByOption: OrderByOption,
    currentDir: OrderDirOption,
    onDismiss: () -> Unit,
    onApply: (by: OrderByOption, dir: OrderDirOption, filter: FilterByOption) -> Unit,
    currentFilterVal: FilterByOption
) {
    var selByOrder by remember { mutableStateOf(orderByOption) }
    var selByOrderDir by remember { mutableStateOf(currentDir) }
    var selectByFilterVal by remember { mutableStateOf(currentFilterVal) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Настройка показа", fontWeight = FontWeight.SemiBold) },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    "Поле для фильтрации",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                OptionChipsRow(
                    options = FilterByOption.values().toList(),
                    selected = selectByFilterVal,
                    onSelect = { selectByFilterVal = it },
                    labelFor = { it.label }
                )

                Text(
                    "Поле для сортировки",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OptionChipsRow(
                    options = OrderByOption.values().toList(),
                    selected = selByOrder,
                    onSelect = { selByOrder = it },
                    labelFor = { it.label }
                )

                Text(
                    "Направление",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OptionChipsRow(
                    options = OrderDirOption.values().toList(),
                    selected = selByOrderDir,
                    onSelect = { selByOrderDir = it },
                    labelFor = { it.label }
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onApply(selByOrder, selByOrderDir, selectByFilterVal) }) { Text("Применить") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Отмена") }
        }
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun <T> OptionChipsRow(
    options: List<T>,
    selected: T,
    onSelect: (T) -> Unit,
    labelFor: (T) -> String
) {
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        options.forEach { opt ->
            FilterChip(
                selected = opt == selected,
                onClick = { onSelect(opt) },
                label = {
                    Text(
                        labelFor(opt),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            )
        }
    }
}


fun String?.toOrderByOption(default: OrderByOption = OrderByOption.DATE): OrderByOption {
    println("CONVERTINGGG 1 toOrderByOption: ${this}")
    val value = this?.trim()
    return OrderByOption.values().firstOrNull {
        it.wire.equals(value, ignoreCase = true) || it.label.equals(value, ignoreCase = true)
    } ?: default
}

fun String?.toOrderDirOption(default: OrderDirOption = OrderDirOption.DESC): OrderDirOption {
    println("CONVERTINGGG 2 toOrderDirOption: ${this}")
    val value = this?.trim()
    return OrderDirOption.values().firstOrNull {
        it.wire.equals(value, ignoreCase = true) || it.label.equals(value, ignoreCase = true)
    } ?: default
}

//FilterByOption
fun String?.toFilterByOption(default: FilterByOption = FilterByOption.ACTIVE): FilterByOption {
    println("CONVERTINGGG 3 FilterByOption: ${this}")
    val value = this?.trim()
    return FilterByOption.values().firstOrNull {
        it.wire.equals(value, ignoreCase = true) || it.label.equals(value, ignoreCase = true)
    } ?: default
}
