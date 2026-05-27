package com.tagaev.trrcrm.push

actual fun disablePushDeliveryForLoggedOutUser() {
    // Android push delivery is tied to FCM token and backend routing.
    // Keep this as a safe no-op; logout cleanup is handled by shared state reset and backend logout.
}

