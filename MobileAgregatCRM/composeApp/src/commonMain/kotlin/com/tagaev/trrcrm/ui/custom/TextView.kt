package com.tagaev.trrcrm.ui.custom

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class)
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
    snackbarHostState: SnackbarHostState? = null, // optional, pass if you want toast/snackbar
) {
    val clipboard = LocalClipboardManager.current
    val scope = rememberCoroutineScope()

    Text(
        text = text,
        style = style,
        textAlign = textAlign,
        maxLines = maxLines,
        fontWeight = fontWeight,
        overflow = overflow,
        softWrap = softWrap,
        color = color,
        modifier = modifier
            .combinedClickable(
                onClick = { /* no-op or open details */ },
                onLongClick = {
                    clipboard.setText(AnnotatedString(text))
                    if (snackbarHostState != null) {
                        scope.launch {
                            snackbarHostState.showSnackbar(
                                message = "Скопировано",
                                duration = SnackbarDuration.Short
                            )
                        }
                    }
                }
            )
            .padding(4.dp)
    )
}