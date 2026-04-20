package com.tagaev.trrcrm.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

typealias IncomingApplicationsResponse = List<IncomingApplicationDto>

/** Документ 1C «ВходящиеЗаявки» (список через getitemslist). */
@Serializable
data class IncomingApplicationDto(
    @SerialName("guid") val guid: String,
    @SerialName("Проведен") val posted: String? = null,
    @SerialName("Ссылка") val link: String? = null,
    @SerialName("ПометкаУдаления") val deletionMark: String? = null,
    @SerialName("Дата") val date: String? = null,
    @SerialName("Номер") val number: String? = null,
    @SerialName("ПодразделениеКомпании") val branch: String? = null,
    @SerialName("Автор") val author: String? = null,
    @SerialName("Тип") val type: String? = null,
    @SerialName("СутьОбращения") val subjectMatter: String? = null,
    @SerialName("Модель") val model: String? = null,
    @SerialName("ДанныеКлиента") val clientData: String? = null,
    @SerialName("ЗаездСостоялся") val visitHappened: String? = null,
    @SerialName("ХозОперация") val operation: String? = null,
    @SerialName("ДокументОснование") val baseDocument: String? = null,
    @SerialName("ПричинаОбращения") val reason: String? = null,
    @SerialName("Контрагент") val counterparty: String? = null,
    @SerialName("ДатаЗаезда") val visitDate: String? = null,
    @SerialName("Телефон") val phone: String? = null,
    @SerialName("Комментарий") val comment: String? = null,
    @SerialName("Автомобиль") val car: String? = null,
    @SerialName("VIN") val vin: String? = null,
    @SerialName("Организация") val organization: String? = null,
    @SerialName("ГодВыпуска") val manufactureYear: String? = null,
    @SerialName("ТипКПП") val transmissionType: String? = null,
    @SerialName("ОбъемДвигателя") val engineVolume: String? = null,
    @SerialName("Проценка") val pricingQuote: String? = null,
    @SerialName("Ответственный") val responsible: String? = null,
)

private val emptyOneCDateTime = Regex("""^\s*01\.01\.0001\s+0:00:00\s*$""")

private fun String?.isMeaningfulNumericVolume(): Boolean {
    val t = this?.trim().orEmpty()
    if (t.isEmpty()) return false
    if (t == "0") return false
    return true
}

/**
 * Пары подпись (RU) → значение для экрана деталей: только непустые значения;
 * пустые строки, «01.01.0001 0:00:00» и «0» для объёма двигателя не показываем.
 */
fun IncomingApplicationDto.nonEmptyDisplayRows(): List<Pair<String, String>> = buildList {
    fun add(labelRu: String, raw: String?, skipNumericZero: Boolean = false) {
        val v = raw?.trim().orEmpty()
        if (v.isEmpty()) return
        if (skipNumericZero && !raw.isMeaningfulNumericVolume()) return
        if (emptyOneCDateTime.matches(v)) return
        add(labelRu to v)
    }
    add("Проведен", posted)
    add("Ссылка", link)
    add("Пометка удаления", deletionMark)
    add("Дата", date)
    add("Номер", number)
    add("Подразделение", branch)
    add("Автор", author)
    add("Тип", type)
    add("Суть обращения", subjectMatter)
    add("Модель", model)
    add("Данные клиента", clientData)
    add("Заезд состоялся", visitHappened)
    add("Хоз. операция", operation)
    add("Документ-основание", baseDocument)
    add("Причина обращения", reason)
    add("Контрагент", counterparty)
    add("Дата заезда", visitDate)
    add("Телефон", phone)
    add("Комментарий", comment)
    add("Автомобиль", car)
    add("VIN", vin)
    add("Организация", organization)
    add("Год выпуска", manufactureYear)
    add("Тип КПП", transmissionType)
    add("Объём двигателя", engineVolume, skipNumericZero = true)
    add("Проценка", pricingQuote)
    add("Ответственный", responsible)
    if (guid.isNotBlank()) add("GUID" to guid.trim())
}
