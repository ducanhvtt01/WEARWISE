package com.example.dacs3.dashboard

import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.dacs3.connectDB.DashboardViewModel
import com.example.dacs3.ui.components.GlassCard
import com.example.dacs3.connectDB.OutfitScheduleWithDetails
import com.example.dacs3.connectDB.OutfitDbModel
import com.example.dacs3.connectDB.OutfitItemDbModel
import com.example.dacs3.connectDB.ClothingItem
import com.example.dacs3.connectDB.supabase
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun CalendarScreen(
    viewModel: DashboardViewModel,
    onBack: () -> Unit = {}
) {
    val scope = rememberCoroutineScope()
    val userId = supabase.auth.currentUserOrNull()?.id ?: ""
    val todayDateStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

    // Tải lịch trình và quần áo khi bắt đầu màn hình
    LaunchedEffect(userId) {
        if (userId.isNotEmpty()) {
            viewModel.fetchOutfitSchedules(userId)
            viewModel.getClothingItems(userId)
        }
    }

    val schedules by viewModel.outfitSchedules.collectAsState()
    val clothesList by viewModel.clothingItems.collectAsState()

    var selectedDateStr by remember { mutableStateOf(todayDateStr) }
    val calendar = remember { Calendar.getInstance() }

    // Lấy WeatherViewModel phục vụ AI Smart Suggest
    val weatherViewModel: WeatherViewModel = viewModel()
    val currentTemp by weatherViewModel.temperature.collectAsState()
    val currentCondition by weatherViewModel.condition.collectAsState()
    val tomorrowTemp by weatherViewModel.tomorrowTemperature.collectAsState()
    val tomorrowCondition by weatherViewModel.tomorrowCondition.collectAsState()

    // Tải thông tin thời tiết
    LaunchedEffect(userId) {
        if (userId.isNotEmpty()) {
            weatherViewModel.fetchWeather()
        }
    }

    // Trạng thái cho hộp thoại lên lịch
    var showScheduleDialog by remember { mutableStateOf(false) }
    var savedOutfitsList by remember { mutableStateOf<List<OutfitDbModel>>(emptyList()) }
    var isLoadingOutfits by remember { mutableStateOf(false) }
    var selectedOutfitToSchedule by remember { mutableStateOf<OutfitDbModel?>(null) }

    // Các biến trạng thái tìm kiếm, lọc và danh sách liên kết outfit_items
    var searchQuery by remember { mutableStateOf("") }
    var selectedOccasionFilter by remember { mutableStateOf("All") }
    var showCleanOnly by remember { mutableStateOf(false) }
    var outfitItemsList by remember { mutableStateOf<List<OutfitItemDbModel>>(emptyList()) }

    // Các biến trạng thái bổ sung cho chế độ tạo Outfit mới trực tiếp trong Dialog
    var isCreatingNewOutfit by remember { mutableStateOf(false) }
    var newOutfitName by remember { mutableStateOf("") }
    var newOutfitOccasion by remember { mutableStateOf("") }
    val selectedClothingItems = remember { mutableStateListOf<String>() }
    var clothingSearchQuery by remember { mutableStateOf("") }
    var selectedClothingCategory by remember { mutableStateOf("All") }

    // Tạo danh sách các ngày trong tuần
    val weeklyDates = remember {
        val list = mutableListOf<Date>()
        val cal = Calendar.getInstance()
        // Bắt đầu từ 3 ngày trước đến 11 ngày tiếp theo
        cal.add(Calendar.DAY_OF_YEAR, -3)
        for (i in 0..14) {
            list.add(cal.time)
            cal.add(Calendar.DAY_OF_YEAR, 1)
        }
        list
    }

    // Lọc lịch trình theo ngày đã chọn
    val schedulesForSelectedDate = schedules.filter { it.schedule.plannedDate == selectedDateStr }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp)
        ) {
            // Tiêu đề
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(90.dp)
                    .padding(top = 40.dp, bottom = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .size(40.dp)
                        .background(MaterialTheme.colorScheme.surface, CircleShape)
                ) {
                    Icon(
                        Icons.Default.ArrowBack,
                        contentDescription = "Back",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    text = "Calendar & Outfit Diary",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Lịch tuần cuộn ngang
            Text(
                text = "Select Date",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
            ) {
                items(weeklyDates, key = { date -> date.time }) { date ->
                    val dayFormat = SimpleDateFormat("EEE", Locale.US)
                    val dateFormat = SimpleDateFormat("dd", Locale.US)
                    val fullFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)

                    val dateStr = fullFormat.format(date)
                    val isSelected = dateStr == selectedDateStr
                    val isToday = dateStr == todayDateStr

                    val dayName = dayFormat.format(date)
                    val dayNum = dateFormat.format(date)

                    val textColor = if (isSelected) MaterialTheme.colorScheme.onPrimary
                                    else MaterialTheme.colorScheme.onSurface

                    Box(
                        modifier = Modifier
                            .width(60.dp)
                            .height(80.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(
                                if (isSelected) {
                                    Brush.verticalGradient(
                                        listOf(
                                            MaterialTheme.colorScheme.primary,
                                            MaterialTheme.colorScheme.secondary.copy(alpha = 0.85f)
                                        )
                                    )
                                } else if (isToday) {
                                    SolidColor(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
                                } else {
                                    SolidColor(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
                                }
                            )
                            .border(
                                width = 1.dp,
                                color = if (isSelected) Color.Transparent
                                        else if (isToday) MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
                                        else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
                                shape = RoundedCornerShape(16.dp)
                            )
                            .clickable {
                                selectedDateStr = dateStr
                            }
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxSize()
                        ) {
                            Text(
                                text = dayName,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = textColor.copy(alpha = 0.7f)
                            )
                            Text(
                                text = dayNum,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = textColor
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Khu vực chi tiết / lịch trình
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Planned Outfits",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                Text(
                    text = SimpleDateFormat("MMMM dd, yyyy", Locale.US).format(
                        SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(selectedDateStr) ?: Date()
                    ),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (schedulesForSelectedDate.isEmpty()) {
                // Trạng thái trống cho ngày được chọn
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(vertical = 16.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f))
                        .border(
                            1.dp,
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f),
                            RoundedCornerShape(24.dp)
                        )
                        .clickable {
                            // Tải danh sách outfit và danh sách liên kết outfit_items từ Supabase
                            isLoadingOutfits = true
                            showScheduleDialog = true
                            scope.launch(Dispatchers.IO) {
                                try {
                                    val list = supabase.from("outfits")
                                        .select { filter { eq("user_id", userId) } }
                                        .decodeList<OutfitDbModel>()
                                    val outfitIds = list.mapNotNull { it.id }
                                    val itemsList = if (outfitIds.isNotEmpty()) {
                                        supabase.from("outfit_items")
                                            .select { filter { isIn("outfit_id", outfitIds) } }
                                            .decodeList<OutfitItemDbModel>()
                                    } else {
                                        emptyList()
                                    }
                                    withContext(Dispatchers.Main) {
                                        savedOutfitsList = list
                                        outfitItemsList = itemsList
                                        isLoadingOutfits = false
                                    }
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                    withContext(Dispatchers.Main) {
                                        isLoadingOutfits = false
                                    }
                                }
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(24.dp)
                    ) {
                        Icon(
                            Icons.Outlined.EventNote,
                            contentDescription = "No Outfit planned",
                            modifier = Modifier.size(56.dp),
                            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "No outfit scheduled for this day",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Tap to plan your outfit ahead and stay organized.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = {
                                isLoadingOutfits = true
                                showScheduleDialog = true
                                scope.launch(Dispatchers.IO) {
                                    try {
                                        val list = supabase.from("outfits")
                                            .select { filter { eq("user_id", userId) } }
                                            .decodeList<OutfitDbModel>()
                                        val outfitIds = list.mapNotNull { it.id }
                                        val itemsList = if (outfitIds.isNotEmpty()) {
                                            supabase.from("outfit_items")
                                                .select { filter { isIn("outfit_id", outfitIds) } }
                                                .decodeList<OutfitItemDbModel>()
                                        } else {
                                            emptyList()
                                        }
                                        withContext(Dispatchers.Main) {
                                            savedOutfitsList = list
                                            outfitItemsList = itemsList
                                            isLoadingOutfits = false
                                        }
                                    } catch (e: Exception) {
                                        e.printStackTrace()
                                        withContext(Dispatchers.Main) {
                                            isLoadingOutfits = false
                                        }
                                    }
                                }
                            },
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Filled.Add, null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Plan Outfit", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    contentPadding = PaddingValues(bottom = 80.dp)
                ) {
                    items(schedulesForSelectedDate, key = { it.schedule.id ?: "" }) { scheduleDetails ->
                        OutfitScheduleCard(
                            scheduleDetails = scheduleDetails,
                            isToday = selectedDateStr == todayDateStr,
                            onConfirmWorn = {
                                viewModel.confirmWornOutfit(
                                    schedule = scheduleDetails.schedule,
                                    userId = userId,
                                    onSuccess = {
                                        // Hiển thị thông báo thành công
                                    }
                                )
                            },
                            onDelete = {
                                scheduleDetails.schedule.id?.let { scheduleId ->
                                    viewModel.deleteOutfitSchedule(scheduleId, userId)
                                }
                            }
                        )
                    }
                }
            }
        } // kết thúc Column

        // Nút FAB để thêm lịch phối đồ nhanh khi đã có lịch trình trong ngày
        if (schedulesForSelectedDate.isNotEmpty()) {
            FloatingActionButton(
                onClick = {
                    isLoadingOutfits = true
                    showScheduleDialog = true
                    scope.launch(Dispatchers.IO) {
                        try {
                            val list = supabase.from("outfits")
                                .select { filter { eq("user_id", userId) } }
                                .decodeList<OutfitDbModel>()
                            val outfitIds = list.mapNotNull { it.id }
                            val itemsList = if (outfitIds.isNotEmpty()) {
                                supabase.from("outfit_items")
                                    .select { filter { isIn("outfit_id", outfitIds) } }
                                    .decodeList<OutfitItemDbModel>()
                            } else {
                                emptyList()
                            }
                            withContext(Dispatchers.Main) {
                                savedOutfitsList = list
                                outfitItemsList = itemsList
                                isLoadingOutfits = false
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                            withContext(Dispatchers.Main) {
                                isLoadingOutfits = false
                            }
                        }
                    }
                },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(bottom = 90.dp, end = 24.dp),
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(Icons.Filled.Add, contentDescription = "Plan Outfit")
            }
        }
    } // kết thúc Box

    // Hộp thoại lên lịch phối đồ thông minh tích hợp AI Smart Suggest & Quick Filters
    if (showScheduleDialog) {
        val isSelectedToday = selectedDateStr == todayDateStr
        val tomorrowDateStr = remember {
            val cal = Calendar.getInstance()
            cal.add(Calendar.DAY_OF_YEAR, 1)
            SimpleDateFormat("yyyy-MM-dd", Locale.US).format(cal.time)
        }
        val isSelectedTomorrow = selectedDateStr == tomorrowDateStr

        val targetTempStr = if (isSelectedToday) currentTemp else if (isSelectedTomorrow) tomorrowTemp else "25°C"
        val targetCondition = if (isSelectedToday) currentCondition else if (isSelectedTomorrow) tomorrowCondition else "Clear"
        val tempVal = targetTempStr.replace(Regex("[^0-9-]"), "").toIntOrNull() ?: 25

        // Thuật toán đề xuất AI Smart Suggest
        val aiRecommendedOutfits = remember(savedOutfitsList, outfitItemsList, clothesList, targetTempStr, targetCondition, selectedDateStr) {
            savedOutfitsList.filter { outfit ->
                val outfitClothes = outfitItemsList.filter { it.outfitId == outfit.id }
                    .mapNotNull { item -> clothesList.find { it.id == item.clothingId } }
                // Đề xuất outfit sạch 100% (không chứa đồ bẩn)
                outfitClothes.isNotEmpty() && outfitClothes.all { it.status.uppercase() !in listOf("WORN", "IN_WASH", "NEED_IRON") }
            }.map { outfit ->
                val outfitClothes = outfitItemsList.filter { it.outfitId == outfit.id }
                    .mapNotNull { item -> clothesList.find { it.id == item.clothingId } }

                // 1. Trọng số Recency (Đồ lâu chưa mặc nhất)
                val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
                val currentTimeMs = System.currentTimeMillis()
                var maxWornTimeMs = 0L
                var hasWornDate = false
                for (item in outfitClothes) {
                    item.lastWornDate?.let { dateStr ->
                        try {
                            val date = sdf.parse(dateStr)
                            if (date != null) {
                                if (date.time > maxWornTimeMs) {
                                    maxWornTimeMs = date.time
                                }
                                hasWornDate = true
                            }
                        } catch (e: Exception) {}
                    }
                }
                val recencyScore = if (hasWornDate) {
                    val diffDays = (currentTimeMs - maxWornTimeMs) / (1000 * 60 * 60 * 24)
                    diffDays.toFloat().coerceAtMost(100f)
                } else {
                    100f // Ưu tiên cao nhất cho đồ chưa từng mặc
                }

                // 2. Trọng số Weather (Thời tiết hiện tại / ngày được chọn)
                var weatherScore = 0f
                val isWinterOutfit = outfit.season?.contains("Winter", ignoreCase = true) == true
                val isSummerOutfit = outfit.season?.contains("Summer", ignoreCase = true) == true
                val isSpringOutfit = outfit.season?.contains("Spring", ignoreCase = true) == true
                val isAutumnOutfit = outfit.season?.contains("Autumn", ignoreCase = true) == true

                if (tempVal < 18) { // Lạnh
                    if (isWinterOutfit) weatherScore += 40f
                    if (isAutumnOutfit) weatherScore += 20f
                    val hasWarmItems = outfitClothes.any {
                        it.category.contains("Jacket", ignoreCase = true) ||
                        it.category.contains("Coat", ignoreCase = true) ||
                        it.category.contains("Sweater", ignoreCase = true) ||
                        it.category.contains("Hoodie", ignoreCase = true)
                    }
                    if (hasWarmItems) weatherScore += 20f
                } else if (tempVal > 28) { // Nóng
                    if (isSummerOutfit) weatherScore += 40f
                    if (isSpringOutfit) weatherScore += 20f
                    val hasThickItems = outfitClothes.any {
                        it.category.contains("Coat", ignoreCase = true) ||
                        it.category.contains("Sweater", ignoreCase = true)
                    }
                    if (hasThickItems) weatherScore -= 40f
                } else { // Ôn hòa
                    if (isSpringOutfit || isAutumnOutfit) weatherScore += 40f
                    if (isSummerOutfit) weatherScore += 20f
                }

                // Điểm hỗ trợ trời mưa
                val isRainy = targetCondition.contains("Rain", ignoreCase = true) ||
                              targetCondition.contains("Drizzle", ignoreCase = true) ||
                              targetCondition.contains("Thunderstorm", ignoreCase = true)
                if (isRainy) {
                    val hasRainProtection = outfitClothes.any {
                        it.category.contains("Jacket", ignoreCase = true) ||
                        it.category.contains("Coat", ignoreCase = true) ||
                        it.clothes_name.contains("Rain", ignoreCase = true) ||
                        it.clothes_name.contains("Waterproof", ignoreCase = true)
                    }
                    if (hasRainProtection) weatherScore += 30f
                }

                // 3. Trọng số yêu thích (Favorite)
                val favoriteScore = if (outfit.isFavorite) 10f else 0f

                val totalScore = recencyScore + weatherScore + favoriteScore
                outfit to totalScore
            }.sortedByDescending { it.second }.map { it.first }.take(3)
        }

        // Lọc danh sách outfit theo bộ lọc thủ công
        val filteredOutfits = remember(savedOutfitsList, outfitItemsList, clothesList, searchQuery, selectedOccasionFilter, showCleanOnly) {
            savedOutfitsList.filter { outfit ->
                val outfitClothes = outfitItemsList.filter { it.outfitId == outfit.id }
                    .mapNotNull { item -> clothesList.find { it.id == item.clothingId } }
                val isClean = outfitClothes.all { it.status.uppercase() !in listOf("WORN", "IN_WASH", "NEED_IRON") }

                val matchesSearch = outfit.name.contains(searchQuery, ignoreCase = true) ||
                                    (outfit.description?.contains(searchQuery, ignoreCase = true) ?: false)
                val matchesOccasion = selectedOccasionFilter == "All" ||
                                      outfit.occasion?.split(",")?.any { tag ->
                                          tag.trim().equals(selectedOccasionFilter, ignoreCase = true)
                                      } == true
                val matchesClean = !showCleanOnly || isClean

                matchesSearch && matchesOccasion && matchesClean
            }
        }

        val dismissAction = {
            showScheduleDialog = false
            isCreatingNewOutfit = false
            newOutfitName = ""
            newOutfitOccasion = ""
            selectedClothingItems.clear()
            clothingSearchQuery = ""
            selectedClothingCategory = "All"
            selectedOutfitToSchedule = null
            searchQuery = ""
            selectedOccasionFilter = "All"
            showCleanOnly = false
        }

        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = dismissAction,
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.background,
            dragHandle = { BottomSheetDefaults.DragHandle() },
            modifier = Modifier.fillMaxHeight(0.95f)
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isCreatingNewOutfit) "Create New Outfit ✨" else "Select Outfit to Plan",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.primary,
                        letterSpacing = (-0.5).sp
                    )
                    IconButton(
                        onClick = dismissAction,
                        modifier = Modifier.background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), CircleShape)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                    // Content
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .padding(horizontal = 24.dp)
                    ) {
                        Spacer(modifier = Modifier.height(16.dp))
                    if (isCreatingNewOutfit) {
                        OutlinedTextField(
                            value = newOutfitName,
                            onValueChange = { newOutfitName = it },
                            label = { Text("Outfit Name") },
                            placeholder = { Text("e.g. Sunny Day Work, Party Night...") },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
                            )
                        )


                        // Occasion chips (select only)
                        Text(
                            text = "Occasion (Optional)",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = 6.dp)
                        )
                        val suggestions = listOf("Work", "Casual", "Party", "Sport", "Travel", "Wedding")
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 12.dp)
                        ) {
                            suggestions.forEach { sugg ->
                                val isSelected = newOutfitOccasion.trim().equals(sugg, ignoreCase = true)
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { newOutfitOccasion = if (isSelected) "" else sugg },
                                    label = { Text(sugg, fontSize = 12.sp) }
                                )
                            }
                        }


                        Text(
                            text = "Select Closet Items (${selectedClothingItems.size} selected)",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )

                        OutlinedTextField(
                            value = clothingSearchQuery,
                            onValueChange = { clothingSearchQuery = it },
                            placeholder = { Text("Search clothes...", fontSize = 13.sp) },
                            leadingIcon = { Icon(Icons.Default.Search, null, modifier = Modifier.size(18.dp)) },
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 8.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
                            )
                        )

                        val clothingCategories = listOf("All", "Tops", "Bottoms", "Shoes", "Outerwear", "Accessories")
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 12.dp)
                        ) {
                            items(clothingCategories) { cat ->
                                val isSelected = selectedClothingCategory == cat
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { selectedClothingCategory = cat },
                                    label = { Text(cat, fontSize = 11.sp) }
                                )
                            }
                        }

                        // Lọc danh sách quần áo của tủ đồ
                        val filteredClothes = remember(clothesList, clothingSearchQuery, selectedClothingCategory) {
                            clothesList.filter { item ->
                                val matchesSearch = item.clothes_name.contains(clothingSearchQuery, ignoreCase = true)
                                val matchesCategory = when (selectedClothingCategory) {
                                    "All" -> true
                                    "Tops" -> item.category.lowercase().run { contains("top") || contains("áo") || contains("shirt") }
                                    "Bottoms" -> item.category.lowercase().run { contains("bottom") || contains("quần") || contains("pants") }
                                    "Shoes" -> item.category.lowercase().run { contains("shoes") || contains("giày") }
                                    "Outerwear" -> item.category.lowercase().run { contains("outerwear") || contains("khoác") || contains("jacket") || contains("coat") }
                                    "Accessories" -> item.category.lowercase().run { contains("accessories") || contains("phụ kiện") || contains("hat") || contains("bag") || contains("belt") }
                                    else -> true
                                }
                                matchesSearch && matchesCategory
                            }
                        }

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            if (filteredClothes.isEmpty()) {
                                Text("No clothes found in closet.", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
                            } else {
                                val rows = filteredClothes.chunked(3)
                                LazyColumn(
                                    verticalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.fillMaxSize()
                                ) {
                                    items(rows) { rowItems ->
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            for (i in 0..2) {
                                                if (i < rowItems.size) {
                                                    val item = rowItems[i]
                                                    val isSelected = selectedClothingItems.contains(item.id)
                                                    Box(
                                                        modifier = Modifier
                                                            .weight(1f)
                                                            .aspectRatio(1f)
                                                            .clip(RoundedCornerShape(12.dp))
                                                            .background(
                                                                if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                                                                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                                                            )
                                                            .border(
                                                                width = if (isSelected) 2.dp else 1.dp,
                                                                color = if (isSelected) MaterialTheme.colorScheme.primary
                                                                        else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f),
                                                                shape = RoundedCornerShape(12.dp)
                                                            )
                                                            .clickable {
                                                                val itemId = item.id
                                                                if (itemId != null) {
                                                                    if (isSelected) {
                                                                        selectedClothingItems.remove(itemId)
                                                                    } else {
                                                                        selectedClothingItems.add(itemId)
                                                                    }
                                                                }
                                                            }
                                                    ) {
                                                        if (item.imageUrl.isNotEmpty()) {
                                                            AsyncImage(
                                                                model = item.imageUrl,
                                                                contentDescription = item.clothes_name,
                                                                contentScale = ContentScale.Crop,
                                                                modifier = Modifier.fillMaxSize()
                                                            )
                                                        } else {
                                                            Icon(
                                                                Icons.Default.Checkroom,
                                                                contentDescription = null,
                                                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                                                modifier = Modifier.align(Alignment.Center).size(24.dp)
                                                            )
                                                        }
                                                        if (isSelected) {
                                                            Box(
                                                                modifier = Modifier
                                                                    .align(Alignment.TopEnd)
                                                                    .padding(4.dp)
                                                                    .size(18.dp)
                                                                    .background(MaterialTheme.colorScheme.primary, CircleShape),
                                                                contentAlignment = Alignment.Center
                                                            ) {
                                                                Icon(
                                                                    Icons.Default.Check,
                                                                    contentDescription = null,
                                                                    tint = MaterialTheme.colorScheme.onPrimary,
                                                                    modifier = Modifier.size(12.dp)
                                                                )
                                                            }
                                                        }
                                                        Box(
                                                            modifier = Modifier
                                                                .align(Alignment.BottomCenter)
                                                                .fillMaxWidth()
                                                                .background(Color.Black.copy(alpha = 0.5f))
                                                                .padding(vertical = 2.dp, horizontal = 4.dp)
                                                        ) {
                                                            Text(
                                                                text = item.clothes_name,
                                                                color = Color.White,
                                                                fontSize = 8.sp,
                                                                maxLines = 1,
                                                                overflow = TextOverflow.Ellipsis,
                                                                textAlign = TextAlign.Center,
                                                                modifier = Modifier.fillMaxWidth()
                                                            )
                                                        }
                                                    }
                                                } else {
                                                    Spacer(modifier = Modifier.weight(1f))
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        // --- NORMAL SELECTION MODE ---
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 20.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.05f)),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Can't find the perfect fit?",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        text = "Create a brand new outfit from your closet.",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(top = 2.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Button(
                                    onClick = {
                                        isCreatingNewOutfit = true
                                        selectedClothingItems.clear()
                                        newOutfitName = ""
                                        newOutfitOccasion = if (selectedOccasionFilter != "All") selectedOccasionFilter else ""
                                    },
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = "Create New", modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Create", fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        // Ô tìm kiếm
                        TextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = { Text("Search outfits...", fontSize = 14.sp) },
                            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(20.dp)) },
                            trailingIcon = {
                                if (searchQuery.isNotEmpty()) {
                                    IconButton(onClick = { searchQuery = "" }) {
                                        Icon(Icons.Default.Clear, contentDescription = "Clear search", modifier = Modifier.size(16.dp))
                                    }
                                }
                            },
                            singleLine = true,
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 16.dp),
                            colors = TextFieldDefaults.colors(
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent,
                                disabledIndicatorColor = Color.Transparent,
                                errorIndicatorColor = Color.Transparent,
                                focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                            )
                        )

                        // Hàng ngang các bộ lọc nhanh (Filter Chips)
                        val dynamicOccasions = remember(savedOutfitsList) {
                            val fixedTags = listOf("All", "Work", "Casual", "Party", "Sport", "Travel", "Wedding", "Date", "Beach")
                            val fromDB = savedOutfitsList
                                .mapNotNull { it.occasion }
                                .flatMap { it.split(",") }
                                .map { it.trim() }
                                .filter { it.isNotBlank() }
                            (fixedTags + fromDB).distinct()
                        }
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 20.dp)
                        ) {
                            item {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(if (showCleanOnly) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                        .clickable { showCleanOnly = !showCleanOnly }
                                        .padding(horizontal = 14.dp, vertical = 8.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        if (showCleanOnly) {
                                            Icon(Icons.Default.Check, null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onPrimary)
                                            Spacer(modifier = Modifier.width(4.dp))
                                        }
                                        Text(
                                            "Clean Only 🧼",
                                            color = if (showCleanOnly) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                                            fontWeight = if (showCleanOnly) FontWeight.Bold else FontWeight.Medium,
                                            fontSize = 13.sp
                                        )
                                    }
                                }
                            }

                            items(dynamicOccasions) { occasion ->
                                val isSelected = selectedOccasionFilter == occasion
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                        .clickable { selectedOccasionFilter = occasion }
                                        .padding(horizontal = 14.dp, vertical = 8.dp)
                                ) {
                                    Text(
                                        occasion,
                                        color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        fontSize = 13.sp
                                    )
                                }
                            }
                        }

                        // Danh sách hiển thị chính
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isLoadingOutfits) {
                                CircularProgressIndicator()
                            } else if (savedOutfitsList.isEmpty()) {
                                Text(
                                    text = "No saved outfits found. Please save some outfits first from the Stylist tab!",
                                    textAlign = TextAlign.Center,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            } else {
                                LazyColumn(
                                    verticalArrangement = Arrangement.spacedBy(12.dp),
                                    modifier = Modifier.fillMaxSize()
                                ) {
                                    // Gợi ý AI (AI Smart Suggest)
                                    if (aiRecommendedOutfits.isNotEmpty() && searchQuery.isEmpty() && selectedOccasionFilter == "All") {
                                        item {
                                            Column(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(vertical = 4.dp)
                                                    .clip(RoundedCornerShape(16.dp))
                                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.05f))
                                                    .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.15f), RoundedCornerShape(16.dp))
                                                    .padding(12.dp)
                                            ) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    modifier = Modifier.fillMaxWidth()
                                                ) {
                                                    Icon(
                                                        Icons.Default.AutoAwesome,
                                                        contentDescription = "AI Suggestions",
                                                        tint = MaterialTheme.colorScheme.primary,
                                                        modifier = Modifier.size(18.dp)
                                                    )
                                                    Spacer(modifier = Modifier.width(8.dp))
                                                    Text(
                                                        text = "AI Smart Suggest ✨",
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 14.sp,
                                                        color = MaterialTheme.colorScheme.primary
                                                    )
                                                    Spacer(modifier = Modifier.weight(1f))
                                                    Text(
                                                        text = "$targetTempStr ($targetCondition)",
                                                        fontSize = 11.sp,
                                                        fontWeight = FontWeight.Medium,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }

                                                Spacer(modifier = Modifier.height(8.dp))

                                                Column(
                                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                                ) {
                                                    aiRecommendedOutfits.forEach { outfit ->
                                                        val isSelected = selectedOutfitToSchedule?.id == outfit.id
                                                        val outfitClothes = outfitItemsList.filter { it.outfitId == outfit.id }
                                                            .mapNotNull { item -> clothesList.find { it.id == item.clothingId } }
                                                        GlassCard(
                                                            cornerRadius = 16.dp,
                                                            alpha = if (isSelected) 0.25f else 0.05f,
                                                            borderAlpha = if (isSelected) 0.4f else 0.1f,
                                                            modifier = Modifier
                                                                .fillMaxWidth()
                                                                .clickable { selectedOutfitToSchedule = outfit }
                                                                .border(
                                                                    width = if (isSelected) 1.5.dp else 0.dp,
                                                                    color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                                                                    shape = RoundedCornerShape(16.dp)
                                                                )
                                                        ) {
                                                            Column(
                                                                modifier = Modifier
                                                                    .padding(12.dp)
                                                                    .fillMaxWidth()
                                                            ) {
                                                                Text(
                                                                    text = outfit.name,
                                                                    fontWeight = FontWeight.Bold,
                                                                    fontSize = 14.sp,
                                                                    color = MaterialTheme.colorScheme.onSurface
                                                                )

                                                                val details = mutableListOf<String>()
                                                                if (!outfit.occasion.isNullOrEmpty()) details.add(outfit.occasion)
                                                                if (!outfit.season.isNullOrEmpty()) details.add(outfit.season)

                                                                Text(
                                                                    text = details.joinToString(" | ") + " (Recommended)",
                                                                    fontSize = 11.sp,
                                                                    color = MaterialTheme.colorScheme.primary
                                                                )

                                                                Spacer(modifier = Modifier.height(10.dp))

                                                                LazyRow(
                                                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                                                    modifier = Modifier.fillMaxWidth()
                                                                ) {
                                                                    items(outfitClothes) { item ->
                                                                        Box(
                                                                            modifier = Modifier
                                                                                .size(36.dp)
                                                                                .clip(RoundedCornerShape(8.dp))
                                                                                .background(MaterialTheme.colorScheme.surfaceVariant),
                                                                            contentAlignment = Alignment.Center
                                                                        ) {
                                                                            if (item.imageUrl.isNotEmpty()) {
                                                                                AsyncImage(
                                                                                    model = item.imageUrl,
                                                                                    contentDescription = item.clothes_name,
                                                                                    contentScale = ContentScale.Crop,
                                                                                    modifier = Modifier.fillMaxSize()
                                                                                )
                                                                            } else {
                                                                                Icon(
                                                                                    Icons.Default.Checkroom,
                                                                                    contentDescription = null,
                                                                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                                                    modifier = Modifier.size(18.dp)
                                                                                )
                                                                            }
                                                                        }
                                                                    }
                                                                }
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }

                                    // Danh mục tất cả hoặc lọc
                                    item {
                                        Text(
                                            text = if (searchQuery.isNotEmpty() || selectedOccasionFilter != "All" || showCleanOnly) "Filtered Outfits" else "All Outfits",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                                        )
                                    }

                                    if (filteredOutfits.isEmpty()) {
                                        item {
                                            Column(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(vertical = 24.dp)
                                                    .clip(RoundedCornerShape(16.dp))
                                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f))
                                                    .border(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f), RoundedCornerShape(16.dp))
                                                    .padding(24.dp),
                                                horizontalAlignment = Alignment.CenterHorizontally
                                            ) {
                                                Icon(
                                                    Icons.Default.Checkroom,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(48.dp),
                                                    tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                                                )
                                                Spacer(modifier = Modifier.height(12.dp))
                                                Text(
                                                    text = "No outfits match your filters",
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 14.sp,
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )
                                                Spacer(modifier = Modifier.height(4.dp))
                                                Text(
                                                    text = "Create a new outfit to plan for this occasion.",
                                                    fontSize = 11.sp,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    textAlign = TextAlign.Center
                                                )
                                                Spacer(modifier = Modifier.height(16.dp))
                                                Button(
                                                    onClick = {
                                                        isCreatingNewOutfit = true
                                                        selectedClothingItems.clear()
                                                        newOutfitName = ""
                                                        newOutfitOccasion = if (selectedOccasionFilter != "All") selectedOccasionFilter else ""
                                                    },
                                                    shape = RoundedCornerShape(12.dp)
                                                ) {
                                                    Icon(Icons.Default.Add, null, modifier = Modifier.size(16.dp))
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Text("Create Outfit", fontWeight = FontWeight.Bold)
                                                }
                                            }
                                        }
                                    } else {
                                        items(filteredOutfits) { outfit ->
                                            val isSelected = selectedOutfitToSchedule?.id == outfit.id
                                            val outfitClothes = outfitItemsList.filter { it.outfitId == outfit.id }
                                                .mapNotNull { item -> clothesList.find { it.id == item.clothingId } }
                                            val isClean = outfitClothes.all { it.status.uppercase() !in listOf("WORN", "IN_WASH", "NEED_IRON") }

                                            GlassCard(
                                                cornerRadius = 16.dp,
                                                alpha = if (isSelected) 0.25f else 0.05f,
                                                borderAlpha = if (isSelected) 0.4f else 0.1f,
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clickable { selectedOutfitToSchedule = outfit }
                                                    .border(
                                                        width = if (isSelected) 2.dp else 0.dp,
                                                        color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                                                        shape = RoundedCornerShape(16.dp)
                                                    )
                                            ) {
                                                Column(
                                                    modifier = Modifier
                                                        .padding(16.dp)
                                                        .fillMaxWidth()
                                                ) {
                                                    Row(
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        modifier = Modifier.fillMaxWidth()
                                                    ) {
                                                        Text(
                                                            text = outfit.name,
                                                            fontWeight = FontWeight.Bold,
                                                            fontSize = 15.sp,
                                                            color = MaterialTheme.colorScheme.onSurface,
                                                            modifier = Modifier.weight(1f)
                                                        )

                                                        if (!isClean) {
                                                            Text(
                                                                text = "Dirty Items 🧺",
                                                                color = MaterialTheme.colorScheme.error,
                                                                fontSize = 10.sp,
                                                                fontWeight = FontWeight.Bold,
                                                                modifier = Modifier
                                                                    .background(MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f), RoundedCornerShape(6.dp))
                                                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                                                            )
                                                        }
                                                    }

                                                    val detailsList = mutableListOf<String>()
                                                    if (!outfit.occasion.isNullOrEmpty()) {
                                                        detailsList.add("Occasion: ${outfit.occasion}")
                                                    }
                                                    if (!outfit.season.isNullOrEmpty()) {
                                                        detailsList.add("Season: ${outfit.season}")
                                                    }
                                                    if (detailsList.isNotEmpty()) {
                                                        Spacer(modifier = Modifier.height(2.dp))
                                                        Text(
                                                            text = detailsList.joinToString(" | "),
                                                            fontSize = 12.sp,
                                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                                        )
                                                    }

                                                    Spacer(modifier = Modifier.height(12.dp))

                                                    LazyRow(
                                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                                        modifier = Modifier.fillMaxWidth()
                                                    ) {
                                                        items(outfitClothes) { item ->
                                                            Box(
                                                                modifier = Modifier
                                                                    .size(40.dp)
                                                                    .clip(RoundedCornerShape(8.dp))
                                                                    .background(MaterialTheme.colorScheme.surfaceVariant),
                                                                contentAlignment = Alignment.Center
                                                            ) {
                                                                if (item.imageUrl.isNotEmpty()) {
                                                                    AsyncImage(
                                                                        model = item.imageUrl,
                                                                        contentDescription = item.clothes_name,
                                                                        contentScale = ContentScale.Crop,
                                                                        modifier = Modifier.fillMaxSize()
                                                                    )
                                                                } else {
                                                                    Icon(
                                                                        Icons.Default.Checkroom,
                                                                        contentDescription = null,
                                                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                                        modifier = Modifier.size(20.dp)
                                                                    )
                                                                }
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    Divider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))

                    // Nút hành động
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (isCreatingNewOutfit) {
                            TextButton(onClick = {
                                isCreatingNewOutfit = false
                                selectedClothingItems.clear()
                            }) {
                                Text("Back", fontWeight = FontWeight.Bold)
                            }
                        } else {
                            TextButton(onClick = dismissAction) {
                                Text("Cancel", fontWeight = FontWeight.Bold)
                            }
                        }
                        
                        Spacer(modifier = Modifier.width(8.dp))
                        
                        if (isCreatingNewOutfit) {
                            Button(
                                enabled = newOutfitName.isNotBlank() && selectedClothingItems.isNotEmpty(),
                                onClick = {
                                    viewModel.createAndScheduleOutfit(
                                        userId = userId,
                                        name = newOutfitName,
                                        occasion = newOutfitOccasion,
                                        clothingIds = selectedClothingItems,
                                        plannedDate = selectedDateStr
                                    ) { success ->
                                        if (success) {
                                            dismissAction()
                                        }
                                    }
                                },
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("Create & Plan", fontWeight = FontWeight.Bold)
                            }
                        } else {
                            Button(
                                enabled = selectedOutfitToSchedule != null,
                                onClick = {
                                    val outfitId = selectedOutfitToSchedule?.id
                                    if (outfitId != null) {
                                        viewModel.saveOutfitSchedule(
                                            outfitId = outfitId,
                                            plannedDate = selectedDateStr,
                                            userId = userId
                                        ) { success ->
                                            if (success) {
                                                dismissAction()
                                            }
                                        }
                                    }
                                },
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("Confirm", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
    }


@Composable
fun OutfitScheduleCard(
    scheduleDetails: OutfitScheduleWithDetails,
    isToday: Boolean,
    onConfirmWorn: () -> Unit,
    onDelete: () -> Unit
) {
    // Kiểm tra xem có món đồ nào đang bẩn (WORN, IN_WASH, NEED_IRON) không
    val dirtyItems = scheduleDetails.items.filter { it.status.uppercase() in listOf("WORN", "IN_WASH", "NEED_IRON") }
    val hasDirtyWarning = dirtyItems.isNotEmpty()

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)),
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
                shape = RoundedCornerShape(20.dp)
            )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    Icons.Default.Checkroom,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = scheduleDetails.outfitName,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "${scheduleDetails.items.size} Clothing Items",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                IconButton(onClick = onDelete) {
                    Icon(
                        Icons.Default.DeleteOutline,
                        contentDescription = "Delete",
                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Danh sách các món đồ trong bộ phối đồ
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(scheduleDetails.items, key = { it.id ?: "" }) { item ->
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.width(60.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(50.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                .border(
                                    width = 1.dp,
                                    color = if (item.status.uppercase() in listOf("WORN", "IN_WASH")) MaterialTheme.colorScheme.error.copy(alpha = 0.6f)
                                            else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
                                    shape = RoundedCornerShape(12.dp)
                                )
                        ) {
                            if (item.imageUrl.isNotEmpty()) {
                                AsyncImage(
                                    model = item.imageUrl,
                                    contentDescription = item.clothes_name,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            } else {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Default.Checkroom,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = item.clothes_name,
                            fontSize = 9.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // CẢNH BÁO AN TOÀN: Nếu trang phục đang bẩn/đang giặt
            if (hasDirtyWarning) {
                Spacer(modifier = Modifier.height(12.dp))
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Warning,
                            contentDescription = "Warning",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Warning: ${dirtyItems.size} items in this outfit are currently dirty/washing!",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // Nút "Xác nhận mặc" (chỉ hiện đối với ngày hôm nay)
            if (isToday) {
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = onConfirmWorn,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (hasDirtyWarning) MaterialTheme.colorScheme.secondary
                                         else MaterialTheme.colorScheme.primary
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Check, null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Confirm Worn Today", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
