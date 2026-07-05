package com.tagaev.trrcrm.data.fixator

import com.tagaev.trrcrm.ui.permissions.CameraFixatorLog
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import kotlinx.coroutines.suspendCancellableCoroutine
import okio.Path
import okio.Path.Companion.toPath
import platform.Foundation.NSData
import platform.Foundation.NSApplicationSupportDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSURL
import platform.Foundation.NSUserDomainMask
import platform.Foundation.create
import platform.Photos.PHAccessLevelAddOnly
import platform.Photos.PHAssetCreationRequest
import platform.Photos.PHAuthorizationStatusAuthorized
import platform.Photos.PHAuthorizationStatusDenied
import platform.Photos.PHAuthorizationStatusLimited
import platform.Photos.PHAuthorizationStatusNotDetermined
import platform.Photos.PHAuthorizationStatusRestricted
import platform.Photos.PHPhotoLibrary
import platform.UIKit.UIImage
import kotlin.coroutines.resume

actual fun fixatorAppStorageFolderName(): String = "TRR Client"

private class IosPublicGallerySaver : PublicGallerySaver {
    @OptIn(ExperimentalForeignApi::class)
    override suspend fun save(normalizedBytes: ByteArray, displayName: String): GallerySaveResult {
        when (val auth = ensureAddOnlyAuthorization()) {
            GallerySaveResult.PermissionDenied,
            GallerySaveResult.Unavailable,
            -> return auth
            GallerySaveResult.Failed -> return GallerySaveResult.Failed
            GallerySaveResult.Success -> Unit
        }

        return suspendCancellableCoroutine { continuation ->
            val data = normalizedBytes.toNsData()
            val image = UIImage(data = data)
            if (image == null) {
                continuation.resume(GallerySaveResult.Failed)
                return@suspendCancellableCoroutine
            }
            PHPhotoLibrary.sharedPhotoLibrary().performChanges({
                PHAssetCreationRequest.creationRequestForAssetFromImage(image)
            }) { success, error ->
                if (success) {
                    continuation.resume(GallerySaveResult.Success)
                } else {
                    CameraFixatorLog.d("gallery_save_failed message=${error?.localizedDescription}")
                    continuation.resume(GallerySaveResult.Failed)
                }
            }
        }
    }

    private suspend fun ensureAddOnlyAuthorization(): GallerySaveResult {
        val current = PHPhotoLibrary.authorizationStatusForAccessLevel(PHAccessLevelAddOnly)
        return when (current) {
            PHAuthorizationStatusAuthorized,
            PHAuthorizationStatusLimited,
            -> GallerySaveResult.Success
            PHAuthorizationStatusDenied,
            PHAuthorizationStatusRestricted,
            -> GallerySaveResult.PermissionDenied
            PHAuthorizationStatusNotDetermined -> requestAddOnlyAuthorization()
            else -> GallerySaveResult.Unavailable
        }
    }

    private suspend fun requestAddOnlyAuthorization(): GallerySaveResult =
        suspendCancellableCoroutine { continuation ->
            PHPhotoLibrary.requestAuthorizationForAccessLevel(PHAccessLevelAddOnly) { status ->
                val result = when (status) {
                    PHAuthorizationStatusAuthorized,
                    PHAuthorizationStatusLimited,
                    -> GallerySaveResult.Success
                    PHAuthorizationStatusDenied,
                    PHAuthorizationStatusRestricted,
                    -> GallerySaveResult.PermissionDenied
                    else -> GallerySaveResult.Unavailable
                }
                continuation.resume(result)
            }
        }
}

@OptIn(ExperimentalForeignApi::class)
private fun ByteArray.toNsData(): NSData {
    if (isEmpty()) return NSData()
    return usePinned { pinned ->
        NSData.create(bytes = pinned.addressOf(0), length = size.toULong())
    }
}

actual fun createFixatorPhotoStorage(): FixatorPhotoStorage {
    val fileManager = NSFileManager.defaultManager
    val baseDir = fileManager.URLsForDirectory(
        directory = NSApplicationSupportDirectory,
        inDomains = NSUserDomainMask,
    ).firstOrNull() as? NSURL
    val rootPath = (baseDir?.path ?: "/tmp")
        .toPath() / fixatorAppStorageFolderName()
    return FixatorPhotoStorage(
        storageRoot = rootPath,
        publicGallerySaver = IosPublicGallerySaver(),
    )
}
