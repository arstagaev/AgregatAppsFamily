package com.tagaev.trrcrm.data.fixator

import kotlinx.serialization.Serializable

@Serializable
data class FixatorPendingManifest(
    val documentNumber: String,
    val documentName: String,
    val photos: List<FixatorPendingPhotoEntry> = emptyList(),
)

@Serializable
data class FixatorPendingPhotoEntry(
    val id: String,
    val fileName: String,
    val createdAtEpochMs: Long,
)

enum class GallerySaveResult {
    Success,
    PermissionDenied,
    Unavailable,
    Failed,
}
