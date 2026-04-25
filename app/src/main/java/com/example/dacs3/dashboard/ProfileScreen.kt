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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
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

    val sharedPrefs =
        remember { context.getSharedPreferences("WearwisePrefs", Context.MODE_PRIVATE) }

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
    var showStyleSheet by remember { mutableStateOf(false) } // State mới cho Style Preferences

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val userId = remember { supabase.auth.currentUserOrNull()?.id ?: "" }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                val hasSystemPermission = checkNotificationPermission(context)
                val wantsEnabledInApp = sharedPrefs.getBoolean("is_notif_enabled", false)

                if (!hasSystemPermission && wantsEnabledInApp) {
                    notificationsEnabled = false
                    sharedPrefs.edit().putBoolean("is_notif_enabled", false).apply()
                    WorkManager.getInstance(context).cancelUniqueWork("WeatherMonitorTask")
                    WorkManager.getInstance(context).cancelUniqueWork("DailyOutfitTask")
                } else if (hasSystemPermission && !wantsEnabledInApp) {
                    notificationsEnabled = true
                    sharedPrefs.edit().putBoolean("is_notif_enabled", true).apply()
                    WorkerSetupHelper.setupWeatherMonitor(context)
                    WorkerSetupHelper.setupDailyOutfitNotification(context)
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

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

    // --- HIỂN THỊ BOTTOM SHEET SỐ ĐO (MEASUREMENTS) ---
    if (showMeasurementSheet && userProfile != null) {
        ModalBottomSheet(
            onDismissRequest = { showMeasurementSheet = false },
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.background
        ) {
            MeasurementEditSheetContent(
                currentProfile = userProfile,
                onSave = { h, w, shape, skin, top, bottom, shoe ->
                    if (h != null && w != null && shoe != null) {
                        viewModel.updateMeasurements(userId, h, w, shape, skin, top, bottom, shoe)
                    }
                    showMeasurementSheet = false
                },
                onCancel = { showMeasurementSheet = false }
            )
        }
    }

    // --- HIỂN THỊ BOTTOM SHEET SỞ THÍCH (STYLE PREFERENCES) ---
    if (showStyleSheet && userProfile != null) {
        ModalBottomSheet(
            onDismissRequest = { showStyleSheet = false },
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.background
        ) {
            StylePreferencesSheetContent(
                currentProfile = userProfile,
                onSave = { styles, colors ->
                    viewModel.updateStylePreferences(userId, styles, colors)
                    showStyleSheet = false
                },
                onCancel = { showStyleSheet = false }
            )
        }
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

            // Tóm tắt Measurements
            val measureSubtitle = if (userProfile != null) {
                "${userProfile.heightCm.toInt()}cm, ${userProfile.weightKg.toInt()}kg, ${userProfile.bodyShape}\n" +
                        "Sizes: Top ${userProfile.topSize}, Bottom ${userProfile.bottomSize}, Shoe EU ${userProfile.shoeSizeEu}"
            } else "Loading..."

            SettingRow(
                icon = Icons.Outlined.Straighten,
                title = "My Measurements",
                subtitle = measureSubtitle,
                onClick = { showMeasurementSheet = true }
            )

            // Tóm tắt Style Preferences
            val styleSubtitle = if (userProfile != null) {
                val styles =
                    if (userProfile.favoriteStyles.isNotEmpty()) userProfile.favoriteStyles.joinToString(
                        ", "
                    ) else "None"
                val colors =
                    if (userProfile.favoriteColors.isNotEmpty()) userProfile.favoriteColors.joinToString(
                        ", "
                    ) else "None"
                "Styles: $styles\nColors: $colors"
            } else "Loading..."

            SettingRow(
                icon = Icons.Outlined.Style,
                title = "Style Preferences",
                subtitle = styleSubtitle,
                onClick = { showStyleSheet = true } // Bấm vào để mở bảng chọn Style
            )
        }

        item { Spacer(modifier = Modifier.height(24.dp)) }

        // --- APP SETTINGS SECTION ---
        item {
            SectionTitle("App Settings")

            SettingToggleRow(
                icon = Icons.Outlined.Notifications,
                title = "Notifications",
                isChecked = notificationsEnabled,
                onCheckedChange = { isTurningOn ->
                    if (isTurningOn) {
                        if (checkNotificationPermission(context)) {
                            notificationsEnabled = true
                            sharedPrefs.edit().putBoolean("is_notif_enabled", true).apply()
                            WorkerSetupHelper.setupWeatherMonitor(context)
                            WorkerSetupHelper.setupDailyOutfitNotification(context)
                        } else {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            } else {
                                showSettingsDialog = true
                            }
                        }
                    } else {
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
// 1. CẬP NHẬT BOTTOM SHEET CHO "MY MEASUREMENTS"
// -------------------------------------------------------------
@Composable
fun MeasurementEditSheetContent(
    currentProfile: Profile,
    onSave: (Int?, Int?, String, String, String, String, Int?) -> Unit,
    onCancel: () -> Unit
) {
    var heightInput by remember { mutableStateOf(currentProfile.heightCm.toInt().toString()) }
    var weightInput by remember { mutableStateOf(currentProfile.weightKg.toInt().toString()) }
    var shoeSizeInput by remember { mutableStateOf(currentProfile.shoeSizeEu.toString()) }

    var selectedShape by remember {
        mutableStateOf(currentProfile.bodyShape.takeIf { it.isNotBlank() } ?: "Rectangle")
    }
    var selectedSkinTone by remember {
        mutableStateOf(currentProfile.skinTone.takeIf { it.isNotBlank() } ?: "Neutral")
    }
    var selectedTopSize by remember {
        mutableStateOf(currentProfile.topSize.takeIf { it.isNotBlank() } ?: "M")
    }
    var selectedBottomSize by remember {
        mutableStateOf(currentProfile.bottomSize.takeIf { it.isNotBlank() } ?: "M")
    }

    val scrollState = rememberScrollState() // Thêm thanh cuộn vì nội dung dài

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 16.dp)
            .verticalScroll(scrollState),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Edit Measurements",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 24.dp)
        )

        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
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

        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Box(modifier = Modifier.weight(1f)) {
                DropdownSelector(
                    "Body Shape",
                    listOf("Rectangle", "Triangle", "Hourglass", "Inverted Triangle", "Oval"),
                    selectedShape
                ) { selectedShape = it }
            }
            Box(modifier = Modifier.weight(1f)) {
                DropdownSelector(
                    "Skin Tone",
                    listOf("Cool", "Warm", "Neutral", "Fair", "Medium", "Dark"),
                    selectedSkinTone
                ) { selectedSkinTone = it }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Box(modifier = Modifier.weight(1f)) {
                DropdownSelector(
                    "Top Size",
                    listOf("XS", "S", "M", "L", "XL", "XXL"),
                    selectedTopSize
                ) { selectedTopSize = it }
            }
            Box(modifier = Modifier.weight(1f)) {
                DropdownSelector(
                    "Bottom Size",
                    listOf("28", "29", "30", "31", "32", "34", "36"),
                    selectedBottomSize
                ) { selectedBottomSize = it }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = shoeSizeInput,
            onValueChange = { shoeSizeInput = it },
            label = { Text("Shoe Size (EU)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        )

        Spacer(modifier = Modifier.height(32.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            OutlinedButton(
                onClick = onCancel,
                modifier = Modifier.weight(1f).height(50.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Cancel")
            }
            Button(
                onClick = {
                    onSave(
                        heightInput.toIntOrNull(),
                        weightInput.toIntOrNull(),
                        selectedShape,
                        selectedSkinTone,
                        selectedTopSize,
                        selectedBottomSize,
                        shoeSizeInput.toIntOrNull()
                    )
                },
                modifier = Modifier.weight(1f).height(50.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Save Changes")
            }
        }
        Spacer(modifier = Modifier.height(32.dp))
    }
}

// -------------------------------------------------------------
// 2. TẠO MỚI BOTTOM SHEET CHO "STYLE PREFERENCES"
// -------------------------------------------------------------
@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun StylePreferencesSheetContent(
    currentProfile: Profile,
    onSave: (List<String>, List<String>) -> Unit,
    onCancel: () -> Unit
) {
    val availableStyles = listOf(
        "Smart Casual",
        "Minimalist",
        "Streetwear",
        "Vintage",
        "Athleisure",
        "Classic",
        "Grunge"
    )
    val availableColors =
        listOf("Black", "White", "Navy", "Beige", "Gray", "Red", "Blue", "Green", "Earth Tones")

    // Dùng mutableStateListOf để dễ thêm/xoá item khi người dùng click
    val selectedStyles =
        remember { mutableStateListOf(*currentProfile.favoriteStyles.toTypedArray()) }
    val selectedColors =
        remember { mutableStateListOf(*currentProfile.favoriteColors.toTypedArray()) }

    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 16.dp)
            .verticalScroll(scrollState),
    ) {
        Text(
            text = "Style Preferences",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 24.dp).align(Alignment.CenterHorizontally)
        )

        Text(
            "Favorite Styles",
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            availableStyles.forEach { style ->
                FilterChip(
                    selected = selectedStyles.contains(style),
                    onClick = {
                        if (selectedStyles.contains(style)) selectedStyles.remove(style)
                        else selectedStyles.add(style)
                    },
                    label = { Text(style) }
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            "Favorite Colors",
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            availableColors.forEach { color ->
                FilterChip(
                    selected = selectedColors.contains(color),
                    onClick = {
                        if (selectedColors.contains(color)) selectedColors.remove(color)
                        else selectedColors.add(color)
                    },
                    label = { Text(color) }
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            OutlinedButton(
                onClick = onCancel,
                modifier = Modifier.weight(1f).height(50.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Cancel")
            }
            Button(
                onClick = { onSave(selectedStyles.toList(), selectedColors.toList()) },
                modifier = Modifier.weight(1f).height(50.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Save Changes")
            }
        }
        Spacer(modifier = Modifier.height(32.dp))
    }
}

// -------------------------------------------------------------
// UI PHỤ TRỢ
// -------------------------------------------------------------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DropdownSelector(
    label: String,
    options: List<String>,
    selectedOption: String,
    onOptionSelected: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
        modifier = Modifier.fillMaxWidth()
    ) {
        OutlinedTextField(
            value = selectedOption,
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.menuAnchor().fillMaxWidth()
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option) },
                    onClick = {
                        onOptionSelected(option)
                        expanded = false
                    }
                )
            }
        }
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