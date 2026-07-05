package com.tagaev.trrcrm.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ImageMediatorValidationTest {

    @Test
    fun validDocumentNumber_withLeadingZeros() {
        assertTrue(isValidDocumentNumber("0000549041"))
        assertEquals("0000549041", normalizeDocumentNumber(" 0000549041 "))
    }

    @Test
    fun invalidDocumentNumber_tooShort() {
        assertFalse(isValidDocumentNumber("12345"))
    }

    @Test
    fun invalidDocumentNumber_tooLong() {
        assertFalse(isValidDocumentNumber("1234567890123"))
    }

    @Test
    fun invalidDocumentNumber_nonDigits() {
        assertFalse(isValidDocumentNumber("000054904A"))
    }
}
