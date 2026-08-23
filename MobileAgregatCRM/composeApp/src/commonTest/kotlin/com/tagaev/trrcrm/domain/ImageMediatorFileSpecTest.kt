package com.tagaev.trrcrm.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ImageMediatorFileSpecTest {

    @Test
    fun classify_jpegByMagic() {
        val bytes = byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte(), 0xE0.toByte())
        val result = classifyImageMediatorFile(bytes, originalName = null)
        val ok = assertIs<ImageMediatorFilePrepareResult.Ok>(result)
        assertEquals("image/jpeg", ok.part.mimeType)
        assertEquals(ImageMediatorFileKind.Image, ok.part.kind)
        assertTrue(ok.part.fileName.endsWith(".jpg"))
    }

    @Test
    fun classify_pdfKeepsFilenameAndMime() {
        val bytes = "%PDF-1.4 fake".encodeToByteArray()
        val result = classifyImageMediatorFile(bytes, originalName = "Акт.pdf")
        val ok = assertIs<ImageMediatorFilePrepareResult.Ok>(result)
        assertEquals("application/pdf", ok.part.mimeType)
        assertEquals(ImageMediatorFileKind.Document, ok.part.kind)
        assertEquals("Акт.pdf", ok.part.fileName)
    }

    @Test
    fun classify_txtByExtension() {
        val result = classifyImageMediatorFile("hello".encodeToByteArray(), originalName = "notes.txt")
        val ok = assertIs<ImageMediatorFilePrepareResult.Ok>(result)
        assertEquals("text/plain", ok.part.mimeType)
        assertEquals("notes.txt", ok.part.fileName)
    }

    @Test
    fun classify_rejectsUnsupported() {
        val result = classifyImageMediatorFile(byteArrayOf(1, 2, 3), originalName = "virus.exe")
        val rejected = assertIs<ImageMediatorFilePrepareResult.Rejected>(result)
        assertEquals(ImageMediatorFileRejectReason.UnsupportedType, rejected.reason)
    }

    @Test
    fun classify_rejectsImageOver25Mb() {
        val bytes = ByteArray((25 * 1024 * 1024) + 4)
        bytes[0] = 0xFF.toByte()
        bytes[1] = 0xD8.toByte()
        bytes[2] = 0xFF.toByte()
        val result = classifyImageMediatorFile(bytes, "big.jpg")
        val rejected = assertIs<ImageMediatorFilePrepareResult.Rejected>(result)
        assertEquals(ImageMediatorFileRejectReason.ImageTooLarge, rejected.reason)
    }

    @Test
    fun classify_rejectsDocumentOver15Mb() {
        val prefix = "%PDF-1.4".encodeToByteArray()
        val bytes = ByteArray((15 * 1024 * 1024) + prefix.size)
        prefix.copyInto(bytes)
        val result = classifyImageMediatorFile(bytes, "big.pdf")
        val rejected = assertIs<ImageMediatorFilePrepareResult.Rejected>(result)
        assertEquals(ImageMediatorFileRejectReason.DocumentTooLarge, rejected.reason)
    }

    @Test
    fun validateUploadBatch_rejectsMoreThan10Files() {
        val part = jpegUploadPart(byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte()))
        val reason = validateUploadBatch(List(11) { part })
        assertEquals(ImageMediatorFileRejectReason.TooManyFiles, reason)
    }

    @Test
    fun validateUploadBatch_requestLimitIs150Mb() {
        assertEquals(150L * 1024 * 1024, ImageMediatorFileLimits.MAX_REQUEST_BYTES)
        assertEquals(25L * 1024 * 1024, ImageMediatorFileLimits.MAX_IMAGE_BYTES)
        assertEquals(15L * 1024 * 1024, ImageMediatorFileLimits.MAX_DOCUMENT_BYTES)
        assertEquals(10, ImageMediatorFileLimits.MAX_FILES_PER_REQUEST)
    }

    @Test
    fun validateUploadBatch_acceptsTenSmallFiles() {
        val part = jpegUploadPart(byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte()))
        assertNull(validateUploadBatch(List(10) { part }))
    }

    @Test
    fun pendingStoredFileName_usesMimeExtension() {
        assertEquals("abc.pdf", pendingStoredFileName("abc", "application/pdf", "scan.PDF"))
        assertEquals("id.jpg", pendingStoredFileName("id", "image/jpeg", null))
    }

    @Test
    fun mimeTypeForOpen_pdfAndHtmlFallback() {
        assertEquals("application/pdf", mimeTypeForOpen("%PDF-1.4".encodeToByteArray()))
        assertEquals("application/octet-stream", mimeTypeForOpen("<!DOCTYPE html>".encodeToByteArray()))
    }

    @Test
    fun sanitizedOpenFileName_stripsPathAndKeepsCyrillic() {
        assertEquals("Акт.pdf", sanitizedOpenFileName("../docs/Акт.pdf", "application/pdf"))
        assertEquals("file.xlsx", sanitizedOpenFileName("", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
    }

    @Test
    fun displayFileStem_dropsExtensionAndPrefersOriginal() {
        assertEquals("Акт выполненных работ", displayFileStem("docs/Акт выполненных работ.pdf", "img_1"))
        assertEquals("notes", displayFileStem("notes.txt", "img_1"))
        assertEquals("img_1", displayFileStem(null, "img_1"))
        assertEquals("scan.docx", preferredStoredFileName("scan.docx", "stored.bin"))
        assertEquals("stored.bin", preferredStoredFileName("  ", "stored.bin"))
        assertEquals(null, preferredStoredFileName(null, null))
    }
}
