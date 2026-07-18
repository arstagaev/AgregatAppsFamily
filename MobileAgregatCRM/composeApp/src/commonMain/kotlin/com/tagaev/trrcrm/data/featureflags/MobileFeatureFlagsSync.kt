package com.tagaev.trrcrm.data.featureflags

import com.tagaev.secrets.Secrets
import com.tagaev.trrcrm.data.MainRepository
import com.tagaev.trrcrm.data.remote.Resource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

/**
 * Fetches CoreService `GET /feature-toggles/mobile` after login.
 */
object MobileFeatureFlagsSync : KoinComponent {
    private val repository: MainRepository by inject()
    private val appScope: CoroutineScope by inject()
    private val requestMutex = Mutex()
    private var lastRequestAtMs: Long = 0L

    private const val REQUEST_DEBOUNCE_MS = 2_000L

    fun refreshAsync(reason: String, force: Boolean = false) {
        appScope.launch {
            refreshNow(reason = reason, force = force)
        }
    }

    suspend fun refreshNow(reason: String, force: Boolean = false): Boolean {
        return requestMutex.withLock {
            val now = currentTimeMillis()
            if (!force && now - lastRequestAtMs < REQUEST_DEBOUNCE_MS) {
                logPublish("FEATURE_FLAGS: mobile toggles refresh skipped (debounce) reason=$reason")
                return@withLock false
            }
            lastRequestAtMs = now

            when (val result = repository.refreshMobileFeatureToggles()) {
                is Resource.Success -> {
                    logPublish("FEATURE_FLAGS: mobile toggles refresh success reason=$reason")
                    true
                }
                is Resource.Error -> {
                    logPublish(
                        "FEATURE_FLAGS: mobile toggles refresh failed reason=$reason " +
                            "detail=${result.causes ?: result.exception?.message}",
                    )
                    false
                }
                is Resource.Loading -> false
            }
        }
    }

    private fun logPublish(message: String) {
        if (Secrets.IS_PUBLISH.toBoolean()) {
            println(message)
        }
    }

    @OptIn(ExperimentalTime::class)
    private fun currentTimeMillis(): Long = Clock.System.now().toEpochMilliseconds()
}
