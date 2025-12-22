package com.tagaev.trrcrm.push

import androidx.compose.runtime.Composable

/**
 * Returns a function you can call from any Composable
 * to request notification permission.
 *
 * onResult(true)  -> permission granted / not needed
 * onResult(false) -> permission denied
 */
@Composable
expect fun rememberNotificationPermissionRequester(
    onResult: (Boolean) -> Unit
): () -> Unit
