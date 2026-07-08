package com.tagaev.trrcrm.ui.camera

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
import com.tagaev.trrcrm.domain.normalizeFixatorPhoto
import com.tagaev.trrcrm.models.ImageMediatorUploadResult
import com.tagaev.trrcrm.ui.common.rememberBusyActionGate
import com.tagaev.trrcrm.ui.permissions.CameraFixatorLog
import com.tagaev.trrcrm.ui.permissions.CameraPermissionGate
import com.tagaev.trrcrm.ui.permissions.FixatorCameraControls
import com.tagaev.trrcrm.ui.permissions.FixatorCameraPreview
import com.tagaev.trrcrm.ui.permissions.decodePhotoThumbnail
import com.tagaev.trrcrm.ui.permissions.rememberGalleryPhotoPicker
import com.tagaev.trrcrm.ui.permissions.rememberPhotoLibrarySavePermission
import compose.icons.FeatherIcons
import compose.icons.feathericons.ArrowLeft
import compose.icons.feathericons.Camera
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
private const val MAX_PHOTOS_PER_UPLOAD = 10

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DocumentCameraScreen(
    documentNumber: String,
    title: String,
    documentName: String = "Комплектация",
    showUploadStatusBlock: Boolean = false,
    onBack: () -> Unit,
) {
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

    val isUploading = uploadStatus is UploadUiStatus.Uploading
    val isCameraBusy = photoActionGate.isBusy || isProcessingPhoto || isUploading || isCapturePending
    val hasReachedPhotoLimit = photos.size >= MAX_PHOTOS_PER_UPLOAD
    val canCapture = cameraControls != null && !isCameraBusy && !hasReachedPhotoLimit
    val canAddFromGallery = !isCameraBusy && !hasReachedPhotoLimit

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
                showCameraSnackbar("Фото сохранено локально, в галерею не добавлено")
            }
            GallerySaveResult.Failed -> {
                showCameraSnackbar("Фото сохранено локально, в галерею не добавлено")
            }
            GallerySaveResult.Unavailable -> Unit
        }
    }

    LaunchedEffect(Unit) {
        if (galleryPermission.shouldRequest) {
            galleryPermission.request()
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

    fun addPhotosFromBytes(rawBytesList: List<ByteArray>, fromCamera: Boolean) {
        if (fromCamera) {
            isCapturePending = false
        }
        if (rawBytesList.isEmpty() || isCameraBusy) return
        photoActionGate.launch(scope) {
            if (photos.size >= MAX_PHOTOS_PER_UPLOAD) {
                showCameraSnackbar(
                    "Можно добавить не более $MAX_PHOTOS_PER_UPLOAD фото за одну отправку",
                    kind = CameraSnackbarKind.Error,
                )
                return@launch
            }

            val availableSlots = MAX_PHOTOS_PER_UPLOAD - photos.size
            val bytesToAdd = rawBytesList.take(availableSlots)
            if (bytesToAdd.size < rawBytesList.size) {
                showCameraSnackbar(
                    "Можно добавить не более $MAX_PHOTOS_PER_UPLOAD фото за одну отправку",
                    kind = CameraSnackbarKind.Error,
                )
            }

            isProcessingPhoto = true
            try {
                var addedCount = 0
                var failedCount = 0
                for (rawBytes in bytesToAdd) {
                    val rawSize = rawBytes.size
                    val normalizedResult = runCatching {
                        withContext(Dispatchers.Default) {
                            normalizeFixatorPhoto(rawBytes)
                        }
                    }
                    val processed = normalizedResult
                        .onSuccess { normalizedBytes ->
                            val savedEntry = runCatching {
                                photoStorage.savePendingPhoto(
                                    documentNumber = documentNumber,
                                    documentName = documentName,
                                    normalizedBytes = normalizedBytes,
                                )
                            }.getOrElse { error ->
                                failedCount += 1
                                CameraFixatorLog.d("photo_save_failed message=${error.message}")
                                return@onSuccess
                            }
                            trySaveToPublicGallery(normalizedBytes, savedEntry.fileName)
                            photos.add(
                                CapturedPhoto(
                                    entry = savedEntry,
                                    thumbnail = decodePhotoThumbnail(normalizedBytes),
                                ),
                            )
                            addedCount += 1
                            CameraFixatorLog.d(
                                "photo_added count=${photos.size} size=${normalizedBytes.size} normalized_from=$rawSize",
                            )
                        }
                        .onFailure { error ->
                            failedCount += 1
                            val message = when (error) {
                                is FixatorPhotoNormalizationException -> error.message ?: "Не удалось обработать фото"
                                else -> "Не удалось обработать фото"
                            }
                            CameraFixatorLog.d("photo_normalize_failed from=$rawSize message=$message")
                            showCameraSnackbar(message, kind = CameraSnackbarKind.Error)
                        }
                    if (processed.isFailure && addedCount == 0 && failedCount == bytesToAdd.size) {
                        break
                    }
                }

                when {
                    fromCamera && addedCount == 1 -> {
                        showCameraSnackbar("Фото сделано")
                        performCameraHapticFeedback(CameraHapticFeedbackStrength.PhotoCaptured)
                    }
                    !fromCamera && addedCount > 1 -> showCameraSnackbar("Добавлено $addedCount фото из галереи")
                    !fromCamera && addedCount == 1 -> {
                        showCameraSnackbar("Фото добавлено из галереи")
                        performCameraHapticFeedback(CameraHapticFeedbackStrength.PhotoCaptured)
                    }
                    failedCount > 0 && addedCount == 0 -> {
                        if (bytesToAdd.size == 1) {
                            showCameraSnackbar("Не удалось добавить фото", kind = CameraSnackbarKind.Error)
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

    fun removePhoto(photoId: String) {
        val photo = photos.find { it.id == photoId } ?: return
        scope.launch {
            runCatching {
                photoStorage.deletePendingPhoto(documentNumber, photo.entry)
            }.onFailure { error ->
                CameraFixatorLog.d("photo_delete_failed id=$photoId message=${error.message}")
            }
            photos.removeAll { it.id == photoId }
            showCameraSnackbar("Фото удалено")
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(FeatherIcons.ArrowLeft, contentDescription = "Назад")
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
                            when (
                                val result = repository.uploadFixatorPhotos(
                                    documentNumber = documentNumber,
                                    photos = photoBytes,
                                    documentName = documentName,
                                )
                            ) {
                                is Resource.Success -> {
                                    CameraFixatorLog.d(
                                        "upload_success files=${result.data.storedFilenames.size} folder=${result.data.ftpFolderPath}",
                                    )
                                    uploadStatus = UploadUiStatus.Success(result.data)
                                    photoStorage.deletePendingPhotos(documentNumber, entries)
                                    photos.clear()
                                    showUploadSuccessDialog = true
                                    performCameraHapticFeedback(CameraHapticFeedbackStrength.UploadSucceeded)
                                }
                                is Resource.Error -> {
                                    CameraFixatorLog.d("upload_error message=${result.causes}")
                                    val message = result.causes ?: "Не удалось отправить фото"
                                    uploadStatus = UploadUiStatus.Error(message)
                                    showCameraSnackbar(message, kind = CameraSnackbarKind.Error)
                                }
                                is Resource.Loading -> Unit
                            }
                        }
                    },
                    enabled = photos.isNotEmpty() && !isCameraBusy,
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
                        Text("Отправить")
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
            if (showUploadStatusBlock) {
                UploadStatusBlock(status = uploadStatus)
            }

            CameraPermissionGate(rationaleText = "Для съёмки нужен доступ к камере.") {
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
                            max = MAX_PHOTOS_PER_UPLOAD,
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
                                text = "Сделать фото",
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
                                text = if (cameraControls?.isTorchOn == true) "Выкл" else "Вкл",
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
                item(key = "gallery_pick") {
                    PhotoGalleryPickTile(
                        enabled = canAddFromGallery,
                        onClick = {
                            if (isCameraBusy) return@PhotoGalleryPickTile
                            launchGalleryPicker(MAX_PHOTOS_PER_UPLOAD - photos.size)
                        },
                    )
                }
                items(photos, key = { it.id }) { photo ->
                    PhotoThumbnailTile(
                        photo = photo,
                        onPreview = { previewPhoto = photo },
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
                Text("Фотографии успешно загружены в комплектацию $documentNumber")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showUploadSuccessDialog = false
                        uploadStatus = UploadUiStatus.Idle
                    },
                ) {
                    Text("ОК")
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
                    contentDescription = "Закрыть",
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
                text = "Отправка…",
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
                    text = "Фото отправлено",
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
            FeatherIcons.Image,
            contentDescription = "Добавить из галереи",
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
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
            contentDescription = "Добавить фото",
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
                )
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
                contentDescription = "Удалить фото",
                modifier = Modifier.size(14.dp),
                tint = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}
