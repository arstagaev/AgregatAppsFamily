package com.tagaev.trrcrm.data.featureflags

import com.russhwolf.settings.Settings
import com.tagaev.trrcrm.data.AppSettings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
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

class MobileFeatureFlagsStoreTest {

    private fun store(settings: Settings = MemorySettings()): MobileFeatureFlagsStore {
        val json = Json { ignoreUnknownKeys = true }
        return MobileFeatureFlagsStore(settings = AppSettings(settings, json), json = json)
    }

    @Test
    fun missingFlag_defaultsToFalse() = runTest {
        val s = store()
        assertFalse(s.isPhotosUploadWorkOrdersEtcEnabled())
        assertFalse(s.isPhotosDownloadWorkOrdersEtcEnabled())
        assertFalse(s.isPhotosInnerOrderEnabled())
        assertFalse(s.isPhotosEventsEnabled())
        assertFalse(s.isPhotosCargoEnabled())
    }

    @Test
    fun trueAllowsFeatures() = runTest {
        val s = store()
        s.applyFlags(
            mapOf(
                MobileFeatureFlagsStore.KEY_PHOTOS_UPLOAD_WORK_ORDERS_ETC to true,
                MobileFeatureFlagsStore.KEY_PHOTOS_DOWNLOAD_WORK_ORDERS_ETC to true,
                MobileFeatureFlagsStore.KEY_PHOTOS_INNER_ORDER to true,
                MobileFeatureFlagsStore.KEY_PHOTOS_EVENTS to true,
                MobileFeatureFlagsStore.KEY_PHOTOS_CARGO to true,
            ),
            incomingRevision = "r1",
        )
        assertTrue(s.isPhotosUploadWorkOrdersEtcEnabled())
        assertTrue(s.isPhotosDownloadWorkOrdersEtcEnabled())
        assertTrue(s.isPhotosInnerOrderEnabled())
        assertTrue(s.isPhotosEventsEnabled())
        assertTrue(s.isPhotosCargoEnabled())
    }

    @Test
    fun falseBlocksDownloadButNotUploadIndependently() = runTest {
        val s = store()
        s.applyFlags(
            mapOf(
                MobileFeatureFlagsStore.KEY_PHOTOS_UPLOAD_WORK_ORDERS_ETC to true,
                MobileFeatureFlagsStore.KEY_PHOTOS_DOWNLOAD_WORK_ORDERS_ETC to false,
            ),
        )
        assertTrue(s.isPhotosUploadWorkOrdersEtcEnabled())
        assertFalse(s.isPhotosDownloadWorkOrdersEtcEnabled())
    }

    @Test
    fun falseBlocksUpload() = runTest {
        val s = store()
        s.applyFlags(
            mapOf(
                MobileFeatureFlagsStore.KEY_PHOTOS_UPLOAD_WORK_ORDERS_ETC to false,
                MobileFeatureFlagsStore.KEY_PHOTOS_DOWNLOAD_WORK_ORDERS_ETC to true,
            ),
        )
        assertFalse(s.isPhotosUploadWorkOrdersEtcEnabled())
        assertTrue(s.isPhotosDownloadWorkOrdersEtcEnabled())
    }

    @Test
    fun cargoFalseDoesNotAffectOthers() = runTest {
        val s = store()
        s.applyFlags(
            mapOf(
                MobileFeatureFlagsStore.KEY_PHOTOS_CARGO to false,
                MobileFeatureFlagsStore.KEY_PHOTOS_EVENTS to true,
                MobileFeatureFlagsStore.KEY_PHOTOS_INNER_ORDER to true,
                MobileFeatureFlagsStore.KEY_PHOTOS_UPLOAD_WORK_ORDERS_ETC to true,
            ),
        )
        assertFalse(s.isPhotosCargoEnabled())
        assertTrue(s.isPhotosEventsEnabled())
        assertTrue(s.isPhotosInnerOrderEnabled())
        assertTrue(s.isPhotosUploadWorkOrdersEtcEnabled())
    }

    @Test
    fun documentTypeCombinedFlagsBlockIndependently() = runTest {
        val s = store()
        s.applyFlags(
            mapOf(
                MobileFeatureFlagsStore.KEY_PHOTOS_INNER_ORDER to false,
                MobileFeatureFlagsStore.KEY_PHOTOS_EVENTS to false,
                MobileFeatureFlagsStore.KEY_PHOTOS_CARGO to true,
            ),
        )
        assertFalse(s.isPhotosInnerOrderEnabled())
        assertFalse(s.isPhotosEventsEnabled())
        assertTrue(s.isPhotosCargoEnabled())
    }

    @Test
    fun emptyApplyDoesNotEraseCache() = runTest {
        val s = store()
        s.applyFlags(
            mapOf(MobileFeatureFlagsStore.KEY_PHOTOS_DOWNLOAD_WORK_ORDERS_ETC to false),
            incomingRevision = "keep",
        )
        s.applyFlags(null)
        s.applyFlags(emptyMap())
        assertFalse(s.isPhotosDownloadWorkOrdersEtcEnabled())
        assertEquals("keep", s.currentRevision())
    }

    @Test
    fun keysAbsentFromAppliedMapAreOff() = runTest {
        val s = store()
        s.applyFlags(mapOf("some_other_flag" to true))
        assertFalse(s.isPhotosUploadWorkOrdersEtcEnabled())
        assertFalse(s.isPhotosDownloadWorkOrdersEtcEnabled())
        assertTrue(s.isEnabled("some_other_flag"))
    }

    @Test
    fun bootstrapThenHeartbeatUpdates() = runTest {
        val s = store()
        s.applyFlags(
            mapOf(MobileFeatureFlagsStore.KEY_PHOTOS_UPLOAD_WORK_ORDERS_ETC to false),
            incomingRevision = "boot",
        )
        assertFalse(s.isPhotosUploadWorkOrdersEtcEnabled())
        s.applyFlags(
            mapOf(MobileFeatureFlagsStore.KEY_PHOTOS_UPLOAD_WORK_ORDERS_ETC to true),
            incomingRevision = "hb",
        )
        assertTrue(s.isPhotosUploadWorkOrdersEtcEnabled())
        assertEquals("hb", s.currentRevision())
    }

    @Test
    fun persistsAcrossNewStoreInstance() = runTest {
        val memory = MemorySettings()
        val json = Json { ignoreUnknownKeys = true }
        val appSettings = AppSettings(memory, json)
        val first = MobileFeatureFlagsStore(appSettings, json)
        first.applyFlags(
            mapOf(MobileFeatureFlagsStore.KEY_PHOTOS_DOWNLOAD_WORK_ORDERS_ETC to false),
            incomingRevision = "disk",
        )
        val second = MobileFeatureFlagsStore(appSettings, json)
        assertFalse(second.isPhotosDownloadWorkOrdersEtcEnabled())
        assertEquals("disk", second.currentRevision())
    }
}
