package com.tagaev.trrcrm.ui.permissions

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap

@Composable
actual fun FixatorCameraPreview(
    modifier: Modifier,
    onPhotoCaptured: (ByteArray) -> Unit,
    onControlsChanged: (FixatorCameraControls) -> Unit,
    onLog: (String) -> Unit,
) {
    LaunchedEffect(Unit) {
        onLog("desktop preview stub")
        onControlsChanged(
            FixatorCameraControls(
                capturePhoto = { onLog("desktop capture unavailable") },
                toggleTorch = { onLog("desktop torch unavailable") },
                isTorchOn = false,
                isTorchAvailable = false,
            ),
        )
    }

    Box(
        modifier = modifier.background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "Камера недоступна на desktop",
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

actual fun decodePhotoThumbnail(bytes: ByteArray): ImageBitmap? = null
