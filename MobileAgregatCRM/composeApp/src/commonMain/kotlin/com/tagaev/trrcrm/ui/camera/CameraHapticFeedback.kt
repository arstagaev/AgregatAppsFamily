package com.tagaev.trrcrm.ui.camera

enum class CameraHapticFeedbackStrength {
    /** Мягкая вибрация при успешном снимке (базовая сила). */
    PhotoCaptured,

    /** Чуть сильнее при успешной отправке (~×2 от снимка, всё ещё мягко). */
    UploadSucceeded,
}

expect fun performCameraHapticFeedback(strength: CameraHapticFeedbackStrength)
