package com.tagaev.mobileagregatcrm.ui.login

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
//import com.tagaev.mobileagregatcrm.composeapp.generated.resources.Res
//import com.tagaev.mobileagregatcrm.composeapp.generated.resources.botlogo
import com.tagaev.mobileagregatcrm.ui.login.ILoginComponent
import mobileagregatcrm.composeapp.generated.resources.Res
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.painterResource
import org.koin.compose.koinInject
import com.tagaev.mobileagregatcrm.data.AppSettings
import com.tagaev.mobileagregatcrm.utils.CONST
import mobileagregatcrm.composeapp.generated.resources.botlogo

import androidx.compose.runtime.collectAsState

/**
 * Login screen with two modes:
 * 1) Login + Password
 * 2) Token only
 *
 * Token is persisted via AppSettings (key: "API_TOKEN") and restored on open.
 *
 * Expected component API:
 *   - component.onLoginWithCredentials(user, pass)
 *   - component.onLoginWithToken(token)
 */
private enum class Mode { Credentials, Token }

@OptIn(ExperimentalResourceApi::class)
@Composable
private fun ImageWrapperLogo() {
    // Centered logo that respects theming container
    androidx.compose.foundation.Image(
        painter = painterResource(Res.drawable.botlogo),
        contentDescription = null,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp)
            //.size(48.dp) // inside the 72dp container
    )
}

@Composable
fun LoginScreen(component: ILoginComponent) {
    val appSettings = koinInject<AppSettings>()

    // Observe LoginComponent state (Idle / Loading / Error)
    val uiState by component.uiState.collectAsState()
    var showErrorDialog by rememberSaveable { mutableStateOf(false) }
    var currentError by remember { mutableStateOf("") }

    // When component reports an error, open dialog once
    LaunchedEffect(uiState) {
        if (uiState is LoginUiState.Error) {
            currentError = (uiState as LoginUiState.Error).message
            showErrorDialog = true
        }
    }

    var mode by rememberSaveable { mutableStateOf(Mode.Credentials) }
//    var user by rememberSaveable { mutableStateOf("kolosov.a.a@my.agregatka.ru") }
//    var pass by rememberSaveable { mutableStateOf("NfKqZXzSyew)") }

//    var user by rememberSaveable { mutableStateOf("malikov.a.d@teach.agregatka.ru") }
//    var pass by rememberSaveable { mutableStateOf("D7Wq39EoA7__") }

    var user by rememberSaveable { mutableStateOf("bannikov.v.e@teach.agregatka.ru") }
    var pass by rememberSaveable { mutableStateOf("7YO8sNvvHA4(") }

//    var user by rememberSaveable { mutableStateOf("") }
//    var pass by rememberSaveable { mutableStateOf("") }

    // Persisted token
    var token by rememberSaveable {
        mutableStateOf("")
//        mutableStateOf(appSettings.getString("API_TOKEN", ""))
    }

    // TextField theme colors (ensures readability in dark mode)
    val tfColors = OutlinedTextFieldDefaults.colors(
        focusedTextColor = MaterialTheme.colorScheme.onSurface,
        unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
        disabledTextColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
        errorTextColor = MaterialTheme.colorScheme.onError,

        focusedContainerColor = MaterialTheme.colorScheme.surface,
        unfocusedContainerColor = MaterialTheme.colorScheme.surface,
        disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
        errorContainerColor = MaterialTheme.colorScheme.surface,

        cursorColor = MaterialTheme.colorScheme.primary,
        errorCursorColor = MaterialTheme.colorScheme.error,

        focusedBorderColor = MaterialTheme.colorScheme.primary,
        unfocusedBorderColor = MaterialTheme.colorScheme.outline,
        disabledBorderColor = MaterialTheme.colorScheme.outlineVariant,
        errorBorderColor = MaterialTheme.colorScheme.error,

        focusedLabelColor = MaterialTheme.colorScheme.primary,
        unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
        disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
        errorLabelColor = MaterialTheme.colorScheme.error,

        focusedPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant,
        unfocusedPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant,
        disabledPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
        errorPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant
    )

    val canLogin by derivedStateOf {
        when (mode) {
            Mode.Credentials -> user.isNotBlank() && pass.isNotBlank()
            Mode.Token -> token.isNotBlank()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(24.dp))

        ImageWrapperLogo()

        Spacer(Modifier.height(16.dp))

        // Title
        ShimmerTitle(
            text = "AgrApp CRM",
        )

        Spacer(Modifier.height(24.dp))

        // Mode selector (tabs)
//        TabRow(selectedTabIndex = if (mode == Mode.Credentials) 0 else 1) {
//            Tab(
//                selected = mode == Mode.Credentials,
//                onClick = { mode = Mode.Credentials },
//                text = { Text("Логин и Пароль") }
//            )
//            Tab(
//                selected = mode == Mode.Token,
//                onClick = { mode = Mode.Token },
//                text = { Text("API Токен") }
//            )
//        }

        Spacer(Modifier.height(20.dp))

        when (mode) {
            Mode.Credentials -> {
                OutlinedTextField(
                    value = user,
                    onValueChange = { user = it },
                    label = { Text("Логин") },
                    singleLine = true,
                    colors = tfColors,
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(
                        imeAction = ImeAction.Next,
                        keyboardType = KeyboardType.Email
                    )
                )
                Spacer(Modifier.height(12.dp))
                var passVisible by rememberSaveable { mutableStateOf(false) }
                OutlinedTextField(
                    value = pass,
                    onValueChange = { pass = it },
                    label = { Text("Пароль") },
                    singleLine = true,
                    colors = tfColors,
                    modifier = Modifier.fillMaxWidth(),
                    visualTransformation = if (passVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        val label = if (passVisible) "Скрыть" else "Показать"
                        TextButton(onClick = { passVisible = !passVisible }) { Text(label) }
                    },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done, keyboardType = KeyboardType.Password),
                    keyboardActions = KeyboardActions(onDone = {
                        if (canLogin) component.onLoginWithCredentials(user.trim(), pass)
                    })
                )
            }
            Mode.Token -> {
                OutlinedTextField(
                    value = token,
                    onValueChange = { value ->
                        token = value
                        // Persist on every change so it is restored next time
                        appSettings.setString("API_TOKEN", token)
                    },
                    label = { Text("API Token") },
                    placeholder = { Text("Paste your token here") },
                    singleLine = true,
                    colors = tfColors,
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = {
                        if (canLogin) component.onLoginWithToken(token.trim())
                    })
                )
            }
        }

        Spacer(Modifier.height(20.dp))

        Button(
            onClick = {
                when (mode) {
                    Mode.Credentials -> component.onLoginWithCredentials(user.trim(), pass.trim())
                    Mode.Token -> component.onLoginWithToken(token.trim())
                }
            },
            enabled = canLogin && uiState !is LoginUiState.Loading,
            modifier = Modifier.fillMaxWidth().height(48.dp)
        ) {
            Text("Войти", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
        }

        Spacer(Modifier.height(24.dp))

        // Footer: version line
        //val version = remember { appSettings.getString("APP_VERSION", "—") }
        Text(
            text = "Версия: ${CONST.VERSION}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }

    if (uiState is LoginUiState.Loading) {
        AlertDialog(
            onDismissRequest = { /* block dismiss during loading */ },
            confirmButton = {},
            title = { Text("Вход") },
            text = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(strokeWidth = 2.dp, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(12.dp))
                    Text("Проверка данных…")
                }
            }
        )
    }

    if (showErrorDialog && uiState is LoginUiState.Error) {
        AlertDialog(
            onDismissRequest = { showErrorDialog = false },
            confirmButton = {
                TextButton(onClick = { showErrorDialog = false }) { Text("OK") }
            },
            title = { Text("Ошибка входа") },
            text = { Text(currentError) }
        )
    }
}


@Composable
private fun ShimmerTitle(
    text: String,
    durationMillis: Int = 4000,
) {
    var widthPx by remember { mutableStateOf(0f) }
    var heightPx by remember { mutableStateOf(0f) }

    // Band size depends on measured width (min 80px). Using remember to avoid churn.
    val band by remember(widthPx) { mutableStateOf(maxOf(widthPx * 0.25f, 80f)) }

    // Use Animatable so we can precisely control the sweep from -band to width+band.
    val offsetX = remember { Animatable(0f) }

    LaunchedEffect(widthPx, band, durationMillis) {
        if (widthPx <= 0f) return@LaunchedEffect
        while (true) {
            // Start just before the left edge
            offsetX.snapTo(-band)
            // Sweep past the right edge
            offsetX.animateTo(
                targetValue = widthPx + band,
                animationSpec = tween(durationMillis = durationMillis, easing = LinearEasing)
            )
        }
    }

    val brush = Brush.linearGradient(
        colors = listOf(
            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.25f),
            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.90f),
            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.25f)
        ),
        // Keep a diagonal streak that fully traverses the measured text bounds
        start = Offset(x = offsetX.value - band, y = 0f),
        end   = Offset(x = offsetX.value + band, y = heightPx)
    )

    Text(
        text = buildAnnotatedString {
            withStyle(SpanStyle(brush = brush)) { append(text) }
        },
        style = MaterialTheme.typography.headlineMedium,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.onGloballyPositioned { c ->
            widthPx = c.size.width.toFloat()
            heightPx = c.size.height.toFloat()
        }
    )
}
