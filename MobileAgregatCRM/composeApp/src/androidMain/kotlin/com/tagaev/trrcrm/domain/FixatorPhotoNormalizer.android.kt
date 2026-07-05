package com.tagaev.trrcrm.domain

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import androidx.exifinterface.media.ExifInterface
import com.tagaev.trrcrm.ui.permissions.CameraFixatorLog
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream

actual fun normalizeFixatorPhoto(rawBytes: ByteArray): ByteArray {
    if (rawBytes.isEmpty()) {
        throw FixatorPhotoNormalizationException("Empty photo data")
    }

    val inputSize = rawBytes.size
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeByteArray(rawBytes, 0, rawBytes.size, bounds)
    if (bounds.outWidth <= 0 || bounds.outHeight <= 0) {
        throw FixatorPhotoNormalizationException("Unable to decode photo dimensions")
    }

    var maxEdge = FixatorPhotoSpec.MAX_LONG_EDGE_PX
    var quality = initialJpegQuality()

    while (true) {
        val oriented = decodeOrientedBitmap(rawBytes) ?: throw FixatorPhotoNormalizationException(
            "Unable to decode photo",
        )
        val (targetW, targetH) = computeTargetSize(oriented.width, oriented.height, maxEdge)
        val scaled = if (targetW != oriented.width || targetH != oriented.height) {
            Bitmap.createScaledBitmap(oriented, targetW, targetH, true).also {
                if (it !== oriented) oriented.recycle()
            }
        } else {
            oriented
        }

        while (true) {
            val encoded = encodeJpeg(scaled, quality)
            if (encoded.size <= FixatorPhotoSpec.HARD_MAX_BYTES) {
                scaled.recycle()
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

        scaled.recycle()
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

private fun decodeOrientedBitmap(rawBytes: ByteArray): Bitmap? {
    val decoded = BitmapFactory.decodeByteArray(rawBytes, 0, rawBytes.size) ?: return null
    val rotation = readExifRotationDegrees(rawBytes)
    if (rotation == 0) return decoded

    val matrix = Matrix().apply { postRotate(rotation.toFloat()) }
    return Bitmap.createBitmap(decoded, 0, 0, decoded.width, decoded.height, matrix, true).also {
        if (it !== decoded) decoded.recycle()
    }
}

private fun readExifRotationDegrees(rawBytes: ByteArray): Int {
    return runCatching {
        ExifInterface(ByteArrayInputStream(rawBytes)).rotationDegrees
    }.getOrDefault(0)
}

private fun encodeJpeg(bitmap: Bitmap, quality: Int): ByteArray {
    val output = ByteArrayOutputStream()
    if (!bitmap.compress(Bitmap.CompressFormat.JPEG, quality, output)) {
        throw FixatorPhotoNormalizationException("JPEG compression failed")
    }
    return output.toByteArray()
}
