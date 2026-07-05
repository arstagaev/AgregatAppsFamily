package com.tagaev.trrcrm.ui.camera

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import org.koin.core.context.GlobalContext

private const val PHOTO_PULSE_MS = 12L
private const val PHOTO_AMPLITUDE = 28
private const val UPLOAD_PULSE_GAP_MS = 45L

actual fun performCameraHapticFeedback(strength: CameraHapticFeedbackStrength) {
    val vibrator = appVibrator() ?: return
    if (!vibrator.hasVibrator()) return

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        val effect = when (strength) {
            CameraHapticFeedbackStrength.PhotoCaptured -> {
                VibrationEffect.createOneShot(PHOTO_PULSE_MS, PHOTO_AMPLITUDE)
            }
            CameraHapticFeedbackStrength.UploadSucceeded -> {
                VibrationEffect.createWaveform(
                    longArrayOf(0, PHOTO_PULSE_MS, UPLOAD_PULSE_GAP_MS, PHOTO_PULSE_MS),
                    intArrayOf(0, PHOTO_AMPLITUDE, 0, PHOTO_AMPLITUDE),
                    -1,
                )
            }
        }
        vibrator.vibrate(effect)
    } else {
        @Suppress("DEPRECATION")
        when (strength) {
            CameraHapticFeedbackStrength.PhotoCaptured -> vibrator.vibrate(PHOTO_PULSE_MS)
            CameraHapticFeedbackStrength.UploadSucceeded -> {
                vibrator.vibrate(longArrayOf(0, PHOTO_PULSE_MS, UPLOAD_PULSE_GAP_MS, PHOTO_PULSE_MS), -1)
            }
        }
    }
}

private fun appVibrator(): Vibrator? {
    val context = GlobalContext.get().get<Context>()
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val manager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
        manager?.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
    }
}
