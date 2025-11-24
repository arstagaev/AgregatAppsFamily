package com.tagaev.trrcrm

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.arkivanov.decompose.defaultComponentContext
import com.tagaev.trrcrm.ui.root.AppRoot
import com.tagaev.trrcrm.ui.root.DefaultRootComponent

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val root = DefaultRootComponent(
            componentContext = defaultComponentContext()
        )
        setContent {
            AppRoot(root)
        }
    }
}

@Preview
@Composable
fun AppAndroidPreview() {
    App()
}