package com.tagaev.trrcrm.ui.qrscanner

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ClipboardManager
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import compose.icons.FeatherIcons
import compose.icons.feathericons.AlertCircle
import compose.icons.feathericons.CheckCircle
import compose.icons.feathericons.Copy
import com.tagaev.trrcrm.ui.permissions.CameraPermissionGate
import com.tagaev.trrcrm.ui.permissions.CameraView
import com.tagaev.trrcrm.utils.getTimestamp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QRScannerScreen(component: IQRScannerComponent) {
    val state by component.state.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }

    // Show error from state (like "Некорректный QR-код")
    LaunchedEffect(state.lastError) {
        state.lastError?.let { snackbarHostState.showSnackbar(it) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("QR-сканер") },
                actions = {
                    // Optional: add flashlight toggle back when ready
                    // IconButton(onClick = { component.toggleFlash() }) { ... }
                    // IconButton(onClick = { component.clearHistory() }) { ... }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Box(
            Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            Column(Modifier.fillMaxSize()) {

                // TOP: camera scanner
                Box(
                    Modifier
                        .fillMaxWidth()
                        .weight(0.58f)
                        .background(Color.Black)
                ) {
                    CameraPermissionGate(rationaleText = "Для сканирования нужен доступ к камере.") {
                        CameraView { decodedString ->
                            component.onScanned(decodedString)
                        }
                    }
                }

                // BOTTOM: attempts list
                Text(
                    "История сканирования",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                )
                Divider()

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(0.42f)
                ) {
                    // IMPORTANT: use id as key (unique), not startedAt
                    items(state.attempts.reversed(), key = { it.id }) { attempt ->
                        AttemptRow(
                            attempt = attempt,
                            onClick = { component.onAttemptClicked(attempt) }
                        )
                    }
                    item { Spacer(Modifier.height(12.dp)) }
                }
            }

            // Loading dialog while repo is fetching
            if (state.isLoading) {
                AlertDialog(
                    onDismissRequest = { /* block close during loading */ },
                    confirmButton = {},
                    title = { Text("Загрузка") },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp
                            )
                            Spacer(Modifier.width(12.dp))
                            Text("Получение данных по QR…")
                        }
                    }
                )
            }

            // Dialog for selected attempt (from scan or history)
            state.selectedAttempt?.let { attempt ->
                val clipboard = LocalClipboardManager.current
                AttemptDetailsDialog(
                    attempt = attempt,
                    clipboard = clipboard,
                    snackbarHostState = snackbarHostState,
                    onDismiss = { component.onDialogDismissed() }
                )
            }
        }
    }
}

@Composable
private fun AttemptRow(
    attempt: QRAttempt,
    onClick: () -> Unit
) {
    ListItem(
        modifier = Modifier.clickable { onClick() },
        headlineContent = {
            Text(
                when (attempt.status) {
                    AttemptStatus.Loading -> "Запрос…"
                    AttemptStatus.Success -> attempt.response?.nomenclature ?: "Успех"
                    AttemptStatus.Error -> "Ошибка сканирования"
                }
            )
        },
        supportingContent = {
            Text(attempt.rawText.substringAfter("code=", attempt.rawText))
        },
        trailingContent = {
            when (attempt.status) {
                AttemptStatus.Loading -> CircularProgressIndicator(
                    Modifier.size(16.dp),
                    strokeWidth = 2.dp
                )

                AttemptStatus.Success -> Icon(
                    FeatherIcons.CheckCircle,
                    contentDescription = null
                )

                AttemptStatus.Error -> Icon(
                    FeatherIcons.AlertCircle,
                    contentDescription = null
                )
            }
        }
    )
    Divider()
}

@Composable
private fun AttemptDetailsDialog(
    attempt: QRAttempt,
    clipboard: ClipboardManager,
    snackbarHostState: SnackbarHostState,
    onDismiss: () -> Unit
) {
    val scope = rememberCoroutineScope()

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("OK") }
        },
        title = {
            Text(
                when (attempt.status) {
                    AttemptStatus.Success -> "Данные TRS"
                    AttemptStatus.Error -> "Ошибка сканирования"
                    AttemptStatus.Loading -> "Запрос…"
                }
            )
        },
        text = {
            LazyColumn(Modifier.fillMaxWidth()) {
                when (attempt.status) {
                    AttemptStatus.Success -> {
                        val r = attempt.response
                        if (r != null) {
                            item { InfoCopy("Гарантийный номер", r.warrantyNumber, clipboard, snackbarHostState, scope) }
                            item { InfoCopy("Подразделение", r.department, clipboard, snackbarHostState, scope) }
                            item { InfoCopy("Номенклатура", r.nomenclature, clipboard, snackbarHostState, scope) }
                            item { InfoCopy("Статус", r.status, clipboard, snackbarHostState, scope) }
                            item { InfoCopy("Срок гарантии", r.warrantyPeriod, clipboard, snackbarHostState, scope) }
                            item { InfoCopy("Гравер", r.graver, clipboard, snackbarHostState, scope) }
                            item { InfoCopy("Комплектация", r.completion, clipboard, snackbarHostState, scope) }
                            item { InfoCopy("Компл. №", r.completionNumber, clipboard, snackbarHostState, scope) }
                            item { InfoCopy("Компл. дата", r.completionDate, clipboard, snackbarHostState, scope) }
                            item { InfoCopy("Дата", r.date, clipboard, snackbarHostState, scope) }
                            item { InfoCopy("Комментарий", r.comment, clipboard, snackbarHostState, scope) }
                            item { InfoCopy("Характеристика", r.characteristicNomenclature, clipboard, snackbarHostState, scope) }
                        } else {
                            item { Text("Нет данных по TRS.") }
                        }
                    }

                    AttemptStatus.Error -> {
                        val shortError = attempt.error ?: "Неизвестная ошибка"
                        item {
                            InfoCopy(
                                label = "Ошибка",
                                value = shortError,
                                clipboard = clipboard,
                                snackbarHostState = snackbarHostState,
                                scope = scope
                            )
                        }
                    }

                    AttemptStatus.Loading -> {
                        item {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    strokeWidth = 2.dp
                                )
                                Spacer(Modifier.width(8.dp))
                                Text("Запрос выполняется…")
                            }
                        }
                    }
                }
            }
        }
    )
}

@Composable
private fun InfoCopy(
    label: String,
    value: String,
    clipboard: ClipboardManager,
    snackbarHostState: SnackbarHostState,
    scope: CoroutineScope
) {
    if (value.isBlank()) return
    Row(
        Modifier.fillMaxWidth().padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text("$label:", style = MaterialTheme.typography.bodyMedium)
            Text(value, style = MaterialTheme.typography.bodyMedium)
        }
        IconButton(onClick = {
            clipboard.setText(AnnotatedString(value))
            scope.launch { snackbarHostState.showSnackbar("Скопировано: $label") }
        }) {
            Icon(FeatherIcons.Copy, contentDescription = "Копировать $label")
        }
    }
}
