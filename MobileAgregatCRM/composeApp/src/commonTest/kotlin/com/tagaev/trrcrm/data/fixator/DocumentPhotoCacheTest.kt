package com.tagaev.trrcrm.data.fixator

import kotlinx.coroutines.test.runTest
import okio.FileSystem
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class DocumentPhotoCacheTest {

    private val fileSystem = FileSystem.SYSTEM

    @Test
    fun writeAndRead_returnsCachedBytes() = runTest {
        val root = createTempRoot()
        val cache = DocumentPhotoCache(storageRoot = root, fileSystem = fileSystem)
        val key = DocumentPhotoCacheKey(
            documentType = "complectation",
            documentNumber = "0000549041",
            imageId = "img_a83fd912e2c4f719",
        )
        val bytes = byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte(), 0xE0.toByte())
        try {
            cache.writeBytes(key, bytes)
            assertContentEquals(bytes, cache.readBytes(key))
        } finally {
            deleteRecursively(root)
        }
    }

    @Test
    fun differentDocumentTypes_doNotOverlap() = runTest {
        val root = createTempRoot()
        val cache = DocumentPhotoCache(storageRoot = root, fileSystem = fileSystem)
        val imageId = "img_shared"
        val complectationKey = DocumentPhotoCacheKey("complectation", "0000000001", imageId)
        val workOrderKey = DocumentPhotoCacheKey("work_order", "0000000001", imageId)
        try {
            cache.writeBytes(complectationKey, byteArrayOf(1))
            cache.writeBytes(workOrderKey, byteArrayOf(2))
            assertContentEquals(byteArrayOf(1), cache.readBytes(complectationKey))
            assertContentEquals(byteArrayOf(2), cache.readBytes(workOrderKey))
        } finally {
            deleteRecursively(root)
        }
    }

    @Test
    fun differentDocumentNumbers_doNotOverlap() = runTest {
        val root = createTempRoot()
        val cache = DocumentPhotoCache(storageRoot = root, fileSystem = fileSystem)
        val imageId = "img_shared"
        val firstKey = DocumentPhotoCacheKey("complectation", "0000000001", imageId)
        val secondKey = DocumentPhotoCacheKey("complectation", "0000000002", imageId)
        try {
            cache.writeBytes(firstKey, byteArrayOf(10))
            cache.writeBytes(secondKey, byteArrayOf(20))
            assertContentEquals(byteArrayOf(10), cache.readBytes(firstKey))
            assertContentEquals(byteArrayOf(20), cache.readBytes(secondKey))
        } finally {
            deleteRecursively(root)
        }
    }

    @Test
    fun clearDocument_removesOnlyTargetDocumentFiles() = runTest {
        val root = createTempRoot()
        val cache = DocumentPhotoCache(storageRoot = root, fileSystem = fileSystem)
        val targetKey = DocumentPhotoCacheKey("complectation", "0000549041", "img_target")
        val otherNumberKey = DocumentPhotoCacheKey("complectation", "0000549042", "img_other_number")
        val otherTypeKey = DocumentPhotoCacheKey("work_order", "0000549041", "img_other_type")
        try {
            cache.writeBytes(targetKey, byteArrayOf(1))
            cache.writeBytes(otherNumberKey, byteArrayOf(2))
            cache.writeBytes(otherTypeKey, byteArrayOf(3))
            val stats = cache.clearDocument("complectation", "0000549041")
            assertEquals(1, stats.deletedFiles)
            assertEquals(1L, stats.freedBytes)
            assertNull(cache.readBytes(targetKey))
            assertContentEquals(byteArrayOf(2), cache.readBytes(otherNumberKey))
            assertContentEquals(byteArrayOf(3), cache.readBytes(otherTypeKey))
        } finally {
            deleteRecursively(root)
        }
    }

    @Test
    fun clearAll_removesViewerCacheFiles() = runTest {
        val root = createTempRoot()
        val cache = DocumentPhotoCache(storageRoot = root, fileSystem = fileSystem)
        val key = DocumentPhotoCacheKey("complectation", "0000549041", "img_clear_test")
        val bytes = byteArrayOf(0x01, 0x02, 0x03)
        try {
            cache.writeBytes(key, bytes)
            val stats = cache.clearAll()
            assertEquals(1, stats.deletedFiles)
            assertEquals(bytes.size.toLong(), stats.freedBytes)
            assertNull(cache.readBytes(key))
            assertTrue(!fileSystem.exists(root / "fixator" / "viewer-cache"))
        } finally {
            deleteRecursively(root)
        }
    }

    private fun createTempRoot(): okio.Path {
        val root = FileSystem.SYSTEM_TEMPORARY_DIRECTORY /
            "document-photo-cache-test-${Random.nextLong()}"
        fileSystem.createDirectories(root)
        return root
    }

    private fun deleteRecursively(root: okio.Path) {
        if (!fileSystem.exists(root)) return
        fileSystem.list(root).forEach { child ->
            if (fileSystem.metadata(child).isDirectory) {
                deleteRecursively(child)
            } else {
                fileSystem.delete(child)
            }
        }
        fileSystem.delete(root)
    }
}
