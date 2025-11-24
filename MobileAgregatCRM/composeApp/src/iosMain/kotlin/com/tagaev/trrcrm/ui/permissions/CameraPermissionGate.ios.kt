package com.tagaev.trrcrm.ui.permissions

import androidx.compose.runtime.Composable

@Composable
actual fun CameraPermissionGate(
    rationaleText: String,
    content: @Composable () -> Unit
) {
    // iOS: the system prompts at first camera use; just show content.
    content()
}