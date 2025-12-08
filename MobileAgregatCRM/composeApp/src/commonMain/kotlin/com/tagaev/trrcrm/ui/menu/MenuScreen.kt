package com.tagaev.trrcrm.ui.menu

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import compose.icons.FeatherIcons
import compose.icons.LineAwesomeIcons
import compose.icons.feathericons.Settings
import compose.icons.lineawesomeicons.ToolboxSolid
import compose.icons.lineawesomeicons.TruckSolid
import org.jetbrains.compose.resources.painterResource

/**
 * Decompose component for this screen.
 * Root component will create this and handle navigation.
 */
//interface MenuComponent {
//    fun openCargo()
//    // add more functions later for other cards
//}

/**
 * Top-level screen composable used by Decompose.
 */
@Composable
fun MenuScreen(
    component: IMenuComponent,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Меню",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        MenuGridScreen(
            items = remember {
                listOf(
                    MenuCardData(
                        id = "cargo",
                        title = "Груз",
                        iconRes = LineAwesomeIcons.TruckSolid // TODO: replace with actual resource
                    ),
                    MenuCardData(
                        id = "settings",
                        title = "Настройки",
                        iconRes = LineAwesomeIcons.ToolboxSolid// TODO: replace with actual resource
                    )
                )
            },
            onItemClick = { item ->
                when (item.id) {
                    "cargo" -> component.openCargo()
                    "settings" -> component.openSettings()
                    // add other when branches for new cards
                }
            },
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        )
    }
}

/* --- Internal UI model & composables --- */

data class MenuCardData(
    val id: String,
    val title: String,
    val iconRes: ImageVector // we’ll resolve to painterResource inside
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun MenuGridScreen(
    items: List<MenuCardData>,
    onItemClick: (MenuCardData) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(items) { item ->
            MenuCard(
                item = item,
                onClick = { onItemClick(item) }
            )
        }
    }
}

@Composable
private fun MenuCard(
    item: MenuCardData,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
//    val painter = painterResource(item.iconRes)

    Card(
        modifier = modifier
            .fillMaxWidth()
            // 1:1 ratio for square menu tiles
            .aspectRatio(1f)
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Top 70%: icon / image
            Box(
                modifier = Modifier
                    .weight(0.7f)
                    .fillMaxWidth()
                    .padding(8.dp),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    imageVector = item.iconRes,
                    contentDescription = item.title,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit
                )
            }

            // Bottom 30%: title
            Box(
                modifier = Modifier
                    .weight(0.3f)
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Text(
                    text = item.title,
                    fontSize = 20.sp,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
