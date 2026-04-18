package com.tagaev.trrcrm.domain

import com.tagaev.trrcrm.models.BuyerOrderDto
import com.tagaev.trrcrm.models.CargoDto
import com.tagaev.trrcrm.models.ComplaintDto
import com.tagaev.trrcrm.models.EventItemDto
import com.tagaev.trrcrm.models.InnerOrderDto
import com.tagaev.trrcrm.models.SupplierOrderDto
import com.tagaev.trrcrm.models.WorkOrderDto

enum class TreeRootDocumentKind {
    EVENT,
    WORK_ORDER,
    COMPLECTATION,
    COMPLAINT,
    INNER_ORDER,
    BUYER_ORDER,
    SUPPLIER_ORDER,
    CARGO,
}

fun TreeRootDocumentKind.displayNameRu(): String = when (this) {
    TreeRootDocumentKind.EVENT -> "Событие"
    TreeRootDocumentKind.WORK_ORDER -> "Заказ-наряд"
    TreeRootDocumentKind.COMPLECTATION -> "Комплектация"
    TreeRootDocumentKind.COMPLAINT -> "Рекламация"
    TreeRootDocumentKind.INNER_ORDER -> "Заказ внутренний"
    TreeRootDocumentKind.BUYER_ORDER -> "Заказ покупателя"
    TreeRootDocumentKind.SUPPLIER_ORDER -> "Заказ поставщику"
    TreeRootDocumentKind.CARGO -> "Груз"
}

data class TreeRootDocumentRef(
    val rawType: String,
    val requestName: String,
    val documentNumber: String,
    val kind: TreeRootDocumentKind,
)

sealed class TreeRootResolvedDocument {
    abstract val kind: TreeRootDocumentKind
    abstract val guid: String?

    data class Event(val value: EventItemDto) : TreeRootResolvedDocument() {
        override val kind: TreeRootDocumentKind = TreeRootDocumentKind.EVENT
        override val guid: String? = value.guid
    }

    data class WorkOrder(val value: WorkOrderDto) : TreeRootResolvedDocument() {
        override val kind: TreeRootDocumentKind = TreeRootDocumentKind.WORK_ORDER
        override val guid: String? = value.guid
    }

    data class Complectation(val value: WorkOrderDto) : TreeRootResolvedDocument() {
        override val kind: TreeRootDocumentKind = TreeRootDocumentKind.COMPLECTATION
        override val guid: String? = value.guid
    }

    data class Complaint(val value: ComplaintDto) : TreeRootResolvedDocument() {
        override val kind: TreeRootDocumentKind = TreeRootDocumentKind.COMPLAINT
        override val guid: String? = value.guid
    }

    data class InnerOrder(val value: InnerOrderDto) : TreeRootResolvedDocument() {
        override val kind: TreeRootDocumentKind = TreeRootDocumentKind.INNER_ORDER
        override val guid: String? = value.guid
    }

    data class BuyerOrder(val value: BuyerOrderDto) : TreeRootResolvedDocument() {
        override val kind: TreeRootDocumentKind = TreeRootDocumentKind.BUYER_ORDER
        override val guid: String? = value.guid
    }

    data class SupplierOrder(val value: SupplierOrderDto) : TreeRootResolvedDocument() {
        override val kind: TreeRootDocumentKind = TreeRootDocumentKind.SUPPLIER_ORDER
        override val guid: String? = value.guid
    }

    data class Cargo(val value: CargoDto) : TreeRootResolvedDocument() {
        override val kind: TreeRootDocumentKind = TreeRootDocumentKind.CARGO
        override val guid: String? = value.guid
    }
}

/**
 * Stable key for scoping per-document details UI (scroll, expansions) in the link stack.
 */
fun TreeRootResolvedDocument.stableStateKey(): String {
    val g = guid?.trim().orEmpty()
    if (g.isNotBlank() && g != "null") return "${kind.name}:$g"
    return when (this) {
        is TreeRootResolvedDocument.Event -> "${kind.name}:n:${value.number}"
        is TreeRootResolvedDocument.WorkOrder -> "${kind.name}:n:${value.number}"
        is TreeRootResolvedDocument.Complectation -> "${kind.name}:n:${value.number}"
        is TreeRootResolvedDocument.Complaint -> "${kind.name}:n:${value.number}"
        is TreeRootResolvedDocument.InnerOrder -> "${kind.name}:n:${value.number}"
        is TreeRootResolvedDocument.BuyerOrder -> "${kind.name}:n:${value.number}"
        is TreeRootResolvedDocument.SupplierOrder -> "${kind.name}:n:${value.number}"
        is TreeRootResolvedDocument.Cargo -> "${kind.name}:n:${value.number}"
    }
}

/**
 * Normalizes 1C-style document reference strings for display and for [TreeRootDocument.parse]:
 * NBSP, CR/LF to spaces, collapses whitespace.
 */
fun normalizeRawDocumentLabel(raw: String): String =
    raw.trim()
        .replace('\u00a0', ' ')
        .replace('\r', ' ')
        .replace('\n', ' ')
        .replace(Regex("""\s+"""), " ")
        .trim()

object TreeRootDocument {
    private val typeAndNumberRegex = Regex(
        pattern = """^\s*([^\d]+?)\s+([0-9A-Za-zА-Яа-я\-_/]+)\s+от\s+.+$""",
        option = RegexOption.IGNORE_CASE
    )

    fun parse(rawBaseDocument: String): TreeRootDocumentRef? {
        val raw = normalizeRawDocumentLabel(rawBaseDocument)
        if (raw.isBlank()) return null

        val match = typeAndNumberRegex.find(raw) ?: return null
        val rawType = match.groupValues.getOrNull(1)?.trim().orEmpty()
        val numberToken = match.groupValues.getOrNull(2)?.trim().orEmpty()
        if (rawType.isBlank() || numberToken.isBlank()) return null

        val normalizedType = normalizeType(rawType) ?: return null
        val normalizedNumber = normalizeNumber(numberToken) ?: return null
        return TreeRootDocumentRef(
            rawType = rawType,
            requestName = normalizedType.requestName,
            documentNumber = normalizedNumber,
            kind = normalizedType.kind
        )
    }

    private data class TypeMapping(
        val requestName: String,
        val kind: TreeRootDocumentKind
    )

    private fun normalizeType(rawType: String): TypeMapping? {
        val normalized = rawType
            .lowercase()
            .replace(Regex("""["'`«»]"""), "")
            .replace("-", " ")
            // Unicode dashes (1C / copy-paste) so «Заказ–наряд» matches «заказ наряд»
            .replace('\u2013', ' ')
            .replace('\u2014', ' ')
            .replace('\u2010', ' ')
            .replace(Regex("""\s+"""), " ")
            .trim()

        return when (normalized) {
            "событие" -> TypeMapping("Событие", TreeRootDocumentKind.EVENT)
            "заказ наряд" -> TypeMapping("ЗаказНаряд", TreeRootDocumentKind.WORK_ORDER)
            "комплектация" -> TypeMapping("Комплектация", TreeRootDocumentKind.COMPLECTATION)
            "рекламация" -> TypeMapping("Рекламация", TreeRootDocumentKind.COMPLAINT)
            "заказ внутренний", "внутренний заказ" -> TypeMapping("ЗаказВнутренний", TreeRootDocumentKind.INNER_ORDER)
            "заказ покупателя", "заказ покупат", "покупательский заказ" -> TypeMapping("ЗаказПокупателя", TreeRootDocumentKind.BUYER_ORDER)
            "заказ поставщику", "заказ поставщика" -> TypeMapping("ЗаказПоставщику", TreeRootDocumentKind.SUPPLIER_ORDER)
            "груз" -> TypeMapping("Груз", TreeRootDocumentKind.CARGO)
            else -> null
        }
    }

    private fun normalizeNumber(rawNumber: String): String? {
        val normalized = rawNumber
            .replace(Regex("""\s+"""), "")
            .trim()
        return normalized.takeIf { it.isNotBlank() }
    }
}
