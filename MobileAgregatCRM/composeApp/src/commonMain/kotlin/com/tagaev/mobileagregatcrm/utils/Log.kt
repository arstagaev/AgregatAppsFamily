package org.agregatcrm.utils

import io.ktor.client.plugins.logging.Logger
import io.ktor.http.decodeURLPart

object Log {
    fun info(message: String) {
        println("Log: ${message}")
    }
}



class HumanLogger : Logger {
    override fun log(message: String) {
        // decode only if it looks like a URL or contains % encoding
        val decoded = if (message.contains('%')) {
            runCatching { message.decodeURLPart() }.getOrDefault(message)
        } else {
            message
        }
        println(decoded)
    }
}