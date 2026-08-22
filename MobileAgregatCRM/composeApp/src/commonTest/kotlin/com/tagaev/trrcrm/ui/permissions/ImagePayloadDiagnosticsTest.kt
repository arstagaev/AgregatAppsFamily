package com.tagaev.trrcrm.ui.permissions

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ImagePayloadDiagnosticsTest {

    @Test
    fun emptyBody() {
        val inspection = inspectImagePayload(byteArrayOf())
        assertEquals("empty", inspection.sniff)
        assertEquals("empty_body", inspection.structuralReason)
        assertEquals(0, inspection.sizeBytes)
    }

    @Test
    fun truncatedJpegMissingEoi() {
        val bytes = byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte(), 0xE0.toByte(), 0x00, 0x10)
        val inspection = inspectImagePayload(bytes)
        assertEquals("jpeg", inspection.sniff)
        assertEquals(false, inspection.jpegHasEoi)
        assertEquals("truncated_jpeg_missing_eoi", inspection.structuralReason)
    }

    @Test
    fun completeJpegHasNoStructuralReason() {
        val bytes = byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte(), 0xD9.toByte())
        val inspection = inspectImagePayload(bytes)
        assertEquals("jpeg", inspection.sniff)
        assertEquals(true, inspection.jpegHasEoi)
        assertNull(inspection.structuralReason)
    }

    @Test
    fun truncatedPngMissingIend() {
        val bytes = pngSignature() + pngIhdr(width = 1, height = 1)
        val inspection = inspectImagePayload(bytes)
        assertEquals("png", inspection.sniff)
        assertEquals(1, inspection.pngWidth)
        assertEquals(1, inspection.pngHeight)
        assertEquals(false, inspection.pngHasIend)
        assertEquals("truncated_png_missing_iend", inspection.structuralReason)
    }

    @Test
    fun htmlPayload() {
        val inspection = inspectImagePayload("<html>not an image".encodeToByteArray())
        assertEquals("html", inspection.sniff)
        assertEquals("not_image_html_or_json", inspection.structuralReason)
    }

    @Test
    fun jsonPayload() {
        val inspection = inspectImagePayload("""{"detail":"error"}""".encodeToByteArray())
        assertEquals("json", inspection.sniff)
        assertEquals("not_image_html_or_json", inspection.structuralReason)
    }

    @Test
    fun decodeReasonAfterFailedPngHeader() {
        val completeHeader = pngSignature() + pngIhdr(width = 8, height = 8) + pngIend()
        val inspection = inspectImagePayload(completeHeader)
        assertEquals("png", inspection.sniff)
        assertEquals(true, inspection.pngHasIend)
        assertNull(inspection.structuralReason)
        assertEquals("png_header_ok_but_decoder_failed", decodeReasonAfterFailedDecoder(inspection))
    }

    private fun pngSignature(): ByteArray = byteArrayOf(
        0x89.toByte(), 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A,
    )

    private fun pngIhdr(width: Int, height: Int): ByteArray {
        val chunk = ByteArray(4 + 4 + 13 + 4)
        chunk[3] = 13
        chunk[4] = 0x49
        chunk[5] = 0x48
        chunk[6] = 0x44
        chunk[7] = 0x52
        writeIntBe(chunk, 8, width)
        writeIntBe(chunk, 12, height)
        chunk[16] = 8
        chunk[17] = 2
        return chunk
    }

    private fun pngIend(): ByteArray = byteArrayOf(
        0x00, 0x00, 0x00, 0x00,
        0x49, 0x45, 0x4E, 0x44,
        0x00, 0x00, 0x00, 0x00,
    )

    private fun writeIntBe(target: ByteArray, offset: Int, value: Int) {
        target[offset] = ((value ushr 24) and 0xFF).toByte()
        target[offset + 1] = ((value ushr 16) and 0xFF).toByte()
        target[offset + 2] = ((value ushr 8) and 0xFF).toByte()
        target[offset + 3] = (value and 0xFF).toByte()
    }
}
