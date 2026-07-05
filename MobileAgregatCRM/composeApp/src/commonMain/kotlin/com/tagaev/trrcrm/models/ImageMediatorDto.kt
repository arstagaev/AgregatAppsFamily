package com.tagaev.trrcrm.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ImageMediatorCanUploadRequest(
    @SerialName("document_number") val documentNumber: String,
    @SerialName("document_name") val documentName: String? = null,
)

@Serializable
data class ImageMediatorLimits(
    @SerialName("max_photos_per_user_30_min") val maxPhotosPerUser30Min: Int? = null,
    @SerialName("used_last_30_min") val usedLast30Min: Int? = null,
    val remaining: Int? = null,
    @SerialName("max_files_per_request") val maxFilesPerRequest: Int? = null,
)

@Serializable
data class ImageMediatorCanUploadResponse(
    val allowed: Boolean,
    @SerialName("document_number") val documentNumber: String,
    @SerialName("resolved_year") val resolvedYear: Int? = null,
    @SerialName("resolved_month") val resolvedMonth: Int? = null,
    val limits: ImageMediatorLimits? = null,
    @SerialName("folder_found") val folderFound: Boolean = false,
    @SerialName("folder_path") val folderPath: String? = null,
)

@Serializable
data class ImageMediatorUploadedFile(
    @SerialName("original_filename") val originalFilename: String? = null,
    @SerialName("stored_filename") val storedFilename: String? = null,
    @SerialName("ftp_file_path") val ftpFilePath: String? = null,
    @SerialName("mime_type") val mimeType: String? = null,
    @SerialName("size_bytes") val sizeBytes: Long? = null,
    val sha256: String? = null,
)

@Serializable
data class ImageMediatorUploadResponse(
    @SerialName("document_number") val documentNumber: String,
    @SerialName("resolved_year") val resolvedYear: Int? = null,
    @SerialName("resolved_month") val resolvedMonth: Int? = null,
    @SerialName("ftp_folder_path") val ftpFolderPath: String? = null,
    @SerialName("uploaded_files") val uploadedFiles: List<ImageMediatorUploadedFile> = emptyList(),
)

data class ImageMediatorUploadResult(
    val documentNumber: String,
    val storedFilenames: List<String>,
    val ftpFolderPath: String?,
    val resolvedYear: Int?,
    val resolvedMonth: Int?,
)
