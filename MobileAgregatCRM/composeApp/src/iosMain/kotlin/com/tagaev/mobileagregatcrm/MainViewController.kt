package com.tagaev.mobileagregatcrm

import androidx.compose.runtime.remember
import androidx.compose.ui.window.ComposeUIViewController
import com.arkivanov.decompose.DefaultComponentContext
import com.arkivanov.essenty.lifecycle.ApplicationLifecycle
import com.tagaev.mobileagregatcrm.ui.root.AppRoot
import com.tagaev.mobileagregatcrm.ui.root.DefaultRootComponent
import com.tagaev.mobileagregatcrm.ui.root.IRootComponent
import platform.UIKit.UIViewController

fun MainViewController(): UIViewController = ComposeUIViewController {
    // Create the Decompose root component once per controller
    val root: IRootComponent = remember {
        DefaultRootComponent(
            componentContext = DefaultComponentContext(ApplicationLifecycle())
        )
    }
    AppRoot(root)
}