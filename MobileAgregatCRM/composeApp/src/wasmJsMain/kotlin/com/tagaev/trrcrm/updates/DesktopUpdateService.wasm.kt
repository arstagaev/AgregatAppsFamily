package com.tagaev.trrcrm.updates

import io.ktor.client.HttpClient

actual fun createDesktopUpdateService(client: HttpClient): DesktopUpdateService = NoOpDesktopUpdateService()

