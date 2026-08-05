package com.tagaev.trrcrm.ui.settings

import com.tagaev.trrcrm.ui.i18n.s

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
import com.tagaev.trrcrm.ui.root.LocalAppSnackbar
import compose.icons.FeatherIcons
import compose.icons.feathericons.ArrowLeft

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CameraFixatorScreen(
    onBack: () -> Unit,
) {
    var documentNumber by rememberSaveable { mutableStateOf("") }
    val showSnackbar = LocalAppSnackbar.current
    val isDocumentNumberValid = isValidDocumentNumber(documentNumber)

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
                    // This service screen has only a number, not the document creation date.
                    // Uploading from it would force a guessed period, which is forbidden.
                    showSnackbar(s("upload_document_date_missing"))
                },
                enabled = isDocumentNumberValid,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(s("settings_otkryt_kameru"))
            }
        }
    }
}
