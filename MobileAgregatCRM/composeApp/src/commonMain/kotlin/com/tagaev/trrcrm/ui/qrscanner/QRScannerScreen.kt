package com.tagaev.trrcrm.ui.qrscanner

import com.tagaev.trrcrm.ui.i18n.s

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
import com.tagaev.trrcrm.getPlatform
import com.tagaev.trrcrm.data.remote.userFacingMessage
import com.tagaev.trrcrm.ui.permissions.CameraPermissionGate
import com.tagaev.trrcrm.ui.permissions.CameraView
import com.tagaev.trrcrm.utils.getTimestamp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QRScannerScreen(component: IQRScannerComponent) {
    val state by component.state.collectAsState()
    val isDesktopTarget = remember { getPlatform().name.startsWith("Desktop") }

    val snackbarHostState = remember { SnackbarHostState() }

    // Show error from state (like s("qr_nekorrektnyy_qr_kod"))
    LaunchedEffect(state.lastError) {
        state.lastError?.let { snackbarHostState.showSnackbar(userFacingMessage(it, it)) }
    }
    LaunchedEffect(state.openComplectationError) {
        state.openComplectationError?.let {
            snackbarHostState.showSnackbar(userFacingMessage(it, it))
            component.onOpenComplectationErrorShown()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(s("qr_qr_skaner")) },
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
                    if (isDesktopTarget) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = s("complectation_qr_skaner_poka_nedostupen_na_desktop"),
                                style = MaterialTheme.typography.bodyLarge,
                                color = Color.White
                            )
                        }
                    } else {
                        CameraPermissionGate(rationaleText = s("complectation_dlya_skanirovaniya_nuzhen_dostup_k_kamere")) {
                            CameraView(
                                decodedString = { decodedString ->
                                    component.onScanned(decodedString)
                                },
                                autoStart = false
                            )
                        }
                    }
                }

                // BOTTOM: attempts list
                Text(
                    s("qr_istoriya_skanirovaniya"),
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
                    title = { Text(s("qr_zagruzka")) },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp
                            )
                            Spacer(Modifier.width(12.dp))
                            Text(s("qr_poluchenie_dannyh_po_qr"))
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
                    isOpeningComplectation = state.isOpeningComplectation,
                    onOpenComplectation = { component.onOpenComplectationClicked() },
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
                    AttemptStatus.Loading -> s("qr_zapros")
                    AttemptStatus.Success -> attempt.response?.nomenclature ?: s("qr_uspeh")
                    AttemptStatus.Error -> s("qr_oshibka_skanirovaniya")
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
    isOpeningComplectation: Boolean,
    onOpenComplectation: () -> Unit,
    onDismiss: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val canOpenComplectation = attempt.status == AttemptStatus.Success &&
            !attempt.response?.completionNumber.isNullOrBlank()

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("OK") }
        },
        dismissButton = {
            if (canOpenComplectation) {
                TextButton(
                    onClick = onOpenComplectation,
                    enabled = !isOpeningComplectation
                ) {
                    if (isOpeningComplectation) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(12.dp),
                            strokeWidth = 2.dp
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(s("qr_otkrytie"))
                    } else {
                        Text(s("qr_otkryt_komplektatsiyu"))
                    }
                }
            }
        },
        title = {
            Text(
                when (attempt.status) {
                    AttemptStatus.Success -> s("qr_dannye_trs")
                    AttemptStatus.Error -> s("qr_oshibka_skanirovaniya")
                    AttemptStatus.Loading -> s("qr_zapros")
                }
            )
        },
        text = {
            LazyColumn(Modifier.fillMaxWidth()) {
                when (attempt.status) {
                    AttemptStatus.Success -> {
                        val r = attempt.response
                        if (r != null) {
                            item { InfoCopy(s("qr_garantiynyy_nomer"), r.warrantyNumber, clipboard, snackbarHostState, scope) }
                            item { InfoCopy(s("events_podrazdelenie"), r.department, clipboard, snackbarHostState, scope) }
                            item { InfoCopy(s("qr_nomenklatura"), r.nomenclature, clipboard, snackbarHostState, scope) }
                            item { InfoCopy(s("list_status"), r.status, clipboard, snackbarHostState, scope) }
                            item { InfoCopy(s("qr_srok_garantii"), r.warrantyPeriod, clipboard, snackbarHostState, scope) }
                            item { InfoCopy(s("qr_graver"), r.graver, clipboard, snackbarHostState, scope) }
                            item { InfoCopy(s("nav_komplektatsiya"), r.completion, clipboard, snackbarHostState, scope) }
                            item { InfoCopy(s("qr_kompl"), r.completionNumber, clipboard, snackbarHostState, scope) }
                            item { InfoCopy(s("qr_kompl_data"), r.completionDate, clipboard, snackbarHostState, scope) }
                            item { InfoCopy(s("filter_data"), r.date, clipboard, snackbarHostState, scope) }
                            item { InfoCopy(s("complectation_kommentariy"), r.comment, clipboard, snackbarHostState, scope) }
                            item { InfoCopy(s("qr_harakteristika"), r.characteristicNomenclature, clipboard, snackbarHostState, scope) }
                        } else {
                            item { Text(s("qr_net_dannyh_po_trs")) }
                        }
                    }

                    AttemptStatus.Error -> {
                        val shortError = attempt.error ?: s("main_neizvestnaya_oshibka")
                        item {
                            InfoCopy(
                                label = s("qr_oshibka"),
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
                                Text(s("qr_zapros_vypolnyaetsya"))
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
