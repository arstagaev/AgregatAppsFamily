package com.tagaev.trrcrm.updates

import com.tagaev.trrcrm.data.AppSettings
import io.ktor.client.HttpClient

actual fun createDesktopUpdateService(
    client: HttpClient,
    settings: AppSettings
): DesktopUpdateService {
    val osName = System.getProperty("os.name").orEmpty().lowercase()
    return if (osName.contains("windows")) {
        DesktopUpdateServiceImpl(client, settings)
    } else {
        NoOpDesktopUpdateService()
    }
}
