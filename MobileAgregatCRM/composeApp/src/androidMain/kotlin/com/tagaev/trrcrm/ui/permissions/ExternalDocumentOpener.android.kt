package com.tagaev.trrcrm.ui.permissions

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.tagaev.trrcrm.domain.sanitizedOpenFileName
import org.koin.core.context.GlobalContext
import java.io.File

actual fun openExternalDocument(fileName: String, mimeType: String, bytes: ByteArray): Boolean {
    val context = runCatching { GlobalContext.get().get<Context>() }.getOrNull() ?: return false
    return runCatching {
        val safeName = sanitizedOpenFileName(fileName, mimeType)
        val dir = File(context.cacheDir, "open-docs")
        if (!dir.exists() && !dir.mkdirs()) return false
        val file = File(dir, safeName)
        file.writeBytes(bytes)
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file,
        )
        val viewIntent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, mimeType.ifBlank { "*/*" })
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        val chooser = Intent.createChooser(viewIntent, null).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(chooser)
        true
    }.onFailure { error ->
        CameraFixatorLog.d("open_external_document_failed name=$fileName mime=$mimeType message=${error.message}")
    }.getOrDefault(false)
}
