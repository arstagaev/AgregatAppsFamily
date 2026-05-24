package com.tagaev.trrcrm.domain.messages

private val spacesRegex = Regex("\\s+")

fun normalizedPushRecipients(
    candidates: Iterable<String?>,
    currentUser: String?,
): List<String> {
    val normalizedCurrentUser = currentUser.normalizeRecipientName()
    val deduped = LinkedHashMap<String, String>()
    for (candidate in candidates) {
        val normalized = candidate.normalizeRecipientName() ?: continue
        if (normalized.equals("я", ignoreCase = true)) continue
        if (normalizedCurrentUser != null && normalized.equals(normalizedCurrentUser, ignoreCase = true)) continue
        val dedupeKey = normalized.lowercase()
        if (!deduped.containsKey(dedupeKey)) {
            deduped[dedupeKey] = normalized
        }
    }
    return deduped.values.toList()
}

private fun String?.normalizeRecipientName(): String? {
    val normalized = this?.trim()?.replace(spacesRegex, " ").orEmpty()
    return normalized.ifBlank { null }
}
