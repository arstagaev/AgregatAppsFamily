package com.tagaev.mobileagregatcrm.utils

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.OutlinedTextFieldDefaults.DecorationBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CenteredNoPaddingOutlinedField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    labelText: String? = null,                  // ⬅️ label you asked for
    placeholderText: String? = null,
    enabled: Boolean = true,
    isError: Boolean = false,
    textStyle: TextStyle = LocalTextStyle.current.copy(textAlign = TextAlign.Center),
) {
    val interaction = remember { MutableInteractionSource() }

    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        singleLine = true,
        textStyle = textStyle,
        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
        modifier = modifier,
        decorationBox = { inner ->
            DecorationBox(
                value = value,
                // hard-center the editable text
                innerTextField = { Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { inner() } },
                enabled = enabled,
                isError = isError,
                singleLine = true,
                visualTransformation = VisualTransformation.None,
                interactionSource = interaction,
                // add a tiny top inset only when a label exists so the notch/label don't collide
                contentPadding = if (labelText != null) PaddingValues(top = 12.dp) else PaddingValues(0.dp),
                label = labelText?.let { { androidx.compose.material3.Text(it) } },
                placeholder = placeholderText?.let { { androidx.compose.material3.Text(it) } },
                colors = OutlinedTextFieldDefaults.colors(),
                // draw the standard outlined container with label notch
                container = {
                    OutlinedTextFieldDefaults.Container(
                        enabled = enabled,
                        isError = isError,
                        interactionSource = interaction
                    )
                }
            )
        }
    )
}
