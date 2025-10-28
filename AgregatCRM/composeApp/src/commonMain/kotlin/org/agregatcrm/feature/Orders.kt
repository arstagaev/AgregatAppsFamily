package org.agregatcrm.feature

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

// Display label (RU) + wire value (what you send to API)
enum class OrderByOption(val label: String, val wire: String) {
    DATE("Дата", "Дата"),
    KIND("ВидСобытия", "ВидСобытия"),
    STATE("Состояние", "Состояние"),
    NUMBER("Номер", "Номер");
}

enum class OrderDirOption(val label: String, val wire: String) {
    ASC("По возрастанию", "asc"),
    DESC("По убыванию", "desc");
}

@Composable
fun OrderDialog(
    currentBy: OrderByOption,
    currentDir: OrderDirOption,
    onDismiss: () -> Unit,
    onApply: (by: OrderByOption, dir: OrderDirOption) -> Unit
) {
    var selBy by remember { mutableStateOf(currentBy) }
    var selDir by remember { mutableStateOf(currentDir) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Сортировка", fontWeight = FontWeight.SemiBold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {

                Text("Поле для сортировки", style = MaterialTheme.typography.titleSmall)
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OrderByOption.values().forEach { opt ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selBy = opt }
                                .padding(end = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            RadioButton(selected = selBy == opt, onClick = { selBy = opt })
                            Text(opt.label)
                        }
                    }
                }

                Spacer(Modifier.height(8.dp))

                Text("Направление", style = MaterialTheme.typography.titleSmall)
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OrderDirOption.values().forEach { opt ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selDir = opt }
                                .padding(end = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            RadioButton(selected = selDir == opt, onClick = { selDir = opt })
                            Text(opt.label)
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onApply(selBy, selDir) }) { Text("Применить") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Отмена") }
        }
    )
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