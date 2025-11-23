package com.tagaev.mobileagregatcrm.domain.messages

import com.tagaev.mobileagregatcrm.utils.DRAFTED_MESSAGES_ARRAY

data class DraftedMessage(val guid: String, var message: String)

fun findDraftForGuid(guid: String?): String {
    if (guid.isNullOrBlank()) return ""
    return DRAFTED_MESSAGES_ARRAY.firstOrNull { it.guid == guid }?.message.orEmpty()
}

fun upsertDraftForGuid(guid: String?, message: String) {
    if (guid.isNullOrBlank()) return
    val idx = DRAFTED_MESSAGES_ARRAY.indexOfFirst { it.guid == guid }
    if (idx >= 0) {
        // update existing draft
        val existing = DRAFTED_MESSAGES_ARRAY[idx]
        if (existing.message != message) {
            DRAFTED_MESSAGES_ARRAY[idx] = existing.copy(message = message)
        }
    } else {
        // add new draft
        DRAFTED_MESSAGES_ARRAY.add(DraftedMessage(guid = guid, message = message))
    }
}

fun removeDraftIfMatches(guid: String?, message: String) {
    val iterator = DRAFTED_MESSAGES_ARRAY.iterator()
    while (iterator.hasNext()) {
        val draft = iterator.next()
        if (draft.guid == guid || draft.message == message) {
            iterator.remove()
            break
        }
    }
}