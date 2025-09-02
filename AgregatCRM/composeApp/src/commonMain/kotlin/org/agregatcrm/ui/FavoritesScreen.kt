package org.agregatcrm.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import org.agregatcrm.repository.EventsRepository

@Composable
fun FavoritesScreen(
    //repo: EventsRepository
) {
//    val favorites by repo.favorites.collectAsState()
//    val isFav = ev.number?.let { favorites.contains(it) } ?: false
    // Live favorites
//    val favorites by repo.favoritesFlow().collectAsState(initial = emptySet())
//
//    LazyColumn(
//        modifier = Modifier.fillMaxSize(),
//        contentPadding = PaddingValues(12.dp),
//        verticalArrangement = Arrangement.spacedBy(12.dp)
//    ) {
//        items(
//            items = items,
//            key = { it.number ?: it.guid ?: it.hashCode().toString() }
//        ) { ev ->
//            EventRow(
//                ev = ev,
//                favorites = favorites,
//                onToggleFavorite = { number ->
//                    // launch in rememberCoroutineScope or hoist to ViewModel/Controller
//                    rememberCoroutineScope().launch {
//                        favoritesRepo.toggle(number)
//                    }
//                }
//            )
//        }
//    }
}