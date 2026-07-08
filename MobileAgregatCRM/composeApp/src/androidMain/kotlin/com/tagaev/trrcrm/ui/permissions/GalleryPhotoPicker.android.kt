package com.tagaev.trrcrm.ui.permissions

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val MAX_GALLERY_PICK_ITEMS = 10

@Composable
actual fun rememberGalleryPhotoPicker(
    onResult: (List<ByteArray>) -> Unit,
): (maxItems: Int) -> Unit {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val callback = remember(onResult) { onResult }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(MAX_GALLERY_PICK_ITEMS),
    ) { uris ->
        if (uris.isEmpty()) return@rememberLauncherForActivityResult
        scope.launch {
            val bytesList = withContext(Dispatchers.IO) {
                uris.mapNotNull { uri ->
                    runCatching {
                        context.contentResolver.openInputStream(uri)?.use { stream ->
                            stream.readBytes()
                        }
                    }.getOrNull()
                }
            }
            callback(bytesList)
        }
    }

    return remember(launcher) {
        { _: Int ->
            launcher.launch(
                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
            )
        }
    }
}
