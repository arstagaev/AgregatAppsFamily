package com.tagaev.trrcrm.ui.complectation

import androidx.compose.foundation.ScrollState
import com.tagaev.trrcrm.models.WorkOrderDto

/** Expandable groups in the complectation document header. */
enum class ComplectationDetailsSection {
    PRODUCTS,
    JOBS,
    DEFECTS,
    PLANNING,
    CHECKLIST,
}

/**
 * Per-document UI state for the details stack (scroll, history pager, section toggles).
 * Not serialized — cleared when a different list item is selected.
 */
data class StackedDocumentDetailsSnapshot(
    val scroll: Int = 0,
    val showAllHistory: Boolean = false,
    val sectionExpanded: Set<ComplectationDetailsSection> = emptySet(),
) {
    fun isSectionExpanded(section: ComplectationDetailsSection): Boolean = section in sectionExpanded

    fun withSectionExpanded(section: ComplectationDetailsSection, value: Boolean): StackedDocumentDetailsSnapshot {
        val newSet = if (value) {
            sectionExpanded + section
        } else {
            sectionExpanded - section
        }
        return copy(sectionExpanded = newSet)
    }

    fun withScroll(s: Int): StackedDocumentDetailsSnapshot = if (s == scroll) this else copy(scroll = s)
    fun withShowAllHistory(b: Boolean): StackedDocumentDetailsSnapshot =
        if (b == showAllHistory) this else copy(showAllHistory = b)
}

/**
 * St stable key for the comp completion opened from the master list (root of the link stack).
 */
fun complectationRootListStateKey(order: WorkOrderDto): String {
    val id = order.guid.toString()
    if (id.isNotBlank() && id != "null") return "COMPLECTATION_LIST:$id"
    val num = order.number?.trim().orEmpty()
    return "COMPLECTATION_LIST:number:$num"
}

/** Scroll + снимок для [ComplectationDetailsSheet] при навигации в стеке. */
data class ComplectationTreeStackedUi(
    val detailsScroll: ScrollState,
    val detailsSnapshot: StackedDocumentDetailsSnapshot,
    val onDetailsSnapshot: (StackedDocumentDetailsSnapshot) -> Unit,
)
