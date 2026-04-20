package com.tagaev.trrcrm.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull

typealias RepairTemplateCatalogResponse = List<RepairTemplateCatalogItemDto>

@Serializable
data class RepairTemplateWorkLineDto(
    @SerialName("НомерСтроки") val lineNumber: String? = null,
    @SerialName("Работа") val work: String? = null,
    @SerialName("Нормочас") val normHours: String? = null,
    @SerialName("Коэффициент") val coefficient: String? = null,
    @SerialName("Количество") val quantity: String? = null,
    @SerialName("Аналог") val analog: String? = null,
    @SerialName("Цена") val price: String? = null,
)

@Serializable
data class RepairTemplateGoodsLineDto(
    @SerialName("НомерСтроки") val lineNumber: String? = null,
    @SerialName("Номенклатура") val nomenclature: String? = null,
    @SerialName("Нормочас") val normHours: String? = null,
    @SerialName("Коэффициент") val coefficient: String? = null,
    @SerialName("Количество") val quantity: String? = null,
    @SerialName("Аналог") val analog: String? = null,
    @SerialName("Цена") val price: String? = null,
)

/** Справочник «ШаблоныРемонта» (getitemslist, type=Справочник). */
@Serializable
data class RepairTemplateCatalogItemDto(
    @SerialName("guid") val guid: String,
    @SerialName("Ссылка") val link: String? = null,
    @SerialName("Родитель") val parent: String? = null,
    @SerialName("Наименование") val name: String? = null,
    @SerialName("ТипКПП") val transmissionType: String? = null,
    @SerialName("Модель") val model: String? = null,
    @SerialName("ГодОт") val yearFrom: String? = null,
    @SerialName("ГодДо") val yearTo: String? = null,
    @SerialName("ДВС") val engine: String? = null,
    @SerialName("ЦенаРекомендованная") val recommendedPrice: String? = null,
    @SerialName("ЦенаРозничная") val retailPrice: String? = null,
    @SerialName("Код") val code: String? = null,
    @SerialName("ВидРемонта") val repairKind: String? = null,
    @SerialName("ЦенаАгрегата") val aggregatePrice: String? = null,
    @SerialName("Работы") val works: List<RepairTemplateWorkLineDto> = emptyList(),
    @SerialName("Товары") val goods: List<RepairTemplateGoodsLineDto> = emptyList(),
    /** Структура строк может отличаться; для отображения парсим как JSON. */
    @SerialName("Заказчики") val customers: JsonElement? = null,
)

private fun JsonElement.asPrimitiveText(): String = when (this) {
    is JsonPrimitive -> contentOrNull?.trim().orEmpty()
    else -> toString().trim()
}

private fun JsonObject.lineSummary(): String =
    entries.joinToString(separator = ", ") { (k, v) ->
        "$k: ${v.asPrimitiveText()}"
    }

fun RepairTemplateCatalogItemDto.nonEmptyHeaderPairs(): List<Pair<String, String>> = buildList {
    fun add(labelRu: String, raw: String?) {
        val v = raw?.trim().orEmpty()
        if (v.isEmpty()) return
        add(labelRu to v)
    }
    add("Ссылка", link)
    add("Родитель", parent)
    add("Наименование", name)
    add("Тип КПП", transmissionType)
    add("Модель", model)
    add("Год от", yearFrom)
    add("Год до", yearTo)
    add("ДВС", engine)
    add("Цена рекоменд.", recommendedPrice)
    add("Цена розничная", retailPrice)
}

fun RepairTemplateCatalogItemDto.customerRowsForDisplay(): List<String> {
    val el = customers ?: return emptyList()
    if (el !is JsonArray || el.isEmpty()) return emptyList()
    return el.map { child ->
        when (child) {
            is JsonObject -> child.lineSummary().ifBlank { child.toString() }
            else -> child.asPrimitiveText().ifBlank { child.toString() }
        }
    }
}

fun RepairTemplateCatalogItemDto.hasCustomersSection(): Boolean = customerRowsForDisplay().isNotEmpty()
