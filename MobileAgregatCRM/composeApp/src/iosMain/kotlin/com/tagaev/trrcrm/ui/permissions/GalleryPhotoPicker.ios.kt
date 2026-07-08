package com.tagaev.trrcrm.ui.permissions

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import platform.Foundation.NSData
import platform.Foundation.NSURL
import platform.Foundation.dataWithContentsOfURL
import platform.PhotosUI.PHPickerConfiguration
import platform.PhotosUI.PHPickerFilter
import platform.PhotosUI.PHPickerResult
import platform.PhotosUI.PHPickerViewController
import platform.PhotosUI.PHPickerViewControllerDelegateProtocol
import platform.UIKit.UIApplication
import platform.UIKit.UIViewController
import platform.UIKit.UIWindow
import platform.UIKit.UIWindowScene
import platform.UniformTypeIdentifiers.UTTypeImage
import platform.darwin.NSObject
import platform.darwin.dispatch_async
import platform.darwin.dispatch_get_main_queue

private const val MAX_GALLERY_PICK_ITEMS = 10

private object ActiveGalleryPicker {
    var delegate: GalleryPickerDelegate? = null
}

@Composable
actual fun rememberGalleryPhotoPicker(
    onResult: (List<ByteArray>) -> Unit,
): (maxItems: Int) -> Unit {
    val callback = remember(onResult) { onResult }

    return remember {
        { maxItems: Int ->
            val root = findTopViewController() ?: return@remember
            val limit = maxItems.coerceIn(1, MAX_GALLERY_PICK_ITEMS).toLong()
            val configuration = PHPickerConfiguration().apply {
                selectionLimit = limit
                filter = PHPickerFilter.imagesFilter
            }
            val picker = PHPickerViewController(configuration)
            val delegate = GalleryPickerDelegate(callback)
            ActiveGalleryPicker.delegate = delegate
            picker.delegate = delegate
            root.presentViewController(picker, animated = true, completion = null)
        }
    }
}

private fun findTopViewController(): UIViewController? {
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

@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
private class GalleryPickerDelegate(
    private val onResult: (List<ByteArray>) -> Unit,
) : NSObject(), PHPickerViewControllerDelegateProtocol {

    override fun picker(
        picker: PHPickerViewController,
        didFinishPicking: List<*>,
    ) {
        picker.dismissViewControllerAnimated(true, completion = null)
        val results = didFinishPicking.filterIsInstance<PHPickerResult>()
        if (results.isEmpty()) {
            deliverResult(emptyList())
            return
        }

        val images = mutableListOf<ByteArray>()
        var pending = results.size

        fun finishIfDone() {
            pending -= 1
            if (pending == 0) {
                deliverResult(images.toList())
            }
        }

        results.forEach { result ->
            val provider = result.itemProvider
            if (!provider.hasItemConformingToTypeIdentifier(UTTypeImage.identifier)) {
                finishIfDone()
                return@forEach
            }
            provider.loadFileRepresentationForTypeIdentifier(
                typeIdentifier = UTTypeImage.identifier,
                completionHandler = { url, _ ->
                    if (url != null) {
                        val data = NSData.dataWithContentsOfURL(url as NSURL)
                        if (data != null) {
                            images.add(data.toByteArray())
                        }
                    }
                    finishIfDone()
                },
            )
        }
    }

    private fun deliverResult(images: List<ByteArray>) {
        dispatch_async(dispatch_get_main_queue()) {
            onResult(images)
            ActiveGalleryPicker.delegate = null
        }
    }
}

@OptIn(ExperimentalForeignApi::class)
private fun NSData.toByteArray(): ByteArray {
    val length = this.length.toInt()
    if (length <= 0) return ByteArray(0)
    val bytes = ByteArray(length)
    bytes.usePinned { pinned ->
        platform.posix.memcpy(pinned.addressOf(0), this.bytes, this.length)
    }
    return bytes
}
