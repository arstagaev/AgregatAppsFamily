package com.tagaev.trrcrm.ui.permissions

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable

@Stable
class PhotoLibrarySavePermissionState(
    val canSaveToGallery: Boolean,
    val shouldRequest: Boolean,
    val request: () -> Unit,
)

@Composable
expect fun rememberPhotoLibrarySavePermission(): PhotoLibrarySavePermissionState
