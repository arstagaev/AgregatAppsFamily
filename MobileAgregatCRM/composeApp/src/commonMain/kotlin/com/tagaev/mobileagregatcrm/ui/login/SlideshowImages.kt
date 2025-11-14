import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.isActive
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import kotlin.math.min
import kotlin.math.roundToInt

enum class TrainDirection {
    LeftToRight,
    RightToLeft
}

@Suppress("UnrememberedMutableState")
@Composable
fun InfiniteImageTrain(
    modifier: Modifier = Modifier,
    images: List<DrawableResource>,
    rowHeight: Dp = 140.dp,
    // small gap so train looks continuous
    spacing: Dp = 8.dp,
    // speed in px per second
    speedPxPerSecond: Float = 40f,
    direction: TrainDirection = TrainDirection.RightToLeft,
    // how many images we roughly want visible at once
    visibleImagesOnScreen: Int = 3,
    aspectRatio: Float = 16f / 9f
) {
    if (images.isEmpty()) return

    val density = LocalDensity.current
    var viewportWidthPx by remember { mutableStateOf(0) }
    val offsetAnim = remember { Animatable(0f) }

    // ---- sizing: all images same size, scaled to fit small screens ----
    val visibleCount = visibleImagesOnScreen.coerceAtLeast(1)

    val spacingPx = with(density) { spacing.toPx() }
    val maxHeightPx = with(density) { rowHeight.toPx() }

    val maxWidthPerItemPx = if (viewportWidthPx > 0) {
        val totalSpacing = spacingPx * (visibleCount - 1)
        val freeWidth = (viewportWidthPx - totalSpacing).coerceAtLeast(0f)
        freeWidth / visibleCount.toFloat()
    } else {
        maxHeightPx * aspectRatio
    }

    val scale = min(
        maxHeightPx,              // not higher than row height
        maxWidthPerItemPx / aspectRatio
    )

    val imageHeightPx = scale
    val imageWidthPx = scale * aspectRatio

    val imageHeightDp = with(density) { imageHeightPx.toDp() }
    val imageWidthDp = with(density) { imageWidthPx.toDp() }
    val rowHeightDp = imageHeightDp

    // One step = image + gap between images
    val stepPx = imageWidthPx + spacingPx
    // True period length (from one copy of the first image to the next copy)
    val periodPx = stepPx * images.size

    // How many copies of the sequence we need so that the row
    // is always longer than the viewport → no blank gaps.
    val repeats = remember(viewportWidthPx, periodPx, images.size) {
        if (periodPx <= 0f || viewportWidthPx <= 0) {
            3
        } else {
            val base = (viewportWidthPx / periodPx).toInt() + 2
            base.coerceAtLeast(3)
        }
    }

    val repeatedImages = remember(images, repeats) {
        List(repeats * images.size) { index ->
            images[index % images.size]
        }
    }

    // ---- animation: endless circle over [0, periodPx) ----
    LaunchedEffect(periodPx, direction, speedPxPerSecond) {
        if (periodPx <= 0f || !periodPx.isFinite()) return@LaunchedEffect

        val durationMillis = ((periodPx / speedPxPerSecond) * 1000f)
            .roundToInt()
            .coerceAtLeast(1_000)

        while (isActive) {
            offsetAnim.snapTo(0f)
            offsetAnim.animateTo(
                targetValue = periodPx,
                animationSpec = tween(
                    durationMillis = durationMillis,
                    easing = LinearEasing
                )
            )
        }
    }

    // Map animation 0..period → cyclic offset and apply direction
    val translatedOffsetPx by derivedStateOf {
        val p = periodPx
        if (p <= 0f) 0f else {
            val raw = offsetAnim.value % p
            val normalized = if (raw < 0f) raw + p else raw
            when (direction) {
                TrainDirection.RightToLeft -> -normalized
                TrainDirection.LeftToRight -> normalized
            }
        }
    }

    // ---- UI ----
    Box(
        modifier = modifier
            .onGloballyPositioned { coords ->
                viewportWidthPx = coords.size.width
            }
            .height(rowHeightDp)
            .clip(RoundedCornerShape(24.dp))
            .background(Color.Black)
            .padding(horizontal = 8.dp)
            .clip(RoundedCornerShape(24.dp)) // clip moving content
    ) {
        Row(
            modifier = Modifier
                .offset { IntOffset(translatedOffsetPx.roundToInt(), 0) }
                .fillMaxHeight(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(spacing)
        ) {
            repeatedImages.forEach { res ->
                Image(
                    painter = painterResource(res),
                    contentDescription = null,
                    modifier = Modifier
                        .width(imageWidthDp)
                        .height(imageHeightDp)
                )
            }
        }
    }
}
