package com.tagaev.trrcrm.ui.accounts

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.essenty.backhandler.BackCallback
import com.arkivanov.essenty.lifecycle.subscribe
import com.tagaev.trrcrm.data.AppSettings
import com.tagaev.trrcrm.data.AppSettingsKeys
import com.tagaev.trrcrm.data.accounts.AccountSessionCaches
import com.tagaev.trrcrm.data.accounts.AccountSessionStore
import com.tagaev.trrcrm.data.accounts.AccountSlot
import com.tagaev.trrcrm.data.accounts.MAX_SAVED_ACCOUNTS
import com.tagaev.trrcrm.data.accounts.PinVerifyResult
import com.tagaev.trrcrm.data.accounts.SetPinResult
import com.tagaev.trrcrm.data.db.EventsCacheStore
import com.tagaev.trrcrm.data.db.FavoritesStore
import com.tagaev.trrcrm.data.remote.ApiConfig
import com.tagaev.trrcrm.push.CoreSessionCoordinator
import com.tagaev.trrcrm.push.CoreSessionResult
import com.tagaev.trrcrm.push.disablePushDeliveryForLoggedOutUser
import com.tagaev.trrcrm.ui.i18n.tr
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

data class AccountSwitcherUiState(
    val accounts: List<AccountSlot> = emptyList(),
    val activeAccountId: String? = null,
    val canAdd: Boolean = true,
    val needsPinGate: Boolean = false,
    val showPinStatus: Boolean = false,
    val pinDialog: PinDialogState = PinDialogState.Hidden,
    val pinError: String? = null,
    val infoDialog: String? = null,
    val confirmDeleteId: String? = null,
    val confirmRemovePinId: String? = null,
)

sealed interface PinDialogState {
    data object Hidden : PinDialogState
    data class SetPin(
        val accountId: String,
        val firstEntry: String = "",
        val confirmEntry: String = "",
    ) : PinDialogState
    data class EnterPin(val accountId: String, val entry: String = "") : PinDialogState
}

interface IAccountSwitcherComponent {
    val uiState: StateFlow<AccountSwitcherUiState>
    fun refresh()
    fun onBack()
    fun consumeInfoDialog()
    fun onAddAccount()
    fun onAccountClick(accountId: String)
    fun onSetPinClick(accountId: String)
    fun onRemovePinClick(accountId: String)
    fun confirmRemovePin()
    fun cancelRemovePin()
    fun onDeleteClick(accountId: String)
    fun confirmDelete()
    fun cancelDelete()
    fun onUnlockAccountSelected(accountId: String)
    fun onForgotPin()
    fun onPinDigit(digit: String)
    fun onPinBackspace()
    fun dismissPinDialog()
}

class AccountSwitcherComponent(
    componentContext: ComponentContext,
    private val onBackToHost: () -> Unit,
    private val onAddAccountRequested: () -> Unit,
    private val onAccountActivated: () -> Unit,
    private val onForgotPinRequested: (AccountSlot) -> Unit,
    private val onNoAccountsLeft: () -> Unit,
) : IAccountSwitcherComponent, ComponentContext by componentContext, KoinComponent {
    private val store: AccountSessionStore by inject()
    private val settings: AppSettings by inject()
    private val apiConfig: ApiConfig by inject()
    private val eventsCacheStore: EventsCacheStore by inject()
    private val favoritesStore: FavoritesStore by inject()
    private val appScope: CoroutineScope by inject()
    private val coreSession: CoreSessionCoordinator by inject()
    private val documentPhotoCache: com.tagaev.trrcrm.data.fixator.DocumentPhotoCache by inject()
    private val uploadQuotaTracker: com.tagaev.trrcrm.data.fixator.UploadSessionQuotaTracker by inject()
    private var switchInFlight = false

    private val _uiState = MutableStateFlow(AccountSwitcherUiState())
    override val uiState: StateFlow<AccountSwitcherUiState> = _uiState

    private val backCallback = BackCallback {
        onBack()
    }
    private var observeAccountsJob: Job? = null

    init {
        store.ensureMigrated()
        refresh()
        maybeShowInactivityUnlockPad()
        backHandler.register(backCallback)
        observeAccountsJob = appScope.launch {
            store.snapshotFlow.collect { refresh() }
        }
        lifecycle.subscribe(
            onResume = {
                refresh()
                maybeShowInactivityUnlockPad()
            },
            onDestroy = { observeAccountsJob?.cancel() },
        )
    }

    override fun refresh() {
        store.ensureMigrated()
        val snapshot = store.snapshot()
        _uiState.value = _uiState.value.copy(
            accounts = snapshot.accounts,
            activeAccountId = snapshot.activeAccountId,
            canAdd = snapshot.accounts.size < MAX_SAVED_ACCOUNTS,
            needsPinGate = store.needsPinGate(),
            showPinStatus = snapshot.accounts.isNotEmpty(),
        )
    }

    private fun maybeShowInactivityUnlockPad() {
        if (!store.shouldLockForInactivity() || !store.allPinsReady()) return
        if (_uiState.value.pinDialog !is PinDialogState.Hidden) return
        val id = store.activeAccount()?.id ?: store.accounts().firstOrNull()?.id ?: return
        _uiState.value = _uiState.value.copy(
            pinDialog = PinDialogState.EnterPin(id),
            pinError = null,
        )
    }

    override fun onBack() {
        if (store.needsPinGate()) {
            _uiState.value = _uiState.value.copy(infoDialog = tr("accounts_pin_gate_message"))
            return
        }
        if (store.shouldLockForInactivity()) {
            _uiState.value = _uiState.value.copy(infoDialog = tr("accounts_enter_pin_to_continue"))
            return
        }
        restartHostSessionLikeAccountSwitch()
    }

    override fun consumeInfoDialog() {
        _uiState.value = _uiState.value.copy(infoDialog = null)
    }

    override fun onAddAccount() {
        if (!store.canAddAccount()) {
            _uiState.value = _uiState.value.copy(infoDialog = tr("accounts_limit"))
            return
        }
        store.snapshotActiveFromSettings()
        store.markPendingCreateNewSlot()
        onAddAccountRequested()
    }

    override fun onAccountClick(accountId: String) {
        if (store.needsPinGate()) {
            _uiState.value = _uiState.value.copy(infoDialog = tr("accounts_pin_gate_message"))
            return
        }
        val hasActiveToken = !settings.getStringOrNull(AppSettingsKeys.TOKEN_KEY).isNullOrBlank()
        val inactivityLocked = store.shouldLockForInactivity()
        if (accountId == _uiState.value.activeAccountId && hasActiveToken && !inactivityLocked) {
            restartHostSessionLikeAccountSwitch()
            return
        }
        val target = store.accounts().firstOrNull { it.id == accountId } ?: return
        if (store.accounts().size >= 2) {
            if (!target.hasPin) {
                onSetPinClick(accountId)
                return
            }
            _uiState.value = _uiState.value.copy(
                pinDialog = PinDialogState.EnterPin(accountId),
                pinError = null,
            )
            return
        }
        switchTo(accountId)
    }

    override fun onSetPinClick(accountId: String) {
        _uiState.value = _uiState.value.copy(
            pinDialog = PinDialogState.SetPin(accountId),
            pinError = null,
        )
    }

    override fun onRemovePinClick(accountId: String) {
        if (store.accounts().size != 1) return
        _uiState.value = _uiState.value.copy(confirmRemovePinId = accountId)
    }

    override fun cancelRemovePin() {
        _uiState.value = _uiState.value.copy(confirmRemovePinId = null)
    }

    override fun confirmRemovePin() {
        val accountId = _uiState.value.confirmRemovePinId ?: return
        _uiState.value = _uiState.value.copy(confirmRemovePinId = null)
        store.clearPin(accountId)
        refresh()
    }

    override fun onUnlockAccountSelected(accountId: String) {
        val dialog = _uiState.value.pinDialog as? PinDialogState.EnterPin ?: return
        if (dialog.accountId == accountId) return
        _uiState.value = _uiState.value.copy(
            pinDialog = PinDialogState.EnterPin(accountId),
            pinError = null,
        )
    }

    override fun onForgotPin() {
        val dialog = _uiState.value.pinDialog as? PinDialogState.EnterPin ?: return
        val slot = store.accounts().firstOrNull { it.id == dialog.accountId } ?: return
        dismissPinDialog()
        onForgotPinRequested(slot)
    }

    override fun onDeleteClick(accountId: String) {
        _uiState.value = _uiState.value.copy(confirmDeleteId = accountId)
    }

    override fun cancelDelete() {
        _uiState.value = _uiState.value.copy(confirmDeleteId = null)
    }

    override fun confirmDelete() {
        val accountId = _uiState.value.confirmDeleteId ?: return
        _uiState.value = _uiState.value.copy(confirmDeleteId = null)
        val wasActive = accountId == store.snapshot().activeAccountId
        val remainingCount = store.accounts().size
        val slot = store.accounts().firstOrNull { it.id == accountId }
        if (wasActive && slot != null) {
            val isLast = remainingCount <= 1
            appScope.launch {
                if (isLast) {
                    coreSession.logoutActive(deactivateDeviceToken = true)
                } else {
                    coreSession.stopHeartbeat()
                }
            }
            AccountSessionCaches.clearCrmUserCaches(settings, eventsCacheStore, favoritesStore)
            appScope.launch {
                uploadQuotaTracker.reset()
                runCatching { documentPhotoCache.clearAll() }
            }
            disablePushDeliveryForLoggedOutUser()
            settings.setString(AppSettingsKeys.TOKEN_KEY, "")
            settings.setString(AppSettingsKeys.PERSONAL_DATA, "")
            settings.setString(AppSettingsKeys.DEPARTMENT, "")
            settings.setString(AppSettingsKeys.ACCOUNT_LOGIN, "")
            settings.setString(AppSettingsKeys.CORE_SESSION_ID, "")
            runCatching { apiConfig.token = "" }
        }
        val remaining = store.removeAccount(accountId)
        refresh()
        if (remaining.accounts.isEmpty()) {
            onNoAccountsLeft()
        }
    }

    override fun onPinDigit(digit: String) {
        if (switchInFlight) return
        when (val dialog = _uiState.value.pinDialog) {
            is PinDialogState.EnterPin -> {
                val next = (dialog.entry + digit).take(4)
                _uiState.value = _uiState.value.copy(
                    pinDialog = dialog.copy(entry = next),
                    pinError = null,
                )
                if (next.length == 4) submitEnterPin(dialog.accountId, next)
            }
            is PinDialogState.SetPin -> {
                _uiState.value = _uiState.value.copy(pinError = null)
                if (dialog.firstEntry.length < 4) {
                    val first = (dialog.firstEntry + digit).take(4)
                    _uiState.value = _uiState.value.copy(pinDialog = dialog.copy(firstEntry = first))
                } else {
                    val confirm = (dialog.confirmEntry + digit).take(4)
                    _uiState.value = _uiState.value.copy(pinDialog = dialog.copy(confirmEntry = confirm))
                    if (confirm.length == 4) submitSetPin(dialog.accountId, dialog.firstEntry, confirm)
                }
            }
            PinDialogState.Hidden -> Unit
        }
    }

    override fun onPinBackspace() {
        when (val dialog = _uiState.value.pinDialog) {
            is PinDialogState.EnterPin -> {
                _uiState.value = _uiState.value.copy(
                    pinDialog = dialog.copy(entry = dialog.entry.dropLast(1)),
                    pinError = null,
                )
            }
            is PinDialogState.SetPin -> {
                if (dialog.confirmEntry.isNotEmpty()) {
                    _uiState.value = _uiState.value.copy(
                        pinDialog = dialog.copy(confirmEntry = dialog.confirmEntry.dropLast(1)),
                        pinError = null,
                    )
                } else {
                    _uiState.value = _uiState.value.copy(
                        pinDialog = dialog.copy(firstEntry = dialog.firstEntry.dropLast(1)),
                        pinError = null,
                    )
                }
            }
            PinDialogState.Hidden -> Unit
        }
    }

    override fun dismissPinDialog() {
        _uiState.value = _uiState.value.copy(pinDialog = PinDialogState.Hidden, pinError = null)
    }

    private fun submitSetPin(accountId: String, first: String, confirm: String) {
        if (first != confirm) {
            _uiState.value = _uiState.value.copy(
                pinDialog = PinDialogState.SetPin(accountId),
                pinError = tr("accounts_pin_mismatch"),
            )
            return
        }
        when (store.setPin(accountId, first)) {
            SetPinResult.Ok -> {
                dismissPinDialog()
                refresh()
            }
            SetPinResult.InvalidPin -> {
                _uiState.value = _uiState.value.copy(pinError = tr("accounts_pin_mismatch"))
            }
            SetPinResult.AccountMissing -> dismissPinDialog()
        }
    }

    private fun submitEnterPin(accountId: String, pin: String) {
        when (val result = store.verifyPin(accountId, pin)) {
            PinVerifyResult.Ok -> {
                dismissPinDialog()
                store.markAppScreenOpened()
                val activeId = store.activeAccount()?.id
                val hasActiveToken = !settings.getStringOrNull(AppSettingsKeys.TOKEN_KEY).isNullOrBlank()
                if (accountId == activeId && hasActiveToken) {
                    onAccountActivated()
                } else {
                    switchTo(accountId)
                }
            }
            is PinVerifyResult.Wrong -> {
                val message = if (result.warnAfterFive) {
                    tr("accounts_pin_wrong_warn", result.remaining.toString())
                } else {
                    tr("accounts_pin_wrong", result.remaining.toString())
                }
                _uiState.value = _uiState.value.copy(
                    pinDialog = PinDialogState.EnterPin(accountId),
                    pinError = message,
                )
            }
            is PinVerifyResult.Locked -> {
                val message = if (result.isHourLock) {
                    tr("accounts_pin_locked_1h")
                } else {
                    tr("accounts_pin_locked_10m")
                }
                _uiState.value = _uiState.value.copy(
                    pinDialog = PinDialogState.EnterPin(accountId),
                    pinError = message,
                )
            }
        }
    }

    private fun restartHostSessionLikeAccountSwitch() {
        val token = store.activeAccount()?.token
            ?: settings.getStringOrNull(AppSettingsKeys.TOKEN_KEY).orEmpty()
        if (token.isBlank()) {
            onBackToHost()
            return
        }
        wipeUserListsAndRestartHost(token)
    }

    private fun wipeUserListsAndRestartHost(token: String) {
        AccountSessionCaches.clearCrmUserCaches(settings, eventsCacheStore, favoritesStore)
        appScope.launch {
            uploadQuotaTracker.reset()
            runCatching { documentPhotoCache.clearAll() }
        }
        if (token.isNotBlank()) {
            runCatching { apiConfig.token = token }
        }
        store.markAppScreenOpened()
        onAccountActivated()
    }

    private fun switchTo(accountId: String) {
        if (switchInFlight) return
        val target = store.accounts().firstOrNull { it.id == accountId } ?: return
        switchInFlight = true
        appScope.launch {
            try {
                val result = coreSession.switchTo(target)
                withContext(Dispatchers.Main.immediate) {
                    when (result) {
                        is CoreSessionResult.Ok,
                        CoreSessionResult.DeferredMissingFcm -> {
                            wipeUserListsAndRestartHost(target.token)
                        }
                        is CoreSessionResult.Error -> {
                            _uiState.value = _uiState.value.copy(infoDialog = result.message)
                        }
                        CoreSessionResult.SkippedMissingUser -> {
                            _uiState.value = _uiState.value.copy(infoDialog = tr("accounts_switch_failed"))
                        }
                    }
                }
            } finally {
                withContext(Dispatchers.Main.immediate) {
                    switchInFlight = false
                }
            }
        }
    }
}
