package com.tagaev.trrcrm.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime

/** Wire codes for ImageMediator document_name / document_type. */
enum class ImageDocumentType(val wireName: String) {
    Complects("Complects"),
    WorkOrder("WorkOrder"),
    InnerOrder("InnerOrder"),
    Event("Event"),
    Delivery("Delivery"),
    ;

    /** Russian UI label (not for API). */
    val labelRu: String
        get() = when (this) {
            Complects -> "Комплектация"
            WorkOrder -> "Заказ-наряд"
            InnerOrder -> "Внутренняя заявка"
            Event -> "Событие"
            Delivery -> "Доставка"
        }

    companion object {
        fun fromWireName(raw: String?): ImageDocumentType {
            val normalized = raw?.trim().orEmpty()
            if (normalized.isEmpty()) return Complects
            return entries.firstOrNull { it.wireName.equals(normalized, ignoreCase = true) }
                ?: Complects
        }
    }
}

/** @deprecated Prefer [ImageDocumentType.Complects.wireName]. */
const val IMAGE_MEDIATOR_DOCUMENT_COMPLECTS = "Complects"

/** Max successfully uploaded photos per document key during one app process run (client-only). */
const val MAX_PHOTOS_PER_DOCUMENT_PER_APP_RUN = 15

/** Default max files in a single multipart upload when API omits max_files_per_request. */
const val MAX_PHOTOS_PER_UPLOAD_REQUEST = 10

data class DocumentUploadKey(
    val documentType: ImageDocumentType,
    val documentNumber: String,
)

/**
 * Folder period supplied by the document itself.  It is deliberately not derived
 * from the device clock: an upload must always land in the document's month.
 */
data class DocumentUploadPeriod(
    val year: Int,
    val month: Int,
) {
    init {
        require(month in 1..12) { "Month must be in 1..12" }
    }

    companion object {
        fun fromResolved(year: Int?, month: Int?, fallback: DocumentUploadPeriod): DocumentUploadPeriod =
            if (year != null && month != null && month in 1..12) {
                DocumentUploadPeriod(year, month)
            } else {
                fallback
            }

        fun from(date: LocalDateTime?): DocumentUploadPeriod? =
            date?.let { DocumentUploadPeriod(it.year, it.month.ordinal + 1) }

        /** Supports 1C dates (`dd.MM.yyyy ...`) and ISO dates without using device time. */
        fun from(rawDate: String?): DocumentUploadPeriod? {
            val raw = rawDate?.trim().orEmpty()
            val russianMatch = RU_DATE.find(raw)
            val match = russianMatch ?: ISO_DATE.find(raw) ?: return null
            val (year, month, day) = if (russianMatch != null) {
                Triple(match.groupValues[3].toIntOrNull(), match.groupValues[2].toIntOrNull(), match.groupValues[1].toIntOrNull())
            } else Triple(match.groupValues[1].toIntOrNull(), match.groupValues[2].toIntOrNull(), match.groupValues[3].toIntOrNull())
            return runCatching { LocalDate(year ?: return null, month ?: return null, day ?: return null) }
                .getOrNull()
                ?.let { DocumentUploadPeriod(it.year, it.month.ordinal + 1) }
        }

        private val RU_DATE = Regex("^(\\d{1,2})\\.(\\d{1,2})\\.(\\d{4})")
        private val ISO_DATE = Regex("^(\\d{4})-(\\d{1,2})-(\\d{1,2})")
    }
}

@Serializable
data class ImageMediatorCanUploadRequest(
    @SerialName("document_number") val documentNumber: String,
    @SerialName("document_name") val documentName: String? = null,
    val year: Int,
    val month: Int,
)

@Serializable
data class ImageMediatorLimits(
    @SerialName("max_photos_per_document") val maxPhotosPerDocument: Int? = null,
    @SerialName("photos_in_folder") val photosInFolder: Int? = null,
    /** Legacy alias of [maxPhotosPerDocument] (API &lt; 1.5). */
    @SerialName("max_photos_per_user_30_min") val maxPhotosPerUser30Min: Int? = null,
    /** Legacy alias of [photosInFolder] (API &lt; 1.5). */
    @SerialName("used_last_30_min") val usedLast30Min: Int? = null,
    val remaining: Int? = null,
    @SerialName("max_files_per_request") val maxFilesPerRequest: Int? = null,
) {
    fun effectiveMaxPhotos(): Int? = maxPhotosPerDocument ?: maxPhotosPerUser30Min

    fun effectivePhotosInFolder(): Int? = photosInFolder ?: usedLast30Min

    fun effectiveRemaining(): Int? = remaining

    fun effectiveMaxFilesPerRequest(
        fallback: Int = MAX_PHOTOS_PER_UPLOAD_REQUEST,
    ): Int = maxFilesPerRequest?.takeIf { it > 0 } ?: fallback
}

@Serializable
data class ImageMediatorCanUploadResponse(
    val allowed: Boolean,
    @SerialName("document_number") val documentNumber: String,
    @SerialName("resolved_document_number") val resolvedDocumentNumber: String? = null,
    @SerialName("api_version") val apiVersion: String? = null,
    @SerialName("document_type") val documentType: String? = null,
    @SerialName("resolved_year") val resolvedYear: Int? = null,
    @SerialName("resolved_month") val resolvedMonth: Int? = null,
    val limits: ImageMediatorLimits? = null,
    @SerialName("folder_found") val folderFound: Boolean = false,
    @SerialName("folder_path") val folderPath: String? = null,
)

/**
 * Combined server + client-session quota for camera/upload UX.
 * [availableNow] = min(serverRemaining, sessionRemaining, requestLimit).
 * Session cap remains [MAX_PHOTOS_PER_DOCUMENT_PER_APP_RUN]; request cap is typically 10.
 */
data class UploadAvailability(
    val serverRemaining: Int,
    val sessionRemaining: Int,
    val requestLimit: Int,
    val photosInFolder: Int?,
    val maxPhotosPerDocument: Int?,
    val uploadedInAppRun: Int,
    val folderFound: Boolean = true,
    val allowed: Boolean = true,
    val resolvedDocumentNumber: String? = null,
    val resolvedYear: Int? = null,
    val resolvedMonth: Int? = null,
) {
    val availableNow: Int
        get() = minOf(
            serverRemaining.coerceAtLeast(0),
            sessionRemaining.coerceAtLeast(0),
            requestLimit.coerceAtLeast(1),
        )

    /** Backward-compatible alias used by older call sites / tests. */
    val remaining: Int get() = serverRemaining
    val maxFilesPerRequest: Int get() = requestLimit
    val sessionMax: Int get() = availableNow

    /** Local optimistic update after a confirmed batch upload. */
    fun afterSuccessfulUpload(confirmedCount: Int): UploadAvailability {
        val confirmed = confirmedCount.coerceAtLeast(0)
        if (confirmed == 0) return this
        return copy(
            serverRemaining = (serverRemaining - confirmed).coerceAtLeast(0),
            sessionRemaining = (sessionRemaining - confirmed).coerceAtLeast(0),
            photosInFolder = photosInFolder?.let { it + confirmed },
            uploadedInAppRun = uploadedInAppRun + confirmed,
        )
    }

    companion object {
        fun from(
            response: ImageMediatorCanUploadResponse,
            sessionRemaining: Int,
            uploadedInAppRun: Int,
        ): UploadAvailability {
            val limits = response.limits
            val serverRemaining = when {
                !response.folderFound || !response.allowed -> 0
                else -> limits?.effectiveRemaining() ?: 0
            }
            return UploadAvailability(
                serverRemaining = serverRemaining.coerceAtLeast(0),
                sessionRemaining = sessionRemaining.coerceAtLeast(0),
                requestLimit = limits?.effectiveMaxFilesPerRequest()
                    ?: MAX_PHOTOS_PER_UPLOAD_REQUEST,
                photosInFolder = limits?.effectivePhotosInFolder(),
                maxPhotosPerDocument = limits?.effectiveMaxPhotos(),
                uploadedInAppRun = uploadedInAppRun.coerceAtLeast(0),
                folderFound = response.folderFound,
                allowed = response.allowed,
                resolvedDocumentNumber = response.resolvedDocumentNumber ?: response.documentNumber,
                resolvedYear = response.resolvedYear,
                resolvedMonth = response.resolvedMonth,
            )
        }
    }
}

/** @deprecated Prefer [UploadAvailability]. */
typealias FixatorUploadQuota = UploadAvailability

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
    @SerialName("resolved_document_number") val resolvedDocumentNumber: String? = null,
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

@Serializable
data class ImageMediatorImageCountResponse(
    @SerialName("document_number") val documentNumber: String,
    @SerialName("resolved_document_number") val resolvedDocumentNumber: String? = null,
    @SerialName("resolved_year") val resolvedYear: Int? = null,
    @SerialName("resolved_month") val resolvedMonth: Int? = null,
    val count: Int,
)

@Serializable
data class ImageMediatorImageMeta(
    @SerialName("image_id") val imageId: String,
    @SerialName("content_url") val contentUrl: String,
    @SerialName("size_bytes") val sizeBytes: Long? = null,
)

@Serializable
data class ImageMediatorImageListResponse(
    @SerialName("document_number") val documentNumber: String,
    @SerialName("resolved_document_number") val resolvedDocumentNumber: String? = null,
    @SerialName("resolved_year") val resolvedYear: Int? = null,
    @SerialName("resolved_month") val resolvedMonth: Int? = null,
    val page: Int,
    @SerialName("page_size") val pageSize: Int,
    @SerialName("total_count") val totalCount: Int,
    @SerialName("total_pages") val totalPages: Int,
    @SerialName("has_next") val hasNext: Boolean,
    @SerialName("has_previous") val hasPrevious: Boolean,
    val images: List<ImageMediatorImageMeta> = emptyList(),
)
