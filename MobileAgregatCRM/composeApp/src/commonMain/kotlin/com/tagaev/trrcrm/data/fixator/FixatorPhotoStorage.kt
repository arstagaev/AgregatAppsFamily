package com.tagaev.trrcrm.data.fixator

import kotlinx.serialization.json.Json
import okio.Path

expect fun fixatorAppStorageFolderName(): String

interface PublicGallerySaver {
    suspend fun save(normalizedBytes: ByteArray, displayName: String): GallerySaveResult
}

class FixatorPhotoStorage(
    private val storageRoot: Path,
    private val publicGallerySaver: PublicGallerySaver,
    private val json: Json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        explicitNulls = false
    },
) {
    private val manifestStore = FixatorPendingManifestStore(json)

    suspend fun savePendingPhoto(
        documentNumber: String,
        documentName: String,
        normalizedBytes: ByteArray,
        storedFileName: String? = null,
        mimeType: String = "image/jpeg",
    ): FixatorPendingPhotoEntry = manifestStore.savePendingPhoto(
        root = storageRoot,
        documentNumber = documentNumber,
        documentName = documentName,
        normalizedBytes = normalizedBytes,
        storedFileName = storedFileName,
        mimeType = mimeType,
    )

    suspend fun listPendingPhotos(documentNumber: String): List<FixatorPendingPhotoEntry> =
        manifestStore.listPendingPhotos(storageRoot, documentNumber)

    suspend fun readPendingPhotoBytes(
        documentNumber: String,
        entry: FixatorPendingPhotoEntry,
    ): ByteArray = manifestStore.readPendingPhotoBytes(storageRoot, documentNumber, entry)

    suspend fun deletePendingPhoto(
        documentNumber: String,
        entry: FixatorPendingPhotoEntry,
    ) = manifestStore.deletePendingPhoto(storageRoot, documentNumber, entry)

    suspend fun deletePendingPhotos(
        documentNumber: String,
        entries: List<FixatorPendingPhotoEntry>,
    ) = manifestStore.deletePendingPhotos(storageRoot, documentNumber, entries)

    suspend fun saveToPublicGallery(
        normalizedBytes: ByteArray,
        displayName: String,
    ): GallerySaveResult = publicGallerySaver.save(normalizedBytes, displayName)
}

expect fun createFixatorPhotoStorage(): FixatorPhotoStorage
