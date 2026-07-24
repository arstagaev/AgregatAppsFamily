package com.tagaev.trrcrm.ui.camera

enum class CameraHapticFeedbackStrength {
    /** Мини-вибро при успешном снимке. */
    PhotoCaptured,

    /** Мини-вибро при успешной отправке (тот же лёгкий импульс). */
    UploadSucceeded,
}

expect fun performCameraHapticFeedback(strength: CameraHapticFeedbackStrength)
