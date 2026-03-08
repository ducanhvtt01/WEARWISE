package com.example.dacs3.dashboard

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
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
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import com.google.ai.client.generativeai.type.generationConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.json.JSONObject // THÊM THƯ VIỆN NÀY ĐỂ PARSE JSON
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.Calendar
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeUI(isDarkMode: Boolean = false, onThemeChange: (Boolean) -> Unit = {}) {
    var selectedTab by remember { mutableIntStateOf(0) }
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }

    // --- CÁC BIẾN CHO AI SCAN TRẢ VỀ JSON ---
    val scope = rememberCoroutineScope()
    var isAiScanning by remember { mutableStateOf(false) }
    var aiScanResultText by remember { mutableStateOf<String?>(null) } // Để hiển thị lên màn hình
    var rawScannedJson by remember { mutableStateOf<JSONObject?>(null) } // Lưu data chuẩn để đẩy lên DB

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap ->
        if (bitmap != null) {
            isAiScanning = true
            scope.launch(Dispatchers.IO) {
                try {
                    val generativeModel = GenerativeModel(
                        modelName = "gemini-2.5-flash",
                        apiKey = "AIzaSyCYi0mC2bYHbxy3y1Ynv1xZNfoB5bOmge8",
                        generationConfig = generationConfig {
                            temperature = 0.2f // Giảm sáng tạo để kết quả JSON chính xác hơn
                            responseMimeType = "application/json" // ÉP AI PHẢI TRẢ VỀ ĐỊNH DẠNG JSON
                        }
                    )

                    // Câu lệnh chi tiết, bắt buộc trả về đúng cấu trúc bạn cần
                    val prompt = """
                        Analyze this clothing item in the image. 
                        Return strictly valid JSON with the following schema:
                        {
                            "name": "Brief brand and item name (e.g. Uniqlo Blue Shirt)",
                            "category": "Choose one: Top, Bottom, Shoes, Outerwear, Accessories",
                            "main_color": "Dominant color (e.g. Blue, Black, White)",
                            "seasons": ["Spring", "Summer", "Autumn", "Winter"],
                            "occasions": ["Casual", "Work", "Party", "Sport", "Formal"]
                        }
                    """.trimIndent()

                    val response = generativeModel.generateContent(
                        content {
                            image(bitmap)
                            text(prompt)
                        }
                    )

                    // Ép kiểu chuỗi trả về thành JSONObject
                    val jsonString = response.text ?: "{}"
                    val json = JSONObject(jsonString)

                    // Lưu lại json vào biến State để dùng khi bấm nút "Add to Closet"
                    rawScannedJson = json

                    // Trích xuất dữ liệu để hiển thị đẹp mắt lên UI
                    // Trích xuất dữ liệu cơ bản
                    val itemName = json.optString("name", "Unknown Item")
                    val itemCategory = json.optString("category", "N/A")
                    val itemColor = json.optString("main_color", "N/A")
                    val occasionsArray = json.optJSONArray("occasions")
                    val itemOccasions = if (occasionsArray != null && occasionsArray.length() > 0) {
                        List(occasionsArray.length()) { occasionsArray.getString(it) }.joinToString(", ")
                    } else {
                        "N/A"
                    }

                    val seasonsArray = json.optJSONArray("seasons")
                    val itemSeasons = if (seasonsArray != null && seasonsArray.length() > 0) {
                        List(seasonsArray.length()) { seasonsArray.getString(it) }.joinToString(", ")
                    } else {
                        "N/A"
                    }

                    // Đưa toàn bộ lên giao diện hiển thị
                    aiScanResultText = "$itemName\n• Category: $itemCategory\n• Color: $itemColor\n• Seasons: $itemSeasons\n• Occasions: $itemOccasions"

                } catch (e: Exception) {
                    aiScanResultText = "AI Scan Error: ${e.localizedMessage}"
                    rawScannedJson = null
                } finally {
                    isAiScanning = false
                }
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            bottomBar = {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 8.dp,
                    modifier = Modifier.height(84.dp).clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                ) {
                    val items = listOf("Home", "Closet", "Stylist", "Profile")
                    val icons = listOf(Icons.Outlined.Home, Icons.Outlined.Checkroom, Icons.Outlined.AutoAwesome, Icons.Outlined.Person)
                    val selectedIcons = listOf(Icons.Filled.Home, Icons.Filled.Checkroom, Icons.Filled.AutoAwesome, Icons.Filled.Person)

                    items.forEachIndexed { index, item ->
                        NavigationBarItem(
                            selected = selectedTab == index,
                            onClick = { selectedTab = index },
                            icon = { Icon(if (selectedTab == index) selectedIcons[index] else icons[index], contentDescription = item, modifier = Modifier.size(if (selectedTab == index) 28.dp else 24.dp)) },
                            label = { Text(item, fontSize = 12.sp, fontWeight = if (selectedTab == index) FontWeight.SemiBold else FontWeight.Normal) },
                            colors = NavigationBarItemDefaults.colors(selectedIconColor = MaterialTheme.colorScheme.primary, unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant, selectedTextColor = MaterialTheme.colorScheme.primary, unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant, indicatorColor = MaterialTheme.colorScheme.secondaryContainer)
                        )
                    }
                }
            }
        ) { innerPadding ->
            Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
                when (selectedTab) {
                    0 -> DashboardContent()
                    1 -> ClosetScreen()
                    2 -> StylistScreen()
                    3 -> ProfileScreen(isDarkMode = isDarkMode, onThemeChange = onThemeChange)
                }
            }
        }

        // --- HỘP THOẠI HIỂN THỊ KẾT QUẢ AI SCAN ---
        if (isAiScanning || aiScanResultText != null) {
            AlertDialog(
                onDismissRequest = {
                    if (!isAiScanning) {
                        aiScanResultText = null
                        rawScannedJson = null
                    }
                },
                containerColor = MaterialTheme.colorScheme.surface,
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {

                        Icon(
                            imageVector = Icons.Default.Psychology,
                            contentDescription = "AI"
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        Text(
                            text = if (isAiScanning) "AI is analyzing..." else "AI Stylist Result",
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                },
                text = {
                    if (isAiScanning) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Spacer(modifier = Modifier.padding(top = 20.dp))
                            // Lottie 1
                            val comp1 by rememberLottieComposition(LottieCompositionSpec.RawRes(R.raw.ai_loading))
                            val prog1 by animateLottieCompositionAsState(comp1, iterations = LottieConstants.IterateForever)
                            if (comp1 != null) LottieAnimation(comp1, { prog1 }, modifier = Modifier.size(120.dp))

                            // Lottie 2
                            val comp2 by rememberLottieComposition(LottieCompositionSpec.RawRes(R.raw.loading3))
                            val prog2 by animateLottieCompositionAsState(comp2, iterations = LottieConstants.IterateForever)
                            if (comp2 != null) LottieAnimation(comp2, { prog2 }, modifier = Modifier.size(120.dp))
                        }
                    } else {
                        Text(
                            text = aiScanResultText ?: "No results found",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface,
                            lineHeight = 24.sp
                        )
                    }
                },
                confirmButton = {
                    if (!isAiScanning && rawScannedJson != null) {
                        Button(
                            onClick = {
                                // 1. BÓC TÁCH DỮ LIỆU RA TỪ JSON
                                val json = rawScannedJson!!

                                // Chuyển đổi mảng JSON thành Danh sách Kotlin (List<String>)
                                val seasonsArray = json.optJSONArray("seasons")
                                val seasonsList = List(seasonsArray?.length() ?: 0) { seasonsArray?.getString(it) ?: "" }

                                val occasionsArray = json.optJSONArray("occasions")
                                val occasionsList = List(occasionsArray?.length() ?: 0) { occasionsArray?.getString(it) ?: "" }

                                // 2. TẠO ĐỐI TƯỢNG CLOTHING ITEM CỦA BẠN SẴN SÀNG LƯU VÀO DB
                                /*
                                val newClothingItem = ClothingItem(
                                    userId = "MÃ_USER_CỦA_BẠN_TỪ_SUPABASE",
                                    imageUrl = "LINK_ẢNH_SAU_KHI_UPLOAD",
                                    category = json.optString("category", "Unknown"),
                                    mainColor = json.optString("main_color", "Unknown"),
                                    seasons = seasonsList,
                                    occasions = occasionsList
                                )

                                // TODO: Gọi hàm Supabase insert object newClothingItem vào database tại đây!
                                */

                                // 3. Dọn dẹp Hộp thoại
                                aiScanResultText = null
                                rawScannedJson = null
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Text("Add to Closet")
                        }
                    }
                },
                dismissButton = {
                    if (!isAiScanning) {
                        TextButton(onClick = {
                            aiScanResultText = null
                            rawScannedJson = null
                        }) {
                            Text("Close", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            )
        }

        // --- NÚT FAB MÁY ẢNH AI ---
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = 110.dp, end = 16.dp)
                .offset { IntOffset(offsetX.roundToInt(), offsetY.roundToInt()) }
                .shadow(8.dp, CircleShape)
                .background(brush = Brush.horizontalGradient(colors = listOf(MaterialTheme.colorScheme.secondary, MaterialTheme.colorScheme.primary)), shape = CircleShape)
                .clip(CircleShape)
                .clickable { cameraLauncher.launch(null) }
                .pointerInput(Unit) {
                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        offsetX += dragAmount.x
                        offsetY += dragAmount.y
                    }
                }
                .padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            Icon(Icons.Filled.CameraAlt, "AI Scan", tint = if (isDarkMode) Color.DarkGray else Color.White)
            Spacer(modifier = Modifier.width(8.dp))
            Text("AI Scan", fontWeight = FontWeight.Bold, color = if (isDarkMode) Color.DarkGray else Color.White)
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
                Column(modifier = Modifier.weight(1f)) {
                    val currentHour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)

                    val greetingText = when (currentHour) {
                        in 5..11 -> "Good morning, ☀️"
                        in 12..17 -> "Good afternoon, 🌤️"
                        in 18..22 -> "Good evening, 🌙"
                        else -> "Up late, ✨"
                    }

                    Text(
                        text = greetingText,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Đức Anh",
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
                        .shadow(
                            4.dp,
                            RoundedCornerShape(16.dp),
                            spotColor = MaterialTheme.colorScheme.error.copy(alpha = 0.3f)
                        )
                        .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(16.dp))
                        .clickable {
                            val intent = android.content.Intent(
                                android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                            ).apply {
                                data =
                                    android.net.Uri.fromParts("package", context.packageName, null)
                            }
                            context.startActivity(intent)
                        }
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Icon(
                        Icons.Filled.LocationOff,
                        "Location Off",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        "Enable Location",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        } else {
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