package com.tagaev.trrcrm.ui.common

import com.tagaev.trrcrm.ui.i18n.tr

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import compose.icons.FeatherIcons
import compose.icons.feathericons.ChevronDown
import compose.icons.feathericons.ChevronLeft
import compose.icons.feathericons.ChevronRight
import compose.icons.feathericons.ChevronUp
import compose.icons.feathericons.Minus
import compose.icons.feathericons.Plus
import compose.icons.feathericons.X

private const val MIN_ZOOM = 1f
private const val MAX_ZOOM = 5f
private const val ZOOM_STEP = 0.25f
private const val PAN_STEP = 80f

/** Scale that makes ContentScale.Fit content fill the viewport width. */
private fun fitWidthScale(bitmap: ImageBitmap, viewport: IntSize): Float {
    if (viewport.width <= 0 || viewport.height <= 0) return MIN_ZOOM
    if (bitmap.width <= 0 || bitmap.height <= 0) return MIN_ZOOM
    val vw = viewport.width.toFloat()
    val vh = viewport.height.toFloat()
    val imageAspect = bitmap.width.toFloat() / bitmap.height.toFloat()
    val viewAspect = vw / vh
    val fittedWidth = if (viewAspect > imageAspect) vh * imageAspect else vw
    if (fittedWidth <= 0f) return MIN_ZOOM
    return (vw / fittedWidth).coerceIn(MIN_ZOOM, MAX_ZOOM)
}

@Composable
fun ZoomableImagePreview(
    bitmap: ImageBitmap,
    onDismiss: () -> Unit,
) {
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    var viewportSize by remember { mutableStateOf(IntSize.Zero) }

    fun fitToWidth() {
        scale = fitWidthScale(bitmap, viewportSize)
        offset = Offset.Zero
    }

    val transformableState = rememberTransformableState { zoomChange, panChange, _ ->
        val newScale = (scale * zoomChange).coerceIn(MIN_ZOOM, MAX_ZOOM)
        scale = newScale
        if (newScale > MIN_ZOOM) {
            offset += panChange
        } else {
            offset = Offset.Zero
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.92f)),
        ) {
            Image(
                bitmap = bitmap,
                contentDescription = null,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(48.dp)
                    .onSizeChanged { viewportSize = it }
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                        translationX = offset.x
                        translationY = offset.y
                    }
                    .transformable(state = transformableState)
                    .pointerInput(bitmap, viewportSize) {
                        detectTapGestures(
                            onDoubleTap = { fitToWidth() },
                        )
                    },
                contentScale = ContentScale.Fit,
            )

            IconButton(
                onClick = onDismiss,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp),
            ) {
                Icon(
                    FeatherIcons.X,
                    contentDescription = tr("camera_zakryt"),
                    tint = Color.White,
                )
            }

            Surface(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 24.dp),
                color = Color.Black.copy(alpha = 0.45f),
                shape = androidx.compose.foundation.shape.RoundedCornerShape(24.dp),
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(
                        onClick = { offset = Offset(offset.x, offset.y + PAN_STEP) },
                    ) {
                        Icon(
                            FeatherIcons.ChevronUp,
                            contentDescription = tr("common_vverh"),
                            tint = Color.White,
                        )
                    }
                    IconButton(
                        onClick = { offset = Offset(offset.x, offset.y - PAN_STEP) },
                    ) {
                        Icon(
                            FeatherIcons.ChevronDown,
                            contentDescription = tr("common_vniz"),
                            tint = Color.White,
                        )
                    }
                    IconButton(
                        onClick = {
                            scale = (scale - ZOOM_STEP).coerceIn(MIN_ZOOM, MAX_ZOOM)
                            if (scale == MIN_ZOOM) offset = Offset.Zero
                        },
                        enabled = scale > MIN_ZOOM,
                    ) {
                        Icon(
                            FeatherIcons.Minus,
                            contentDescription = tr("common_umenshit"),
                            tint = Color.White,
                        )
                    }
                    IconButton(
                        onClick = {
                            scale = (scale + ZOOM_STEP).coerceIn(MIN_ZOOM, MAX_ZOOM)
                        },
                        enabled = scale < MAX_ZOOM,
                    ) {
                        Icon(
                            FeatherIcons.Plus,
                            contentDescription = tr("common_uvelichit"),
                            tint = Color.White,
                        )
                    }
                    IconButton(
                        onClick = { offset = Offset(offset.x + PAN_STEP, offset.y) },
                    ) {
                        Icon(
                            FeatherIcons.ChevronLeft,
                            contentDescription = tr("common_vlevo"),
                            tint = Color.White,
                        )
                    }
                    IconButton(
                        onClick = { offset = Offset(offset.x - PAN_STEP, offset.y) },
                    ) {
                        Icon(
                            FeatherIcons.ChevronRight,
                            contentDescription = tr("common_vpravo"),
                            tint = Color.White,
                        )
                    }
                }
            }
        }
    }
}
