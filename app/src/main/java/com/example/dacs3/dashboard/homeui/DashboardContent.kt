package com.example.dacs3.dashboard.homeui

import androidx.compose.material3.IconButton
import androidx.lifecycle.viewmodel.compose.viewModel

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Checkroom
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.LocalLaundryService
import androidx.compose.material.icons.filled.FormatListBulleted
import androidx.compose.material.icons.filled.Today
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Checkroom
import androidx.compose.material.icons.outlined.DirectionsWalk
import androidx.compose.material.icons.outlined.Event
import androidx.compose.material.icons.outlined.LocalMall
import androidx.compose.material.icons.outlined.Mood
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.collectAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.material3.ButtonColors
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.dacs3.connectDB.ClothingItem
import com.example.dacs3.connectDB.Profile
import io.github.jan.supabase.gotrue.auth
import com.example.dacs3.ui.components.EmptyWardrobeState
import com.example.dacs3.ui.components.ShimmerCard
import java.util.Calendar
import coil.compose.AsyncImage
import androidx.compose.ui.layout.ContentScale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardContent(
    currentProfile: Profile?,
    closetItems: List<ClothingItem>,
    topFavoriteClothes: List<Pair<ClothingItem, Int>> = emptyList(),
    onLogOotd: (List<String>, List<String>, String?) -> Unit = { _, _, _ -> },
    onNavigateToStylist: () -> Unit = {},
    onNavigateToSeasonStores: (String) -> Unit = {},
    onMenuClick: () -> Unit = {},
    onNavigateToTodo: () -> Unit = {},
    onNavigateToCalendar: () -> Unit = {},
    onNavigateToLaundry: () -> Unit = {},
    dashboardViewModel: com.example.dacs3.connectDB.DashboardViewModel = viewModel()
) {
    val trips by dashboardViewModel.packingListsHistory.collectAsState()
    val todayDateStr = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date())
    val activeTripReturningToday = trips.find { it.list.returnDate == todayDateStr }

    val weatherViewModel: com.example.dacs3.dashboard.WeatherViewModel = viewModel()
    val tempString by weatherViewModel.temperature.collectAsState()
    val tempValue = remember(tempString) {
        tempString.replace(Regex("[^0-9-]"), "").toIntOrNull()
    }
    val currentMonth = remember { Calendar.getInstance().get(Calendar.MONTH) + 1 }
    val resolvedSeason = remember(currentMonth) {
        when (currentMonth) {
            in 3..5 -> "spring"
            in 6..8 -> "summer"
            in 9..11 -> "autumn"
            else -> "winter"
        }
    }
    val seasonDisplayName = remember(resolvedSeason) {
        when (resolvedSeason) {
            "spring" -> "Spring"
            "summer" -> "Summer"
            "autumn" -> "Autumn"
            else -> "Winter"
        }
    }

    var showShopSheet by remember { mutableStateOf(false) }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var isLocationDenied by remember { mutableStateOf(false) }

    var showOotdSheet by remember { mutableStateOf(false) }
    var selectedOotdTop by remember { mutableStateOf<ClothingItem?>(null) }
    var selectedOotdBottom by remember { mutableStateOf<ClothingItem?>(null) }
    var selectedOotdShoes by remember { mutableStateOf<ClothingItem?>(null) }

    val haptic = LocalHapticFeedback.current
    val uriHandler = LocalUriHandler.current

    // Tính toán số lượng đồ chưa mặc > 3 tháng
    val deadItemsCount = remember(closetItems) {
        val formatter = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
        val todayDate = java.util.Date()
        closetItems.count { item ->
            val referenceDateString = item.lastWornDate ?: item.createdAt
            if (referenceDateString != null) {
                try {
                    val refDate = formatter.parse(referenceDateString.substring(0, 10))
                    if (refDate != null) {
                        val diffInMillies = kotlin.math.abs(todayDate.time - refDate.time)
                        val diffInDays = java.util.concurrent.TimeUnit.DAYS.convert(
                            diffInMillies,
                            java.util.concurrent.TimeUnit.MILLISECONDS
                        )
                        diffInDays > 90
                    } else false
                } catch (e: Exception) {
                    false
                }
            } else false
        }
    }
    var showDeclutterWarning by remember { mutableStateOf(true) }

    if (showOotdSheet) {
        ModalBottomSheet(
            onDismissRequest = { showOotdSheet = false },
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.background
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    Text(
                        "Log Your OOTD",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                val tops = closetItems.filter { it.category.equals("top", true) }
                val bottoms = closetItems.filter { it.category.equals("bottom", true) }
                val shoes = closetItems.filter { it.category.equals("shoes", true) }

                item { Text("Select Top:", fontWeight = FontWeight.Bold) }
                item {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(tops.size) { i ->
                            val item = tops[i]
                            FilterChip(
                                selected = selectedOotdTop == item,
                                onClick = { selectedOotdTop = item },
                                label = { Text(item.clothes_name) },
                                leadingIcon = {
                                    if (item.imageUrl.isNotEmpty()) {
                                        AsyncImage(
                                            model = item.imageUrl,
                                            contentDescription = null,
                                            modifier = Modifier
                                                .size(24.dp)
                                                .clip(RoundedCornerShape(4.dp)),
                                            contentScale = ContentScale.Crop
                                        )
                                    } else {
                                        Icon(
                                            Icons.Filled.Checkroom,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            )
                        }
                    }
                }

                item { Text("Select Bottom:", fontWeight = FontWeight.Bold) }
                item {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(bottoms.size) { i ->
                            val item = bottoms[i]
                            FilterChip(
                                selected = selectedOotdBottom == item,
                                onClick = { selectedOotdBottom = item },
                                label = { Text(item.clothes_name) },
                                leadingIcon = {
                                    if (item.imageUrl.isNotEmpty()) {
                                        AsyncImage(
                                            model = item.imageUrl,
                                            contentDescription = null,
                                            modifier = Modifier
                                                .size(24.dp)
                                                .clip(RoundedCornerShape(4.dp)),
                                            contentScale = ContentScale.Crop
                                        )
                                    } else {
                                        Icon(
                                            Icons.Filled.Checkroom,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            )
                        }
                    }
                }

                item { Text("Select Shoes:", fontWeight = FontWeight.Bold) }
                item {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(shoes.size) { i ->
                            val item = shoes[i]
                            FilterChip(
                                selected = selectedOotdShoes == item,
                                onClick = { selectedOotdShoes = item },
                                label = { Text(item.clothes_name) },
                                leadingIcon = {
                                    if (item.imageUrl.isNotEmpty()) {
                                        AsyncImage(
                                            model = item.imageUrl,
                                            contentDescription = null,
                                            modifier = Modifier
                                                .size(24.dp)
                                                .clip(RoundedCornerShape(4.dp)),
                                            contentScale = ContentScale.Crop
                                        )
                                    } else {
                                        Icon(
                                            Icons.Filled.Checkroom,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            )
                        }
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = {
                            val ids = listOfNotNull(
                                selectedOotdTop?.id,
                                selectedOotdBottom?.id,
                                selectedOotdShoes?.id
                            )
                            if (ids.isNotEmpty()) {
                                onLogOotd(ids, emptyList(), null)
                                showOotdSheet = false
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Save OOTD")
                    }
                    Spacer(modifier = Modifier.height(40.dp))
                }
            }
        }
    }

    if (showShopSheet) {
        ModalBottomSheet(
            onDismissRequest = { showShopSheet = false },
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.background
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 16.dp)
            ) {
                Text(
                    text = "Shop $seasonDisplayName Collection",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Choose how you want to explore seasonal items.",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = {
                        showShopSheet = false
                        val keyword = "${seasonDisplayName.lowercase()} fashion"
                        val encodedKeyword = try {
                            java.net.URLEncoder.encode(keyword, "UTF-8")
                        } catch (e: Exception) {
                            "th%E1%BB%9Di%20trang%20m%C3%B9a%20thu"
                        }
                        uriHandler.openUri(
                            "https://shopee.vn/search?keyword=$encodedKeyword"
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondary
                    )
                ) {
                    Icon(
                        Icons.Outlined.LocalMall,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )

                    Spacer(modifier = Modifier.width(10.dp))

                    Text(
                        text = "Shop online on Shopee",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedButton(
                    onClick = {
                        showShopSheet = false
                        onNavigateToSeasonStores(resolvedSeason)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(
                        Icons.Outlined.LocalMall,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )

                    Spacer(modifier = Modifier.width(10.dp))

                    Text(
                        text = "Find nearby seasonal stores",
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(32.dp))
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
                IconButton(
                    onClick = onMenuClick,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                ) {
                    Icon(
                        Icons.Default.Menu,
                        contentDescription = "Open Drawer Menu",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
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
                        text = currentProfile?.fullName?.substringAfterLast(" ") ?: "User",
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
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.12f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 28.dp)
                    .shadow(
                        12.dp,
                        RoundedCornerShape(24.dp),
                        spotColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                    )
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.linearGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.primary,
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.85f),
                                    MaterialTheme.colorScheme.secondary.copy(alpha = 0.9f)
                                )
                            )
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
                                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.6f),
                                letterSpacing = 1.5.sp,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                            Text(
                                text = "$seasonDisplayName Collection ${if (tempValue != null) "($tempString)" else ""}",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.padding(bottom = 4.dp)
                            )
                            Text(
                                "Get 20% off exclusively via WEARWISE.",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f),
                                modifier = Modifier.padding(bottom = 12.dp)
                            )
                            Button(
                                onClick = { showShopSheet = true },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                                shape = RoundedCornerShape(12.dp),
                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 0.dp),
                                modifier = Modifier.height(36.dp)
                            ) {
                                Text(
                                    "Shop Now",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSystemInDarkTheme()) MaterialTheme.colorScheme.background else Color.White
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
        }

        if (activeTripReturningToday != null) {
            item {
                val isDark = isSystemInDarkTheme()
                val warnCardBg = if (isDark) Color(0xFF2C1414) else Color(0xFFFFEBEE)
                val warnCardBorder = if (isDark) Color(0xFF5A1F1F) else Color(0xFFEF9A9A)
                val warnIconBg = if (isDark) Color(0xFF4A1C1C) else Color(0xFFFFCDD2)
                val warnIconTint = if (isDark) Color(0xFFEF5350) else Color(0xFFD32F2F)
                val warnTitleColor = if (isDark) Color(0xFFFF8A80) else Color(0xFFC62828)
                val warnBodyColor = if (isDark) Color(0xFFFFCDD2) else Color(0xFFB71C1C)
                val warnBtnColor = if (isDark) Color(0xFFE53935) else Color(0xFFD32F2F)

                Card(
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = warnCardBg),
                    border = BorderStroke(1.dp, warnCardBorder),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 28.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(20.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(50.dp)
                                .clip(CircleShape)
                                .background(warnIconBg),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Filled.Warning, 
                                null, 
                                tint = warnIconTint,
                                modifier = Modifier.size(26.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "CHECK OUT TODAY! 🚨",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = warnTitleColor,
                                letterSpacing = 1.sp,
                                modifier = Modifier.padding(bottom = 4.dp)
                            )
                            Text(
                                text = "Leaving ${activeTripReturningToday.list.destination} today? Verify your packing checklist so you don't leave anything behind!",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                color = warnBodyColor
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Button(
                                onClick = {
                                    dashboardViewModel.showTravelHistoryTrigger = true
                                    onNavigateToStylist()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = warnBtnColor),
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 0.dp),
                                modifier = Modifier.height(34.dp)
                            ) {
                                Text(
                                    text = "Check List",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }
                    }
                }
            }
        }

        if (deadItemsCount > 0 && showDeclutterWarning) {
            item {
                Card(
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 28.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(20.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(50.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.error.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Filled.Warning, null, tint = MaterialTheme.colorScheme.error)
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "Declutter Time!",
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                            Text(
                                "You have $deadItemsCount items unworn in 3+ months. Consider donating or selling them to keep your closet fresh.",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.8f)
                            )
                        }
                        IconButton(onClick = {
                            showDeclutterWarning = false
                        }) {
                            Icon(
                                Icons.Filled.KeyboardArrowDown,
                                "Dismiss",
                                tint = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }
                }
            }
        }

        // ─── TIỆN ÍCH TRUY CẬP NHANH PHỤ CẬN ───
        item {
            val schedules by dashboardViewModel.outfitSchedules.collectAsState()
            val todaySchedule = schedules.find { it.schedule.plannedDate == todayDateStr }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 28.dp)
            ) {
                Text(
                    text = "Today's Outfit Plan",
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                if (todaySchedule != null) {
                    val dirtyItems = todaySchedule.items.filter { it.status.uppercase() in listOf("WORN", "IN_WASH", "NEED_IRON") }
                    val hasDirtyWarning = dirtyItems.isNotEmpty()

                    Card(
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                        ),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(20.dp))
                            .clickable { onNavigateToCalendar() }
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(
                                    Icons.Default.Today,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = todaySchedule.outfitName,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        text = "${todaySchedule.items.size} items scheduled for today",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Icon(
                                    Icons.Default.KeyboardArrowRight,
                                    contentDescription = "View Calendar",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                )
                            }

                            if (hasDirtyWarning) {
                                Spacer(modifier = Modifier.height(10.dp))
                                Card(
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.8f)
                                    ),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            Icons.Default.Warning,
                                            contentDescription = "Warning",
                                            tint = MaterialTheme.colorScheme.error,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "${dirtyItems.size} scheduled items are dirty/washing!",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.error
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = {
                                        val userId = com.example.dacs3.connectDB.supabase.auth.currentUserOrNull()?.id ?: ""
                                        dashboardViewModel.confirmWornOutfit(
                                            schedule = todaySchedule.schedule,
                                            userId = userId,
                                            onSuccess = {}
                                        )
                                    },
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (hasDirtyWarning) MaterialTheme.colorScheme.secondary 
                                                         else MaterialTheme.colorScheme.primary
                                    )
                                ) {
                                    Icon(Icons.Default.Check, null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Confirm Worn", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }

                                OutlinedButton(
                                    onClick = { onNavigateToCalendar() },
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.weight(0.8f)
                                ) {
                                    Text("Change", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                } else {
                    Card(
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
                        ),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(20.dp))
                            .clickable { onNavigateToCalendar() }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.Today,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = "No outfit planned today",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = "Plan ahead to save time in the morning.",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Button(
                                onClick = { onNavigateToCalendar() },
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                                modifier = Modifier.height(32.dp),
                            ) {
                                Text("Plan", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }

        // ─── TÓM TẮT NHANH GIẶT ỦI & VIỆC CẦN LÀM (DÒNG) ───
        item {
            val todos by dashboardViewModel.todos.collectAsState()
            val incompleteTodos = todos.filter { !it.isCompleted }
            val dirtyClothesCount = closetItems.count { it.status.uppercase() in listOf("WORN", "NEED_IRON") }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 28.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                    ),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)),
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(20.dp))
                        .clickable { onNavigateToLaundry() }
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Icon(
                            Icons.Default.LocalLaundryService,
                            contentDescription = "Laundry Tracker",
                            tint = if (dirtyClothesCount > 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Laundry",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = if (dirtyClothesCount > 0) "$dirtyClothesCount items to wash" else "All clothes clean ✨",
                            fontSize = 12.sp,
                            color = if (dirtyClothesCount > 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                    ),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)),
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(20.dp))
                        .clickable { onNavigateToTodo() }
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Icon(
                            Icons.Default.FormatListBulleted,
                            contentDescription = "Wardrobe To-Do",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "To-Do List",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = if (incompleteTodos.isNotEmpty()) incompleteTodos.first().title else "No tasks today! 💎",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }



        item {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(
                        12.dp,
                        RoundedCornerShape(24.dp),
                        spotColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                    )
                    .clip(RoundedCornerShape(24.dp))
                    .clickable { onNavigateToStylist() }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "What should I wear today?",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "Ask your AI Stylist for a personalized look based on your closet.",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = onNavigateToStylist,
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            Icon(Icons.Filled.AutoAwesome, null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Open AI Stylist", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .background(
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                                CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Filled.AutoAwesome,
                            null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(36.dp)) }

        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "OOTD Journal",
                    fontSize = 20.sp,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                )
                Button(onClick = { showOotdSheet = true }) {
                    Text("Log Today's Outfit")
                }
            }
        }

        if (topFavoriteClothes.isNotEmpty()) {
            item {
                Text(
                    "Your Top Favorite Items",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }
            item {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 24.dp)
                ) {
                    items(topFavoriteClothes.size) { index ->
                        val (item, count) = topFavoriteClothes[index]
                        FavoriteItemCard(item.clothes_name, count.toString(), item.imageUrl)
                    }
                }
            }
        } else if (closetItems.isEmpty()) {
            // ─── EMPTY STATE (Premium): Shown when closet is completely empty ───
            item {
                EmptyWardrobeState(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 24.dp)
                )
            }
        } else {
            // Closet has items but no OOTD logged yet — show shimmer placeholders
            item {
                Text(
                    "Start logging your OOTDs to see your favorites here!",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    modifier = Modifier.padding(bottom = 12.dp)
                )
            }
            item {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.padding(bottom = 24.dp)
                ) {
                    items(4) {
                        ShimmerCard(
                            modifier = Modifier.size(width = 110.dp, height = 130.dp)
                        )
                    }
                }
            }
        }

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
                val topsCount = closetItems.count { it.category.equals("top", ignoreCase = true) }
                val bottomsCount = closetItems.count { it.category.equals("bottom", ignoreCase = true) }
                val shoesCount = closetItems.count { it.category.equals("shoes", ignoreCase = true) }
                val outerwearCount = closetItems.count { it.category.equals("outerwear", ignoreCase = true) }
                val accsCount = closetItems.count { it.category.equals("accessories", ignoreCase = true) }

                val stats = listOf(
                    "Tops" to topsCount.toString(),
                    "Bottoms" to bottomsCount.toString(),
                    "Shoes" to shoesCount.toString(),
                    "Outerwear" to outerwearCount.toString(),
                    "Accs" to accsCount.toString()
                )
                items(stats.size) { index ->
                    StatCard(stats[index].first, stats[index].second)
                }
            }
        }

        item { Spacer(modifier = Modifier.height(120.dp)) }
    }
}