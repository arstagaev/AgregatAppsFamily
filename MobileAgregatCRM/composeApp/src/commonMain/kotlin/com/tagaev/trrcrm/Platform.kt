package com.tagaev.trrcrm

interface Platform {
    val name: String
    val baseDebugFCMurl: String
    val deviceSpecificInfo: String
}

expect fun getPlatform(): Platform
expect fun pushPlatformId(): String