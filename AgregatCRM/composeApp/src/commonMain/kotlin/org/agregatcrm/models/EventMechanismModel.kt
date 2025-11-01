package org.agregatcrm.models

import kotlinx.datetime.LocalDateTime
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.agregatcrm.utils.LocalDateTimeSerializer

@Serializable
data class EventItemDto(
    // common flags/ids
    @SerialName("Проведен") val posted: String? = null,            // "Да"/"Нет" or "Yes"/"No"
    @SerialName("ПометкаУдаления") val deletedMark: String? = null, // "Да"/"Нет"
    @SerialName("Номер") val number: String? = null,
    @SerialName("guid") val guid: String? = null,

    // primary meta
    @SerialName("Ссылка") val link: String? = null,

    @Serializable(with = LocalDateTimeSerializer::class)
    @SerialName("Дата") val date: LocalDateTime? = null,                  // "dd.MM.yyyy H:mm:ss"

    @SerialName("ХозОперация") val operation: String? = null,      // "Событие"
    @SerialName("ВидСобытия") val eventType: String? = null,       // "Возврат", "Прочее", ...
    @SerialName("Состояние") val state: String? = null,            // "Запланировано", "Выполнено"
    @SerialName("Тема") val subject: String? = null,
    @SerialName("Содержание") val content: String? = null,
    @SerialName("Прочитано") val readMark: String? = null,         // "Да"/"Нет"

    // dates (as raw strings)
    @SerialName("ДатаНачала") val startDate: String? = null,
    @SerialName("ДатаОкончания") val endDate: String? = null,
    @SerialName("ДатаМод") val modifiedDate: String? = null,
    @SerialName("ФактическаяДатаВыдачиАвтомобиля") val actualCarIssueDate: String? = null,

    // relations / refs
    @SerialName("ДокументОснование") val baseDocument: String? = null,
    @SerialName("ПутьКФайлам") val filesPath: String? = null,

    // people & org
    @SerialName("Автор") val author: String? = null,
    @SerialName("Контрагент") val counterparty: String? = null,
    @SerialName("Организация") val organization: String? = null,
    @SerialName("ПодразделениеКомпании") val companyDepartment: String? = null,

    // misc
    @SerialName("Приоритет") val priority: String? = null,         // "Средняя"
    @SerialName("Результат") val result: String? = null,
    @SerialName("Решение") val decision: String? = null,
    @SerialName("Комментарий") val comment: String? = null,
    @SerialName("Примечание") val note: String? = null,
    @SerialName("Автомобиль") val car: String? = null,
    @SerialName("ТипСообщения") val messageType: String? = null,   // "Исходящее"
    @SerialName("ФорматТекста") val textFormat: String? = null,    // "Простой текст"
    @SerialName("ИсточникИнформации") val infoSource: String? = null,

    // blocks
    @SerialName("Пользователи") val users: List<UserRowDto> = emptyList(),
    @SerialName("СторонниеЛица") val externalUsers: List<ExternalRowDto> = emptyList(),
    @SerialName("Товары") val products: List<ItemRowDto> = emptyList(),
    @SerialName("Автомобили") val cars: List<String> = emptyList(), // appears empty in samples
    @SerialName("tasks") val tasks: List<TaskDto> = emptyList(),
    @SerialName("messages") val messages: List<MessageDto> = emptyList()
)

@Serializable
data class UserRowDto(
    @SerialName("НомерСтроки") val rowNo: String? = null,
    @SerialName("Пользователь") val user: String? = null,
    @SerialName("Ответственный") val responsible: String? = null, // "Да"/"Нет"
    @SerialName("Роль") val role: String? = null
)

@Serializable
data class ExternalRowDto(
    @SerialName("НомерСтроки") val rowNo: String? = null,
    @SerialName("Контрагент") val counterparty: String? = null,
    @SerialName("КонтактноеЛицо") val contactPerson: String? = null,
    @SerialName("КонтактнаяИнформация") val contactInfo: String? = null,
    @SerialName("ТипКонтактнойИнформации") val contactInfoType: String? = null,
    @SerialName("ВладелецКИ") val contactOwner: String? = null,
    @SerialName("Статус") val status: String? = null,
    @SerialName("ДатаПолучения") val receivedDate: String? = null,
    @SerialName("ДатаСобытия") val eventDate: String? = null,
    @SerialName("GUID") val guid: String? = null,
    @SerialName("ОписаниеОшибки") val errorDescription: String? = null
)

@Serializable
data class ItemRowDto(
    @SerialName("НомерСтроки") val rowNo: String? = null,
    @SerialName("Номенклатура") val itemName: String? = null,
    @SerialName("Количество") val quantity: String? = null,
    @SerialName("ЕдиницаИзмерения") val unit: String? = null,
    @SerialName("Коэффициент") val coef: String? = null,
    @SerialName("ХарактеристикаНоменклатуры") val itemFeature: String? = null
)

@Serializable
data class TaskDto(
    @SerialName("ДатаРабот") val workDate: String? = null,
    @SerialName("Исполнитель") val executor: String? = null,
    @SerialName("Работа1") val work1: String? = null,
    @SerialName("Цена") val price: String? = null,
    @SerialName("Документ") val document: String? = null,
    @SerialName("Автор") val author: String? = null,
    @SerialName("Работа") val work: String? = null,
    @SerialName("Комментарий") val comment: String? = null
)

@Serializable
data class MessageDto(
    @SerialName("ДатаРабот") val workDate: String? = null,
    @SerialName("Исполнитель") val executor: String? = null,
    @SerialName("Работа1") val work1: String? = null,
    @SerialName("Цена") val price: String? = null,
    @SerialName("Документ") val document: String? = null,
    @SerialName("Автор") val author: String? = null,
    @SerialName("Работа") val work: String? = null,
    @SerialName("Комментарий") val comment: String? = null
)
