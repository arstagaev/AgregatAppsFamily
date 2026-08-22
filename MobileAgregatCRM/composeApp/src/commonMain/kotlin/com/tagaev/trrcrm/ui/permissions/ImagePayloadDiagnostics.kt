package com.tagaev.trrcrm.ui.permissions

import androidx.compose.ui.graphics.ImageBitmap

data class ImagePayloadInspection(
    val sizeBytes: Int,
    val hexPrefix: String,
    val sniff: String,
    val pngWidth: Int? = null,
    val pngHeight: Int? = null,
    val pngBitDepth: Int? = null,
    val pngColorType: Int? = null,
    val pngHasIend: Boolean? = null,
    val jpegHasEoi: Boolean? = null,
    val structuralReason: String? = null,
) {
    fun toLogFields(): String = buildString {
        append("actualLen=").append(sizeBytes)
        append(" sniff=").append(sniff)
        append(" hex=").append(hexPrefix)
        if (pngWidth != null && pngHeight != null) {
            append(" png=").append(pngWidth).append('x').append(pngHeight)
        }
        pngBitDepth?.let { append(" bit=").append(it) }
        pngColorType?.let { append(" color=").append(it) }
        pngHasIend?.let { append(" iend=").append(it) }
        jpegHasEoi?.let { append(" eoi=").append(it) }
        structuralReason?.let { append(" reason=").append(it) }
    }
}

fun inspectImagePayload(bytes: ByteArray): ImagePayloadInspection {
    if (bytes.isEmpty()) {
        return ImagePayloadInspection(
            sizeBytes = 0,
            hexPrefix = "",
            sniff = "empty",
            structuralReason = "empty_body",
        )
    }
    val hexPrefix = bytes.toHexPrefix(16)
    val sniff = sniffImageFormat(bytes)
    val png = if (sniff == "png") parsePng(bytes) else null
    val jpegHasEoi = if (sniff == "jpeg") bytes.containsSequence(JPEG_EOI) else null
    val structuralReason = structuralReason(
        size = bytes.size,
        sniff = sniff,
        png = png,
        jpegHasEoi = jpegHasEoi,
    )
    return ImagePayloadInspection(
        sizeBytes = bytes.size,
        hexPrefix = hexPrefix,
        sniff = sniff,
        pngWidth = png?.width,
        pngHeight = png?.height,
        pngBitDepth = png?.bitDepth,
        pngColorType = png?.colorType,
        pngHasIend = png?.hasIend,
        jpegHasEoi = jpegHasEoi,
        structuralReason = structuralReason,
    )
}

fun decodeReasonAfterFailedDecoder(inspection: ImagePayloadInspection): String =
    inspection.structuralReason ?: when (inspection.sniff) {
        "png" -> "png_header_ok_but_decoder_failed"
        "jpeg" -> "jpeg_header_ok_but_decoder_failed"
        else -> "decoder_failed"
    }

fun decodePhotoThumbnailLogged(
    bytes: ByteArray,
    imageId: String? = null,
    contentUrl: String? = null,
): ImageBitmap? {
    val inspection = inspectImagePayload(bytes)
    val idPart = imageId?.let { " id=$it" }.orEmpty()
    val urlPart = contentUrl?.let { " url=$it" }.orEmpty()
    if (inspection.structuralReason != null && inspection.sniff != "png" && inspection.sniff != "jpeg") {
        CameraFixatorLog.d("image_decode_failed$idPart$urlPart ${inspection.toLogFields()}")
        return null
    }
    val bitmap = decodePhotoThumbnail(bytes)
    if (bitmap != null) {
        CameraFixatorLog.d("image_decode_ok$idPart$urlPart ${inspection.toLogFields()}")
        return bitmap
    }
    val extraReason = if (inspection.structuralReason == null) {
        " reason=${decodeReasonAfterFailedDecoder(inspection)}"
    } else {
        ""
    }
    CameraFixatorLog.d("image_decode_failed$idPart$urlPart ${inspection.toLogFields()}$extraReason")
    return null
}

internal fun ByteArray.toHexPrefix(count: Int = 16): String =
    take(count).joinToString(separator = "") { byte ->
        (byte.toInt() and 0xFF).toString(16).padStart(2, '0')
    }

private data class PngInfo(
    val width: Int?,
    val height: Int?,
    val bitDepth: Int?,
    val colorType: Int?,
    val hasIend: Boolean,
    val hasIhdr: Boolean,
)

private fun sniffImageFormat(bytes: ByteArray): String {
    if (bytes.startsWith(PNG_SIGNATURE)) return "png"
    if (bytes.size >= 2 && bytes[0] == 0xFF.toByte() && bytes[1] == 0xD8.toByte()) return "jpeg"
    if (bytes.startsWith(GIF87A) || bytes.startsWith(GIF89A)) return "gif"
    if (bytes.size >= 12 && bytes.startsWith(RIFF) && bytes.copyOfRange(8, 12).contentEquals(WEBP)) {
        return "webp"
    }
    val trimmed = bytes.dropWhile { it == ' '.code.toByte() || it == '\n'.code.toByte() ||
        it == '\r'.code.toByte() || it == '\t'.code.toByte() }
    val first = trimmed.firstOrNull()?.toInt()?.toChar()
    if (first == '<') return "html"
    if (first == '{' || first == '[') return "json"
    return "unknown"
}

private fun parsePng(bytes: ByteArray): PngInfo {
    val hasIend = bytes.containsSequence(PNG_IEND)
    if (bytes.size < 24 || !bytes.startsWith(PNG_SIGNATURE)) {
        return PngInfo(null, null, null, null, hasIend, hasIhdr = false)
    }
    val chunkType = bytes.copyOfRange(12, 16)
    if (!chunkType.contentEquals(PNG_IHDR_TYPE)) {
        return PngInfo(null, null, null, null, hasIend, hasIhdr = false)
    }
    if (bytes.size < 26) {
        return PngInfo(null, null, null, null, hasIend, hasIhdr = false)
    }
    val width = bytes.readIntBe(16)
    val height = bytes.readIntBe(20)
    return PngInfo(
        width = width.takeIf { it > 0 },
        height = height.takeIf { it > 0 },
        bitDepth = bytes[24].toInt() and 0xFF,
        colorType = bytes[25].toInt() and 0xFF,
        hasIend = hasIend,
        hasIhdr = true,
    )
}

private fun structuralReason(
    size: Int,
    sniff: String,
    png: PngInfo?,
    jpegHasEoi: Boolean?,
): String? {
    if (sniff == "html" || sniff == "json") return "not_image_html_or_json"
    if (sniff == "png") {
        if (png?.hasIhdr != true) return "too_small_for_photo"
        if (png.hasIend != true) return "truncated_png_missing_iend"
        return null
    }
    if (sniff == "jpeg") {
        if (jpegHasEoi != true) return "truncated_jpeg_missing_eoi"
        return null
    }
    if (size < 100) return "too_small_for_photo"
    return null
}

private fun ByteArray.startsWith(prefix: ByteArray): Boolean {
    if (size < prefix.size) return false
    for (i in prefix.indices) {
        if (this[i] != prefix[i]) return false
    }
    return true
}

private fun ByteArray.containsSequence(marker: ByteArray): Boolean {
    if (marker.size > size) return false
    val last = size - marker.size
    outer@ for (i in 0..last) {
        for (j in marker.indices) {
            if (this[i + j] != marker[j]) continue@outer
        }
        return true
    }
    return false
}

private fun ByteArray.readIntBe(offset: Int): Int {
    if (size < offset + 4) return 0
    return ((this[offset].toInt() and 0xFF) shl 24) or
        ((this[offset + 1].toInt() and 0xFF) shl 16) or
        ((this[offset + 2].toInt() and 0xFF) shl 8) or
        (this[offset + 3].toInt() and 0xFF)
}

private val PNG_SIGNATURE = byteArrayOf(
    0x89.toByte(), 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A,
)
private val PNG_IHDR_TYPE = byteArrayOf(0x49, 0x48, 0x44, 0x52)
private val PNG_IEND = byteArrayOf(0x49, 0x45, 0x4E, 0x44)
private val JPEG_EOI = byteArrayOf(0xFF.toByte(), 0xD9.toByte())
private val GIF87A = byteArrayOf(0x47, 0x49, 0x46, 0x38, 0x37, 0x61)
private val GIF89A = byteArrayOf(0x47, 0x49, 0x46, 0x38, 0x39, 0x61)
private val RIFF = byteArrayOf(0x52, 0x49, 0x46, 0x46)
private val WEBP = byteArrayOf(0x57, 0x45, 0x42, 0x50)
