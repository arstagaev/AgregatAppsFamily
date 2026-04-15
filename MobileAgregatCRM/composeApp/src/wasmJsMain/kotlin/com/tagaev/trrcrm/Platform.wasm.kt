package com.tagaev.trrcrm

private class WebPlatform : Platform {
    override val name: String = "Web (Wasm)"
    override val baseDebugFCMurl: String = "http://localhost:8000"
    override val deviceSpecificInfo: String = "web-client"
}

actual fun getPlatform(): Platform = WebPlatform()

