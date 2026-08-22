package com.tagaev.trrcrm.navigation

import com.tagaev.trrcrm.ui.i18n.tr
import com.tagaev.trrcrm.ui.root.IRootComponent
import com.tagaev.trrcrm.utils.KnownPermission

enum class BottomNavItemId(val wire: String) {
    EVENTS("events"),
    WORK_ORDER("work_order"),
    COMPLECTATION("complectation"),
    CARGO("cargo"),
    BUYER_ORDER("buyer_order"),
    SUPPLIER_ORDER("supplier_order"),
    COMPLAINT("complaint"),
    INNER_ORDER("inner_order"),
    INCOMING_APPLICATIONS("incoming_applications"),
    REPAIR_TEMPLATE_CATALOG("repair_template_catalog"),
    EXPENSE_REQUESTS("expense_requests"),
    QR_SCANNER("qr_scanner"),
    MENU("menu");

    val label: String
        get() = when (this) {
            EVENTS -> tr("nav_sobytiya")
            WORK_ORDER -> tr("nav_zakaz_naryady")
            COMPLECTATION -> tr("nav_komplektatsiya")
            CARGO -> tr("nav_dostavki")
            BUYER_ORDER -> tr("nav_zakazy_pokup")
            SUPPLIER_ORDER -> tr("nav_zakazy_post")
            COMPLAINT -> tr("nav_reklamatsii")
            INNER_ORDER -> tr("nav_vnutr_zakazy")
            INCOMING_APPLICATIONS -> tr("nav_vhodyaschie_zayavki")
            REPAIR_TEMPLATE_CATALOG -> tr("nav_kalkulyatsiya")
            EXPENSE_REQUESTS -> tr("nav_zayavki_rashod_ds")
            QR_SCANNER -> tr("nav_qr_skaner")
            MENU -> tr("menu_nastroyki")
        }

    val permission: KnownPermission?
        get() = when (this) {
            WORK_ORDER -> KnownPermission.ZAKAZ_NARYAD
            COMPLECTATION -> KnownPermission.KOMPLEKTATSIYA
            CARGO -> KnownPermission.GRUZ
            BUYER_ORDER -> KnownPermission.ZAKAZ_POKUPATELYA
            SUPPLIER_ORDER -> KnownPermission.ZAKAZ_POSTAVSHCHIKU
            INNER_ORDER -> KnownPermission.ZAKAZ_VNUTRENNIY
            EXPENSE_REQUESTS -> KnownPermission.ZAYAVKA_NA_RASHOD_DS
            else -> null
        }

    val isHideable: Boolean
        get() = when (this) {
            EVENTS, QR_SCANNER, MENU -> false
            else -> true
        }

    fun isSelected(child: IRootComponent.Child): Boolean = when (this) {
        EVENTS -> child is IRootComponent.Child.Events
        WORK_ORDER -> child is IRootComponent.Child.WorkOrder
        COMPLECTATION -> child is IRootComponent.Child.Complectation
        CARGO -> child is IRootComponent.Child.Cargo
        BUYER_ORDER -> child is IRootComponent.Child.BuyerOrder
        SUPPLIER_ORDER -> child is IRootComponent.Child.SupplierOrder
        COMPLAINT -> child is IRootComponent.Child.Complaint
        INNER_ORDER -> child is IRootComponent.Child.InnerOrder
        INCOMING_APPLICATIONS -> child is IRootComponent.Child.IncomingApplications
        REPAIR_TEMPLATE_CATALOG -> child is IRootComponent.Child.RepairTemplateCatalog
        EXPENSE_REQUESTS -> child is IRootComponent.Child.ExpenseRequests
        QR_SCANNER -> child is IRootComponent.Child.QRScanner
            MENU -> child is IRootComponent.Child.Settings
    }

    companion object {
        val defaultOrder: List<BottomNavItemId> = listOf(
            EVENTS,
            WORK_ORDER,
            COMPLECTATION,
            CARGO,
            BUYER_ORDER,
            SUPPLIER_ORDER,
            COMPLAINT,
            INNER_ORDER,
            INCOMING_APPLICATIONS,
            REPAIR_TEMPLATE_CATALOG,
            EXPENSE_REQUESTS,
            QR_SCANNER,
            MENU,
        )

        fun fromWire(wire: String): BottomNavItemId? =
            entries.firstOrNull { it.wire == wire.trim() }
    }
}

fun IRootComponent.Child.toBottomNavItemId(): BottomNavItemId? = when (this) {
    is IRootComponent.Child.Events -> BottomNavItemId.EVENTS
    is IRootComponent.Child.WorkOrder -> BottomNavItemId.WORK_ORDER
    is IRootComponent.Child.Complectation -> BottomNavItemId.COMPLECTATION
    is IRootComponent.Child.Cargo -> BottomNavItemId.CARGO
    is IRootComponent.Child.BuyerOrder -> BottomNavItemId.BUYER_ORDER
    is IRootComponent.Child.SupplierOrder -> BottomNavItemId.SUPPLIER_ORDER
    is IRootComponent.Child.Complaint -> BottomNavItemId.COMPLAINT
    is IRootComponent.Child.InnerOrder -> BottomNavItemId.INNER_ORDER
    is IRootComponent.Child.IncomingApplications -> BottomNavItemId.INCOMING_APPLICATIONS
    is IRootComponent.Child.RepairTemplateCatalog -> BottomNavItemId.REPAIR_TEMPLATE_CATALOG
    is IRootComponent.Child.ExpenseRequests -> BottomNavItemId.EXPENSE_REQUESTS
    is IRootComponent.Child.QRScanner -> BottomNavItemId.QR_SCANNER
    is IRootComponent.Child.Settings -> BottomNavItemId.MENU
    else -> null
}
