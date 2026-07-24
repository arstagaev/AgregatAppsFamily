package com.tagaev.trrcrm.ui.camera

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import org.koin.core.context.GlobalContext

private const val MINI_PULSE_MS = 12L
private const val MINI_AMPLITUDE = 28

actual fun performCameraHapticFeedback(strength: CameraHapticFeedbackStrength) {
    val vibrator = appVibrator() ?: return
    if (!vibrator.hasVibrator()) return

    when (strength) {
        CameraHapticFeedbackStrength.PhotoCaptured,
        CameraHapticFeedbackStrength.UploadSucceeded,
        -> {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(MINI_PULSE_MS, MINI_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(MINI_PULSE_MS)
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
