package com.tagaev.trrcrm.ui.settings

import com.tagaev.trrcrm.ui.i18n.s

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.tagaev.trrcrm.data.featureflags.MobileFeatureFlagsStore
import com.tagaev.trrcrm.domain.isValidDocumentNumber
import com.tagaev.trrcrm.domain.normalizeDocumentNumber
import com.tagaev.trrcrm.models.ImageDocumentType
import com.tagaev.trrcrm.ui.camera.DocumentCameraScreen
import com.tagaev.trrcrm.ui.root.LocalAppSnackbar
import compose.icons.FeatherIcons
import compose.icons.feathericons.ArrowLeft
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CameraFixatorScreen(
    onBack: () -> Unit,
) {
    var documentNumber by rememberSaveable { mutableStateOf("") }
    var documentTypeName by rememberSaveable { mutableStateOf(ImageDocumentType.Complects.wireName) }
    var cameraOpen by rememberSaveable { mutableStateOf(false) }
    val featureFlags = koinInject<MobileFeatureFlagsStore>()
    val showSnackbar = LocalAppSnackbar.current
    val scope = rememberCoroutineScope()
    val documentType = ImageDocumentType.fromWireName(documentTypeName)
    val normalizedDocumentNumber = normalizeDocumentNumber(documentNumber)
    val isDocumentNumberValid = isValidDocumentNumber(documentNumber)

    if (cameraOpen && isDocumentNumberValid) {
        DocumentCameraScreen(
            documentNumber = normalizedDocumentNumber,
            title = s("settings_kamera_fiksator"),
            documentType = documentType,
            showUploadStatusBlock = true,
            onBack = { cameraOpen = false },
        )
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(s("settings_kamera_fiksator")) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(FeatherIcons.ArrowLeft, contentDescription = s("settings_nazad"))
                    }
                },
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(s("upload_document_type"))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                ImageDocumentType.entries.forEach { type ->
                    FilterChip(
                        selected = documentType == type,
                        onClick = { documentTypeName = type.wireName },
                        label = { Text(type.labelRu) },
                    )
                }
            }
            OutlinedTextField(
                modifier = Modifier.fillMaxWidth(),
                value = documentNumber,
                onValueChange = {
                    documentNumber = it.filter { ch -> ch.isDigit() }.take(12)
                },
                singleLine = true,
                label = { Text(s("settings_nomer_dokumenta")) },
                supportingText = {
                    Text(
                        if (documentNumber.isBlank() || isDocumentNumberValid) {
                            s("settings_612_tsifr_s_veduschimi_nulyami")
                        } else {
                            s("settings_vvedite_ot_6_do_12_tsifr")
                        }
                    )
                },
                isError = documentNumber.isNotBlank() && !isDocumentNumberValid,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            )
            Button(
                onClick = {
                    if (documentType == ImageDocumentType.Complects) {
                        cameraOpen = true
                    } else {
                        scope.launch {
                            val allowed = when (documentType) {
                                ImageDocumentType.Complects -> true
                                ImageDocumentType.WorkOrder ->
                                    featureFlags.isPhotosUploadWorkOrdersEtcEnabled()
                                ImageDocumentType.InnerOrder ->
                                    featureFlags.isPhotosInnerOrderEnabled()
                                ImageDocumentType.Event ->
                                    featureFlags.isPhotosEventsEnabled()
                                ImageDocumentType.Delivery ->
                                    featureFlags.isPhotosCargoEnabled()
                            }
                            if (allowed) {
                                cameraOpen = true
                            } else {
                                showSnackbar(s("error_zagruzka_nedostupna"))
                            }
                        }
                    }
                },
                enabled = isDocumentNumberValid,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(s("settings_otkryt_kameru"))
            }
        }
    }
}
