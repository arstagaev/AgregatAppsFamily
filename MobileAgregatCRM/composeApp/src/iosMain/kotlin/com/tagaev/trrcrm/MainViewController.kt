package com.tagaev.trrcrm

import androidx.compose.runtime.remember
import androidx.compose.ui.window.ComposeUIViewController
import com.arkivanov.decompose.DefaultComponentContext
import com.arkivanov.essenty.lifecycle.ApplicationLifecycle
import com.tagaev.trrcrm.di.initKoinIos
import com.tagaev.trrcrm.ui.root.AppRoot
import com.tagaev.trrcrm.ui.root.DefaultRootComponent
import com.tagaev.trrcrm.ui.root.IRootComponent
import platform.UIKit.UIViewController

fun MainViewController(): UIViewController = ComposeUIViewController {
    // Create the Decompose root component once per controller
    initKoinIos()
    val root: IRootComponent = remember {
        DefaultRootComponent(
            componentContext = DefaultComponentContext(ApplicationLifecycle())
        )
    }
    AppRoot(root)
}