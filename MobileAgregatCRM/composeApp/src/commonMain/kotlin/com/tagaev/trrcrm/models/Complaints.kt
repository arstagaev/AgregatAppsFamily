package com.tagaev.trrcrm.models

import androidx.compose.ui.graphics.Color
import com.tagaev.trrcrm.ui.style.DefaultColors
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

// Top level: array of complaints
// Json.decodeFromString<List<ComplaintDto>>(jsonString)
//
// Выполнен Закрыт Запланировано  Выполняется  Начать работу
@Serializable
data class ComplaintDto(
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

    @SerialName("CRM_CLONДатаВремяЗвонка")
    val crmCallDateTime: String? = null,

    @SerialName("CRM_CLONНомерА")
    val crmPhoneA: String? = null,

    @SerialName("CRM_CLONНомерБ")
    val crmPhoneB: String? = null,

    @SerialName("CRM_GUIDЗвонка")
    val crmCallGuid: String? = null,

    @SerialName("GUIDСесииSMSСообщения")
    val smsSessionGuid: String? = null,

    @SerialName("SMSНомерОтправителя")
    val smsSenderNumber: String? = null,

    @SerialName("SMSОборудование")
    val smsDevice: String? = null,

    @SerialName("Автомобиль")
    val car: String? = null,

    @SerialName("Автор")
    val author: String? = null,

    @SerialName("АвторВыдачиАвтомобиля")
    val carIssueAuthor: String? = null,

    @SerialName("АвторПроверкиАвтомобиля")
    val carCheckAuthor: String? = null,

    @SerialName("АвторПроверкиДокументов")
    val documentsCheckAuthor: String? = null,

    @SerialName("АвторПроверкиОплаты")
    val paymentCheckAuthor: String? = null,

    @SerialName("ВидСобытия")
    val eventType: String? = null,

    @SerialName("ДатаНачала")
    val startDate: String? = null,

    @SerialName("ДатаОкончания")
    val endDate: String? = null,

    @SerialName("ДатаПроверкиАвтомобиля")
    val carCheckDate: String? = null,

    @SerialName("ДатаПроверкиДокументов")
    val documentsCheckDate: String? = null,

    @SerialName("ДатаПроверкиОплаты")
    val paymentCheckDate: String? = null,

    @SerialName("ДокументОснование")
    val baseDocument: String? = null,

    @SerialName("ЗаказНаАвтомобиль")
    val carOrder: String? = null,

    @SerialName("ИсточникИнформации")
    val infoSource: String? = null,

    @SerialName("Комментарий")
    val comment: String? = null,

    @SerialName("КонтактнаяИнформация")
    val contactInfo: String? = null,

    @SerialName("КонтактноеЛицо")
    val contactPerson: String? = null,

    @SerialName("Контрагент")
    val counterparty: String? = null,

    @SerialName("МестоВыдачиАвтомобиля")
    val carIssuePlace: String? = null,

    @SerialName("НомерОтправителя")
    val senderNumber: String? = null,

    @SerialName("Организация")
    val organization: String? = null,

    @SerialName("Отзыв")
    val review: String? = null,

    @SerialName("ПланируемаяДатаВыдачиАвтомобиля")
    val plannedCarIssueDate: String? = null,

    @SerialName("ПланируемоеВремяВыдачиАвтомобиля")
    val plannedCarIssueTime: String? = null,

    @SerialName("ПодразделениеКомпании")
    val branch: String? = null,

    @SerialName("Примечание")
    val note: String? = null,

    @SerialName("Приоритет")
    val priority: String? = null,

    @SerialName("Продолжительность")
    val duration: String? = null,

    @SerialName("Проект")
    val project: String? = null,

    @SerialName("Продавец")
    val seller: String? = null,

    @SerialName("Прочитано")
    val read: String? = null,

    @SerialName("Результат")
    val result: String? = null,

    @SerialName("Решение")
    val resolution: String? = null,

    @SerialName("Содержание")
    val content: String? = null,

    @SerialName("Состояние")
    val state: String? = null,

    @SerialName("СтатусSMS")
    val smsStatus: String? = null,

    @SerialName("Тема")
    val topic: String? = null,

    @SerialName("ТипСообщения")
    val messageType: String? = null,

    @SerialName("ФактическаяДатаВыдачиАвтомобиля")
    val actualCarIssueDate: String? = null,

    @SerialName("ФорматТекста")
    val textFormat: String? = null,

    @SerialName("ХозОперация")
    val operationType: String? = null,

    @SerialName("ПутьКФайлам")
    val filesPath: String? = null,

    @SerialName("ДатаМод")
    val modifiedDate: String? = null,

    @SerialName("ПолучательВозврата")
    val refundRecipient: String? = null,

    @SerialName("МастерИзЗН")
    val masterFromOrder: String? = null,

    @SerialName("Инструкция")
    val instruction: String? = null,

    @SerialName("ТемаСпр")
    val topicRef: String? = null,

    @SerialName("ОплатаСогласована")
    val paymentApproved: String? = null,

    @SerialName("ОплатаСогласованаСотрудник")
    val paymentApprovedEmployee: String? = null,

    @SerialName("ОплатаСогласованаДата")
    val paymentApprovedDate: String? = null,

    @SerialName("Ошибка")
    val error: String? = null,

    @SerialName("Ответственный")
    val responsible: String? = null,

    @SerialName("ТипКПП")
    val transmissionType: String? = null,

    @SerialName("ТипДвигателя")
    val engineType: String? = null,

    // Nested arrays

    @SerialName("Пользователи")
    val users: List<ComplaintUserDto> = emptyList(),

    // Unknown structure yet → keep as raw JSON so parsing won’t break when backend fills them
    @SerialName("СторонниеЛица")
    val externalPersons: List<JsonObject> = emptyList(),

    @SerialName("Товары")
    val goods: List<ComplaintGoodsDto> = emptyList(),

    @SerialName("Автомобили")
    val cars: List<JsonObject> = emptyList(),

    @SerialName("Работы")
    val works: List<ComplaintWorkDto> = emptyList(),

    @SerialName("ДефектТаб")
    val defects: List<ComplaintDefectDto> = emptyList(),

    @SerialName("ЧекЛист")
    val checklist: List<JsonObject> = emptyList(),

    @SerialName("tasks")
    val tasks: List<ComplaintTaskDto> = emptyList(),

    @SerialName("messages")
    val messages: List<ComplaintMessageDto> = emptyList(),
)

@Serializable
data class ComplaintUserDto(
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
data class ComplaintGoodsDto(
    @SerialName("НомерСтроки")
    val lineNumber: String? = null,

    @SerialName("Номенклатура")
    val itemName: String? = null,

    @SerialName("Количество")
    val qty: String? = null,

    @SerialName("ЕдиницаИзмерения")
    val unit: String? = null,

    @SerialName("Коэффициент")
    val coefficient: String? = null,

    @SerialName("ХарактеристикаНоменклатуры")
    val itemCharacteristic: String? = null,

    @SerialName("Цена")
    val price: String? = null,

    @SerialName("Сумма")
    val amount: String? = null,

    @SerialName("СтавкаНДС")
    val vatRate: String? = null,

    @SerialName("СуммаНДС")
    val vatAmount: String? = null,

    @SerialName("СуммаВсего")
    val totalAmount: String? = null,

    @SerialName("ПроцентСкидки")
    val discountPercent: String? = null,

    @SerialName("СуммаСкидки")
    val discountAmount: String? = null,
)

@Serializable
data class ComplaintWorkDto(
    @SerialName("НомерСтроки")
    val lineNumber: String? = null,

    @SerialName("Работа")
    val workName: String? = null,

    @SerialName("ИдентификаторРаботы")
    val workId: String? = null,

    @SerialName("Количество")
    val qty: String? = null,

    @SerialName("Нормочас")
    val normHour: String? = null,

    @SerialName("Коэффициент")
    val coefficient: String? = null,

    @SerialName("Цена")
    val price: String? = null,

    @SerialName("Сумма")
    val amount: String? = null,

    @SerialName("СтавкаНДС")
    val vatRate: String? = null,

    @SerialName("СуммаНДС")
    val vatAmount: String? = null,

    @SerialName("ПроцентСкидки")
    val discountPercent: String? = null,

    @SerialName("СуммаСкидки")
    val discountAmount: String? = null,

    @SerialName("СуммаВсего")
    val totalAmount: String? = null,

    @SerialName("СкидкаНаТовар")
    val itemDiscount: String? = null,

    @SerialName("ПроцентСкидкиСтроки")
    val lineDiscountPercent: String? = null,

    @SerialName("СуммаСкидкиСтроки")
    val lineDiscountAmount: String? = null,

    @SerialName("ПримечаниеРаботыПечать")
    val notePrint: String? = null,

    @SerialName("ПримечаниеРаботы")
    val note: String? = null,

    @SerialName("Контрагент")
    val counterparty: String? = null,

    @SerialName("ДоговорВзаиморасчетов")
    val contract: String? = null,

    @SerialName("ПакетРабот")
    val workPackageId: String? = null,

    @SerialName("НомерПакета")
    val packageNumber: String? = null,

    @SerialName("ПакетЗакрыт")
    val packageClosed: String? = null,

    @SerialName("УправляющийКод")
    val controlCode: String? = null,
)

@Serializable
data class ComplaintDefectDto(
    @SerialName("НомерСтроки")
    val lineNumber: String? = null,

    @SerialName("Автор")
    val author: String? = null,

    @SerialName("Дата")
    val date: String? = null,

    @SerialName("Наименование")
    val name: String? = null,

    @SerialName("Состояние")
    val state: String? = null,

    @SerialName("Решение")
    val resolution: String? = null,

    @SerialName("Пояснение")
    val explanation: String? = null,

    @SerialName("Фото")
    val photo: String? = null,

    @SerialName("Инстр")
    val instruction: String? = null,
)

@Serializable
data class ComplaintTaskDto(
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
data class ComplaintMessageDto(
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

//// Выполнен Закрыт Запланировано  Выполняется  Начать работу
enum class ComplaintColor (val name1C: String, val colorF: Color, val colorBack: Color) {
    DONE("Выполнен"           , colorF = DefaultColors.RainbowGreenFg, colorBack = DefaultColors.RainbowGreenBg),
    CLOSED("Закрыт"           , colorF = DefaultColors.StatusMutedFg, colorBack = DefaultColors.StatusMutedBg),
    PLAN("Запланировано"      , colorF = DefaultColors.RainbowIndigoFg, colorBack = DefaultColors.RainbowIndigoBg),
    PROGRESS("Выполняется"    , colorF = DefaultColors.RainbowBlueFg, colorBack = DefaultColors.RainbowBlueBg),
    LETS_BEGIN("Начать работу", colorF = DefaultColors.RainbowVioletFg, colorBack = DefaultColors.RainbowVioletBg),
}
