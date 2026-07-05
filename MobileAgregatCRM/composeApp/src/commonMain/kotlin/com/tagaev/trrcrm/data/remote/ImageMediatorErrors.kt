package com.tagaev.trrcrm.data.remote

import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.plugins.ServerResponseException

class ImageMediatorException(
    val statusCode: Int,
    val url: String,
    val responseBody: String,
) : Exception("HTTP $statusCode")

fun imageMediatorErrorMessage(statusCode: Int, fallback: String = "Не удалось отправить фото"): String =
    when (statusCode) {
        400 -> "Некорректные данные или формат изображения"
        401 -> "Сессия истекла. Войдите заново"
        404 -> "Папка документа не найдена. Повторите через ~10 мин"
        413 -> "Файл слишком большой"
        429 -> "Превышен лимит загрузок (20 фото / 30 мин)"
        in 500..599 -> "Ошибка сервера. Повторите позже"
        else -> fallback
    }

fun Throwable?.toImageMediatorError(fallback: String): String {
    if (this == null) return fallback
    return when (this) {
        is ImageMediatorException -> imageMediatorErrorMessage(statusCode, fallback)
        is ClientRequestException -> imageMediatorErrorMessage(response.status.value, friendlyError(this, fallback))
        is ServerResponseException -> imageMediatorErrorMessage(response.status.value, friendlyError(this, fallback))
        else -> friendlyError(this, fallback)
    }
}

fun canUploadBlockedMessage(response: com.tagaev.trrcrm.models.ImageMediatorCanUploadResponse): String {
    if (!response.folderFound) {
        return "Папка документа не найдена. Повторите через ~10 мин"
    }
    if (!response.allowed) {
        val remaining = response.limits?.remaining
        return if (remaining != null) {
            "Загрузка недоступна. Осталось фото: $remaining"
        } else {
            "Загрузка недоступна для этого документа"
        }
    }
    return "Загрузка недоступна"
}
