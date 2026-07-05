package com.tagaev.trrcrm.data.fixator

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okio.FileSystem
import okio.Path
import kotlin.random.Random
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

internal class FixatorPendingManifestStore(
    private val json: Json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        explicitNulls = false
        prettyPrint = true
    },
    private val fileSystem: FileSystem = FileSystem.SYSTEM,
) {
    companion object {
        private const val MANIFEST_FILE = "manifest.json"
        private const val FIXATOR_DIR = "fixator"
        private const val PENDING_DIR = "pending"
    }

    fun pendingDir(root: Path, documentNumber: String): Path =
        root / FIXATOR_DIR / PENDING_DIR / documentNumber

    suspend fun savePendingPhoto(
        root: Path,
        documentNumber: String,
        documentName: String,
        normalizedBytes: ByteArray,
    ): FixatorPendingPhotoEntry = withContext(Dispatchers.Default) {
        val dir = pendingDir(root, documentNumber)
        fileSystem.createDirectories(dir)
        val id = generatePhotoId()
        val entry = FixatorPendingPhotoEntry(
            id = id,
            fileName = "$id.jpg",
            createdAtEpochMs = currentTimeMillis(),
        )
        val photoPath = dir / entry.fileName
        fileSystem.write(photoPath) { write(normalizedBytes) }
        val manifest = loadManifest(dir) ?: FixatorPendingManifest(
            documentNumber = documentNumber,
            documentName = documentName,
        )
        writeManifest(
            dir,
            manifest.copy(
                documentNumber = documentNumber,
                documentName = documentName,
                photos = manifest.photos + entry,
            ),
        )
        entry
    }

    suspend fun listPendingPhotos(root: Path, documentNumber: String): List<FixatorPendingPhotoEntry> =
        withContext(Dispatchers.Default) {
            val dir = pendingDir(root, documentNumber)
            if (!fileSystem.exists(dir)) return@withContext emptyList()
            val manifest = loadManifest(dir) ?: return@withContext emptyList()
            manifest.photos.filter { entry ->
                fileSystem.exists(dir / entry.fileName)
            }
        }

    suspend fun readPendingPhotoBytes(
        root: Path,
        documentNumber: String,
        entry: FixatorPendingPhotoEntry,
    ): ByteArray = withContext(Dispatchers.Default) {
        fileSystem.read(pendingDir(root, documentNumber) / entry.fileName) { readByteArray() }
    }

    suspend fun deletePendingPhoto(
        root: Path,
        documentNumber: String,
        entry: FixatorPendingPhotoEntry,
    ) = withContext(Dispatchers.Default) {
        val dir = pendingDir(root, documentNumber)
        val photoPath = dir / entry.fileName
        if (fileSystem.exists(photoPath)) {
            fileSystem.delete(photoPath)
        }
        val manifest = loadManifest(dir) ?: return@withContext
        val updatedPhotos = manifest.photos.filterNot { it.id == entry.id }
        if (updatedPhotos.isEmpty()) {
            deleteRecursivelyIfExists(dir)
        } else {
            writeManifest(dir, manifest.copy(photos = updatedPhotos))
        }
    }

    suspend fun deletePendingPhotos(
        root: Path,
        documentNumber: String,
        entries: List<FixatorPendingPhotoEntry>,
    ) = withContext(Dispatchers.Default) {
        entries.forEach { deletePendingPhoto(root, documentNumber, it) }
    }

    private fun loadManifest(dir: Path): FixatorPendingManifest? {
        val manifestPath = dir / MANIFEST_FILE
        if (!fileSystem.exists(manifestPath)) return null
        return runCatching {
            val raw = fileSystem.read(manifestPath) { readUtf8() }
            json.decodeFromString<FixatorPendingManifest>(raw)
        }.getOrNull()
    }

    private fun writeManifest(dir: Path, manifest: FixatorPendingManifest) {
        val manifestPath = dir / MANIFEST_FILE
        fileSystem.write(manifestPath) {
            writeUtf8(json.encodeToString(manifest))
        }
    }

    private fun deleteRecursivelyIfExists(dir: Path) {
        if (!fileSystem.exists(dir)) return
        fileSystem.list(dir).forEach { child ->
            if (fileSystem.metadata(child).isDirectory) {
                deleteRecursivelyIfExists(child)
            } else {
                fileSystem.delete(child)
            }
        }
        fileSystem.delete(dir)
    }

    private fun generatePhotoId(): String =
        "${currentTimeMillis()}_${Random.nextLong().toString(16)}"

    @OptIn(ExperimentalTime::class)
    private fun currentTimeMillis(): Long = Clock.System.now().toEpochMilliseconds()
}
