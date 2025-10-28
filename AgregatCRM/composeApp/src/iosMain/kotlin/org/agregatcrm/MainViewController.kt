package org.agregatcrm

import androidx.compose.runtime.remember
import androidx.compose.ui.window.ComposeUIViewController
import kotlinx.coroutines.MainScope
import org.agregatcrm.di.initKoinIos
import org.agregatcrm.domain.provideEventsController
import org.agregatcrm.ui.App

//fun MainViewController() = ComposeUIViewController { App() }
fun MainViewController() = ComposeUIViewController(
    configure = {
        initKoinIos()
    }
) {
    // A single scope tied to this VC

    val scope = remember { MainScope() }
    var controller = provideEventsController(scope)

    App(scope = scope, controller = controller)
}