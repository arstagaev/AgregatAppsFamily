package com.tagaev.trrcrm.ui.settings

import com.tagaev.trrcrm.ui.i18n.tr

import com.arkivanov.decompose.ComponentContext
import com.tagaev.trrcrm.data.AppSettings
import com.tagaev.trrcrm.data.AppSettingsKeys
import com.tagaev.trrcrm.data.MainRepository
import com.tagaev.trrcrm.data.accounts.AccountSessionCaches
import com.tagaev.trrcrm.data.accounts.AccountSessionStore
import com.tagaev.trrcrm.data.db.EventsCacheStore
import com.tagaev.trrcrm.data.db.FavoritesStore
import com.tagaev.trrcrm.data.remote.CoreApiErrorKind
import com.tagaev.trrcrm.data.remote.toCoreApiError
import com.tagaev.trrcrm.push.CoreSessionCoordinator
import com.tagaev.trrcrm.push.NotificationsUnreadState
import com.tagaev.trrcrm.push.PushRegistrationCoordinator
import com.tagaev.trrcrm.push.disablePushDeliveryForLoggedOutUser
import com.tagaev.trrcrm.navigation.BottomNavItemId
import com.tagaev.trrcrm.navigation.BottomNavLayoutItem
import com.tagaev.trrcrm.navigation.BottomNavLayoutResolver
import com.tagaev.trrcrm.navigation.BottomNavLayoutState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

enum class DeviceMuteDocType(val wire: String, val title: String) {
    EVENT("event", "Event"),
    WORK_ORDER("work_order", "Work Order"),
    COMPLECTATION("complectation", "Complectation");
}

interface ISettingsComponent : BottomNavLayoutEditorHost {
    val showBottomNavEditor: StateFlow<Boolean>
    val showNotificationSettings: StateFlow<Boolean>
    val muteAll: StateFlow<Boolean>
    val mutedDocTypes: StateFlow<Set<DeviceMuteDocType>>
    val muteLoading: StateFlow<Boolean>
    val muteErrorMessage: StateFlow<String?>

    fun refreshMuteState()
    fun setMuteAll(enabled: Boolean)
    fun setDocumentTypeMuted(type: DeviceMuteDocType, muted: Boolean)
    fun consumeMuteError()
    fun onWriteToDeveloper()
    fun onLogout()
    fun openCatalog()
    fun openAccounts()
    fun back()
    fun openBottomNavEditor()
    fun closeBottomNavEditor()
    fun openNotificationSettings()
    fun closeNotificationSettings()
    fun clearDocumentPhotoCache(onResult: (String) -> Unit)
}

class SettingsComponent(
    componentContext: ComponentContext,
    private val onBack: () -> Unit,
    private val onWriteToDeveloperAction: () -> Unit = {},
    private val onLogoutAction: () -> Unit,
    private val onOpenCatalog: () -> Unit = {},
    private val onOpenAccounts: () -> Unit = {},
) : ISettingsComponent, KoinComponent, ComponentContext by componentContext {
    private val eventsCacheStore: EventsCacheStore by inject()
    private val favoritesStore: FavoritesStore by inject()
    private val settings: AppSettings by inject()
    private val accountStore: AccountSessionStore by inject()
    private val repository: MainRepository by inject()
    private val appScope: CoroutineScope by inject()
    private val coreSession: CoreSessionCoordinator by inject()
    private val uploadQuotaTracker: com.tagaev.trrcrm.data.fixator.UploadSessionQuotaTracker by inject()
    private val muteUpdateMutex = Mutex()
    private var muteRequestVersion: Long = 0L
    private val bottomNavEditor = BottomNavLayoutEditorController(settings)

    private val _showBottomNavEditor = MutableStateFlow(false)
    override val showBottomNavEditor: StateFlow<Boolean> = _showBottomNavEditor

    private val _showNotificationSettings = MutableStateFlow(false)
    override val showNotificationSettings: StateFlow<Boolean> = _showNotificationSettings

    override val bottomNavDraft = bottomNavEditor.bottomNavDraft
    override val bottomNavDirty = bottomNavEditor.bottomNavDirty
    override val bottomNavSaveMessage = bottomNavEditor.bottomNavSaveMessage
    override val bottomNavSaveError = bottomNavEditor.bottomNavSaveError

    override fun moveBottomNavItem(fromIndex: Int, toIndex: Int) =
        bottomNavEditor.moveBottomNavItem(fromIndex, toIndex)

    override fun toggleBottomNavVisible(id: String) =
        bottomNavEditor.toggleBottomNavVisible(id)

    override fun saveBottomNavLayout(activeTabId: BottomNavItemId?) =
        bottomNavEditor.saveBottomNavLayout(activeTabId)

    override fun resetBottomNavDraft() = bottomNavEditor.resetBottomNavDraft()

    override fun consumeBottomNavSaveMessage() = bottomNavEditor.consumeBottomNavSaveMessage()

    override fun consumeBottomNavSaveError() = bottomNavEditor.consumeBottomNavSaveError()

    private val _muteAll = MutableStateFlow(settings.getBool(AppSettingsKeys.DEVICE_MUTE_ALL, false))
    override val muteAll: StateFlow<Boolean> = _muteAll

    private val _mutedDocTypes = MutableStateFlow(
        buildSet {
            if (settings.getBool(AppSettingsKeys.DEVICE_MUTE_EVENT, false)) add(DeviceMuteDocType.EVENT)
            if (settings.getBool(AppSettingsKeys.DEVICE_MUTE_WORK_ORDER, false)) add(DeviceMuteDocType.WORK_ORDER)
            if (settings.getBool(AppSettingsKeys.DEVICE_MUTE_COMPLECTATION, false)) add(DeviceMuteDocType.COMPLECTATION)
        }
    )
    override val mutedDocTypes: StateFlow<Set<DeviceMuteDocType>> = _mutedDocTypes

    private val _muteLoading = MutableStateFlow(false)
    override val muteLoading: StateFlow<Boolean> = _muteLoading

    private val _muteErrorMessage = MutableStateFlow<String?>(null)
    override val muteErrorMessage: StateFlow<String?> = _muteErrorMessage

    init {
        refreshMuteState()
    }

    override fun refreshMuteState() {
        val sessionId = settings.getStringOrNull(AppSettingsKeys.CORE_SESSION_ID).orEmpty().trim()
        if (sessionId.isBlank()) {
            _muteErrorMessage.value = tr("settings_net_aktivnoy_sessii_perezaydite_v_prilozhenie")
            return
        }
        val expectedVersion = ++muteRequestVersion
        appScope.launch {
            _muteLoading.value = true
            try {
                when (val res = repository.coreDeviceMuteState(sessionId)) {
                    is com.tagaev.trrcrm.data.remote.Resource.Success -> {
                        if (expectedVersion != muteRequestVersion) return@launch
                        println("PUSH_SERVICE: mute_state_fetch_success mute_all=${res.data.muteAll} muted_types=${res.data.mutedDocumentTypes}")
                        applyMuteState(
                            muteAll = res.data.muteAll,
                            mutedDocumentTypes = res.data.mutedDocumentTypes
                        )
                    }
                    is com.tagaev.trrcrm.data.remote.Resource.Error -> {
                        if (expectedVersion != muteRequestVersion) return@launch
                        val mapped = res.exception.toCoreApiError(res.causes ?: tr("settings_ne_udalos_zagruzit_nastroyki_uvedomleniy"))
                        println("PUSH_SERVICE: mute_state_fetch_failed kind=${mapped.kind} status=${mapped.statusCode} reason=${mapped.message}")
                        if (mapped.kind == CoreApiErrorKind.NotFound && recoverSessionForMute("state_fetch")) {
                            val retrySessionId = settings.getStringOrNull(AppSettingsKeys.CORE_SESSION_ID).orEmpty().trim()
                            if (retrySessionId.isNotBlank()) {
                                when (val retry = repository.coreDeviceMuteState(retrySessionId)) {
                                    is com.tagaev.trrcrm.data.remote.Resource.Success -> {
                                        if (expectedVersion != muteRequestVersion) return@launch
                                        println("PUSH_SERVICE: mute_state_fetch_retry_success mute_all=${retry.data.muteAll} muted_types=${retry.data.mutedDocumentTypes}")
                                        applyMuteState(
                                            muteAll = retry.data.muteAll,
                                            mutedDocumentTypes = retry.data.mutedDocumentTypes
                                        )
                                        return@launch
                                    }
                                    is com.tagaev.trrcrm.data.remote.Resource.Error -> {
                                        if (expectedVersion != muteRequestVersion) return@launch
                                        val retryMapped = retry.exception.toCoreApiError(retry.causes ?: tr("settings_ne_udalos_zagruzit_nastroyki_uvedomleniy"))
                                        _muteErrorMessage.value = actionableMuteError(retryMapped)
                                        return@launch
                                    }
                                    is com.tagaev.trrcrm.data.remote.Resource.Loading -> Unit
                                }
                            }
                        }
                        _muteErrorMessage.value = actionableMuteError(mapped)
                    }
                    is com.tagaev.trrcrm.data.remote.Resource.Loading -> Unit
                }
            } finally {
                if (expectedVersion == muteRequestVersion) {
                    _muteLoading.value = false
                }
            }
        }
    }

    override fun setMuteAll(enabled: Boolean) {
        val sessionId = settings.getStringOrNull(AppSettingsKeys.CORE_SESSION_ID).orEmpty().trim()
        if (sessionId.isBlank()) {
            _muteErrorMessage.value = tr("settings_net_aktivnoy_sessii_perezaydite_v_prilozhenie")
            return
        }
        val previousMuteAll = _muteAll.value
        val previousMutedSet = _mutedDocTypes.value
        val targetMutedSet = if (enabled) allDocTypesSet() else emptySet()
        _muteAll.value = enabled
        _mutedDocTypes.value = targetMutedSet
        persistMuteCache(_muteAll.value, _mutedDocTypes.value)
        val expectedVersion = ++muteRequestVersion

        appScope.launch {
            muteUpdateMutex.withLock {
                _muteLoading.value = true
                try {
                    when (val res = repository.coreDeviceMuteSetAll(sessionId, enabled)) {
                        is com.tagaev.trrcrm.data.remote.Resource.Success -> {
                            if (expectedVersion != muteRequestVersion) return@withLock
                            println("PUSH_SERVICE: mute_update_success scope=all mute_all=${res.data.muteAll} muted_types=${res.data.mutedDocumentTypes}")
                            applyMuteState(
                                muteAll = enabled,
                                mutedDocumentTypes = if (enabled) DeviceMuteDocType.entries.map { it.wire } else emptyList()
                            )
                        }
                        is com.tagaev.trrcrm.data.remote.Resource.Error -> {
                            if (expectedVersion != muteRequestVersion) return@withLock
                            val mapped = res.exception.toCoreApiError(res.causes ?: tr("settings_ne_udalos_obnovit_obschiy_mute"))
                            println("PUSH_SERVICE: mute_update_failed scope=all kind=${mapped.kind} status=${mapped.statusCode} reason=${mapped.message}")
                            if (mapped.kind == CoreApiErrorKind.NotFound && recoverSessionForMute("set_all")) {
                                val retrySessionId = settings.getStringOrNull(AppSettingsKeys.CORE_SESSION_ID).orEmpty().trim()
                                if (retrySessionId.isNotBlank()) {
                                    when (val retry = repository.coreDeviceMuteSetAll(retrySessionId, enabled)) {
                                        is com.tagaev.trrcrm.data.remote.Resource.Success -> {
                                            if (expectedVersion != muteRequestVersion) return@withLock
                                            println("PUSH_SERVICE: mute_update_retry_success scope=all mute_all=${retry.data.muteAll} muted_types=${retry.data.mutedDocumentTypes}")
                                            applyMuteState(
                                                muteAll = enabled,
                                                mutedDocumentTypes = if (enabled) DeviceMuteDocType.entries.map { it.wire } else emptyList()
                                            )
                                            return@withLock
                                        }
                                        is com.tagaev.trrcrm.data.remote.Resource.Error -> Unit
                                        is com.tagaev.trrcrm.data.remote.Resource.Loading -> Unit
                                    }
                                }
                            }
                            _muteAll.value = previousMuteAll
                            _mutedDocTypes.value = previousMutedSet
                            persistMuteCache(_muteAll.value, _mutedDocTypes.value)
                            _muteErrorMessage.value = actionableMuteError(mapped)
                        }
                        is com.tagaev.trrcrm.data.remote.Resource.Loading -> Unit
                    }
                } finally {
                    if (expectedVersion == muteRequestVersion) {
                        _muteLoading.value = false
                    }
                }
            }
        }
    }

    override fun setDocumentTypeMuted(type: DeviceMuteDocType, muted: Boolean) {
        val sessionId = settings.getStringOrNull(AppSettingsKeys.CORE_SESSION_ID).orEmpty().trim()
        if (sessionId.isBlank()) {
            _muteErrorMessage.value = tr("settings_net_aktivnoy_sessii_perezaydite_v_prilozhenie")
            return
        }
        val previousSet = _mutedDocTypes.value
        val nextSet = previousSet.toMutableSet().apply {
            if (muted) add(type) else remove(type)
        }
        _mutedDocTypes.value = nextSet
        persistMuteCache(_muteAll.value, nextSet)
        val expectedVersion = ++muteRequestVersion

        appScope.launch {
            muteUpdateMutex.withLock {
                _muteLoading.value = true
                try {
                    when (val res = repository.coreDeviceMuteSetType(sessionId, type.wire, muted)) {
                        is com.tagaev.trrcrm.data.remote.Resource.Success -> {
                            if (expectedVersion != muteRequestVersion) return@withLock
                            println("PUSH_SERVICE: mute_update_success scope=${type.wire} muted=$muted mute_all=${res.data.muteAll} muted_types=${res.data.mutedDocumentTypes}")
                            applyMuteState(
                                muteAll = res.data.muteAll,
                                mutedDocumentTypes = res.data.mutedDocumentTypes
                            )
                        }
                        is com.tagaev.trrcrm.data.remote.Resource.Error -> {
                            if (expectedVersion != muteRequestVersion) return@withLock
                            val mapped = res.exception.toCoreApiError(res.causes ?: tr("settings_ne_udalos_obnovit_mute_po_tipu"))
                            println("PUSH_SERVICE: mute_update_failed scope=${type.wire} kind=${mapped.kind} status=${mapped.statusCode} reason=${mapped.message}")
                            if (mapped.kind == CoreApiErrorKind.NotFound && recoverSessionForMute("set_${type.wire}")) {
                                val retrySessionId = settings.getStringOrNull(AppSettingsKeys.CORE_SESSION_ID).orEmpty().trim()
                                if (retrySessionId.isNotBlank()) {
                                    when (val retry = repository.coreDeviceMuteSetType(retrySessionId, type.wire, muted)) {
                                        is com.tagaev.trrcrm.data.remote.Resource.Success -> {
                                            if (expectedVersion != muteRequestVersion) return@withLock
                                            println("PUSH_SERVICE: mute_update_retry_success scope=${type.wire} muted=$muted mute_all=${retry.data.muteAll} muted_types=${retry.data.mutedDocumentTypes}")
                                            applyMuteState(
                                                muteAll = retry.data.muteAll,
                                                mutedDocumentTypes = retry.data.mutedDocumentTypes
                                            )
                                            return@withLock
                                        }
                                        is com.tagaev.trrcrm.data.remote.Resource.Error -> Unit
                                        is com.tagaev.trrcrm.data.remote.Resource.Loading -> Unit
                                    }
                                }
                            }
                            _mutedDocTypes.value = previousSet
                            persistMuteCache(_muteAll.value, previousSet)
                            _muteErrorMessage.value = actionableMuteError(mapped)
                        }
                        is com.tagaev.trrcrm.data.remote.Resource.Loading -> Unit
                    }
                } finally {
                    if (expectedVersion == muteRequestVersion) {
                        _muteLoading.value = false
                    }
                }
            }
        }
    }

    override fun consumeMuteError() {
        _muteErrorMessage.value = null
    }

    override fun onWriteToDeveloper() {
        onWriteToDeveloperAction()
    }

    override fun onLogout() {
        val isLastAccount = accountStore.accounts().size <= 1
        appScope.launch {
            coreSession.logoutActive(deactivateDeviceToken = isLastAccount)
        }

        AccountSessionCaches.clearCrmUserCaches(settings, eventsCacheStore, favoritesStore)
        appScope.launch {
            uploadQuotaTracker.reset()
            runCatching { repository.clearDocumentPhotoCache() }
        }
        BottomNavLayoutState.applySaved(BottomNavLayoutResolver.defaultLayout())
        accountStore.removeActiveAccount()
        settings.clearForLogoutPreservingInstallIdentity()
        NotificationsUnreadState.setCount(0)
        disablePushDeliveryForLoggedOutUser()

        onLogoutAction.invoke()
    }



    override fun back() = onBack()

    override fun openCatalog() = onOpenCatalog()

    override fun openAccounts() {
        accountStore.ensureMigrated()
        onOpenAccounts()
    }

    override fun openBottomNavEditor() {
        closeAllSubScreens(exceptBottomNav = true)
        bottomNavEditor.resetBottomNavDraft()
        _showBottomNavEditor.value = true
    }

    override fun closeBottomNavEditor() {
        bottomNavEditor.resetBottomNavDraft()
        _showBottomNavEditor.value = false
    }

    override fun openNotificationSettings() {
        closeAllSubScreens(exceptNotifications = true)
        _showNotificationSettings.value = true
    }

    override fun closeNotificationSettings() {
        _showNotificationSettings.value = false
    }

    override fun clearDocumentPhotoCache(onResult: (String) -> Unit) {
        appScope.launch {
            val stats = runCatching { repository.clearDocumentPhotoCache() }
                .getOrElse {
                    onResult(tr("settings_ne_udalos_ochistit_kesh_fotografiy"))
                    return@launch
                }
            val sizeMb = stats.freedBytes / (1024.0 * 1024.0)
            val roundedSizeMb = (sizeMb * 10.0).toInt() / 10.0
            onResult("Удалено ${stats.deletedFiles} файлов ($roundedSizeMb МБ)")
        }
    }

    private fun closeAllSubScreens(
        exceptBottomNav: Boolean = false,
        exceptNotifications: Boolean = false,
    ) {
        if (!exceptBottomNav) {
            bottomNavEditor.resetBottomNavDraft()
            _showBottomNavEditor.value = false
        }
        if (!exceptNotifications) {
            _showNotificationSettings.value = false
        }
    }

    private fun applyMuteState(muteAll: Boolean, mutedDocumentTypes: List<String>) {
        val parsed = mutedDocumentTypes.mapNotNullTo(mutableSetOf()) { wire ->
            DeviceMuteDocType.entries.firstOrNull { it.wire == wire.trim().lowercase() }
        }
        val normalized = if (muteAll) allDocTypesSet() else parsed
        _muteAll.value = muteAll
        _mutedDocTypes.value = normalized
        persistMuteCache(muteAll, normalized)
    }

    private fun persistMuteCache(muteAll: Boolean, mutedTypes: Set<DeviceMuteDocType>) {
        settings.setBool(AppSettingsKeys.DEVICE_MUTE_ALL, muteAll)
        settings.setBool(AppSettingsKeys.DEVICE_MUTE_EVENT, DeviceMuteDocType.EVENT in mutedTypes)
        settings.setBool(AppSettingsKeys.DEVICE_MUTE_WORK_ORDER, DeviceMuteDocType.WORK_ORDER in mutedTypes)
        settings.setBool(AppSettingsKeys.DEVICE_MUTE_COMPLECTATION, DeviceMuteDocType.COMPLECTATION in mutedTypes)
    }

    private fun allDocTypesSet(): Set<DeviceMuteDocType> = DeviceMuteDocType.entries.toSet()

    private suspend fun recoverSessionForMute(reason: String): Boolean {
        println("PUSH_SERVICE: mute_session_recover_attempt reason=$reason")
        val recovered = PushRegistrationCoordinator.recoverCoreSessionNow(
            reason = "mute_$reason",
            forceRebootstrap = true
        )
        if (!recovered) {
            _muteErrorMessage.value = tr("settings_sessiya_ustarela_perezaydite_v_prilozhenie")
        }
        return recovered
    }

    private fun actionableMuteError(error: com.tagaev.trrcrm.data.remote.CoreApiError): String = when (error.kind) {
        CoreApiErrorKind.Unauthorized, CoreApiErrorKind.Forbidden ->
            tr("settings_oshibka_avtorizatsii_perezaydite_v_prilozhenie")
        CoreApiErrorKind.NotFound ->
            tr("settings_sessiya_ne_naydena_ili_istekla_perezaydite_v_prilozh")
        CoreApiErrorKind.Validation ->
            tr("settings_parametry_mute_ne_prinyaty_serverom_obnovite_ekran_i")
        else -> error.message.ifBlank { tr("settings_ne_udalos_obnovit_nastroyki_uvedomleniy") }
    }
}
