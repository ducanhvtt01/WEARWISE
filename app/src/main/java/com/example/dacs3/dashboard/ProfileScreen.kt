package com.example.dacs3.dashboard

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import com.canhub.cropper.CropImageContract
import com.canhub.cropper.CropImageContractOptions
import com.canhub.cropper.CropImageOptions
import com.canhub.cropper.CropImageView
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
import coil.compose.AsyncImage
import androidx.compose.ui.layout.ContentScale
import android.graphics.ImageDecoder
import android.net.Uri

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
    var showEditProfileSheet by remember { mutableStateOf(false) }
    var showImageSourceSheet by remember { mutableStateOf(false) }
    
    var tempImageUri by remember { mutableStateOf<Uri?>(null) }

    val measurementSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val styleSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val editProfileSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val imageSourceSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val userId = remember { supabase.auth.currentUserOrNull()?.id ?: "" }

    val itemsList by viewModel.clothingItems.collectAsState()
    val feedbackMap by viewModel.clothingFeedbackMap.collectAsState()

    // Lọc ra danh sách đồ "Needs Love"
    val deadItems = remember(itemsList, feedbackMap) {
        val formatter = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
        val todayDate = java.util.Date()
        itemsList.filter { item ->
            val rating = feedbackMap[item.id]
            if (rating == -1) return@filter true // Đồ bị Dislike
            
            // Hoặc đồ hơn 90 ngày chưa mặc
            val referenceDateString = item.lastWornDate ?: item.createdAt
            if (referenceDateString != null) {
                try {
                    val refDate = formatter.parse(referenceDateString.substring(0, 10))
                    if (refDate != null) {
                        val diffInMillies = kotlin.math.abs(todayDate.time - refDate.time)
                        val diffInDays = java.util.concurrent.TimeUnit.DAYS.convert(diffInMillies, java.util.concurrent.TimeUnit.MILLISECONDS)
                        diffInDays > 90
                    } else false
                } catch (e: Exception) { false }
            } else false
        }
    }
    
    var showDeadItemsSheet by remember { mutableStateOf(false) }

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

    val cropImageLauncher = rememberLauncherForActivityResult(CropImageContract()) { result ->
        if (result.isSuccessful) {
            val uri = result.uriContent
            uri?.let {
                val bitmap = if (Build.VERSION.SDK_INT < 28) {
                    @Suppress("DEPRECATION")
                    android.provider.MediaStore.Images.Media.getBitmap(context.contentResolver, it)
                } else {
                    val source = ImageDecoder.createSource(context.contentResolver, it)
                    ImageDecoder.decodeBitmap(source)
                }
                viewModel.uploadAvatar(context, userId, bitmap)
            }
        } else {
            val exception = result.error
            exception?.printStackTrace()
        }
    }

    // Launcher cho Gallery
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            cropImageLauncher.launch(
                CropImageContractOptions(
                    uri = it,
                    cropImageOptions = CropImageOptions(
                        guidelines = CropImageView.Guidelines.ON,
                        cropShape = CropImageView.CropShape.OVAL,
                        fixAspectRatio = true,
                        aspectRatioX = 1,
                        aspectRatioY = 1,
                        outputRequestWidth = 512,
                        outputRequestHeight = 512,
                        backgroundColor = 0x77000000.toInt(), // Màu nền mờ
                        cropMenuCropButtonTitle = "Save" // Đổi chữ nút Lưu
                    )
                )
            )
        }
    }

    // Launcher cho Camera
    val cameraSourceLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success && tempImageUri != null) {
            cropImageLauncher.launch(
                CropImageContractOptions(
                    uri = tempImageUri,
                    cropImageOptions = CropImageOptions(
                        guidelines = CropImageView.Guidelines.ON,
                        cropShape = CropImageView.CropShape.OVAL,
                        fixAspectRatio = true,
                        aspectRatioX = 1,
                        aspectRatioY = 1,
                        outputRequestWidth = 512,
                        outputRequestHeight = 512,
                        backgroundColor = 0x77000000.toInt(), // Màu nền mờ
                        cropMenuCropButtonTitle = "Save" // Đổi chữ nút Lưu
                    )
                )
            )
        }
    }

    // Hàm tạo URI tạm thời cho Camera
    fun createTempPictureUri(): Uri {
        val tempFile = java.io.File.createTempFile("avatar_capture", ".jpg", context.cacheDir).apply {
            createNewFile()
            deleteOnExit()
        }
        return androidx.core.content.FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            tempFile
        )
    }

    // --- HIỂN THỊ BOTTOM SHEET SỐ ĐO (MEASUREMENTS) ---
    if (showMeasurementSheet && userProfile != null) {
        ModalBottomSheet(
            onDismissRequest = { showMeasurementSheet = false },
            sheetState = measurementSheetState,
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
            sheetState = styleSheetState,
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

    // --- HIỂN THỊ BOTTOM SHEET CHỈNH SỬA HỒ SƠ ---
    if (showEditProfileSheet && userProfile != null) {
        ModalBottomSheet(
            onDismissRequest = { showEditProfileSheet = false },
            sheetState = editProfileSheetState,
            containerColor = MaterialTheme.colorScheme.background
        ) {
            EditProfileSheetContent(
                currentFullName = userProfile.fullName ?: "",
                onSave = { newName ->
                    viewModel.updateProfile(userId, newName)
                    showEditProfileSheet = false
                },
                onCancel = { showEditProfileSheet = false }
            )
        }
    }

    // --- HIỂN THỊ BOTTOM SHEET CHỌN NGUỒN ẢNH (CAMERA/GALLERY) ---
    if (showImageSourceSheet) {
        ModalBottomSheet(
            onDismissRequest = { showImageSourceSheet = false },
            sheetState = imageSourceSheetState,
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            ImageSourceSheetContent(
                onGalleryClick = {
                    showImageSourceSheet = false
                    galleryLauncher.launch("image/*")
                },
                onCameraClick = {
                    showImageSourceSheet = false
                    try {
                        val uri = createTempPictureUri()
                        tempImageUri = uri
                        cameraSourceLauncher.launch(uri)
                    } catch (e: Exception) {
                        e.printStackTrace()
                        android.widget.Toast.makeText(context, "Cannot open camera", android.widget.Toast.LENGTH_SHORT).show()
                    }
                }
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

    if (showDeadItemsSheet) {
        ModalBottomSheet(
            onDismissRequest = { showDeadItemsSheet = false },
            containerColor = MaterialTheme.colorScheme.background
        ) {
            DeadItemsSheetContent(
                deadItems = deadItems,
                feedbackMap = feedbackMap,
                onResetFeedback = { itemId -> viewModel.saveClothingFeedback(itemId, 0) }, // 0 là xóa feedback
                onClose = { showDeadItemsSheet = false }
            )
        }
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
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.secondaryContainer)
                            .clickable { showImageSourceSheet = true },
                        contentAlignment = Alignment.Center
                    ) {
                        if (userProfile?.avatarUrl != null) {
                            AsyncImage(
                                model = userProfile.avatarUrl,
                                contentDescription = "Profile Picture",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Text(
                                userProfile?.fullName?.firstOrNull()?.uppercase() ?: "A",
                                fontSize = 40.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }

                        // HIỂN THỊ LOADING KHI ĐANG UPLOAD
                        if (viewModel.isUpdating) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Color.Black.copy(alpha = 0.4f)),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(
                                    color = Color.White,
                                    modifier = Modifier.size(30.dp),
                                    strokeWidth = 2.dp
                                )
                            }
                        }
                        
                        // Overlay icon edit nhỏ
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Black.copy(alpha = 0.1f)),
                            contentAlignment = Alignment.BottomEnd
                        ) {
                            Icon(
                                imageVector = Icons.Filled.CameraAlt,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier
                                    .padding(8.dp)
                                    .size(20.dp)
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
                            onClick = { showEditProfileSheet = true },
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

        // --- CLOSET MANAGEMENT SECTION ---
        item {
            SectionTitle("Closet Management")
            
            SettingRow(
                icon = Icons.Outlined.HeartBroken,
                title = "Needs Love (${deadItems.size} items)",
                subtitle = "Items unworn for 3+ months or disliked",
                onClick = { showDeadItemsSheet = true }
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
// 00. BOTTOM SHEET CHỌN NGUỒN ẢNH (CAMERA/GALLERY)
// -------------------------------------------------------------
@Composable
fun ImageSourceSheetContent(
    onGalleryClick: () -> Unit,
    onCameraClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            "Change Profile Picture",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 24.dp)
        )
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            // Nút Gallery
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .clickable { onGalleryClick() }
                    .padding(16.dp)
            ) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.size(60.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Default.PhotoLibrary,
                            contentDescription = "Gallery",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text("Gallery", fontWeight = FontWeight.Medium)
            }

            // Nút Camera
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .clickable { onCameraClick() }
                    .padding(16.dp)
            ) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    modifier = Modifier.size(60.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Default.CameraAlt,
                            contentDescription = "Camera",
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text("Camera", fontWeight = FontWeight.Medium)
            }
        }
        Spacer(modifier = Modifier.height(24.dp))
    }
}

// -------------------------------------------------------------
// 0. CẬP NHẬT BOTTOM SHEET CHO "EDIT PROFILE"
// -------------------------------------------------------------
@Composable
fun EditProfileSheetContent(
    currentFullName: String,
    onSave: (String) -> Unit,
    onCancel: () -> Unit
) {
    var fullNameInput by remember { mutableStateOf(currentFullName) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Edit Profile",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 24.dp)
        )

        OutlinedTextField(
            value = fullNameInput,
            onValueChange = { fullNameInput = it },
            label = { Text("Full Name") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            singleLine = true
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
                onClick = { onSave(fullNameInput) },
                modifier = Modifier.weight(1f).height(50.dp),
                shape = RoundedCornerShape(12.dp),
                enabled = fullNameInput.isNotBlank()
            ) {
                Text("Save Changes")
            }
        }
        Spacer(modifier = Modifier.height(32.dp))
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
    var heightInput by remember { mutableStateOf(currentProfile.heightCm.let { if (it.isNaN()) "0" else it.toInt().toString() }) }
    var weightInput by remember { mutableStateOf(currentProfile.weightKg.let { if (it.isNaN()) "0" else it.toInt().toString() }) }
    var shoeSizeInput by remember { mutableStateOf(currentProfile.shoeSizeEu.toString()) }

    var selectedShape by remember {
        mutableStateOf(currentProfile.bodyShape.takeIf { !it.isNullOrBlank() } ?: "Rectangle")
    }
    var selectedSkinTone by remember {
        mutableStateOf(currentProfile.skinTone.takeIf { !it.isNullOrBlank() } ?: "Neutral")
    }
    var selectedTopSize by remember {
        mutableStateOf(currentProfile.topSize.takeIf { !it.isNullOrBlank() } ?: "M")
    }
    var selectedBottomSize by remember {
        mutableStateOf(currentProfile.bottomSize.takeIf { !it.isNullOrBlank() } ?: "M")
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
        remember { mutableStateListOf(*(currentProfile.favoriteStyles?.toTypedArray() ?: emptyArray())) }
    val selectedColors =
        remember { mutableStateListOf(*(currentProfile.favoriteColors?.toTypedArray() ?: emptyArray())) }

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
    
    Box(modifier = Modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = selectedOption,
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = { 
                Icon(
                    imageVector = if (expanded) Icons.Filled.ArrowDropUp else Icons.Filled.ArrowDropDown, 
                    contentDescription = null
                ) 
            },
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        )
        
        // Transparent overlay to capture clicks safely
        Surface(
            modifier = Modifier
                .matchParentSize()
                .clickable { expanded = true },
            color = Color.Transparent
        ) {}
        
        DropdownMenu(
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

// -------------------------------------------------------------
// 3. DEAD ITEMS SHEET (NEEDS LOVE)
// -------------------------------------------------------------
@Composable
fun DeadItemsSheetContent(
    deadItems: List<com.example.dacs3.connectDB.ClothingItem>,
    feedbackMap: Map<String, Int>,
    onResetFeedback: (String) -> Unit,
    onClose: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Needs Love ❤️",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        Text(
            text = "These items haven't been worn in 3 months or were disliked. Give them another chance!",
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 24.dp),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )

        if (deadItems.isEmpty()) {
            Box(modifier = Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                Text("Your closet is perfectly utilized! ✨", color = MaterialTheme.colorScheme.primary)
            }
        } else {
            androidx.compose.foundation.lazy.LazyColumn(
                modifier = Modifier.fillMaxWidth().heightIn(max = 400.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(deadItems.size) { index ->
                    val item = deadItems[index]
                    val isDisliked = feedbackMap[item.id] == -1
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp).fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier.size(60.dp).clip(RoundedCornerShape(8.dp)).background(Color.Gray),
                                contentAlignment = Alignment.Center
                            ) {
                                if (item.imageUrl.isNotEmpty()) {
                                    AsyncImage(
                                        model = item.imageUrl,
                                        contentDescription = item.clothes_name,
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop
                                    )
                                } else {
                                    Icon(Icons.Outlined.Checkroom, null, tint = Color.White)
                                }
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(item.clothes_name, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                if (isDisliked) {
                                    Text("Disliked 👎", color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                                } else {
                                    Text("Unworn > 3M 🕸️", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                                }
                            }
                            if (isDisliked) {
                                OutlinedButton(onClick = { item.id?.let { onResetFeedback(it) } }) {
                                    Text("Reset", fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(24.dp))
        Button(onClick = onClose, modifier = Modifier.fillMaxWidth().height(50.dp)) {
            Text("Done")
        }
        Spacer(modifier = Modifier.height(24.dp))
    }
}