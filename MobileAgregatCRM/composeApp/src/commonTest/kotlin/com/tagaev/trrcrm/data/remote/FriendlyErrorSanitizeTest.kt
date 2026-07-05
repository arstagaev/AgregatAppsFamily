package com.tagaev.trrcrm.data.remote

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class FriendlyErrorSanitizeTest {

    @Test
    fun userFacingMessage_stripsHttpUrl() {
        val raw = "Client request(GET https://agrapp.agregatka.ru/api/v1/events) invalid"
        val result = userFacingMessage(raw, "Ошибка запроса")
        assertEquals("Ошибка запроса", result)
        assertFalse(result.contains("agregatka"))
    }

    @Test
    fun userFacingMessage_stripsHostWithoutScheme() {
        val raw = "trrservice.agregatka.ru:8777/api/v1/uploads/photos failed"
        val result = userFacingMessage(raw, "Не удалось отправить фото")
        assertEquals("Не удалось отправить фото", result)
    }

    @Test
    fun userFacingMessage_keepsSafeRussianText() {
        val safe = "Нет соединения с интернетом."
        assertEquals(safe, userFacingMessage(safe, "fallback"))
    }

    @Test
    fun userFacingMessage_keepsAuthMessage() {
        val safe = "Сессия истекла. Войдите заново"
        assertEquals(safe, userFacingMessage(safe, "fallback"))
    }

    @Test
    fun sanitizeMessage_removesHttpStatusWithUrl() {
        val raw = "HTTP 404 http://trrservice.agregatka.ru:8777/api/v1/uploads/photos"
        val sanitized = sanitizeMessage(raw)
        assertTrue(sanitized.isBlank() || !sanitized.contains("agregatka"))
    }

    @Test
    fun resourceError_userMessage_prefersSafeCauses() {
        val error = Resource.Error<String>(
            causes = "Сессия истекла. Войдите заново",
        )
        assertEquals("Сессия истекла. Войдите заново", error.userMessage("fallback"))
    }
}
