package com.tagaev.trrcrm.models

import com.tagaev.trrcrm.data.fixator.UploadSessionQuotaTracker
import com.tagaev.trrcrm.data.remote.canUploadBlockedMessage
import com.tagaev.trrcrm.ui.i18n.AppLanguage
import com.tagaev.trrcrm.ui.i18n.AppLanguageHolder
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest

class FixatorUploadQuotaTest {

    @BeforeTest
    fun useRussian() {
        AppLanguageHolder.current = AppLanguage.Russian
    }

    @Test
    fun availableNow_isZeroWhenSessionExhausted() {
        val availability = UploadAvailability(
            serverRemaining = 480,
            sessionRemaining = 0,
            requestLimit = 10,
            photosInFolder = 20,
            maxPhotosPerDocument = 500,
            uploadedInAppRun = 15,
        )
        assertEquals(0, availability.availableNow)
    }

    @Test
    fun afterSuccessfulUpload_reducesSessionAndServerRemaining() {
        val availability = UploadAvailability(
            serverRemaining = 480,
            sessionRemaining = 5,
            requestLimit = 10,
            photosInFolder = 20,
            maxPhotosPerDocument = 500,
            uploadedInAppRun = 10,
        )
        val next = availability.afterSuccessfulUpload(5)
        assertEquals(475, next.serverRemaining)
        assertEquals(0, next.sessionRemaining)
        assertEquals(25, next.photosInFolder)
        assertEquals(15, next.uploadedInAppRun)
        assertEquals(0, next.availableNow)
    }

    @Test
    fun from_withZeroSessionRemaining_availableNowIsZero() {
        val response = ImageMediatorCanUploadResponse(
            allowed = true,
            documentNumber = "0000549041",
            folderFound = true,
            limits = ImageMediatorLimits(
                maxPhotosPerDocument = 500,
                photosInFolder = 20,
                remaining = 480,
                maxFilesPerRequest = 10,
            ),
        )
        val availability = UploadAvailability.from(
            response = response,
            sessionRemaining = 0,
            uploadedInAppRun = 15,
        )
        assertEquals(0, availability.sessionRemaining)
        assertEquals(0, availability.availableNow)
        assertEquals(15, availability.uploadedInAppRun)
    }

    @Test
    fun tracker_afterCommit15_remainingIsZeroForAvailability() = runTest {
        val tracker = UploadSessionQuotaTracker()
        val key = DocumentUploadKey(ImageDocumentType.Complects, "0000549041")
        tracker.tryReserve(key, 15)?.let { tracker.commit(it, confirmedCount = 15) }
        assertEquals(0, tracker.remaining(key))
        assertEquals(15, tracker.uploadedCount(key))

        val response = ImageMediatorCanUploadResponse(
            allowed = true,
            documentNumber = "0000549041",
            folderFound = true,
            limits = ImageMediatorLimits(
                remaining = 485,
                maxFilesPerRequest = 10,
                maxPhotosPerDocument = 500,
                photosInFolder = 15,
            ),
        )
        val availability = UploadAvailability.from(
            response = response,
            sessionRemaining = tracker.remaining(key),
            uploadedInAppRun = tracker.uploadedCount(key),
        )
        assertEquals(0, availability.availableNow)
    }

    @Test
    fun availableNow_isMinOfServerSessionAndRequest() {
        val availability = UploadAvailability(
            serverRemaining = 480,
            sessionRemaining = 15,
            requestLimit = 10,
            photosInFolder = 20,
            maxPhotosPerDocument = 500,
            uploadedInAppRun = 0,
        )
        assertEquals(10, availability.availableNow)
    }

    @Test
    fun availableNow_capsAtSessionRemaining() {
        val availability = UploadAvailability(
            serverRemaining = 480,
            sessionRemaining = 5,
            requestLimit = 10,
            photosInFolder = 20,
            maxPhotosPerDocument = 500,
            uploadedInAppRun = 10,
        )
        assertEquals(5, availability.availableNow)
    }

    @Test
    fun availableNow_capsAtServerRemaining() {
        val availability = UploadAvailability(
            serverRemaining = 3,
            sessionRemaining = 15,
            requestLimit = 10,
            photosInFolder = 497,
            maxPhotosPerDocument = 500,
            uploadedInAppRun = 0,
        )
        assertEquals(3, availability.availableNow)
    }

    @Test
    fun from_prefersNewLimitFieldsOverLegacyAliases() {
        val response = ImageMediatorCanUploadResponse(
            allowed = true,
            documentNumber = "0000549041",
            folderFound = true,
            limits = ImageMediatorLimits(
                maxPhotosPerDocument = 500,
                photosInFolder = 4,
                maxPhotosPerUser30Min = 20,
                usedLast30Min = 99,
                remaining = 496,
                maxFilesPerRequest = 10,
            ),
        )
        val availability = UploadAvailability.from(
            response = response,
            sessionRemaining = 15,
            uploadedInAppRun = 0,
        )
        assertEquals(500, availability.maxPhotosPerDocument)
        assertEquals(4, availability.photosInFolder)
        assertEquals(496, availability.serverRemaining)
        assertEquals(10, availability.requestLimit)
        assertEquals(10, availability.availableNow)
    }

    @Test
    fun from_fallsBackToLegacyAliases() {
        val response = ImageMediatorCanUploadResponse(
            allowed = true,
            documentNumber = "0000549041",
            folderFound = true,
            limits = ImageMediatorLimits(
                maxPhotosPerUser30Min = 500,
                usedLast30Min = 10,
                remaining = 5,
                maxFilesPerRequest = 10,
            ),
        )
        val availability = UploadAvailability.from(response, sessionRemaining = 15, uploadedInAppRun = 0)
        assertEquals(500, availability.maxPhotosPerDocument)
        assertEquals(10, availability.photosInFolder)
        assertEquals(5, availability.availableNow)
    }

    @Test
    fun canUploadBlocked_folderUnavailable() {
        val message = canUploadBlockedMessage(
            ImageMediatorCanUploadResponse(
                allowed = false,
                documentNumber = "0000549041",
                folderFound = false,
                limits = ImageMediatorLimits(remaining = 0),
            ),
        )
        assertTrue(message.contains("Папка") || message.contains("недоступна"), message)
        assertTrue(!message.contains("30"), message)
    }

    @Test
    fun canUploadBlocked_documentFull_usesDynamicLimit() {
        val message = canUploadBlockedMessage(
            ImageMediatorCanUploadResponse(
                allowed = false,
                documentNumber = "0000549041",
                folderFound = true,
                limits = ImageMediatorLimits(
                    remaining = 0,
                    maxPhotosPerDocument = 500,
                    photosInFolder = 500,
                ),
            ),
        )
        assertTrue(message.contains("500"), message)
        assertTrue(!message.contains("30"), message)
    }

    @Test
    fun tracker_parallelReserveDoesNotExceed15() = runTest {
        val tracker = UploadSessionQuotaTracker()
        val key = DocumentUploadKey(ImageDocumentType.Complects, "0000549041")
        val first = tracker.tryReserve(key, 10)
        val second = tracker.tryReserve(key, 5)
        val third = tracker.tryReserve(key, 1)
        assertNotNull(first)
        assertNotNull(second)
        assertNull(third)
        tracker.commit(first!!, confirmedCount = 10)
        tracker.commit(second!!, confirmedCount = 5)
        assertEquals(0, tracker.remaining(key))
        assertEquals(15, tracker.uploadedCount(key))
    }

    @Test
    fun tracker_releaseDoesNotConsumeQuota() = runTest {
        val tracker = UploadSessionQuotaTracker()
        val key = DocumentUploadKey(ImageDocumentType.WorkOrder, "0000549041")
        val reservation = tracker.tryReserve(key, 10)
        assertNotNull(reservation)
        tracker.release(reservation!!)
        assertEquals(15, tracker.remaining(key))
        assertEquals(0, tracker.uploadedCount(key))
    }

    @Test
    fun tracker_differentTypesIndependent() = runTest {
        val tracker = UploadSessionQuotaTracker()
        val a = DocumentUploadKey(ImageDocumentType.Complects, "0000549041")
        val b = DocumentUploadKey(ImageDocumentType.WorkOrder, "0000549041")
        tracker.tryReserve(a, 15)?.let { tracker.commit(it) }
        assertEquals(0, tracker.remaining(a))
        assertEquals(15, tracker.remaining(b))
    }

    @Test
    fun tracker_newInstanceResets() = runTest {
        val key = DocumentUploadKey(ImageDocumentType.Complects, "1")
        val first = UploadSessionQuotaTracker()
        first.tryReserve(key, 15)?.let { first.commit(it) }
        val second = UploadSessionQuotaTracker()
        assertEquals(15, second.remaining(key))
    }
}
