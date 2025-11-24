package com.tagaev.trrcrm

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform