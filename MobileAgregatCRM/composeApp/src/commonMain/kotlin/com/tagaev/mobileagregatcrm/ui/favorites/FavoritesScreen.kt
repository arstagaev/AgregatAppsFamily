package com.tagaev.mobileagregatcrm.ui.favorites

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.tagaev.mobileagregatcrm.ui.mainscreen.ListComponent

@Composable
fun FavoritesScreen(component: FavoritesComponent) {
    Box(Modifier.fillMaxSize()) {
        Text(modifier = Modifier.align(Alignment.Center), text = "Секция избранное в разработке...")
    }
}
