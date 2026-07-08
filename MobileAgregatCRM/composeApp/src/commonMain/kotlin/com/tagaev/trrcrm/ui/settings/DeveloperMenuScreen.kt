package com.tagaev.trrcrm.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.tagaev.trrcrm.data.AppSettings
import com.tagaev.trrcrm.developer.ComplectationPhotosViewerFeatureState
import compose.icons.FeatherIcons
import compose.icons.feathericons.ArrowLeft
import org.koin.compose.koinInject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeveloperMenuScreen(
    onBack: () -> Unit,
    onOpenCameraFixator: () -> Unit,
) {
    val appSettings = koinInject<AppSettings>()

    LaunchedEffect(Unit) {
        ComplectationPhotosViewerFeatureState.loadFrom(appSettings)
    }

    val photosViewerEnabled by ComplectationPhotosViewerFeatureState.enabled

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Developer") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(FeatherIcons.ArrowLeft, contentDescription = "Назад")
                    }
                },
            )
        },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
        ) {
            item {
                ListItem(
                    headlineContent = { Text("Просмотр фотографий комплектации") },
                    supportingContent = { Text("Сетка 2×N, пагинация по 10") },
                    trailingContent = {
                        Switch(
                            checked = photosViewerEnabled,
                            onCheckedChange = { enabled ->
                                ComplectationPhotosViewerFeatureState.setEnabled(appSettings, enabled)
                            },
                        )
                    },
                )
                Divider()
            }
            item {
                ListItem(
                    headlineContent = { Text("Камера фиксатор") },
                    supportingContent = { Text("Съёмка и отправка фотографий") },
                    modifier = Modifier.clickable(onClick = onOpenCameraFixator),
                )
                Divider()
            }
        }
    }
}
