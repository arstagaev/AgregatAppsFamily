package com.tagaev.mobileagregatcrm.ui.qrscanner

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.tagaev.mobileagregatcrm.ui.permissions.CameraPermissionGate
import com.tagaev.mobileagregatcrm.ui.permissions.CameraView
import compose.icons.FeatherIcons
import compose.icons.feathericons.CheckCircle
import compose.icons.feathericons.AlertCircle
import compose.icons.feathericons.Trash
import compose.icons.feathericons.Zap
import compose.icons.feathericons.ZapOff
import androidx.compose.foundation.clickable
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import compose.icons.feathericons.Copy
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QRScannerScreen(component: IQRScannerComponent) {
    val state by component.state.collectAsState()

    var openGallery by remember { mutableStateOf(false) }
    var selectedAttempt by remember { mutableStateOf<QRAttempt?>(null) }

    // Success dialog
    var showSuccess by remember { mutableStateOf(false) }
    LaunchedEffect(state.lastSuccess) {
        if (state.lastSuccess != null) showSuccess = true
    }

    // Error snackbar (optional)
    val snackbar = remember { SnackbarHostState() }
    LaunchedEffect(state.lastError) {
        state.lastError?.let { snackbar.showSnackbar(it) }
    }

    DisposableEffect(Unit) {
        onDispose {
            state.lastSuccess = null
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("QR-сканер") },
                actions = {
//                    IconButton(onClick = { component.toggleFlash() }) {
//                        Icon(
//                            imageVector = if (state.flashlight) FeatherIcons.Zap else FeatherIcons.ZapOff,
//                            contentDescription = if (state.flashlight) "Flash on" else "Flash off"
//                        )
//                    }
//                    IconButton(onClick = { component.clearHistory() }) {
//                        Icon(FeatherIcons.Trash, contentDescription = "Clear")
//                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbar) }
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding)
        ) {
            // TOP: camera scanner
            Box(
                Modifier
                    .fillMaxWidth()
                    .weight(0.58f) // ~58% height for camera
                    .background(Color.Black)
            ) {
                CameraPermissionGate(rationaleText = "Для сканирования нужен доступ к камере.") {
                    CameraView() { decodedString ->
                        component.onScanned(decodedString)
                    }
//                    Scanner(onScanned = {
//                        println(it);
//                        component.onScanned(it)
//                            true
//                        }, types = listOf(CodeType.QR))
//                    QrScanner(
//                        modifier = Modifier.fillMaxSize(),
//                        flashlightOn = state.flashlight,
//                        cameraLens = CameraLens.Back,
//                        openImagePicker = openGallery,
//                        imagePickerHandler = { openGallery = it },
//                        overlayShape = OverlayShape.Square,
//                        overlayColor = Color(0x88000000),
//                        overlayBorderColor = Color.White,
//                        zoomLevel = 1f,§§§
//                        maxZoomLevel = 3f,
//                        onCompletion = { text ->
//                            println("QR: ${text}")
//                            component.onScanned(text)
//                                       },
//                        onFailure = { err ->
//                            println("ERROR SCAN ${err}")
//
//                        /* optional: snackbar */ }
//                    )
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
                items(state.attempts.reversed(), key = { it.id }) { a ->
                    AttemptRow(a, onClick = { selectedAttempt = it })
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
                        CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                        Spacer(Modifier.width(12.dp))
                        Text("Получение данных по QR…")
                    }
                }
            )
        }

        // Success dialog with TRS details
        if (showSuccess && state.lastSuccess != null) {
            val r = state.lastSuccess!!
            AlertDialog(
                onDismissRequest = { showSuccess = false },
                confirmButton = {
                    TextButton(onClick = { showSuccess = false }) { Text("OK") }
                },
                title = { Text("Данные TRS") },
                text = {
                    Column(Modifier.fillMaxWidth()) {
                        Info("Гарантийный номер", r.warrantyNumber)
                        Info("Подразделение", r.department)
                        Info("Номенклатура", r.nomenclature)
                        Info("Статус", r.status)
                        Info("Срок гарантии", r.warrantyPeriod)
                        Info("Гравер", r.graver)
                        Info("Комплектация", r.completion)
                        Info("Компл. №", r.completionNumber)
                        Info("Компл. дата", r.completionDate)
                        Info("Дата", r.date)
                        Info("Комментарий", r.comment)
                        Info("Характеристика", r.characteristicNomenclature)
                    }
                }
            )
        }

        // Details dialog for a tapped attempt (with per-field copy buttons)
        if (selectedAttempt != null) {
            val attempt = selectedAttempt!!
            val clipboard = LocalClipboardManager.current
            val scope = rememberCoroutineScope()

            AlertDialog(
                onDismissRequest = { selectedAttempt = null },
                confirmButton = {
                    TextButton(onClick = { selectedAttempt = null }) { Text("OK") }
                },
                title = { Text("Детали попытки") },
                text = {
                    Column(Modifier.fillMaxWidth()) {
//                        InfoCopy(label = "Сырое значение", value = attempt.rawText, clipboard = clipboard, snackbar = snackbar)
                        when (attempt.status) {
                            AttemptStatus.Success -> {
                                val r = attempt.response
                                if (r != null) {
                                    InfoCopy("Гарантийный номер", r.warrantyNumber, clipboard, snackbar)
                                    InfoCopy("Подразделение", r.department, clipboard, snackbar)
                                    InfoCopy("Номенклатура", r.nomenclature, clipboard, snackbar)
                                    InfoCopy("Статус", r.status, clipboard, snackbar)
                                    InfoCopy("Срок гарантии", r.warrantyPeriod, clipboard, snackbar)
                                    InfoCopy("Гравер", r.graver, clipboard, snackbar)
                                    InfoCopy("Комплектация", r.completion, clipboard, snackbar)
                                    InfoCopy("Компл. №", r.completionNumber, clipboard, snackbar)
                                    InfoCopy("Компл. дата", r.completionDate, clipboard, snackbar)
                                    InfoCopy("Дата", r.date, clipboard, snackbar)
                                    InfoCopy("Комментарий", r.comment, clipboard, snackbar)
                                    InfoCopy("Характеристика", r.characteristicNomenclature, clipboard, snackbar)
                                }
                            }
                            AttemptStatus.Error -> {
                                InfoCopy(label = "Ошибка", value = attempt.error?.takeLast(10) ?: "Неизвестная ошибка", clipboard = clipboard, snackbar = snackbar)
                            }
                            AttemptStatus.Loading -> {
                                Text("Запрос выполняется…")
                            }
                        }
                    }
                }
            )
        }
    }
}

@Composable private fun AttemptRow(a: QRAttempt, onClick: (QRAttempt) -> Unit) {
    ListItem(
        modifier = Modifier.clickable { onClick(a) },
        headlineContent = {
            Text(
                when (a.status) {
                    AttemptStatus.Loading -> "Запрос…"
                    AttemptStatus.Success -> a.response?.nomenclature ?: "Успех"
                    AttemptStatus.Error -> "Ошибка сканирования" // a.error ?:
                }
            )
        },
        supportingContent = { Text(a.rawText.substringAfter("code=")) },
        trailingContent = {
            when (a.status) {
                AttemptStatus.Loading -> CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
                AttemptStatus.Success -> Icon(FeatherIcons.CheckCircle, contentDescription = null)
                AttemptStatus.Error -> Icon(FeatherIcons.AlertCircle, contentDescription = null)
            }
        }
    )
    Divider()
}

@Composable private fun Info(label: String, value: String) {
    if (value.isBlank()) return
    Row(Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
        Text("$label: ", style = MaterialTheme.typography.bodyMedium)
        Text(value, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable private fun InfoCopy(
    label: String,
    value: String,
    clipboard: androidx.compose.ui.platform.ClipboardManager,
    snackbar: SnackbarHostState
) {
    if (value.isBlank()) return
    val scope = rememberCoroutineScope()
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
            scope.launch { snackbar.showSnackbar("Скопировано: $label") }
        }) {
            Icon(FeatherIcons.Copy, contentDescription = "Копировать $label")
        }
    }
}