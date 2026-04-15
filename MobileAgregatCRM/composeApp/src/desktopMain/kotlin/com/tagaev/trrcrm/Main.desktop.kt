package com.tagaev.trrcrm

import androidx.compose.runtime.remember
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import com.arkivanov.decompose.DefaultComponentContext
import com.arkivanov.essenty.lifecycle.LifecycleRegistry
import com.tagaev.trrcrm.di.initKoinDesktop
import com.tagaev.trrcrm.ui.root.AppRoot
import com.tagaev.trrcrm.ui.root.DefaultRootComponent
import com.tagaev.trrcrm.ui.root.IRootComponent

fun main() = application {
    initKoinDesktop()

    Window(
        onCloseRequest = ::exitApplication,
        title = "MobileAgregatCRM"
    ) {
        val root: IRootComponent = remember {
            DefaultRootComponent(
                componentContext = DefaultComponentContext(LifecycleRegistry())
            )
        }
        AppRoot(root)
    }
}
