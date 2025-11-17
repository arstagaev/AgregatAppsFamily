package com.tagaev.mobileagregatcrm.ui.permissions

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.g00fy2.quickie.QRResult
import io.github.g00fy2.quickie.ScanQRCode


@Composable
actual fun CameraView(decodedString: (String) -> Unit) {
    var last by remember { mutableStateOf<String?>(null) }

    // Quickie launcher opens the camera UI and returns a QRResult
    val scanLauncher = rememberLauncherForActivityResult(ScanQRCode()) { result ->
        when (result) {
            is QRResult.QRSuccess -> {
                val value = result.content.rawValue.orEmpty()
                last = value
                decodedString.invoke(value)
            }
            QRResult.QRUserCanceled -> {
                last = "Canceled"
            }
            is QRResult.QRMissingPermission -> {
                last = "Camera permission denied"
            }
            is QRResult.QRError -> {
                last = "Error: ${result.exception.message ?: "unknown"}"
            }
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Button(onClick = { scanLauncher.launch(null) }) {
            Text("Сканировать QR код")
        }
//        Spacer(Modifier.height(12.dp))
//        Text(
//            text = last?.let { "Last result: $it" } ?: "No scan yet",
//            maxLines = 2,
//            overflow = TextOverflow.Ellipsis
//        )
    }
}