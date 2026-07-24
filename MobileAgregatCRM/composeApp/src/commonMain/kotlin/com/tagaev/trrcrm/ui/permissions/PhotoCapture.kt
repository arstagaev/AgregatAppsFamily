package com.tagaev.trrcrm.ui.permissions

import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

data class FixatorCameraControls(
    val capturePhoto: () -> Unit,
    val toggleTorch: () -> Unit,
    /** Normalized preview coordinates in 0..1 range (origin top-left). */
    val focusAtNormalized: (x: Float, y: Float) -> Unit,
    val isTorchOn: Boolean,
    val isTorchAvailable: Boolean,
)

@Composable
expect fun FixatorCameraPreview(
    modifier: Modifier,
    onPhotoCaptured: (ByteArray) -> Unit,
    onControlsChanged: (FixatorCameraControls) -> Unit,
    onLog: (String) -> Unit = CameraFixatorLog::d,
)

expect fun decodePhotoThumbnail(bytes: ByteArray): ImageBitmap?

/** Max long edge when decoding photos for on-screen preview/grid (avoids Canvas OOM). */
internal const val PHOTO_DISPLAY_MAX_LONG_EDGE_PX = 2048

private val FocusRingSize = 60.dp
private val FocusRingDurationMs = 1_000L

/**
 * Preview wrapper: tap-to-focus gesture + short-lived focus ring.
 * [onTapNormalized] receives coordinates in 0..1 relative to the preview box.
 */
@Composable
internal fun CameraFocusTapOverlay(
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onTapNormalized: (x: Float, y: Float) -> Unit,
    content: @Composable BoxScope.() -> Unit,
) {
    var focusRing by remember { mutableStateOf<Offset?>(null) }
    val density = LocalDensity.current
    val ringHalfPx = with(density) { FocusRingSize.toPx() / 2f }

    LaunchedEffect(focusRing) {
        if (focusRing == null) return@LaunchedEffect
        delay(FocusRingDurationMs)
        focusRing = null
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(enabled) {
                if (!enabled) return@pointerInput
                detectTapGestures { offset ->
                    val width = size.width.coerceAtLeast(1)
                    val height = size.height.coerceAtLeast(1)
                    val nx = (offset.x / width).coerceIn(0f, 1f)
                    val ny = (offset.y / height).coerceIn(0f, 1f)
                    focusRing = offset
                    onTapNormalized(nx, ny)
                }
            },
    ) {
        content()
        focusRing?.let { pos ->
            Box(
                modifier = Modifier
                    .offset {
                        IntOffset(
                            x = (pos.x - ringHalfPx).roundToInt(),
                            y = (pos.y - ringHalfPx).roundToInt(),
                        )
                    }
                    .size(FocusRingSize)
                    .border(2.dp, Color.White, RoundedCornerShape(4.dp)),
            )
        }
    }
}
