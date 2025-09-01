package org.agregatcrm

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform