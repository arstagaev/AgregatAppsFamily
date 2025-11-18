package com.tagaev.mobileagregatcrm.ui.work_order

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.tagaev.mobileagregatcrm.data.remote.Resource
import com.tagaev.mobileagregatcrm.models.WorkOrderDto
import compose.icons.FeatherIcons
import compose.icons.feathericons.RefreshCw

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkOrdersScreen(
    component: IWorkOrdersComponent,
    modifier: Modifier = Modifier
) {
    val resource by component.workOrders.collectAsState()

    var selectedOrder by remember { mutableStateOf<WorkOrderDto?>(null) }

    // Sheet state with 3 positions: hidden, partial, expanded
    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = false
    )

    // Control sheet visibility & initial position (half-open)
    LaunchedEffect(selectedOrder) {
        if (selectedOrder != null) {
            sheetState.partialExpand()
        } else {
            sheetState.hide()
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text("Заказ-наряды") },
                actions = {
                    IconButton(onClick = { component.fullRefresh() }) {
                        Icon(FeatherIcons.RefreshCw, contentDescription = "Обновить")
                    }
                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            when (val state = resource) {
                is Resource.Loading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }

                is Resource.Error -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "Не удалось загрузить заказ-наряды",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Spacer(Modifier.height(8.dp))
                        state.causes?.let {
                            Text(
                                text = it,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                        Spacer(Modifier.height(16.dp))
                        Button(onClick = { component.fullRefresh() }) {
                            Text("Повторить")
                        }
                    }
                }

                is Resource.Success -> {
                    val items = state.data.orEmpty()
                    if (items.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("Заказ-наряды не найдены")
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(
                                items = items,
                                key = { it.guid ?: it.number.orEmpty() }
                            ) { order ->
                                WorkOrderCard(
                                    order = order,
                                    onClick = { selectedOrder = order }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Bottom sheet with details
    if (selectedOrder != null) {
        ModalBottomSheet(
            onDismissRequest = { selectedOrder = null },
            sheetState = sheetState,
            dragHandle = { BottomSheetDefaults.DragHandle() }
        ) {
            WorkOrderDetailsSheet(
                order = selectedOrder!!,
                onClose = { selectedOrder = null }
            )
        }
    }
}

@Composable
private fun WorkOrderCard(
    order: WorkOrderDto,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = MaterialTheme.shapes.medium,
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 12.dp, vertical = 10.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = order.number?.let { "№ $it" } ?: "Без номера",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.width(8.dp))
                order.date?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(Modifier.weight(1f))
                WorkOrderStatusBadge(order.status)
            }

            order.car?.takeIf { it.isNotBlank() }?.let {
                Spacer(Modifier.height(4.dp))
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            Row(
                modifier = Modifier.padding(top = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                order.customer?.takeIf { it.isNotBlank() }?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(Modifier.weight(1f))
                order.repairType?.takeIf { it.isNotBlank() }?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            order.reason?.takeIf { it.isNotBlank() }?.let {
                Spacer(Modifier.height(6.dp))
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2
                )
            }
        }
    }
}

@Composable
fun WorkOrderStatusBadge(status: String?) {
    if (status.isNullOrBlank()) return

    val (bg, fg) = when (status) {
        "Закрыт" -> MaterialTheme.colorScheme.tertiaryContainer to MaterialTheme.colorScheme.onTertiaryContainer
        "В работе" -> MaterialTheme.colorScheme.primaryContainer to MaterialTheme.colorScheme.onPrimaryContainer
        else -> MaterialTheme.colorScheme.secondaryContainer to MaterialTheme.colorScheme.onSecondaryContainer
    }

    Surface(
        color = bg,
        contentColor = fg,
        shape = MaterialTheme.shapes.small
    ) {
        Text(
            text = status,
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
        )
    }
}
