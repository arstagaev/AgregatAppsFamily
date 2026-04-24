package com.tagaev.trrcrm.ui.root

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Text
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.arkivanov.decompose.extensions.compose.stack.animation.fade
import com.arkivanov.decompose.extensions.compose.stack.animation.stackAnimation
import com.arkivanov.decompose.extensions.compose.stack.Children
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import com.tagaev.trrcrm.ui.details.DetailsScreen
import com.tagaev.trrcrm.ui.favorites.FavoritesScreen
import com.tagaev.trrcrm.ui.login.LoginScreen
import com.tagaev.trrcrm.ui.settings.SettingsScreen
import com.tagaev.trrcrm.ui.style.AppTheme
import com.tagaev.trrcrm.ui.style.ThemeController
import compose.icons.FeatherIcons
import org.koin.compose.koinInject
import com.tagaev.trrcrm.ui.cargo.CargoScreen
import com.tagaev.trrcrm.ui.buyer_order.BuyerOrdersScreen
import com.tagaev.trrcrm.ui.complectation.ComplectationsScreen
import com.tagaev.trrcrm.ui.complaints.ComplaintsScreen
import com.tagaev.trrcrm.ui.events.EventsScreen
import com.tagaev.trrcrm.ui.incoming_applications.IncomingApplicationsScreen
import com.tagaev.trrcrm.ui.repair_template_catalog.RepairTemplateCatalogScreen
import com.tagaev.trrcrm.ui.inner_orders.InnerOrdersScreen
import com.tagaev.trrcrm.ui.menu.MenuScreen
import com.tagaev.trrcrm.ui.qrscanner.QRScannerScreen
import com.tagaev.trrcrm.ui.supplier_order.SupplierOrdersScreen
import com.tagaev.trrcrm.ui.work_order.WorkOrdersScreen
import compose.icons.LineAwesomeIcons
import compose.icons.feathericons.Box
import compose.icons.feathericons.Grid
import compose.icons.feathericons.Phone
import compose.icons.feathericons.Truck
import compose.icons.feathericons.Zap
import compose.icons.feathericons.Cpu
import compose.icons.lineawesomeicons.CarSideSolid
import compose.icons.lineawesomeicons.CheckCircle
import compose.icons.lineawesomeicons.QrcodeSolid
import compose.icons.lineawesomeicons.ToolsSolid
import kotlinx.coroutines.launch
import com.tagaev.trrcrm.utils.KnownPermission
import com.tagaev.trrcrm.utils.SessionPermissions

val LocalAppSnackbar = staticCompositionLocalOf<(String) -> Unit> {
    { _ -> }
}

@Composable
fun AppRoot(root: IRootComponent) {
    val stack by root.childStack.subscribeAsState()   // State<ChildStack<..., ...>>
    val activeChild = stack.active.instance
    val themeController = koinInject<ThemeController>()
    AppTheme(controller = themeController) {
        val snackbarHostState = remember { SnackbarHostState() }
        val scope = rememberCoroutineScope()

        CompositionLocalProvider(
            LocalAppSnackbar provides { message: String ->
                scope.launch {
                    snackbarHostState.showSnackbar(
                        message = message,
                        duration = SnackbarDuration.Short // ~4 seconds
                    )
                }
            }
        ) {
            Scaffold(
                contentWindowInsets = WindowInsets.safeDrawing,
                snackbarHost = {
                    SnackbarHost(hostState = snackbarHostState)
                },
                bottomBar = {
                    AnimatedVisibility(visible = activeChild !is IRootComponent.Child.Login) {
                        AppBottomNavBar2(
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
                            onComplectation = {
                                val needBackToList = if (activeChild !is IRootComponent.Child.Complectation) {
                                    false
                                } else {
                                    true
                                }
                                root.openComplectation(needBackToList)
                            },
                            onQRScanner = { if (activeChild !is IRootComponent.Child.QRScanner) root.openQRScanner() },
                            onFavorites = { if (activeChild !is IRootComponent.Child.Favorites) root.openFavorites() },
                            onCargo = {
                                val needBackToList = if (activeChild !is IRootComponent.Child.Cargo) {
                                    false
                                } else {
                                    true
                                }
                                root.openCargo(needBackToList)
                            },
                            onBuyerOrder = {
                                val needBackToList = if (activeChild !is IRootComponent.Child.BuyerOrder) {
                                    false
                                } else {
                                    true
                                }
                                root.openBuyerOrders(needBackToList)
                            },
                            onSupplierOrder = {
                                val needBackToList = if (activeChild !is IRootComponent.Child.SupplierOrder) {
                                    false
                                } else {
                                    true
                                }
                                root.openSupplierOrders(needBackToList)
                            },
                            onComplaint = {
                                val needBackToList = if (activeChild !is IRootComponent.Child.Complaint) {
                                    false
                                } else {
                                    true
                                }
                                root.openComplaint(needBackToList)
                            },
                            onInnerOrder = {
                                val needBackToList = if (activeChild !is IRootComponent.Child.InnerOrder) {
                                    false
                                } else {
                                    true
                                }
                                root.openInnerOrder(needBackToList)
                            },
                            onIncomingApplications = {
                                val needBackToList = if (activeChild !is IRootComponent.Child.IncomingApplications) {
                                    false
                                } else {
                                    true
                                }
                                root.openIncomingApplications(needBackToList)
                            },
                            onRepairTemplateCatalog = {
                                val needBackToList = if (activeChild !is IRootComponent.Child.RepairTemplateCatalog) {
                                    false
                                } else {
                                    true
                                }
                                root.openRepairTemplateCatalog(needBackToList)
                            },
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
                        is IRootComponent.Child.Complectation -> ComplectationsScreen(c.component)
                        is IRootComponent.Child.Cargo -> CargoScreen(c.component)
                        is IRootComponent.Child.BuyerOrder -> BuyerOrdersScreen(c.component)
                        is IRootComponent.Child.SupplierOrder -> SupplierOrdersScreen(c.component)
                        is IRootComponent.Child.Complaint -> ComplaintsScreen(c.component)
                        is IRootComponent.Child.InnerOrder -> InnerOrdersScreen(c.component)
                        is IRootComponent.Child.IncomingApplications -> IncomingApplicationsScreen(c.component)
                        is IRootComponent.Child.RepairTemplateCatalog -> RepairTemplateCatalogScreen(c.component)
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
}

@Composable
fun AppBottomNavBar2(
    activeChild: IRootComponent.Child,
    onEvents: () -> Unit,
    onDetails: () -> Unit,
    onQRScanner: () -> Unit,
    onMenu: () -> Unit,
    onCargo: () -> Unit,
    onBuyerOrder: () -> Unit,
    onSupplierOrder: () -> Unit,
    onFavorites: () -> Unit,
    onSettings: () -> Unit,
    onWorkOrder: () -> Unit,
    onComplectation: () -> Unit,
    onComplaint: () -> Unit,
    onInnerOrder: () -> Unit,
    onIncomingApplications: () -> Unit,
    onRepairTemplateCatalog: () -> Unit,
) {
    val permissionMap by SessionPermissions.state
    val showWorkOrderTab = remember(permissionMap) {
        SessionPermissions.canOpenDocumentTab(KnownPermission.ZAKAZ_NARYAD)
    }
    val showComplectationTab = remember(permissionMap) {
        SessionPermissions.canOpenDocumentTab(KnownPermission.KOMPLEKTATSIYA)
    }
    val showCargoTab = remember(permissionMap) {
        SessionPermissions.canOpenDocumentTab(KnownPermission.GRUZ)
    }
    val showBuyerOrderTab = remember(permissionMap) {
        SessionPermissions.canOpenDocumentTab(KnownPermission.ZAKAZ_POKUPATELYA)
    }
    val showSupplierOrderTab = remember(permissionMap) {
        SessionPermissions.canOpenDocumentTab(KnownPermission.ZAKAZ_POSTAVSHCHIKU)
    }
    val showInnerOrderTab = remember(permissionMap) {
        SessionPermissions.canOpenDocumentTab(KnownPermission.ZAKAZ_VNUTRENNIY)
    }

    // Use Surface to mimic NavigationBar style but control layout ourselves
    Surface(
        tonalElevation = NavigationBarDefaults.Elevation,
        color = NavigationBarDefaults.containerColor,
        contentColor = Color.Blue,
        modifier = Modifier.navigationBarsPadding()
    ) {
        Row(
            modifier = Modifier
                .horizontalScroll(rememberScrollState())
                .height(IntrinsicSize.Min)
                .padding(horizontal = 4.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            BottomNavChip(
                selected = activeChild is IRootComponent.Child.Events,
                onClick = onEvents,
                icon = { Icon(LineAwesomeIcons.CheckCircle, contentDescription = null) },
                label = "События"
            )

            if (showWorkOrderTab) {
                BottomNavChip(
                    selected = activeChild is IRootComponent.Child.WorkOrder,
                    onClick = onWorkOrder,
                    icon = { Icon(LineAwesomeIcons.CarSideSolid, contentDescription = null) },
                    label = "Заказ-Наряды"
                )
            }

            if (showComplectationTab) {
                BottomNavChip(
                    selected = activeChild is IRootComponent.Child.Complectation,
                    onClick = onComplectation,
                    icon = { Icon(LineAwesomeIcons.ToolsSolid, contentDescription = null) },
                    label = "Комплектация"
                )
            }

            if (showCargoTab) {
                BottomNavChip(
                    selected = activeChild is IRootComponent.Child.Cargo,
                    onClick = onCargo,
                    icon = { Icon(FeatherIcons.Truck, contentDescription = null) },
                    label = "Доставки"
                )
            }

            if (showBuyerOrderTab) {
                BottomNavChip(
                    selected = activeChild is IRootComponent.Child.BuyerOrder,
                    onClick = onBuyerOrder,
                    icon = { Icon(FeatherIcons.Box, contentDescription = null) },
                    label = "Заказы покуп."
                )
            }

            if (showSupplierOrderTab) {
                BottomNavChip(
                    selected = activeChild is IRootComponent.Child.SupplierOrder,
                    onClick = onSupplierOrder,
                    icon = { Icon(FeatherIcons.Box, contentDescription = null) },
                    label = "Заказы пост."
                )
            }

            BottomNavChip(
                selected = activeChild is IRootComponent.Child.Complaint,
                onClick = onComplaint,
                icon = { Icon(FeatherIcons.Zap, contentDescription = null) },
                label = "Рекламации"
            )

            if (showInnerOrderTab) {
                BottomNavChip(
                    selected = activeChild is IRootComponent.Child.InnerOrder,
                    onClick = onInnerOrder,
                    icon = { Icon(FeatherIcons.Box, contentDescription = null) },
                    label = "Внутр. заказы"
                )
            }

            BottomNavChip(
                selected = activeChild is IRootComponent.Child.IncomingApplications,
                onClick = onIncomingApplications,
                icon = { Icon(FeatherIcons.Phone, contentDescription = null) },
                label = "Входящие Заявки"
            )

            BottomNavChip(
                selected = activeChild is IRootComponent.Child.RepairTemplateCatalog,
                onClick = onRepairTemplateCatalog,
                icon = { Icon(FeatherIcons.Cpu, contentDescription = null) },
                label = "Калькуляция"
            )

            BottomNavChip(
                selected = activeChild is IRootComponent.Child.QRScanner,
                onClick = onQRScanner,
                icon = { Icon(LineAwesomeIcons.QrcodeSolid, contentDescription = null) },
                label = "QR Сканер"
            )

            BottomNavChip(
                selected = activeChild is IRootComponent.Child.Menu,
                onClick = onMenu,
                icon = { Icon(FeatherIcons.Grid, contentDescription = null) },
                label = "Меню"
            )

            // If later you want:
            // BottomNavChip(
            //     selected = activeChild is IRootComponent.Child.Settings,
            //     onClick = onSettings,
            //     icon = { Icon(FeatherIcons.Settings, contentDescription = null) },
            //     label = "Настройки"
            // )
        }
    }
}
@Composable
private fun BottomNavChip(
    selected: Boolean,
    onClick: () -> Unit,
    icon: @Composable () -> Unit,
    label: String
) {
//    val colors = NavigationBarDefaults.itemColors()

    val containerColor =
        if (selected) MaterialTheme.colorScheme.secondaryContainer
        else MaterialTheme.colorScheme.surface

    val contentColor =
        if (selected) MaterialTheme.colorScheme.onSecondaryContainer
        else MaterialTheme.colorScheme.onSurfaceVariant

    Surface(
        color = containerColor,
        contentColor = contentColor,
        shape = MaterialTheme.shapes.large,
        tonalElevation = if (selected) 2.dp else 0.dp,
        modifier = Modifier
            .padding(horizontal = 4.dp)
            .fillMaxHeight()

    ) {
        Column(
            modifier = Modifier.clickable(onClick = onClick)
                .padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            icon()
            Spacer(Modifier.width(0.dp)) // keep spacer API but no extra width horizontally
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )
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
    onCargo: () -> Unit,
    onFavorites: () -> Unit,
    onSettings: () -> Unit,
    onWorkOrder: () -> Unit
) {
    NavigationBar(Modifier.navigationBarsPadding()) {
        Row(
            modifier = Modifier
                .horizontalScroll(rememberScrollState()),
            verticalAlignment = Alignment.CenterVertically
        ) {
            NavigationBarItem(
                selected = activeChild is IRootComponent.Child.Events,
                onClick = onEvents,
                icon = { Icon(LineAwesomeIcons.ToolsSolid, null) },
                label = { Text("События") }
            )

            NavigationBarItem(
                selected = activeChild is IRootComponent.Child.WorkOrder,
                onClick = onWorkOrder,
                icon = { Icon(LineAwesomeIcons.CarSideSolid, null) },
                label = { Text("Заказ-Наряды", fontSize = 10.sp) }
            )

            NavigationBarItem(
                selected = activeChild is IRootComponent.Child.Cargo,
                onClick = onCargo,
                icon = { Icon(FeatherIcons.Truck, null) },
                label = { Text("Доставки") }
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

//            NavigationBarItem(
//                selected = activeChild is IRootComponent.Child.Favorites,
//                onClick = onFavorites,
//                icon = { Icon(FeatherIcons.Star, null) },
//                label = { Text("Избранное") }
//            )
//
//            NavigationBarItem(
//                selected = activeChild is IRootComponent.Child.Settings,
//                onClick = onSettings,
//                icon = { Icon(FeatherIcons.Settings, null) },
//                label = { Text("Настройки") }
//            )
        }
    }
}
