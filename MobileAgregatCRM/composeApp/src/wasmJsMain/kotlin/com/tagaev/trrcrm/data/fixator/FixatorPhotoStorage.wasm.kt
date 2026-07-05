package com.tagaev.trrcrm.data.fixator

import okio.Path.Companion.toPath

actual fun fixatorAppStorageFolderName(): String = "TRR APP"

private class NoOpPublicGallerySaver : PublicGallerySaver {
    override suspend fun save(normalizedBytes: ByteArray, displayName: String): GallerySaveResult =
        GallerySaveResult.Unavailable
}

actual fun createFixatorPhotoStorage(): FixatorPhotoStorage =
    FixatorPhotoStorage(
        storageRoot = "/tmp/trrcrm-fixator".toPath(),
        publicGallerySaver = NoOpPublicGallerySaver(),
    )
