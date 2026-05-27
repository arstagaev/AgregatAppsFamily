package com.tagaev.trrcrm.ui.product_demo

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun BasketTab(
    basketItems: List<BasketItem>,
    onToggle: (String) -> Unit,
    onRemove: (String) -> Unit,
    onOrderRequest: () -> Unit,
) {
    val selectedCount = basketItems.count { it.isSelected }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text("Выбранные товары", style = MaterialTheme.typography.headlineSmall)
        if (basketItems.isEmpty()) {
            Text(
                text = "Добавьте товары из каталога, чтобы отправить заявку",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(basketItems, key = { it.product.id }) { item ->
                    OutlinedCard(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = item.isSelected,
                                    onCheckedChange = { onToggle(item.product.id) }
                                )
                                Column {
                                    Text(
                                        text = item.product.name,
                                        fontWeight = FontWeight.Medium,
                                        style = MaterialTheme.typography.bodyLarge
                                    )
                                    Text(
                                        text = item.product.id,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            OutlinedButton(onClick = { onRemove(item.product.id) }) {
                                Text("Удалить")
                            }
                        }
                    }
                }
            }
        }

        Button(
            onClick = onOrderRequest,
            modifier = Modifier.fillMaxWidth(),
            enabled = selectedCount > 0
        ) {
            Text("Отправить заявку ($selectedCount)")
        }
    }
}
