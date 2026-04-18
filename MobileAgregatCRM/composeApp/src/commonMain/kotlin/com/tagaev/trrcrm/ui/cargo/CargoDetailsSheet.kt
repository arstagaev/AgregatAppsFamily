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
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.layout.layout
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.tagaev.trrcrm.domain.TreeRootDocument
import com.tagaev.trrcrm.domain.normalizeRawDocumentLabel
import com.tagaev.trrcrm.models.CargoDto
import com.tagaev.trrcrm.ui.custom.TextC
import com.tagaev.trrcrm.ui.work_order.formatProductQuantityWithUnit
import compose.icons.FeatherIcons
import compose.icons.feathericons.ArrowRight
import compose.icons.feathericons.ChevronDown


@Composable
fun CargoDetailsSheet(
    cargo: CargoDto,
    onClose: () -> Unit,
    onOpenBaseDocument: (String) -> Unit = {},
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

        cargo.baseDocument.takeIf { it.isNotBlank() }?.let { baseDocument ->
            val cleanedBase = remember(baseDocument) { normalizeRawDocumentLabel(baseDocument) }
            Text(
                text = "Документ-основание",
                style = MaterialTheme.typography.titleSmall
            )
            TextC(
                text = cleanedBase,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.clickable { onOpenBaseDocument(cleanedBase) },
                allowLinkTap = false,
                allowLongPressCopy = false,
            )
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
                CargoDocumentReferenceText(
                    raw = order.order,
                    maxLines = 2,
                    onOpen = onOpenBaseDocument,
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
                        CargoDocumentReferenceText(
                            raw = product.order,
                            maxLines = 3,
                            onOpen = onOpenBaseDocument,
                        )
                        TextC(
                            text = "${product.productName} (${formatProductQuantityWithUnit(product.quantity, product.unit) ?: "—"})",
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
private fun CargoDocumentReferenceText(
    raw: String,
    maxLines: Int,
    onOpen: (String) -> Unit,
) {
    val cleaned = remember(raw) { normalizeRawDocumentLabel(raw) }
    if (cleaned.isBlank()) return
    val isDocLink = remember(cleaned) { TreeRootDocument.parse(cleaned) != null }
    if (isDocLink) {
        TextC(
            text = cleaned,
            style = MaterialTheme.typography.bodyMedium,
            maxLines = maxLines,
            overflow = TextOverflow.Ellipsis,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.clickable { onOpen(cleaned) },
            allowLinkTap = false,
            allowLongPressCopy = false,
        )
    } else {
        TextC(
            text = cleaned,
            style = MaterialTheme.typography.bodyMedium,
            maxLines = maxLines,
            overflow = TextOverflow.Ellipsis,
            allowLinkTap = false,
            allowLongPressCopy = false,
        )
    }
}

/** Widen a child to extend past the parent's horizontal bounds (Compose forbids negative padding). */
private fun Modifier.expandDividerHorizontally(outdent: Dp): Modifier =
    if (outdent == 0.dp) {
        this
    } else {
        this.layout { measurable, constraints ->
            val ox = outdent.roundToPx()
            val w = constraints.maxWidth + 2 * ox
            val placeable = measurable.measure(
                Constraints(
                    minWidth = w,
                    maxWidth = w,
                    minHeight = constraints.minHeight,
                    maxHeight = constraints.maxHeight
                )
            )
            layout(constraints.maxWidth, placeable.height) {
                placeable.placeRelative(-ox, 0)
            }
        }
    }

@Composable
fun <T> ExpandableListSection(
    title: String,
    items: List<T>,
    initiallyExpanded: Boolean = false,
    listContentPadding: PaddingValues = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
    itemSpacing: Dp = 4.dp,
    showItemDividers: Boolean = false,
    dividerHorizontalOutdent: Dp = 0.dp,
    /**
     * When `null` (default), section manages expand/collapse with [initiallyExpanded] as the initial value.
     * When non-null, the caller owns `expanded` and should pass [onExpandedChange] to toggle.
     */
    expanded: Boolean? = null,
    onExpandedChange: ((Boolean) -> Unit)? = null,
    itemContent: @Composable (T) -> Unit
) {
    var uncontrolledExpanded by remember { mutableStateOf(initiallyExpanded) }
    val effectiveExpanded = expanded ?: uncontrolledExpanded
    val rotation by animateFloatAsState(
        targetValue = if (effectiveExpanded) 180f else 0f,
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
                    .clickable {
                        if (onExpandedChange != null && expanded != null) {
                            onExpandedChange(!expanded)
                        } else {
                            uncontrolledExpanded = !uncontrolledExpanded
                        }
                    }
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

            AnimatedVisibility(visible = effectiveExpanded) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(listContentPadding),
                    verticalArrangement = Arrangement.spacedBy(itemSpacing)
                ) {
                    if (items.isNotEmpty()) {
                        items.forEachIndexed { index, item ->
                            if (showItemDividers && index > 0) {
                                HorizontalDivider(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .expandDividerHorizontally(dividerHorizontalOutdent),
                                    thickness = 1.dp,
                                    color = MaterialTheme.colorScheme.outlineVariant
                                )
                            }
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