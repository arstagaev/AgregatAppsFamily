package com.tagaev.trrcrm.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

typealias SupplierOrdersResponse = List<SupplierOrderDto>

@Serializable
data class SupplierOrderDto(
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
    @SerialName("СуммаДокумента") val documentAmount: String? = null,
    @SerialName("ХозОперация") val operation: String? = null,
    @SerialName("ДокументОснование") val baseDocument: String? = null,
    @SerialName("Состояние") val status: String? = null,
    @SerialName("Менеджер") val manager: String? = null,
    @SerialName("ДоговорВзаиморасчетов") val paymentContract: String? = null,
    @SerialName("ВалютаДокумента") val documentCurrency: String? = null,
    @SerialName("ТипЦен") val priceType: String? = null,
    @SerialName("СрокПоставки") val deliveryDate: String? = null,
    @SerialName("ПризнакОплаты") val paymentSign: String? = null,
    @SerialName("Груз") val cargo: String? = null,
    @SerialName("СкладКомпании") val companyWarehouse: String? = null,
    @SerialName("Товары") val products: List<WorkOrderProductDto> = emptyList(),
    @SerialName("РаспределениеЗаказа") val orderDistribution: List<SupplierOrderDistributionDto> = emptyList(),
    @SerialName("Пользователи") val users: List<UserRowDto> = emptyList(),
    @SerialName("tasks") val tasks: List<TaskDto> = emptyList(),
    @SerialName("Задания") val tasksRu: List<TaskDto> = emptyList(),
    @SerialName("messages") val messages: List<WorkOrderMessageDto> = emptyList(),
    @SerialName("guid") val guid: String? = null,
)

@Serializable
data class SupplierOrderDistributionDto(
    @SerialName("НомерСтроки") val lineNumber: String? = null,
    @SerialName("Номенклатура") val itemName: String? = null,
    @SerialName("Количество") val quantity: String? = null,
    @SerialName("ЕдиницаИзмерения") val unit: String? = null,
    @SerialName("Коэффициент") val coefficient: String? = null,
    @SerialName("Цена") val price: String? = null,
    @SerialName("ХарактеристикаНоменклатуры") val itemCharacteristic: String? = null,
    @SerialName("ЗаказПокупателя") val buyerOrder: String? = null,
    @SerialName("ЗаказОтправленПоставщику") val sentToSupplier: String? = null,
)

