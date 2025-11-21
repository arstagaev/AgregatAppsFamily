package com.tagaev.mobileagregatcrm.ui.work_order

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.CircularProgressIndicator
import com.tagaev.mobileagregatcrm.data.remote.Resource
import com.tagaev.mobileagregatcrm.feature.FilterByOption
import com.tagaev.mobileagregatcrm.models.WorkOrderDto
import compose.icons.FeatherIcons
import compose.icons.feathericons.RefreshCw
import compose.icons.feathericons.Filter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkOrdersScreen(
    component: WorkOrdersComponent,
    modifier: Modifier = Modifier
) {
    val resource by component.workOrders.collectAsState()
    val ncount by component.ncount.collectAsState()
    val currentFilter by component.currentFilter.collectAsState()

    val isLoadingMore by component.isLoadingMore.collectAsState()

    var selectedOrder by remember { mutableStateOf<WorkOrderDto?>(null) }
    var showFilterDialog by remember { mutableStateOf(false) }

    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = false
    )

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
                    IconButton(onClick = { showFilterDialog = true }) {
                        Icon(FeatherIcons.Filter, contentDescription = "Фильтр")
                    }
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
                    val allItems = state.data.orEmpty()
                    val items = allItems // уже отфильтровано на стороне API

                    if (items.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("Заказ-наряды не найдены")
                        }
                    } else {
                        LiveListWrapper(
                            items = items,
                            maxItems = 1000,
                            idSelector = { it.guid },
                            contentChanged = { old, new -> old != new },
                            modifier = Modifier.fillMaxSize(),
                            onRefresh = { currentSize, maxSize ->
                                component.loadMore()
                                doRefresh(currentSize, maxSize)
                            },
                            onLoadMore = { currentSize, maxSize ->

                            }
                        ) { order, isChanged, isMoved ->
                            WorkOrderCard(
                                order = order,
                                onClick = { selectedOrder = order }
                            )
                        }


                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(
                                items = items,
                                key = { it.guid ?: it.number.orEmpty() }
                            ) { order ->

                            }

                            // Пагинация: показываем кнопку "Загрузить ещё (+12)",
                            // если полученный размер >= текущего ncount
                            val showLoadMore = allItems.size >= ncount
                            if (showLoadMore) {
                                item(key = "load_more") {
                                    LoadMoreButton(
                                        onClick = { component.loadMore() },
                                        isLoading = isLoadingMore,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 8.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showFilterDialog) {
        WorkOrderFilterDialog(
            current = currentFilter,
            onDismiss = { showFilterDialog = false },
            onApply = { newFilter ->
                component.setFilter(newFilter)
                showFilterDialog = false
            }
        )
    }

    if (selectedOrder != null) {
        val currentOrder = selectedOrder!! // захватываем в локальную пер. для лямбды

        ModalBottomSheet(
            onDismissRequest = { selectedOrder = null },
            sheetState = sheetState,
            dragHandle = { BottomSheetDefaults.DragHandle() }
        ) {
            WorkOrderDetailsSheet(
                order = currentOrder,
                onClose = { selectedOrder = null },
                onSendMessage = { message ->
                    val number = currentOrder.number.orEmpty()
                    val date = currentOrder.date.orEmpty()
                    component.sendMessage(number, date, message)
                }
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
        shape = MaterialTheme.shapes.large
    ) {
        Text(
            text = status,
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
        )
    }
}

@Composable
private fun LoadMoreButton(
    onClick: () -> Unit,
    isLoading: Boolean,
    modifier: Modifier = Modifier
) {
    OutlinedButton(
        onClick = onClick,
        enabled = !isLoading,
        modifier = modifier
            .padding(horizontal = 8.dp)
            .heightIn(min = 40.dp)
    ) {
        if (isLoading) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                CircularProgressIndicator(
                    strokeWidth = 2.dp,
                    modifier = Modifier.size(18.dp)
                )
                Text("Загрузка…")
            }
        } else {
            Text("Загрузить ещё")
        }
    }
}

@Composable
private fun WorkOrderFilterDialog(
    current: FilterByOption,
    onDismiss: () -> Unit,
    onApply: (FilterByOption) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Настройка показа", fontWeight = FontWeight.SemiBold) },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    "Фильтр по состоянию",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterByOption.values().forEach { option ->
                        FilterChip(
                            selected = option == current,
                            onClick = { onApply(option) },
                            label = {
                                Text(
                                    option.label,
                                    style = MaterialTheme.typography.labelMedium
                                )
                            }
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Закрыть")
            }
        }
    )
}