package com.tagaev.mobileagregatcrm.ui.permissions

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.PermissionChecker
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue

@Composable
actual fun CameraPermissionGate(
    rationaleText: String,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val activity = context as Activity

    fun granted(): Boolean =
        ContextCompat.checkSelfPermission(activity, Manifest.permission.CAMERA) ==
                PermissionChecker.PERMISSION_GRANTED

    var hasPermission by remember { mutableStateOf(granted()) }

    // Launcher to request CAMERA at runtime (official Activity Result API)
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasPermission = isGranted
    }

    if (hasPermission) {
        content()
        return
    }

    // Denied: figure out whether to show rationale or go straight to Settings
    val shouldShowRationale =
        ActivityCompat.shouldShowRequestPermissionRationale(activity, Manifest.permission.CAMERA)

    Box(Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = rationaleText,
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(Modifier.height(12.dp))
            if (shouldShowRationale) {
                // User denied once → show rationale and a request button
                Button(onClick = { launcher.launch(Manifest.permission.CAMERA) }) {
                    Text("Разрешить доступ к камере")
                }
            } else {
                // “Don’t ask again” or first time on some OEMs → take to Settings
                Button(onClick = {
                    val intent = Intent(
                        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                        Uri.parse("package:${activity.packageName}")
                    )
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    activity.startActivity(intent)
                }) {
                    Text("Разрешить доступ к камере через настройки приложения:\n(Разрешения -> Камера -> Использовать всегда / когда вкл. приложение)")
                }
                Spacer(Modifier.height(8.dp))
                TextButton(onClick = { launcher.launch(Manifest.permission.CAMERA) }) {
                    Text("Разрешить доступ к камере")
                }
            }
        }
    }
}