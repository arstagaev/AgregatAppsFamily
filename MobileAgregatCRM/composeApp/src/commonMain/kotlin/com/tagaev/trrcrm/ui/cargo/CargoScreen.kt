package com.tagaev.trrcrm.ui.cargo

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.tagaev.trrcrm.models.CargoDto
import com.tagaev.trrcrm.ui.custom.StatusBadge
import com.tagaev.trrcrm.ui.custom.StatusStyle
import com.tagaev.trrcrm.ui.custom.TextC
import com.tagaev.trrcrm.ui.custom.withEscapedNewlines
import com.tagaev.trrcrm.ui.master_screen.MasterScreen
import com.tagaev.trrcrm.ui.master_screen.RefineScreen
import com.tagaev.trrcrm.ui.style.DefaultColors

@Composable
fun CargoScreen(component: ICargoComponent) {
    val resource by component.cargos.collectAsState()
    val refineState by component.refineState.collectAsState()
    val panel by component.masterScreenPanel.collectAsState()
    val selectedId by component.selectedItemGuid.collectAsState()

    val scope = rememberCoroutineScope()
//    var isSendingMessage by remember { mutableStateOf(false) }
//    var lastSendError by remember { mutableStateOf<String?>(null) }

    MasterScreen(
        title = "Доставки",
        resource = resource,
        errorText = "Не удалось загрузить доставки",
        notFoundText = "Доставки не найдены",
        refineState = refineState,
        onRefresh = { component.fullRefresh() },
        onLoadMore = { component.loadMore() },
        onFilterChanged = { component.setRefineState(it) },

        itemId = { it.guid.toString() },
        isItemChanged = { old, new -> old.orders.size != new.orders.size },

        listItem = { cargo, isChanged, onClick ->
            CargoListItem(
                cargo = cargo,
                onClick = onClick
            )
        },

        // Full-screen details content (not bottom-sheet)
        detailsContent = { cargo, onClose ->
            CargoDetailsSheet(cargo, onClose)
        },

        // Full-screen filter screen (not dialog)
        filterScreen = { current, onDismiss, onApply ->
            RefineScreen(
                current = current,
                onBack = onDismiss,
                messageForUser = "Корректно работает только сортировка по Дате, остальные фильтры пока в разработке",
                onApply = { newState ->
                    println(">>>>>> ${newState.toString()}")
                    println(">>>>>> ${newState.searchQueryType.wire}")
                    component.setRefineState(newState)
                    onApply(newState)     // MasterDetailFilterScreen получит обновлённый стейт

                }
            )
        },

        panel = panel,
        onPanelChange = {
            component.changePanel(it)

        },

        selectedItemId = selectedId,
        onSelectedItemChange = { id -> component.selectItemFromList(id) },
    )
}


@Composable
private fun CargoListItem(
    cargo: CargoDto,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp)
            ,
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp)
                .clickable(onClick = onClick),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Верхняя строка: номер + (опционально) маркер изменений + статус
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    TextC(
                        text = cargo.number,
                        style = MaterialTheme.typography.titleMedium
                    )
//                    if (isChanged) {
//                        Text(
//                            text = "новое",
//                            style = MaterialTheme.typography.labelSmall,
//                            color = NeumoColors.RainbowGreenBg
//                        )
//                    }
                }

                StatusBadge(
                    state = cargo.status,
                    styles = mapOf(
                        Pair(CargoStatus.PROPOSAL.value, StatusStyle(DefaultColors.RainbowYellowFg, Color.Black)),
                        Pair(CargoStatus.RECEIVED.value, StatusStyle(DefaultColors.RainbowBlueBg, Color.Black)),
                        Pair(CargoStatus.IN_WORK.value, StatusStyle(DefaultColors.RainbowGreenBg, Color.Black)),
                        Pair(CargoStatus.PROPOSAL_FOR_GET_CARGO.value, StatusStyle(DefaultColors.RainbowRedFg, Color.Black)),
                        Pair(CargoStatus.SENT_TO_MAIN_DEPT.value, StatusStyle(DefaultColors.RainbowVioletBg, Color.Black)),
                        Pair(CargoStatus.WAIT_FOR_LOAD_CAR_FOUND.value, StatusStyle(DefaultColors.RainbowRedBg, Color.Black)),
                    )
                )
            }

            // Основные сведения
            Column(
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                if (cargo.route.isNotBlank()) {
                    TextC(
                        text = "${cargo.route}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                Text(
                    text = "Организация: ${cargo.organization}\nПодразделение: ${cargo.department}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                val counters = buildList {
                    if (cargo.orders.isNotEmpty()) add("Заказов: ${cargo.orders.size}")
                    if (cargo.products.isNotEmpty()) add("Товаров: ${cargo.products.size}")
                }.joinToString(" · ")

                if (counters.isNotEmpty()) {
                    Text(
                        text = counters,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (!cargo.comment.isNullOrBlank()) {
                    HorizontalDivider(Modifier.fillMaxWidth(), thickness = 1.dp, color = DefaultColors.NeumoHighlight)
                    Text(
                        text = "${cargo.comment.withEscapedNewlines()}",
                        maxLines = 3,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Дата в правом нижнем углу
            Text(
                modifier = Modifier.align(Alignment.End),
                text = cargo.date,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
