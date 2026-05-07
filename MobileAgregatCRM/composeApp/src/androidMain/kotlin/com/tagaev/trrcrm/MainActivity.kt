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
    private var lastHandledDeepLinkSignature: String? = null

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
        val extras = intent?.extras
        val extraKeys = extras?.keySet()?.sorted().orEmpty()
        val screen = firstIntentValue(intent, "screen", "screen_raw")
        val rawDocId = firstIntentValue(intent, "docId", "doc_id", "doc_id_raw")
        val messageHint = firstIntentValue(intent, "message_text", "body", "gcm.notification.body", "gcm.n.body")
        val title = firstIntentValue(intent, "title", "docTitle", "gcm.notification.title", "gcm.n.title")
        val hasCanonicalPayload = intent?.getBooleanExtra("has_canonical_payload", false) ?: false
        val docId = extractGuidOrTrim(rawDocId)
        val inferredScreen = screen ?: inferScreenFromTitle(title) ?: if (!title.isNullOrBlank()) "events" else null
        val hasDeepLinkContext = !inferredScreen.isNullOrBlank() || !docId.isNullOrBlank() || !title.isNullOrBlank() || !messageHint.isNullOrBlank()
        val signature = listOf(inferredScreen.orEmpty(), docId.orEmpty(), title.orEmpty(), messageHint.orEmpty()).joinToString("|")
        println(
            "PUSH_SERVICE DEEPLINK: MainActivity.handleIntent hasDeepLink=$hasDeepLinkContext, " +
                    "screen='$inferredScreen', docId='$docId' (raw='$rawDocId'), " +
                    "title='${title.orEmpty()}', keys=$extraKeys, root=${root != null}"
        )
        if (!hasDeepLinkContext) {
            println("PUSH_SERVICE DEEPLINK: missing_push_context, ignoring")
            return
        }
        if (!hasCanonicalPayload && title.isNullOrBlank()) {
            println("PUSH_SERVICE DEEPLINK: missing_canonical_push_payload and no title fallback, skip")
            return
        }
        if (signature == lastHandledDeepLinkSignature) {
            println("PUSH_SERVICE DEEPLINK: duplicate deeplink signature, skipping")
            return
        }

        if (root != null) {
            val resolvedScreen = inferredScreen ?: "events"
            println("PUSH_SERVICE DEEPLINK: Calling onDeepLink with screen='$resolvedScreen'")
            root?.onDeepLink(resolvedScreen, docId, messageHint, title)
            lastHandledDeepLinkSignature = signature
            clearDeepLinkExtras(intent)
        } else {
            println("PUSH_SERVICE DEEPLINK: root is null, cannot process deep link")
        }
    }

    private fun clearDeepLinkExtras(intent: Intent?) {
        intent?.removeExtra("screen")
        intent?.removeExtra("screen_raw")
        intent?.removeExtra("docId")
        intent?.removeExtra("doc_id")
        intent?.removeExtra("doc_id_raw")
        intent?.removeExtra("message_text")
        intent?.removeExtra("body")
        intent?.removeExtra("title")
        intent?.removeExtra("docTitle")
        intent?.removeExtra("gcm.notification.title")
        intent?.removeExtra("gcm.notification.body")
        intent?.removeExtra("gcm.n.title")
        intent?.removeExtra("gcm.n.body")
    }

    private fun extractGuidOrTrim(value: String?): String? {
        if (value.isNullOrBlank()) return null
        val trimmed = value.trim()
        val match = GUID_REGEX.find(trimmed)
        return match?.value ?: trimmed
    }

    private fun firstIntentValue(intent: Intent?, vararg keys: String): String? {
        for (key in keys) {
            val value = intent?.getStringExtra(key)?.trim()
            if (!value.isNullOrBlank()) return value
        }
        return null
    }

    private fun inferScreenFromTitle(title: String?): String? {
        val normalized = title?.lowercase()?.trim().orEmpty()
        if (normalized.isBlank()) return null
        return when {
            normalized.contains("комплект") -> "complectation"
            normalized.contains("заказ-нар") || normalized.contains("заказнар") -> "work_orders"
            normalized.contains("событ") -> "events"
            else -> null
        }
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
