package com.tagaev.trrcrm

private class DesktopPlatform : Platform {
    override val name: String = buildString {
        append("Desktop ")
        append(System.getProperty("os.name").orEmpty())
        append(" ")
        append(System.getProperty("os.version").orEmpty())
    }.trim()

    override val baseDebugFCMurl: String = "http://localhost:8000"
    override val deviceSpecificInfo: String =
        "${System.getProperty("os.name").orEmpty().replace(" ", "_")}-desktop"
}

actual fun getPlatform(): Platform = DesktopPlatform()
