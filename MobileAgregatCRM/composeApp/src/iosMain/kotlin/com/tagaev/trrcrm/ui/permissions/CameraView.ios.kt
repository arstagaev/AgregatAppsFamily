package com.tagaev.trrcrm.ui.permissions

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.UIKitView
import kotlinx.cinterop.ExperimentalForeignApi
import platform.AVFoundation.AVCaptureConnection
import platform.AVFoundation.AVCaptureDevice
import platform.AVFoundation.AVCaptureDeviceInput
import platform.AVFoundation.AVCaptureMetadataOutput
import platform.AVFoundation.AVCaptureMetadataOutputObjectsDelegateProtocol
import platform.AVFoundation.AVCaptureOutput
import platform.AVFoundation.AVCaptureSession
import platform.AVFoundation.AVCaptureSessionPresetHigh
import platform.AVFoundation.AVCaptureVideoPreviewLayer
import platform.AVFoundation.AVLayerVideoGravityResizeAspectFill
import platform.AVFoundation.AVMediaTypeVideo
import platform.AVFoundation.AVMetadataMachineReadableCodeObject
import platform.AVFoundation.AVMetadataObjectTypeQRCode
import platform.AudioToolbox.AudioServicesPlaySystemSound
import platform.AudioToolbox.kSystemSoundID_Vibrate
import platform.Foundation.NSLog
import platform.UIKit.UIScreen
import platform.UIKit.UIView
import platform.darwin.NSObject
import platform.darwin.dispatch_get_main_queue

@OptIn(ExperimentalForeignApi::class)
@Composable
actual fun CameraView(
    decodedString: (String) -> Unit,
    autoStart: Boolean
) {
    // Create and remember the capture session for this composable lifecycle
    val captureSession = remember { AVCaptureSession() }
    var configured by remember { mutableStateOf(false) }
    var metadataDelegate by remember { mutableStateOf<ScannerMetadataOutputDelegate?>(null) }
    var lastScanned by remember { mutableStateOf<String?>(null) }

    // Configure session only once
    LaunchedEffect(Unit) {
        NSLog("STARTED QR CODE READER")
        if (!configured) {
            setupCaptureSession(
                session = captureSession,
                onScanResult = { code ->
                    lastScanned = code
                    decodedString(code)
                },
                onDelegateCreated = { createdDelegate ->
                    metadataDelegate = createdDelegate
                }
            )
            configured = true
        }
        if (!captureSession.running) {
            captureSession.startRunning()
        }
    }

    // Stop camera when composable leaves composition
    DisposableEffect(Unit) {
        onDispose {
            if (captureSession.running) {
                NSLog("END QR CODE READER")
                captureSession.stopRunning()
            }
        }
    }

    // Render native camera preview with restart button overlay
    Box(modifier = Modifier.fillMaxSize()) {
        UIKitView(
            modifier = Modifier.fillMaxSize(),
            factory = {
                val root = UIView(frame = UIScreen.mainScreen.bounds)

                val previewLayer = AVCaptureVideoPreviewLayer(session = captureSession).apply {
                    videoGravity = AVLayerVideoGravityResizeAspectFill
                    frame = root.bounds
                }

                root.layer.addSublayer(previewLayer)
                root
            },
            update = { view ->
                // Keep preview layer in sync with view size
                (view.layer.sublayers?.firstOrNull { it is AVCaptureVideoPreviewLayer }
                        as? AVCaptureVideoPreviewLayer
                        )?.frame = view.bounds
            }
        )

        if (lastScanned != null) {
            Button(
                onClick = {
                    lastScanned = null
                    if (!captureSession.running) {
                        captureSession.startRunning()
                    }
                },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 32.dp)
            ) {
                Text("Сканировать ещё")
            }
        }
    }
}

class ScannerMetadataOutputDelegate(
    private val onScanResult: (String) -> Unit
) : NSObject(), AVCaptureMetadataOutputObjectsDelegateProtocol {

    override fun captureOutput(
        output: AVCaptureOutput,
        didOutputMetadataObjects: List<*>,
        fromConnection: AVCaptureConnection
    ) {
        // Called every time AVFoundation detects supported metadata (QR, etc.)
        val metadata = didOutputMetadataObjects.firstOrNull()
                as? AVMetadataMachineReadableCodeObject ?: return

        if (metadata.type == AVMetadataObjectTypeQRCode) {
            val code = metadata.stringValue ?: return

            // Vibrate once for feedback (optional)
            AudioServicesPlaySystemSound(kSystemSoundID_Vibrate)

            NSLog("QR scanned: $code")
            onScanResult(code)
        }
    }
}

@OptIn(ExperimentalForeignApi::class)
private fun setupCaptureSession(
    session: AVCaptureSession,
    onScanResult: (String) -> Unit,
    onDelegateCreated: (ScannerMetadataOutputDelegate) -> Unit
) {
    // 1. Camera device
    val device = AVCaptureDevice.defaultDeviceWithMediaType(AVMediaTypeVideo)
    if (device == null) {
        NSLog("No video device available")
        return
    }

    // 2. Input (camera)
    val input = AVCaptureDeviceInput.deviceInputWithDevice(device, error = null)
    if (input == null) {
        NSLog("Failed to create camera input")
        return
    }

    if (session.canAddInput(input)) {
        session.addInput(input)
    } else {
        NSLog("Cannot add camera input to session")
    }

    // 3. Output (metadata – QR codes)
    val metadataOutput = AVCaptureMetadataOutput()
    if (session.canAddOutput(metadataOutput)) {
        session.addOutput(metadataOutput)

        val delegate = ScannerMetadataOutputDelegate { code ->
            // Pass value up to composable
            onScanResult(code)
            // Stop scanning after first successful scan; user must press the
            // restart button in the composable to start scanning again.
            if (session.running) {
                session.stopRunning()
            }
        }
        // Hold a strong reference to the delegate so it is not deallocated,
        // because AVCaptureMetadataOutput keeps its delegate as a weak reference.
        onDelegateCreated(delegate)

        metadataOutput.setMetadataObjectsDelegate(
            objectsDelegate = delegate,
            queue = dispatch_get_main_queue()
        )
        // Only look for QR codes
        metadataOutput.metadataObjectTypes = listOf(AVMetadataObjectTypeQRCode)
    } else {
        NSLog("Cannot add metadata output to session")
    }

    // Session preset – good quality enough for QR scanning
    session.sessionPreset = AVCaptureSessionPresetHigh
}
