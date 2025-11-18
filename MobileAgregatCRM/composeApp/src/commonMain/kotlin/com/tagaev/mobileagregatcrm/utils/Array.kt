package com.tagaev.mobileagregatcrm.utils

private val ROLE_ORDER = listOf("ответственный", "делаю", "помогаю", "наблюдаю")

fun roleRank(raw: String?): Int {
    val key = raw?.trim()?.lowercase() ?: return Int.MAX_VALUE
    val i = ROLE_ORDER.indexOf(key)
    return if (i == -1) Int.MAX_VALUE else i
}