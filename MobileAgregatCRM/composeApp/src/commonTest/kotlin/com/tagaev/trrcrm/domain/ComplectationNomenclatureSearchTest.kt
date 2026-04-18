package com.tagaev.trrcrm.domain

import kotlin.test.Test
import kotlin.test.assertEquals

class ComplectationNomenclatureSearchTest {

    @Test
    fun stripsCyrillicSerialNumberPrefix() {
        assertEquals("ЦБ153214", complectationSearchTokenFromNomenclatureCharacteristic("с/н ЦБ153214"))
    }

    @Test
    fun stripsDashVariant() {
        assertEquals("ABC12", complectationSearchTokenFromNomenclatureCharacteristic("с-н ABC12"))
    }

    @Test
    fun emptyWhenOnlyPrefix() {
        assertEquals("", complectationSearchTokenFromNomenclatureCharacteristic("с/н  "))
    }
}
