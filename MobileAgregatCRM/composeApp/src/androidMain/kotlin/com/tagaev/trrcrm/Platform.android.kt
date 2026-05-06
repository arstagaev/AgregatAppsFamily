package com.tagaev.trrcrm

import android.os.Build
import com.tagaev.trrcrm.utils.DeviceIdProvider
import org.koin.core.component.KoinComponent
import org.koin.core.component.get

class AndroidPlatform(private val deviceIdProvider: DeviceIdProvider) : Platform {
    override val name: String = "Android ${Build.VERSION.SDK_INT}"
    override val baseDebugFCMurl: String = "http://10.0.2.2:8000"
    override val deviceSpecificInfo: String
        get() {
            val manufacturer = Build.MANUFACTURER.orEmpty().replace(" ", "")
            val model = Build.MODEL.orEmpty().replace(" ", "")
            val friendlyName = "$manufacturer-$model" // e.g. "Google-Pixel7"
            val shortId = deviceIdProvider.deviceId.takeLast(6)
            return "${friendlyName}_$shortId"
        }
}
/**
 * Small holder object so we can access Koin here.
 */
private object AndroidPlatformHolder : KoinComponent {
    val instance: Platform by lazy {
        AndroidPlatform(get()) // get<DeviceIdProvider>() from Koin
    }
}

actual fun getPlatform(): Platform = AndroidPlatformHolder.instance
actual fun pushPlatformId(): String = "android"
