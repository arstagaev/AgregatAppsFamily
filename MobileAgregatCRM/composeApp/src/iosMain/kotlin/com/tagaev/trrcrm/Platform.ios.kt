package com.tagaev.trrcrm

import platform.Foundation.NSUUID
import platform.Foundation.NSUserDefaults
import platform.UIKit.UIDevice

class IOSPlatform : Platform {
    override val name: String = UIDevice.currentDevice.systemName() + " " + UIDevice.currentDevice.systemVersion
    override val baseDebugFCMurl: String = "http://localhost:8000"
    override val deviceSpecificInfo: String = buildDeviceSpecificInfo()
}

private fun buildDeviceSpecificInfo(): String {
    val device = UIDevice.currentDevice

    // e.g. "Arsen’s iPhone" -> "Arsens_iPhone"
    val rawName = device.name ?: "iOS"
    val cleanedName = rawName
        .replace(" ", "_")
        .replace("’", "")
        .replace("'", "")

    // Prefer system identifierForVendor (stable per vendor + device)
    val vendorId = device.identifierForVendor?.UUIDString

    // Fallback: our own UUID stored in NSUserDefaults
    val defaults = NSUserDefaults.standardUserDefaults
    val key = "fallback_device_id"

    val storedId = defaults.stringForKey(key)
    val fallbackId = storedId ?: NSUUID().UUIDString().also { newId ->
        defaults.setObject(newId, forKey = key)
        defaults.synchronize()
    }

    val finalId = vendorId ?: fallbackId

    // Keep it readable, only last 6 chars is enough to distinguish devices
    val shortId = if (finalId.length > 6) finalId.takeLast(6) else finalId

    return "${cleanedName}_$shortId"
}

actual fun getPlatform(): Platform = IOSPlatform()
actual fun pushPlatformId(): String = "ios"
