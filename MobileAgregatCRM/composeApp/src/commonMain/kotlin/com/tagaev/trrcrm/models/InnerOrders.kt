package com.tagaev.trrcrm.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

@Serializable
data class InnerOrderDto(
    @SerialName("guid")
    val guid: String,

    @SerialName("Проведен")
    val posted: String? = null,

    @SerialName("Ссылка")
    val link: String? = null,

    @SerialName("ПометкаУдаления")
    val deletionMark: String? = null,

    @SerialName("Дата")
    val date: String? = null,

    @SerialName("Номер")
    val number: String? = null,

    @SerialName("Автор")
    val author: String? = null,

    @SerialName("Организация")
    val organization: String? = null,

    @SerialName("ПодразделениеКомпании")
    val branch: String? = null,

    @SerialName("Комментарий")
    val comment: String? = null,

    @SerialName("СкладКомпании")
    val companyWarehouse: String? = null,

    @SerialName("ВалютаДокумента")
    val currency: String? = null,

    @SerialName("КурсДокумента")
    val rate: String? = null,

    @SerialName("ДокументОснование")
    val baseDocument: String? = null,

    @SerialName("ТипЦен")
    val priceType: String? = null,

    @SerialName("СуммаДокумента")
    val documentAmount: String? = null,

    @SerialName("ХозОперация")
    val operationType: String? = null,

    @SerialName("ПодразделениеПолучатель")
    val receiverBranch: String? = null,

    @SerialName("КурсВалютыУпр")
    val managementCurrencyRate: String? = null,

    @SerialName("ДатаСоздания")
    val creationDate: String? = null,

    @SerialName("ДатаОперации")
    val operationDate: String? = null,

    @SerialName("НомерЗН")
    val workOrderNumber: String? = null,

    @SerialName("ТекстЗаказа")
    val orderText: String? = null,

    @SerialName("ТекстМашины")
    val carText: String? = null,

    @SerialName("Состояние")
    val state: String? = null,

    @SerialName("ПутьКФайлам")
    val filesPath: String? = null,

    @SerialName("Обработан")
    val processed: String? = null,

    @SerialName("Менеджер")
    val manager: String? = null,

    // Nested arrays

    @SerialName("Товары")
    val goods: List<InnerOrderGoodsDto> = emptyList(),

    @SerialName("РаспределениеЗаказа")
    val orderAllocation: List<JsonObject> = emptyList(),

    @SerialName("КакиеЗапчасти")
    val whichParts: List<JsonObject> = emptyList(),

    @SerialName("Пользователи")
    val users: List<InnerOrderUserDto> = emptyList(),

    @SerialName("МояРоль")
    val myRole: String? = null,

    @SerialName("tasks")
    val tasks: List<InnerOrderTaskDto> = emptyList(),

    @SerialName("messages")
    val messages: List<InnerOrderMessageDto> = emptyList(),
)

@Serializable
data class InnerOrderGoodsDto(
    @SerialName("НомерСтроки")
    val lineNumber: String? = null,

    @SerialName("Номенклатура")
    val itemName: String? = null,

    @SerialName("Количество")
    val quantity: String? = null,

    @SerialName("Резерв")
    val reserve: String? = null,

    @SerialName("Распределено")
    val distributed: String? = null,

    @SerialName("ЕдиницаИзмерения")
    val unit: String? = null,

    @SerialName("Коэффициент")
    val coefficient: String? = null,

    @SerialName("Цена")
    val price: String? = null,

    @SerialName("Сумма")
    val amount: String? = null,

    @SerialName("СуммаВсего")
    val totalAmount: String? = null,

    @SerialName("ХарактеристикаНоменклатуры")
    val itemCharacteristic: String? = null,

    @SerialName("ИдентификаторСтроки")
    val rowId: String? = null,

    @SerialName("ЯчейкаХранения")
    val storageCell: String? = null,

    @SerialName("Комментарий")
    val comment: String? = null,

    @SerialName("ТипВозврата")
    val returnType: String? = null,
)

@Serializable
data class InnerOrderUserDto(
    @SerialName("НомерСтроки")
    val lineNumber: String? = null,

    @SerialName("Пользователь")
    val user: String? = null,

    @SerialName("Ответственный")
    val responsible: String? = null,

    @SerialName("Роль")
    val role: String? = null,
)

@Serializable
data class InnerOrderTaskDto(
    @SerialName("ДатаРабот")
    val workDate: String? = null,

    @SerialName("Исполнитель")
    val performer: String? = null,

    @SerialName("Работа1")
    val work1: String? = null,

    @SerialName("Цена")
    val price: String? = null,

    @SerialName("Документ")
    val document: String? = null,

    @SerialName("Автор")
    val author: String? = null,

    @SerialName("Работа")
    val work: String? = null,

    @SerialName("Комментарий")
    val comment: String? = null,
)

@Serializable
data class InnerOrderMessageDto(
    @SerialName("ДатаРабот")
    val workDate: String? = null,

    @SerialName("Исполнитель")
    val performer: String? = null,

    @SerialName("Работа1")
    val work1: String? = null,

    @SerialName("Цена")
    val price: String? = null,

    @SerialName("Документ")
    val document: String? = null,

    @SerialName("Автор")
    val author: String? = null,

    @SerialName("Работа")
    val work: String? = null,

    @SerialName("Комментарий")
    val comment: String? = null,
)
