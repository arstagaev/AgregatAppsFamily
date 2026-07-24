package com.tagaev.trrcrm.ui.camera

import platform.UIKit.UIImpactFeedbackGenerator
import platform.UIKit.UIImpactFeedbackStyle

actual fun performCameraHapticFeedback(strength: CameraHapticFeedbackStrength) {
    when (strength) {
        CameraHapticFeedbackStrength.PhotoCaptured,
        CameraHapticFeedbackStrength.UploadSucceeded,
        -> {
            UIImpactFeedbackGenerator(UIImpactFeedbackStyle.UIImpactFeedbackStyleLight).impactOccurred()
        }
    }
}
