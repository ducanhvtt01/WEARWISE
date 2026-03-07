package com.example.dacs3.dashboard

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.airbnb.lottie.compose.*
import com.example.dacs3.R
import com.example.dacs3.login.ui.theme.MidnightBlue
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.delay
import java.util.Calendar
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeUI(isDarkMode: Boolean = false, onThemeChange: (Boolean) -> Unit = {}) {
    var selectedTab by remember { mutableIntStateOf(0) }
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            bottomBar = {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 8.dp,
                    modifier = Modifier
                        .height(84.dp)
                        .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                ) {
                    val items = listOf("Home", "Closet", "Stylist", "Profile")
                    val icons = listOf(
                        Icons.Outlined.Home,
                        Icons.Outlined.Checkroom,
                        Icons.Outlined.AutoAwesome,
                        Icons.Outlined.Person
                    )
                    val selectedIcons = listOf(
                        Icons.Filled.Home,
                        Icons.Filled.Checkroom,
                        Icons.Filled.AutoAwesome,
                        Icons.Filled.Person
                    )

                    items.forEachIndexed { index, item ->
                        NavigationBarItem(
                            selected = selectedTab == index,
                            onClick = { selectedTab = index },
                            icon = {
                                Icon(
                                    imageVector = if (selectedTab == index) selectedIcons[index] else icons[index],
                                    contentDescription = item,
                                    modifier = Modifier.size(if (selectedTab == index) 28.dp else 24.dp)
                                )
                            },
                            label = {
                                Text(
                                    text = item,
                                    fontSize = 12.sp,
                                    fontWeight = if (selectedTab == index) FontWeight.SemiBold else FontWeight.Normal
                                )
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.primary,
                                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                selectedTextColor = MaterialTheme.colorScheme.primary,
                                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                indicatorColor = MaterialTheme.colorScheme.secondaryContainer
                            )
                        )
                    }
                }
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                when (selectedTab) {
                    0 -> DashboardContent()
                    1 -> ClosetScreen()
                    2 -> StylistScreen()
                    // Truyền tiếp vào ProfileScreen
                    3 -> ProfileScreen(isDarkMode = isDarkMode, onThemeChange = onThemeChange)
                }
            }
        }

        // --- NÚT FAB MÁY ẢNH AI (Tự custom để xóa viền đen) ---
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = 110.dp, end = 16.dp)
                .offset { IntOffset(offsetX.roundToInt(), offsetY.roundToInt()) }
                .shadow(8.dp, CircleShape) // Tạo bóng đổ cho nút nổi lên
                .background(
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.secondary,
                            MaterialTheme.colorScheme.primary
                        )
                    ),
                    shape = CircleShape
                )
                .clip(CircleShape) // Bo tròn hiệu ứng bấm (ripple)
                .clickable { /* TODO: Mở Camera Scan đồ */ }
                .pointerInput(Unit) {
                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        offsetX += dragAmount.x
                        offsetY += dragAmount.y
                    }
                }
                .padding(horizontal = 20.dp, vertical = 16.dp) // Căn chỉnh lề trong
        ) {
            if (isDarkMode == false) {
                Icon(Icons.Filled.CameraAlt, "AI Scan", tint = Color.White)
                Spacer(modifier = Modifier.width(8.dp))
                Text("AI Scan", fontWeight = FontWeight.Bold, color = Color.White)
            } else {
                Icon(Icons.Filled.CameraAlt, "AI Scan", tint = Color.DarkGray)
                Spacer(modifier = Modifier.width(8.dp))
                Text("AI Scan", fontWeight = FontWeight.Bold, color = Color.DarkGray)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardContent() {
    var selectedEvent by remember { mutableStateOf("University") }
    var selectedMood by remember { mutableStateOf("Confident") }

    var showMoodSheet by remember { mutableStateOf(false) }
    var showEventSheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var isLocationDenied by remember { mutableStateOf(false) }

    val haptic = LocalHapticFeedback.current
    val uriHandler = LocalUriHandler.current

    val eventCategories = mapOf(
        "Academic & Work" to listOf("University", "Presentation", "Library", "Internship"),
        "Social & Dating" to listOf("Coffee Date", "Dinner Date", "Party", "First Date"),
        "Active & Trip" to listOf("Gym Session", "Weekend Trip", "Hiking", "Beach Day")
    )

    val moodCategories = mapOf(
        "Energetic" to listOf("Confident", "Productive", "Bold", "Creative"),
        "Relaxed" to listOf("Chill", "Cozy", "Peaceful", "Effortless"),
        "Emotional" to listOf("Elegant", "Romantic", "Nostalgic", "Edgy")
    )

    if (showEventSheet) {
        ModalBottomSheet(
            onDismissRequest = { showEventSheet = false },
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.background
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 8.dp)
            ) {
                item {
                    Text(
                        "Select your occasion",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                }
                eventCategories.forEach { (category, events) ->
                    item {
                        Text(
                            category.uppercase(),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                    }
                    item {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.padding(bottom = 24.dp)
                        ) {
                            events.chunked(2).forEach { rowEvents ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    rowEvents.forEach { event ->
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .clip(RoundedCornerShape(12.dp))
                                                .background(if (selectedEvent == event) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface)
                                                .clickable {
                                                    selectedEvent = event
                                                    showEventSheet = false
                                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                }
                                                .padding(vertical = 12.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                event,
                                                color = if (selectedEvent == event) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary,
                                                fontWeight = if (selectedEvent == event) FontWeight.Bold else FontWeight.Medium,
                                                fontSize = 13.sp
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                item { Spacer(modifier = Modifier.height(40.dp)) }
            }
        }
    }

    if (showMoodSheet) {
        ModalBottomSheet(
            onDismissRequest = { showMoodSheet = false },
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.background
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 8.dp)
            ) {
                item {
                    Text(
                        "Select your feeling!",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                }

                moodCategories.forEach { (category, moods) ->
                    item {
                        Text(
                            category.uppercase(),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                    }
                    item {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.padding(bottom = 24.dp)
                        ) {
                            moods.chunked(2).forEach { rowMoods ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    rowMoods.forEach { mood ->
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .clip(RoundedCornerShape(12.dp))
                                                .background(if (selectedMood == mood) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface)
                                                .clickable {
                                                    selectedMood = mood
                                                    showMoodSheet = false
                                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                }
                                                .padding(vertical = 12.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                mood,
                                                color = if (selectedMood == mood) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary,
                                                fontWeight = if (selectedMood == mood) FontWeight.Bold else FontWeight.Medium,
                                                fontSize = 13.sp
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                item { Spacer(modifier = Modifier.height(40.dp)) }
            }
        }
    }

    LazyColumn(modifier = Modifier
        .fillMaxSize()
        .padding(horizontal = 24.dp)) {
        item {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 32.dp, bottom = 24.dp)
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    // 1. Lấy giờ hiện tại (từ 0 đến 23)
                    val currentHour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)

                    // 2. Dùng when để tạo câu chào tương ứng
                    val greetingText = when (currentHour) {
                        in 5..11 -> "Good morning, ☀️"
                        in 12..17 -> "Good afternoon, 🌤️"
                        in 18..22 -> "Good evening, 🌙"
                        else -> "Up late, ✨"
                    }

                    // 3. Hiển thị câu chào
                    Text(
                        text = greetingText,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Đức Anh", // Xoá icon vẫy tay ở đây vì đã có ở trên
                        fontSize = 26.sp,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                WeatherWidget(
                    isLocationDenied = isLocationDenied,
                    onDeniedChange = { isLocationDenied = it })
            }
        }

        item {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 28.dp)
                    .shadow(
                        12.dp,
                        RoundedCornerShape(24.dp),
                        spotColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.5f)
                    )
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp)
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "SPONSORED",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.secondary,
                            letterSpacing = 1.5.sp,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        Text(
                            "Autumn Collection '24",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                        Text(
                            "Get 20% off exclusively via WEARWISE.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )
                        Button(
                            onClick = { uriHandler.openUri("https://shopee.vn") },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 0.dp),
                            modifier = Modifier.height(36.dp)
                        ) {
                            Text(
                                "Shop Now",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(80.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.15f))
                    ) {
                        Icon(
                            Icons.Outlined.LocalMall,
                            "Product",
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(36.dp)
                        )
                    }
                }
            }
        }

        item {
            Column(modifier = Modifier.padding(bottom = 28.dp)) {
                Text(
                    "What's the plan today?",
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
                FilterChip(
                    selected = true,
                    onClick = { showEventSheet = true },
                    label = { Text("Event: $selectedEvent", fontWeight = FontWeight.Medium) },
                    leadingIcon = {
                        Icon(
                            Icons.Outlined.Event,
                            null,
                            modifier = Modifier.size(18.dp)
                        )
                    },
                    trailingIcon = {
                        Icon(
                            Icons.Filled.KeyboardArrowDown,
                            null,
                            modifier = Modifier.size(18.dp)
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                        selectedLeadingIconColor = MaterialTheme.colorScheme.onPrimary,
                        selectedTrailingIconColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    shape = RoundedCornerShape(12.dp), border = null
                )

                Spacer(modifier = Modifier.height(16.dp))

                Column(modifier = Modifier.padding(bottom = 5.dp)) {
                    Text(
                        "How do you feel today?",
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                    FilterChip(
                        selected = true,
                        onClick = { showMoodSheet = true },
                        label = { Text("Mood: $selectedMood", fontWeight = FontWeight.Medium) },
                        leadingIcon = {
                            Icon(
                                Icons.Outlined.Mood,
                                null,
                                modifier = Modifier.size(18.dp)
                            )
                        },
                        trailingIcon = {
                            Icon(
                                Icons.Filled.KeyboardArrowDown,
                                null,
                                modifier = Modifier.size(18.dp)
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                            selectedLeadingIconColor = MaterialTheme.colorScheme.onPrimary,
                            selectedTrailingIconColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        shape = RoundedCornerShape(12.dp), border = null
                    )
                }
            }
        }

        item {
            if (isLocationDenied) {
                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(bottom = 16.dp)
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(32.dp)
                                .background(
                                    MaterialTheme.colorScheme.secondaryContainer,
                                    CircleShape
                                )
                        ) {
                            Icon(
                                Icons.Filled.AutoAwesome,
                                null,
                                tint = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            "AI Stylist Suggestion",
                            fontSize = 20.sp,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(5.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Icons.Default.Warning,
                            null,
                            modifier = Modifier.size(24.dp),
                            tint = MaterialTheme.colorScheme.error
                        )
                        Text(
                            "Allow location for the best AI outfit tips!",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.error,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                }
            } else {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(bottom = 16.dp)
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(32.dp)
                            .background(MaterialTheme.colorScheme.secondaryContainer, CircleShape)
                    ) {
                        Icon(
                            Icons.Filled.AutoAwesome,
                            null,
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        "AI Stylist Suggestion",
                        fontSize = 20.sp,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        item {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(
                        16.dp,
                        RoundedCornerShape(24.dp),
                        spotColor = MaterialTheme.colorScheme.surfaceVariant
                    )
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp)
                ) {
                    Text(
                        "Perfect match for a $selectedMood $selectedEvent feeling.",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(bottom = 20.dp)
                    )

                    OutfitItemPlaceholder(
                        "White Oxford Shirt",
                        Icons.Outlined.Checkroom,
                        "From your closet",
                        MaterialTheme.colorScheme.secondaryContainer,
                        MaterialTheme.colorScheme.secondary
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutfitItemPlaceholder(
                        "Navy Tailored Trousers",
                        Icons.Filled.Checkroom,
                        "From your closet",
                        MaterialTheme.colorScheme.surfaceVariant,
                        MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutfitItemPlaceholder(
                        "Brown Leather Loafers",
                        Icons.Outlined.DirectionsWalk,
                        "From your closet",
                        MaterialTheme.colorScheme.tertiaryContainer,
                        Color(0xFFFF9800)
                    )
                    Spacer(modifier = Modifier.height(24.dp))

                    Button(
                        onClick = { haptic.performHapticFeedback(HapticFeedbackType.LongPress) },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            Icons.Outlined.Refresh,
                            null,
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "Shuffle Outfit",
                            color = MaterialTheme.colorScheme.onPrimary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(36.dp)) }

        item {
            Text(
                "Virtual Closet Stats",
                fontSize = 20.sp,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 16.dp)
            )
        }

        item {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                val stats = listOf("Tops" to "12", "Bottoms" to "8", "Shoes" to "5", "Accs" to "10")
                items(stats.size) { index ->
                    StatCard(stats[index].first, stats[index].second)
                }
            }
        }

        item { Spacer(modifier = Modifier.height(120.dp)) }
    }
}

@Composable
fun WeatherWidget(
    viewModel: WeatherViewModel = viewModel(),
    isLocationDenied: Boolean,
    onDeniedChange: (Boolean) -> Unit
) {
    val context = LocalContext.current

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val isGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true ||
                permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true

        if (isGranted) {
            onDeniedChange(false)
            LocationHelper.getCurrentLocation(
                context = context,
                onLocationFetched = { lat, lon ->
                    viewModel.fetchWeatherByLocation(
                        context,
                        lat,
                        lon
                    )
                },
                onFailed = { viewModel.fetchWeather() }
            )
        } else {
            onDeniedChange(true)
            viewModel.fetchWeather()
        }
    }

    LaunchedEffect(Unit) {
        val hasCoarse = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val hasFine = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        if (hasCoarse || hasFine) {
            onDeniedChange(false)
            LocationHelper.getCurrentLocation(
                context = context,
                onLocationFetched = { lat, lon ->
                    viewModel.fetchWeatherByLocation(
                        context,
                        lat,
                        lon
                    )
                },
                onFailed = { viewModel.fetchWeather() }
            )
        } else {
            onDeniedChange(true)
            locationPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                    Manifest.permission.ACCESS_FINE_LOCATION
                )
            )
        }
    }

    val temperature by viewModel.temperature.collectAsState()
    val condition by viewModel.condition.collectAsState()
    val isNight by viewModel.isNight.collectAsState()
    val cityName by viewModel.cityName.collectAsState()

    var currentTime by remember { mutableLongStateOf(System.currentTimeMillis()) }

    LaunchedEffect(Unit) {
        while (true) {
            delay(60000L)
            currentTime = System.currentTimeMillis()
        }
    }

    val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
    val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
    val timeStr = timeFormat.format(Date(currentTime))
    val dateStr = dateFormat.format(Date(currentTime))

    val lottieResId = when {
        condition == "Clear" && isNight -> R.raw.night
        condition == "Clear" && !isNight -> R.raw.sunny
        condition == "Clouds" && isNight -> R.raw.cloudy_night
        condition == "Clouds" && !isNight -> R.raw.cloudy_day
        (condition == "Rain" || condition == "Drizzle") && isNight -> R.raw.rainy_night
        (condition == "Rain" || condition == "Drizzle") && !isNight -> R.raw.rainy_day
        condition == "Thunderstorm" -> R.raw.storm
        condition in listOf(
            "Sand",
            "Dust",
            "Ash",
            "Haze",
            "Fog",
            "Mist",
            "Smoke",
            "Sand",
            "Squall",
            "Tornado"
        ) -> R.raw.sandy

        else -> if (isNight) R.raw.night else R.raw.sunny
    }

    val widgetBgColor = when {
        condition == "Clear" && !isNight -> MaterialTheme.colorScheme.tertiaryContainer
        isNight -> MaterialTheme.colorScheme.surfaceVariant
        condition in listOf("Rain", "Drizzle", "Thunderstorm") -> Color(0xFFE0F7FA)
        else -> MaterialTheme.colorScheme.surfaceVariant
    }

    val widgetTextColor = when {
        condition == "Clear" && !isNight -> Color(0xFFE65100)
        isNight -> MaterialTheme.colorScheme.primary
        condition in listOf("Rain", "Drizzle", "Thunderstorm") -> Color(0xFF006064)
        else -> MaterialTheme.colorScheme.primary
    }

    Column(
        modifier = Modifier.wrapContentWidth(),
        horizontalAlignment = Alignment.End
    ) {
        Column(
            horizontalAlignment = Alignment.End,
            modifier = Modifier.padding(end = 8.dp)
        ) {
            Text(
                text = if (isLocationDenied) "Loading..." else cityName,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(5.dp))
            Text(
                text = "$timeStr • $dateStr",
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Medium
            )
        }

        Spacer(modifier = Modifier.height(5.dp))

        if (isLocationDenied) {
            Column(horizontalAlignment = Alignment.End) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .shadow(4.dp, RoundedCornerShape(16.dp), spotColor = MaterialTheme.colorScheme.error.copy(alpha = 0.3f))
                        .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(16.dp))
                        .clickable {
                            // --- THAY ĐỔI TẠI ĐÂY ---
                            // Tạo Intent để mở thẳng trang Cài đặt (App Info) của ứng dụng này
                            val intent = android.content.Intent(
                                android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                            ).apply {
                                data = android.net.Uri.fromParts("package", context.packageName, null)
                            }
                            context.startActivity(intent)
                        }
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Icon(Icons.Filled.LocationOff, "Location Off", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Enable Location", fontWeight = FontWeight.ExtraBold, fontSize = 13.sp, color = MaterialTheme.colorScheme.primary)
                }
            }
        }
        else {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .shadow(
                        6.dp,
                        RoundedCornerShape(16.dp),
                        spotColor = Color.Gray.copy(alpha = 0.2f)
                    )
                    .background(widgetBgColor, RoundedCornerShape(16.dp))
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                val composition by rememberLottieComposition(
                    LottieCompositionSpec.RawRes(
                        lottieResId
                    )
                )
                val progress by animateLottieCompositionAsState(
                    composition,
                    iterations = LottieConstants.IterateForever
                )

                if (composition != null) {
                    LottieAnimation(composition, { progress }, modifier = Modifier.size(36.dp))
                } else {
                    Icon(
                        Icons.Filled.WbSunny,
                        null,
                        tint = Color(0xFFFF9800),
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(modifier = Modifier.width(4.dp))

                Text(
                    temperature,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 14.sp,
                    color = widgetTextColor
                )
            }
        }
    }
}

@Composable
fun OutfitItemPlaceholder(
    name: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    subtext: String,
    iconBgColor: Color,
    iconColor: Color
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(16.dp))
            .padding(8.dp)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(56.dp)
                .background(iconBgColor, RoundedCornerShape(16.dp))
        ) {
            Icon(icon, null, tint = iconColor, modifier = Modifier.size(24.dp))
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column {
            Text(
                name,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold,
                fontSize = 16.sp
            )
            Text(subtext, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
        }
    }
}

@Composable
fun StatCard(title: String, count: String) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(2.dp),
        modifier = Modifier.size(88.dp)
    ) {
        Column(
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxSize()
        ) {
            Text(
                count,
                fontSize = 28.sp,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.secondary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                title,
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Medium
            )
        }
    }
}