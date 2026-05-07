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
import com.tagaev.trrcrm.push.NotificationContextParser
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
        val hasPushLaunchMarkers = hasPushLaunchMarkers(intent, extraKeys)
        val screen = firstIntentText(intent, "screen", "screen_raw")
        val rawDocId = firstIntentText(
            intent,
            "docId",
            "doc_id",
            "doc_id_raw",
            "search_key",
            "searchQuery",
            "search_query"
        )
        val messageHint = firstIntentText(
            intent,
            "message_text",
            "body",
            "gcm.notification.body",
            "gcm.n.body",
            "android.text",
            "android.bigText"
        )
        val title = firstIntentText(
            intent,
            "title",
            "docTitle",
            "doc_title",
            "notification_title",
            "gcm.notification.title",
            "gcm.n.title",
            "android.title",
            "android.title.big"
        )
        val parsedContext = NotificationContextParser.parse(
            title = title,
            screen = screen,
            docId = rawDocId,
            messageText = messageHint
        )
        val parsedScreen = parsedContext.screen
        val parsedIdentifier = parsedContext.primaryKey
        val hasDeepLinkContext =
            !parsedScreen.isNullOrBlank() ||
                    !parsedIdentifier.isNullOrBlank() ||
                    !title.isNullOrBlank() ||
                    !messageHint.isNullOrBlank()
        val signature = listOf(
            parsedScreen.orEmpty(),
            parsedIdentifier.orEmpty(),
            title.orEmpty(),
            messageHint.orEmpty()
        ).joinToString("|")
        println(
            "PUSH_SERVICE DEEPLINK: MainActivity.handleIntent hasDeepLink=$hasDeepLinkContext, " +
                    "screen='$parsedScreen', identifier='$parsedIdentifier' (rawDocId='$rawDocId'), " +
                    "title='${title.orEmpty()}', keys=$extraKeys, root=${root != null}"
        )
        if (hasPushLaunchMarkers) {
            println("PUSH_SERVICE DEEPLINK: detected push launch markers, opening MainHome")
            root?.onPushLaunchIntent()
            return
        }
        if (!hasDeepLinkContext) {
            println("PUSH_SERVICE DEEPLINK: missing_push_context, ignoring")
            return
        }
        if (signature == lastHandledDeepLinkSignature) {
            println("PUSH_SERVICE DEEPLINK: duplicate deeplink signature, skipping")
            return
        }

        if (root != null) {
            val resolvedScreen = parsedScreen ?: "events"
            println("PUSH_SERVICE DEEPLINK: Calling onDeepLink with screen='$resolvedScreen'")
            root?.onDeepLink(resolvedScreen, parsedIdentifier, messageHint, title)
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
        intent?.removeExtra("doc_title")
        intent?.removeExtra("gcm.notification.title")
        intent?.removeExtra("gcm.notification.body")
        intent?.removeExtra("gcm.n.title")
        intent?.removeExtra("gcm.n.body")
        intent?.removeExtra("search_key")
        intent?.removeExtra("searchQuery")
        intent?.removeExtra("search_query")
    }

    private fun firstIntentText(intent: Intent?, vararg keys: String): String? {
        val extras = intent?.extras
        for (key in keys) {
            val value = intent?.getStringExtra(key)?.trim()
            if (!value.isNullOrBlank()) return value
            val bundledText = extras?.getString(key)?.trim()
            if (!bundledText.isNullOrBlank()) return bundledText
            val bundledSequence = extras?.getCharSequence(key)?.toString()?.trim()
            if (!bundledSequence.isNullOrBlank()) return bundledSequence
        }
        return null
    }

    private fun hasPushLaunchMarkers(intent: Intent?, keys: List<String>): Boolean {
        if (keys.any { it == "google.message_id" || it == "from" || it == "collapse_key" || it == "gcm.n.analytics_data" }) {
            return true
        }
        return !firstIntentText(intent, "google.message_id", "from", "collapse_key").isNullOrBlank()
    }
}

@Preview
@Composable
fun AppAndroidPreview() {
    App()
}
