package com.tagaev.trrcrm.utils

import com.tagaev.trrcrm.ui.i18n.tr

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import com.tagaev.trrcrm.models.UserPermissionEntryDto

/**
 * Права из `getpermission` за сессию. [state] подписывается из Compose для пересчёта UI.
 */
object SessionPermissions {
    private val _state: MutableState<Map<String, String>> = mutableStateOf(emptyMap())
    val state: MutableState<Map<String, String>> get() = _state

    fun replaceAll(entries: List<UserPermissionEntryDto>) {
        _state.value = entries.associate { it.permission.trim() to it.value.trim() }
    }

    fun clear() {
        _state.value = emptyMap()
    }

    fun value(permissionKey: String): String? = _state.value[permissionKey]

    fun canShowDepartmentFilter(): Boolean {
        val raw = value(KnownPermission.PODRAZDELENIYA.wire) ?: return false
        return classifyPermissionValue(raw) != AccessValueKind.NONE
    }

    fun canOpenExpenseRequestsTab(): Boolean {
        val raw = value(KnownPermission.ZAYAVKA_NA_RASHOD_DS.wire) ?: return false
        return expenseRequestAllowedValues.any { it.equals(raw.trim(), ignoreCase = true) }
    }

    /** Нет явного ключа или не «Нет доступа» — показываем вкладку (кроме allow-list типов). */
    fun canOpenDocumentTab(key: KnownPermission): Boolean {
        if (key == KnownPermission.ZAYAVKA_NA_RASHOD_DS) return canOpenExpenseRequestsTab()
        val raw = value(key.wire) ?: return true
        return classifyPermissionValue(raw) != AccessValueKind.NONE
    }

    private val expenseRequestAllowedValues = setOf(
        "Редактирование все",
        "Просмотр по подразделению в карточке пользователя",
        "Возможность чтения",
        tr("settings_da"),
    )
}

/** Ключи `permission`, которые клиент интерпретирует в v1. */
enum class KnownPermission(val wire: String) {
    PODRAZDELENIYA("Право доступа ПодразделенияКомпании"),
    ZAKAZ_NARYAD("Право доступа ЗаказНаряд"),
    GRUZ("Право доступа Груз"),
    ZAKAZ_POKUPATELYA("Право доступа ЗаказПокупателя"),
    ZAKAZ_POSTAVSHCHIKU("Право доступа ЗаказПоставщику"),
    ZAKAZ_VNUTRENNIY("Право доступа ЗаказВнутренний"),
    KOMPLEKTATSIYA("Право доступа Комплектация"),
    ZAYAVKA_NA_RASHOD_DS("Право доступа ЗаявкаНаРасходДС"),
}

enum class AccessValueKind {
    NONE,
    READ,
    EDIT_SCOPED,
    EDIT_ALL,
    UNKNOWN,
}

fun classifyPermissionValue(raw: String): AccessValueKind {
    val v = raw.trim()
    if (v.isEmpty()) return AccessValueKind.UNKNOWN
    if (v.equals("Нет доступа", ignoreCase = true)) return AccessValueKind.NONE
    if (v.equals("Возможность чтения", ignoreCase = true)) return AccessValueKind.READ
    if (v.contains("Чтение все", ignoreCase = true)) return AccessValueKind.READ
    if (v.contains("Просмотр", ignoreCase = true)) return AccessValueKind.READ
    if (v.contains("Редактирование все", ignoreCase = true)) return AccessValueKind.EDIT_ALL
    if (v.contains("Редактирование по подразделениям", ignoreCase = true)) return AccessValueKind.EDIT_SCOPED
    if (v.contains("Редактирование по пользователям", ignoreCase = true)) return AccessValueKind.EDIT_SCOPED
    if (v.contains("Редактирование по группам", ignoreCase = true)) return AccessValueKind.EDIT_SCOPED
    if (v.contains("Редактирование", ignoreCase = true)) return AccessValueKind.EDIT_SCOPED
    if (v.contains("Создание новых", ignoreCase = true)) return AccessValueKind.READ
    return AccessValueKind.UNKNOWN
}
