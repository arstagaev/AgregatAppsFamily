package org.agregatcrm

import org.agregatcrm.ext.urlDecodeUtf8
import kotlin.test.Test
import kotlin.test.assertEquals

class ComposeAppCommonTest {

    @Test
    fun example() {
        val a = "https://api.aaaaaaaaa.ru/app/getdata.php?token=111111111&task=getitemslist&type=%D0%94%D0%BE%D0%BA%D1%83%D0%BC%D0%B5%D0%BD%D1%82&name=%D0%A1%D0%BE%D0%B1%D1%8B%D1%82%D0%B8%D0%B5&count=5&ncount=50&orderby=%D0%94%D0%B0%D1%82%D0%B0&orderdir=asc\n"
        println("${a.urlDecodeUtf8()}")
        assertEquals(3, 1 + 2)
    }
}