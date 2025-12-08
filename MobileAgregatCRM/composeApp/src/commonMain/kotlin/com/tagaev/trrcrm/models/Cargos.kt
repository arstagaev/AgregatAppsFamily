package com.tagaev.trrcrm.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// Root: [ { ...CargoDto... }, { ... }, ... ]
typealias CargoListDto = List<CargoDto>

@Serializable
data class CargoDto(
    @SerialName("Проведен")
    val posted: String,

    @SerialName("Ссылка")
    val link: String,

    @SerialName("ПометкаУдаления")
    val deletionMark: String,

    @SerialName("Дата")
    val date: String,

    @SerialName("Номер")
    val number: String,

    @SerialName("Организация")
    val organization: String,

    @SerialName("ПодразделениеКомпании")
    val department: String,

    @SerialName("Автор")
    val author: String,

    @SerialName("ХозОперация")
    val operation: String,

    @SerialName("Комментарий")
    val comment: String,

    @SerialName("Маршрут")
    val route: String,

    @SerialName("Состояние")
    val status: String,

    @SerialName("ДатаОтправления")
    val departureDate: String,

    @SerialName("ДатаПрибытия")
    val arrivalDate: String,

    @SerialName("Грузоперевозчик")
    val carrier: String,

    @SerialName("ПеревозкаСлужбойДоставки")
    val deliveredByService: String,

    @SerialName("Длина")
    val length: String,

    @SerialName("Ширина")
    val width: String,

    @SerialName("Высота")
    val height: String,

    @SerialName("ПутьКФайлам")
    val filesPath: String,

    @SerialName("QR")
    val qr: String,

    @SerialName("Объем")
    val volume: String,

    @SerialName("Вес")
    val weight: String,

    @SerialName("КоличествоМест")
    val placesCount: String,

    @SerialName("ГрузоперевозчикНомерНакладной")
    val carrierInvoiceNumber: String,

    @SerialName("ГрузоперевозчикКонтакты")
    val carrierContacts: String,

    @SerialName("СуммаДокумента")
    val amount: String,

    @SerialName("ДатаОтправкиСообщения")
    val messageSentAt: String,

    @SerialName("СостояниеОтправкиСообщения")
    val messageStatus: String,

    @SerialName("Комплектовщик")
    val picker: String,

    @SerialName("ПромежуточныеТочки")
    val intermediatePoints: String,

    @SerialName("ДокументОснование")
    val baseDocument: String,

    @SerialName("Заказы")
    val orders: List<CargoOrderDto> = emptyList(),

    @SerialName("Товары")
    val products: List<CargoProductDto> = emptyList(),


    // "Грузы" есть в JSON, но там [] — можно игнорировать через ignoreUnknownKeys,
    // поэтому не добавляю сюда отдельное поле.

    @SerialName("guid")
    val guid: String
)

@Serializable
data class CargoOrderDto(
    @SerialName("НомерСтроки")
    val lineNumber: String,

    @SerialName("Заказ")
    val order: String
)

@Serializable
data class CargoProductDto(
    @SerialName("НомерСтроки")
    val lineNumber: String,

    @SerialName("Номенклатура")
    val productName: String,

    @SerialName("Количество")
    val quantity: String,

    @SerialName("ЕдиницаИзмерения")
    val unit: String,

    @SerialName("Заказ")
    val order: String,

    @SerialName("Комментарий")
    val comment: String
)
