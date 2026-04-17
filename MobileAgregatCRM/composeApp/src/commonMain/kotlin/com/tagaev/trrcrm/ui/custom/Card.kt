package com.tagaev.trrcrm.ui.custom

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
fun UniversalCardItem(
    modifier: Modifier = Modifier,
    // Top level (required title)
    title: String,
    subtitle: String? = null,
    topRightPrimary: (@Composable () -> Unit)? = null,
    topRightSecondary: (@Composable () -> Unit)? = null,

    // Middle level A – big texts (max 3)
    bigText1: String? = null,
    bigText2: String? = null,
    bigText3: String? = null,

    // Middle level B – medium texts (max 2)
    midBText1: String? = null,
    midBText2: String? = null,

    // Middle level C – medium texts (max 2)
    midCText1: String? = null,
    midCText2: String? = null,

    // Bottom – two texts aligned left / right
    bottomLeftText: String? = null,
    bottomRightText: String? = null,

    onClick: (() -> Unit)? = null,
    allowTitleLongPressCopy: Boolean = false,
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .then(
                if (onClick != null) Modifier.clickable { onClick() } else Modifier
            ),
        shape = MaterialTheme.shapes.medium,
//        colors = CardDefaults.cardColors(
//            containerColor = MaterialTheme.colorScheme.surface,
//            contentColor = MaterialTheme.colorScheme.onSurface
//        )

        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp)
        ) {
            // ───────── Top level: title / subtitle + topRight slots ─────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                if (topRightPrimary != null) {
                    TextC(
                        text = title,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.weight(1f),
                        allowLinkTap = false,
                        allowLongPressCopy = allowTitleLongPressCopy,
                    )
                    Box(
                        modifier = Modifier.align(Alignment.CenterVertically)
                    ) {
                        topRightPrimary()
                    }
                } else {
                    TextC(
                        text = title,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.fillMaxWidth(),
                        allowLinkTap = false,
                        allowLongPressCopy = allowTitleLongPressCopy,
                    )
                }
            }
            if (!subtitle.isNullOrBlank() || topRightSecondary != null) {
                Spacer(Modifier.height(2.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Top
                ) {
                    if (!subtitle.isNullOrBlank() && topRightSecondary != null) {
                        TextC(
                            text = subtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.weight(1f),
                            allowLinkTap = false,
                            allowLongPressCopy = false,
                        )
                        Box(
                            modifier = Modifier.align(Alignment.CenterVertically)
                        ) {
                            topRightSecondary()
                        }
                    } else if (!subtitle.isNullOrBlank()) {
                        TextC(
                            text = subtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.fillMaxWidth(),
                            allowLinkTap = false,
                            allowLongPressCopy = false,
                        )
                    } else if (topRightSecondary != null) {
                        Box(
                            modifier = Modifier.fillMaxWidth(),
                            contentAlignment = Alignment.CenterEnd
                        ) {
                            topRightSecondary()
                        }
                    }
                }
            }

            // ───────── Middle level A – big texts (up to 3) ─────────
            val hasBig =
                !bigText1.isNullOrBlank() ||
                        !bigText2.isNullOrBlank() ||
                        !bigText3.isNullOrBlank()

            if (hasBig) {
                Spacer(Modifier.height(6.dp))
                Column {
                    bigText1?.takeIf { it.isNotBlank() }?.let {
                        TextC(
                            text = it.withEscapedNewlines(),
                            style = MaterialTheme.typography.bodyMedium,
                            allowLinkTap = false,
                            allowLongPressCopy = false,
                        )
                    }
                    bigText2?.takeIf { it.isNotBlank() }?.let {
                        Spacer(Modifier.height(2.dp))
                        TextC(
                            text = it.withEscapedNewlines(),
                            style = MaterialTheme.typography.bodyMedium,
                            allowLinkTap = false,
                            allowLongPressCopy = false,
                        )
                    }
                    bigText3?.takeIf { it.isNotBlank() }?.let {
                        Spacer(Modifier.height(2.dp))
                        TextC(
                            text = it.withEscapedNewlines(),
                            style = MaterialTheme.typography.bodyMedium,
                            allowLinkTap = false,
                            allowLongPressCopy = false,
                        )
                    }
                }
            }

            // ───────── Middle level B – two medium texts in a row ─────────
            val hasMidB = !midBText1.isNullOrBlank() || !midBText2.isNullOrBlank()

            if (hasMidB) {
                Spacer(Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Top
                ) {
                    midBText1?.takeIf { it.isNotBlank() }?.let {
                        TextC(
                            text = it.withEscapedNewlines(),
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.weight(1f),
                            allowLinkTap = false,
                            allowLongPressCopy = false,
                        )
                    } ?: Spacer(Modifier.weight(1f))

                    midBText2?.takeIf { it.isNotBlank() }?.let {
                        TextC(
                            text = it.withEscapedNewlines(),
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.weight(1f),
                            allowLinkTap = false,
                            allowLongPressCopy = false,
                        )
                    } ?: Spacer(Modifier.weight(1f))
                }
            }

            // ───────── Middle level C – two medium texts in a row ─────────
            val hasMidC = !midCText1.isNullOrBlank() || !midCText2.isNullOrBlank()

            if (hasMidC) {
                Spacer(Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Top
                ) {
                    midCText1?.takeIf { it.isNotBlank() }?.let {
                        TextC(
                            text = it.withEscapedNewlines(),
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.weight(1f),
                            allowLinkTap = false,
                            allowLongPressCopy = false,
                        )
                    } ?: Spacer(Modifier.weight(1f))

                    midCText2?.takeIf { it.isNotBlank() }?.let {
                        TextC(
                            text = it.withEscapedNewlines(),
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.weight(1f),
                            allowLinkTap = false,
                            allowLongPressCopy = false,
                        )
                    } ?: Spacer(Modifier.weight(1f))
                }
            }

            // ───────── Bottom level – left/right texts ─────────
            val hasBottom =
                !bottomLeftText.isNullOrBlank() || !bottomRightText.isNullOrBlank()

            if (hasBottom) {
                Spacer(Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    bottomLeftText?.takeIf { it.isNotBlank() }?.let {
                        TextC(
                            text = it,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            allowLinkTap = false,
                            allowLongPressCopy = false,
                        )
                    }

                    Spacer(Modifier.weight(1f))

                    bottomRightText?.takeIf { it.isNotBlank() }?.let {
                        TextC(
                            text = it,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            allowLinkTap = false,
                            allowLongPressCopy = false,
                        )
                    }
                }
            }
        }
    }
}
///

@Preview(showBackground = false)
@Composable
fun UniversalCardItemPreview() {
    MaterialTheme {
        Surface(
            modifier = Modifier.fillMaxSize()
        ) {
            UniversalCardItem(
                title = "ГПТ0000070",
                subtitle = "Заказано",

                topRightPrimary = {
                    // e.g. status chip imitation
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        shape = MaterialTheme.shapes.small,
                        tonalElevation = 0.dp
                    ) {
                        Text(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            text = "Высокий приоритет",
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                },
                topRightSecondary = {
                    Text(
                        text = "09.12.2025",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },

                // Middle A (big)
                bigText1 = "Рекламация по вибрации",
                bigText2 = "Nissan Teana, JF017E (CVT)",
                bigText3 = "Клиент жалуется на сильную вибрацию при 20–40 км/ч",

                // Middle B (two medium texts)
                midBText1 = "Организация: ООО САМАРА АКПП",
                midBText2 = "Подразделение: Сургут",

                // Middle C (two medium texts)
                midCText1 = "Ответственный: Голиков Максим",
                midCText2 = "Источник: Яндекс",

                // Bottom
                bottomLeftText = "Автор: Елесин Владислав",
                bottomRightText = "guid: 9028ff9c..."
            )
        }
    }
}