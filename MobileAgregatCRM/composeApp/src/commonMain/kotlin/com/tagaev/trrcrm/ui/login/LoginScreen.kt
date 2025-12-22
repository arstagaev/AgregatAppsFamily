package com.tagaev.trrcrm.ui.login

import InfiniteImageTrain
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
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
import mobileagregatcrm.composeapp.generated.resources.Res
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.painterResource
import org.koin.compose.koinInject
import com.tagaev.trrcrm.data.AppSettings
import mobileagregatcrm.composeapp.generated.resources.botlogo

import androidx.compose.runtime.collectAsState
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import com.tagaev.secrets.Secrets
import com.tagaev.trrcrm.ui.custom.ScreenWithDismissableKeyboard
import com.tagaev.trrcrm.utils.anonim
import mobileagregatcrm.composeapp.generated.resources.car1
import mobileagregatcrm.composeapp.generated.resources.car2
import mobileagregatcrm.composeapp.generated.resources.car3
import mobileagregatcrm.composeapp.generated.resources.car4
import mobileagregatcrm.composeapp.generated.resources.car5
import mobileagregatcrm.composeapp.generated.resources.car6left
import mobileagregatcrm.composeapp.generated.resources.car7left
import mobileagregatcrm.composeapp.generated.resources.car8right
import mobileagregatcrm.composeapp.generated.resources.carv

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

private const val SPLASH_HOLD_MS: Long = 900L

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
    var showErrorDialog by rememberSaveable { mutableStateOf(true) }
    var currentError by remember { mutableStateOf("") }

    var keepSplash by rememberSaveable { mutableStateOf(true) }

    // Keep the loading splash on screen a bit longer when leaving Login,
    // so the forms never flash during navigation to MainList.
    LaunchedEffect(uiState) {
        when (uiState) {
            is LoginUiState.Loading -> keepSplash = true
            is LoginUiState.Error -> {
                keepSplash = false
                val e = uiState as LoginUiState.Error
                showErrorDialog = true
                currentError = e.message
            }
            else -> {
                // Navigation / success / idle — hold splash briefly
                keepSplash = true
                kotlinx.coroutines.delay(SPLASH_HOLD_MS)
                // Only drop splash if we haven't gone back to Loading in the meantime
                if (uiState !is LoginUiState.Loading) keepSplash = false
            }
        }
    }

    var mode by rememberSaveable { mutableStateOf(Mode.Credentials) }

//    var user by rememberSaveable { mutableStateOf("kolosov.a.a@my.agregatka.ru") }
//    var pass by rememberSaveable { mutableStateOf("NfKqZXzSyew)") }

//    var user by rememberSaveable { mutableStateOf("malikov.a.d@teach.agregatka.ru") }
//    var pass by rememberSaveable { mutableStateOf("D7Wq39EoA7__") }

//    var user by rememberSaveable { mutableStateOf("bannikov.v.e@teach.agregatka.ru") }
//    var pass by rememberSaveable { mutableStateOf("7YO8sNvvHA4(") }

//    var user by rememberSaveable { mutableStateOf("tagaev.a.i@my.agregatka.ru") }
//    var pass by rememberSaveable { mutableStateOf("eVpfmkGAHWr%") }

    var user by rememberSaveable { mutableStateOf("") }
    var pass by rememberSaveable { mutableStateOf("") }

    //tagaev.r
    //Пароль:
    //c3BzvPjgesW@

//    var user by rememberSaveable { mutableStateOf("tagaev.r") }
//    var pass by rememberSaveable { mutableStateOf("c3BzvPjgesW@") }

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

    val showSplash = keepSplash || (uiState is LoginUiState.Loading)

    if (!showSplash) {
        ScreenWithDismissableKeyboard {
            Box(
                modifier = Modifier
                    .fillMaxSize()
            ) {
                // Fullscreen background image
                Image(
                    painter = painterResource(Res.drawable.carv),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )

                // Optional dark scrim to keep text readable
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Black.copy(alpha = 0.25f),
                                    Color.Black.copy(alpha = 0.99f)
                                )
                            )
                        )
                )

                // Foreground content (forms, version, etc.)
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(vertical = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Spacer(Modifier.height(16.dp))

                    Spacer(Modifier.height(24.dp))

                    Spacer(Modifier.height(20.dp))
                    Column(Modifier.padding(horizontal = 24.dp)) {
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
                                    keyboardOptions = KeyboardOptions(
                                        imeAction = ImeAction.Done,
                                        keyboardType = KeyboardType.Password
                                    ),
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
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                        ) {
                            Text("Войти", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }

                    Column(Modifier.padding(horizontal = 24.dp)) {
                        Text(
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Center,
                            text = "Версия: ${Secrets.VERSION}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                if (showErrorDialog && uiState is LoginUiState.Error) {
                    AlertDialog(
                        onDismissRequest = { showErrorDialog = false },
                        confirmButton = {
                            TextButton(onClick = { showErrorDialog = false }) { Text("OK") }
                        },
                        title = { Text("Ошибка входа") },
                        text = { Text(currentError.anonim().substringBefore('[') ?: "Код ошибки не известен") }
                    )
                }
            }
        }
    }

    if (showSplash) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black),
            contentAlignment = Alignment.Center
        ) {
            InfiniteImageTrain(
                modifier = Modifier.fillMaxWidth().align(Alignment.TopCenter),
                images = listOf(
                    Res.drawable.car4,
                    Res.drawable.car5,
                    Res.drawable.car3,
                    Res.drawable.car6left,
                    Res.drawable.car7left,
//                    Res.drawable,
                ),
                direction = TrainDirection.LeftToRight,
                rowHeight = 150.dp,
                speedPxPerSecond = 15f // slower/faster here
            )
            //TRR APP

            ShimmerTitle(
                text = "TRR APP",
            )

//            Image(
//                painter = painterResource(Res.drawable.botlogo),
//                contentDescription = null,
//                modifier = Modifier
//                    .fillMaxWidth(0.5f)
//                    .wrapContentHeight()
//            )

            InfiniteImageTrain(
                modifier = Modifier.fillMaxWidth().align(Alignment.BottomCenter),
                images = listOf(
                    Res.drawable.car1,
                    Res.drawable.car2,
                    Res.drawable.car8right,
//                    Res.drawable.car3,
                ),
                direction = TrainDirection.RightToLeft,
                rowHeight = 150.dp,
                speedPxPerSecond = 15f // slower/faster here
            )
        }
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
