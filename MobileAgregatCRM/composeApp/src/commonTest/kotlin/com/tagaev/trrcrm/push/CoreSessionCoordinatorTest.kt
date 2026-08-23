package com.tagaev.trrcrm.push

import com.russhwolf.settings.Settings
import com.tagaev.trrcrm.data.AppSettings
import com.tagaev.trrcrm.data.AppSettingsKeys
import com.tagaev.trrcrm.data.accounts.AccountSessionStore
import com.tagaev.trrcrm.data.accounts.AccountSlot
import com.tagaev.trrcrm.data.remote.CoreApiErrorKind
import com.tagaev.trrcrm.data.remote.CoreApiException
import com.tagaev.trrcrm.data.remote.Resource
import com.tagaev.trrcrm.models.CoreSessionBootstrapRequest
import com.tagaev.trrcrm.models.CoreSessionBootstrapResponse
import com.tagaev.trrcrm.models.CoreSessionHeartbeatRequest
import com.tagaev.trrcrm.models.CoreSessionHeartbeatResponse
import com.tagaev.trrcrm.models.CoreSessionLogoutRequest
import com.tagaev.trrcrm.models.CoreSessionLogoutResponse
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json

private class MemorySettings : Settings {
    private val data = mutableMapOf<String, Any?>()
    override val keys: Set<String> get() = data.keys
    override val size: Int get() = data.size
    override fun clear() = data.clear()
    override fun remove(key: String) { data.remove(key) }
    override fun hasKey(key: String): Boolean = data.containsKey(key)
    override fun putInt(key: String, value: Int) { data[key] = value }
    override fun getInt(key: String, defaultValue: Int): Int = data[key] as? Int ?: defaultValue
    override fun getIntOrNull(key: String): Int? = data[key] as? Int
    override fun putLong(key: String, value: Long) { data[key] = value }
    override fun getLong(key: String, defaultValue: Long): Long = data[key] as? Long ?: defaultValue
    override fun getLongOrNull(key: String): Long? = data[key] as? Long
    override fun putString(key: String, value: String) { data[key] = value }
    override fun getString(key: String, defaultValue: String): String = data[key] as? String ?: defaultValue
    override fun getStringOrNull(key: String): String? = data[key] as? String
    override fun putFloat(key: String, value: Float) { data[key] = value }
    override fun getFloat(key: String, defaultValue: Float): Float = data[key] as? Float ?: defaultValue
    override fun getFloatOrNull(key: String): Float? = data[key] as? Float
    override fun putDouble(key: String, value: Double) { data[key] = value }
    override fun getDouble(key: String, defaultValue: Double): Double = data[key] as? Double ?: defaultValue
    override fun getDoubleOrNull(key: String): Double? = data[key] as? Double
    override fun putBoolean(key: String, value: Boolean) { data[key] = value }
    override fun getBoolean(key: String, defaultValue: Boolean): Boolean = data[key] as? Boolean ?: defaultValue
    override fun getBooleanOrNull(key: String): Boolean? = data[key] as? Boolean
}

private class FakeCoreSessionGateway : CoreSessionGateway {
    val calls = mutableListOf<String>()
    var bootstrapHang: CompletableDeferred<Unit>? = null
    private val bootstrapQueue = ArrayDeque<(CoreSessionBootstrapRequest) -> Resource<CoreSessionBootstrapResponse>>()
    private val heartbeatQueue = ArrayDeque<(CoreSessionHeartbeatRequest) -> Resource<CoreSessionHeartbeatResponse>>()

    fun enqueueBootstrap(sessionId: String) {
        bootstrapQueue.addLast { Resource.Success(CoreSessionBootstrapResponse(sessionId = sessionId)) }
    }

    fun enqueueBootstrapError(status: Int, message: String = "err") {
        bootstrapQueue.addLast {
            Resource.Error(
                CoreApiException(status, "/core/session/bootstrap", "", errorMessage = message),
                message,
            )
        }
    }

    fun enqueueHeartbeatOk() {
        heartbeatQueue.addLast { Resource.Success(CoreSessionHeartbeatResponse(status = "ok")) }
    }

    fun enqueueHeartbeatNotFound() {
        heartbeatQueue.addLast {
            Resource.Error(
                CoreApiException(404, "/core/session/heartbeat", "", errorMessage = "not found"),
                "not found",
            )
        }
    }

    override suspend fun bootstrap(request: CoreSessionBootstrapRequest): Resource<CoreSessionBootstrapResponse> {
        calls += "bootstrap:${request.login.orEmpty()}:${request.full_name}:${request.device_id}:${request.fcm_token}"
        bootstrapHang?.await()
        val handler = bootstrapQueue.removeFirstOrNull()
            ?: { Resource.Success(CoreSessionBootstrapResponse(sessionId = "sess-${request.login}")) }
        return handler(request)
    }

    override suspend fun heartbeat(request: CoreSessionHeartbeatRequest): Resource<CoreSessionHeartbeatResponse> {
        calls += "heartbeat:${request.sessionId}:${request.fcmToken.orEmpty()}"
        val handler = heartbeatQueue.removeFirstOrNull()
            ?: { Resource.Success(CoreSessionHeartbeatResponse(status = "ok")) }
        return handler(request)
    }

    override suspend fun logout(request: CoreSessionLogoutRequest): Resource<CoreSessionLogoutResponse> {
        calls += "logout:${request.sessionId}:${request.deactivateDeviceToken}"
        return Resource.Success(CoreSessionLogoutResponse(status = "ok"))
    }
}

class CoreSessionCoordinatorTest {
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    private fun env(): Triple<AppSettings, AccountSessionStore, FakeCoreSessionGateway> {
        val settings = AppSettings(MemorySettings(), json)
        settings.setString(AppSettingsKeys.FCM_TOKEN, "fcm-1")
        settings.setString(AppSettingsKeys.STABLE_DEVICE_ID, "device-1")
        val store = AccountSessionStore(settings, json)
        return Triple(settings, store, FakeCoreSessionGateway())
    }

    private fun TestScope.coordinator(
        settings: AppSettings,
        store: AccountSessionStore,
        gateway: FakeCoreSessionGateway,
    ) = CoreSessionCoordinator(
        settings = settings,
        gateway = gateway,
        accountStore = store,
        appScope = this,
        platformId = { "android" },
        deviceId = { settings.getStringOrNull(AppSettingsKeys.STABLE_DEVICE_ID) ?: "device-1" },
        deviceName = { "test" },
        appVersion = { "test" },
        heartbeatIntervalMs = 60 * 60 * 1000L,
        backoffDelaysMs = emptyList(),
        onSessionReady = {},
    )

    private fun seedAccount(
        settings: AppSettings,
        store: AccountSessionStore,
        login: String,
        fullName: String,
        token: String,
        sessionId: String? = null,
    ): AccountSlot {
        settings.setString(AppSettingsKeys.TOKEN_KEY, token)
        settings.setString(AppSettingsKeys.PERSONAL_DATA, fullName)
        settings.setString(AppSettingsKeys.ACCOUNT_LOGIN, login)
        settings.setString(AppSettingsKeys.DEPARTMENT, "dept")
        if (sessionId != null) {
            settings.setString(AppSettingsKeys.CORE_SESSION_ID, sessionId)
        }
        store.ensureMigrated()
        if (store.accounts().none { it.login.equals(login, true) }) {
            store.markPendingCreateNewSlot()
            store.addOrReuseFromSettings(login)
        } else {
            store.bindSuccessfulLogin(login)
        }
        return store.accounts().first { it.login.equals(login, true) }
    }

    @Test
    fun singleAccountBootstrapHeartbeatLogoutReloginDoesNotNeedRegister() = runTest {
        val (settings, store, gateway) = env()
        seedAccount(settings, store, "a", "User A", "tok-a")
        val coordinator = coordinator(settings, store, gateway)
        gateway.enqueueBootstrap("sess-a")
        val first = coordinator.bootstrapCurrentUserAfterCrmLogin()
        assertIs<CoreSessionResult.Ok>(first)
        coordinator.commitAndStartHeartbeat(first.sessionId)
        assertEquals("sess-a", coordinator.currentSessionId())
        coordinator.heartbeatOnce()
        coordinator.logoutActive(deactivateDeviceToken = true)
        assertEquals("", coordinator.currentSessionId())
        gateway.enqueueBootstrap("sess-a2")
        val second = coordinator.bootstrapCurrentUserAfterCrmLogin()
        assertIs<CoreSessionResult.Ok>(second)
        assertEquals(listOf("bootstrap:a:User A:device-1:fcm-1", "heartbeat:sess-a:fcm-1", "logout:sess-a:true", "bootstrap:a:User A:device-1:fcm-1"), gateway.calls)
        assertFalse(gateway.calls.any { it.startsWith("register") })
        coordinator.stopHeartbeat()
    }

    @Test
    fun addBStopsHeartbeatAAfterSuccessfulBootstrap() = runTest {
        val (settings, store, gateway) = env()
        seedAccount(settings, store, "a", "User A", "tok-a", sessionId = "sess-a")
        val coordinator = coordinator(settings, store, gateway)
        coordinator.commitAndStartHeartbeat("sess-a")
        store.snapshotActiveFromSettings()
        store.markPendingCreateNewSlot()
        settings.setString(AppSettingsKeys.TOKEN_KEY, "tok-b")
        settings.setString(AppSettingsKeys.PERSONAL_DATA, "User B")
        settings.setString(AppSettingsKeys.ACCOUNT_LOGIN, "b")
        gateway.enqueueBootstrap("sess-b")
        val result = coordinator.bootstrapCurrentUserAfterCrmLogin()
        assertIs<CoreSessionResult.Ok>(result)
        store.bindSuccessfulLogin("b")
        coordinator.commitAndStartHeartbeat(result.sessionId)
        coordinator.heartbeatOnce()
        assertEquals("b", store.activeAccount()?.login)
        assertEquals("sess-b", coordinator.currentSessionId())
        assertEquals("heartbeat:sess-b:fcm-1", gateway.calls.last())
        assertFalse(gateway.calls.any { it.startsWith("heartbeat:sess-a") })
        coordinator.stopHeartbeat()
    }

    @Test
    fun switchBtoADoesNotLogoutAndCommitsOnlyAfterBootstrap() = runTest {
        val (settings, store, gateway) = env()
        seedAccount(settings, store, "a", "User A", "tok-a", sessionId = "sess-a")
        settings.setString(AppSettingsKeys.TOKEN_KEY, "tok-b")
        settings.setString(AppSettingsKeys.PERSONAL_DATA, "User B")
        settings.setString(AppSettingsKeys.ACCOUNT_LOGIN, "b")
        store.markPendingCreateNewSlot()
        store.addOrReuseFromSettings("b")
        val slotA = store.accounts().first { it.login == "a" }
        val coordinator = coordinator(settings, store, gateway)
        coordinator.commitAndStartHeartbeat("sess-b")
        gateway.enqueueBootstrap("sess-a2")
        val switched = coordinator.switchTo(slotA)
        assertIs<CoreSessionResult.Ok>(switched)
        assertEquals("a", store.activeAccount()?.login)
        assertEquals("sess-a2", coordinator.currentSessionId())
        assertFalse(gateway.calls.any { it.startsWith("logout") })
        assertTrue(gateway.calls.first { it.startsWith("bootstrap") }.contains("User A"))
        coordinator.stopHeartbeat()
    }

    @Test
    fun bootstrapBErrorKeepsActiveAAndHeartbeatA() = runTest {
        val (settings, store, gateway) = env()
        seedAccount(settings, store, "a", "User A", "tok-a", sessionId = "sess-a")
        settings.setString(AppSettingsKeys.TOKEN_KEY, "tok-b")
        settings.setString(AppSettingsKeys.PERSONAL_DATA, "User B")
        settings.setString(AppSettingsKeys.ACCOUNT_LOGIN, "b")
        store.markPendingCreateNewSlot()
        store.addOrReuseFromSettings("b")
        store.activate(store.accounts().first { it.login == "a" }.id)
        settings.setString(AppSettingsKeys.CORE_SESSION_ID, "sess-a")
        val slotB = store.accounts().first { it.login == "b" }
        val coordinator = coordinator(settings, store, gateway)
        coordinator.commitAndStartHeartbeat("sess-a")
        gateway.enqueueBootstrapError(500, "server")
        val result = coordinator.switchTo(slotB)
        assertIs<CoreSessionResult.Error>(result)
        assertEquals("a", store.activeAccount()?.login)
        assertEquals("sess-a", coordinator.currentSessionId())
        coordinator.heartbeatOnce()
        assertTrue(gateway.calls.any { it.startsWith("heartbeat:sess-a") })
        assertFalse(gateway.calls.any { it.startsWith("logout") })
        coordinator.stopHeartbeat()
    }

    @Test
    fun heartbeat404PerformsSingleBootstrapRecovery() = runTest {
        val (settings, store, gateway) = env()
        seedAccount(settings, store, "a", "User A", "tok-a", sessionId = "sess-old")
        val coordinator = coordinator(settings, store, gateway)
        coordinator.commitAndStartHeartbeat("sess-old")
        gateway.enqueueHeartbeatNotFound()
        gateway.enqueueBootstrap("sess-new")
        coordinator.heartbeatOnce()
        assertEquals("sess-new", coordinator.currentSessionId())
        assertEquals(1, gateway.calls.count { it.startsWith("bootstrap") })
        coordinator.stopHeartbeat()
    }

    @Test
    fun fcmRotationHeartbeatsActiveSessionOrBootstrapsWhenMissing() = runTest {
        val (settings, store, gateway) = env()
        seedAccount(settings, store, "a", "User A", "tok-a", sessionId = "sess-a")
        val coordinator = coordinator(settings, store, gateway)
        coordinator.commitAndStartHeartbeat("sess-a")
        coordinator.onFcmToken("fcm-2")
        assertTrue(gateway.calls.any { it == "heartbeat:sess-a:fcm-2" })
        coordinator.logoutActive(deactivateDeviceToken = false)
        gateway.enqueueBootstrap("sess-a3")
        coordinator.onFcmToken("fcm-3")
        assertTrue(gateway.calls.any { it.startsWith("bootstrap:a:User A:device-1:fcm-3") })
        assertEquals("sess-a3", coordinator.currentSessionId())
        coordinator.stopHeartbeat()
    }

    @Test
    fun doubleSwitchIsSingleFlight() = runTest(StandardTestDispatcher()) {
        val (settings, store, gateway) = env()
        seedAccount(settings, store, "a", "User A", "tok-a", sessionId = "sess-a")
        settings.setString(AppSettingsKeys.TOKEN_KEY, "tok-b")
        settings.setString(AppSettingsKeys.PERSONAL_DATA, "User B")
        settings.setString(AppSettingsKeys.ACCOUNT_LOGIN, "b")
        store.markPendingCreateNewSlot()
        store.addOrReuseFromSettings("b")
        store.activate(store.accounts().first { it.login == "a" }.id)
        settings.setString(AppSettingsKeys.CORE_SESSION_ID, "sess-a")
        val slotB = store.accounts().first { it.login == "b" }
        val hang = CompletableDeferred<Unit>()
        gateway.bootstrapHang = hang
        gateway.enqueueBootstrap("sess-b")
        gateway.enqueueBootstrap("sess-b2")
        val coordinator = coordinator(settings, store, gateway)
        coroutineScope {
            val first = async { coordinator.switchTo(slotB) }
            val second = async { coordinator.switchTo(slotB) }
            testScheduler.runCurrent()
            assertEquals(1, gateway.calls.count { it.startsWith("bootstrap") })
            hang.complete(Unit)
            first.await()
            second.await()
        }
        assertEquals(1, gateway.calls.count { it.startsWith("bootstrap") })
        coordinator.stopHeartbeat()
    }

    @Test
    fun processRestartRestoresSelectedSlotAndStableDeviceId() = runTest {
        val (settings, store, gateway) = env()
        seedAccount(settings, store, "a", "User A", "tok-a", sessionId = "sess-a")
        val first = coordinator(settings, store, gateway)
        first.commitAndStartHeartbeat("sess-a")
        first.stopHeartbeat()
        val restoredStore = AccountSessionStore(settings, json)
        restoredStore.ensureMigrated()
        val second = coordinator(settings, restoredStore, gateway)
        val ensured = second.ensureActiveSession("restart")
        assertIs<CoreSessionResult.Ok>(ensured)
        assertEquals("a", restoredStore.activeAccount()?.login)
        assertEquals("device-1", settings.getStringOrNull(AppSettingsKeys.STABLE_DEVICE_ID))
        assertEquals("sess-a", second.currentSessionId())
        assertEquals(0, gateway.calls.count { it.startsWith("bootstrap") })
        second.stopHeartbeat()
    }

    @Test
    fun conflict409IsNotRetried() = runTest {
        val (settings, store, gateway) = env()
        seedAccount(settings, store, "a", "User A", "tok-a")
        val coordinator = coordinator(settings, store, gateway)
        gateway.enqueueBootstrapError(409, "ambiguous")
        val result = coordinator.bootstrapCurrentUserAfterCrmLogin()
        assertIs<CoreSessionResult.Error>(result)
        assertEquals(CoreApiErrorKind.Conflict, result.kind)
        assertEquals(1, gateway.calls.count { it.startsWith("bootstrap") })
        coordinator.stopHeartbeat()
    }
}
