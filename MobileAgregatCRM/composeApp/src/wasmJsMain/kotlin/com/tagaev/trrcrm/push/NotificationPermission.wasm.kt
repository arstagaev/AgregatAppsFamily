package com.tagaev.trrcrm.push

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

@Composable
actual fun rememberNotificationPermissionRequester(
    onResult: (Boolean) -> Unit
): () -> Unit {
    return remember {
        { onResult(true) }
    }
}

