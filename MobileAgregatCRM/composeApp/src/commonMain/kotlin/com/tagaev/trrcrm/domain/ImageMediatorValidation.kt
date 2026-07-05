package com.tagaev.trrcrm.domain

private val DOCUMENT_NUMBER_REGEX = Regex("^\\d{6,12}$")

fun normalizeDocumentNumber(raw: String): String = raw.trim()

fun isValidDocumentNumber(raw: String): Boolean =
    DOCUMENT_NUMBER_REGEX.matches(normalizeDocumentNumber(raw))
