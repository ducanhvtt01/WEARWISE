// Chuyên lo giao diện xin quyền
package com.example.dacs3

import android.Manifest
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.core.app.NotificationManagerCompat

@Composable
fun RequestWeatherPermissions() {
    val context = LocalContext.current
    var showSettingsDialog by remember { mutableStateOf(false) }

    // Đã thêm Manifest.permission.READ_CALENDAR để đọc lịch cho AI
    val permissionsToRequest = mutableListOf(
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION,
        Manifest.permission.READ_CALENDAR
    )

    val multiplePermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        // Logic xử lý khi người dùng cấp hoặc từ chối quyền (bạn có thể log ra để kiểm tra)
        val locationGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
    }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissionsToRequest.add(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            val areNotificationsEnabled = NotificationManagerCompat.from(context).areNotificationsEnabled()
            if (!areNotificationsEnabled) {
                showSettingsDialog = true
            }
        }
        multiplePermissionLauncher.launch(permissionsToRequest.toTypedArray())
    }

    if (showSettingsDialog) {
        AlertDialog(
            onDismissRequest = { showSettingsDialog = false },
            title = { Text(text = "Enable Notifications") },
            text = { Text(text = "The weather alert feature requires notification permission. Would you like to go to Settings to enable it again?") },
            confirmButton = {
                TextButton(onClick = {
                    showSettingsDialog = false
                    val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                        putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                    }
                    context.startActivity(intent)
                }) { Text("Go to Settings") }
            },
            dismissButton = {
                TextButton(onClick = { showSettingsDialog = false }) { Text("Later") }
            }
        )
    }
}