package com.example.dacs3.dashboard.homeui

import androidx.compose.foundation.background
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
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
import com.example.dacs3.ui.components.EmptyWardrobeState
import com.example.dacs3.ui.components.ShimmerCard
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardContent(
    currentProfile: Profile?,
    closetItems: List<ClothingItem>,
    topFavoriteClothes: List<Pair<ClothingItem, Int>> = emptyList(),
    onLogOotd: (List<String>, String, String) -> Unit = { _, _, _ -> },
    onNavigateToStylist: () -> Unit = {},
    onNavigateToSeasonStores: (String) -> Unit = {}
) {
    var selectedEvent by remember { mutableStateOf("University") }
    var selectedMood by remember { mutableStateOf("Confident") }

    var showMoodSheet by remember { mutableStateOf(false) }
    var showEventSheet by remember { mutableStateOf(false) }
    var showShopSheet by remember { mutableStateOf(false) }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var isLocationDenied by remember { mutableStateOf(false) }

    var showOotdSheet by remember { mutableStateOf(false) }
    var selectedOotdTop by remember { mutableStateOf<ClothingItem?>(null) }
    var selectedOotdBottom by remember { mutableStateOf<ClothingItem?>(null) }
    var selectedOotdShoes by remember { mutableStateOf<ClothingItem?>(null) }

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
                            FilterChip(
                                selected = selectedOotdTop == tops[i],
                                onClick = { selectedOotdTop = tops[i] },
                                label = { Text(tops[i].clothes_name) }
                            )
                        }
                    }
                }

                item { Text("Select Bottom:", fontWeight = FontWeight.Bold) }
                item {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(bottoms.size) { i ->
                            FilterChip(
                                selected = selectedOotdBottom == bottoms[i],
                                onClick = { selectedOotdBottom = bottoms[i] },
                                label = { Text(bottoms[i].clothes_name) }
                            )
                        }
                    }
                }

                item { Text("Select Shoes:", fontWeight = FontWeight.Bold) }
                item {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(shoes.size) { i ->
                            FilterChip(
                                selected = selectedOotdShoes == shoes[i],
                                onClick = { selectedOotdShoes = shoes[i] },
                                label = { Text(shoes[i].clothes_name) }
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
                                onLogOotd(ids, selectedEvent, selectedMood)
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
                    text = "Shop Autumn Collection",
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
                        uriHandler.openUri(
                            "https://shopee.vn/search?keyword=th%E1%BB%9Di%20trang%20m%C3%B9a%20thu"
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
                        onNavigateToSeasonStores("autumn")
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
                        androidx.compose.material3.IconButton(onClick = {
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
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(
                        alpha = 0.5f
                    )
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onNavigateToStylist() }
                    .shadow(
                        12.dp,
                        RoundedCornerShape(24.dp),
                        spotColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                    )
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
                val bottomsCount =
                    closetItems.count { it.category.equals("bottom", ignoreCase = true) }
                val shoesCount =
                    closetItems.count { it.category.equals("shoes", ignoreCase = true) }
                val accsCount =
                    closetItems.count { it.category.equals("accessories", ignoreCase = true) }

                val stats = listOf(
                    "Tops" to topsCount.toString(),
                    "Bottoms" to bottomsCount.toString(),
                    "Shoes" to shoesCount.toString(),
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