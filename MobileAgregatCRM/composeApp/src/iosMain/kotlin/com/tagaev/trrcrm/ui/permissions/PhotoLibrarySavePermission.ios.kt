package com.tagaev.trrcrm.ui.permissions

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import platform.Photos.PHAccessLevelAddOnly
import platform.Photos.PHAuthorizationStatusAuthorized
import platform.Photos.PHAuthorizationStatusDenied
import platform.Photos.PHAuthorizationStatusLimited
import platform.Photos.PHAuthorizationStatusNotDetermined
import platform.Photos.PHAuthorizationStatusRestricted
import platform.Photos.PHPhotoLibrary

@Composable
actual fun rememberPhotoLibrarySavePermission(): PhotoLibrarySavePermissionState {
    fun currentGranted(): Boolean {
        val status = PHPhotoLibrary.authorizationStatusForAccessLevel(PHAccessLevelAddOnly)
        return status == PHAuthorizationStatusAuthorized || status == PHAuthorizationStatusLimited
    }

    var hasPermission by remember { mutableStateOf(currentGranted()) }

    return remember(hasPermission) {
        val status = PHPhotoLibrary.authorizationStatusForAccessLevel(PHAccessLevelAddOnly)
        PhotoLibrarySavePermissionState(
            canSaveToGallery = hasPermission,
            shouldRequest = status == PHAuthorizationStatusNotDetermined,
            request = {
                PHPhotoLibrary.requestAuthorizationForAccessLevel(PHAccessLevelAddOnly) { newStatus ->
                    hasPermission = newStatus == PHAuthorizationStatusAuthorized ||
                        newStatus == PHAuthorizationStatusLimited
                }
            },
        )
    }
}
