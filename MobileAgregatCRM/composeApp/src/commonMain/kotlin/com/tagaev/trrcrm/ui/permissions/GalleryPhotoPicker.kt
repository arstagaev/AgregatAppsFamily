package com.tagaev.trrcrm.ui.permissions

import androidx.compose.runtime.Composable

@Composable
expect fun rememberGalleryPhotoPicker(
    onResult: (List<ByteArray>) -> Unit,
): (maxItems: Int) -> Unit
