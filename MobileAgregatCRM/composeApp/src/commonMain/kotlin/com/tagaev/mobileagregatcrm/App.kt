package com.tagaev.mobileagregatcrm

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.arkivanov.decompose.extensions.compose.stack.animation.fade
import com.arkivanov.decompose.extensions.compose.stack.animation.stackAnimation
import com.arkivanov.decompose.extensions.compose.stack.Children
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import com.tagaev.mobileagregatcrm.ui.details.DetailsScreen
import com.tagaev.mobileagregatcrm.ui.favorites.FavoritesScreen
import com.tagaev.mobileagregatcrm.ui.mainscreen.EventsScreen
import com.tagaev.mobileagregatcrm.ui.root.RootComponent
import compose.icons.FeatherIcons
import compose.icons.feathericons.Box
import compose.icons.feathericons.Eye
import compose.icons.feathericons.Home
import compose.icons.feathericons.Star

@Composable
fun AppRoot(root: RootComponent) {
    val stack by root.childStack.subscribeAsState()   // State<ChildStack<..., ...>>
    val activeChild = stack.active.instance

    Scaffold(
        bottomBar = {
            AppBottomNavBar(
                activeChild = activeChild,
                onList     = { if (activeChild !is RootComponent.Child.List)      root.openList() },
                onDetails  = { if (activeChild !is RootComponent.Child.Details)   root.openDetails() },
                onFavorites= { if (activeChild !is RootComponent.Child.Favorites) root.openFavorites() },
            )
        }
    ) { padding ->
        Children(
            stack = root.childStack,
            animation = stackAnimation(fade()),
            modifier = Modifier.padding(padding)
        ) { created ->
            when (val c = created.instance) {
                is RootComponent.Child.List      -> EventsScreen(c.component)
                is RootComponent.Child.Details   -> DetailsScreen(c.component)
                is RootComponent.Child.Favorites -> FavoritesScreen(c.component)
            }
        }
    }
}

@Composable
fun AppBottomNavBar(
    activeChild: RootComponent.Child,
    onList: () -> Unit,
    onDetails: () -> Unit,
    onFavorites: () -> Unit
) {
    NavigationBar(Modifier.height(70.dp)) {
        NavigationBarItem(
            selected = activeChild is RootComponent.Child.List,
            onClick = onList,
            icon = { Icon(FeatherIcons.Home, null) },
            label = { Text("List") }
        )

        NavigationBarItem(
            selected = activeChild is RootComponent.Child.Details,
            onClick = onDetails,
            icon = { Icon(FeatherIcons.Box, null) },
            label = { Text("Details") }
        )

        NavigationBarItem(
            selected = activeChild is RootComponent.Child.Favorites,
            onClick = onFavorites,
            icon = { Icon(FeatherIcons.Star, null) },
            label = { Text("Favorites") }
        )
    }
}