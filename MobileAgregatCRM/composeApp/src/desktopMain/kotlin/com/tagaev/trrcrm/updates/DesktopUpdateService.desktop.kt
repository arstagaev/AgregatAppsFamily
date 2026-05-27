package com.tagaev.trrcrm.updates

import com.tagaev.trrcrm.data.AppSettings
import io.ktor.client.HttpClient

actual fun createDesktopUpdateService(
    client: HttpClient,
    settings: AppSettings
): DesktopUpdateService = DesktopUpdateServiceImpl(client, settings)
