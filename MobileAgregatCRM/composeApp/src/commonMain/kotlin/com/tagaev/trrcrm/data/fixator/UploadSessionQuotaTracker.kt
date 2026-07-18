package com.tagaev.trrcrm.data.fixator

import com.tagaev.trrcrm.models.DocumentUploadKey
import com.tagaev.trrcrm.models.MAX_PHOTOS_PER_DOCUMENT_PER_APP_RUN
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

data class UploadReservation(
    val key: DocumentUploadKey,
    val count: Int,
    val id: Long,
)

/**
 * Process-scoped photo upload quota: max [MAX_PHOTOS_PER_DOCUMENT_PER_APP_RUN]
 * successful uploads per [DocumentUploadKey] for the lifetime of the app process.
 *
 * Not persisted — a new process starts with empty counters.
 */
class UploadSessionQuotaTracker(
    private val maxPerDocument: Int = MAX_PHOTOS_PER_DOCUMENT_PER_APP_RUN,
) {
    private val mutex = Mutex()
    private val uploaded = mutableMapOf<DocumentUploadKey, Int>()
    private val reserved = mutableMapOf<DocumentUploadKey, Int>()
    private var nextReservationId = 1L

    suspend fun uploadedCount(key: DocumentUploadKey): Int = mutex.withLock {
        uploaded[key] ?: 0
    }

    suspend fun reservedCount(key: DocumentUploadKey): Int = mutex.withLock {
        reserved[key] ?: 0
    }

    suspend fun remaining(key: DocumentUploadKey): Int = mutex.withLock {
        remainingLocked(key)
    }

    /**
     * Atomically reserve [count] slots if `uploaded + reserved + count <= max`.
     * Call [commit] on success or [release] on failure/cancel.
     */
    suspend fun tryReserve(key: DocumentUploadKey, count: Int): UploadReservation? {
        if (count <= 0) return null
        return mutex.withLock {
            if (remainingLocked(key) < count) return@withLock null
            reserved[key] = (reserved[key] ?: 0) + count
            val id = nextReservationId++
            UploadReservation(key = key, count = count, id = id)
        }
    }

    /** Confirm reservation: move reserved slots into uploaded (use confirmed count if smaller). */
    suspend fun commit(reservation: UploadReservation, confirmedCount: Int = reservation.count) {
        mutex.withLock {
            val reservedNow = reserved[reservation.key] ?: 0
            val toRelease = minOf(reservation.count, reservedNow)
            reserved[reservation.key] = (reservedNow - toRelease).coerceAtLeast(0)
            if ((reserved[reservation.key] ?: 0) == 0) {
                reserved.remove(reservation.key)
            }
            val add = confirmedCount.coerceIn(0, reservation.count)
            if (add > 0) {
                uploaded[reservation.key] = (uploaded[reservation.key] ?: 0) + add
            }
        }
    }

    /** Drop reservation without counting toward the session limit. */
    suspend fun release(reservation: UploadReservation) {
        mutex.withLock {
            val reservedNow = reserved[reservation.key] ?: 0
            val toRelease = minOf(reservation.count, reservedNow)
            val next = (reservedNow - toRelease).coerceAtLeast(0)
            if (next == 0) reserved.remove(reservation.key) else reserved[reservation.key] = next
        }
    }

    private fun remainingLocked(key: DocumentUploadKey): Int {
        val used = (uploaded[key] ?: 0) + (reserved[key] ?: 0)
        return (maxPerDocument - used).coerceAtLeast(0)
    }
}
