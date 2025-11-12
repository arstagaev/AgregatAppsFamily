package com.tagaev.mobileagregatcrm.utils

import androidx.compose.runtime.*
import com.arkivanov.essenty.backhandler.BackCallback
import com.arkivanov.essenty.backhandler.BackHandler

@Composable
fun BackBlocker(
    backHandler: BackHandler,
    enabled: Boolean = true,
    onBack: () -> Unit = {} // default: consume and do nothing
) {
    val currentOnBack by rememberUpdatedState(onBack)
    val callback = remember { BackCallback(isEnabled = enabled) { currentOnBack() } }

    SideEffect { callback.isEnabled = enabled }
    DisposableEffect(backHandler) {
        backHandler.register(callback)
        onDispose { backHandler.unregister(callback) }
    }
}
