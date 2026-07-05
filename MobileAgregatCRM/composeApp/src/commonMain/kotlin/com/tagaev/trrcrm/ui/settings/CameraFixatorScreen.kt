package com.tagaev.trrcrm.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.tagaev.trrcrm.domain.isValidDocumentNumber
import com.tagaev.trrcrm.domain.normalizeDocumentNumber
import com.tagaev.trrcrm.ui.camera.DocumentCameraScreen
import compose.icons.FeatherIcons
import compose.icons.feathericons.ArrowLeft

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CameraFixatorScreen(
    onBack: () -> Unit,
) {
    var documentNumber by rememberSaveable { mutableStateOf("") }
    var cameraOpen by rememberSaveable { mutableStateOf(false) }
    val normalizedDocumentNumber = normalizeDocumentNumber(documentNumber)
    val isDocumentNumberValid = isValidDocumentNumber(documentNumber)

    if (cameraOpen && isDocumentNumberValid) {
        DocumentCameraScreen(
            documentNumber = normalizedDocumentNumber,
            title = "Камера фиксатор",
            documentName = "Camera fixator",
            showUploadStatusBlock = true,
            onBack = { cameraOpen = false },
        )
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Камера фиксатор") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(FeatherIcons.ArrowLeft, contentDescription = "Назад")
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
            OutlinedTextField(
                modifier = Modifier.fillMaxWidth(),
                value = documentNumber,
                onValueChange = {
                    documentNumber = it.filter { ch -> ch.isDigit() }.take(12)
                },
                singleLine = true,
                label = { Text("Номер документа") },
                supportingText = {
                    Text(
                        if (documentNumber.isBlank() || isDocumentNumberValid) {
                            "6–12 цифр, с ведущими нулями"
                        } else {
                            "Введите от 6 до 12 цифр"
                        }
                    )
                },
                isError = documentNumber.isNotBlank() && !isDocumentNumberValid,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            )
            Button(
                onClick = { cameraOpen = true },
                enabled = isDocumentNumberValid,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Открыть камеру")
            }
        }
    }
}
