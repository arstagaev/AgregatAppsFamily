package com.tagaev.trrcrm.utils

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import com.tagaev.secrets.Secrets
import com.tagaev.secrets.Secrets.IS_PUBLISH
import com.tagaev.trrcrm.domain.messages.DraftedMessage
import com.tagaev.trrcrm.getPlatform
import com.tagaev.trrcrm.models.EventItemDto


object DefaultValuesConst {
    const val TASK = "getitemslist"

    const val NCOUNT = 0
    const val COUNT = 30

    const val NAME = "Событие"
    const val TYPE = "Документ"

    const val ORDER_BY = "ДатаМод"
    const val ORDER_DIR = "desc"

    const val FILTER_BY = "Состояние" // ПодразделениеКомпании
    const val FILTER_VAL = "Выполняется"
    // Выполнено Выполняется
    const val FILTER_TYPE = "list" // Выполнено Выполняется
    const val MESSAGE_MAX_CHARS = 500

    val GLOBAL_PUSH_URL = if (IS_PUBLISH.toBoolean()) Secrets.PUSH_BASE_URL else getPlatform().baseDebugFCMurl

//    var needBack
}

object ServerAnswers {
    const val EMPTY_ANSWERS = "empty_answers"
}

var TARGET_EVENT = mutableStateOf(EventItemDto())

// In-memory storage of drafted (not yet sent) messages per order GUID.
// Using mutableStateListOf so Compose can react if someone ever observes it.
val DRAFTED_MESSAGES_ARRAY = mutableStateListOf<DraftedMessage>()
