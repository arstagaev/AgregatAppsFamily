package com.tagaev.trrcrm.ui.settings

import com.arkivanov.decompose.ComponentContext
import com.tagaev.trrcrm.data.AppSettings
import com.tagaev.trrcrm.data.AppSettingsKeys
import com.tagaev.trrcrm.data.MainRepository
import com.tagaev.trrcrm.data.db.EventsCacheStore
import com.tagaev.trrcrm.getPlatform
import com.tagaev.trrcrm.push.PushRegistration
import com.tagaev.trrcrm.utils.SessionPermissions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

interface ISettingsComponent {
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


    override fun onWriteToDeveloper() {
        onWriteToDeveloperAction()
    }

    override fun onLogout() {

        val fullName = settings.getStringOrNull(AppSettingsKeys.PERSONAL_DATA) // from your settings / repository
        val platform = getPlatform().deviceSpecificInfo
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
        settings.clearAll()
//        settings.setString(AppSettingsKeys.WORK_ORDERS_REFINE_STATE,"")
//        settings.setString(AppSettingsKeys.EVENTS_REFINE_STATE,"")

        onLogoutAction.invoke()
    }



    override fun back() = onBack()
}