package com.tagaev.trrcrm.ui.photos

import com.tagaev.trrcrm.data.MainRepository
import com.tagaev.trrcrm.data.featureflags.MobileFeatureFlagsStore
import com.tagaev.trrcrm.data.remote.ImageMediatorException
import com.tagaev.trrcrm.data.remote.Resource
import com.tagaev.trrcrm.data.remote.friendlyError
import com.tagaev.trrcrm.domain.isValidDocumentNumber
import com.tagaev.trrcrm.domain.normalizeDocumentNumber
import com.tagaev.trrcrm.domain.resolvedDocumentNumber
import com.tagaev.trrcrm.models.ImageDocumentType
import com.tagaev.trrcrm.models.DocumentUploadPeriod
import com.tagaev.trrcrm.models.UploadAvailability
import com.tagaev.trrcrm.ui.i18n.tr
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * Shared camera / photo-count / viewer state for a fixed [documentType].
 * Complects is never gated. WorkOrder uses split upload/download toggles.
 * InnerOrder / Event / Delivery use one combined toggle each.
 */
class DocumentPhotoSession(
    private val documentType: ImageDocumentType,
    private val repository: MainRepository,
    private val featureFlags: MobileFeatureFlagsStore,
    private val appScope: CoroutineScope,
) {
    private val _isCameraOpen = MutableStateFlow(false)
    val isCameraOpen: StateFlow<Boolean> = _isCameraOpen
    private val _isCameraPrecheckInProgress = MutableStateFlow(false)
    val isCameraPrecheckInProgress: StateFlow<Boolean> = _isCameraPrecheckInProgress
    private val _cameraDocumentNumber = MutableStateFlow<String?>(null)
    val cameraDocumentNumber: StateFlow<String?> = _cameraDocumentNumber
    private val _cameraUploadPeriod = MutableStateFlow<DocumentUploadPeriod?>(null)
    val cameraUploadPeriod: StateFlow<DocumentUploadPeriod?> = _cameraUploadPeriod
    private val _cameraPrecheckError = MutableStateFlow<String?>(null)
    val cameraPrecheckError: StateFlow<String?> = _cameraPrecheckError
    private val _cameraUploadQuota = MutableStateFlow<UploadAvailability?>(null)
    val cameraUploadQuota: StateFlow<UploadAvailability?> = _cameraUploadQuota

    private val _documentPhotoCount = MutableStateFlow(0)
    val documentPhotoCount: StateFlow<Int> = _documentPhotoCount
    private val _isDocumentPhotoCountLoading = MutableStateFlow(false)
    val isDocumentPhotoCountLoading: StateFlow<Boolean> = _isDocumentPhotoCountLoading
    private val _documentPhotoCountLoaded = MutableStateFlow(false)
    val documentPhotoCountLoaded: StateFlow<Boolean> = _documentPhotoCountLoaded
    private var documentPhotoCountRequestId = 0

    private val _isPhotosViewerOpen = MutableStateFlow(false)
    val isPhotosViewerOpen: StateFlow<Boolean> = _isPhotosViewerOpen
    private val _photosViewerDocumentNumber = MutableStateFlow<String?>(null)
    val photosViewerDocumentNumber: StateFlow<String?> = _photosViewerDocumentNumber
    private val _photosViewerUploadPeriod = MutableStateFlow<DocumentUploadPeriod?>(null)
    val photosViewerUploadPeriod: StateFlow<DocumentUploadPeriod?> = _photosViewerUploadPeriod

    suspend fun isUploadEnabled(): Boolean = isPhotosFeatureEnabled(forUpload = true)

    suspend fun isDownloadEnabled(): Boolean = isPhotosFeatureEnabled(forUpload = false)

    private suspend fun isPhotosFeatureEnabled(forUpload: Boolean): Boolean =
        when (documentType) {
            ImageDocumentType.Complects -> true
            ImageDocumentType.WorkOrder -> {
                if (forUpload) {
                    featureFlags.isPhotosUploadWorkOrdersEtcEnabled()
                } else {
                    featureFlags.isPhotosDownloadWorkOrdersEtcEnabled()
                }
            }
            ImageDocumentType.InnerOrder -> featureFlags.isPhotosInnerOrderEnabled()
            ImageDocumentType.Event -> featureFlags.isPhotosEventsEnabled()
            ImageDocumentType.Delivery -> featureFlags.isPhotosCargoEnabled()
        }

    fun requestOpenCamera(rawNumber: String, uploadPeriod: DocumentUploadPeriod?) {
        if (_isCameraPrecheckInProgress.value) return

        val normalized = normalizeDocumentNumber(rawNumber)
        if (!isValidDocumentNumber(normalized)) {
            _cameraPrecheckError.value = tr("complectation_nekorrektnyy_nomer_dokumenta")
            return
        }
        if (uploadPeriod == null) {
            _cameraUploadQuota.value = null
            _cameraPrecheckError.value = tr("upload_document_date_missing")
            return
        }

        appScope.launch {
            // When upload/photos toggle is off: do not call can-upload / open camera.
            if (!isUploadEnabled()) return@launch
            _isCameraPrecheckInProgress.value = true
            _cameraPrecheckError.value = null
            when (val result = repository.checkCanUploadFixatorPhotos(normalized, uploadPeriod, documentType)) {
                is Resource.Success -> {
                    _cameraUploadQuota.value = result.data
                    _cameraDocumentNumber.value = resolvedDocumentNumber(
                        result.data.resolvedDocumentNumber,
                        normalized,
                    )
                    _cameraUploadPeriod.value = DocumentUploadPeriod.fromResolved(
                        result.data.resolvedYear,
                        result.data.resolvedMonth,
                        uploadPeriod,
                    )
                    _isCameraOpen.value = true
                }
                is Resource.Error -> {
                    _cameraUploadQuota.value = null
                    _cameraPrecheckError.value = result.causes
                        ?: friendlyError(
                            result.exception,
                            tr("complectation_ne_udalos_proverit_vozmozhnost_zagruzki"),
                        )
                }
                is Resource.Loading -> Unit
            }
            _isCameraPrecheckInProgress.value = false
        }
    }

    fun closeCamera() {
        val documentNumber = _cameraDocumentNumber.value
        val uploadPeriod = _cameraUploadPeriod.value
        _isCameraOpen.value = false
        _cameraDocumentNumber.value = null
        _cameraUploadPeriod.value = null
        _cameraUploadQuota.value = null
        if (documentNumber != null) {
            refreshDocumentPhotoCount(documentNumber, uploadPeriod)
        }
    }

    fun consumeCameraPrecheckError() {
        _cameraPrecheckError.value = null
    }

    fun refreshDocumentPhotoCount(documentNumber: String, uploadPeriod: DocumentUploadPeriod?) {
        appScope.launch {
            // When download/photos toggle is off: no ImageMediator count request.
            if (!isDownloadEnabled()) {
                _documentPhotoCount.value = 0
                _documentPhotoCountLoaded.value = true
                _isDocumentPhotoCountLoading.value = false
                return@launch
            }
            val normalized = normalizeDocumentNumber(documentNumber)
            if (!isValidDocumentNumber(normalized) || uploadPeriod == null) {
                _documentPhotoCount.value = 0
                _documentPhotoCountLoaded.value = true
                _isDocumentPhotoCountLoading.value = false
                return@launch
            }
            val requestId = ++documentPhotoCountRequestId
            _documentPhotoCountLoaded.value = false
            _isDocumentPhotoCountLoading.value = true
            try {
                when (val result = repository.getFixatorDocumentPhotoCount(normalized, uploadPeriod, documentType)) {
                    is Resource.Success -> {
                        if (requestId != documentPhotoCountRequestId) return@launch
                        _documentPhotoCount.value = result.data
                    }
                    is Resource.Error -> {
                        if (requestId != documentPhotoCountRequestId) return@launch
                        val status = (result.exception as? ImageMediatorException)?.statusCode
                        when (status) {
                            404 -> _documentPhotoCount.value = 0
                            // 401/429/503 are not "no photos".
                            else -> Unit
                        }
                    }
                    is Resource.Loading -> Unit
                }
            } finally {
                if (requestId == documentPhotoCountRequestId) {
                    _documentPhotoCountLoaded.value = true
                    _isDocumentPhotoCountLoading.value = false
                }
            }
        }
    }

    fun requestOpenDocumentPhotos(documentNumber: String, uploadPeriod: DocumentUploadPeriod?) {
        appScope.launch {
            if (!isDownloadEnabled()) return@launch
            if (!_documentPhotoCountLoaded.value || _documentPhotoCount.value <= 0) return@launch

            val normalized = normalizeDocumentNumber(documentNumber)
            if (!isValidDocumentNumber(normalized) || uploadPeriod == null) return@launch

            _photosViewerDocumentNumber.value = normalized
            _photosViewerUploadPeriod.value = uploadPeriod
            _isPhotosViewerOpen.value = true
        }
    }

    fun closePhotosViewer() {
        val documentNumber = _photosViewerDocumentNumber.value
        val uploadPeriod = _photosViewerUploadPeriod.value
        _isPhotosViewerOpen.value = false
        _photosViewerDocumentNumber.value = null
        _photosViewerUploadPeriod.value = null
        if (documentNumber != null) {
            refreshDocumentPhotoCount(documentNumber, uploadPeriod)
        }
    }
}
