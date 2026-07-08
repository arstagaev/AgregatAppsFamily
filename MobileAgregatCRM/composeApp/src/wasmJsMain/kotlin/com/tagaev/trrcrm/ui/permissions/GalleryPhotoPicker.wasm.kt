package com.tagaev.trrcrm.ui.permissions

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

@Composable
actual fun rememberGalleryPhotoPicker(
    onResult: (List<ByteArray>) -> Unit,
): (maxItems: Int) -> Unit {
    val callback = remember(onResult) { onResult }

    return remember {
        { _: Int ->
            callback(emptyList())
        }
    }
}
