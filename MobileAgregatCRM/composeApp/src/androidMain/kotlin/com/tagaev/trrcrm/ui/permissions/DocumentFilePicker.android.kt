package com.tagaev.trrcrm.ui.permissions

import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import com.tagaev.trrcrm.models.MAX_PHOTOS_PER_UPLOAD_REQUEST
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.atomic.AtomicInteger

private val AbsoluteMaxPickItems: Int = MAX_PHOTOS_PER_UPLOAD_REQUEST.coerceAtLeast(2)

private val DOCUMENT_PICKER_MIME_TYPES = arrayOf(
    "image/jpeg",
    "image/png",
    "image/webp",
    "image/heic",
    "image/heif",
    "application/pdf",
    "application/msword",
    "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
    "application/vnd.ms-excel",
    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
    "text/plain",
    "application/rtf",
    "text/rtf",
    "text/csv",
    "text/comma-separated-values",
)

@Composable
actual fun rememberDocumentFilePicker(
    onResult: (List<PickedLocalFile>) -> Unit,
): (maxItems: Int) -> Unit {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val callback = remember(onResult) { onResult }
    val pendingMaxItems = remember { AtomicInteger(AbsoluteMaxPickItems) }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments(),
    ) { uris ->
        if (uris.isEmpty()) return@rememberLauncherForActivityResult
        val limit = pendingMaxItems.get().coerceAtLeast(1)
        scope.launch {
            val files = withContext(Dispatchers.IO) {
                uris.take(limit).mapNotNull { uri ->
                    runCatching { readPickedFile(context, uri) }.getOrNull()
                }
            }
            callback(files)
        }
    }

    return remember(launcher) {
        { maxItems: Int ->
            val limit = maxItems.coerceAtMost(AbsoluteMaxPickItems)
            if (limit <= 0) return@remember
            pendingMaxItems.set(limit)
            launcher.launch(DOCUMENT_PICKER_MIME_TYPES)
        }
    }
}

private fun readPickedFile(context: android.content.Context, uri: Uri): PickedLocalFile? {
    val resolver = context.contentResolver
    val name = resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
        val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
        if (index >= 0 && cursor.moveToFirst()) cursor.getString(index) else null
    }
    val bytes = resolver.openInputStream(uri)?.use { it.readBytes() } ?: return null
    return PickedLocalFile(bytes = bytes, fileName = name)
}
