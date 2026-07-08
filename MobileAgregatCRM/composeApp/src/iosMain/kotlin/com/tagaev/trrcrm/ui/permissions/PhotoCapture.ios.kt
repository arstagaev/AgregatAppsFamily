package com.tagaev.trrcrm.ui.permissions

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import kotlinx.cinterop.usePinned
import org.jetbrains.skia.Image
import platform.AVFoundation.AVCaptureDeviceInput.Companion.deviceInputWithDevice
import platform.AVFoundation.*
import platform.CoreGraphics.CGRectMake
import platform.Foundation.NSData
import platform.Foundation.NSError
import platform.Foundation.NSLog
import platform.UIKit.UIView
import platform.darwin.NSObject

@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
private class FixatorCameraPreviewView(
    session: AVCaptureSession,
) : UIView(frame = CGRectMake(0.0, 0.0, 0.0, 0.0)) {
    private val previewLayer = AVCaptureVideoPreviewLayer(session = session).apply {
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
    var configured by remember { mutableStateOf(false) }
    var isReady by remember { mutableStateOf(false) }
    var torchOn by remember { mutableStateOf(false) }

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
        val device = AVCaptureDevice.defaultDeviceWithMediaType(AVMediaTypeVideo)
        if (device == null) {
            onLogState.value("ios no video device")
            return@LaunchedEffect
        }
        captureDevice = device

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
        onLogState.value("ios configured torchAvailable=${device.isTorchAvailable()}")
    }

    LaunchedEffect(configured) {
        if (!configured) return@LaunchedEffect
        if (!captureSession.running) {
            captureSession.startRunning()
            isReady = captureSession.running
            onLogState.value("ios session started running=$isReady")
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

    val torchAvailable = captureDevice?.isTorchAvailable() == true

    LaunchedEffect(isReady, torchOn, torchAvailable, configured) {
        if (!isReady || !configured) return@LaunchedEffect
        onControlsChanged(
            FixatorCameraControls(
                capturePhoto = {
                    onLogState.value("ios capture start")
                    val settings = AVCapturePhotoSettings.photoSettings()
                    photoOutput.capturePhotoWithSettings(
                        settings = settings,
                        delegate = photoCaptureDelegate,
                    )
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
                isTorchOn = torchOn,
                isTorchAvailable = torchAvailable,
            ),
        )
    }

    Box(modifier = modifier.background(MaterialTheme.colorScheme.surfaceVariant)) {
        UIKitView(
            modifier = Modifier.fillMaxSize(),
            factory = {
                FixatorCameraPreviewView(session = captureSession)
            },
            update = { view ->
                (view as? FixatorCameraPreviewView)?.setNeedsLayout()
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
        platform.posix.memcpy(pinned.addressOf(0), this.bytes, this.length)
    }
    return bytes
}

@OptIn(ExperimentalForeignApi::class)
actual fun decodePhotoThumbnail(bytes: ByteArray): ImageBitmap? {
    return runCatching {
        Image.makeFromEncoded(bytes).toComposeImageBitmap()
    }.getOrNull()
}
