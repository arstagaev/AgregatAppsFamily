package com.tagaev.trrcrm.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class FixatorPhotoSpecTest {

    @Test
    fun computeTargetSize_scalesDownLargePhoto() {
        val (width, height) = computeTargetSize(4000, 3000)
        assertEquals(2560, width)
        assertEquals(1920, height)
    }

    @Test
    fun computeTargetSize_doesNotUpscaleSmallPhoto() {
        val (width, height) = computeTargetSize(800, 600)
        assertEquals(800, width)
        assertEquals(600, height)
    }

    @Test
    fun computeTargetSize_preservesPortraitAspectRatio() {
        val (width, height) = computeTargetSize(3000, 4000)
        assertEquals(1920, width)
        assertEquals(2560, height)
    }

    @Test
    fun selectNextQuality_reducesQualityWhenOverHardMax() {
        assertEquals(75, selectNextQuality(83, FixatorPhotoSpec.HARD_MAX_BYTES + 1))
        assertEquals(65, selectNextQuality(75, FixatorPhotoSpec.HARD_MAX_BYTES + 1))
        assertNull(selectNextQuality(50, FixatorPhotoSpec.HARD_MAX_BYTES + 1))
    }

    @Test
    fun selectNextQuality_returnsNullWhenWithinHardMax() {
        assertNull(selectNextQuality(83, FixatorPhotoSpec.HARD_MAX_BYTES))
    }

    @Test
    fun selectNextMaxEdge_reducesByFifteenPercent() {
        assertEquals(2176, selectNextMaxEdge(2560))
    }

    @Test
    fun exceedsHardMax_detectsOversizedPhoto() {
        assertTrue(exceedsHardMax(FixatorPhotoSpec.HARD_MAX_BYTES.toInt() + 1))
    }
}
