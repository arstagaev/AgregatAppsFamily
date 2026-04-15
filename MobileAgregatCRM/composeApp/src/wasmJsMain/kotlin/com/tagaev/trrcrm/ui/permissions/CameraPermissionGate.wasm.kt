package com.tagaev.trrcrm.ui.permissions

import androidx.compose.runtime.Composable

@Composable
actual fun CameraPermissionGate(
    rationaleText: String,
    content: @Composable () -> Unit
) {
    // Web target: QR/camera flow is disabled for this simplified target.
    content()
}

