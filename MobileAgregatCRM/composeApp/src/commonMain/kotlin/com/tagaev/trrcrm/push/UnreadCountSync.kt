package com.tagaev.trrcrm.push

import com.tagaev.trrcrm.data.AppSettings
import com.tagaev.trrcrm.data.AppSettingsKeys
import com.tagaev.trrcrm.data.MainRepository
import com.tagaev.trrcrm.data.remote.CoreApiErrorKind
import com.tagaev.trrcrm.data.remote.Resource
import com.tagaev.trrcrm.data.remote.toCoreApiError
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

object UnreadCountSync : KoinComponent {
    private val appSettings: AppSettings by inject()
    private val repository: MainRepository by inject()
    private val appScope: CoroutineScope by inject()
    private val requestMutex = Mutex()
    private var lastRequestAtMs: Long = 0L

    private const val REQUEST_DEBOUNCE_MS = 3_000L

    fun refreshAsync(reason: String, force: Boolean = false) {
        appScope.launch {
            refreshNow(reason = reason, force = force)
        }
    }

    suspend fun refreshNow(reason: String, force: Boolean = false): Int {
        return requestMutex.withLock {
            val cached = appSettings.getInt(AppSettingsKeys.NOTIFICATIONS_UNREAD_COUNT, 0).coerceAtLeast(0)
            val now = currentTimeMillis()
            if (!force && now - lastRequestAtMs < REQUEST_DEBOUNCE_MS) {
                return@withLock cached
            }
            lastRequestAtMs = now

            val sessionId = appSettings.getStringOrNull(AppSettingsKeys.CORE_SESSION_ID).orEmpty().trim()
            if (sessionId.isBlank()) {
                return@withLock cached
            }

            when (val result = repository.coreNotificationsUnreadCount(sessionId)) {
                is Resource.Success -> {
                    val unread = result.data.unreadCount.coerceAtLeast(0)
                    appSettings.setInt(AppSettingsKeys.NOTIFICATIONS_UNREAD_COUNT, unread)
                    NotificationsUnreadState.setCount(unread)
                    println("PUSH_SERVICE: unread_count sync success reason=$reason unread=$unread")
                    unread
                }
                is Resource.Error -> {
                    val mapped = result.exception.toCoreApiError(result.causes ?: "Unread count failed")
                    if (mapped.kind == CoreApiErrorKind.NotFound) {
                        println("PUSH_SERVICE: unread_count sync got 404, recovering session")
                        val recovered = PushRegistrationCoordinator.recoverCoreSessionNow(
                            reason = "unread_count_404",
                            forceRebootstrap = true
                        )
                        if (recovered) {
                            val recoveredSessionId = appSettings.getStringOrNull(AppSettingsKeys.CORE_SESSION_ID).orEmpty().trim()
                            if (recoveredSessionId.isNotBlank()) {
                                return@withLock when (val retry = repository.coreNotificationsUnreadCount(recoveredSessionId)) {
                                    is Resource.Success -> {
                                        val unread = retry.data.unreadCount.coerceAtLeast(0)
                                        appSettings.setInt(AppSettingsKeys.NOTIFICATIONS_UNREAD_COUNT, unread)
                                        NotificationsUnreadState.setCount(unread)
                                        println("PUSH_SERVICE: unread_count sync retry success unread=$unread")
                                        unread
                                    }
                                    is Resource.Error -> {
                                        println("PUSH_SERVICE: unread_count sync retry failed ${retry.causes ?: retry.exception?.message}")
                                        cached
                                    }
                                    is Resource.Loading -> cached
                                }
                            }
                        }
                    }
                    println("PUSH_SERVICE: unread_count sync failed reason=$reason ${result.causes ?: result.exception?.message}")
                    cached
                }
                is Resource.Loading -> cached
            }
        }
    }

    @OptIn(ExperimentalTime::class)
    private fun currentTimeMillis(): Long = Clock.System.now().toEpochMilliseconds()
}
