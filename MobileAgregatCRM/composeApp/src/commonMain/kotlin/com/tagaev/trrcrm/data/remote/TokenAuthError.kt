package com.tagaev.trrcrm.data.remote

/** Backend message returned by token-protected 1C endpoints for an expired token. */
fun isTokenAuthenticationError(message: String?): Boolean {
    val normalized = message?.trim().orEmpty()
    if (normalized.isBlank()) return false
    return normalized.equals("Token authentification error", ignoreCase = true) ||
        normalized.equals("Token authentication error", ignoreCase = true)
}
