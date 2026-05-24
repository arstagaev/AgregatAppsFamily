package com.tagaev.trrcrm.ui.settings

import com.arkivanov.decompose.ComponentContext
import com.tagaev.trrcrm.data.AppSettings
import com.tagaev.trrcrm.data.AppSettingsKeys
import com.tagaev.trrcrm.data.MainRepository
import com.tagaev.trrcrm.data.db.EventsCacheStore
import com.tagaev.trrcrm.data.remote.CoreApiErrorKind
import com.tagaev.trrcrm.data.remote.toCoreApiError
import com.tagaev.trrcrm.push.NotificationsUnreadState
import com.tagaev.trrcrm.push.PushRegistration
import com.tagaev.trrcrm.push.PushRegistrationCoordinator
import com.tagaev.trrcrm.pushPlatformId
import com.tagaev.trrcrm.utils.SessionPermissions
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

interface ISettingsComponent {
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
    fun back()
}

class SettingsComponent(
    componentContext: ComponentContext,
    private val onBack: () -> Unit,
    private val onWriteToDeveloperAction: () -> Unit = {},
    private val onLogoutAction: () -> Unit
) : ISettingsComponent, KoinComponent, ComponentContext by componentContext {
    private val eventsCacheStore: EventsCacheStore by inject()
    private val settings: AppSettings by inject()
    private val repository: MainRepository by inject()
    private val appScope: CoroutineScope by inject()
    private val muteUpdateMutex = Mutex()
    private var muteRequestVersion: Long = 0L

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
            _muteErrorMessage.value = "Нет активной сессии. Перезайдите в приложение."
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
                        val mapped = res.exception.toCoreApiError(res.causes ?: "Не удалось загрузить настройки уведомлений")
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
                                        val retryMapped = retry.exception.toCoreApiError(retry.causes ?: "Не удалось загрузить настройки уведомлений")
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
            _muteErrorMessage.value = "Нет активной сессии. Перезайдите в приложение."
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
                            val mapped = res.exception.toCoreApiError(res.causes ?: "Не удалось обновить общий mute")
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
            _muteErrorMessage.value = "Нет активной сессии. Перезайдите в приложение."
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
                            val mapped = res.exception.toCoreApiError(res.causes ?: "Не удалось обновить mute по типу")
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

        val fullName = settings.getStringOrNull(AppSettingsKeys.PERSONAL_DATA) // from your settings / repository
        val platform = pushPlatformId()
        val coreSessionId = settings.getStringOrNull(AppSettingsKeys.CORE_SESSION_ID).orEmpty()

        if (coreSessionId.isNotBlank()) {
            appScope.launch {
                repository.coreSessionLogout(
                    com.tagaev.trrcrm.models.CoreSessionLogoutRequest(
                        sessionId = coreSessionId,
                        deactivateDeviceToken = false
                    )
                )
            }
        }

        if (!fullName.isNullOrBlank()) {
            // FCM token optional here; platform+fullName is enough
            PushRegistration.logoutCurrentDevice(
                fullName = fullName,
                platform = platform
            )
        }

        eventsCacheStore.clearAll()

        SessionPermissions.clear()
        settings.clearForLogoutPreservingInstallIdentity()
        NotificationsUnreadState.setCount(0)
//        settings.setString(AppSettingsKeys.WORK_ORDERS_REFINE_STATE,"")
//        settings.setString(AppSettingsKeys.EVENTS_REFINE_STATE,"")

        onLogoutAction.invoke()
    }



    override fun back() = onBack()

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
            _muteErrorMessage.value = "Сессия устарела. Перезайдите в приложение."
        }
        return recovered
    }

    private fun actionableMuteError(error: com.tagaev.trrcrm.data.remote.CoreApiError): String = when (error.kind) {
        CoreApiErrorKind.Unauthorized, CoreApiErrorKind.Forbidden ->
            "Ошибка авторизации. Перезайдите в приложение."
        CoreApiErrorKind.NotFound ->
            "Сессия не найдена или истекла. Перезайдите в приложение."
        CoreApiErrorKind.Validation ->
            "Параметры mute не приняты сервером. Обновите экран и повторите."
        else -> error.message.ifBlank { "Не удалось обновить настройки уведомлений" }
    }
}
