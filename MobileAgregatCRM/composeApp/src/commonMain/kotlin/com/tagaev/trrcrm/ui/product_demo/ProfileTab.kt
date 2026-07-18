package com.tagaev.trrcrm.ui.product_demo

import com.tagaev.trrcrm.ui.i18n.s

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp

@Composable
fun ProfileTab(
    profile: DemoProfileState,
    onLoginChanged: (String) -> Unit,
    onPasswordChanged: (String) -> Unit,
    onLogin: (String, String) -> Unit,
    onLogout: () -> Unit,
    onOpenSignUp: () -> Unit,
    onOpenCrm: () -> Unit,
    onResetLocalProfile: () -> Unit,
) {
    var passVisible by remember { mutableStateOf(false) }
    val canLogin = profile.login.trim().isNotBlank() && profile.password.isNotBlank()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .imePadding()
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text("Профиль", style = MaterialTheme.typography.headlineSmall)

        if (profile.isCrmLoggedIn) {
            OutlinedCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "Профиль CRM",
                        fontWeight = FontWeight.SemiBold,
                        style = MaterialTheme.typography.bodyLarge
                    )
                    if (profile.displayName.isNotBlank()) {
                        Text("Пользователь: ${profile.displayName}", style = MaterialTheme.typography.bodySmall)
                    }
                    if (profile.email.isNotBlank()) {
                        Text("Логин: ${profile.email}", style = MaterialTheme.typography.bodySmall)
                    }
                    Button(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = onOpenCrm
                    ) {
                        Text("Перейти в CRM")
                    }
                    OutlinedButton(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = onLogout
                    ) {
                        Text(s("settings_vyyti"))
                    }
                }
            }
        } else {
            Text(
                text = "Каталог и отправка заявки доступны без входа.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "Вход в CRM для зарегистрированных пользователей",
                style = MaterialTheme.typography.titleMedium
            )
            OutlinedTextField(
                modifier = Modifier.fillMaxWidth(),
                value = profile.login,
                onValueChange = onLoginChanged,
                singleLine = true,
                label = { Text(s("login_login")) }
            )
            OutlinedTextField(
                modifier = Modifier.fillMaxWidth(),
                value = profile.password,
                onValueChange = onPasswordChanged,
                singleLine = true,
                label = { Text(s("login_parol")) },
                visualTransformation = if (passVisible) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    val label = if (passVisible) s("login_skryt") else s("login_pokazat")
                    TextButton(onClick = { passVisible = !passVisible }) { Text(label) }
                },
                keyboardOptions = KeyboardOptions(
                    imeAction = ImeAction.Done,
                    keyboardType = KeyboardType.Password
                ),
                keyboardActions = KeyboardActions(onDone = {
                    if (canLogin) onLogin(profile.login.trim(), profile.password)
                })
            )
            Button(
                modifier = Modifier.fillMaxWidth(),
                onClick = { onLogin(profile.login.trim(), profile.password) },
                enabled = !profile.isLoginLoading
            ) {
                Text(if (profile.isLoginLoading) "Авторизация..." else s("login_voyti"))
            }
            if (!profile.loginError.isNullOrBlank()) {
                Text(
                    text = profile.loginError.orEmpty(),
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }
            OutlinedButton(
                modifier = Modifier.fillMaxWidth(),
                onClick = onOpenSignUp
            ) {
                Text(s("product_demo_net_akkaunta_zaprosit_dostup_k_crm"))
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
//        OutlinedButton(onClick = onResetLocalProfile) { Text("Сбросить локальные поля") }
    }
}
