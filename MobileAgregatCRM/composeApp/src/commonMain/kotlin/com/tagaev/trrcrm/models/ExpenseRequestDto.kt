package com.tagaev.trrcrm.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

typealias ExpenseRequestsResponse = List<ExpenseRequestDto>

/** Документ 1C «ЗаявкаНаРасходДС» (список через getitemslist). */
@Serializable
data class ExpenseRequestDto(
    @SerialName("guid") val guid: String,
    @SerialName("Проведен") val posted: String? = null,
    @SerialName("Ссылка") val link: String? = null,
    @SerialName("ПометкаУдаления") val deletionMark: String? = null,
    @SerialName("Дата") val date: String? = null,
    @SerialName("Номер") val number: String? = null,
    @SerialName("Автор") val author: String? = null,
    @SerialName("ВалютаДокумента") val currency: String? = null,
    @SerialName("ДоговорВзаиморасчетов") val settlementContract: String? = null,
    @SerialName("ДокументОснование") val baseDocument: String? = null,
    @SerialName("СтруктурнаяЕдиница") val structuralUnit: String? = null,
    @SerialName("Комментарий") val comment: String? = null,
    @SerialName("Контрагент") val counterparty: String? = null,
    @SerialName("КурсДокумента") val exchangeRate: String? = null,
    @SerialName("Назначение") val purpose: String? = null,
    @SerialName("Организация") val organization: String? = null,
    @SerialName("ПодразделениеКомпании") val branch: String? = null,
    @SerialName("Проект") val project: String? = null,
    @SerialName("СпособПоследнегоЗаполнения") val lastFillMethod: String? = null,
    @SerialName("СтатьяДДС") val cashFlowItem: String? = null,
    @SerialName("СуммаДокумента") val amount: String? = null,
    @SerialName("ХозОперация") val operation: String? = null,
    @SerialName("КурсВалютыУпр") val managementExchangeRate: String? = null,
    @SerialName("КурсВалютыВзаиморасчетов") val settlementExchangeRate: String? = null,
    @SerialName("ДатаСоздания") val createdAt: String? = null,
    @SerialName("ДатаОперации") val operationDate: String? = null,
    @SerialName("Подтверждено") val confirmed: String? = null,
    @SerialName("ОплатаСогласована") val paymentApproved: String? = null,
    @SerialName("ОплатаСогласованаСотрудник") val paymentApprovedBy: String? = null,
    @SerialName("ОплатаСогласованаДата") val paymentApprovedAt: String? = null,
    @SerialName("Состояние") val state: String? = null,
    @SerialName("БанковскийСчетКонтрагента") val counterpartyBankAccount: String? = null,
    @SerialName("ОплатаСогласованаУК") val paymentApprovedByUk: String? = null,
    @SerialName("ОплатаСогласованаУКСотрудник") val paymentApprovedByUkEmployee: String? = null,
    @SerialName("ОплатаСогласованаУКДата") val paymentApprovedByUkAt: String? = null,
    @SerialName("Срочное") val urgent: String? = null,
    @SerialName("Платежи") val payments: List<ExpenseRequestPaymentDto> = emptyList(),
    @SerialName("Счета") val accounts: List<ExpenseRequestAccountDto> = emptyList(),
    @SerialName("Подписанты") val signatories: List<ExpenseRequestSignatoryDto> = emptyList(),
    @SerialName("РаспределениеПоСчетам") val accountAllocation: List<ExpenseRequestAccountAllocationDto> = emptyList(),
)

@Serializable
data class ExpenseRequestPaymentDto(
    @SerialName("НомерСтроки") val lineNumber: String? = null,
    @SerialName("ДатаПлатежа") val paymentDate: String? = null,
    @SerialName("СтатьяРасходов") val expenseItem: String? = null,
    @SerialName("Описание") val description: String? = null,
    @SerialName("Сумма") val amount: String? = null,
    @SerialName("СтатьяДДС") val cashFlowItem: String? = null,
    @SerialName("Направление") val direction: String? = null,
    @SerialName("Статус") val status: String? = null,
    @SerialName("Комментарий") val comment: String? = null,
    @SerialName("Согласовал") val approvedBy: String? = null,
    @SerialName("СуммаНДС") val vatAmount: String? = null,
    @SerialName("СтруктурнаяЕдиница") val structuralUnit: String? = null,
    @SerialName("СтавкаНДС") val vatRate: String? = null,
)

@Serializable
data class ExpenseRequestAccountDto(
    @SerialName("НомерСтроки") val lineNumber: String? = null,
)

@Serializable
data class ExpenseRequestAccountAllocationDto(
    @SerialName("НомерСтроки") val lineNumber: String? = null,
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

fun ExpenseRequestPaymentDto.isMeaningful(): Boolean {
    return !amount.isNullOrBlank() ||
        !paymentDate.isNullOrBlank() ||
        !status.isNullOrBlank() ||
        !description.isNullOrBlank()
}

fun ExpenseRequestDto.meaningfulSignatories(): List<ExpenseRequestSignatoryDto> =
    signatories.filter { it.isMeaningful() }

fun ExpenseRequestDto.meaningfulPayments(): List<ExpenseRequestPaymentDto> =
    payments.filter { it.isMeaningful() }

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
        ("Назначение" to detailDisplayValue(purpose)) to (
            "Сумма документа" to (formattedAmount()?.takeIf { it.isNotBlank() } ?: "—")
            ),
        ("Контрагент" to detailDisplayValue(counterparty)) to
            ("Договор взаиморасчётов" to detailDisplayValue(settlementContract)),
        ("Организация" to detailDisplayValue(organization)) to
            ("Подразделение" to detailDisplayValue(branch)),
        ("Состояние" to detailDisplayValue(state)) to
            ("Хоз. операция" to detailDisplayValue(operation)),
    )

/** Дополнительные поля — в той же сетке, только непустые. Нечётная последняя строка — одна колонка. */
fun ExpenseRequestDto.supplementaryDetailGridRows(): List<Pair<Pair<String, String>, Pair<String, String>?>> {
    val singles = buildList {
        fun addPair(label: String, raw: String?) {
            val value = detailDisplayValue(raw)
            if (value != "—") add(label to value)
        }
        addPair("Документ-основание", baseDocument)
        addPair("Автор", author)
        addPair("Дата операции", operationDate)
        addPair("Дата создания", createdAt)
        addPair("Подтверждено", confirmed)
        addPair("Оплата согласована", paymentApproved)
        addPair("Согласовал оплату", paymentApprovedBy)
        addPair("Дата согласования оплаты", paymentApprovedAt)
        addPair("Срочное", urgent)
        addPair("Статья ДДС", cashFlowItem)
        addPair("Курс документа", exchangeRate)
        addPair("Структурная единица", structuralUnit)
        addPair("Проект", project)
        addPair("Банковский счёт контрагента", counterpartyBankAccount)
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
    add("Комментарий", comment)
    add("Назначение", purpose)
    add("Валюта", currency)
    add("Договор взаиморасчётов", settlementContract)
    add("Документ-основание", baseDocument)
    add("Курс документа", exchangeRate)
    add("Организация", organization)
    add("Подразделение", branch)
    add("Статья ДДС", cashFlowItem)
    add("Сумма документа", amount)
    add("Состояние", state)
    add("Контрагент", counterparty)
    add("Структурная единица", structuralUnit)
    if (guid.isNotBlank()) add("GUID" to guid.trim())
}
