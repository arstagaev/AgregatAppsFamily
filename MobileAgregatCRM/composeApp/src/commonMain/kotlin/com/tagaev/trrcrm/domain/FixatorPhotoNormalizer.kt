package com.tagaev.trrcrm.domain

class FixatorPhotoNormalizationException(message: String) : Exception(message)

expect fun normalizeFixatorPhoto(rawBytes: ByteArray): ByteArray
