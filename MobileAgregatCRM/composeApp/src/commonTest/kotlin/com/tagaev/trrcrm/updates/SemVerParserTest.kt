package com.tagaev.trrcrm.updates

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SemVerParserTest {

    @Test
    fun parse_valid_semver() {
        val parsed = SemVerParser.parseOrNull("1.24.300")
        assertEquals(SemVer(1, 24, 300), parsed)
    }

    @Test
    fun parse_invalid_semver_returns_null() {
        assertNull(SemVerParser.parseOrNull("1.0"))
        assertNull(SemVerParser.parseOrNull("v1.0.0"))
        assertNull(SemVerParser.parseOrNull("1.0.0-beta"))
    }

    @Test
    fun compare_semver_numerically() {
        assertTrue(SemVer(1, 10, 0) > SemVer(1, 2, 99))
        assertTrue(SemVer(2, 0, 0) > SemVer(1, 99, 99))
        assertTrue(SemVer(1, 2, 3) == SemVer(1, 2, 3))
    }
}

