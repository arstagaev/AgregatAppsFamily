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
    fun validComplectsNumber_0000198950() {
        assertTrue(isValidDocumentNumber("0000198950"))
        assertEquals("0000198950", normalizeDocumentNumber("0000198950"))
    }

    @Test
    fun validWorkOrderNumber_0000560730() {
        assertTrue(isValidDocumentNumber("0000560730"))
    }

    @Test
    fun validCyrillicPrefix_tsk0000777() {
        assertTrue(isValidDocumentNumber("ТСК0000777"))
        assertEquals("ТСК0000777", normalizeDocumentNumber(" тск0000777 "))
    }

    @Test
    fun validCyrillicPrefix_ar0000337() {
        assertTrue(isValidDocumentNumber("АР0000337"))
        assertEquals("АР0000337", normalizeDocumentNumber("ар0000337"))
    }

    @Test
    fun nfcThenUppercase_composesYo() {
        val decomposed = "Е\u0308К0000337"
        assertEquals("ЁК0000337", normalizeDocumentNumber(decomposed))
        assertTrue(isValidDocumentNumber(decomposed))
    }

    @Test
    fun keepsFullNumber_withoutDigitFilter() {
        val raw = "ТСК0000777"
        val normalized = normalizeDocumentNumber(raw)
        assertEquals("ТСК0000777", normalized)
        assertFalse(normalized.all { it.isDigit() })
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
    fun invalidDocumentNumber_latinPrefix() {
        assertFalse(isValidDocumentNumber("ABC0000337"))
    }

    @Test
    fun invalidDocumentNumber_singleCyrillicLetter() {
        assertFalse(isValidDocumentNumber("А0000337"))
    }

    @Test
    fun invalidDocumentNumber_nonDigits() {
        assertFalse(isValidDocumentNumber("000054904A"))
    }
}
