package com.tagaev.trrcrm

import androidx.compose.runtime.remember
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.ComposeViewport
import com.arkivanov.decompose.DefaultComponentContext
import com.arkivanov.essenty.lifecycle.LifecycleRegistry
import com.tagaev.trrcrm.di.initKoinWasm
import com.tagaev.trrcrm.ui.root.AppRoot
import com.tagaev.trrcrm.ui.root.DefaultRootComponent
import com.tagaev.trrcrm.ui.root.IRootComponent
import kotlinx.browser.document

@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    initKoinWasm()
    ComposeViewport(document.body!!) {
        val root: IRootComponent = remember {
            DefaultRootComponent(
                componentContext = DefaultComponentContext(LifecycleRegistry())
            )
        }
        AppRoot(root)
    }
}
