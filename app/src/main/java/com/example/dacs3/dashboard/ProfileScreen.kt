package com.example.dacs3.dashboard

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.work.WorkManager
import com.example.dacs3.WorkerSetupHelper
import com.example.dacs3.connectDB.DashboardViewModel
import com.example.dacs3.connectDB.Profile
import com.example.dacs3.connectDB.supabase
import io.github.jan.supabase.gotrue.auth
import kotlinx.coroutines.launch

// HÀM KIỂM TRA QUYỀN RIÊNG ĐỂ TÁI SỬ DỤNG
fun checkNotificationPermission(context: Context): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
    } else {
        NotificationManagerCompat.from(context).areNotificationsEnabled()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    isDarkMode: Boolean,
    onThemeChange: (Boolean) -> Unit,
    viewModel: DashboardViewModel = viewModel(),
    onLogoutSuccess: () -> Unit,
    userProfile: Profile?
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val lifecycleOwner = LocalLifecycleOwner.current

    // SỬ DỤNG SHAREDPREFERENCES ĐỂ LƯU "LỰA CHỌN CỦA NGƯỜI DÙNG"
    val sharedPrefs =
        remember { context.getSharedPreferences("WearwisePrefs", Context.MODE_PRIVATE) }

    // STATE THÔNG BÁO: Chỉ bật khi người dùng đã gạt bật VÀ Hệ thống cho phép
    var notificationsEnabled by remember {
        mutableStateOf(
            sharedPrefs.getBoolean(
                "is_notif_enabled",
                false
            ) && checkNotificationPermission(context)
        )
    }

    var showSettingsDialog by remember { mutableStateOf(false) }
    var showMeasurementSheet by remember { mutableStateOf(false) }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val userId = remember { supabase.auth.currentUserOrNull()?.id ?: "" }

    // LẮNG NGHE SỰ KIỆN TỪ CÀI ĐẶT HỆ THỐNG TRỞ VỀ (ON_RESUME)
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                val hasSystemPermission = checkNotificationPermission(context)
                val wantsEnabledInApp = sharedPrefs.getBoolean("is_notif_enabled", false)

                // Kịch bản 1: Ra ngoài Cài đặt tắt đi
                if (!hasSystemPermission && wantsEnabledInApp) {
                    notificationsEnabled = false
                    sharedPrefs.edit().putBoolean("is_notif_enabled", false).apply()
                    WorkManager.getInstance(context).cancelUniqueWork("WeatherMonitorTask")
                    WorkManager.getInstance(context).cancelUniqueWork("DailyOutfitTask")
                }
                // Kịch bản 2: Ra ngoài Cài đặt lén bật lên
                else if (hasSystemPermission && !wantsEnabledInApp) {
                    notificationsEnabled = true
                    sharedPrefs.edit().putBoolean("is_notif_enabled", true).apply()
                    WorkerSetupHelper.setupWeatherMonitor(context)
                    WorkerSetupHelper.setupDailyOutfitNotification(context)
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // LAUNCHER XIN QUYỀN CÓ LƯU TRẠNG THÁI
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            notificationsEnabled = true
            sharedPrefs.edit().putBoolean("is_notif_enabled", true).apply()
            WorkerSetupHelper.setupWeatherMonitor(context)
            WorkerSetupHelper.setupDailyOutfitNotification(context)
        } else {
            showSettingsDialog = true
            notificationsEnabled = false
            sharedPrefs.edit().putBoolean("is_notif_enabled", false).apply()
        }
    }

    // --- HIỂN THỊ BOTTOM SHEET SỐ ĐO ---
    if (showMeasurementSheet && userProfile != null) {
        ModalBottomSheet(
            onDismissRequest = { showMeasurementSheet = false },
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.background
        ) {
            MeasurementEditSheetContent(
                currentProfile = userProfile,
                onSave = { h, w, s ->
                    if (h != null && w != null && s != null) {
                        viewModel.updateMeasurements(userId, h, w, s)
                    }
                    showMeasurementSheet = false
                },
                onCancel = { showMeasurementSheet = false }
            )
        }
    }

    // --- HIỂN THỊ HỘP THOẠI XIN QUYỀN ---
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

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 24.dp)
    ) {
        // --- HEADER ---
        item {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(top = 40.dp, bottom = 24.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Person,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(35.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Profile",
                    fontSize = 35.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.primary,
                    letterSpacing = (-0.5).sp
                )
            }
        }

        // --- USER INFO CARD ---
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(
                        8.dp,
                        RoundedCornerShape(24.dp),
                        spotColor = MaterialTheme.colorScheme.surfaceVariant
                    ),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Row(modifier = Modifier.padding(24.dp)) {
                    Box(
                        modifier = Modifier
                            .size(100.dp)
                            .background(MaterialTheme.colorScheme.secondaryContainer, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            userProfile?.fullName?.firstOrNull()?.uppercase() ?: "A",
                            fontSize = 40.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.secondary
                        )
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .offset(x = 4.dp, y = 4.dp)
                                .size(32.dp)
                                .background(MaterialTheme.colorScheme.primary, CircleShape)
                                .clickable { },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Filled.Edit,
                                "Edit",
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    Column(
                        modifier = Modifier.padding(start = 24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = userProfile?.fullName ?: "Loading...",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            supabase.auth.currentUserOrNull()?.email ?: "email@gmail.com",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = { },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                contentColor = MaterialTheme.colorScheme.primary
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Edit Profile", fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(32.dp)) }

        // --- PERSONALIZATION SECTION ---
        item {
            SectionTitle("AI Personalization")

            SettingRow(
                icon = Icons.Outlined.Straighten,
                title = "My Measurements",
                subtitle = if (userProfile != null)
                    "${userProfile.heightCm?.toInt() ?: 0}cm, ${userProfile.weightKg?.toInt() ?: 0}kg, ${userProfile.bodyShape ?: ""}"
                else "Loading...",
                onClick = { showMeasurementSheet = true }
            )
            SettingRow(
                icon = Icons.Outlined.Style,
                title = "Style Preferences",
                subtitle = "Minimalist, Smart Casual"
            )
        }

        item { Spacer(modifier = Modifier.height(24.dp)) }

        // --- APP SETTINGS SECTION ---
        item {
            SectionTitle("App Settings")

            // XỬ LÝ SỰ KIỆN BẬT/TẮT THÔNG BÁO TẠI ĐÂY (ĐỒNG BỘ 2 CHIỀU)
            SettingToggleRow(
                icon = Icons.Outlined.Notifications,
                title = "Notifications",
                isChecked = notificationsEnabled,
                onCheckedChange = { isTurningOn ->
                    if (isTurningOn) {
                        // KHI NGƯỜI DÙNG MUỐN BẬT
                        if (checkNotificationPermission(context)) {
                            // Máy đã cho phép -> Chỉ cần bật logic chạy ngầm trong app
                            notificationsEnabled = true
                            sharedPrefs.edit().putBoolean("is_notif_enabled", true).apply()
                            WorkerSetupHelper.setupWeatherMonitor(context)
                            WorkerSetupHelper.setupDailyOutfitNotification(context)
                        } else {
                            // Máy chưa cho phép -> Xin quyền
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            } else {
                                showSettingsDialog = true
                            }
                        }
                    } else {
                        // KHI NGƯỜI DÙNG MUỐN TẮT -> Ép mở Cài đặt thông báo của máy
                        val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                            putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                        }
                        context.startActivity(intent)
                    }
                }
            )

            SettingToggleRow(
                icon = Icons.Outlined.DarkMode,
                title = "Dark Mode",
                isChecked = isDarkMode,
                onCheckedChange = onThemeChange
            )
            SettingRow(
                icon = Icons.Outlined.Language,
                title = "Language",
                subtitle = "English (US)"
            )
        }

        item { Spacer(modifier = Modifier.height(24.dp)) }

        item {
            SectionTitle("Support")
            SettingRow(icon = Icons.Outlined.HelpOutline, title = "Help Center")
            SettingRow(icon = Icons.Outlined.PrivacyTip, title = "Privacy Policy")
        }

        item { Spacer(modifier = Modifier.height(32.dp)) }

        // --- LOGOUT BUTTON ---
        item {
            OutlinedButton(
                onClick = {
                    scope.launch {
                        WorkManager.getInstance(context).cancelAllWork()
                        supabase.auth.signOut()
                        onLogoutSuccess()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    MaterialTheme.colorScheme.error.copy(alpha = 0.5f)
                )
            ) {
                Icon(Icons.Outlined.ExitToApp, contentDescription = "Log out")
                Spacer(modifier = Modifier.width(8.dp))
                Text("Log Out", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        }

        item { Spacer(modifier = Modifier.height(120.dp)) }
    }
}

// -------------------------------------------------------------
// CÁC THÀNH PHẦN UI PHỤ TRỢ
// -------------------------------------------------------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MeasurementEditSheetContent(
    currentProfile: Profile,
    onSave: (Int?, Int?, String?) -> Unit,
    onCancel: () -> Unit
) {
    var heightInput by remember { mutableStateOf(currentProfile.heightCm?.toString() ?: "") }
    var weightInput by remember { mutableStateOf(currentProfile.weightKg?.toString() ?: "") }
    var selectedShape by remember { mutableStateOf(currentProfile.bodyShape ?: "Rectangle") }

    val bodyShapes = listOf("Rectangle", "Triangle", "Hourglass", "Inverted Triangle", "Oval")
    var expanded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Edit Measurements",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 24.dp)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedTextField(
                value = heightInput,
                onValueChange = { heightInput = it },
                label = { Text("Height (cm)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp)
            )

            OutlinedTextField(
                value = weightInput,
                onValueChange = { weightInput = it },
                label = { Text("Weight (kg)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = !expanded },
            modifier = Modifier.fillMaxWidth()
        ) {
            OutlinedTextField(
                value = selectedShape,
                onValueChange = {},
                readOnly = true,
                label = { Text("Body Shape") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .menuAnchor()
                    .fillMaxWidth()
            )
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                bodyShapes.forEach { shape ->
                    DropdownMenuItem(
                        text = { Text(shape) },
                        onClick = {
                            selectedShape = shape
                            expanded = false
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedButton(
                onClick = onCancel,
                modifier = Modifier
                    .weight(1f)
                    .height(50.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Cancel")
            }
            Button(
                onClick = {
                    onSave(heightInput.toIntOrNull(), weightInput.toIntOrNull(), selectedShape)
                },
                modifier = Modifier
                    .weight(1f)
                    .height(50.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Save Changes")
            }
        }

        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
fun SectionTitle(title: String) {
    Text(
        text = title,
        fontSize = 14.sp,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(bottom = 8.dp, start = 4.dp),
        letterSpacing = 1.sp
    )
}

@Composable
fun SettingRow(
    icon: ImageVector,
    title: String,
    subtitle: String? = null,
    onClick: () -> Unit = {}
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable { onClick() }
            .padding(vertical = 12.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(MaterialTheme.colorScheme.surface, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                title,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.primary
            )
            if (subtitle != null) {
                Text(subtitle, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Icon(
            Icons.Filled.ChevronRight,
            contentDescription = "Go",
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun SettingToggleRow(
    icon: ImageVector,
    title: String,
    isChecked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable { onCheckedChange(!isChecked) }
            .padding(vertical = 12.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(MaterialTheme.colorScheme.surface, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
        }
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            title,
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.weight(1f)
        )
        Switch(
            checked = isChecked,
            onCheckedChange = { onCheckedChange(it) },
            colors = SwitchDefaults.colors(
                checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                checkedTrackColor = MaterialTheme.colorScheme.secondary,
                uncheckedTrackColor = Color.Gray,
                uncheckedThumbColor = MaterialTheme.colorScheme.surface
            )
        )
    }
}