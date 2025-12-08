package com.tagaev.trrcrm.ui.root

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.arkivanov.decompose.extensions.compose.stack.animation.fade
import com.arkivanov.decompose.extensions.compose.stack.animation.stackAnimation
import com.arkivanov.decompose.extensions.compose.stack.Children
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import com.tagaev.trrcrm.ui.details.DetailsScreen
import com.tagaev.trrcrm.ui.favorites.FavoritesScreen
import com.tagaev.trrcrm.ui.login.LoginScreen
import com.tagaev.trrcrm.ui.mainscreen.MainListScreen
import com.tagaev.trrcrm.ui.settings.SettingsScreen
import com.tagaev.trrcrm.ui.style.AppTheme
import com.tagaev.trrcrm.ui.style.ThemeController
import compose.icons.FeatherIcons
import compose.icons.feathericons.Settings
import org.koin.compose.koinInject
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.ui.unit.sp
import com.tagaev.trrcrm.ui.cargo.CargoScreen
import com.tagaev.trrcrm.ui.events.EventsScreen
import com.tagaev.trrcrm.ui.menu.MenuScreen
import com.tagaev.trrcrm.ui.qrscanner.QRScannerScreen
import com.tagaev.trrcrm.ui.work_order.WorkOrdersScreen
import compose.icons.LineAwesomeIcons
import compose.icons.feathericons.Grid
import compose.icons.feathericons.Menu
import compose.icons.lineawesomeicons.CarSideSolid
import compose.icons.lineawesomeicons.QrcodeSolid
import compose.icons.lineawesomeicons.ToolsSolid

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
                        onEvents = {
                            val needBackToList = if (activeChild !is IRootComponent.Child.Events) {
                                false
                            } else {
                                true
                            }
                            root.openEvents(needBackToList)
                        },
                        onDetails = { if (activeChild !is IRootComponent.Child.Details) root.openDetails() },
                        onWorkOrder = {
                            val needBackToList = if (activeChild !is IRootComponent.Child.WorkOrder) {
                                false
                            } else {
                                true
                            }
                            root.openWorkOrders(needBackToList)
                        },
                        onQRScanner = { if (activeChild !is IRootComponent.Child.QRScanner) root.openQRScanner() },
                        onFavorites = { if (activeChild !is IRootComponent.Child.Favorites) root.openFavorites() },
                        onMenu = { if (activeChild !is IRootComponent.Child.Menu) root.openMenu() },
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
                    is IRootComponent.Child.Events -> EventsScreen(c.component,)
                    is IRootComponent.Child.Details -> DetailsScreen(c.component)
                    is IRootComponent.Child.WorkOrder -> WorkOrdersScreen(c.component)
                    is IRootComponent.Child.Cargo -> CargoScreen(c.component)
                    is IRootComponent.Child.Favorites -> FavoritesScreen(c.component)
                    is IRootComponent.Child.Settings -> SettingsScreen(c.component)
                    is IRootComponent.Child.Menu -> MenuScreen(c.component)
                    is IRootComponent.Child.QRScanner -> QRScannerScreen(c.component)
                    is IRootComponent.Child.Login -> LoginScreen(c.component)
                }
            }
        }
    }
}

@Composable
fun AppBottomNavBar(
    activeChild: IRootComponent.Child,
    onEvents: () -> Unit,
    onDetails: () -> Unit,
    onQRScanner: () -> Unit,
    onMenu: () -> Unit,
    onFavorites: () -> Unit,
    onSettings: () -> Unit,
    onWorkOrder: () -> Unit
) {
    NavigationBar(Modifier.navigationBarsPadding()) {
//        NavigationBarItem(
//            selected = activeChild is IRootComponent.Child.List,
//            onClick = onList,
//            icon = { Icon(FeatherIcons.Home, null) },
//            label = { Text("События") }
//        )

        NavigationBarItem(
            selected = activeChild is IRootComponent.Child.Events,
            onClick = onEvents,
            icon = { Icon(LineAwesomeIcons.ToolsSolid, null) },
            label = { Text("События") }
        )

//        NavigationBarItem(
//            selected = activeChild is IRootComponent.Child.Details,
//            onClick = onDetails,
//            icon = { Icon(FeatherIcons.Box, null) },
//            label = { Text("Событие") }
//        )

        NavigationBarItem(
            selected = activeChild is IRootComponent.Child.WorkOrder,
            onClick = onWorkOrder,
            icon = { Icon(LineAwesomeIcons.CarSideSolid, null) },
            label = { Text("Заказ-Наряды", fontSize = 10.sp) }
        )

        NavigationBarItem(
            selected = activeChild is IRootComponent.Child.QRScanner,
            onClick = onQRScanner,
            icon = { Icon(LineAwesomeIcons.QrcodeSolid, null) },
            label = { Text("QR Сканер") }
        )

        NavigationBarItem(
            selected = activeChild is IRootComponent.Child.Menu,
            onClick = onMenu,
            icon = { Icon(FeatherIcons.Grid, null) },
            label = { Text("Меню") }
        )

//        NavigationBarItem(
//            selected = activeChild is IRootComponent.Child.Favorites,
//            onClick = onFavorites,
//            icon = { Icon(FeatherIcons.Star, null) },
//            label = { Text("Избранное") }
//        )

//        NavigationBarItem(
//            selected = activeChild is IRootComponent.Child.Settings,
//            onClick = onSettings,
//            icon = { Icon(FeatherIcons.Settings, null) },
//            label = { Text("Настройки") }
//        )
    }
}