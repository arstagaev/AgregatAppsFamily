package com.tagaev.trrcrm.ui.permissions

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.FocusMeteringAction
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.tagaev.trrcrm.domain.computeTargetSize
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.concurrent.TimeUnit

private const val AfWaitTimeoutMs = 1_000L

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
    val scope = rememberCoroutineScope()

    var previewView by remember { mutableStateOf<PreviewView?>(null) }
    var imageCapture by remember { mutableStateOf<ImageCapture?>(null) }
    var boundCamera by remember { mutableStateOf<Camera?>(null) }
    var torchOn by remember { mutableStateOf(false) }
    var isReady by remember { mutableStateOf(false) }
    var lastFocusNormalized by remember { mutableStateOf(0.5f to 0.5f) }

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
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
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

    fun focusAtNormalized(nx: Float, ny: Float, reason: String) {
        val view = previewView
        val camera = boundCamera
        if (view == null || camera == null) {
            onLogState.value("android focus skipped ($reason): camera not ready")
            return
        }
        if (view.width <= 0 || view.height <= 0) {
            onLogState.value("android focus skipped ($reason): preview size 0")
            return
        }
        val clampedX = nx.coerceIn(0f, 1f)
        val clampedY = ny.coerceIn(0f, 1f)
        val x = clampedX * view.width
        val y = clampedY * view.height
        lastFocusNormalized = clampedX to clampedY
        onLogState.value("android focusAt ($reason) nx=$clampedX ny=$clampedY px=$x py=$y")
        runCatching {
            val point = view.meteringPointFactory.createPoint(x, y)
            val action = FocusMeteringAction.Builder(
                point,
                FocusMeteringAction.FLAG_AF or FocusMeteringAction.FLAG_AE,
            )
                .setAutoCancelDuration(3, TimeUnit.SECONDS)
                .build()
            camera.cameraControl.startFocusAndMetering(action)
        }.onFailure { error ->
            onLogState.value("android focus failed: ${error.message}")
        }
    }

    suspend fun awaitFocusThen(reason: String): Boolean {
        val view = previewView
        val camera = boundCamera
        if (view == null || camera == null) return false
        if (view.width <= 0 || view.height <= 0) {
            onLogState.value("android af wait skipped ($reason): preview size 0")
            return false
        }
        val (nx, ny) = lastFocusNormalized
        val x = nx * view.width
        val y = ny * view.height
        return runCatching {
            val point = view.meteringPointFactory.createPoint(x, y)
            val action = FocusMeteringAction.Builder(
                point,
                FocusMeteringAction.FLAG_AF or FocusMeteringAction.FLAG_AE,
            )
                .disableAutoCancel()
                .build()
            val future = camera.cameraControl.startFocusAndMetering(action)
            val result = withContext(Dispatchers.IO) {
                runCatching {
                    future.get(AfWaitTimeoutMs, TimeUnit.MILLISECONDS)
                }.getOrElse { error ->
                    future.cancel(true)
                    onLogState.value("android af wait timeout/fail ($reason): ${error.message}")
                    null
                }
            }
            val ok = result?.isFocusSuccessful == true
            if (result != null) {
                onLogState.value("android af wait done ($reason) success=$ok")
            }
            true
        }.getOrElse { error ->
            onLogState.value("android af wait failed ($reason): ${error.message}")
            false
        }
    }

    LaunchedEffect(isReady, torchOn, torchAvailable, imageCapture) {
        val capture = imageCapture
        if (!isReady || capture == null) return@LaunchedEffect
        onControlsChanged(
            FixatorCameraControls(
                capturePhoto = {
                    scope.launch {
                        onLogState.value("android capture start")
                        awaitFocusThen("pre-capture")
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
                    }
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
    if (bytes.isEmpty()) return null
    return try {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
        val srcW = bounds.outWidth
        val srcH = bounds.outHeight
        if (srcW <= 0 || srcH <= 0) {
            CameraFixatorLog.d(
                "image_decoder decoder=bitmapfactory outMimeType=${bounds.outMimeType} out=${srcW}x${srcH} reason=bounds_undecodable",
            )
            return null
        }

        var sampleSize = 1
        val longEdge = maxOf(srcW, srcH)
        while (longEdge / sampleSize > PHOTO_DISPLAY_MAX_LONG_EDGE_PX) {
            sampleSize *= 2
        }

        val options = BitmapFactory.Options().apply { inSampleSize = sampleSize }
        val decoded = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)
        if (decoded == null) {
            CameraFixatorLog.d(
                "image_decoder decoder=bitmapfactory outMimeType=${bounds.outMimeType} out=${srcW}x${srcH} reason=decodeByteArray_null",
            )
            return null
        }

        val (targetW, targetH) = computeTargetSize(
            decoded.width,
            decoded.height,
            PHOTO_DISPLAY_MAX_LONG_EDGE_PX,
        )
        val display = if (targetW != decoded.width || targetH != decoded.height) {
            Bitmap.createScaledBitmap(decoded, targetW, targetH, true).also {
                if (it !== decoded) decoded.recycle()
            }
        } else {
            decoded
        }
        display.asImageBitmap()
    } catch (error: Throwable) {
        CameraFixatorLog.d("image_decoder decoder=bitmapfactory message=${error.message}")
        null
    }
}
