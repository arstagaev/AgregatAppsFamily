package com.tagaev.trrcrm.ui.login

import com.tagaev.trrcrm.data.remote.isTokenAuthenticationError
import com.tagaev.trrcrm.ui.root.IRootComponent

object SessionExpiryBridge {
    private var root: IRootComponent? = null
    private var suppressCount: Int = 0

    fun setRoot(component: IRootComponent?) {
        root = component
    }

    fun suppress() {
        suppressCount++
    }

    fun release() {
        if (suppressCount > 0) suppressCount--
    }

    suspend inline fun <T> suppressing(block: suspend () -> T): T {
        suppress()
        return try {
            block()
        } finally {
            release()
        }
    }

    fun notifyIfExpired(message: String?) {
        if (suppressCount > 0) return
        if (!isTokenAuthenticationError(message)) return
        root?.onSessionExpired()
    }
}
