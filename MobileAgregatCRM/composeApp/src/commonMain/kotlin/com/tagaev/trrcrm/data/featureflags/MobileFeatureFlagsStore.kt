package com.tagaev.trrcrm.data.featureflags

import com.tagaev.trrcrm.data.AppSettings
import com.tagaev.trrcrm.data.AppSettingsKeys
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json

/**
 * Process + disk cache for CoreService mobile feature toggles (`config.flags`).
 * Keys are client-facing (no `feature_toggle.mobile.` prefix).
 *
 * Precedence: latest successful apply → persisted cache → [DEFAULT_ENABLED] if key absent.
 * Absent key means the feature is off (server must send `true` explicitly to enable).
 */
class MobileFeatureFlagsStore(
    private val settings: AppSettings,
    private val json: Json = Json { ignoreUnknownKeys = true; isLenient = true },
) {
    companion object {
        const val KEY_PHOTOS_UPLOAD_WORK_ORDERS_ETC = "photos_upload_work_orders_etc"
        const val KEY_PHOTOS_DOWNLOAD_WORK_ORDERS_ETC = "photos_download_work_orders_etc"
        const val KEY_PHOTOS_INNER_ORDER = "photos_inner_order"
        const val KEY_PHOTOS_EVENTS = "photos_events"
        const val KEY_PHOTOS_CARGO = "photos_cargo"

        /** Missing key from server/cache → feature disabled. */
        const val DEFAULT_ENABLED = false

        private val KNOWN_KEYS = listOf(
            KEY_PHOTOS_UPLOAD_WORK_ORDERS_ETC,
            KEY_PHOTOS_DOWNLOAD_WORK_ORDERS_ETC,
            KEY_PHOTOS_INNER_ORDER,
            KEY_PHOTOS_EVENTS,
            KEY_PHOTOS_CARGO,
        )
    }

    private val mutex = Mutex()
    private var flags: Map<String, Boolean> = emptyMap()
    private var revision: String? = null
    private var loadedFromDisk = false

    suspend fun isPhotosUploadWorkOrdersEtcEnabled(): Boolean =
        isEnabled(KEY_PHOTOS_UPLOAD_WORK_ORDERS_ETC)

    suspend fun isPhotosDownloadWorkOrdersEtcEnabled(): Boolean =
        isEnabled(KEY_PHOTOS_DOWNLOAD_WORK_ORDERS_ETC)

    suspend fun isPhotosInnerOrderEnabled(): Boolean =
        isEnabled(KEY_PHOTOS_INNER_ORDER)

    suspend fun isPhotosEventsEnabled(): Boolean =
        isEnabled(KEY_PHOTOS_EVENTS)

    suspend fun isPhotosCargoEnabled(): Boolean =
        isEnabled(KEY_PHOTOS_CARGO)

    suspend fun isEnabled(key: String): Boolean = mutex.withLock {
        ensureLoadedLocked()
        flags[key] ?: DEFAULT_ENABLED
    }

    /**
     * Apply flags from bootstrap / heartbeat / GET mobile.
     * Null or empty [incoming] does not erase a previously valid cache.
     * [incomingRevision] is opaque — stored as-is, never parsed.
     */
    suspend fun applyFlags(incoming: Map<String, Boolean>?, incomingRevision: String? = null) {
        if (incoming.isNullOrEmpty()) return
        mutex.withLock {
            ensureLoadedLocked()
            flags = incoming
            if (incomingRevision != null) {
                revision = incomingRevision
            }
            persistLocked()
        }
    }

    suspend fun currentRevision(): String? = mutex.withLock {
        ensureLoadedLocked()
        revision
    }

    /** Snapshot for tests. */
    suspend fun snapshotFlags(): Map<String, Boolean> = mutex.withLock {
        ensureLoadedLocked()
        flags
    }

    /**
     * Logs effective ON/OFF for known keys (+ any extra cached keys).
     * Only when [com.tagaev.secrets.Secrets.IS_PUBLISH] is true.
     */
    suspend fun logEffectiveFlagsIfPublish(source: String) {
        if (!com.tagaev.secrets.Secrets.IS_PUBLISH.toBoolean()) return
        mutex.withLock {
            ensureLoadedLocked()
            val keys = (KNOWN_KEYS + flags.keys).distinct()
            println("FEATURE_FLAGS: dump source=$source revision=$revision")
            for (key in keys) {
                val enabled = flags[key] ?: DEFAULT_ENABLED
                val state = if (enabled) "ON" else "OFF"
                println("FEATURE_FLAGS: $key=$state")
            }
        }
    }

    private fun ensureLoadedLocked() {
        if (loadedFromDisk) return
        loadedFromDisk = true
        val raw = settings.getStringOrNull(AppSettingsKeys.MOBILE_FEATURE_FLAGS_JSON)
        if (!raw.isNullOrBlank()) {
            runCatching {
                json.decodeFromString(MapSerializer(String.serializer(), Boolean.serializer()), raw)
            }.onSuccess { flags = it }
        }
        revision = settings.getStringOrNull(AppSettingsKeys.MOBILE_FEATURE_FLAGS_REVISION)
    }

    private fun persistLocked() {
        val encoded = json.encodeToString(
            MapSerializer(String.serializer(), Boolean.serializer()),
            flags,
        )
        settings.setString(AppSettingsKeys.MOBILE_FEATURE_FLAGS_JSON, encoded)
        val rev = revision
        if (rev != null) {
            settings.setString(AppSettingsKeys.MOBILE_FEATURE_FLAGS_REVISION, rev)
        }
    }
}
