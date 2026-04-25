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
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardContent(
    currentProfile: Profile?, 
    closetItems: List<ClothingItem>,
    topFavoriteClothes: List<Pair<ClothingItem, Int>> = emptyList(),
    onLogOotd: (List<String>) -> Unit = {}
) {
    var selectedEvent by remember { mutableStateOf("University") }
    var selectedMood by remember { mutableStateOf("Confident") }

    var showMoodSheet by remember { mutableStateOf(false) }
    var showEventSheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var isLocationDenied by remember { mutableStateOf(false) }

    var suggestedTop by remember { mutableStateOf<ClothingItem?>(null) }
    var suggestedBottom by remember { mutableStateOf<ClothingItem?>(null) }
    var suggestedShoes by remember { mutableStateOf<ClothingItem?>(null) }

    var showOotdSheet by remember { mutableStateOf(false) }
    var selectedOotdTop by remember { mutableStateOf<ClothingItem?>(null) }
    var selectedOotdBottom by remember { mutableStateOf<ClothingItem?>(null) }
    var selectedOotdShoes by remember { mutableStateOf<ClothingItem?>(null) }

    androidx.compose.runtime.LaunchedEffect(closetItems, selectedMood, selectedEvent) {
        suggestedTop = closetItems.filter { it.category.equals("top", true) }.randomOrNull()
        suggestedBottom = closetItems.filter { it.category.equals("bottom", true) }.randomOrNull()
        suggestedShoes = closetItems.filter { it.category.equals("shoes", true) }.randomOrNull()
    }

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
                            val ids = listOfNotNull(selectedOotdTop?.id, selectedOotdBottom?.id, selectedOotdShoes?.id)
                            if (ids.isNotEmpty()) {
                                onLogOotd(ids)
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

                    if (suggestedTop != null) {
                        OutfitItemPlaceholder(
                            suggestedTop!!.clothes_name,
                            Icons.Outlined.Checkroom,
                            "Top • ${suggestedTop!!.mainColor}",
                            MaterialTheme.colorScheme.secondaryContainer,
                            MaterialTheme.colorScheme.secondary
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                    }
                    if (suggestedBottom != null) {
                        OutfitItemPlaceholder(
                            suggestedBottom!!.clothes_name,
                            Icons.Filled.Checkroom,
                            "Bottom • ${suggestedBottom!!.mainColor}",
                            MaterialTheme.colorScheme.surfaceVariant,
                            MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                    }
                    if (suggestedShoes != null) {
                        OutfitItemPlaceholder(
                            suggestedShoes!!.clothes_name,
                            Icons.Outlined.DirectionsWalk,
                            "Shoes • ${suggestedShoes!!.mainColor}",
                            MaterialTheme.colorScheme.tertiaryContainer,
                            Color(0xFFFF9800)
                        )
                    }

                    if (suggestedTop == null && suggestedBottom == null && suggestedShoes == null) {
                        Text(
                            "Add some clothes to your closet first!",
                            color = MaterialTheme.colorScheme.error,
                            fontSize = 14.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Button(
                        onClick = { 
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress) 
                            suggestedTop = closetItems.filter { it.category.equals("top", true) }.randomOrNull()
                            suggestedBottom = closetItems.filter { it.category.equals("bottom", true) }.randomOrNull()
                            suggestedShoes = closetItems.filter { it.category.equals("shoes", true) }.randomOrNull()
                        },
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
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
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
                    modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp)
                ) {
                    items(topFavoriteClothes.size) { index ->
                        val (item, count) = topFavoriteClothes[index]
                        StatCard(item.clothes_name.take(15), "Worn $count x")
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
                val accsCount = closetItems.count { it.category.equals("accessories", ignoreCase = true) }

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