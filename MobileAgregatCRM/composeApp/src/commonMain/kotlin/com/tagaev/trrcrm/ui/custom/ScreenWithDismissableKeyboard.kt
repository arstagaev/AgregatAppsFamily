package com.tagaev.trrcrm.ui.custom

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager

@Composable
fun ScreenWithDismissableKeyboard(
    content: @Composable () -> Unit
) {
    val focusManager = LocalFocusManager.current

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                // Do not steal text-field gestures (selection/copy).
                // Clear focus only on taps that were not consumed by children.
                awaitEachGesture {
                    val down = awaitFirstDown(pass = PointerEventPass.Final)
                    val up = waitForUpOrCancellation(pass = PointerEventPass.Final)
                    if (up != null && !down.isConsumed && !up.isConsumed) {
                        focusManager.clearFocus()
                    }
                }
            }
    ) {
        content()
    }
}
