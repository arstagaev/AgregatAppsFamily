package com.tagaev.trrcrm.ui.common

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex

class BusyActionGate {
    private val mutex = Mutex()
    var isBusy by mutableStateOf(false)
        private set

    suspend fun <T> run(block: suspend () -> T): T? {
        if (!mutex.tryLock()) return null
        isBusy = true
        return try {
            block()
        } finally {
            isBusy = false
            mutex.unlock()
        }
    }

    fun launch(scope: CoroutineScope, block: suspend () -> Unit) {
        if (isBusy) return
        scope.launch {
            run(block)
        }
    }
}

@Composable
fun rememberBusyActionGate(): BusyActionGate = remember { BusyActionGate() }
