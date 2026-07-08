package com.tagaev.trrcrm.data.fixator

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
        private val SAFE_SEGMENT_REGEX = Regex("^[a-zA-Z0-9_-]+$")
    }

    private fun cacheRoot(): Path = storageRoot / FIXATOR_DIR / VIEWER_CACHE_DIR

    private fun cacheFilePath(key: DocumentPhotoCacheKey): Path {
        val documentType = sanitizeSegment(key.documentType)
        val documentNumber = sanitizeSegment(key.documentNumber)
        val imageId = sanitizeSegment(key.imageId)
        return cacheRoot() / documentType / documentNumber / "$imageId.jpg"
    }

    private fun sanitizeSegment(value: String): String {
        val trimmed = value.trim()
        require(trimmed.isNotEmpty()) { "Cache key segment is blank" }
        require(SAFE_SEGMENT_REGEX.matches(trimmed)) { "Unsafe cache key segment: $trimmed" }
        return trimmed
    }

    suspend fun readBytes(key: DocumentPhotoCacheKey): ByteArray? = withContext(Dispatchers.Default) {
        val path = cacheFilePath(key)
        if (!fileSystem.exists(path)) return@withContext null
        runCatching {
            fileSystem.read(path) { readByteArray() }
        }.getOrNull()
    }

    suspend fun writeBytes(key: DocumentPhotoCacheKey, bytes: ByteArray) = withContext(Dispatchers.Default) {
        val path = cacheFilePath(key)
        fileSystem.createDirectories(path.parent!!)
        fileSystem.write(path) { write(bytes) }
    }

    private fun documentCacheDir(documentType: String, documentNumber: String): Path {
        val type = sanitizeSegment(documentType)
        val number = sanitizeSegment(documentNumber)
        return cacheRoot() / type / number
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
