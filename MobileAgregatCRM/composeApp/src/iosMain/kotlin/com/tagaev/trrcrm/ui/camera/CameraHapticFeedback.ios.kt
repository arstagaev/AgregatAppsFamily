package com.tagaev.trrcrm.ui.camera

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import platform.UIKit.UIImpactFeedbackGenerator
import platform.UIKit.UIImpactFeedbackStyle

private const val UPLOAD_SECOND_PULSE_DELAY_MS = 45L

actual fun performCameraHapticFeedback(strength: CameraHapticFeedbackStrength) {
    when (strength) {
        CameraHapticFeedbackStrength.PhotoCaptured -> {
            UIImpactFeedbackGenerator(UIImpactFeedbackStyle.UIImpactFeedbackStyleLight).impactOccurred()
        }
        CameraHapticFeedbackStrength.UploadSucceeded -> {
            val generator = UIImpactFeedbackGenerator(UIImpactFeedbackStyle.UIImpactFeedbackStyleLight)
            generator.prepare()
            generator.impactOccurred()
            CoroutineScope(Dispatchers.Main).launch {
                delay(UPLOAD_SECOND_PULSE_DELAY_MS)
                generator.impactOccurred()
            }
        }
    }
}
