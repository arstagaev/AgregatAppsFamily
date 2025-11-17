package com.tagaev.mobileagregatcrm.ui.permissions

import androidx.compose.runtime.Composable

/** Shows [content] only when camera permission is granted. */
@Composable
expect fun CameraPermissionGate(
    rationaleText: String,
    content: @Composable () -> Unit
)