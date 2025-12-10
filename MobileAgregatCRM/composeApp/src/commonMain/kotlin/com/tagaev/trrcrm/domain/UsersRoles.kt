package com.tagaev.trrcrm.domain

import com.tagaev.trrcrm.models.UserRowDto
import com.tagaev.trrcrm.models.isResponsible


data class UserRow(
    val name: String,
    val role: String,
    val isResponsible: Boolean = false
)

private val rolePriority = mapOf(
    "ответственный" to 0,
    "делаю"         to 1,
    "помогаю"       to 2,
    "наблюдаю"      to 3
)

fun List<UserRow>.sortedByRolePriority(): List<UserRow> = sortedWith(
    compareBy<UserRow> { user ->
        when {
            user.isResponsible -> 0
            else -> {
                val lowerRole = user.role.lowercase()
                rolePriority[lowerRole] ?: Int.MAX_VALUE
            }
        }
    }.thenBy { it.name.lowercase() }
)