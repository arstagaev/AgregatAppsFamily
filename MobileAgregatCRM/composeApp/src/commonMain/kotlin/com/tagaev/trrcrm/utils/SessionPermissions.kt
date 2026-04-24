package com.tagaev.trrcrm.utils

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

    /** Нет явного ключа или не «Нет доступа» — показываем вкладку. */
    fun canOpenDocumentTab(key: KnownPermission): Boolean {
        val raw = value(key.wire) ?: return true
        return classifyPermissionValue(raw) != AccessValueKind.NONE
    }
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
