package com.tagaev.trrcrm

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.annotation.RequiresApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.tooling.preview.Preview
import com.arkivanov.decompose.defaultComponentContext
import com.tagaev.trrcrm.ui.root.AppRoot
import com.tagaev.trrcrm.ui.root.DefaultRootComponent
import com.tagaev.trrcrm.ui.root.IRootComponent

class MainActivity : ComponentActivity() {
    private var root: IRootComponent? = null
    private val handler = Handler(Looper.getMainLooper())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        root = DefaultRootComponent(componentContext = defaultComponentContext())

        requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 4)

        setContent {
            AppRoot(root!!)
        }

        // Handle deep link from cold start - delay to ensure UI is initialized
        handler.post {
            handleIntent(intent)
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent) // Important: update the intent so getIntent() returns the latest
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        val screen = intent?.getStringExtra("screen")
        val rawDocId = intent?.getStringExtra("docId") ?: intent?.getStringExtra("doc_id")
        val docId = extractGuidOrTrim(rawDocId)
        println(">>> MainActivity.handleIntent: screen='$screen', docId='$docId' (raw='$rawDocId'), root=${root != null}")
        if (!screen.isNullOrBlank()) {
            if (root != null) {
                println(">>> MainActivity.handleIntent: Calling onDeepLink")
                root?.onDeepLink(screen, docId)
            } else {
                println(">>> MainActivity.handleIntent: root is null, cannot process deep link")
            }
        } else {
            println(">>> MainActivity.handleIntent: screen is null, ignoring")
        }
    }

    private fun extractGuidOrTrim(value: String?): String? {
        if (value.isNullOrBlank()) return null
        val trimmed = value.trim()
        val match = GUID_REGEX.find(trimmed)
        return match?.value ?: trimmed
    }

    private companion object {
        private val GUID_REGEX = Regex("[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}")
    }
}

@Preview
@Composable
fun AppAndroidPreview() {
    App()
}
