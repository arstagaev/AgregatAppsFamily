package com.tagaev.trrcrm.ui.master_screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.tagaev.trrcrm.domain.messages.findDraftForGuid
import com.tagaev.trrcrm.domain.messages.removeDraftIfMatches
import com.tagaev.trrcrm.domain.messages.upsertDraftForGuid
import com.tagaev.trrcrm.ui.custom.TextC
import com.tagaev.trrcrm.ui.custom.TextCLinkPreview
import com.tagaev.trrcrm.ui.master_screen.models.MessageModel
import com.tagaev.trrcrm.ui.work_order.LimitedOutlinedTextField


@Composable
fun <T> DetailsWithMessagesSheet(
    item: T,
    guid: String,
    messages: List<MessageModel>,
    onBack: () -> Unit,
    onSendMessage: (String, (Boolean) -> Unit) -> Unit,

    lastSendError: String? = null,
    onErrorDismiss: () -> Unit = {},
    initialDraft: String? = null,
    onDraftChanged: (String) -> Unit = {},
    isSendEnabled: (draft: String, item: T) -> Boolean = { draft, _ -> draft.isNotBlank() },
    headerContent: @Composable (ColumnScope.(T) -> Unit),
) {
    val scrollState = rememberScrollState()
    var messageDraft by remember(guid, initialDraft) {
        val base = when {
            !initialDraft.isNullOrEmpty() -> initialDraft
            else -> findDraftForGuid(guid)
        }
        mutableStateOf(base.orEmpty())
    }
    var isSendingMessage: Boolean = false
    var error = remember { mutableStateOf(lastSendError) }
    var internalMessages = messages.toMutableStateList()

    // When server messages contain our draft text, consider it delivered and clear draft
//    LaunchedEffect(guid, messages) {
//        val draft = messageDraft.trim()
//        if (draft.isEmpty()) return@LaunchedEffect
//
//        val hasSameComment = messages.any { msg ->
//            messageComment(msg)?.trim() == draft
//        }
//
//        if (hasSameComment) {
//            removeDraftIfMatches(guid = guid, message = messageDraft)
//            messageDraft = ""
//            onDraftChanged("")
//        }
//    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .verticalScroll(scrollState)
    ) {
        // Loading dialog while sending
        if (isSendingMessage) {
            AlertDialog(
                onDismissRequest = {}, // uncancellable while sending
                title = { Text("Отправка комментария") },
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

        // Error dialog after failed send
        if (!error.value.isNullOrEmpty()) {
            AlertDialog(
                onDismissRequest = onErrorDismiss,
                title = { Text("Ошибка") },
                text = { Text(error.value ?: "") },
                confirmButton = {
                    TextButton(onClick = {
                        error.value = ""
                    }) {
                        Text("OK")
                    }
                }
            )
        }

        // Custom header/content for specific item type
        headerContent(item)

        // Messages section (shared across screens)
        if (messages.isNotEmpty()) {
            SectionTitle("Комментарии:")
            messages.forEach { msg ->
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        (msg.author)?.takeIf { it.isNotBlank() }?.let {
                            Text(
                                text = it,
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(Modifier.width(8.dp))
                        (msg.date)?.takeIf { it.isNotBlank() }?.let {
                            Text(
                                text = it,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    (msg.text)?.takeIf { it.isNotBlank() }?.let {
                        TextCLinkPreview(
                            text = it,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                    HorizontalDivider(Modifier.fillMaxWidth())
                }
            }
        }

        SectionTitle("Добавить комментарий")
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
                .heightIn(min = 80.dp),
            placeholder = "Комментарий по работам…"
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
                        isSendingMessage = true


                        onSendMessage(trimmed) { ok ->
                            internalMessages.add(MessageModel(
                                author = "я",
                                text = trimmed
                            ))

                            if (ok) {
                                println(">>>> Message sent")
                                removeDraftIfMatches(guid = guid, message = messageDraft)
                                messageDraft = ""
                                onDraftChanged("")
                                error.value = ""
                                // locally clear text, append to messages, etc.
                            } else {
                                println(">>>> Message NOT sent")
                                error.value = "Ошибка отправки сообщения"
                                // show error UI inside sheet
                            }
                        }
                    }
                },
                modifier = Modifier.weight(1f),
                enabled = isSendEnabled(messageDraft, item)
            ) {
                Text("Отправить")
            }
        }
        Spacer(Modifier.height(12.dp))
    }
}


// same file: WorkOrdersScreen.kt
@Composable
fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(top = 6.dp, bottom = 4.dp)
    )
}
