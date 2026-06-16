package com.tagaev.trrcrm.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

typealias ExpenseRequestsResponse = List<ExpenseRequestDto>

/** Документ 1C «ЗаявкаНаРасход» (список через getitemslist). */
@Serializable
data class ExpenseRequestDto(
    @SerialName("guid") val guid: String,
    @SerialName("Проведен") val posted: String? = null,
    @SerialName("Ссылка") val link: String? = null,
    @SerialName("ПометкаУдаления") val deletionMark: String? = null,
    @SerialName("Дата") val date: String? = null,
    @SerialName("Номер") val number: String? = null,
    @SerialName("ХозОперация") val operation: String? = null,
    @SerialName("Автор") val author: String? = null,
    @SerialName("ДатаОплаты") val paymentDate: String? = null,
    @SerialName("Комментарий") val comment: String? = null,
    @SerialName("Приоритет") val priority: String? = null,
    @SerialName("Тема") val topic: String? = null,
    @SerialName("ВалютаДокумента") val currency: String? = null,
    @SerialName("ДоговорВзаиморасчетов") val settlementContract: String? = null,
    @SerialName("ДокументОснование") val baseDocument: String? = null,
    @SerialName("КурсДокумента") val exchangeRate: String? = null,
    @SerialName("Организация") val organization: String? = null,
    @SerialName("ПодразделениеКомпании") val branch: String? = null,
    @SerialName("СтатьяДДС") val cashFlowItem: String? = null,
    @SerialName("СуммаДокумента") val amount: String? = null,
    @SerialName("СтатусЗаявки") val requestStatus: String? = null,
    @SerialName("ТипЗаявки") val requestType: String? = null,
    @SerialName("СтруктурнаяЕдиница") val structuralUnit: String? = null,
    @SerialName("Подписанты") val signatories: List<ExpenseRequestSignatoryDto> = emptyList(),
)

@Serializable
data class ExpenseRequestSignatoryDto(
    @SerialName("НомерСтроки") val lineNumber: String? = null,
    @SerialName("Пользователь") val user: String? = null,
    @SerialName("ДатаИзменения") val changeDate: String? = null,
    @SerialName("Статус") val status: String? = null,
)

private val emptyOneCDateTime = Regex("""^\s*01\.01\.0001\s+0:00:00\s*$""")

fun ExpenseRequestSignatoryDto.isMeaningful(): Boolean {
    return !user.isNullOrBlank() || !status.isNullOrBlank()
}

fun ExpenseRequestDto.meaningfulSignatories(): List<ExpenseRequestSignatoryDto> =
    signatories.filter { it.isMeaningful() }

fun ExpenseRequestDto.formattedAmount(): String? {
    val value = amount?.trim().orEmpty()
    if (value.isEmpty()) return null
    val currencySuffix = when (currency?.trim()?.lowercase()) {
        "руб", "rub", "rur" -> " ₽"
        else -> currency?.trim()?.takeIf { it.isNotEmpty() }?.let { " $it" }.orEmpty()
    }
    return value + currencySuffix
}

/** Значение для ячейки деталей; пустые и «01.01.0001» → «—». */
fun ExpenseRequestDto.detailDisplayValue(raw: String?): String {
    val v = raw?.trim().orEmpty()
    if (v.isEmpty() || emptyOneCDateTime.matches(v)) return "—"
    return v
}

/** Основная сетка деталей (5 строк × 2 колонки). */
fun ExpenseRequestDto.primaryDetailGridRows(): List<Pair<Pair<String, String>, Pair<String, String>>> =
    listOf(
        ("Номер" to detailDisplayValue(number)) to ("Дата" to detailDisplayValue(date)),
        ("Тема" to detailDisplayValue(topic)) to (
            "Сумма документа" to (formattedAmount()?.takeIf { it.isNotBlank() } ?: "—")
            ),
        ("Договор взаиморасчётов" to detailDisplayValue(settlementContract)) to
            ("Документ-основание" to detailDisplayValue(baseDocument)),
        ("Организация" to detailDisplayValue(organization)) to
            ("Подразделение" to detailDisplayValue(branch)),
        ("Статус заявки" to detailDisplayValue(requestStatus)) to
            ("Тип заявки" to detailDisplayValue(requestType)),
    )

/** Дополнительные поля — в той же сетке, только непустые. Нечётная последняя строка — одна колонка. */
fun ExpenseRequestDto.supplementaryDetailGridRows(): List<Pair<Pair<String, String>, Pair<String, String>?>> {
    val singles = buildList {
        fun addPair(label: String, raw: String?) {
            val value = detailDisplayValue(raw)
            if (value != "—") add(label to value)
        }
        addPair("Хоз. операция", operation)
        addPair("Автор", author)
        addPair("Дата оплаты", paymentDate)
        addPair("Приоритет", priority)
        addPair("Статья ДДС", cashFlowItem)
        addPair("Курс документа", exchangeRate)
        addPair("Структурная единица", structuralUnit)
        addPair("Проведен", posted)
        addPair("Комментарий", comment)
    }
    return singles.chunked(2).map { chunk ->
        chunk[0] to chunk.getOrNull(1)
    }
}

fun ExpenseRequestDto.nonEmptyDisplayRows(): List<Pair<String, String>> = buildList {
    fun add(labelRu: String, raw: String?) {
        val v = raw?.trim().orEmpty()
        if (v.isEmpty()) return
        if (emptyOneCDateTime.matches(v)) return
        add(labelRu to v)
    }
    add("Проведен", posted)
    add("Ссылка", link)
    add("Пометка удаления", deletionMark)
    add("Дата", date)
    add("Номер", number)
    add("Хоз. операция", operation)
    add("Автор", author)
    add("Дата оплаты", paymentDate)
    add("Комментарий", comment)
    add("Приоритет", priority)
    add("Тема", topic)
    add("Валюта", currency)
    add("Договор взаиморасчётов", settlementContract)
    add("Документ-основание", baseDocument)
    add("Курс документа", exchangeRate)
    add("Организация", organization)
    add("Подразделение", branch)
    add("Статья ДДС", cashFlowItem)
    add("Сумма документа", amount)
    add("Статус заявки", requestStatus)
    add("Тип заявки", requestType)
    add("Структурная единица", structuralUnit)
    if (guid.isNotBlank()) add("GUID" to guid.trim())
}
