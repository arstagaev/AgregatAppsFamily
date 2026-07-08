package com.tagaev.trrcrm.ui.permissions

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import java.io.File
import javax.swing.JFileChooser
import javax.swing.filechooser.FileNameExtensionFilter

@Composable
actual fun rememberGalleryPhotoPicker(
    onResult: (List<ByteArray>) -> Unit,
): (maxItems: Int) -> Unit {
    val callback = remember(onResult) { onResult }

    return remember {
        { maxItems: Int ->
            val chooser = JFileChooser().apply {
                dialogTitle = "Выберите фотографии"
                isMultiSelectionEnabled = maxItems > 1
                fileSelectionMode = JFileChooser.FILES_ONLY
                fileFilter = FileNameExtensionFilter(
                    "Изображения",
                    "jpg",
                    "jpeg",
                    "png",
                    "webp",
                    "heic",
                )
            }
            val result = chooser.showOpenDialog(null)
            if (result != JFileChooser.APPROVE_OPTION) {
                callback(emptyList())
                return@remember
            }
            val files = chooser.selectedFiles
                .toList()
                .take(maxItems.coerceAtLeast(1))
            val bytesList = files.mapNotNull { file ->
                runCatching { File(file.absolutePath).readBytes() }.getOrNull()
            }
            callback(bytesList)
        }
    }
}
