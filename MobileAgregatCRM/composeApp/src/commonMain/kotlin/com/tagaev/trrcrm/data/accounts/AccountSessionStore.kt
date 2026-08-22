package com.tagaev.trrcrm.data.accounts

import com.tagaev.trrcrm.data.AppSettings
import com.tagaev.trrcrm.data.AppSettingsKeys
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.json.Json
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

class AccountSessionStore(
    private val settings: AppSettings,
    private val json: Json,
    private val nowMs: () -> Long = defaultNowMs,
) {
    private val _snapshot = MutableStateFlow(AccountStoreState())
    val snapshotFlow: StateFlow<AccountStoreState> = _snapshot.asStateFlow()

    init {
        _snapshot.value = loadState()
    }

    fun snapshot(): AccountStoreState = loadState()

    fun accounts(): List<AccountSlot> = loadState().accounts

    fun activeAccount(): AccountSlot? {
        val state = loadState()
        return state.accounts.firstOrNull { it.id == state.activeAccountId }
            ?: state.accounts.firstOrNull()
    }

    fun canAddAccount(): Boolean = loadState().accounts.size < MAX_SAVED_ACCOUNTS

    fun needsPinGate(): Boolean {
        val accounts = loadState().accounts
        return accounts.size >= 2 && accounts.any { !it.hasPin }
    }

    fun allPinsReady(): Boolean {
        val accounts = loadState().accounts
        return accounts.size < 2 || accounts.all { it.hasPin }
    }

    fun ensureMigrated() {
        val state = loadState()
        if (state.accounts.isNotEmpty()) return
        val token = settings.getStringOrNull(AppSettingsKeys.TOKEN_KEY).orEmpty()
        if (token.isBlank()) return
        val slot = AccountSlot(
            id = AccountPinCrypto.newAccountId(),
            login = settings.getStringOrNull(AppSettingsKeys.ACCOUNT_LOGIN).orEmpty()
                .ifBlank { settings.getStringOrNull(AppSettingsKeys.EMAIL).orEmpty() },
            fullName = settings.getStringOrNull(AppSettingsKeys.PERSONAL_DATA).orEmpty(),
            department = settings.getStringOrNull(AppSettingsKeys.DEPARTMENT).orEmpty(),
            token = token,
            coreSessionId = settings.getStringOrNull(AppSettingsKeys.CORE_SESSION_ID).orEmpty(),
        )
        saveState(
            AccountStoreState(
                accounts = listOf(slot),
                activeAccountId = slot.id,
                pendingCreateNewSlot = false,
            )
        )
    }

    fun markPendingCreateNewSlot() {
        saveState(loadState().copy(pendingCreateNewSlot = true))
    }

    fun clearPendingCreateNewSlot() {
        saveState(loadState().copy(pendingCreateNewSlot = false))
    }

    fun snapshotActiveFromSettings() {
        ensureMigrated()
        val state = loadState()
        val activeId = state.activeAccountId ?: state.accounts.firstOrNull()?.id ?: return
        saveState(
            state.copy(
                accounts = state.accounts.map { slot ->
                    if (slot.id == activeId) slot.withCurrentSession(settings) else slot
                },
                activeAccountId = activeId,
            )
        )
    }

    fun bindSuccessfulLogin(login: String) {
        ensureMigrated()
        val state = loadState()
        val normalizedLogin = login.trim()
        if (state.pendingCreateNewSlot) {
            when (val result = addOrReuseFromSettings(normalizedLogin)) {
                is AddAccountResult.Created,
                is AddAccountResult.Reused -> saveState(loadState().copy(pendingCreateNewSlot = false))
                AddAccountResult.LimitReached -> saveState(state.copy(pendingCreateNewSlot = false))
            }
            return
        }
        val active = state.accounts.firstOrNull { it.id == state.activeAccountId }
            ?: state.accounts.firstOrNull()
        if (active == null) {
            addOrReuseFromSettings(normalizedLogin)
            return
        }
        val updated = active.withCurrentSession(settings).let { slot ->
            if (normalizedLogin.isNotBlank()) slot.copy(login = normalizedLogin) else slot
        }
        saveState(
            state.copy(
                accounts = state.accounts.map { if (it.id == updated.id) updated else it },
                activeAccountId = updated.id,
            )
        )
        persistActiveLogin(updated.login)
    }

    fun addOrReuseFromSettings(login: String): AddAccountResult {
        val state = loadState()
        val normalizedLogin = login.trim()
        val candidate = AccountSlot(
            id = AccountPinCrypto.newAccountId(),
            login = normalizedLogin,
            fullName = settings.getStringOrNull(AppSettingsKeys.PERSONAL_DATA).orEmpty(),
            department = settings.getStringOrNull(AppSettingsKeys.DEPARTMENT).orEmpty(),
            token = settings.getStringOrNull(AppSettingsKeys.TOKEN_KEY).orEmpty(),
            coreSessionId = settings.getStringOrNull(AppSettingsKeys.CORE_SESSION_ID).orEmpty(),
        )
        val existing = state.accounts.firstOrNull { slot ->
            (normalizedLogin.isNotBlank() && slot.login.equals(normalizedLogin, ignoreCase = true)) ||
                (candidate.fullName.isNotBlank() && slot.fullName.equals(candidate.fullName, ignoreCase = true) &&
                    slot.login.equals(normalizedLogin, ignoreCase = true))
        }
        if (existing != null) {
            val merged = existing.copy(
                login = normalizedLogin.ifBlank { existing.login },
                fullName = candidate.fullName.ifBlank { existing.fullName },
                department = candidate.department.ifBlank { existing.department },
                token = candidate.token.ifBlank { existing.token },
                coreSessionId = candidate.coreSessionId.ifBlank { existing.coreSessionId },
            )
            saveState(
                state.copy(
                    accounts = state.accounts.map { if (it.id == merged.id) merged else it },
                    activeAccountId = merged.id,
                    pendingCreateNewSlot = false,
                )
            )
            persistActiveLogin(merged.login)
            return AddAccountResult.Reused(merged)
        }
        if (state.accounts.size >= MAX_SAVED_ACCOUNTS) return AddAccountResult.LimitReached
        saveState(
            state.copy(
                accounts = state.accounts + candidate,
                activeAccountId = candidate.id,
                pendingCreateNewSlot = false,
            )
        )
        persistActiveLogin(candidate.login)
        return AddAccountResult.Created(candidate)
    }

    fun activate(accountId: String): AccountSlot? {
        val state = loadState()
        val target = state.accounts.firstOrNull { it.id == accountId } ?: return null
        applySlotToSettings(target)
        saveState(state.copy(activeAccountId = target.id))
        return target
    }

    fun removeAccount(accountId: String): AccountStoreState {
        val state = loadState()
        val remaining = state.accounts.filterNot { it.id == accountId }
        val nextActive = when {
            remaining.isEmpty() -> null
            state.activeAccountId == accountId -> null
            else -> state.activeAccountId
        }
        val next = state.copy(accounts = remaining, activeAccountId = nextActive)
        saveState(next)
        return next
    }

    fun removeActiveAccount(): AccountStoreState {
        val activeId = loadState().activeAccountId ?: activeAccount()?.id
        return if (activeId == null) loadState() else removeAccount(activeId)
    }

    fun setPin(accountId: String, pin: String): SetPinResult {
        if (!AccountPinCrypto.isFourDigitPin(pin)) return SetPinResult.InvalidPin
        val state = loadState()
        val slot = state.accounts.firstOrNull { it.id == accountId } ?: return SetPinResult.AccountMissing
        val salt = AccountPinCrypto.newSalt()
        val updated = slot.copy(pinSalt = salt, pinHash = AccountPinCrypto.hashPin(pin, salt))
        saveState(
            state.copy(accounts = state.accounts.map { if (it.id == accountId) updated else it })
        )
        return SetPinResult.Ok
    }

    fun verifyPin(accountId: String, pin: String): PinVerifyResult {
        val lockout = resolveLockout()
        if (lockout.lockedUntilMs > nowMs()) {
            return PinVerifyResult.Locked(
                untilMs = lockout.lockedUntilMs,
                isHourLock = lockout.lockLevel >= 2,
            )
        }
        val slot = loadState().accounts.firstOrNull { it.id == accountId }
            ?: return PinVerifyResult.Wrong(
                remaining = PIN_ATTEMPTS_PER_WINDOW,
                failedInWindow = 0,
                warnAfterFive = false,
            )
        val ok = slot.hasPin &&
            AccountPinCrypto.isFourDigitPin(pin) &&
            AccountPinCrypto.hashPin(pin, slot.pinSalt) == slot.pinHash
        if (ok) {
            saveLockout(PinLockoutState())
            return PinVerifyResult.Ok
        }
        return recordFailedAttempt(lockout)
    }

    fun lockoutSnapshot(): PinLockoutState = resolveLockout()

    fun applySlotToSettings(slot: AccountSlot) {
        settings.setString(AppSettingsKeys.TOKEN_KEY, slot.token)
        settings.setString(AppSettingsKeys.PERSONAL_DATA, slot.fullName)
        settings.setString(AppSettingsKeys.DEPARTMENT, slot.department)
        settings.setString(AppSettingsKeys.CORE_SESSION_ID, slot.coreSessionId)
        persistActiveLogin(slot.login)
    }

    fun updateActiveToken(token: String, coreSessionId: String? = null) {
        val state = loadState()
        val activeId = state.activeAccountId ?: return
        saveState(
            state.copy(
                accounts = state.accounts.map { slot ->
                    if (slot.id != activeId) slot
                    else slot.copy(
                        token = token,
                        coreSessionId = coreSessionId ?: slot.coreSessionId,
                    )
                }
            )
        )
    }

    private fun recordFailedAttempt(current: PinLockoutState): PinVerifyResult {
        val failed = current.failedAttemptsInWindow + 1
        if (failed >= PIN_ATTEMPTS_PER_WINDOW) {
            val nextLevel = if (current.lockLevel >= 1) 2 else 1
            val duration = if (nextLevel >= 2) PIN_LOCK_SECOND_MS else PIN_LOCK_FIRST_MS
            val locked = PinLockoutState(
                failedAttemptsInWindow = 0,
                lockLevel = nextLevel,
                lockedUntilMs = nowMs() + duration,
            )
            saveLockout(locked)
            return PinVerifyResult.Locked(
                untilMs = locked.lockedUntilMs,
                isHourLock = nextLevel >= 2,
            )
        }
        saveLockout(current.copy(failedAttemptsInWindow = failed, lockedUntilMs = 0L))
        return PinVerifyResult.Wrong(
            remaining = PIN_ATTEMPTS_PER_WINDOW - failed,
            failedInWindow = failed,
            warnAfterFive = failed >= PIN_WARN_AFTER_FAILURES,
        )
    }

    private fun resolveLockout(): PinLockoutState {
        val raw = settings.getStringOrNull(AppSettingsKeys.ACCOUNTS_PIN_LOCKOUT_JSON).orEmpty()
        val parsed = if (raw.isBlank()) PinLockoutState() else runCatching {
            json.decodeFromString(PinLockoutState.serializer(), raw)
        }.getOrDefault(PinLockoutState())
        val now = nowMs()
        if (parsed.lockedUntilMs > 0L && now >= parsed.lockedUntilMs) {
            val reset = if (parsed.lockLevel >= 2) {
                PinLockoutState()
            } else {
                parsed.copy(failedAttemptsInWindow = 0, lockedUntilMs = 0L)
            }
            saveLockout(reset)
            return reset
        }
        return parsed
    }

    private fun loadState(): AccountStoreState {
        val raw = settings.getStringOrNull(AppSettingsKeys.ACCOUNTS_JSON).orEmpty()
        if (raw.isBlank()) return AccountStoreState()
        return runCatching { json.decodeFromString(AccountStoreState.serializer(), raw) }
            .getOrDefault(AccountStoreState())
    }

    private fun saveState(state: AccountStoreState) {
        settings.setString(
            AppSettingsKeys.ACCOUNTS_JSON,
            json.encodeToString(AccountStoreState.serializer(), state)
        )
        settings.setString(AppSettingsKeys.ACCOUNTS_ACTIVE_ID, state.activeAccountId.orEmpty())
        _snapshot.value = state
    }

    private fun saveLockout(state: PinLockoutState) {
        settings.setString(
            AppSettingsKeys.ACCOUNTS_PIN_LOCKOUT_JSON,
            json.encodeToString(PinLockoutState.serializer(), state)
        )
    }

    private fun persistActiveLogin(login: String) {
        settings.setString(AppSettingsKeys.ACCOUNT_LOGIN, login)
    }

    private fun AccountSlot.withCurrentSession(appSettings: AppSettings): AccountSlot = copy(
        login = appSettings.getStringOrNull(AppSettingsKeys.ACCOUNT_LOGIN).orEmpty()
            .ifBlank { login },
        fullName = appSettings.getStringOrNull(AppSettingsKeys.PERSONAL_DATA).orEmpty().ifBlank { fullName },
        department = appSettings.getStringOrNull(AppSettingsKeys.DEPARTMENT).orEmpty().ifBlank { department },
        token = appSettings.getStringOrNull(AppSettingsKeys.TOKEN_KEY).orEmpty().ifBlank { token },
        coreSessionId = appSettings.getStringOrNull(AppSettingsKeys.CORE_SESSION_ID).orEmpty()
            .ifBlank { coreSessionId },
    )

    companion object {
        @OptIn(ExperimentalTime::class)
        private val defaultNowMs: () -> Long = { Clock.System.now().toEpochMilliseconds() }
    }
}
