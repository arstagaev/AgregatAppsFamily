// composeApp/src/iosMain/kotlin/notifications/NotificationPermission.ios.kt
package com.tagaev.trrcrm.push

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

@Composable
actual fun rememberNotificationPermissionRequester(
    onResult: (Boolean) -> Unit
): () -> Unit {
    // iOS handles notification permission natively in Swift, not here.
    // From common code we just consider it "not handled" or "granted".
    return remember {
        {
            onResult(true)
        }
    }
}
