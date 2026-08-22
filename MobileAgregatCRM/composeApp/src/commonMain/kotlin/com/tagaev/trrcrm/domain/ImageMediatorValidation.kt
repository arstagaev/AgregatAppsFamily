package com.tagaev.trrcrm.domain

private val DIGITS_ONLY_NUMBER = Regex("^[0-9]{6,12}$")
private val CYRILLIC_PREFIX_NUMBER = Regex("^[А-ЯЁ]{2,3}[0-9]{6,12}$")

/**
 * Unicode NFC for document numbers. Full ICU is not available in commonMain;
 * this composes the combining sequences that actually appear in 1C prefixes
 * (primarily Cyrillic Е/е + U+0308 → Ё/ё).
 */
internal fun nfcNormalize(value: String): String {
    if (value.none { it.isCombiningMark() }) return value
    val out = StringBuilder(value.length)
    var index = 0
    while (index < value.length) {
        val current = value[index]
        val combining = value.getOrNull(index + 1)
        val composed = combining?.let { composeCyrillic(current, it) }
        if (composed != null) {
            out.append(composed)
            index += 2
        } else {
            out.append(current)
            index += 1
        }
    }
    return out.toString()
}

private fun Char.isCombiningMark(): Boolean = this in '\u0300'..'\u036F'

private fun composeCyrillic(base: Char, mark: Char): Char? {
    if (mark != '\u0308') return null
    return when (base) {
        'Е' -> 'Ё'
        'е' -> 'ё'
        else -> null
    }
}

fun normalizeDocumentNumber(raw: String): String =
    nfcNormalize(raw.trim()).uppercase()

fun isValidDocumentNumber(raw: String): Boolean {
    val normalized = normalizeDocumentNumber(raw)
    return DIGITS_ONLY_NUMBER.matches(normalized) || CYRILLIC_PREFIX_NUMBER.matches(normalized)
}

fun resolvedDocumentNumber(resolved: String?, fallback: String): String {
    val candidate = resolved?.let(::normalizeDocumentNumber)?.takeIf(::isValidDocumentNumber)
    return candidate ?: fallback
}
