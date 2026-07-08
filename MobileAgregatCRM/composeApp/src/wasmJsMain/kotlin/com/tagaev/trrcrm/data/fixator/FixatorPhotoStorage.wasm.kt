package com.tagaev.trrcrm.data.fixator

import okio.Path
import okio.Path.Companion.toPath

actual fun fixatorAppStorageFolderName(): String = "TRR APP"

private class NoOpPublicGallerySaver : PublicGallerySaver {
    override suspend fun save(normalizedBytes: ByteArray, displayName: String): GallerySaveResult =
        GallerySaveResult.Unavailable
}

actual fun appFixatorStorageRoot(): Path = "/tmp/trrcrm-fixator".toPath()

actual fun createFixatorPhotoStorage(): FixatorPhotoStorage =
    FixatorPhotoStorage(
        storageRoot = appFixatorStorageRoot(),
        publicGallerySaver = NoOpPublicGallerySaver(),
    )

actual fun createDocumentPhotoCache(): DocumentPhotoCache =
    DocumentPhotoCache(storageRoot = appFixatorStorageRoot())
