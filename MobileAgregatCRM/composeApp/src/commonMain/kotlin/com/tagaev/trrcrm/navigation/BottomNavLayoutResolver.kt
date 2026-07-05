package com.tagaev.trrcrm.navigation

import com.tagaev.trrcrm.utils.SessionPermissions

object BottomNavLayoutResolver {

    fun defaultLayout(): List<BottomNavLayoutItem> =
        BottomNavItemId.defaultOrder.map { BottomNavLayoutItem(id = it.wire, visible = true) }

    fun mergeLayout(saved: List<BottomNavLayoutItem>?): List<BottomNavLayoutItem> {
        val knownIds = BottomNavItemId.defaultOrder.map { it.wire }.toSet()
        val ordered = mutableListOf<BottomNavLayoutItem>()

        saved.orEmpty().forEach { item ->
            if (item.id in knownIds && ordered.none { it.id == item.id }) {
                ordered += item
            }
        }

        BottomNavItemId.defaultOrder.forEach { id ->
            if (ordered.none { it.id == id.wire }) {
                ordered += BottomNavLayoutItem(id = id.wire, visible = true)
            }
        }

        return enforceRequiredVisibility(ordered)
    }

    fun enforceRequiredVisibility(layout: List<BottomNavLayoutItem>): List<BottomNavLayoutItem> =
        layout.map { item ->
            val id = BottomNavItemId.fromWire(item.id)
            if (id != null && !id.isHideable) item.copy(visible = true) else item
        }

    fun isAvailable(id: BottomNavItemId): Boolean {
        val permission = id.permission ?: return true
        return SessionPermissions.canOpenDocumentTab(permission)
    }

    fun resolveVisibleTabs(layout: List<BottomNavLayoutItem>): List<BottomNavItemId> =
        enforceRequiredVisibility(layout)
            .filter { it.visible }
            .mapNotNull { BottomNavItemId.fromWire(it.id) }
            .filter(::isAvailable)

    fun resolveEditorItems(layout: List<BottomNavLayoutItem>): List<BottomNavLayoutItem> =
        layout.filter { item ->
            val id = BottomNavItemId.fromWire(item.id) ?: return@filter false
            isAvailable(id)
        }

    fun layoutsEqual(a: List<BottomNavLayoutItem>, b: List<BottomNavLayoutItem>): Boolean {
        if (a.size != b.size) return false
        return a.zip(b).all { (left, right) -> left.id == right.id && left.visible == right.visible }
    }
}
