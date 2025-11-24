package com.tagaev.trrcrm.ui.custom

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun CustomCircle(size: Dp = 7.dp){
    Canvas(modifier = Modifier.size(size), onDraw = {
        drawCircle(color = Color.Red)
    })
}

@Composable
fun LabelBox(shape: Shape){
    Column(modifier = Modifier.fillMaxWidth().wrapContentSize(Alignment.Center)) {
        Box(
            modifier = Modifier.size(100.dp).clip(shape).background(Color.Red)
        ) {
            Text("2 ")
        }
    }
}