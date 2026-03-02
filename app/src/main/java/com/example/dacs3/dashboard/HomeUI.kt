package com.example.dacs3.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt

// Import cho Weather, Time và Lottie
import androidx.lifecycle.viewmodel.compose.viewModel
import com.airbnb.lottie.compose.*
import com.example.dacs3.R
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.delay

// Bảng màu
val OffWhite = Color(0xFFFBFBFC)
val MidnightBlue = Color(0xFF1A237E)
val SilverMist = Color(0xFF8D99AE)
val LightGray = Color(0xFFF0F2F5)
val AccentTeal = Color(0xFF00BFA5)
val SoftTeal = Color(0xFFE0F2F1)
val SoftOrange = Color(0xFFFFF3E0)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeUI() {
    var selectedTab by remember { mutableIntStateOf(0) }
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        Scaffold(
            containerColor = OffWhite,
            bottomBar = {
                NavigationBar(
                    containerColor = Color.White,
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
                                selectedIconColor = MidnightBlue,
                                unselectedIconColor = SilverMist,
                                selectedTextColor = MidnightBlue,
                                unselectedTextColor = SilverMist,
                                indicatorColor = SoftTeal
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
                    0 -> DashboardContent() // Gọi giao diện Home chính
                    1 -> ClosetScreen()     // Gọi giao diện Tủ đồ (đảm bảo bạn đã có file ClosetScreen.kt)
                    2 -> StylistScreen()
                    3 -> ProfileScreen()
                }
            }
        }

        ExtendedFloatingActionButton(
            onClick = { /* TODO: Open Camera */ },
            containerColor = Color.Transparent,
            contentColor = Color.White,
            shape = CircleShape,
            elevation = FloatingActionButtonDefaults.elevation(0.dp),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = 110.dp, end = 16.dp)
                .offset { IntOffset(offsetX.roundToInt(), offsetY.roundToInt()) }
                .pointerInput(Unit) {
                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        offsetX += dragAmount.x
                        offsetY += dragAmount.y
                    }
                }
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .background(
                        brush = Brush.horizontalGradient(
                            colors = listOf(MidnightBlue, Color(0xFF3949AB))
                        ),
                        shape = CircleShape
                    )
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.CameraAlt,
                    contentDescription = "AI Scan"
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "AI Scan",
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardContent() {
    var selectedEvent by remember { mutableStateOf("Work") }
    var selectedMood by remember { mutableStateOf("Confident") }
    val uriHandler = LocalUriHandler.current

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp)
    ) {
        item {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 32.dp, bottom = 24.dp)
            ) {
                Column(
                    modifier = Modifier.weight(1f) // Giúp cột lời chào chiếm không gian còn lại
                ) {
                    Text(
                        text = "Good morning,",
                        fontSize = 14.sp,
                        color = SilverMist,
                        fontWeight = FontWeight.Medium,
                        letterSpacing = 0.5.sp
                    )
                    Text(
                        text = "Alexander \uD83D\uDC4B",
                        fontSize = 24.sp,
                        color = MidnightBlue,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                WeatherWidget()
            }
        }

        item {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MidnightBlue),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 28.dp)
                    .shadow(
                        elevation = 12.dp,
                        shape = RoundedCornerShape(24.dp),
                        spotColor = AccentTeal.copy(alpha = 0.5f)
                    )
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp)
                ) {
                    Column(
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = "SPONSORED",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = AccentTeal,
                            letterSpacing = 1.5.sp,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        Text(
                            text = "Autumn Collection '24",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                        Text(
                            text = "Get 20% off exclusively via WEARWISE.",
                            fontSize = 12.sp,
                            color = SilverMist,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )
                        Button(
                            onClick = { uriHandler.openUri("https://shopee.vn") },
                            colors = ButtonDefaults.buttonColors(containerColor = AccentTeal),
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 0.dp),
                            modifier = Modifier.height(36.dp)
                        ) {
                            Text(
                                text = "Shop Now",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = MidnightBlue
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(80.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color.White.copy(alpha = 0.15f))
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.LocalMall,
                            contentDescription = "Product",
                            tint = Color.White,
                            modifier = Modifier.size(36.dp)
                        )
                    }
                }
            }
        }

        item {
            Column(
                modifier = Modifier.padding(bottom = 28.dp)
            ) {
                Text(
                    text = "What's the plan today?",
                    fontSize = 15.sp,
                    color = MidnightBlue,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.horizontalScroll(rememberScrollState())
                ) {
                    FilterChip(
                        selected = true,
                        onClick = { },
                        label = { Text("Event: $selectedEvent", fontWeight = FontWeight.Medium) },
                        leadingIcon = { Icon(Icons.Outlined.WorkOutline, null, Modifier.size(18.dp)) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MidnightBlue,
                            selectedLabelColor = Color.White,
                            selectedLeadingIconColor = Color.White
                        ),
                        shape = RoundedCornerShape(12.dp),
                        border = null
                    )
                    FilterChip(
                        selected = true,
                        onClick = { },
                        label = { Text("Mood: $selectedMood", fontWeight = FontWeight.Medium) },
                        leadingIcon = { Icon(Icons.Outlined.Mood, null, Modifier.size(18.dp)) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = SoftTeal,
                            selectedLabelColor = MidnightBlue,
                            selectedLeadingIconColor = AccentTeal
                        ),
                        shape = RoundedCornerShape(12.dp),
                        border = null
                    )
                }
            }
        }

        item {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 16.dp)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(32.dp)
                        .background(SoftTeal, CircleShape)
                ) {
                    Icon(Icons.Filled.AutoAwesome, null, tint = AccentTeal, modifier = Modifier.size(18.dp))
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "AI Stylist Suggestion",
                    fontSize = 20.sp,
                    color = MidnightBlue,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        item {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(16.dp, RoundedCornerShape(24.dp), spotColor = LightGray)
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp)
                ) {
                    Text(
                        text = "Perfect match for a sunny workday feeling confident.",
                        fontSize = 14.sp,
                        color = SilverMist,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(bottom = 20.dp)
                    )

                    OutfitItemPlaceholder("White Oxford Shirt", Icons.Outlined.Checkroom, "From your closet", SoftTeal, AccentTeal)
                    Spacer(Modifier.height(12.dp))
                    OutfitItemPlaceholder("Navy Tailored Trousers", Icons.Filled.Checkroom, "From your closet", Color(0xFFE8EAF6), MidnightBlue)
                    Spacer(Modifier.height(12.dp))
                    OutfitItemPlaceholder("Brown Leather Loafers", Icons.Outlined.DirectionsWalk, "From your closet", SoftOrange, Color(0xFFFF9800))

                    Spacer(Modifier.height(24.dp))

                    Button(
                        onClick = { },
                        colors = ButtonDefaults.buttonColors(containerColor = MidnightBlue),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Outlined.Refresh, null, tint = Color.White, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Shuffle Outfit", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        item { Spacer(Modifier.height(36.dp)) }

        item {
            Text(
                text = "Virtual Closet Stats",
                fontSize = 20.sp,
                color = MidnightBlue,
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

        item { Spacer(Modifier.height(120.dp)) }
    }
}

@Composable
fun WeatherWidget(viewModel: WeatherViewModel = viewModel()) {
    LaunchedEffect(Unit) {
        viewModel.fetchWeather()
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

    // Logic chọn file Lottie
    val lottieResId = when {
        condition == "Clear" && isNight -> R.raw.night
        condition == "Clear" && !isNight -> R.raw.sunny
        condition == "Clouds" && isNight -> R.raw.cloudy_night
        condition == "Clouds" && !isNight -> R.raw.cloudy_day
        (condition == "Rain" || condition == "Drizzle") && isNight -> R.raw.rainy_night
        (condition == "Rain" || condition == "Drizzle") && !isNight -> R.raw.rainy_day
        condition == "Thunderstorm" -> R.raw.storm
        condition in listOf("Sand", "Dust", "Ash", "Haze", "Fog", "Mist", "Smoke", "Sand", "Squall", "Tornado") -> R.raw.sandy
        else -> if (isNight) R.raw.night else R.raw.sunny
    }

    // Màu nền linh hoạt
    val widgetBgColor = when {
        condition == "Clear" && !isNight -> SoftOrange
        isNight -> Color(0xFFE8EAF6)
        condition in listOf("Rain", "Drizzle", "Thunderstorm") -> Color(0xFFE0F7FA)
        else -> Color(0xFFE3F2FD)
    }

    val widgetTextColor = when {
        condition == "Clear" && !isNight -> Color(0xFFE65100)
        isNight -> Color(0xFF283593)
        condition in listOf("Rain", "Drizzle", "Thunderstorm") -> Color(0xFF006064)
        else -> Color(0xFF1565C0)
    }

    Column(
        modifier = Modifier.wrapContentWidth() // Giới hạn Row chỉ rộng vừa đủ nội dung
    ) {
        Column(
            horizontalAlignment = Alignment.End,
            modifier = Modifier.padding(end = 8.dp)
        ) {
            Text(
                text = cityName,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = MidnightBlue,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(5.dp))
            Text(
                text = "$timeStr • $dateStr",
                fontSize = 10.sp,
                color = SilverMist,
                fontWeight = FontWeight.Medium
            )
        }

        Spacer(modifier = Modifier.height(5.dp))

        // PHẦN CỤC THỜI TIẾT (Lồng thêm một Row nhỏ)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .shadow(
                    elevation = 6.dp,
                    shape = RoundedCornerShape(16.dp),
                    spotColor = Color.Gray.copy(alpha = 0.2f)
                )
                .background(
                    color = widgetBgColor,
                    shape = RoundedCornerShape(16.dp)
                )
                .padding(horizontal = 12.dp, vertical = 6.dp)
        ) {
            val composition by rememberLottieComposition(LottieCompositionSpec.RawRes(lottieResId))
            val progress by animateLottieCompositionAsState(
                composition = composition,
                iterations = LottieConstants.IterateForever
            )

            if (composition != null) {
                LottieAnimation(
                    composition = composition,
                    progress = { progress },
                    modifier = Modifier.size(36.dp)
                )
            } else {
                Icon(
                    imageVector = Icons.Filled.WbSunny,
                    contentDescription = null,
                    tint = Color(0xFFFF9800),
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(4.dp))

            Text(
                text = temperature,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 14.sp,
                color = widgetTextColor
            )
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
            .background(Color.White, RoundedCornerShape(16.dp))
            .padding(8.dp)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(56.dp)
                .background(iconBgColor, RoundedCornerShape(16.dp))
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconColor,
                modifier = Modifier.size(24.dp)
            )
        }

        Spacer(Modifier.width(16.dp))

        Column {
            Text(
                text = name,
                color = MidnightBlue,
                fontWeight = FontWeight.SemiBold,
                fontSize = 16.sp
            )
            Text(
                text = subtext,
                color = SilverMist,
                fontSize = 12.sp
            )
        }
    }
}

@Composable
fun StatCard(title: String, count: String) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp),
        modifier = Modifier.size(88.dp)
    ) {
        Column(
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxSize()
        ) {
            Text(
                text = count,
                fontSize = 28.sp,
                fontWeight = FontWeight.ExtraBold,
                color = AccentTeal
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = title,
                fontSize = 13.sp,
                color = MidnightBlue,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun previewHomeUI() {
    HomeUI()
}