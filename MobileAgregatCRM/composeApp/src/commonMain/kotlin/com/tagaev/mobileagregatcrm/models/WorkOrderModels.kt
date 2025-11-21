package com.tagaev.mobileagregatcrm.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

typealias WorkOrdersResponse = List<WorkOrderDto>

@Serializable
data class WorkOrderDto(
    @SerialName("Проведен") val posted: String? = null,
    @SerialName("ПометкаУдаления") val deletionMark: String? = null,
    @SerialName("Ссылка") val link: String? = null,
    @SerialName("Дата") val date: String? = null,
    @SerialName("Номер") val number: String? = null,

    @SerialName("Автор") val author: String? = null,
    @SerialName("Менеджер") val manager: String? = null,
    @SerialName("Организация") val organization: String? = null,
    @SerialName("ПодразделениеКомпании") val branch: String? = null,

    @SerialName("Гарантии") val warrantyText: String? = null,
    @SerialName("ГарантияАКПП") val gearboxWarranty: String? = null,
    @SerialName("Дефектовка") val defectSummary: String? = null,

    @SerialName("Комментарий") val comment: String? = null,
    @SerialName("ХозОперация") val operation: String? = null,
    @SerialName("ВидРемонта") val repairType: String? = null,
    @SerialName("Состояние") val status: String? = null,

    @SerialName("Заказчик") val customer: String? = null,
    @SerialName("Автомобиль") val car: String? = null,
    @SerialName("Пробег") val mileage: String? = null,
    @SerialName("АвтомобильГод") val carAge: String? = null,

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

    @SerialName("Товары") val products: List<WorkOrderProductDto> = emptyList(),
    @SerialName("Работы") val jobs: List<WorkOrderJobDto> = emptyList(),
    @SerialName("МатериалыЗаказчика") val customerMaterials: List<WorkOrderCustomerMaterialDto> = emptyList(),
    @SerialName("Исполнители") val executors: List<WorkOrderExecutorDto> = emptyList(),
    @SerialName("Материалы") val materials: List<Map<String, String>> = emptyList(),
    @SerialName("Товары2") val products2: List<WorkOrderProductDto> = emptyList(),

    @SerialName("Работы2") val jobs2: List<WorkOrderJobDto> = emptyList(),
    @SerialName("ДефектТаб") val defects: List<WorkOrderDefectDto> = emptyList(),
    @SerialName("messages") val messages: List<WorkOrderMessageDto> = emptyList(),

    @SerialName("recomendations") val recommendations: List<Map<String, String>> = emptyList(),
    @SerialName("guid") val guid: String? = null
)

@Serializable
data class WorkOrderJobDto(
    @SerialName("НомерСтроки") val lineNumber: String? = null,
    @SerialName("Работа") val work: String? = null,
    @SerialName("ИдентификаторРаботы") val workId: String? = null,
    @SerialName("Количество") val quantity: String? = null,
    @SerialName("Нормочас") val normHour: String? = null,
    @SerialName("Коэффициент") val coefficient: String? = null,
    @SerialName("Цена") val price: String? = null,
    @SerialName("Сумма") val amount: String? = null,
    @SerialName("СтавкаНДС") val vatRate: String? = null,
    @SerialName("СуммаНДС") val vatAmount: String? = null,
    @SerialName("ПроцентСкидки") val discountPercent: String? = null,
    @SerialName("СуммаСкидки") val discountAmount: String? = null,
    @SerialName("СуммаВсего") val totalAmount: String? = null,
    @SerialName("СкидкаНаТовар") val discountOnItem: String? = null,
    @SerialName("ПроцентСкидкиСтроки") val lineDiscountPercent: String? = null,
    @SerialName("СуммаСкидкиСтроки") val lineDiscountAmount: String? = null,
    @SerialName("ПримечаниеРаботыПечать") val notePrint: String? = null,
    @SerialName("ПримечаниеРаботы") val note: String? = null,
    @SerialName("Контрагент") val counterparty: String? = null,
    @SerialName("ДоговорВзаиморасчетов") val settlementContract: String? = null,
    @SerialName("ПакетРабот") val workPackageId: String? = null,
    @SerialName("НомерПакета") val workPackageNumber: String? = null,
    @SerialName("ПакетЗакрыт") val workPackageClosed: String? = null,
    @SerialName("УправляющийКод") val controlCode: String? = null,
)

@Serializable
data class WorkOrderProductDto(
    @SerialName("НомерСтроки") val lineNumber: String? = null,
    @SerialName("Номенклатура") val name: String? = null,
    @SerialName("Количество") val quantity: String? = null,
    @SerialName("ЕдиницаИзмерения") val unit: String? = null,
    @SerialName("Коэффициент") val coefficient: String? = null,
    @SerialName("Цена") val price: String? = null,
    @SerialName("Сумма") val amount: String? = null,
    @SerialName("СтавкаНДС") val vatRate: String? = null,
    @SerialName("СуммаНДС") val vatAmount: String? = null,
    @SerialName("ПроцентСкидки") val discountPercent: String? = null,
    @SerialName("СуммаСкидки") val discountAmount: String? = null,
    @SerialName("ХарактеристикаНоменклатуры") val characteristic: String? = null,
    @SerialName("СуммаВсего") val totalAmount: String? = null,
    @SerialName("СкладКомпании") val warehouse: String? = null,
    @SerialName("СкидкаНаТовар") val discountOnItem: String? = null,
    @SerialName("ПроцентСкидкиСтроки") val lineDiscountPercent: String? = null,
    @SerialName("СуммаСкидкиСтроки") val lineDiscountAmount: String? = null,
    @SerialName("ПримечаниеНоменклатураПечать") val notePrint: String? = null,
    @SerialName("ПримечаниеНоменклатура") val note: String? = null,
    @SerialName("УправляющийКод") val controlCode: String? = null,
    // присутствует в "Товары2"
    @SerialName("Артикул") val article: String? = null,
    // присутствует только в "Товары"
    @SerialName("Ячейка") val cell: String? = null,
)

@Serializable
data class WorkOrderCustomerMaterialDto(
    @SerialName("НомерСтроки") val lineNumber: String? = null,
    @SerialName("Номенклатура") val name: String? = null,
    @SerialName("Количество") val quantity: String? = null,
    @SerialName("ЕдиницаИзмерения") val unit: String? = null,
    @SerialName("Коэффициент") val coefficient: String? = null,
    @SerialName("ХарактеристикаНоменклатуры") val characteristic: String? = null,
    @SerialName("ПримечаниеНоменклатураПечать") val notePrint: String? = null,
    @SerialName("ПримечаниеНоменклатура") val note: String? = null,
    @SerialName("НоменклатураСтрока") val nameFull: String? = null,
    @SerialName("Код") val code: String? = null,
    @SerialName("ЕстьСертификат") val hasCertificate: String? = null,
    @SerialName("ЕстьЧек") val hasReceipt: String? = null,
)

@Serializable
data class WorkOrderExecutorDto(
    @SerialName("НомерСтроки") val lineNumber: String? = null,
    @SerialName("ИдентификаторРаботы") val workId: String? = null,
    @SerialName("Исполнитель") val executor: String? = null,
    @SerialName("Цех") val workshop: String? = null,
    @SerialName("Процент") val percent: String? = null,
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
