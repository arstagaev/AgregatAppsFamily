package com.tagaev.trrcrm.data.fixator

import com.tagaev.trrcrm.domain.sniffedFileExtension
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okio.FileSystem
import okio.Path

class DocumentPhotoCache(
    private val storageRoot: Path,
    private val fileSystem: FileSystem = FileSystem.SYSTEM,
) {
    companion object {
        private const val FIXATOR_DIR = "fixator"
        private const val VIEWER_CACHE_DIR = "viewer-cache"
    }

    private fun cacheRoot(): Path = storageRoot / FIXATOR_DIR / VIEWER_CACHE_DIR

    private fun documentCacheDir(documentType: String, documentNumber: String): Path {
        val type = sanitizeSegment(documentType)
        val number = sanitizeSegment(documentNumber)
        return cacheRoot() / type / number
    }

    private fun findCacheFile(dir: Path, imageId: String): Path? {
        if (!fileSystem.exists(dir)) return null
        val safeId = sanitizeSegment(imageId)
        val legacyJpg = dir / "$safeId.jpg"
        if (fileSystem.exists(legacyJpg)) return legacyJpg
        return fileSystem.list(dir).firstOrNull { path ->
            val name = path.name
            name == safeId || name.startsWith("$safeId.")
        }
    }

    private fun sanitizeSegment(value: String): String {
        val trimmed = value.trim()
        require(trimmed.isNotEmpty()) { "Cache key segment is blank" }
        require(trimmed.all { it.isLetterOrDigit() || it == '_' || it == '-' }) {
            "Unsafe cache key segment: $trimmed"
        }
        require('/' !in trimmed && '\\' !in trimmed) { "Unsafe cache key segment: $trimmed" }
        return trimmed
    }

    suspend fun readBytes(key: DocumentPhotoCacheKey): ByteArray? = withContext(Dispatchers.Default) {
        val dir = documentCacheDir(key.documentType, key.documentNumber)
        val path = findCacheFile(dir, key.imageId) ?: return@withContext null
        runCatching {
            fileSystem.read(path) { readByteArray() }
        }.getOrNull()
    }

    suspend fun writeBytes(key: DocumentPhotoCacheKey, bytes: ByteArray) = withContext(Dispatchers.Default) {
        val dir = documentCacheDir(key.documentType, key.documentNumber)
        fileSystem.createDirectories(dir)
        val safeId = sanitizeSegment(key.imageId)
        if (fileSystem.exists(dir)) {
            fileSystem.list(dir).forEach { path ->
                val name = path.name
                if (name == safeId || name.startsWith("$safeId.")) {
                    runCatching { fileSystem.delete(path) }
                }
            }
        }
        val ext = sniffedFileExtension(bytes)
        val path = dir / "$safeId.$ext"
        fileSystem.write(path) { write(bytes) }
    }

    suspend fun clearDocument(
        documentType: String,
        documentNumber: String,
    ): DocumentPhotoCacheStats = withContext(Dispatchers.Default) {
        val dir = documentCacheDir(documentType, documentNumber)
        if (!fileSystem.exists(dir)) {
            return@withContext DocumentPhotoCacheStats(deletedFiles = 0, freedBytes = 0L)
        }
        var deletedFiles = 0
        var freedBytes = 0L
        fileSystem.list(dir).forEach { path ->
            if (fileSystem.metadataOrNull(path)?.isDirectory != true) {
                freedBytes += fileSystem.metadata(path).size ?: 0L
                fileSystem.delete(path)
                deletedFiles += 1
            }
        }
        deleteEmptyDirs(cacheRoot())
        DocumentPhotoCacheStats(deletedFiles = deletedFiles, freedBytes = freedBytes)
    }

    suspend fun clearAll(): DocumentPhotoCacheStats = withContext(Dispatchers.Default) {
        val root = cacheRoot()
        if (!fileSystem.exists(root)) {
            return@withContext DocumentPhotoCacheStats(deletedFiles = 0, freedBytes = 0L)
        }
        var deletedFiles = 0
        var freedBytes = 0L
        fileSystem.listRecursively(root).forEach { path ->
            if (fileSystem.metadataOrNull(path)?.isDirectory != true) {
                freedBytes += fileSystem.metadata(path).size ?: 0L
                fileSystem.delete(path)
                deletedFiles += 1
            }
        }
        deleteEmptyDirs(root)
        DocumentPhotoCacheStats(deletedFiles = deletedFiles, freedBytes = freedBytes)
    }

    private fun deleteEmptyDirs(root: Path) {
        if (!fileSystem.exists(root)) return
        val dirs = fileSystem.listRecursively(root)
            .filter { fileSystem.metadataOrNull(it)?.isDirectory == true }
            .sortedByDescending { it.segments.size }
        dirs.forEach { dir ->
            if (dir == root) return@forEach
            val children = runCatching { fileSystem.list(dir) }.getOrNull().orEmpty()
            if (children.isEmpty()) {
                runCatching { fileSystem.delete(dir) }
            }
        }
        val rootChildren = runCatching { fileSystem.list(root) }.getOrNull().orEmpty()
        if (rootChildren.isEmpty()) {
            runCatching { fileSystem.delete(root) }
        }
    }
}

expect fun appFixatorStorageRoot(): Path

expect fun createDocumentPhotoCache(): DocumentPhotoCache
