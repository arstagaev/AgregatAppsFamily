package com.tagaev.trrcrm.domain

object FixatorPhotoSpec {
    const val MAX_LONG_EDGE_PX = 2560
    const val JPEG_QUALITY_START = 83
    const val JPEG_QUALITY_MIN = 50
    const val TARGET_MIN_BYTES = 1_572_864L // 1.5 MB
    const val TARGET_MAX_BYTES = 3_670_016L // 3.5 MB
    const val HARD_MAX_BYTES = 5_242_880L // 5 MB
    const val MIN_LONG_EDGE_PX = 640

    internal val QUALITY_STEPS = intArrayOf(83, 75, 65, 55, 50)
}

fun computeTargetSize(srcW: Int, srcH: Int, maxEdge: Int = FixatorPhotoSpec.MAX_LONG_EDGE_PX): Pair<Int, Int> {
    require(srcW > 0 && srcH > 0) { "Invalid source size: ${srcW}x$srcH" }
    val longEdge = maxOf(srcW, srcH)
    if (longEdge <= maxEdge) return srcW to srcH

    val scale = maxEdge.toDouble() / longEdge.toDouble()
    val dstW = (srcW * scale).toInt().coerceAtLeast(1)
    val dstH = (srcH * scale).toInt().coerceAtLeast(1)
    return dstW to dstH
}

fun selectNextQuality(currentQuality: Int, sizeBytes: Long, hardMax: Long = FixatorPhotoSpec.HARD_MAX_BYTES): Int? {
    if (sizeBytes <= hardMax) return null
    val steps = FixatorPhotoSpec.QUALITY_STEPS
    val currentIndex = steps.indexOf(currentQuality).takeIf { it >= 0 }
        ?: steps.indexOfLast { it <= currentQuality }.takeIf { it >= 0 }
        ?: 0
    val nextIndex = currentIndex + 1
    return steps.getOrNull(nextIndex)
}

fun selectNextMaxEdge(currentMaxEdge: Int): Int {
    val reduced = (currentMaxEdge * 0.85).toInt()
    return reduced.coerceAtLeast(FixatorPhotoSpec.MIN_LONG_EDGE_PX)
}

fun initialJpegQuality(): Int = FixatorPhotoSpec.JPEG_QUALITY_START

fun exceedsHardMax(sizeBytes: Int): Boolean = sizeBytes.toLong() > FixatorPhotoSpec.HARD_MAX_BYTES
