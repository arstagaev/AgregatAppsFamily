package com.tagaev.trrcrm.ui.permissions

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.tagaev.trrcrm.models.MAX_PHOTOS_PER_UPLOAD_REQUEST
import java.io.File
import javax.swing.JFileChooser
import javax.swing.filechooser.FileNameExtensionFilter

@Composable
actual fun rememberDocumentFilePicker(
    onResult: (List<PickedLocalFile>) -> Unit,
): (maxItems: Int) -> Unit {
    val callback = remember(onResult) { onResult }

    return remember {
        { maxItems: Int ->
            if (maxItems <= 0) {
                callback(emptyList())
                return@remember
            }
            val chooser = JFileChooser().apply {
                dialogTitle = "Выберите файлы"
                isMultiSelectionEnabled = maxItems > 1
                fileSelectionMode = JFileChooser.FILES_ONLY
                fileFilter = FileNameExtensionFilter(
                    "Фото и документы",
                    "jpg",
                    "jpeg",
                    "png",
                    "webp",
                    "heic",
                    "heif",
                    "pdf",
                    "doc",
                    "docx",
                    "xls",
                    "xlsx",
                    "txt",
                    "rtf",
                    "csv",
                )
            }
            val result = chooser.showOpenDialog(null)
            if (result != JFileChooser.APPROVE_OPTION) {
                callback(emptyList())
                return@remember
            }
            val selected = if (chooser.isMultiSelectionEnabled) {
                chooser.selectedFiles.toList()
            } else {
                listOfNotNull(chooser.selectedFile)
            }.take(maxItems.coerceAtLeast(1))
            callback(
                selected.mapNotNull { file ->
                    runCatching {
                        PickedLocalFile(
                            bytes = File(file.absolutePath).readBytes(),
                            fileName = file.name,
                        )
                    }.getOrNull()
                },
            )
        }
    }
}
