package com.tagaev.trrcrm.ui.permissions

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.viewinterop.UIKitView
import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.useContents
import kotlinx.cinterop.usePinned
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.jetbrains.skia.Image
import org.jetbrains.skia.Paint
import org.jetbrains.skia.Rect
import org.jetbrains.skia.SamplingMode
import org.jetbrains.skia.Surface
import platform.AVFoundation.AVCaptureDeviceInput.Companion.deviceInputWithDevice
import platform.AVFoundation.*
import platform.CoreGraphics.CGPointMake
import platform.CoreGraphics.CGRectGetHeight
import platform.CoreGraphics.CGRectGetWidth
import platform.CoreGraphics.CGRectMake
import platform.Foundation.NSData
import platform.Foundation.NSError
import platform.Foundation.NSLog
import platform.Foundation.NSNotificationCenter
import platform.Foundation.NSOperationQueue
import platform.UIKit.UIView
import platform.darwin.NSObject
import platform.posix.memcpy
import com.tagaev.trrcrm.domain.computeTargetSize

private const val AfWaitTimeoutMs = 900L
private const val AfPollIntervalMs = 40L

@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
private class FixatorCameraPreviewView(
    session: AVCaptureSession,
) : UIView(frame = CGRectMake(0.0, 0.0, 0.0, 0.0)) {
    val previewLayer = AVCaptureVideoPreviewLayer(session = session).apply {
        videoGravity = AVLayerVideoGravityResizeAspectFill
    }

    init {
        layer.addSublayer(previewLayer)
    }

    override fun layoutSubviews() {
        super.layoutSubviews()
        previewLayer.frame = bounds
    }
}

@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
@Composable
actual fun FixatorCameraPreview(
    modifier: Modifier,
    onPhotoCaptured: (ByteArray) -> Unit,
    onControlsChanged: (FixatorCameraControls) -> Unit,
    onLog: (String) -> Unit,
) {
    val captureSession = remember { AVCaptureSession() }
    val photoOutput = remember { AVCapturePhotoOutput() }
    var captureDevice by remember { mutableStateOf<AVCaptureDevice?>(null) }
    var previewViewRef by remember { mutableStateOf<FixatorCameraPreviewView?>(null) }
    var configured by remember { mutableStateOf(false) }
    var isReady by remember { mutableStateOf(false) }
    var torchOn by remember { mutableStateOf(false) }
    var lastFocusNormalized by remember { mutableStateOf(0.5f to 0.5f) }
    val scope = rememberCoroutineScope()

    val onPhotoCapturedState = rememberUpdatedState(onPhotoCaptured)
    val onLogState = rememberUpdatedState(onLog)

    val photoCaptureDelegate = remember {
        object : NSObject(), AVCapturePhotoCaptureDelegateProtocol {
            override fun captureOutput(
                output: AVCapturePhotoOutput,
                didFinishProcessingPhoto: AVCapturePhoto,
                error: NSError?,
            ) {
                if (error != null) {
                    NSLog("CAMERA_FIXATOR: ios capture error=${error.localizedDescription}")
                    return
                }
                val photoData = didFinishProcessingPhoto.fileDataRepresentation() ?: run {
                    NSLog("CAMERA_FIXATOR: ios capture no jpeg data")
                    return
                }
                val bytes = photoData.toByteArray()
                onLogState.value("ios capture success size=${bytes.size}")
                onPhotoCapturedState.value(bytes)
            }
        }
    }

    LaunchedEffect(Unit) {
        if (configured) return@LaunchedEffect
        onLogState.value("ios configure requested")
        val device = resolveBackCamera()
        if (device == null) {
            onLogState.value("ios no video device")
            return@LaunchedEffect
        }
        captureDevice = device
        configureContinuousFocus(device, onLogState.value)

        val input = deviceInputWithDevice(device = device, error = null)
        if (input == null) {
            onLogState.value("ios failed to create input")
            return@LaunchedEffect
        }

        captureSession.beginConfiguration()
        if (captureSession.canAddInput(input)) {
            captureSession.addInput(input)
        }
        if (captureSession.canAddOutput(photoOutput)) {
            captureSession.addOutput(photoOutput)
        }
        captureSession.sessionPreset = AVCaptureSessionPresetPhoto
        captureSession.commitConfiguration()

        configured = true
        onLogState.value("ios configured torchAvailable=${device.hasTorch}")
    }

    LaunchedEffect(configured) {
        if (!configured) return@LaunchedEffect
        if (!captureSession.running) {
            captureSession.startRunning()
            isReady = captureSession.running
            onLogState.value("ios session started running=$isReady")
        }
    }

    DisposableEffect(captureDevice) {
        val device = captureDevice
        if (device == null) {
            return@DisposableEffect onDispose { }
        }
        val center = NSNotificationCenter.defaultCenter
        val observer = center.addObserverForName(
            name = AVCaptureDeviceSubjectAreaDidChangeNotification,
            `object` = device,
            queue = NSOperationQueue.mainQueue(),
        ) { _ ->
            configureContinuousFocus(device, onLogState.value)
            onLogState.value("ios subject area changed → continuous AF")
        }
        onDispose {
            center.removeObserver(observer)
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            onLogState.value("ios dispose")
            setTorch(device = captureDevice, enabled = false, onLog = onLogState.value)
            torchOn = false
            if (captureSession.running) {
                captureSession.stopRunning()
            }
        }
    }

    val torchAvailable = captureDevice?.hasTorch == true && captureDevice?.isTorchAvailable() == true

    fun focusAtNormalized(nx: Float, ny: Float, reason: String) {
        val device = captureDevice
        val preview = previewViewRef
        if (device == null || preview == null) {
            onLogState.value("ios focus skipped ($reason): not ready")
            return
        }
        val clampedX = nx.coerceIn(0f, 1f)
        val clampedY = ny.coerceIn(0f, 1f)
        lastFocusNormalized = clampedX to clampedY
        val viewWidth = CGRectGetWidth(preview.bounds)
        val viewHeight = CGRectGetHeight(preview.bounds)
        val viewPoint = CGPointMake(clampedX * viewWidth, clampedY * viewHeight)
        val devicePoint = preview.previewLayer.captureDevicePointOfInterestForPoint(viewPoint)
        val (deviceX, deviceY) = devicePoint.useContents { x to y }
        onLogState.value("ios focusAt ($reason) nx=$clampedX ny=$clampedY deviceX=$deviceX deviceY=$deviceY")
        applyFocusAndExposure(device, deviceX, deviceY, onLogState.value)
    }

    suspend fun waitForFocusSettle(reason: String) {
        val device = captureDevice ?: return
        var elapsed = 0L
        while (elapsed < AfWaitTimeoutMs) {
            if (!device.adjustingFocus) {
                onLogState.value("ios af settled ($reason) after ${elapsed}ms")
                return
            }
            delay(AfPollIntervalMs)
            elapsed += AfPollIntervalMs
        }
        onLogState.value("ios af wait timeout ($reason)")
    }

    LaunchedEffect(isReady, torchOn, torchAvailable, configured) {
        if (!isReady || !configured) return@LaunchedEffect
        onControlsChanged(
            FixatorCameraControls(
                capturePhoto = {
                    scope.launch {
                        onLogState.value("ios capture start")
                        val (nx, ny) = lastFocusNormalized
                        focusAtNormalized(nx, ny, "pre-capture")
                        waitForFocusSettle("pre-capture")
                        val settings = AVCapturePhotoSettings.photoSettings()
                        photoOutput.capturePhotoWithSettings(
                            settings = settings,
                            delegate = photoCaptureDelegate,
                        )
                    }
                },
                toggleTorch = {
                    if (!torchAvailable) {
                        onLogState.value("ios torch unavailable")
                        return@FixatorCameraControls
                    }
                    val next = !torchOn
                    val ok = setTorch(captureDevice, next, onLogState.value)
                    if (ok) {
                        torchOn = next
                        onLogState.value("ios torch=${if (next) "on" else "off"}")
                    }
                },
                focusAtNormalized = { x, y -> focusAtNormalized(x, y, "tap") },
                isTorchOn = torchOn,
                isTorchAvailable = torchAvailable,
            ),
        )
    }

    CameraFocusTapOverlay(
        modifier = modifier.background(MaterialTheme.colorScheme.surfaceVariant),
        enabled = isReady,
        onTapNormalized = { x, y -> focusAtNormalized(x, y, "tap") },
    ) {
        UIKitView(
            modifier = Modifier.fillMaxSize(),
            factory = {
                FixatorCameraPreviewView(session = captureSession).also { previewViewRef = it }
            },
            update = { view ->
                val preview = view as? FixatorCameraPreviewView
                if (preview != null) {
                    previewViewRef = preview
                    preview.setNeedsLayout()
                }
            },
        )
        if (!isReady) {
            Text(
                text = "Запуск камеры…",
                modifier = Modifier.align(Alignment.Center),
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

@OptIn(ExperimentalForeignApi::class)
private fun resolveBackCamera(): AVCaptureDevice? {
    val wide = runCatching {
        AVCaptureDevice.defaultDeviceWithDeviceType(
            deviceType = AVCaptureDeviceTypeBuiltInWideAngleCamera,
            mediaType = AVMediaTypeVideo,
            position = AVCaptureDevicePositionBack,
        )
    }.getOrNull()
    return wide ?: AVCaptureDevice.defaultDeviceWithMediaType(AVMediaTypeVideo)
}

@OptIn(ExperimentalForeignApi::class)
private fun configureContinuousFocus(device: AVCaptureDevice, onLog: (String) -> Unit) {
    runCatching {
        device.lockForConfiguration(null)
        if (device.isFocusModeSupported(AVCaptureFocusModeContinuousAutoFocus)) {
            device.focusMode = AVCaptureFocusModeContinuousAutoFocus
        }
        if (device.isExposureModeSupported(AVCaptureExposureModeContinuousAutoExposure)) {
            device.exposureMode = AVCaptureExposureModeContinuousAutoExposure
        }
        if (device.smoothAutoFocusSupported) {
            device.smoothAutoFocusEnabled = true
        }
        device.subjectAreaChangeMonitoringEnabled = true
        device.unlockForConfiguration()
        onLog("ios continuous AF configured")
    }.onFailure { error ->
        onLog("ios continuous AF failed: ${error.message}")
    }
}

@OptIn(ExperimentalForeignApi::class)
private fun applyFocusAndExposure(
    device: AVCaptureDevice,
    deviceX: Double,
    deviceY: Double,
    onLog: (String) -> Unit,
) {
    runCatching {
        device.lockForConfiguration(null)
        val point = CGPointMake(deviceX, deviceY)
        if (device.focusPointOfInterestSupported) {
            device.focusPointOfInterest = point
            if (device.isFocusModeSupported(AVCaptureFocusModeAutoFocus)) {
                device.focusMode = AVCaptureFocusModeAutoFocus
            }
        }
        if (device.exposurePointOfInterestSupported) {
            device.exposurePointOfInterest = point
            if (device.isExposureModeSupported(AVCaptureExposureModeAutoExpose)) {
                device.exposureMode = AVCaptureExposureModeAutoExpose
            }
        }
        device.unlockForConfiguration()
    }.onFailure { error ->
        onLog("ios focus/exposure apply failed: ${error.message}")
    }
}

@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
private fun setTorch(device: AVCaptureDevice?, enabled: Boolean, onLog: (String) -> Unit): Boolean {
    val captureDevice = device ?: return false
    if (!captureDevice.isTorchAvailable()) return false
    return runCatching {
        captureDevice.lockForConfiguration(null)
        captureDevice.torchMode = if (enabled) AVCaptureTorchModeOn else AVCaptureTorchModeOff
        captureDevice.unlockForConfiguration()
        true
    }.getOrElse { error ->
        onLog("ios torch config failed: ${error.message}")
        false
    }
}

@OptIn(ExperimentalForeignApi::class)
private fun NSData.toByteArray(): ByteArray {
    val length = this.length.toInt()
    if (length <= 0) return ByteArray(0)
    val bytes = ByteArray(length)
    bytes.usePinned { pinned ->
        memcpy(pinned.addressOf(0), this.bytes, this.length)
    }
    return bytes
}

@OptIn(ExperimentalForeignApi::class)
actual fun decodePhotoThumbnail(bytes: ByteArray): ImageBitmap? {
    return runCatching {
        if (bytes.isEmpty()) return@runCatching null
        val source = Image.makeFromEncoded(bytes) ?: return@runCatching null
        val (targetW, targetH) = computeTargetSize(
            source.width,
            source.height,
            PHOTO_DISPLAY_MAX_LONG_EDGE_PX,
        )
        val display = if (targetW != source.width || targetH != source.height) {
            resizeEncodedImage(source, targetW, targetH)
        } else {
            source
        }
        display.toComposeImageBitmap()
    }.getOrNull()
}

@OptIn(ExperimentalForeignApi::class)
private fun resizeEncodedImage(source: Image, targetW: Int, targetH: Int): Image {
    val surface = Surface.makeRasterN32Premul(targetW, targetH)
    val canvas = surface.canvas
    canvas.drawImageRect(
        source,
        Rect.makeWH(source.width.toFloat(), source.height.toFloat()),
        Rect.makeWH(targetW.toFloat(), targetH.toFloat()),
        SamplingMode.LINEAR,
        Paint(),
        true,
    )
    return surface.makeImageSnapshot()
}
