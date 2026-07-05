package com.tagaev.trrcrm.ui.permissions

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

@Composable
actual fun rememberPhotoLibrarySavePermission(): PhotoLibrarySavePermissionState =
    remember {
        PhotoLibrarySavePermissionState(
            canSaveToGallery = false,
            shouldRequest = false,
            request = {},
        )
    }
