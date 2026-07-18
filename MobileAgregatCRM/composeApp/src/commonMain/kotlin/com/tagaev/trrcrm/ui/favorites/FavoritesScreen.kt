package com.tagaev.trrcrm.ui.favorites

import com.tagaev.trrcrm.ui.i18n.s

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

@Composable
fun FavoritesScreen(component: FavoritesComponent) {
    Box(Modifier.fillMaxSize()) {
        Text(modifier = Modifier.align(Alignment.Center), text = s("favorites_sektsiya_izbrannoe_v_razrabotke"))
    }
}
