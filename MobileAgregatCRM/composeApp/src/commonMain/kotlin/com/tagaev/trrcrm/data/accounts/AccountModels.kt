package com.tagaev.trrcrm.data.accounts

import kotlinx.serialization.Serializable

const val MAX_SAVED_ACCOUNTS = 3
const val ACCOUNT_PIN_LENGTH = 4
const val PIN_ATTEMPTS_PER_WINDOW = 10
const val PIN_WARN_AFTER_FAILURES = 5
const val PIN_LOCK_FIRST_MS = 10L * 60L * 1000L
const val PIN_LOCK_SECOND_MS = 60L * 60L * 1000L

@Serializable
data class AccountSlot(
    val id: String,
    val login: String = "",
    val fullName: String = "",
    val department: String = "",
    val token: String = "",
    val coreSessionId: String = "",
    val pinSalt: String = "",
    val pinHash: String = "",
) {
    val hasPin: Boolean get() = pinSalt.isNotBlank() && pinHash.isNotBlank()
    val displayName: String get() = fullName.ifBlank { login.ifBlank { id } }
    val initials: String
        get() {
            val parts = displayName.trim().split(Regex("\\s+")).filter { it.isNotBlank() }
            if (parts.isEmpty()) return "?"
            val letters = parts.take(2).mapNotNull { part ->
                part.firstOrNull { it.isLetterOrDigit() }?.uppercaseChar()
            }
            return letters.joinToString("").ifBlank { "?" }
        }
}

@Serializable
data class AccountStoreState(
    val accounts: List<AccountSlot> = emptyList(),
    val activeAccountId: String? = null,
    val pendingCreateNewSlot: Boolean = false,
)

@Serializable
data class PinLockoutState(
    val failedAttemptsInWindow: Int = 0,
    val lockLevel: Int = 0,
    val lockedUntilMs: Long = 0L,
)

sealed interface PinVerifyResult {
    data object Ok : PinVerifyResult
    data class Wrong(
        val remaining: Int,
        val failedInWindow: Int,
        val warnAfterFive: Boolean,
    ) : PinVerifyResult
    data class Locked(val untilMs: Long, val isHourLock: Boolean) : PinVerifyResult
}

sealed interface SetPinResult {
    data object Ok : SetPinResult
    data object InvalidPin : SetPinResult
    data object AccountMissing : SetPinResult
}

sealed interface AddAccountResult {
    data class Created(val slot: AccountSlot) : AddAccountResult
    data class Reused(val slot: AccountSlot) : AddAccountResult
    data object LimitReached : AddAccountResult
}
