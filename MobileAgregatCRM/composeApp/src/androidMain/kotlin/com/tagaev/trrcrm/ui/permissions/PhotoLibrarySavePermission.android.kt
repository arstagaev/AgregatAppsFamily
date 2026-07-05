package com.tagaev.trrcrm.ui.permissions

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.core.content.PermissionChecker

@Composable
actual fun rememberPhotoLibrarySavePermission(): PhotoLibrarySavePermissionState {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        return remember {
            PhotoLibrarySavePermissionState(
                canSaveToGallery = true,
                shouldRequest = false,
                request = {},
            )
        }
    }

    val context = LocalContext.current
    fun granted(): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE) ==
            PermissionChecker.PERMISSION_GRANTED

    var hasPermission by remember { mutableStateOf(granted()) }
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { isGranted ->
        hasPermission = isGranted
    }

    return remember(hasPermission) {
        PhotoLibrarySavePermissionState(
            canSaveToGallery = hasPermission,
            shouldRequest = !hasPermission,
            request = { launcher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE) },
        )
    }
}
