package com.tagaev.trrcrm.ui.permissions

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import com.tagaev.trrcrm.models.MAX_PHOTOS_PER_DOCUMENT_PER_APP_RUN
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.atomic.AtomicInteger

/** Absolute upper bound for multi-select registration (platform min of this and request). */
private val AbsoluteMaxPickItems: Int = MAX_PHOTOS_PER_DOCUMENT_PER_APP_RUN.coerceAtLeast(2)

@Composable
actual fun rememberGalleryPhotoPicker(
    onResult: (List<ByteArray>) -> Unit,
): (maxItems: Int) -> Unit {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val callback = remember(onResult) { onResult }
    val pendingMaxItems = remember { AtomicInteger(AbsoluteMaxPickItems) }

    fun deliverUris(uris: List<Uri>) {
        if (uris.isEmpty()) return
        val limit = pendingMaxItems.get().coerceAtLeast(1)
        scope.launch {
            val bytesList = withContext(Dispatchers.IO) {
                uris.take(limit).mapNotNull { uri ->
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

    val multipleLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(AbsoluteMaxPickItems),
    ) { uris ->
        deliverUris(uris)
    }

    val singleLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
    ) { uri ->
        if (uri != null) deliverUris(listOf(uri))
    }

    return remember(multipleLauncher, singleLauncher) {
        { maxItems: Int ->
            val limit = maxItems.coerceAtMost(AbsoluteMaxPickItems)
            if (limit <= 0) return@remember
            pendingMaxItems.set(limit)
            when (limit) {
                1 -> singleLauncher.launch(
                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                )
                else -> multipleLauncher.launch(
                    PickVisualMediaRequest(
                        mediaType = ActivityResultContracts.PickVisualMedia.ImageOnly,
                        maxItems = limit,
                    ),
                )
            }
        }
    }
}
