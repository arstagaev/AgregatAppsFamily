package com.tagaev.trrcrm.data.accounts

import com.russhwolf.settings.Settings
import com.tagaev.trrcrm.data.AppSettings
import com.tagaev.trrcrm.data.AppSettingsKeys
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue
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

class AccountSessionStoreTest {
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    private fun store(now: () -> Long = { 0L }, seed: AppSettings.() -> Unit = {}): Pair<AccountSessionStore, AppSettings> {
        val settings = AppSettings(MemorySettings(), json).apply(seed)
        return AccountSessionStore(settings, json, nowMs = now) to settings
    }

    @Test
    fun migratesSingleTokenWithoutRequiringPin() {
        val (store, _) = store {
            setString(AppSettingsKeys.TOKEN_KEY, "tok-a")
            setString(AppSettingsKeys.PERSONAL_DATA, "Иван Иванов")
            setString(AppSettingsKeys.ACCOUNT_LOGIN, "ivan")
        }
        store.ensureMigrated()
        assertEquals(1, store.accounts().size)
        assertFalse(store.needsPinGate())
        assertTrue(store.allPinsReady())
        assertEquals("Иван Иванов", store.activeAccount()?.fullName)
        assertFalse(store.activeAccount()?.hasPin == true)
    }

    @Test
    fun pinGateRequiredOnlyWhenTwoAccountsAndAnyPinMissing() {
        val (store, settings) = store {
            setString(AppSettingsKeys.TOKEN_KEY, "tok-a")
            setString(AppSettingsKeys.PERSONAL_DATA, "A User")
            setString(AppSettingsKeys.ACCOUNT_LOGIN, "a")
        }
        store.ensureMigrated()
        assertFalse(store.needsPinGate())
        settings.setString(AppSettingsKeys.TOKEN_KEY, "tok-b")
        settings.setString(AppSettingsKeys.PERSONAL_DATA, "B User")
        settings.setString(AppSettingsKeys.ACCOUNT_LOGIN, "b")
        store.markPendingCreateNewSlot()
        store.bindSuccessfulLogin("b")
        assertEquals(2, store.accounts().size)
        assertEquals(2, store.snapshotFlow.value.accounts.size)
        assertTrue(store.needsPinGate())
        val first = store.accounts().first { it.login == "a" }
        val second = store.accounts().first { it.login == "b" }
        assertEquals(SetPinResult.Ok, store.setPin(first.id, "1234"))
        assertTrue(store.needsPinGate())
        assertEquals(SetPinResult.Ok, store.setPin(second.id, "4321"))
        assertFalse(store.needsPinGate())
    }

    @Test
    fun rejectsFourthAccount() {
        val (store, settings) = store {
            setString(AppSettingsKeys.TOKEN_KEY, "t1")
            setString(AppSettingsKeys.PERSONAL_DATA, "One")
            setString(AppSettingsKeys.ACCOUNT_LOGIN, "one")
        }
        store.ensureMigrated()
        repeat(2) { index ->
            settings.setString(AppSettingsKeys.TOKEN_KEY, "t${index + 2}")
            settings.setString(AppSettingsKeys.PERSONAL_DATA, "User ${index + 2}")
            settings.setString(AppSettingsKeys.ACCOUNT_LOGIN, "user${index + 2}")
            store.markPendingCreateNewSlot()
            val result = store.addOrReuseFromSettings("user${index + 2}")
            assertIs<AddAccountResult.Created>(result)
        }
        settings.setString(AppSettingsKeys.TOKEN_KEY, "t4")
        settings.setString(AppSettingsKeys.PERSONAL_DATA, "Four")
        store.markPendingCreateNewSlot()
        assertEquals(AddAccountResult.LimitReached, store.addOrReuseFromSettings("four"))
        assertEquals(3, store.accounts().size)
    }

    @Test
    fun pinHashIsNotStoredInPlaintextAndWrongPinCountsDown() {
        val (store, _) = store {
            setString(AppSettingsKeys.TOKEN_KEY, "tok")
            setString(AppSettingsKeys.PERSONAL_DATA, "User")
        }
        store.ensureMigrated()
        val id = store.activeAccount()!!.id
        store.setPin(id, "2580")
        val slot = store.activeAccount()!!
        assertTrue(slot.hasPin)
        assertTrue("2580" !in slot.pinHash)
        assertTrue("2580" !in slot.pinSalt)
        val wrong = store.verifyPin(id, "0000")
        val result = assertIs<PinVerifyResult.Wrong>(wrong)
        assertEquals(9, result.remaining)
        assertFalse(result.warnAfterFive)
        assertEquals(PinVerifyResult.Ok, store.verifyPin(id, "2580"))
    }

    @Test
    fun lockoutTenMinutesThenHourAndResetAfterHourExpires() {
        var now = 1_000L
        val (store, _) = store(now = { now }) {
            setString(AppSettingsKeys.TOKEN_KEY, "tok")
            setString(AppSettingsKeys.PERSONAL_DATA, "User")
        }
        store.ensureMigrated()
        val id = store.activeAccount()!!.id
        store.setPin(id, "1111")
        repeat(5) { store.verifyPin(id, "0000") }
        val warn = assertIs<PinVerifyResult.Wrong>(store.verifyPin(id, "0000"))
        assertTrue(warn.warnAfterFive)
        assertEquals(4, warn.remaining)
        repeat(3) { store.verifyPin(id, "0000") }
        val locked10 = assertIs<PinVerifyResult.Locked>(store.verifyPin(id, "0000"))
        assertFalse(locked10.isHourLock)
        now = locked10.untilMs - 1
        assertIs<PinVerifyResult.Locked>(store.verifyPin(id, "1111"))
        now = locked10.untilMs
        repeat(9) { store.verifyPin(id, "0000") }
        val lockedHour = assertIs<PinVerifyResult.Locked>(store.verifyPin(id, "0000"))
        assertTrue(lockedHour.isHourLock)
        now = lockedHour.untilMs
        assertEquals(PinVerifyResult.Ok, store.verifyPin(id, "1111"))
    }

    @Test
    fun expiredTokenReauthDoesNotDropSlotOrPin() {
        val (store, settings) = store {
            setString(AppSettingsKeys.TOKEN_KEY, "old-token")
            setString(AppSettingsKeys.PERSONAL_DATA, "User")
            setString(AppSettingsKeys.ACCOUNT_LOGIN, "user")
        }
        store.ensureMigrated()
        val id = store.activeAccount()!!.id
        store.setPin(id, "9999")
        settings.setString(AppSettingsKeys.TOKEN_KEY, "new-token")
        store.bindSuccessfulLogin("user")
        val slot = store.activeAccount()!!
        assertEquals("new-token", slot.token)
        assertTrue(slot.hasPin)
        assertEquals("user", slot.login)
        assertEquals(1, store.accounts().size)
    }

    @Test
    fun logoutClearPreservesOtherAccounts() {
        val (store, settings) = store {
            setString(AppSettingsKeys.TOKEN_KEY, "tok-a")
            setString(AppSettingsKeys.PERSONAL_DATA, "A")
            setString(AppSettingsKeys.ACCOUNT_LOGIN, "a")
        }
        store.ensureMigrated()
        settings.setString(AppSettingsKeys.TOKEN_KEY, "tok-b")
        settings.setString(AppSettingsKeys.PERSONAL_DATA, "B")
        store.markPendingCreateNewSlot()
        store.bindSuccessfulLogin("b")
        store.removeAccount(store.activeAccount()!!.id)
        settings.setString(AppSettingsKeys.FCM_TOKEN, "fcm")
        settings.clearForLogoutPreservingInstallIdentity()
        val restored = AccountSessionStore(settings, json)
        restored.ensureMigrated()
        assertEquals(1, restored.accounts().size)
        assertEquals("A", restored.accounts().single().fullName)
        assertEquals("fcm", settings.getStringOrNull(AppSettingsKeys.FCM_TOKEN))
    }

    @Test
    fun activateWritesSessionKeysWithoutTouchingOtherTokens() {
        val (store, settings) = store {
            setString(AppSettingsKeys.TOKEN_KEY, "tok-a")
            setString(AppSettingsKeys.PERSONAL_DATA, "A")
            setString(AppSettingsKeys.ACCOUNT_LOGIN, "a")
        }
        store.ensureMigrated()
        settings.setString(AppSettingsKeys.TOKEN_KEY, "tok-b")
        settings.setString(AppSettingsKeys.PERSONAL_DATA, "B")
        store.markPendingCreateNewSlot()
        store.bindSuccessfulLogin("b")
        val a = store.accounts().first { it.login == "a" }
        store.activate(a.id)
        assertEquals("tok-a", settings.getStringOrNull(AppSettingsKeys.TOKEN_KEY))
        assertEquals("A", settings.getStringOrNull(AppSettingsKeys.PERSONAL_DATA))
        assertEquals("tok-b", store.accounts().first { it.login == "b" }.token)
    }
}
