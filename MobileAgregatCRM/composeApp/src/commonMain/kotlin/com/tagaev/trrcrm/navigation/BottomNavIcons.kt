package com.tagaev.trrcrm.navigation

import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import compose.icons.FeatherIcons
import compose.icons.LineAwesomeIcons
import compose.icons.feathericons.Box
import compose.icons.feathericons.Cpu
import compose.icons.feathericons.DollarSign
import compose.icons.feathericons.Grid
import compose.icons.feathericons.Phone
import compose.icons.feathericons.Truck
import compose.icons.feathericons.Zap
import compose.icons.lineawesomeicons.CarSideSolid
import compose.icons.lineawesomeicons.CheckCircle
import compose.icons.lineawesomeicons.QrcodeSolid
import compose.icons.lineawesomeicons.ToolsSolid

@Composable
fun BottomNavItemIcon(id: BottomNavItemId) {
    when (id) {
        BottomNavItemId.EVENTS -> Icon(LineAwesomeIcons.CheckCircle, contentDescription = null)
        BottomNavItemId.WORK_ORDER -> Icon(LineAwesomeIcons.CarSideSolid, contentDescription = null)
        BottomNavItemId.COMPLECTATION -> Icon(LineAwesomeIcons.ToolsSolid, contentDescription = null)
        BottomNavItemId.CARGO -> Icon(FeatherIcons.Truck, contentDescription = null)
        BottomNavItemId.BUYER_ORDER -> Icon(FeatherIcons.Box, contentDescription = null)
        BottomNavItemId.SUPPLIER_ORDER -> Icon(FeatherIcons.Box, contentDescription = null)
        BottomNavItemId.COMPLAINT -> Icon(FeatherIcons.Zap, contentDescription = null)
        BottomNavItemId.INNER_ORDER -> Icon(FeatherIcons.Box, contentDescription = null)
        BottomNavItemId.INCOMING_APPLICATIONS -> Icon(FeatherIcons.Phone, contentDescription = null)
        BottomNavItemId.REPAIR_TEMPLATE_CATALOG -> Icon(FeatherIcons.Cpu, contentDescription = null)
        BottomNavItemId.EXPENSE_REQUESTS -> Icon(FeatherIcons.DollarSign, contentDescription = null)
        BottomNavItemId.QR_SCANNER -> Icon(LineAwesomeIcons.QrcodeSolid, contentDescription = null)
        BottomNavItemId.MENU -> Icon(FeatherIcons.Grid, contentDescription = null)
    }
}
