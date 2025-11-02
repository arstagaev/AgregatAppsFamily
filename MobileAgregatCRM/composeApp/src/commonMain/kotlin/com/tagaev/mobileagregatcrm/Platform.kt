package com.tagaev.mobileagregatcrm

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform