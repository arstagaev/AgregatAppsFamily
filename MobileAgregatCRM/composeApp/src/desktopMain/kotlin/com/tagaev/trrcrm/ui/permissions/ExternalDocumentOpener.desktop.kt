package com.tagaev.trrcrm.ui.permissions

import com.tagaev.trrcrm.domain.sanitizedOpenFileName
import java.awt.Desktop
import java.nio.file.Files

actual fun openExternalDocument(fileName: String, mimeType: String, bytes: ByteArray): Boolean {
    return runCatching {
        if (!Desktop.isDesktopSupported()) return false
        val desktop = Desktop.getDesktop()
        if (!desktop.isSupported(Desktop.Action.OPEN)) return false
        val safeName = sanitizedOpenFileName(fileName, mimeType)
        val dir = Files.createTempDirectory("trr-open-docs")
        val file = dir.resolve(safeName).toFile()
        file.writeBytes(bytes)
        desktop.open(file)
        true
    }.onFailure { error ->
        CameraFixatorLog.d("open_external_document_failed name=$fileName mime=$mimeType message=${error.message}")
    }.getOrDefault(false)
}
