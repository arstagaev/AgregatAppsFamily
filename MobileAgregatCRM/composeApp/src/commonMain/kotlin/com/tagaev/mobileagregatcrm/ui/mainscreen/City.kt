package com.tagaev.mobileagregatcrm.ui.mainscreen

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tagaev.mobileagregatcrm.utils.CenteredNoPaddingOutlinedField

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CityDropdown(
    label: String,
    selectedValue: String,
    onSelected: (String) -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier
) {
    val compactTextStyle = LocalTextStyle.current.copy(fontSize = 13.sp)
    val compactItemTextStyle = MaterialTheme.typography.bodySmall
    var expanded by remember { mutableStateOf(false) }
    val selectedLabel = remember(selectedValue) {
        CITY_OPTIONS.find { it.value == selectedValue }?.label ?: ""
    }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { if (enabled) expanded = !expanded },
        modifier = modifier
    ) {
        CenteredNoPaddingOutlinedField(
            value =  if (selectedLabel.isNotBlank()) selectedLabel else "",
            onValueChange = {},
            readOnly = true,
            enabled = enabled,
            labelText = label,            // shows floating label
            placeholderText = label,     // optional
            modifier = Modifier.menuAnchor().fillMaxWidth(),             // add your .weight/.height/.fillMaxWidth as needed
        )

//        OutlinedTextField(
//            readOnly = true,
//            value = if (selectedLabel.isNotBlank()) selectedLabel else "",
//            onValueChange = {},
//            label = { Text(label, style = MaterialTheme.typography.labelSmall) },
//            placeholder = { Text(label, style = MaterialTheme.typography.labelSmall) },
//            enabled = enabled,
//            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
//            modifier = Modifier
//                .menuAnchor()
//                .fillMaxWidth(),
//            singleLine = true,
//            textStyle = compactTextStyle
//        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            CITY_OPTIONS.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option.label, style = compactItemTextStyle, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                    onClick = {
                        expanded = false
                        onSelected(option.value)
                    },
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }
    }
}

private data class CityOption(val label: String, val value: String)

private val CITY_OPTIONS = listOf(
    CityOption("Анталия", "Анталия"),
    CityOption("Барнаул", "Барнаул"),
    CityOption("Волгоград", "Волгоград"),
    CityOption("Воронеж", "Воронеж"),
    CityOption("Екатеринбург", "Екатеринбург"),
    CityOption("Ижевск", "Ижевск"),
    CityOption("Иркутск", "Иркутск"),
    CityOption("Йошкар-Ола", "Йошкар-Ола"),
    CityOption("Казань", "Казань"),
    CityOption("Киров", "Киров"),
    CityOption("Краснодар", "Краснодар"),
    CityOption("Магнитогорск", "Магнитогорск"),
    CityOption("Москва", "Москва"),
    CityOption("Мурманск", "Мурманск"),
    CityOption("Набережные Челны", "Набережные_Челны"),
    CityOption("Нижневартовск", "Нижневартовск"),
    CityOption("Нижний Новгород", "Нижний_Новгород"),
    CityOption("Новосибирск", "Новосибирск"),
    CityOption("Омск", "Омск"),
    CityOption("Оренбург", "Оренбург"),
    CityOption("Пермь", "Пермь"),
    CityOption("Петрозаводск", "Петрозаводск"),
    CityOption("Ростов-на-Дону", "Ростов-на-Дону"),
    CityOption("Самара", "Самара"),
    CityOption("Санкт-Петербург", "Санкт-Петербург"),
    CityOption("Саратов", "Саратов"),
    CityOption("Сочи", "Сочи"),
    CityOption("Сургут", "Сургут"),
    CityOption("Сыктывкар", "Сыктывкар"),
    CityOption("Тольятти", "Тольятти"),
    CityOption("Тюмень", "Тюмень"),
    CityOption("Ульяновск", "Ульяновск"),
    CityOption("Уфа", "Уфа"),
    CityOption("Чебоксары", "Чебоксары"),
    CityOption("Челябинск", "Челябинск"),
    CityOption("Ярославль", "Ярославль")
)
