package com.tagaev.mobileagregatcrm.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

typealias WorkOrdersResponse = List<WorkOrderDto>

@Serializable
data class WorkOrderDto(
    @SerialName("Проведен") val posted: String? = null,
    @SerialName("Ссылка") val link: String? = null,
    @SerialName("Дата") val date: String? = null,
    @SerialName("Номер") val number: String? = null,

    @SerialName("Автор") val author: String? = null,
    @SerialName("Менеджер") val manager: String? = null,
    @SerialName("Организация") val organization: String? = null,
    @SerialName("ПодразделениеКомпании") val branch: String? = null,

    @SerialName("Комментарий") val comment: String? = null,
    @SerialName("ХозОперация") val operation: String? = null,
    @SerialName("ВидРемонта") val repairType: String? = null,
    @SerialName("Состояние") val status: String? = null,

    @SerialName("Заказчик") val customer: String? = null,
    @SerialName("Автомобиль") val car: String? = null,
    @SerialName("Пробег") val mileage: String? = null,

    @SerialName("Мастер") val master: String? = null,
    @SerialName("Диспетчер") val dispatcher: String? = null,

    @SerialName("ДатаМашинозаезда") val checkInDate: String? = null,
    @SerialName("ДатаНачала") val startDate: String? = null,
    @SerialName("ДатаОкончания") val endDate: String? = null,
    @SerialName("ДатаЗакрытия") val closeDate: String? = null,

    @SerialName("ПричинаОбращения") val reason: String? = null,
    @SerialName("ИсточникИнформации") val infoSource: String? = null,

    @SerialName("Ошибка") val errorCodes: String? = null,
    @SerialName("ТипКПП") val gearboxType: String? = null,
    @SerialName("ТипДвигателя") val engineType: String? = null,

    @SerialName("Работы2") val jobs: List<WorkOrderJobDto> = emptyList(),
    @SerialName("ДефектТаб") val defects: List<WorkOrderDefectDto> = emptyList(),
    @SerialName("messages") val messages: List<WorkOrderMessageDto> = emptyList(),

    @SerialName("guid") val guid: String? = null,
)

@Serializable
data class WorkOrderJobDto(
    @SerialName("НомерСтроки") val lineNumber: String? = null,
    @SerialName("Работа") val work: String? = null,
    @SerialName("Количество") val quantity: String? = null,
    @SerialName("Цена") val price: String? = null,
    @SerialName("Сумма") val amount: String? = null,
    @SerialName("СтавкаНДС") val vatRate: String? = null,
)

@Serializable
data class WorkOrderDefectDto(
    @SerialName("НомерСтроки") val lineNumber: String? = null,
    @SerialName("Наименование") val name: String? = null,
    @SerialName("Состояние") val state: String? = null,
    @SerialName("Решение") val decision: String? = null,
    @SerialName("Пояснение") val description: String? = null,
    @SerialName("Фото") val photo: String? = null,
    @SerialName("Инстр") val action: String? = null,
)

@Serializable
data class WorkOrderMessageDto(
    @SerialName("ДатаРабот") val workDate: String? = null,
    @SerialName("Автомобиль") val car: String? = null,
    @SerialName("Исполнитель") val executor: String? = null,
    @SerialName("Работа1") val workShort: String? = null,
    @SerialName("Цена") val price: String? = null,
    @SerialName("Документ") val document: String? = null,
    @SerialName("Автор") val author: String? = null,
    @SerialName("Работа") val work: String? = null,
    @SerialName("Комментарий") val comment: String? = null,
)
