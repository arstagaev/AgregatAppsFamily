package com.tagaev.trrcrm.models

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class DocumentUploadPeriodTest {

    @Test
    fun `document created in march 2025 resolves march 2025 upload period`() {
        assertEquals(DocumentUploadPeriod(2025, 3), DocumentUploadPeriod.from("12.03.2025 14:30:00"))
    }

    @Test
    fun `document date is used directly and does not depend on device current date`() {
        assertEquals(DocumentUploadPeriod(2025, 3), DocumentUploadPeriod.from("2025-03-12T14:30:00+05:00"))
    }

    @Test
    fun `missing creation date has no upload period`() {
        assertNull(DocumentUploadPeriod.from(null as String?))
        assertNull(DocumentUploadPeriod.from("  "))
    }
}
