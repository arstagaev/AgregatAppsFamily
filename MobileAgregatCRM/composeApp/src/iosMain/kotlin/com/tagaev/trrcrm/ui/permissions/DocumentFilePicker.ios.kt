package com.tagaev.trrcrm.ui.permissions

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.tagaev.trrcrm.models.MAX_PHOTOS_PER_UPLOAD_REQUEST
import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import platform.Foundation.NSData
import platform.Foundation.NSURL
import platform.Foundation.dataWithContentsOfURL
import platform.Foundation.lastPathComponent
import platform.UIKit.UIApplication
import platform.UIKit.UIDocumentPickerDelegateProtocol
import platform.UIKit.UIDocumentPickerViewController
import platform.UIKit.UIViewController
import platform.UIKit.UIWindow
import platform.UIKit.UIWindowScene
import platform.UniformTypeIdentifiers.UTType
import platform.UniformTypeIdentifiers.UTTypeImage
import platform.UniformTypeIdentifiers.UTTypePDF
import platform.UniformTypeIdentifiers.UTTypePlainText
import platform.UniformTypeIdentifiers.UTTypeRTF
import platform.UniformTypeIdentifiers.UTTypeSpreadsheet
import platform.darwin.NSObject
import platform.darwin.dispatch_async
import platform.darwin.dispatch_get_main_queue

private const val MAX_FILE_PICK_ITEMS = MAX_PHOTOS_PER_UPLOAD_REQUEST

private object ActiveDocumentFilePicker {
    var delegate: DocumentFilePickerDelegate? = null
}

@Composable
actual fun rememberDocumentFilePicker(
    onResult: (List<PickedLocalFile>) -> Unit,
): (maxItems: Int) -> Unit {
    val callback = remember(onResult) { onResult }

    return remember {
        { maxItems: Int ->
            val limit = maxItems.coerceAtMost(MAX_FILE_PICK_ITEMS)
            if (limit <= 0) return@remember
            val root = findDocumentPickerRoot() ?: return@remember
            val types = documentPickerContentTypes()
            val picker = UIDocumentPickerViewController(
                forOpeningContentTypes = types,
                asCopy = true,
            )
            picker.allowsMultipleSelection = limit > 1
            val delegate = DocumentFilePickerDelegate(callback, limit)
            ActiveDocumentFilePicker.delegate = delegate
            picker.delegate = delegate
            root.presentViewController(picker, animated = true, completion = null)
        }
    }
}

private fun documentPickerContentTypes(): List<UTType> {
    val extraIds = listOf(
        "public.heic",
        "public.heif",
        "com.microsoft.word.doc",
        "org.openxmlformats.wordprocessingml.document",
        "com.microsoft.excel.xls",
        "org.openxmlformats.spreadsheetml.sheet",
        "public.comma-separated-values-text",
    )
    return buildList {
        add(UTTypeImage)
        add(UTTypePDF)
        add(UTTypePlainText)
        add(UTTypeRTF)
        add(UTTypeSpreadsheet)
        extraIds.mapNotNullTo(this) { id ->
            UTType.typeWithIdentifier(id)
        }
    }
}

private fun findDocumentPickerRoot(): UIViewController? {
    val application = UIApplication.sharedApplication
    val keyWindow = application.connectedScenes
        .flatMap { scene ->
            val windowScene = scene as? UIWindowScene ?: return@flatMap emptyList<UIWindow>()
            windowScene.windows.mapNotNull { it as? UIWindow }
        }
        .firstOrNull { it.isKeyWindow() }
        ?: application.windows
            .mapNotNull { it as? UIWindow }
            .firstOrNull { it.isKeyWindow() }
        ?: application.keyWindow
    var controller = keyWindow?.rootViewController
    while (controller?.presentedViewController != null) {
        controller = controller.presentedViewController
    }
    return controller
}

private class DocumentFilePickerDelegate(
    private val onResult: (List<PickedLocalFile>) -> Unit,
    private val maxItems: Int,
) : NSObject(), UIDocumentPickerDelegateProtocol {

    override fun documentPicker(
        controller: UIDocumentPickerViewController,
        didPickDocumentsAtURLs: List<*>,
    ) {
        val urls = didPickDocumentsAtURLs.filterIsInstance<NSURL>().take(maxItems.coerceAtLeast(0))
        val files = urls.mapNotNull { url ->
            val data = NSData.dataWithContentsOfURL(url) ?: return@mapNotNull null
            PickedLocalFile(
                bytes = data.toByteArray(),
                fileName = url.lastPathComponent,
            )
        }
        deliver(files)
    }

    override fun documentPickerWasCancelled(controller: UIDocumentPickerViewController) {
        deliver(emptyList())
    }

    private fun deliver(files: List<PickedLocalFile>) {
        dispatch_async(dispatch_get_main_queue()) {
            onResult(files)
            ActiveDocumentFilePicker.delegate = null
        }
    }
}

@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
private fun NSData.toByteArray(): ByteArray {
    val length = this.length.toInt()
    if (length <= 0) return ByteArray(0)
    val bytes = ByteArray(length)
    bytes.usePinned { pinned ->
        platform.posix.memcpy(pinned.addressOf(0), this.bytes, this.length)
    }
    return bytes
}
