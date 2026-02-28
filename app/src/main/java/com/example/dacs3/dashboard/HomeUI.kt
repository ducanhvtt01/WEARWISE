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
import androidx.compose.ui.platform.LocalUriHandler // THÊM IMPORT NÀY
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt

// Bảng màu đã được tinh chỉnh
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

    // State lưu trữ tọa độ X, Y khi kéo thả nút AI Scan
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }

    // Dùng Box bao bọc toàn bộ màn hình để nút có thể nổi lên trên và di chuyển
    Box(modifier = Modifier.fillMaxSize()) {

        // --- GIAO DIỆN CHÍNH (SCAFFOLD) ---
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
                    val icons = listOf(Icons.Outlined.Home, Icons.Outlined.Checkroom, Icons.Outlined.AutoAwesome, Icons.Outlined.Person)
                    val selectedIcons = listOf(Icons.Filled.Home, Icons.Filled.Checkroom, Icons.Filled.AutoAwesome, Icons.Filled.Person)

                    items.forEachIndexed { index, item ->
                        NavigationBarItem(
                            selected = selectedTab == index,
                            onClick = { selectedTab = index },
                            icon = {
                                Icon(
                                    imageVector = if (selectedTab == index) selectedIcons[index] else icons[index],
                                    contentDescription = item,
                                    modifier = if (selectedTab == index) Modifier.size(28.dp) else Modifier.size(24.dp)
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
            // --- KHU VỰC CHUYỂN ĐỔI MÀN HÌNH THEO TAB ---
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                when (selectedTab) {
                    0 -> DashboardContent() // Gọi giao diện Home chính
                    1 -> ClosetScreen()     // Gọi giao diện Tủ đồ
                    2 -> StylistScreen()    // Gọi giao diện AI Stylist
                    3 -> ProfileScreen()    // Gọi giao diện Profile
                }
            }
        }

        // --- NÚT AI SCAN (KÉO THẢ ĐƯỢC) ĐẶT NGOÀI SCAFFOLD ---
        ExtendedFloatingActionButton(
            onClick = { /* TODO: Mở Camera */ },
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
                },
            containerColor = Color.Transparent,
            contentColor = Color.White,
            shape = CircleShape,
            elevation = FloatingActionButtonDefaults.elevation(0.dp)
        ) {
            Row(
                modifier = Modifier
                    .background(
                        brush = Brush.horizontalGradient(
                            colors = listOf(MidnightBlue, Color(0xFF3949AB))
                        ),
                        shape = CircleShape
                    )
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Filled.CameraAlt, contentDescription = "AI Scan")
                Spacer(modifier = Modifier.width(8.dp))
                Text("AI Scan", fontWeight = FontWeight.Bold)
            }
        }
    }
}

// --- TÁCH GIAO DIỆN TRANG CHỦ RA THÀNH COMPOSABLE RIÊNG ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardContent() {
    var selectedEvent by remember { mutableStateOf("Work") }
    var selectedMood by remember { mutableStateOf("Confident") }

    // Trình xử lý mở link web
    val uriHandler = LocalUriHandler.current

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp)
    ) {
        item {
            // 1. HEADER (Lời chào + Thời tiết)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 32.dp, bottom = 24.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Good morning,",
                        fontSize = 14.sp,
                        color = SilverMist,
                        fontWeight = FontWeight.Medium,
                        letterSpacing = 0.5.sp
                    )
                    Text(
                        text = "Alexander \uD83D\uDC4B",
                        fontSize = 28.sp,
                        color = MidnightBlue,
                        fontWeight = FontWeight.Bold,
                    )
                }

                // Widget Thời tiết
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .background(SoftOrange, RoundedCornerShape(16.dp))
                        .padding(horizontal = 14.dp, vertical = 10.dp)
                ) {
                    Icon(Icons.Filled.WbSunny, contentDescription = "Sunny", tint = Color(0xFFFF9800), modifier = Modifier.size(22.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("26°C", fontWeight = FontWeight.Bold, color = Color(0xFFE65100))
                }
            }
        }

        // 2. THẺ QUẢNG CÁO (PROMO BANNER)
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 28.dp)
                    .shadow(12.dp, RoundedCornerShape(24.dp), spotColor = AccentTeal.copy(alpha = 0.5f)),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MidnightBlue) // Nền màu xanh đậm sang trọng
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Cột thông tin quảng cáo
                    Column(modifier = Modifier.weight(1f)) {
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
                            onClick = {
                                // Mở link mua hàng / Affiliate Link
                                uriHandler.openUri("https://shopee.vn")
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = AccentTeal),
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 0.dp),
                            modifier = Modifier.height(36.dp)
                        ) {
                            Text("Shop Now", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MidnightBlue)
                        }
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    // Khu vực chứa ảnh Sản phẩm (Dùng Icon làm placeholder)
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color.White.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Outlined.LocalMall, contentDescription = "Product", tint = Color.White, modifier = Modifier.size(36.dp))
                    }
                }
            }
        }

        item {
            // 3. Chips chọn Ngữ cảnh
            Column(modifier = Modifier.padding(bottom = 28.dp)) {
                Text(
                    text = "What's the plan today?",
                    fontSize = 15.sp,
                    color = MidnightBlue,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    FilterChip(
                        selected = true,
                        onClick = { },
                        label = { Text("Event: $selectedEvent", fontWeight = FontWeight.Medium) },
                        leadingIcon = { Icon(Icons.Outlined.WorkOutline, contentDescription = null, modifier = Modifier.size(18.dp)) },
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
                        leadingIcon = { Icon(Icons.Outlined.Mood, contentDescription = null, modifier = Modifier.size(18.dp)) },
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
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 16.dp)) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .background(SoftTeal, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Filled.AutoAwesome, contentDescription = "AI", tint = AccentTeal, modifier = Modifier.size(18.dp))
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
            // 4. Thẻ Gợi ý Outfit
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(elevation = 16.dp, shape = RoundedCornerShape(24.dp), spotColor = LightGray, ambientColor = LightGray),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Perfect match for a sunny workday feeling confident.",
                        fontSize = 14.sp,
                        color = SilverMist,
                        fontWeight = FontWeight.Normal,
                        modifier = Modifier.padding(bottom = 20.dp),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )

                    OutfitItemPlaceholder("White Oxford Shirt", Icons.Outlined.Checkroom, "From your closet", SoftTeal, AccentTeal)
                    Spacer(modifier = Modifier.height(12.dp))
                    OutfitItemPlaceholder("Navy Tailored Trousers", Icons.Filled.Checkroom, "From your closet", Color(0xFFE8EAF6), MidnightBlue)
                    Spacer(modifier = Modifier.height(12.dp))
                    OutfitItemPlaceholder("Brown Leather Loafers", Icons.Outlined.DirectionsWalk, "From your closet", SoftOrange, Color(0xFFFF9800))

                    Spacer(modifier = Modifier.height(24.dp))

                    // Nút Shuffle
                    Button(
                        onClick = { },
                        colors = ButtonDefaults.buttonColors(containerColor = MidnightBlue),
                        shape = RoundedCornerShape(16.dp),
                        contentPadding = PaddingValues(vertical = 16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Outlined.Refresh, contentDescription = null, modifier = Modifier.size(20.dp), tint = Color.White)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Shuffle Outfit", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(36.dp)) }

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
                    StatCard(title = stats[index].first, count = stats[index].second)
                }
            }
        }

        item { Spacer(modifier = Modifier.height(36.dp)) }

        item {
            Text(
                text = "My Profile",
                fontSize = 20.sp,
                color = MidnightBlue,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 16.dp)
            )
        }

        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(4.dp, RoundedCornerShape(24.dp), spotColor = LightGray),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .background(SoftTeal, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("A", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = AccentTeal)
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = "Alexander", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MidnightBlue)
                        Text(text = "alexander@style.com", fontSize = 14.sp, color = SilverMist)
                    }

                    IconButton(
                        onClick = { },
                        modifier = Modifier.background(LightGray, CircleShape)
                    ) {
                        Icon(Icons.Filled.ChevronRight, contentDescription = "Go", tint = MidnightBlue)
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(120.dp)) } // Spacer tránh bị che bởi FAB và Bottom Bar
    }
}

// Cập nhật lại OutfitItemPlaceholder có màu nền Icon tùy biến
@Composable
fun OutfitItemPlaceholder(name: String, icon: androidx.compose.ui.graphics.vector.ImageVector, subtext: String, iconBgColor: Color, iconColor: Color) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White, RoundedCornerShape(16.dp))
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .background(iconBgColor, RoundedCornerShape(16.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(24.dp))
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column {
            Text(text = name, color = MidnightBlue, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
            Text(text = subtext, color = SilverMist, fontWeight = FontWeight.Normal, fontSize = 12.sp)
        }
    }
}

// Cập nhật lại StatCard
@Composable
fun StatCard(title: String, count: String) {
    Card(
        modifier = Modifier.size(88.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = count, fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, color = AccentTeal)
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = title, fontSize = 13.sp, color = MidnightBlue, fontWeight = FontWeight.Medium)
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewHomeUI() {
    HomeUI()
}