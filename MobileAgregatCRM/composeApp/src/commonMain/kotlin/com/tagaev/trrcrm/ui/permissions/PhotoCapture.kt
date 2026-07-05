package com.tagaev.trrcrm.ui.permissions

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap

data class FixatorCameraControls(
    val capturePhoto: () -> Unit,
    val toggleTorch: () -> Unit,
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
