package com.tagaev.trrcrm.ui.settings

import com.tagaev.trrcrm.ui.i18n.s

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
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import compose.icons.FeatherIcons
import compose.icons.feathericons.ArrowLeft

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeveloperMenuScreen(
    onBack: () -> Unit,
    onOpenCameraFixator: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Developer") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(FeatherIcons.ArrowLeft, contentDescription = s("settings_nazad"))
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
                    headlineContent = { Text(s("settings_kamera_fiksator")) },
                    supportingContent = { Text(s("settings_semka_i_otpravka_fotografiy")) },
                    modifier = Modifier.clickable(onClick = onOpenCameraFixator),
                )
                Divider()
            }
        }
    }
}
