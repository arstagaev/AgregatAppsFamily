package com.tagaev.trrcrm.ui.camera

import com.tagaev.trrcrm.ui.i18n.s

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.TopAppBar
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.tagaev.trrcrm.data.MainRepository
import com.tagaev.trrcrm.data.fixator.FixatorPendingPhotoEntry
import com.tagaev.trrcrm.data.fixator.FixatorPhotoStorage
import com.tagaev.trrcrm.data.fixator.GallerySaveResult
import com.tagaev.trrcrm.data.remote.Resource
import com.tagaev.trrcrm.data.remote.userFacingMessage
import com.tagaev.trrcrm.domain.FixatorPhotoNormalizationException
import com.tagaev.trrcrm.domain.ImageMediatorFileKind
import com.tagaev.trrcrm.domain.ImageMediatorFilePrepareResult
import com.tagaev.trrcrm.domain.ImageMediatorUploadPart
import com.tagaev.trrcrm.domain.classifyImageMediatorFile
import com.tagaev.trrcrm.domain.displayFileStem
import com.tagaev.trrcrm.domain.jpegUploadPart
import com.tagaev.trrcrm.domain.kindFromMime
import com.tagaev.trrcrm.domain.normalizeFixatorPhoto
import com.tagaev.trrcrm.models.DocumentUploadPeriod
import com.tagaev.trrcrm.models.ImageDocumentType
import com.tagaev.trrcrm.models.ImageMediatorUploadResult
import com.tagaev.trrcrm.models.MAX_PHOTOS_PER_UPLOAD_REQUEST
import com.tagaev.trrcrm.models.UploadAvailability
import com.tagaev.trrcrm.data.remote.ImageMediatorApi
import com.tagaev.trrcrm.data.remote.imageMediatorFileRejectMessage
import com.tagaev.trrcrm.ui.common.rememberBusyActionGate
import com.tagaev.trrcrm.ui.permissions.CameraFixatorLog
import com.tagaev.trrcrm.ui.permissions.CameraPermissionGate
import com.tagaev.trrcrm.ui.permissions.FixatorCameraControls
import com.tagaev.trrcrm.ui.permissions.FixatorCameraPreview
import com.tagaev.trrcrm.ui.permissions.decodePhotoThumbnail
import com.tagaev.trrcrm.ui.permissions.rememberDocumentFilePicker
import com.tagaev.trrcrm.ui.permissions.rememberGalleryPhotoPicker
import com.tagaev.trrcrm.ui.permissions.rememberPhotoLibrarySavePermission
import com.tagaev.trrcrm.ui.permissions.openExternalDocument
import compose.icons.FeatherIcons
import compose.icons.feathericons.ArrowLeft
import compose.icons.feathericons.Camera
import compose.icons.feathericons.File
import compose.icons.feathericons.Image
import compose.icons.feathericons.X
import compose.icons.feathericons.Zap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.compose.koinInject

internal data class CapturedPhoto(
    val entry: FixatorPendingPhotoEntry,
    val thumbnail: ImageBitmap?,
) {
    val id: String get() = entry.id
}

private sealed interface UploadUiStatus {
    data object Idle : UploadUiStatus
    data object Uploading : UploadUiStatus
    data class Success(val result: ImageMediatorUploadResult) : UploadUiStatus
    data class Error(val message: String) : UploadUiStatus
}

private enum class CameraSnackbarKind {
    Default,
    Error,
    Success,
}

private val SuccessSnackbarTextColor = Color(0xFF2E7D32)
private val SuccessSnackbarContainerColor = Color(0xFFE8F5E9)

private const val THUMB_SIZE_DP = 72
/** Hidden until the next release; keep FilePickTile and picker wiring in this file. */
private const val SHOW_FILE_PICKER = false
/** Fallback per-request cap when can-upload has not returned limits yet. */
private const val MAX_PHOTOS_PER_UPLOAD_FALLBACK = MAX_PHOTOS_PER_UPLOAD_REQUEST

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DocumentCameraScreen(
    documentNumber: String,
    uploadPeriod: DocumentUploadPeriod,
    title: String,
    documentType: ImageDocumentType = ImageDocumentType.Complects,
    initialQuota: UploadAvailability? = null,
    showUploadStatusBlock: Boolean = false,
    onBack: () -> Unit,
) {
    val documentName = documentType.wireName
    val repository = koinInject<MainRepository>()
    val photoStorage = koinInject<FixatorPhotoStorage>()
    val galleryPermission = rememberPhotoLibrarySavePermission()
    val scope = rememberCoroutineScope()
    val photoActionGate = rememberBusyActionGate()
    val snackbarHostState = remember { SnackbarHostState() }
    var snackbarKind by remember { mutableStateOf(CameraSnackbarKind.Default) }

    val photos = remember { mutableStateListOf<CapturedPhoto>() }
    var uploadStatus by remember { mutableStateOf<UploadUiStatus>(UploadUiStatus.Idle) }
    var cameraControls by remember { mutableStateOf<FixatorCameraControls?>(null) }
    var isProcessingPhoto by remember { mutableStateOf(false) }
    var isCapturePending by remember { mutableStateOf(false) }
    var previewPhoto by remember { mutableStateOf<CapturedPhoto?>(null) }
    var showUploadSuccessDialog by remember { mutableStateOf(false) }
    var uploadQuota by remember(documentNumber, initialQuota) { mutableStateOf(initialQuota) }
    var quotaLoadError by remember { mutableStateOf<String?>(null) }
    var pendingUploadIdempotencyKey by remember { mutableStateOf<String?>(null) }

    val sessionMax = uploadQuota?.availableNow ?: MAX_PHOTOS_PER_UPLOAD_FALLBACK
    val isUploading = uploadStatus is UploadUiStatus.Uploading
    val isCameraBusy = photoActionGate.isBusy || isProcessingPhoto || isUploading || isCapturePending
    val hasReachedPhotoLimit = photos.size >= sessionMax
    val canCapture = cameraControls != null && !isCameraBusy && !hasReachedPhotoLimit && quotaLoadError == null
    val canAddFromGallery = !isCameraBusy && !hasReachedPhotoLimit && quotaLoadError == null
    val pendingPhotoIdentity = photos.joinToString(separator = ",") { it.id }

    LaunchedEffect(documentNumber, pendingPhotoIdentity) {
        pendingUploadIdempotencyKey = null
    }

    fun showCameraSnackbar(message: String, kind: CameraSnackbarKind = CameraSnackbarKind.Default) {
        snackbarKind = kind
        val displayMessage = if (kind == CameraSnackbarKind.Error) {
            userFacingMessage(message, message)
        } else {
            message
        }
        scope.launch {
            snackbarHostState.showSnackbar(
                message = displayMessage,
                duration = SnackbarDuration.Short,
            )
        }
    }

    suspend fun trySaveToPublicGallery(normalizedBytes: ByteArray, displayName: String) {
        if (!galleryPermission.canSaveToGallery) return
        when (photoStorage.saveToPublicGallery(normalizedBytes, displayName)) {
            GallerySaveResult.Success -> Unit
            GallerySaveResult.PermissionDenied -> {
                showCameraSnackbar(s("camera_foto_sohraneno_lokalno_v_galereyu_ne_dobavleno"))
            }
            GallerySaveResult.Failed -> {
                showCameraSnackbar(s("camera_foto_sohraneno_lokalno_v_galereyu_ne_dobavleno"))
            }
            GallerySaveResult.Unavailable -> Unit
        }
    }

    LaunchedEffect(Unit) {
        if (galleryPermission.shouldRequest) {
            galleryPermission.request()
        }
    }

    LaunchedEffect(documentNumber, uploadPeriod, documentType, initialQuota) {
        if (initialQuota != null) {
            uploadQuota = initialQuota
            quotaLoadError = null
        }
        when (val result = repository.getFixatorUploadAvailability(documentNumber, uploadPeriod, documentType)) {
            is Resource.Success -> {
                uploadQuota = result.data
                quotaLoadError = null
            }
            is Resource.Error -> {
                if (initialQuota == null) {
                    uploadQuota = null
                    val message = result.causes
                        ?: s("complectation_ne_udalos_proverit_vozmozhnost_zagruzki")
                    quotaLoadError = message
                    showCameraSnackbar(message, kind = CameraSnackbarKind.Error)
                }
            }
            is Resource.Loading -> Unit
        }
    }

    LaunchedEffect(documentNumber) {
        val pending = photoStorage.listPendingPhotos(documentNumber)
        if (pending.isEmpty()) return@LaunchedEffect
        photos.clear()
        pending.forEach { entry ->
            runCatching {
                val bytes = photoStorage.readPendingPhotoBytes(documentNumber, entry)
                photos.add(
                    CapturedPhoto(
                        entry = entry,
                        thumbnail = decodePhotoThumbnail(bytes),
                    ),
                )
            }.onFailure { error ->
                CameraFixatorLog.d("photo_restore_failed id=${entry.id} message=${error.message}")
            }
        }
        if (photos.isNotEmpty()) {
            showCameraSnackbar("Восстановлено ${photos.size} фото, ожидают отправки")
        }
    }

    fun addPhotosFromBytes(
        rawBytesList: List<ByteArray>,
        fromCamera: Boolean,
        fileNames: List<String?> = emptyList(),
    ) {
        if (fromCamera) {
            isCapturePending = false
        }
        if (rawBytesList.isEmpty() || isCameraBusy) return
        photoActionGate.launch(scope) {
            val maxForSession = uploadQuota?.availableNow ?: MAX_PHOTOS_PER_UPLOAD_FALLBACK
            if (photos.size >= maxForSession) {
                showCameraSnackbar(
                    s("upload_select_at_most_n", maxForSession),
                    kind = CameraSnackbarKind.Error,
                )
                return@launch
            }

            val availableSlots = maxForSession - photos.size
            val bytesToAdd = rawBytesList.take(availableSlots)
            if (bytesToAdd.size < rawBytesList.size) {
                showCameraSnackbar(
                    s("upload_select_at_most_n", availableSlots),
                    kind = CameraSnackbarKind.Error,
                )
            }

            isProcessingPhoto = true
            try {
                var addedCount = 0
                var failedCount = 0
                var addedDocuments = 0
                for ((index, rawBytes) in bytesToAdd.withIndex()) {
                    val rawSize = rawBytes.size
                    val originalName = fileNames.getOrNull(index)
                    val prepared = if (fromCamera) {
                        runCatching {
                            val jpeg = withContext(Dispatchers.Default) {
                                normalizeFixatorPhoto(rawBytes)
                            }
                            jpegUploadPart(jpeg, photos.size)
                        }
                    } else {
                        when (
                            val classified = classifyImageMediatorFile(
                                bytes = rawBytes,
                                originalName = originalName,
                                fallbackIndex = photos.size,
                            )
                        ) {
                            is ImageMediatorFilePrepareResult.Ok -> Result.success(classified.part)
                            is ImageMediatorFilePrepareResult.Rejected -> Result.failure(
                                IllegalArgumentException(imageMediatorFileRejectMessage(classified.reason)),
                            )
                        }
                    }
                    prepared
                        .onSuccess { part ->
                            val savedEntry = runCatching {
                                photoStorage.savePendingPhoto(
                                    documentNumber = documentNumber,
                                    documentName = documentName,
                                    normalizedBytes = part.bytes,
                                    mimeType = part.mimeType,
                                )
                            }.getOrElse { error ->
                                failedCount += 1
                                CameraFixatorLog.d("photo_save_failed message=${error.message}")
                                return@onSuccess
                            }
                            if (part.kind == ImageMediatorFileKind.Image) {
                                trySaveToPublicGallery(part.bytes, savedEntry.fileName)
                            } else {
                                addedDocuments += 1
                            }
                            photos.add(
                                CapturedPhoto(
                                    entry = savedEntry,
                                    thumbnail = decodePhotoThumbnail(part.bytes),
                                ),
                            )
                            addedCount += 1
                            CameraFixatorLog.d(
                                "photo_added count=${photos.size} size=${part.bytes.size} mime=${part.mimeType} from=$rawSize",
                            )
                        }
                        .onFailure { error ->
                            failedCount += 1
                            val message = when (error) {
                                is FixatorPhotoNormalizationException -> error.message ?: s("camera_ne_udalos_obrabotat_foto")
                                is IllegalArgumentException -> error.message ?: s("upload_unsupported_file_type")
                                else -> s("camera_ne_udalos_obrabotat_foto")
                            }
                            CameraFixatorLog.d("photo_normalize_failed from=$rawSize message=$message")
                            showCameraSnackbar(message, kind = CameraSnackbarKind.Error)
                        }
                    if (prepared.isFailure && addedCount == 0 && failedCount == bytesToAdd.size) {
                        break
                    }
                }

                when {
                    fromCamera && addedCount == 1 -> {
                        showCameraSnackbar(s("camera_foto_sdelano"))
                        performCameraHapticFeedback(CameraHapticFeedbackStrength.PhotoCaptured)
                    }
                    !fromCamera && addedDocuments > 0 && addedCount == addedDocuments && addedCount > 1 -> {
                        showCameraSnackbar(s("camera_dobavleno_addedcount_faylov", addedCount))
                    }
                    !fromCamera && addedDocuments == 1 && addedCount == 1 -> {
                        showCameraSnackbar(s("camera_fayl_dobavlen"))
                        performCameraHapticFeedback(CameraHapticFeedbackStrength.PhotoCaptured)
                    }
                    !fromCamera && addedCount > 1 -> showCameraSnackbar("Добавлено $addedCount фото из галереи")
                    !fromCamera && addedCount == 1 -> {
                        showCameraSnackbar(s("camera_foto_dobavleno_iz_galerei"))
                        performCameraHapticFeedback(CameraHapticFeedbackStrength.PhotoCaptured)
                    }
                    failedCount > 0 && addedCount == 0 -> {
                        if (bytesToAdd.size == 1) {
                            // already snacked
                        }
                    }
                }
            } finally {
                isProcessingPhoto = false
            }
        }
    }

    fun addPhoto(rawBytes: ByteArray) {
        addPhotosFromBytes(listOf(rawBytes), fromCamera = true)
    }

    val launchGalleryPicker = rememberGalleryPhotoPicker { pickedBytes ->
        if (pickedBytes.isNotEmpty()) {
            addPhotosFromBytes(pickedBytes, fromCamera = false)
        }
    }

    val launchDocumentFilePicker = rememberDocumentFilePicker { pickedFiles ->
        if (pickedFiles.isNotEmpty()) {
            addPhotosFromBytes(
                rawBytesList = pickedFiles.map { it.bytes },
                fromCamera = false,
                fileNames = pickedFiles.map { it.fileName },
            )
        }
    }

    fun removePhoto(photoId: String) {
        val photo = photos.find { it.id == photoId } ?: return
        scope.launch {
            runCatching {
                photoStorage.deletePendingPhoto(documentNumber, photo.entry)
            }.onFailure { error ->
                CameraFixatorLog.d("photo_delete_failed id=$photoId message=${error.message}")
            }
            photos.removeAll { it.id == photoId }
            showCameraSnackbar(s("camera_foto_udaleno"))
        }
    }

    fun previewPendingPhoto(photo: CapturedPhoto) {
        if (photo.thumbnail != null) {
            previewPhoto = photo
            return
        }
        scope.launch {
            val opened = runCatching {
                val bytes = photoStorage.readPendingPhotoBytes(documentNumber, photo.entry)
                openExternalDocument(photo.entry.fileName, photo.entry.mimeType, bytes)
            }.onFailure { error ->
                CameraFixatorLog.d("open_pending_document_failed id=${photo.id} message=${error.message}")
            }.getOrDefault(false)
            if (!opened) {
                showCameraSnackbar(
                    s("complectation_ne_udalos_otobrazit_foto"),
                    kind = CameraSnackbarKind.Error,
                )
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(FeatherIcons.ArrowLeft, contentDescription = s("settings_nazad"))
                    }
                },
            )
        },
        snackbarHost = {
            SnackbarHost(snackbarHostState) { data ->
                Snackbar(
                    snackbarData = data,
                    containerColor = when (snackbarKind) {
                        CameraSnackbarKind.Error -> MaterialTheme.colorScheme.errorContainer
                        CameraSnackbarKind.Success -> SuccessSnackbarContainerColor
                        CameraSnackbarKind.Default -> MaterialTheme.colorScheme.inverseSurface
                    },
                    contentColor = when (snackbarKind) {
                        CameraSnackbarKind.Error -> MaterialTheme.colorScheme.onErrorContainer
                        CameraSnackbarKind.Success -> SuccessSnackbarTextColor
                        CameraSnackbarKind.Default -> MaterialTheme.colorScheme.inverseOnSurface
                    },
                )
            }
        },
        bottomBar = {
            Surface(tonalElevation = 3.dp) {
                Button(
                    onClick = {
                        if (photos.isEmpty() || isCameraBusy) return@Button
                        val maxForSession = uploadQuota?.availableNow ?: MAX_PHOTOS_PER_UPLOAD_FALLBACK
                        if (photos.size > maxForSession) {
                            showCameraSnackbar(
                                s("upload_select_at_most_n", maxForSession),
                                kind = CameraSnackbarKind.Error,
                            )
                            return@Button
                        }
                        photoActionGate.launch(scope) {
                            uploadStatus = UploadUiStatus.Uploading
                            CameraFixatorLog.d(
                                "upload_start document=$documentNumber photos=${photos.size}",
                            )
                            val entries = photos.map { it.entry }
                            val photoBytes = withContext(Dispatchers.Default) {
                                entries.map { entry ->
                                    photoStorage.readPendingPhotoBytes(documentNumber, entry)
                                }
                            }
                            val parts = entries.zip(photoBytes).map { (entry, bytes) ->
                                ImageMediatorUploadPart(
                                    bytes = bytes,
                                    fileName = entry.fileName,
                                    mimeType = entry.mimeType,
                                    kind = kindFromMime(entry.mimeType),
                                )
                            }
                            val idempotencyKey = pendingUploadIdempotencyKey
                                ?: ImageMediatorApi.generateIdempotencyKey().also {
                                    pendingUploadIdempotencyKey = it
                                }
                            when (
                                val result = repository.uploadFixatorPhotos(
                                    documentNumber = documentNumber,
                                    photos = parts,
                                    uploadPeriod = uploadPeriod,
                                    documentType = documentType,
                                    idempotencyKey = idempotencyKey,
                                )
                            ) {
                                is Resource.Success -> {
                                    pendingUploadIdempotencyKey = null
                                    CameraFixatorLog.d(
                                        "upload_success files=${result.data.storedFilenames.size} folder=${result.data.ftpFolderPath}",
                                    )
                                    uploadStatus = UploadUiStatus.Success(result.data)
                                    photoStorage.deletePendingPhotos(documentNumber, entries)
                                    photos.clear()
                                    showUploadSuccessDialog = true
                                    performCameraHapticFeedback(CameraHapticFeedbackStrength.UploadSucceeded)
                                    val confirmed = result.data.storedFilenames.size
                                        .takeIf { it > 0 } ?: entries.size
                                    uploadQuota = (uploadQuota ?: initialQuota)
                                        ?.afterSuccessfulUpload(confirmed)
                                    when (
                                        val refreshed = repository.getFixatorUploadAvailability(
                                            documentNumber,
                                            uploadPeriod,
                                            documentType,
                                        )
                                    ) {
                                        is Resource.Success -> uploadQuota = refreshed.data
                                        else -> Unit
                                    }
                                }
                                is Resource.Error -> {
                                    CameraFixatorLog.d("upload_error message=${result.causes}")
                                    val message = result.causes ?: s("error_ne_udalos_otpravit_foto")
                                    uploadStatus = UploadUiStatus.Error(message)
                                    showCameraSnackbar(message, kind = CameraSnackbarKind.Error)
                                }
                                is Resource.Loading -> Unit
                            }
                        }
                    },
                    enabled = photos.isNotEmpty() && !isCameraBusy && quotaLoadError == null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                ) {
                    if (isUploading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                        )
                    } else {
                        Text(s("camera_otpravit"))
                    }
                }
            }
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
//            uploadQuota?.let { quota ->
//                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
//                    val inFolder = quota.photosInFolder
//                    val maxDoc = quota.maxPhotosPerDocument
//                    if (inFolder != null && maxDoc != null) {
//                        Text(
//                            text = s("upload_folder_m_of_l", inFolder, maxDoc),
//                            style = MaterialTheme.typography.bodyMedium,
//                            color = MaterialTheme.colorScheme.onSurfaceVariant,
//                        )
//                    }
//                    Text(
//                        text = s(
//                            "upload_session_y_of_15",
//                            quota.uploadedInAppRun,
//                            MAX_PHOTOS_PER_DOCUMENT_PER_APP_RUN,
//                        ),
//                        style = MaterialTheme.typography.bodyMedium,
//                        color = MaterialTheme.colorScheme.onSurfaceVariant,
//                    )
//                    Text(
//                        text = s("upload_available_now_n", quota.availableNow),
//                        style = MaterialTheme.typography.bodySmall,
//                        color = MaterialTheme.colorScheme.onSurfaceVariant,
//                    )
//                }
//            }

            if (showUploadStatusBlock) {
                UploadStatusBlock(status = uploadStatus)
            }

            CameraPermissionGate(rationaleText = s("camera_dlya_semki_nuzhen_dostup_k_kamere")) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                    ) {
                        FixatorCameraPreview(
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(RoundedCornerShape(16.dp)),
                            onPhotoCaptured = ::addPhoto,
                            onControlsChanged = { cameraControls = it },
                            onLog = CameraFixatorLog::d,
                        )
                        CameraPhotoCounterBadge(
                            count = photos.size,
                            max = sessionMax,
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(12.dp),
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Button(
                            onClick = {
                                if (!canCapture) return@Button
                                isCapturePending = true
                                cameraControls?.capturePhoto()
                            },
                            enabled = canCapture,
                            modifier = Modifier.weight(1f),
                        ) {
                            Icon(FeatherIcons.Camera, contentDescription = null)
                            Text(
                                text = s("camera_sdelat_foto"),
                                modifier = Modifier.padding(start = 8.dp),
                            )
                        }
                        OutlinedButton(
                            onClick = { cameraControls?.toggleTorch() },
                            enabled = cameraControls?.isTorchAvailable == true,
                            modifier = Modifier.weight(1f),
                        ) {
                            Icon(FeatherIcons.Zap, contentDescription = null)
                            Text(
                                text = if (cameraControls?.isTorchOn == true) s("camera_vykl") else s("camera_vkl"),
                                modifier = Modifier.padding(start = 8.dp),
                            )
                        }
                    }
                }
            }

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(bottom = 8.dp),
            ) {
                item(key = "pickers") {
                    val remainingSlots = (sessionMax - photos.size).coerceAtLeast(0)
                    fun launchPicker(fromGallery: Boolean) {
                        if (isCameraBusy) return
                        if (remainingSlots <= 0) {
                            showCameraSnackbar(
                                s("upload_select_at_most_n", sessionMax),
                                kind = CameraSnackbarKind.Error,
                            )
                            return
                        }
                        if (fromGallery) launchGalleryPicker(remainingSlots)
                        else if (SHOW_FILE_PICKER) launchDocumentFilePicker(remainingSlots)
                    }
                    if (photos.isEmpty()) {
                        Column(
                            modifier = Modifier.fillParentMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            PhotoGalleryPickTile(
                                expanded = true,
                                enabled = canAddFromGallery,
                                modifier = Modifier.fillMaxWidth(),
                                onClick = { launchPicker(fromGallery = true) },
                            )
                            if (SHOW_FILE_PICKER) {
                                FilePickTile(
                                    expanded = true,
                                    enabled = canAddFromGallery,
                                    modifier = Modifier.fillMaxWidth(),
                                    onClick = { launchPicker(fromGallery = false) },
                                )
                            }
                        }
                    } else {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            PhotoGalleryPickTile(
                                expanded = false,
                                enabled = canAddFromGallery,
                                onClick = { launchPicker(fromGallery = true) },
                            )
                            if (SHOW_FILE_PICKER) {
                                FilePickTile(
                                    expanded = false,
                                    enabled = canAddFromGallery,
                                    onClick = { launchPicker(fromGallery = false) },
                                )
                            }
                        }
                    }
                }
                items(photos, key = { it.id }) { photo ->
                    PhotoThumbnailTile(
                        photo = photo,
                        onPreview = { previewPendingPhoto(photo) },
                        onRemove = { removePhoto(photo.id) },
                    )
                }
            }
        }
    }

    previewPhoto?.let { photo ->
        PhotoPreviewDialog(
            photo = photo,
            onDismiss = { previewPhoto = null },
        )
    }

    if (showUploadSuccessDialog) {
        AlertDialog(
            onDismissRequest = { showUploadSuccessDialog = false },
            text = {
                Text("Успешно загружено в №$documentNumber")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showUploadSuccessDialog = false
                        uploadStatus = UploadUiStatus.Idle
                    },
                ) {
                    Text(s("login_ok"))
                }
            },
        )
    }
}

@Composable
private fun CameraPhotoCounterBadge(
    count: Int,
    max: Int,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        color = Color.Black.copy(alpha = 0.55f),
    ) {
        Text(
            text = "$count/$max",
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            color = Color.White,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            fontSize = 15.sp,
        )
    }
}

@Composable
private fun PhotoPreviewDialog(
    photo: CapturedPhoto,
    onDismiss: () -> Unit,
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.92f)),
            contentAlignment = Alignment.Center,
        ) {
            if (photo.thumbnail != null) {
                Image(
                    bitmap = photo.thumbnail,
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(48.dp),
                    contentScale = ContentScale.Fit,
                )
            }
            IconButton(
                onClick = onDismiss,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp),
            ) {
                Icon(
                    FeatherIcons.X,
                    contentDescription = s("camera_zakryt"),
                    tint = Color.White,
                )
            }
        }
    }
}

@Composable
private fun UploadStatusBlock(status: UploadUiStatus) {
    when (status) {
        UploadUiStatus.Idle -> Unit
        UploadUiStatus.Uploading -> {
            RowStatus(
                text = s("camera_otpravka"),
                color = MaterialTheme.colorScheme.onSurface,
                showProgress = true,
            )
        }
        is UploadUiStatus.Error -> {
            RowStatus(
                text = status.message,
                color = MaterialTheme.colorScheme.error,
            )
        }
        is UploadUiStatus.Success -> {
            val details = buildList {
                status.result.ftpFolderPath?.let { add("Папка: $it") }
                if (status.result.storedFilenames.isNotEmpty()) {
                    add("Файл: ${status.result.storedFilenames.joinToString()}")
                }
                val year = status.result.resolvedYear
                val month = status.result.resolvedMonth
                if (year != null && month != null) {
                    add("Период: $year / $month")
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = s("camera_foto_otpravleno"),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary,
                )
                details.forEach { line ->
                    Text(
                        text = line,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun RowStatus(
    text: String,
    color: Color,
    showProgress: Boolean = false,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (showProgress) {
            CircularProgressIndicator(
                modifier = Modifier.size(16.dp),
                strokeWidth = 2.dp,
            )
        }
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = color,
        )
    }
}

@Composable
private fun PhotoGalleryPickTile(
    expanded: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val label = s("camera_dobavit_iz_galerei")
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
            .then(
                if (expanded) {
                    Modifier.fillMaxWidth()
                } else {
                    Modifier.size(THUMB_SIZE_DP.dp)
                },
            )
            .animateContentSize(animationSpec = spring()),
        contentPadding = if (expanded) {
            PaddingValues(horizontal = 16.dp, vertical = 8.dp)
        } else {
            PaddingValues(0.dp)
        },
    ) {
        if (expanded) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(text = label)
                Icon(
                    FeatherIcons.Image,
                    contentDescription = null,
                )
            }
        } else {
            Icon(
                FeatherIcons.Image,
                contentDescription = label,
            )
        }
    }
}

@Composable
private fun FilePickTile(
    expanded: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val label = s("camera_dobavit_fayl")
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
            .then(
                if (expanded) {
                    Modifier.fillMaxWidth()
                } else {
                    Modifier.size(THUMB_SIZE_DP.dp)
                },
            )
            .animateContentSize(animationSpec = spring()),
        contentPadding = if (expanded) {
            PaddingValues(horizontal = 16.dp, vertical = 8.dp)
        } else {
            PaddingValues(0.dp)
        },
    ) {
        if (expanded) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(text = label)
                Icon(FeatherIcons.File, contentDescription = null)
            }
        } else {
            Icon(FeatherIcons.File, contentDescription = label)
        }
    }
}

@Composable
private fun PhotoAddTile(
    enabled: Boolean,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(THUMB_SIZE_DP.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            FeatherIcons.Camera,
            contentDescription = s("camera_dobavit_foto"),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun PhotoThumbnailTile(
    photo: CapturedPhoto,
    onPreview: () -> Unit,
    onRemove: () -> Unit,
) {
    Box(modifier = Modifier.size(THUMB_SIZE_DP.dp)) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(12.dp))
                .clickable(onClick = onPreview),
        ) {
            if (photo.thumbnail != null) {
                Image(
                    bitmap = photo.thumbnail,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                ) {
                    val ext = photo.entry.fileName.substringAfterLast('.', "FILE").uppercase()
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(2.dp),
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(horizontal = 4.dp, vertical = 18.dp),
                    ) {
                        Icon(
                            FeatherIcons.File,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            text = ext,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Text(
                        text = displayFileStem(photo.entry.fileName, photo.id),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .padding(horizontal = 4.dp, vertical = 4.dp),
                    )
                }
            }
        }

        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .offset(x = 6.dp, y = (-6).dp)
                .size(22.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surface)
                .clickable(onClick = onRemove),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                FeatherIcons.X,
                contentDescription = s("camera_udalit_foto"),
                modifier = Modifier.size(14.dp),
                tint = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}
