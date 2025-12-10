package com.tagaev.trrcrm.ui.custom

import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

data class StatusStyle(
    val background: Color,
    val foreground: Color,
)

/**
 * Универсальный бэйдж: цвета задаются снаружи.
 *
 * @param state       текст статуса
 * @param styles      мапа "статус" -> стиль (цвет фона/текста)
 * @param defaultStyle стиль по умолчанию, если статус не найден в styles
 * @param ignoreCase  искать статус без учета регистра
 */
@Composable
fun StatusBadge(
    state: String,
    styles: Map<String, StatusStyle> = mapOf(),
    modifier: Modifier = Modifier,
    defaultStyle: StatusStyle = StatusStyle(
        background = MaterialTheme.colorScheme.secondaryContainer,
        foreground = MaterialTheme.colorScheme.onSecondaryContainer
    ),
    ignoreCase: Boolean = true
) {
    if (state.isBlank()) return

    val lookupMap =
        if (ignoreCase) styles.mapKeys { it.key.lowercase() } else styles

    val key = if (ignoreCase) state.lowercase() else state
    val style = lookupMap[key] ?: defaultStyle

    Surface(
        color = style.background,
        contentColor = style.foreground,
        shape = MaterialTheme.shapes.small,
        modifier = modifier
    ) {
        Text(
            text = state,
            modifier = Modifier.basicMarquee().padding(horizontal = 8.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelMedium
        )
    }
}
