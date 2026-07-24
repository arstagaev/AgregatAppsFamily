package com.tagaev.trrcrm.data.fixator

import kotlinx.coroutines.test.runTest
import okio.FileSystem
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class FixatorPendingManifestStoreTest {

    private val fileSystem = FileSystem.SYSTEM
    private val store = FixatorPendingManifestStore(fileSystem = fileSystem)

    @Test
    fun saveListReadAndDeletePendingPhoto() = runTest {
        val root = createTempRoot()
        try {
            val bytes = byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte(), 0x00)
            val entry = store.savePendingPhoto(
                root = root,
                documentNumber = "123456",
                documentName = "Комплектация",
                normalizedBytes = bytes,
            )
            val listed = store.listPendingPhotos(root, "123456")
            assertEquals(1, listed.size)
            assertEquals(entry.id, listed.first().id)

            val readBack = store.readPendingPhotoBytes(root, "123456", entry)
            assertContentEquals(bytes, readBack)

            store.deletePendingPhoto(root, "123456", entry)
            assertTrue(store.listPendingPhotos(root, "123456").isEmpty())
        } finally {
            deleteRecursively(root)
        }
    }

    @Test
    fun deletePendingPhotos_removesAllEntries() = runTest {
        val root = createTempRoot()
        try {
            val first = store.savePendingPhoto(root, "654321", "Комплектация", byteArrayOf(1))
            val second = store.savePendingPhoto(root, "654321", "Комплектация", byteArrayOf(2))
            assertEquals(2, store.listPendingPhotos(root, "654321").size)

            store.deletePendingPhotos(root, "654321", listOf(first, second))
            assertTrue(store.listPendingPhotos(root, "654321").isEmpty())
        } finally {
            deleteRecursively(root)
        }
    }

    private fun createTempRoot(): okio.Path {
        val root = FileSystem.SYSTEM_TEMPORARY_DIRECTORY /
            "fixator-store-test-${Random.nextLong()}"
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
