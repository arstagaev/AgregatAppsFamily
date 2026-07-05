package com.tagaev.trrcrm.ui.permissions

import android.graphics.BitmapFactory
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import java.io.File

@Composable
actual fun FixatorCameraPreview(
    modifier: Modifier,
    onPhotoCaptured: (ByteArray) -> Unit,
    onControlsChanged: (FixatorCameraControls) -> Unit,
    onLog: (String) -> Unit,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val mainExecutor = remember(context) { ContextCompat.getMainExecutor(context) }

    var previewView by remember { mutableStateOf<PreviewView?>(null) }
    var imageCapture by remember { mutableStateOf<ImageCapture?>(null) }
    var boundCamera by remember { mutableStateOf<Camera?>(null) }
    var torchOn by remember { mutableStateOf(false) }
    var isReady by remember { mutableStateOf(false) }

    val onPhotoCapturedState = rememberUpdatedState(onPhotoCaptured)
    val onLogState = rememberUpdatedState(onLog)

    LaunchedEffect(previewView, lifecycleOwner) {
        val view = previewView ?: return@LaunchedEffect
        onLogState.value("android bind requested")
        val cameraProvider = ProcessCameraProvider.getInstance(context).get()
        val preview = Preview.Builder().build().also {
            it.surfaceProvider = view.surfaceProvider
        }
        val capture = ImageCapture.Builder()
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
            .build()

        runCatching {
            cameraProvider.unbindAll()
            val camera = cameraProvider.bindToLifecycle(
                lifecycleOwner,
                CameraSelector.DEFAULT_BACK_CAMERA,
                preview,
                capture,
            )
            imageCapture = capture
            boundCamera = camera
            isReady = true
            onLogState.value("android camera bound torchAvailable=${camera.cameraInfo.hasFlashUnit()}")
        }.onFailure { error ->
            isReady = false
            onLogState.value("android bind failed: ${error.message}")
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            onLogState.value("android unbind")
            runCatching {
                boundCamera?.cameraControl?.enableTorch(false)
                ProcessCameraProvider.getInstance(context).get().unbindAll()
            }
            imageCapture = null
            boundCamera = null
            isReady = false
            torchOn = false
        }
    }

    val torchAvailable = boundCamera?.cameraInfo?.hasFlashUnit() == true

    LaunchedEffect(isReady, torchOn, torchAvailable, imageCapture) {
        val capture = imageCapture
        if (!isReady || capture == null) return@LaunchedEffect
        onControlsChanged(
            FixatorCameraControls(
                capturePhoto = {
                    onLogState.value("android capture start")
                    val outputFile = File(context.cacheDir, "fixator_${System.currentTimeMillis()}.jpg")
                    val outputOptions = ImageCapture.OutputFileOptions.Builder(outputFile).build()
                    capture.takePicture(
                        outputOptions,
                        mainExecutor,
                        object : ImageCapture.OnImageSavedCallback {
                            override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                                runCatching {
                                    val bytes = outputFile.readBytes()
                                    outputFile.delete()
                                    onLogState.value("android capture success size=${bytes.size}")
                                    onPhotoCapturedState.value(bytes)
                                }.onFailure { error ->
                                    onLogState.value("android capture read failed: ${error.message}")
                                }
                            }

                            override fun onError(exception: ImageCaptureException) {
                                outputFile.delete()
                                onLogState.value("android capture error: ${exception.message}")
                            }
                        },
                    )
                },
                toggleTorch = {
                    if (!torchAvailable) {
                        onLogState.value("android torch unavailable")
                        return@FixatorCameraControls
                    }
                    val next = !torchOn
                    runCatching {
                        boundCamera?.cameraControl?.enableTorch(next)
                        torchOn = next
                        onLogState.value("android torch=${if (next) "on" else "off"}")
                    }.onFailure { error ->
                        onLogState.value("android torch failed: ${error.message}")
                    }
                },
                isTorchOn = torchOn,
                isTorchAvailable = torchAvailable,
            ),
        )
    }

    Box(modifier = modifier.background(MaterialTheme.colorScheme.surfaceVariant)) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                PreviewView(ctx).also { previewView = it }
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

actual fun decodePhotoThumbnail(bytes: ByteArray): ImageBitmap? {
    return runCatching {
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap()
    }.getOrNull()
}
