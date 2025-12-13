package com.tagaev.trrcrm.ui.custom

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.tagaev.trrcrm.ui.root.LocalAppSnackbar
import kotlinx.coroutines.launch

private val urlRegex =
    "(https?://[A-Za-z0-9\\-._~:/?#\\[\\]@!$&'()*+,;=%]+)".toRegex()

private fun trimTrailingPunctuation(url: String): String =
    url.trimEnd('.', ',', ';', ':', '!', '?', ')', ']', '"', '\'')

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TextCAnnotated(
    text: AnnotatedString,
    modifier: Modifier = Modifier,
    style: TextStyle = LocalTextStyle.current,
    fontWeight: FontWeight? = null,
    textAlign: TextAlign? = null,
    overflow: TextOverflow = TextOverflow.Clip,
    softWrap: Boolean = true,
    color: Color = Color.Unspecified,
    maxLines: Int = Int.MAX_VALUE,
    snackbarHostState: SnackbarHostState? = null, // optional, pass if you want toast/snackbar
    linkColor: Color = MaterialTheme.colorScheme.primary,
) {
    val clipboard = LocalClipboardManager.current
    val scope = rememberCoroutineScope()
    val uriHandler = LocalUriHandler.current
    val showSnackbar = LocalAppSnackbar.current

    // Build a new AnnotatedString with URL spans highlighted and annotated.
    val linkAnnotatedText = remember(text, linkColor) {
        val raw = text.text
        buildAnnotatedString {
            append(raw)

            for (match in urlRegex.findAll(raw)) {
                val matchValue = trimTrailingPunctuation(match.value)
                if (matchValue.isEmpty()) continue

                val start = match.range.first
                val end = start + matchValue.length

                addStyle(
                    style = SpanStyle(
                        color = linkColor,
                        textDecoration = TextDecoration.Underline,
                        fontWeight = fontWeight ?: style.fontWeight
                    ),
                    start = start,
                    end = end
                )
                addStringAnnotation(
                    tag = "URL",
                    annotation = matchValue,
                    start = start,
                    end = end
                )
            }
        }
    }

    val resolvedColor = if (color == Color.Unspecified) {
        MaterialTheme.colorScheme.onSurface
    } else {
        color
    }

    var textLayoutResult: TextLayoutResult? = remember { null }

    Text(
        text = linkAnnotatedText,
        style = style.copy(
            color = resolvedColor,
            textAlign = textAlign ?: style.textAlign
        ),
        maxLines = maxLines,
        softWrap = softWrap,
        overflow = overflow,
        modifier = modifier
            .pointerInput(linkAnnotatedText, textLayoutResult) {
                detectTapGestures(
                    onTap = { offsetPos ->
                        println(">>>>> onTap")
                        val layout = textLayoutResult ?: return@detectTapGestures
                        val offset = layout.getOffsetForPosition(offsetPos)
                        val annotations = linkAnnotatedText.getStringAnnotations(
                            tag = "URL",
                            start = offset,
                            end = offset
                        )
                        val url = annotations.firstOrNull()?.item
                        if (url != null) {
                            uriHandler.openUri(url)
                        }
                    },
                    onLongPress = {
                        println(">>>>> onLongPress")
                        clipboard.setText(text)
                        if (snackbarHostState != null) {
                            scope.launch {
                                snackbarHostState.showSnackbar(
                                    message = "Скопировано",
                                    duration = SnackbarDuration.Short
                                )
                            }
                        }

                        showSnackbar("Скопировано!")
                    }
                )
            }
            .padding(4.dp),
        onTextLayout = { layoutResult ->
            textLayoutResult = layoutResult
        }
    )
}

@Composable
fun TextC(
    text: String,
    modifier: Modifier = Modifier,
    style: TextStyle = LocalTextStyle.current,
    fontWeight: FontWeight? = null,
    textAlign: TextAlign? = null,
    overflow: TextOverflow = TextOverflow.Clip,
    softWrap: Boolean = true,
    color: Color = Color.Unspecified,
    maxLines: Int = Int.MAX_VALUE,
    snackbarHostState: SnackbarHostState? = null,
) {
    TextCAnnotated(
        text = AnnotatedString(text.withEscapedNewlines()),
        modifier = modifier,
        style = style,
        fontWeight = fontWeight,
        textAlign = textAlign,
        overflow = overflow,
        softWrap = softWrap,
        color = color,
        maxLines = maxLines,
        snackbarHostState = snackbarHostState,
    )
}

@Composable
fun TextCLinkPreview(
    text: String,
    modifier: Modifier = Modifier,
    style: TextStyle = LocalTextStyle.current,
    fontWeight: FontWeight? = null,
    textAlign: TextAlign? = null,
    overflow: TextOverflow = TextOverflow.Clip,
    softWrap: Boolean = true,
    color: Color = Color.Unspecified,
    maxLines: Int = Int.MAX_VALUE,
    snackbarHostState: SnackbarHostState? = null,
) {
    Column(modifier = modifier) {
        TextC(
            text = text,
//            modifier = modifier,
            style = style,
            fontWeight = fontWeight,
            textAlign = textAlign,
            overflow = overflow,
            softWrap = softWrap,
            color = color,
            maxLines = maxLines,
            snackbarHostState = snackbarHostState,
        )

        LinkPreviewBox(
            text = text,
            modifier = modifier,
        )
    }

}


fun String.withEscapedNewlines(): String =
    replace("\\n", "\n")


