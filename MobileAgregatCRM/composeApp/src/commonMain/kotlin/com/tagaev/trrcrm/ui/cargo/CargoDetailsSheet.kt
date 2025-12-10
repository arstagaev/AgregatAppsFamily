package com.tagaev.trrcrm.ui.cargo

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.tagaev.trrcrm.models.CargoDto
import com.tagaev.trrcrm.ui.custom.TextC
import compose.icons.FeatherIcons
import compose.icons.feathericons.ArrowRight
import compose.icons.feathericons.ChevronDown


@Composable
fun CargoDetailsSheet(
    cargo: CargoDto,
    onClose: () -> Unit
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                TextC(
                    text = "${cargo.number}",
                    style = MaterialTheme.typography.headlineLarge
                )
                TextC(
                    text = "${cargo.route}",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    overflow = TextOverflow.Ellipsis
                )
//                Text(
//                    text = "Организация: ${cargo.organization}",
//                    style = MaterialTheme.typography.bodySmall,
//                    color = MaterialTheme.colorScheme.onSurfaceVariant,
//                    maxLines = 1,
//                    overflow = TextOverflow.Ellipsis
//                )

            }
        }

        // Основная информация (самое важное сверху).
        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Основная информация",
                    style = MaterialTheme.typography.titleMedium
                )

                InfoFieldGrid(
                    fields = listOf(
                        "Проведен" to cargo.posted,
                        "Пометка удаления" to cargo.deletionMark,

                        "Номер" to cargo.number,
                        "Дата" to cargo.date,

                        "Состояние" to cargo.status,
                        "Маршрут" to cargo.route,

                        "Организация" to cargo.organization,
                        "Подразделение" to cargo.department,

                        "Автор" to cargo.author,
                        "Количество мест" to cargo.placesCount,

                        "Сумма документа" to cargo.amount
                    )
                )
            }
        }

        // Комментарий (если есть) — отдельной карточкой.
        if (!cargo.comment.isNullOrBlank()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "Комментарий",
                        style = MaterialTheme.typography.titleMedium
                    )
                    TextC(
                        text = cargo.comment,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }

        // Логистическая информация и служебные поля можно также свернуть в сетку.
        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Логистика",
                    style = MaterialTheme.typography.titleMedium
                )

                InfoFieldGrid(
                    fields = listOf(
                        "Дата отправления" to cargo.departureDate , //shipDate,
                        "Дата прибытия" to cargo.arrivalDate,
                        "Перевозка службой доставки" to cargo.deliveredByService,
                        "Грузоперевозчик" to cargo.carrier,
                        "Контакт перевозчика" to cargo.carrierContacts,
                        "№ накладной" to cargo.carrierInvoiceNumber,
                        "Вес" to cargo.weight,
                        "Объем" to cargo.volume,
                        "Длина" to cargo.length,
                        "Ширина" to cargo.width,
                        "Высота" to cargo.height,
//                        "Путь к файлам" to cargo.filesPath,
                        "QR" to cargo.qr,
                        "Дата отправки сообщения" to cargo.messageSentAt,
                        "Состояние сообщения" to cargo.messageStatus,
                        "Комплектовщик" to cargo.picker
                    )
                )
            }
        }

        // Внизу — разворачиваемые блоки со списками.
        if (cargo.orders.isNotEmpty()) {
            ExpandableListSection(
                title = "Заказы",
                items = cargo.orders
            ) { order ->
//                val mainText = when (order) {
//                    is String -> order
//                    else -> order.toString()
//                }
                TextC(
                    text = order.order,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        if (cargo.products.isNotEmpty()) {
            ExpandableListSection(
                title = "Товары",
                items = cargo.products
            ) { product ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        // 1:1 ratio for square menu tiles
                        ,
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Column(Modifier.padding(3.dp).fillMaxWidth()) {
                        TextC(
                            text = "${product.order}",
                            style = MaterialTheme.typography.bodyMedium,
                            overflow = TextOverflow.Ellipsis
                        )
                        TextC(
                            text = "${product.productName} (${product.quantity} ${product.unit})",
                            style = MaterialTheme.typography.bodyMedium,
                            overflow = TextOverflow.Ellipsis
                        )
                        TextC(
                            text = "${product.comment}",
                            style = MaterialTheme.typography.bodyMedium,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

            }
        }

//        if (cargo.cargos.isNotEmpty()) {
//            ExpandableListSection(
//                title = "Грузы",
//                items = cargo.cargos
//            ) { nested ->
//                val mainText = when (nested) {
//                    is String -> nested
//                    else -> nested.toString()
//                }
//                Text(
//                    text = mainText,
//                    style = MaterialTheme.typography.bodyMedium,
//                    maxLines = 2,
//                    overflow = TextOverflow.Ellipsis
//                )
//            }
//        }
    }
}

@Composable
fun <T> ExpandableListSection(
    title: String,
    items: List<T>,
    initiallyExpanded: Boolean = false,
    itemContent: @Composable (T) -> Unit
) {
    var expanded by remember { mutableStateOf(initiallyExpanded) }
    val rotation by animateFloatAsState(
        targetValue = if (expanded) 180f else 0f,
        label = "arrowRotation"
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded }
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f)
                )
                Icon(
                    imageVector = FeatherIcons.ChevronDown,
                    contentDescription = null,
                    modifier = Modifier.rotate(rotation)
                )
            }

            AnimatedVisibility(visible = expanded) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    if (items.isNotEmpty()) {
                        items.forEach { item ->
                            itemContent(item)
                        }
                    } else {
                        Text(
                            text = "Пусто",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}
@Composable
private fun InfoFieldGrid(
    fields: List<Pair<String, String?>>,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        fields
            .filter { !it.second.isNullOrBlank() }
            .chunked(2)
            .forEach { rowItems ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    rowItems.forEach { (label, value) ->
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            TextC(
                                text = value.orEmpty(),
                                style = MaterialTheme.typography.bodyMedium,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                    if (rowItems.size == 1) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
    }
}