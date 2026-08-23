package com.tagaev.trrcrm.ui.permissions

import com.tagaev.trrcrm.domain.sanitizedOpenFileName
import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import platform.Foundation.NSData
import platform.Foundation.NSTemporaryDirectory
import platform.Foundation.NSURL
import platform.Foundation.create
import platform.Foundation.writeToURL
import platform.UIKit.UIApplication
import platform.UIKit.UIDocumentInteractionController
import platform.UIKit.UIDocumentInteractionControllerDelegateProtocol
import platform.UIKit.UIViewController
import platform.UIKit.UIWindow
import platform.UIKit.UIWindowScene
import platform.darwin.NSObject

private object ActiveExternalDocumentOpener {
    var controller: UIDocumentInteractionController? = null
    var delegate: ExternalDocumentOpenDelegate? = null
}

@OptIn(ExperimentalForeignApi::class)
actual fun openExternalDocument(fileName: String, mimeType: String, bytes: ByteArray): Boolean {
    return runCatching {
        val safeName = sanitizedOpenFileName(fileName, mimeType)
        val url = writeTempFile(safeName, bytes) ?: return false
        val controller = UIDocumentInteractionController.interactionControllerWithURL(url)
        val delegate = ExternalDocumentOpenDelegate()
        ActiveExternalDocumentOpener.controller = controller
        ActiveExternalDocumentOpener.delegate = delegate
        controller.delegate = delegate
        val previewed = controller.presentPreviewAnimated(true)
        if (previewed) return true
        val root = findDocumentOpenerRoot()
        val view = root?.view
        if (root == null || view == null) {
            CameraFixatorLog.d("open_external_document_failed name=$fileName mime=$mimeType message=no_root")
            ActiveExternalDocumentOpener.controller = null
            ActiveExternalDocumentOpener.delegate = null
            return false
        }
        controller.presentOpenInMenuFromRect(view.bounds, inView = view, animated = true)
        true
    }.onFailure { error ->
        CameraFixatorLog.d("open_external_document_failed name=$fileName mime=$mimeType message=${error.message}")
        ActiveExternalDocumentOpener.controller = null
        ActiveExternalDocumentOpener.delegate = null
    }.getOrDefault(false)
}

@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
private fun writeTempFile(fileName: String, bytes: ByteArray): NSURL? {
    val path = NSTemporaryDirectory() + fileName
    val url = NSURL.fileURLWithPath(path)
    val data = bytes.toNSData()
    val written = data.writeToURL(url, atomically = true)
    return url.takeIf { written }
}

@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
private fun ByteArray.toNSData(): NSData {
    if (isEmpty()) return NSData()
    return usePinned { pinned ->
        NSData.create(bytes = pinned.addressOf(0), length = size.toULong())
    }
}

private fun findDocumentOpenerRoot(): UIViewController? {
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

private class ExternalDocumentOpenDelegate :
    NSObject(),
    UIDocumentInteractionControllerDelegateProtocol {

    override fun documentInteractionControllerViewControllerForPreview(
        controller: UIDocumentInteractionController,
    ): UIViewController {
        return findDocumentOpenerRoot() ?: UIViewController()
    }

    override fun documentInteractionControllerDidEndPreview(controller: UIDocumentInteractionController) {
        ActiveExternalDocumentOpener.controller = null
        ActiveExternalDocumentOpener.delegate = null
    }
}
