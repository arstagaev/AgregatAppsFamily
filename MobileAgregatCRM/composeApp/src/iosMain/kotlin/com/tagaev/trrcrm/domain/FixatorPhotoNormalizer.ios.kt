package com.tagaev.trrcrm.domain

import com.tagaev.trrcrm.ui.permissions.CameraFixatorLog
import org.jetbrains.skia.EncodedImageFormat
import org.jetbrains.skia.Image
import org.jetbrains.skia.Paint
import org.jetbrains.skia.Rect
import org.jetbrains.skia.SamplingMode
import org.jetbrains.skia.Surface

actual fun normalizeFixatorPhoto(rawBytes: ByteArray): ByteArray {
    if (rawBytes.isEmpty()) {
        throw FixatorPhotoNormalizationException("Empty photo data")
    }

    val inputSize = rawBytes.size
    val source = Image.makeFromEncoded(rawBytes)
        ?: throw FixatorPhotoNormalizationException("Unable to decode photo")

    val sourceWidth = source.width
    val sourceHeight = source.height
    if (sourceWidth <= 0 || sourceHeight <= 0) {
        throw FixatorPhotoNormalizationException("Unable to decode photo dimensions")
    }

    var maxEdge = FixatorPhotoSpec.MAX_LONG_EDGE_PX
    var quality = initialJpegQuality()

    while (true) {
        val (targetW, targetH) = computeTargetSize(sourceWidth, sourceHeight, maxEdge)
        val working = if (targetW != sourceWidth || targetH != sourceHeight) {
            resizeImage(source, targetW, targetH)
        } else {
            source
        }

        while (true) {
            val encoded = working.encodeToData(EncodedImageFormat.JPEG, quality)?.bytes
                ?: throw FixatorPhotoNormalizationException("JPEG compression failed")
            if (encoded.size <= FixatorPhotoSpec.HARD_MAX_BYTES) {
                CameraFixatorLog.d(
                    "normalized in=$inputSize out=${encoded.size} q=$quality edge=$maxEdge ${targetW}x$targetH",
                )
                return encoded
            }

            val nextQuality = selectNextQuality(quality, encoded.size.toLong())
            if (nextQuality != null) {
                quality = nextQuality
                continue
            }
            break
        }

        val nextEdge = selectNextMaxEdge(maxEdge)
        if (nextEdge == maxEdge) {
            throw FixatorPhotoNormalizationException(
                "Photo exceeds ${FixatorPhotoSpec.HARD_MAX_BYTES / 1024 / 1024} MB after normalization",
            )
        }
        maxEdge = nextEdge
        quality = initialJpegQuality()
    }
}

private fun resizeImage(source: Image, targetW: Int, targetH: Int): Image {
    val surface = Surface.makeRasterN32Premul(targetW, targetH)
    val canvas = surface.canvas
    canvas.drawImageRect(
        source,
        Rect.makeWH(source.width.toFloat(), source.height.toFloat()),
        Rect.makeWH(targetW.toFloat(), targetH.toFloat()),
        SamplingMode.LINEAR,
        Paint(),
        true,
    )
    return surface.makeImageSnapshot()
}
