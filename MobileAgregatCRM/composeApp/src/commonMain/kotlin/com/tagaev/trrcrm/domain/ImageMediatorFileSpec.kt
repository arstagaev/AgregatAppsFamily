package com.tagaev.trrcrm.domain

enum class ImageMediatorFileKind {
    Image,
    Document,
}

data class ImageMediatorUploadPart(
    val bytes: ByteArray,
    val fileName: String,
    val mimeType: String,
    val kind: ImageMediatorFileKind,
)

object ImageMediatorFileLimits {
    const val MAX_FILES_PER_REQUEST = 10
    const val MAX_IMAGE_BYTES = 25L * 1024 * 1024
    const val MAX_DOCUMENT_BYTES = 15L * 1024 * 1024
    const val MAX_REQUEST_BYTES = 150L * 1024 * 1024
}

enum class ImageMediatorFileRejectReason {
    UnsupportedType,
    ImageTooLarge,
    DocumentTooLarge,
    RequestTooLarge,
    TooManyFiles,
}

sealed class ImageMediatorFilePrepareResult {
    data class Ok(val part: ImageMediatorUploadPart) : ImageMediatorFilePrepareResult()
    data class Rejected(val reason: ImageMediatorFileRejectReason) : ImageMediatorFilePrepareResult()
}

fun maxBytesForKind(kind: ImageMediatorFileKind): Long = when (kind) {
    ImageMediatorFileKind.Image -> ImageMediatorFileLimits.MAX_IMAGE_BYTES
    ImageMediatorFileKind.Document -> ImageMediatorFileLimits.MAX_DOCUMENT_BYTES
}

fun kindFromMime(mimeType: String): ImageMediatorFileKind =
    if (mimeType.startsWith("image/")) ImageMediatorFileKind.Image else ImageMediatorFileKind.Document

fun jpegUploadPart(bytes: ByteArray, index: Int = 0): ImageMediatorUploadPart =
    ImageMediatorUploadPart(
        bytes = bytes,
        fileName = "photo_$index.jpg",
        mimeType = "image/jpeg",
        kind = ImageMediatorFileKind.Image,
    )

fun pendingStoredFileName(id: String, mimeType: String, originalName: String? = null): String {
    val ext = extensionFromFileName(originalName) ?: extensionForMime(mimeType)
    return "$id.$ext"
}

fun extensionForMime(mimeType: String): String = when (mimeType.lowercase()) {
    "image/jpeg", "image/jpg" -> "jpg"
    "image/png" -> "png"
    "image/webp" -> "webp"
    "image/heic" -> "heic"
    "image/heif" -> "heif"
    "application/pdf" -> "pdf"
    "application/msword" -> "doc"
    "application/vnd.openxmlformats-officedocument.wordprocessingml.document" -> "docx"
    "application/vnd.ms-excel" -> "xls"
    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet" -> "xlsx"
    "text/plain" -> "txt"
    "application/rtf", "text/rtf" -> "rtf"
    "text/csv", "text/comma-separated-values" -> "csv"
    else -> "bin"
}

fun extensionFromFileName(name: String?): String? {
    val trimmed = name?.substringAfterLast('/')?.substringAfterLast('\\')?.trim().orEmpty()
    if (trimmed.isEmpty() || '.' !in trimmed) return null
    val ext = trimmed.substringAfterLast('.').lowercase()
    return ext.takeIf { it in ALLOWED_EXTENSIONS }
}

fun sniffedFileExtension(bytes: ByteArray): String {
    val sniff = sniffImageMediatorFile(bytes, originalName = null) ?: return "bin"
    return extensionForMime(sniff.mimeType)
}

fun mimeTypeForOpen(bytes: ByteArray, originalName: String? = null): String =
    sniffImageMediatorFile(bytes, originalName)?.mimeType ?: "application/octet-stream"

fun sanitizedOpenFileName(fileName: String, mimeType: String): String {
    val raw = fileName.substringAfterLast('/').substringAfterLast('\\').trim()
    val ext = extensionFromFileName(raw) ?: extensionForMime(mimeType)
    val stem = raw.substringBeforeLast('.', raw)
        .replace("..", "_")
        .filter { it.isLetterOrDigit() || it == '_' || it == '-' || it == '.' || it == ' ' }
        .ifBlank { "file" }
        .take(80)
    return "$stem.$ext"
}

fun preferredStoredFileName(originalFilename: String?, storedFilename: String?): String? =
    originalFilename?.trim()?.takeIf { it.isNotEmpty() }
        ?: storedFilename?.trim()?.takeIf { it.isNotEmpty() }

fun displayFileStem(rawName: String?, fallbackId: String): String {
    val name = rawName
        ?.substringAfterLast('/')
        ?.substringAfterLast('\\')
        ?.trim()
        .orEmpty()
    if (name.isBlank()) return fallbackId
    val stem = name.substringBeforeLast('.', name).trim()
    return stem.ifBlank { name }
}

fun classifyImageMediatorFile(
    bytes: ByteArray,
    originalName: String? = null,
    fallbackIndex: Int = 0,
): ImageMediatorFilePrepareResult {
    if (bytes.isEmpty()) {
        return ImageMediatorFilePrepareResult.Rejected(ImageMediatorFileRejectReason.UnsupportedType)
    }
    val classified = sniffImageMediatorFile(bytes, originalName)
        ?: return ImageMediatorFilePrepareResult.Rejected(ImageMediatorFileRejectReason.UnsupportedType)
    val limit = maxBytesForKind(classified.kind)
    if (bytes.size.toLong() > limit) {
        return ImageMediatorFilePrepareResult.Rejected(
            if (classified.kind == ImageMediatorFileKind.Image) {
                ImageMediatorFileRejectReason.ImageTooLarge
            } else {
                ImageMediatorFileRejectReason.DocumentTooLarge
            },
        )
    }
    val fileName = sanitizedDisplayName(originalName, classified.mimeType, fallbackIndex)
    return ImageMediatorFilePrepareResult.Ok(
        ImageMediatorUploadPart(
            bytes = bytes,
            fileName = fileName,
            mimeType = classified.mimeType,
            kind = classified.kind,
        ),
    )
}

fun validateUploadBatch(parts: List<ImageMediatorUploadPart>): ImageMediatorFileRejectReason? {
    if (parts.size > ImageMediatorFileLimits.MAX_FILES_PER_REQUEST) {
        return ImageMediatorFileRejectReason.TooManyFiles
    }
    val total = parts.sumOf { it.bytes.size.toLong() }
    if (total > ImageMediatorFileLimits.MAX_REQUEST_BYTES) {
        return ImageMediatorFileRejectReason.RequestTooLarge
    }
    parts.forEach { part ->
        if (part.bytes.size.toLong() > maxBytesForKind(part.kind)) {
            return if (part.kind == ImageMediatorFileKind.Image) {
                ImageMediatorFileRejectReason.ImageTooLarge
            } else {
                ImageMediatorFileRejectReason.DocumentTooLarge
            }
        }
    }
    return null
}

private data class SniffedFile(val mimeType: String, val kind: ImageMediatorFileKind)

private fun sniffImageMediatorFile(bytes: ByteArray, originalName: String?): SniffedFile? {
    val ext = extensionFromFileName(originalName)
    when {
        isJpeg(bytes) -> return SniffedFile("image/jpeg", ImageMediatorFileKind.Image)
        isPng(bytes) -> return SniffedFile("image/png", ImageMediatorFileKind.Image)
        isWebp(bytes) -> return SniffedFile("image/webp", ImageMediatorFileKind.Image)
        isHeif(bytes) -> {
            val mime = if (ext == "heif") "image/heif" else "image/heic"
            return SniffedFile(mime, ImageMediatorFileKind.Image)
        }
        isPdf(bytes) -> return SniffedFile("application/pdf", ImageMediatorFileKind.Document)
        isRtf(bytes) -> return SniffedFile("application/rtf", ImageMediatorFileKind.Document)
        isOle(bytes) -> {
            return when (ext) {
                "xls" -> SniffedFile(MIME_XLS, ImageMediatorFileKind.Document)
                else -> SniffedFile(MIME_DOC, ImageMediatorFileKind.Document)
            }
        }
        isZip(bytes) -> {
            return when (ext) {
                "xlsx" -> SniffedFile(MIME_XLSX, ImageMediatorFileKind.Document)
                "docx" -> SniffedFile(MIME_DOCX, ImageMediatorFileKind.Document)
                else -> null
            }
        }
    }
    return when (ext) {
        "jpg", "jpeg" -> SniffedFile("image/jpeg", ImageMediatorFileKind.Image)
        "png" -> SniffedFile("image/png", ImageMediatorFileKind.Image)
        "webp" -> SniffedFile("image/webp", ImageMediatorFileKind.Image)
        "heic" -> SniffedFile("image/heic", ImageMediatorFileKind.Image)
        "heif" -> SniffedFile("image/heif", ImageMediatorFileKind.Image)
        "pdf" -> SniffedFile("application/pdf", ImageMediatorFileKind.Document)
        "doc" -> SniffedFile(MIME_DOC, ImageMediatorFileKind.Document)
        "docx" -> SniffedFile(MIME_DOCX, ImageMediatorFileKind.Document)
        "xls" -> SniffedFile(MIME_XLS, ImageMediatorFileKind.Document)
        "xlsx" -> SniffedFile(MIME_XLSX, ImageMediatorFileKind.Document)
        "txt" -> SniffedFile("text/plain", ImageMediatorFileKind.Document)
        "rtf" -> SniffedFile("application/rtf", ImageMediatorFileKind.Document)
        "csv" -> SniffedFile("text/csv", ImageMediatorFileKind.Document)
        else -> null
    }
}

private fun sanitizedDisplayName(originalName: String?, mimeType: String, fallbackIndex: Int): String {
    val base = originalName
        ?.substringAfterLast('/')
        ?.substringAfterLast('\\')
        ?.trim()
        .orEmpty()
    if (base.isNotEmpty() && ".." !in base && '/' !in base && '\\' !in base) {
        val ext = extensionFromFileName(base) ?: extensionForMime(mimeType)
        val stem = base.substringBeforeLast('.', base).ifBlank { "file_$fallbackIndex" }
        return "$stem.$ext"
    }
    return "file_$fallbackIndex.${extensionForMime(mimeType)}"
}

private fun isJpeg(bytes: ByteArray): Boolean =
    bytes.size >= 3 &&
        bytes[0] == 0xFF.toByte() &&
        bytes[1] == 0xD8.toByte() &&
        bytes[2] == 0xFF.toByte()

private fun isPng(bytes: ByteArray): Boolean = bytes.startsWith(PNG_SIGNATURE)

private fun isWebp(bytes: ByteArray): Boolean =
    bytes.size >= 12 &&
        bytes.startsWith(RIFF) &&
        bytes.copyOfRange(8, 12).contentEquals(WEBP)

private fun isHeif(bytes: ByteArray): Boolean {
    if (bytes.size < 12) return false
    if (bytes[4] != 'f'.code.toByte() ||
        bytes[5] != 't'.code.toByte() ||
        bytes[6] != 'y'.code.toByte() ||
        bytes[7] != 'p'.code.toByte()
    ) {
        return false
    }
    val brand = bytes.copyOfRange(8, 12).decodeToString().lowercase()
    return brand in HEIF_BRANDS
}

private fun isPdf(bytes: ByteArray): Boolean = bytes.startsWith(PDF_SIGNATURE)

private fun isRtf(bytes: ByteArray): Boolean = bytes.startsWith(RTF_SIGNATURE)

private fun isOle(bytes: ByteArray): Boolean = bytes.startsWith(OLE_SIGNATURE)

private fun isZip(bytes: ByteArray): Boolean =
    bytes.size >= 4 &&
        bytes[0] == 0x50.toByte() &&
        bytes[1] == 0x4B.toByte() &&
        (bytes[2] == 0x03.toByte() || bytes[2] == 0x05.toByte() || bytes[2] == 0x07.toByte())

private fun ByteArray.startsWith(prefix: ByteArray): Boolean {
    if (size < prefix.size) return false
    for (i in prefix.indices) {
        if (this[i] != prefix[i]) return false
    }
    return true
}

private val ALLOWED_EXTENSIONS = setOf(
    "jpg", "jpeg", "png", "webp", "heic", "heif",
    "pdf", "doc", "docx", "xls", "xlsx", "txt", "rtf", "csv",
)

private const val MIME_DOC = "application/msword"
private const val MIME_DOCX = "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
private const val MIME_XLS = "application/vnd.ms-excel"
private const val MIME_XLSX = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"

private val PNG_SIGNATURE = byteArrayOf(
    0x89.toByte(), 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A,
)
private val RIFF = byteArrayOf(0x52, 0x49, 0x46, 0x46)
private val WEBP = byteArrayOf(0x57, 0x45, 0x42, 0x50)
private val PDF_SIGNATURE = byteArrayOf(0x25, 0x50, 0x44, 0x46)
private val RTF_SIGNATURE = byteArrayOf(0x7B, 0x5C, 0x72, 0x74, 0x66)
private val OLE_SIGNATURE = byteArrayOf(
    0xD0.toByte(), 0xCF.toByte(), 0x11, 0xE0.toByte(),
    0xA1.toByte(), 0xB1.toByte(), 0x1A, 0xE1.toByte(),
)
private val HEIF_BRANDS = setOf(
    "heic", "heix", "hevc", "hevx", "heim", "heis", "mif1", "msf1", "heif",
)
