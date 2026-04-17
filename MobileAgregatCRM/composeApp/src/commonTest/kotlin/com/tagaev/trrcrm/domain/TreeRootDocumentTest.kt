package com.tagaev.trrcrm.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class TreeRootDocumentTest {

    @Test
    fun parseWorkOrderWithHyphen() {
        val parsed = TreeRootDocument.parse("Заказ-наряд 0000530288 от 09.04.2026 16:29:54")
        assertNotNull(parsed)
        assertEquals("ЗаказНаряд", parsed.requestName)
        assertEquals(TreeRootDocumentKind.WORK_ORDER, parsed.kind)
        assertEquals("0000530288", parsed.documentNumber)
    }

    @Test
    fun parseWorkOrderWithoutHyphen() {
        val parsed = TreeRootDocument.parse("Заказ наряд 123 от 01.01.2026 12:00:00")
        assertNotNull(parsed)
        assertEquals("ЗаказНаряд", parsed.requestName)
        assertEquals("123", parsed.documentNumber)
    }

    @Test
    fun parseBuyerOrder() {
        val parsed = TreeRootDocument.parse("Заказ покупателя 777 от 01.01.2026 12:00:00")
        assertNotNull(parsed)
        assertEquals(TreeRootDocumentKind.BUYER_ORDER, parsed.kind)
        assertEquals("ЗаказПокупателя", parsed.requestName)
        assertEquals("777", parsed.documentNumber)
    }

    @Test
    fun parseUnknownTypeReturnsNull() {
        val parsed = TreeRootDocument.parse("Неизвестный документ 777 от 01.01.2026")
        assertNull(parsed)
    }
}
