package org.agregatcrm.ui

import androidx.compose.foundation.layout.height
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import compose.icons.FeatherIcons
import compose.icons.feathericons.Columns
import compose.icons.feathericons.Heart
import compose.icons.feathericons.Home
import compose.icons.feathericons.Star
import org.agregatcrm.utils.CURRENT_SCREEN
import org.agregatcrm.utils.CurrentScreen

data class BottomNavItem(
    val label: String,
    val icon: ImageVector,
    val screen: CurrentScreen
)

val bottomNavItems = listOf(
    BottomNavItem("Главная", FeatherIcons.Home,    CurrentScreen.List     , ),
    BottomNavItem("События", FeatherIcons.Columns, CurrentScreen.Details  , ),
    BottomNavItem("Любимые", FeatherIcons.Heart,   CurrentScreen.Favorites, )
)

@Composable
fun AppBottomNavBar(
    items: List<BottomNavItem> = bottomNavItems,
    onItemSelected: (BottomNavItem) -> Unit
) {
    var currentScreen by remember { mutableStateOf(CURRENT_SCREEN) }

    NavigationBar(Modifier.height(40.dp)) {
        items.forEachIndexed { index, item ->
            NavigationBarItem(
                selected = item.screen == currentScreen.value,
                onClick = {

                    onItemSelected(item)
                },
                icon = { Icon(item.icon, contentDescription = item.label) },
                //label = { Text(item.label) }
            )
        }
    }
}