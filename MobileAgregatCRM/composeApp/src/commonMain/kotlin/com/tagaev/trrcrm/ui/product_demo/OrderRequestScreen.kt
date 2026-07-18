package com.tagaev.trrcrm.ui.product_demo

import com.tagaev.trrcrm.ui.i18n.s

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun OrderRequestScreen(
    form: OrderContactFormState,
    onBack: () -> Unit,
    onNameChanged: (String) -> Unit,
    onAddressChanged: (String) -> Unit,
    onCompanyChanged: (String) -> Unit,
    onPhoneChanged: (String) -> Unit,
    onEmailChanged: (String) -> Unit,
    onSubmit: () -> Unit,
) {
    Surface(
        modifier = Modifier
            .fillMaxSize()
            .imePadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            OutlinedButton(
                onClick = onBack,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(s("settings_nazad"))
            }

            Text(
                text = "Контакты для заявки",
                style = MaterialTheme.typography.headlineSmall
            )

            OutlinedTextField(
                modifier = Modifier.fillMaxWidth(),
                value = form.name,
                onValueChange = onNameChanged,
                singleLine = true,
                isError = !form.fieldErrors["name"].isNullOrBlank(),
                label = { Text("Имя *") },
                supportingText = {
                    val text = form.fieldErrors["name"]
                    if (!text.isNullOrBlank()) Text(text)
                }
            )
            OutlinedTextField(
                modifier = Modifier.fillMaxWidth(),
                value = form.address,
                onValueChange = onAddressChanged,
                singleLine = true,
                isError = !form.fieldErrors["address"].isNullOrBlank(),
                label = { Text("Адрес / город (опционально)") },
                supportingText = {
                    val text = form.fieldErrors["address"]
                    if (!text.isNullOrBlank()) Text(text)
                }
            )
            OutlinedTextField(
                modifier = Modifier.fillMaxWidth(),
                value = form.companyName,
                onValueChange = onCompanyChanged,
                singleLine = true,
                isError = !form.fieldErrors["company"].isNullOrBlank(),
                label = { Text("Компания (опционально)") },
                supportingText = {
                    val text = form.fieldErrors["company"]
                    if (!text.isNullOrBlank()) Text(text)
                }
            )
            OutlinedTextField(
                modifier = Modifier.fillMaxWidth(),
                value = form.phoneNumber,
                onValueChange = onPhoneChanged,
                singleLine = true,
                isError = !form.fieldErrors["phone"].isNullOrBlank(),
                label = { Text("Телефон (опционально)") },
                supportingText = {
                    val text = form.fieldErrors["phone"]
                    if (!text.isNullOrBlank()) Text(text)
                }
            )
            OutlinedTextField(
                modifier = Modifier.fillMaxWidth(),
                value = form.email,
                onValueChange = onEmailChanged,
                singleLine = true,
                isError = !form.fieldErrors["email"].isNullOrBlank(),
                label = { Text("Эл. почта *") },
                supportingText = {
                    val text = form.fieldErrors["email"]
                    if (!text.isNullOrBlank()) Text(text)
                }
            )

            if (!form.validationError.isNullOrBlank()) {
                Text(
                    text = form.validationError.orEmpty(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }

            Button(
                onClick = onSubmit,
                enabled = !form.isSubmitting,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (form.isSubmitting) "Отправляем..." else s("product_demo_otpravit_zayavku"))
            }
        }
    }
}

