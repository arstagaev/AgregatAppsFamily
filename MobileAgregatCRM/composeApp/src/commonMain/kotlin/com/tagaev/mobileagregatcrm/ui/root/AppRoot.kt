package com.tagaev.mobileagregatcrm.ui.root

import androidx.compose.animation.AnimatedVisibility
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
import com.tagaev.mobileagregatcrm.ui.login.LoginScreen
import com.tagaev.mobileagregatcrm.ui.mainscreen.MainListScreen
import com.tagaev.mobileagregatcrm.ui.settings.SettingsScreen
import com.tagaev.mobileagregatcrm.ui.style.AppTheme
import com.tagaev.mobileagregatcrm.ui.style.ThemeController
import compose.icons.FeatherIcons
import compose.icons.feathericons.Box
import compose.icons.feathericons.Home
import compose.icons.feathericons.Settings
import compose.icons.feathericons.Star
import org.koin.compose.koinInject
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.navigationBarsPadding

@Composable
fun AppRoot(root: IRootComponent) {
    val stack by root.childStack.subscribeAsState()   // State<ChildStack<..., ...>>
    val activeChild = stack.active.instance
    val themeController = koinInject<ThemeController>()
    AppTheme(controller = themeController) {
        Scaffold(
            contentWindowInsets = WindowInsets.safeDrawing,
            bottomBar = {
                AnimatedVisibility(visible = activeChild !is IRootComponent.Child.Login) {
                    AppBottomNavBar(
                        activeChild = activeChild,
                        onList = { if (activeChild !is IRootComponent.Child.List) root.openList() },
                        onDetails = { if (activeChild !is IRootComponent.Child.Details) root.openDetails() },
                        onFavorites = { if (activeChild !is IRootComponent.Child.Favorites) root.openFavorites() },
                        onSettings = { if (activeChild !is IRootComponent.Child.Settings) root.openSettings() },
                    )
                }
            }
        ) { padding ->
            Children(
                stack = root.childStack,
                animation = stackAnimation(fade()),
                modifier = Modifier
                    .padding(padding)
                    .consumeWindowInsets(padding)
            ) { created ->
                when (val c = created.instance) {
                    is IRootComponent.Child.List -> MainListScreen(c.component)
                    is IRootComponent.Child.Details -> DetailsScreen(c.component)
                    is IRootComponent.Child.Favorites -> FavoritesScreen(c.component)
                    is IRootComponent.Child.Settings -> SettingsScreen(c.component)
                    is IRootComponent.Child.Login -> LoginScreen(c.component)
                }
            }
        }
    }
}

@Composable
fun AppBottomNavBar(
    activeChild: IRootComponent.Child,
    onList: () -> Unit,
    onDetails: () -> Unit,
    onFavorites: () -> Unit,
    onSettings: () -> Unit
) {
    NavigationBar(Modifier.navigationBarsPadding()) {
        NavigationBarItem(
            selected = activeChild is IRootComponent.Child.List,
            onClick = onList,
            icon = { Icon(FeatherIcons.Home, null) },
            label = { Text("События") }
        )

        NavigationBarItem(
            selected = activeChild is IRootComponent.Child.Details,
            onClick = onDetails,
            icon = { Icon(FeatherIcons.Box, null) },
            label = { Text("Событие") }
        )

        NavigationBarItem(
            selected = activeChild is IRootComponent.Child.Favorites,
            onClick = onFavorites,
            icon = { Icon(FeatherIcons.Star, null) },
            label = { Text("Избранное") }
        )

        NavigationBarItem(
            selected = activeChild is IRootComponent.Child.Settings,
            onClick = onSettings,
            icon = { Icon(FeatherIcons.Settings, null) },
            label = { Text("Настройки") }
        )
    }
}