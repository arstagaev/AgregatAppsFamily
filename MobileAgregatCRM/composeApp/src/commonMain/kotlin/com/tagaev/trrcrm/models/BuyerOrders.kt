package com.tagaev.trrcrm.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

typealias BuyerOrdersResponse = List<BuyerOrderDto>

@Serializable
data class BuyerOrderDto(
    @SerialName("Проведен") val posted: String? = null,
    @SerialName("ПометкаУдаления") val deletionMark: String? = null,
    @SerialName("Ссылка") val link: String? = null,
    @SerialName("Дата") val date: String? = null,
    @SerialName("Номер") val number: String? = null,
    @SerialName("Автор") val author: String? = null,
    @SerialName("Организация") val organization: String? = null,
    @SerialName("ПодразделениеКомпании") val branch: String? = null,
    @SerialName("Комментарий") val comment: String? = null,
    @SerialName("Контрагент") val counterparty: String? = null,
    @SerialName("Автомобиль") val car: String? = null,
    @SerialName("СуммаДокумента") val documentAmount: String? = null,
    @SerialName("ХозОперация") val operation: String? = null,
    @SerialName("ДокументОснование") val baseDocument: String? = null,
    @SerialName("Состояние") val status: String? = null,
    @SerialName("Менеджер") val manager: String? = null,
    @SerialName("ТекстМашины") val carText: String? = null,
    @SerialName("ПутьКФайлам") val filesPath: String? = null,
    @SerialName("НомерЗН") val workOrderRef: String? = null,
    @SerialName("Товары") val products: List<WorkOrderProductDto> = emptyList(),
    @SerialName("КакиеЗапчасти") val spareParts: List<WorkOrderProductDto> = emptyList(),
    @SerialName("Пользователи") val users: List<UserRowDto> = emptyList(),
    @SerialName("tasks") val tasks: List<TaskDto> = emptyList(),
    @SerialName("Задания") val tasksRu: List<TaskDto> = emptyList(),
    @SerialName("messages") val messages: List<WorkOrderMessageDto> = emptyList(),
    @SerialName("guid") val guid: String? = null,
)

