package com.tagaev.trrcrm.ui.custom

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import kotlin.random.Random

private data class SnowflakeSeed(
    val xFraction: Float,
    val initialOffset: Float,
    val speedMultiplier: Float,
    val radiusPx: Float
)

@Composable
fun Modifier.snowflakeBackground(
    flakes: Int = 24
): Modifier {
    // Color of the flakes (subtle)
    val flakeColor = Color.LightGray.copy(alpha = 0.75f)
    val density = LocalDensity.current

    // Pre-generate deterministic snowflake "seeds" so they don't jump every recomposition
    val seeds = remember(flakes) {
        val random = Random(42)
        List(flakes) {
            val radiusPx = with(density) {
                val minRadius = 1.dp.toPx()
                val maxRadius = 3.dp.toPx()
                minRadius + random.nextFloat() * (maxRadius - minRadius)
            }
            SnowflakeSeed(
                xFraction = random.nextFloat(),          // 0..1 across width
                initialOffset = random.nextFloat(),      // 0..1 starting vertical offset
                speedMultiplier = 0.4f + random.nextFloat() * 0.8f, // varied fall speed
                radiusPx = radiusPx
            )
        }
    }

    // Single animated "time" value that all flakes use to compute their vertical position
    val transition = rememberInfiniteTransition(label = "snowfall")
    val time by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 12000,
                easing = LinearEasing
            ),
            repeatMode = RepeatMode.Restart
        ),
        label = "snowfall-time"
    )

    return this.then(
        Modifier.drawBehind {
            val width = size.width
            val height = size.height
            if (width <= 0f || height <= 0f) return@drawBehind

            seeds.forEach { seed ->
                val x = seed.xFraction * width

                // Base distance fallen from top, wrap using modulo to keep flakes looping
                val baseFall = (time * height * seed.speedMultiplier) + (seed.initialOffset * height)
                val y = baseFall % height

                drawCircle(
                    color = flakeColor,
                    radius = seed.radiusPx,
                    center = Offset(x, y)
                )
            }
        }
    )
}