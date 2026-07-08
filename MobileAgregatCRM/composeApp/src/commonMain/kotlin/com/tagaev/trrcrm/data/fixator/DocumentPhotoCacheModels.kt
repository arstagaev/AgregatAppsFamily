package com.tagaev.trrcrm.data.fixator

data class DocumentPhotoCacheKey(
    val documentType: String,
    val documentNumber: String,
    val imageId: String,
)

data class DocumentPhotoCacheStats(
    val deletedFiles: Int,
    val freedBytes: Long,
)
