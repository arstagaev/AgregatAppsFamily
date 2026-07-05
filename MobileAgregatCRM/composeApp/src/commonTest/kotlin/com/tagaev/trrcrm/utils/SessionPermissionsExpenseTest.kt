package com.tagaev.trrcrm.utils

import com.tagaev.trrcrm.models.UserPermissionEntryDto
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SessionPermissionsExpenseTest {

    @AfterTest
    fun tearDown() {
        SessionPermissions.clear()
    }

    @Test
    fun missingKey_hidesExpenseRequestsTab() {
        SessionPermissions.clear()
        assertFalse(SessionPermissions.canOpenExpenseRequestsTab())
        assertFalse(SessionPermissions.canOpenDocumentTab(KnownPermission.ZAYAVKA_NA_RASHOD_DS))
    }

    @Test
    fun allowedValues_showExpenseRequestsTab() {
        val allowed = listOf(
            "Редактирование все",
            "Просмотр по подразделению в карточке пользователя",
            "Возможность чтения",
            "Да",
        )
        allowed.forEach { value ->
            SessionPermissions.replaceAll(
                listOf(UserPermissionEntryDto("Право доступа ЗаявкаНаРасходДС", value))
            )
            assertTrue(
                SessionPermissions.canOpenExpenseRequestsTab(),
                "Expected visible for value=$value",
            )
        }
    }

    @Test
    fun deniedValues_hideExpenseRequestsTab() {
        listOf("Нет", "Нет доступа", "Редактирование по подразделениям").forEach { value ->
            SessionPermissions.replaceAll(
                listOf(UserPermissionEntryDto("Право доступа ЗаявкаНаРасходДС", value))
            )
            assertFalse(
                SessionPermissions.canOpenExpenseRequestsTab(),
                "Expected hidden for value=$value",
            )
        }
    }
}
