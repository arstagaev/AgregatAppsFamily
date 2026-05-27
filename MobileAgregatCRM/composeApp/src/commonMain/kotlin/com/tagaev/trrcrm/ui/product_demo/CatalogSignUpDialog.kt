package com.tagaev.trrcrm.ui.product_demo

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun CatalogSignUpDialog(
    form: SignUpFormState,
    onDismiss: () -> Unit,
    onNameChanged: (String) -> Unit,
    onPhoneChanged: (String) -> Unit,
    onEmailChanged: (String) -> Unit,
    onSubmit: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Запрос доступа к CRM") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    modifier = Modifier.fillMaxWidth(),
                    value = form.name,
                    onValueChange = onNameChanged,
                    singleLine = true,
                    isError = !form.fieldErrors["name"].isNullOrBlank(),
                    label = { Text("Имя") },
                    supportingText = {
                        val text = form.fieldErrors["name"]
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
                    label = { Text("Эл. почта") },
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
            }
        },
        confirmButton = {
            Button(onClick = onSubmit, enabled = !form.isSubmitting) {
                Text(if (form.isSubmitting) "Отправляем..." else "Отправить запрос")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Отмена")
            }
        }
    )
}
