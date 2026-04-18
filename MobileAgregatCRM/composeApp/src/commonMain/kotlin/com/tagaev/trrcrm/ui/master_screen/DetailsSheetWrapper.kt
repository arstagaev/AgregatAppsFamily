package com.tagaev.trrcrm.ui.master_screen

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.tagaev.trrcrm.domain.messages.findDraftForGuid
import com.tagaev.trrcrm.domain.messages.removeDraftIfMatches
import com.tagaev.trrcrm.domain.messages.upsertDraftForGuid
import com.tagaev.trrcrm.ui.custom.StatusBadge
import com.tagaev.trrcrm.ui.custom.StatusStyle
import com.tagaev.trrcrm.ui.custom.TextCLinkPreview
import com.tagaev.trrcrm.ui.master_screen.models.MessageModel
import com.tagaev.trrcrm.ui.work_order.LimitedOutlinedTextField


/**
 * Generic detail sheet with message history and composer in one scrollable column
 * (composer scrolls together with document header and history).
 *
 * [onSendMessage] receives the draft text and a callback.
 *   Callback argument: null = success, non-null String = error message.
 */
@Composable
fun <T> DetailsWithMessagesSheet(
    item: T,
    guid: String,
    messages: List<MessageModel>,
    onBack: () -> Unit,
    /** (draft, result) — result is null on success, error string on failure. */
    onSendMessage: (String, (String?) -> Unit) -> Unit,

    initialDraft: String? = null,
    onDraftChanged: (String) -> Unit = {},
    isSendEnabled: (draft: String, item: T) -> Boolean = { draft, _ -> draft.isNotBlank() },
    historyTitle: String = "Комментарии:",
    historyEmptyText: String = "Нет комментариев",
    historyPagerDescription: (showAll: Boolean, total: Int) -> String = { showAll, total ->
        if (showAll) "Показаны все $total комментариев"
        else "Показаны последние 10 из $total"
    },
    addCommentTitle: String = "Добавить комментарий",
    composerPlaceholder: String = "Комментарий по работам…",
    sendingDialogTitle: String = "Отправка комментария",
    /** When non-null, hide composer after this many rows exist in history (e.g. 1 for complectation). */
    maxMessagesInHistory: Int? = null,
    showComposer: Boolean = true,
    /**
     * When null, a local [androidx.compose.foundation.rememberScrollState] is used.
     * When non-null, the scroll position is fully controlled (e.g. to restore when returning from a linked document).
     */
    scrollState: ScrollState? = null,
    /**
     * When both non-null, "Показать все" for the history list (>10) is owned by the caller. Otherwise internal state.
     */
    showAllHistory: Boolean? = null,
    onShowAllHistoryChange: ((Boolean) -> Unit)? = null,
    headerContent: @Composable (ColumnScope.(T) -> Unit),
) {
    val detailsScrollState = scrollState ?: rememberScrollState()
    var messageDraft by remember(guid, initialDraft) {
        val base = when {
            !initialDraft.isNullOrEmpty() -> initialDraft
            else -> findDraftForGuid(guid)
        }
        mutableStateOf(base.orEmpty())
    }
    var isSendingMessage by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var lastFailedDraft by remember { mutableStateOf<String?>(null) }
    val internalMessages = remember(messages) { messages.toMutableStateList() }
    var showAllInternal by remember(messages) { mutableStateOf(false) }
    val isExternalHistory = showAllHistory != null && onShowAllHistoryChange != null
    val showAllMessages: Boolean = if (isExternalHistory) showAllHistory == true else showAllInternal
    fun setShowAllMessages(new: Boolean) {
        if (isExternalHistory) onShowAllHistoryChange!!(new) else showAllInternal = new
    }

    val composerVisible = showComposer && (maxMessagesInHistory == null || internalMessages.size < maxMessagesInHistory)

    // Helper: execute send with all guard / state logic
    fun doSend(draft: String) {
        if (isSendingMessage) return
        isSendingMessage = true
        lastFailedDraft = draft
        onSendMessage(draft) { err ->
            isSendingMessage = false
            if (err == null) {
                internalMessages.add(MessageModel(author = "я", text = draft))
                removeDraftIfMatches(guid = guid, message = draft)
                messageDraft = ""
                onDraftChanged("")
                lastFailedDraft = null
                errorMessage = null
            } else {
                errorMessage = err.ifBlank { "Ошибка отправки сообщения" }
            }
        }
    }

    // Sending progress dialog (non-dismissable)
    if (isSendingMessage) {
        AlertDialog(
            onDismissRequest = {},
            title = { Text(sendingDialogTitle) },
            text = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(
                        modifier = Modifier
                            .height(24.dp)
                            .width(24.dp)
                    )
                    Spacer(Modifier.width(12.dp))
                    Text("Пожалуйста, подождите…")
                }
            },
            confirmButton = {}
        )
    }

    // Error dialog with Retry
    val currentError = errorMessage
    if (currentError != null) {
        AlertDialog(
            onDismissRequest = { errorMessage = null; lastFailedDraft = null },
            title = { Text("Ошибка отправки") },
            text = { Text(currentError) },
            confirmButton = {
                val retryDraft = lastFailedDraft
                if (retryDraft != null) {
                    TextButton(onClick = {
                        errorMessage = null
                        doSend(retryDraft)
                    }) { Text("Повторить") }
                }
            },
            dismissButton = {
                TextButton(onClick = { errorMessage = null; lastFailedDraft = null }) {
                    Text("Закрыть")
                }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .navigationBarsPadding()
            .verticalScroll(detailsScrollState)
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        headerContent(item)

        SectionTitle(historyTitle)

        if (internalMessages.isEmpty()) {
            Text(
                text = historyEmptyText,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 4.dp)
            )
        } else {
            if (internalMessages.size > 10) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .border(
                            width = 1.dp,
                            color = MaterialTheme.colorScheme.outlineVariant,
                            shape = RoundedCornerShape(8.dp)
                        )
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = historyPagerDescription(showAllMessages, internalMessages.size),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    TextButton(
                        onClick = { setShowAllMessages(!showAllMessages) },
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Text(if (showAllMessages) "Скрыть часть" else "Показать все")
                    }
                }
            }

            val visibleMessages = if (showAllMessages || internalMessages.size <= 10) {
                internalMessages
            } else {
                internalMessages.takeLast(10)
            }

            visibleMessages.forEach { msg ->
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        msg.author.takeIf { it.isNotBlank() }?.let {
                            StatusBadge(
                                state = it,
                                defaultStyle = StatusStyle(
                                    background = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
                                    foreground = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.9f)
                                )
                            )
                        }
                        Spacer(Modifier.width(8.dp))
                        msg.date.takeIf { it.isNotBlank() }?.let {
                            Text(
                                text = it,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    msg.text.takeIf { it.isNotBlank() }?.let {
                        TextCLinkPreview(
                            text = it,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                    HorizontalDivider(Modifier.fillMaxWidth())
                }
            }
        }

        Spacer(Modifier.height(8.dp))
        HorizontalDivider()

        if (composerVisible) {
            SectionTitle(addCommentTitle)
            LimitedOutlinedTextField(
                value = messageDraft,
                onValueChange = { newValue ->
                    messageDraft = newValue
                    onDraftChanged(newValue)
                    upsertDraftForGuid(guid, newValue)
                },
                maxChars = 500,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp),
                placeholder = composerPlaceholder
            )

            Spacer(Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = onBack,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Закрыть")
                }

                Button(
                    onClick = {
                        val trimmed = messageDraft.trim()
                        if (trimmed.isNotEmpty()) {
                            doSend(trimmed)
                        }
                    },
                    modifier = Modifier.weight(1f),
                    enabled = isSendEnabled(messageDraft, item) && !isSendingMessage
                ) {
                    Text("Отправить")
                }
            }
        } else {
            Spacer(Modifier.height(8.dp))
            OutlinedButton(
                onClick = onBack,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Закрыть")
            }
        }

        Spacer(Modifier.height(16.dp))
    }
}


@Composable
fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(top = 6.dp, bottom = 4.dp)
    )
}
