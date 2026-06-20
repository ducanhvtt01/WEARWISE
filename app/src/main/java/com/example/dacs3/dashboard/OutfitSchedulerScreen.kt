package com.example.dacs3.dashboard

import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Checkroom
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.dacs3.connectDB.ClothingItem
import com.example.dacs3.connectDB.DashboardViewModel
import com.example.dacs3.connectDB.OutfitDbModel
import com.example.dacs3.connectDB.OutfitItemDbModel
import com.example.dacs3.connectDB.supabase
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OutfitSchedulerScreen(
    viewModel: DashboardViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val userId = supabase.auth.currentUserOrNull()?.id ?: ""

    // SharedPreferences for daily events
    val prefs = remember { context.getSharedPreferences("wearwise_scheduler_events", Context.MODE_PRIVATE) }

    // State for weekly list
    val days = remember {
        val list = mutableListOf<Calendar>()
        for (i in 0 until 7) {
            val cal = Calendar.getInstance()
            cal.add(Calendar.DAY_OF_YEAR, i)
            list.add(cal)
        }
        list
    }

    // Weather forecast from WeatherViewModel
    val weatherViewModel: WeatherViewModel = viewModel()
    val forecastList by weatherViewModel.weeklyForecast.collectAsState()
    val currentTemp by weatherViewModel.temperature.collectAsState()
    val currentCondition by weatherViewModel.condition.collectAsState()

    LaunchedEffect(Unit) {
        weatherViewModel.fetchWeather()
    }

    // Saved outfits lists
    var outfitsList by remember { mutableStateOf<List<OutfitDbModel>>(emptyList()) }
    var outfitItemsList by remember { mutableStateOf<List<OutfitItemDbModel>>(emptyList()) }
    var isLoadingOutfits by remember { mutableStateOf(false) }

    val schedules by viewModel.outfitSchedules.collectAsState()
    val clothesList by viewModel.clothingItems.collectAsState()

    // Fetch schedules & clothes
    LaunchedEffect(userId) {
        if (userId.isNotEmpty()) {
            viewModel.fetchOutfitSchedules(userId)
            viewModel.getClothingItems(userId)
            
            // Fetch saved outfits
            isLoadingOutfits = true
            scope.launch(Dispatchers.IO) {
                try {
                    val list = supabase.from("outfits")
                        .select { filter { eq("user_id", userId) } }
                        .decodeList<OutfitDbModel>()
                    val outfitIds = list.mapNotNull { it.id }
                    val items = if (outfitIds.isNotEmpty()) {
                        supabase.from("outfit_items")
                            .select { filter { isIn("outfit_id", outfitIds) } }
                            .decodeList<OutfitItemDbModel>()
                    } else {
                        emptyList()
                    }
                    withContext(Dispatchers.Main) {
                        outfitsList = list
                        outfitItemsList = items
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                } finally {
                    withContext(Dispatchers.Main) {
                        isLoadingOutfits = false
                    }
                }
            }
        }
    }

    // Dialogue State
    var targetDateForSchedule by remember { mutableStateOf<String?>(null) }
    var showManualSelectDialog by remember { mutableStateOf(false) }
    var showAiSuggestDialog by remember { mutableStateOf(false) }
    var aiSuggestions by remember { mutableStateOf<List<Pair<OutfitDbModel, String>>>(emptyList()) }
    var showClearAllDialog by remember { mutableStateOf(false) }

    if (showClearAllDialog) {
        AlertDialog(
            onDismissRequest = { showClearAllDialog = false },
            title = { Text("Clear All Schedules?") },
            text = { Text("This will remove all scheduled outfits for the entire week. This action cannot be undone.") },
            confirmButton = {
                Button(
                    onClick = {
                        scope.launch {
                            schedules.forEach { schedule ->
                                val id = schedule.schedule.id
                                if (id != null) {
                                    viewModel.deleteOutfitSchedule(id, userId)
                                }
                            }
                            Toast.makeText(context, "Cleared all weekly schedules", Toast.LENGTH_SHORT).show()
                        }
                        showClearAllDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Clear All")
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearAllDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            "AI Weekly Scheduler",
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.primary,
                            fontSize = 20.sp
                        )
                        Text(
                            "Plan your outfits based on weather & events",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (schedules.isNotEmpty()) {
                        TextButton(onClick = { showClearAllDialog = true }) {
                            Text("Clear All", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(horizontal = 24.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(days) { calendar ->
                val dateStr = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(calendar.time)
                val displayDayName = SimpleDateFormat("EEEE", Locale.getDefault()).format(calendar.time)
                val displayDate = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(calendar.time)

                // Get weather info for this date
                val forecast = forecastList.find { it.dateStr == dateStr }
                val isToday = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date()) == dateStr
                
                val tempStr = if (isToday) currentTemp else (forecast?.tempStr ?: "26°C")
                val condition = if (isToday) currentCondition else (forecast?.condition ?: "Clear")
                val weatherEmoji = when {
                    condition.lowercase().contains("rain") -> "🌧️"
                    condition.lowercase().contains("cloud") -> "☁️"
                    condition.lowercase().contains("snow") -> "❄️"
                    condition.lowercase().contains("thunder") -> "⚡"
                    condition.lowercase().contains("drizzle") -> "🌦️"
                    else -> "☀️"
                }

                // SharedPreferences event state
                var selectedEvent by remember {
                    mutableStateOf(prefs.getString("event_$dateStr", "Daily Life 🏠") ?: "Daily Life 🏠")
                }

                // Dropdown menu state
                var eventMenuExpanded by remember { mutableStateOf(false) }
                val eventOptions = listOf(
                    "Daily Life 🏠",
                    "Work / Office 💼",
                    "Gym / Workout 🏋️",
                    "Dinner / Date 🍷",
                    "Hangout / Cafe ☕"
                )

                // Scheduled outfit details
                val daySchedule = schedules.find { it.schedule.plannedDate == dateStr }

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        // Header Date & Weather Info
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = if (isToday) "Today ($displayDayName)" else "$displayDayName, $displayDate",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    color = if (isToday) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "$weatherEmoji $condition ($tempStr)",
                                        fontSize = 13.sp,
                                        color = MaterialTheme.colorScheme.secondary,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }

                            // Event Selector Trigger
                            Box {
                                TextButton(
                                    onClick = { eventMenuExpanded = true },
                                    colors = ButtonDefaults.textButtonColors(
                                        containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
                                    ),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Text(selectedEvent, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    Icon(Icons.Filled.ArrowDropDown, null, modifier = Modifier.size(16.dp))
                                }
                                DropdownMenu(
                                    expanded = eventMenuExpanded,
                                    onDismissRequest = { eventMenuExpanded = false }
                                ) {
                                    eventOptions.forEach { option ->
                                        DropdownMenuItem(
                                            text = { Text(option, fontSize = 13.sp, fontWeight = FontWeight.Medium) },
                                            onClick = {
                                                selectedEvent = option
                                                prefs.edit().putString("event_$dateStr", option).apply()
                                                eventMenuExpanded = false
                                            }
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))
                        HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
                        Spacer(modifier = Modifier.height(12.dp))

                        // Outfit Slot Content
                        if (daySchedule != null) {
                            // Render scheduled outfit
                            Column(
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                // List Outfit Images
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    daySchedule.items.take(3).forEach { cloth ->
                                        Box(
                                            modifier = Modifier
                                                .size(54.dp)
                                                .clip(RoundedCornerShape(10.dp))
                                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                                .border(0.5.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f), RoundedCornerShape(10.dp)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            if (cloth.imageUrl.isNotEmpty()) {
                                                AsyncImage(
                                                    model = cloth.imageUrl,
                                                    contentDescription = cloth.clothes_name,
                                                    contentScale = ContentScale.Crop,
                                                    modifier = Modifier.fillMaxSize()
                                                )
                                            } else {
                                                Icon(Icons.Outlined.Checkroom, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                            }
                                        }
                                    }
                                    if (daySchedule.items.size > 3) {
                                        Box(
                                            modifier = Modifier
                                                .size(54.dp)
                                                .clip(RoundedCornerShape(10.dp))
                                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                "+${daySchedule.items.size - 3}",
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.primary,
                                                fontSize = 12.sp
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text(
                                            text = daySchedule.outfitName,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp,
                                            color = MaterialTheme.colorScheme.primary,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = "${daySchedule.items.size} items",
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(8.dp))

                                    // Delete Button
                                    IconButton(
                                        onClick = {
                                            viewModel.deleteOutfitSchedule(daySchedule.schedule.id ?: "", userId)
                                            Toast.makeText(context, "Removed schedule", Toast.LENGTH_SHORT).show()
                                        },
                                        modifier = Modifier
                                            .size(36.dp)
                                            .background(MaterialTheme.colorScheme.error.copy(alpha = 0.1f), CircleShape)
                                    ) {
                                        Icon(
                                            Icons.Filled.Delete,
                                            "Delete",
                                            tint = MaterialTheme.colorScheme.error,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        } else {
                            // Show Selection Options Buttons
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Button(
                                    onClick = {
                                        targetDateForSchedule = dateStr
                                        // Run AI Recommendation Scoring
                                        val suggestions = runAiRecommendation(
                                            tempStr = tempStr,
                                            condition = condition,
                                            event = selectedEvent,
                                            outfits = outfitsList,
                                            outfitItems = outfitItemsList,
                                            clothes = clothesList
                                        )
                                        aiSuggestions = suggestions
                                        showAiSuggestDialog = true
                                    },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                    contentPadding = PaddingValues(vertical = 10.dp)
                                ) {
                                    Icon(Icons.Filled.AutoAwesome, null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("AI Auto-Suggest", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }

                                OutlinedButton(
                                    onClick = {
                                        targetDateForSchedule = dateStr
                                        showManualSelectDialog = true
                                    },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(12.dp),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)),
                                    contentPadding = PaddingValues(vertical = 10.dp)
                                ) {
                                    Icon(Icons.Filled.List, null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Select Manually", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // DIALOG: AI RECOMMENDATIONS SHOWCASE
    if (showAiSuggestDialog && targetDateForSchedule != null) {
        Dialog(onDismissRequest = { showAiSuggestDialog = false }) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
            ) {
                Column(
                    modifier = Modifier
                        .padding(20.dp)
                        .fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "AI Smart Suggestions 🤖",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 18.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                        IconButton(onClick = { showAiSuggestDialog = false }) {
                            Icon(Icons.Filled.Close, null)
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))

                    if (aiSuggestions.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(150.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "No suitable outfits found.\nTry creating a custom outfit first!",
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 13.sp
                            )
                        }
                    } else {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.weight(1f, fill = false)
                        ) {
                            itemsIndexed(aiSuggestions) { index, (outfit, reason) ->
                                val outfitClothes = outfitItemsList
                                    .filter { it.outfitId == outfit.id }
                                    .mapNotNull { link -> clothesList.find { it.id == link.clothingId } }

                                val isBestPick = index == 0
                                val borderColor = if (isBestPick) Color(0xFFFFD700) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
                                val borderStrokeWidth = if (isBestPick) 2.dp else 1.dp
                                val cardBg = if (isBestPick) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)

                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(14.dp),
                                    colors = CardDefaults.cardColors(containerColor = cardBg),
                                    border = BorderStroke(borderStrokeWidth, borderColor)
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = outfit.name,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 14.sp,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                            if (isBestPick) {
                                                Box(
                                                    modifier = Modifier
                                                        .clip(RoundedCornerShape(6.dp))
                                                        .background(Color(0xFFFFD700))
                                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                                ) {
                                                    Text(
                                                        "★ BEST PICK",
                                                        color = Color.Black,
                                                        fontWeight = FontWeight.ExtraBold,
                                                        fontSize = 9.sp
                                                    )
                                                }
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = reason,
                                            fontSize = 11.sp,
                                            color = if (isBestPick) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.secondary,
                                            fontWeight = FontWeight.Medium
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))

                                        // Clothing items horizontal row
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.Start,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                                outfitClothes.take(4).forEach { cloth ->
                                                    Box(
                                                        modifier = Modifier
                                                            .size(42.dp)
                                                            .clip(RoundedCornerShape(8.dp))
                                                            .background(MaterialTheme.colorScheme.surface)
                                                    ) {
                                                        if (cloth.imageUrl.isNotEmpty()) {
                                                            AsyncImage(
                                                                model = cloth.imageUrl,
                                                                contentDescription = cloth.clothes_name,
                                                                contentScale = ContentScale.Crop,
                                                                modifier = Modifier.fillMaxSize()
                                                            )
                                                        } else {
                                                            Icon(Icons.Outlined.Checkroom, null, modifier = Modifier.size(16.dp).align(Alignment.Center))
                                                        }
                                                    }
                                                }
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(10.dp))

                                        Button(
                                            onClick = {
                                                viewModel.saveOutfitSchedule(
                                                    outfitId = outfit.id ?: "",
                                                    plannedDate = targetDateForSchedule!!,
                                                    userId = userId
                                                ) { success ->
                                                    if (success) {
                                                        Toast.makeText(context, "Outfit scheduled successfully!", Toast.LENGTH_SHORT).show()
                                                    }
                                                }
                                                showAiSuggestDialog = false
                                            },
                                            modifier = Modifier.align(Alignment.Start),
                                            shape = RoundedCornerShape(8.dp),
                                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Filled.Check,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(14.dp)
                                                )
                                                Text("Choose Outfit", fontSize = 12.sp, fontWeight = FontWeight.Bold)
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

    // DIALOG: MANUAL OUTFIT SELECTION
    if (showManualSelectDialog && targetDateForSchedule != null) {
        Dialog(onDismissRequest = { showManualSelectDialog = false }) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
            ) {
                Column(
                    modifier = Modifier
                        .padding(20.dp)
                        .fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Select Outfit 👗",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 18.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                        IconButton(onClick = { showManualSelectDialog = false }) {
                            Icon(Icons.Filled.Close, null)
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))

                    if (outfitsList.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(150.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "No saved outfits available.\nCreate some first in Closet or Swiper!",
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 13.sp
                            )
                        }
                    } else {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.weight(1f, fill = false)
                        ) {
                            items(outfitsList) { outfit ->
                                val outfitClothes = outfitItemsList
                                    .filter { it.outfitId == outfit.id }
                                    .mapNotNull { link -> clothesList.find { it.id == link.clothingId } }

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                                        .clickable {
                                            viewModel.saveOutfitSchedule(
                                                outfitId = outfit.id ?: "",
                                                plannedDate = targetDateForSchedule!!,
                                                userId = userId
                                            ) { success ->
                                                if (success) {
                                                    Toast.makeText(context, "Outfit scheduled successfully!", Toast.LENGTH_SHORT).show()
                                                }
                                            }
                                            showManualSelectDialog = false
                                        }
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            outfit.name,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp
                                        )
                                        Text(
                                            "${outfitClothes.size} items • ${outfit.occasion ?: "Casual"}",
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Icon(
                                        Icons.Filled.ChevronRight,
                                        null,
                                        tint = MaterialTheme.colorScheme.primary,
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

// AI SUGGESTION SCORING ALGORITHM
private fun runAiRecommendation(
    tempStr: String,
    condition: String,
    event: String,
    outfits: List<OutfitDbModel>,
    outfitItems: List<OutfitItemDbModel>,
    clothes: List<ClothingItem>
): List<Pair<OutfitDbModel, String>> {
    val tempVal = try {
        tempStr.replace("°C", "").trim().toFloat()
    } catch (e: Exception) {
        26f
    }

    val result = outfits.map { outfit ->
        val outfitClothes = outfitItems
            .filter { it.outfitId == outfit.id }
            .mapNotNull { link -> clothes.find { it.id == link.clothingId } }

        var score = 0
        val reasons = mutableListOf<String>()

        // 1. Event Type Matching (up to 50 points)
        when {
            event.contains("Work") -> {
                val matchesOccasion = outfit.occasion?.lowercase()?.run { contains("work") || contains("formal") || contains("office") } ?: false
                if (matchesOccasion) {
                    score += 30
                    reasons.add("Occasion matches Work")
                }
                val hasFormalItem = outfitClothes.any {
                    it.category.lowercase().run { contains("shirt") || contains("trouser") || contains("pants") || contains("blazer") }
                }
                if (hasFormalItem) {
                    score += 20
                    reasons.add("Contains formal clothes (Shirt/Trouser)")
                }
            }
            event.contains("Gym") -> {
                val matchesOccasion = outfit.occasion?.lowercase()?.run { contains("sport") || contains("gym") || contains("workout") || contains("active") } ?: false
                if (matchesOccasion) {
                    score += 30
                    reasons.add("Occasion matches Gym")
                }
                val hasSportyItem = outfitClothes.any {
                    it.category.lowercase().run { contains("t-shirt") || contains("short") || contains("sport") || contains("giày thể thao") || contains("shoes") }
                }
                if (hasSportyItem) {
                    score += 20
                    reasons.add("Contains activewear/shorts")
                }
            }
            event.contains("Dinner") -> {
                val matchesOccasion = outfit.occasion?.lowercase()?.run { contains("date") || contains("party") || contains("formal") || contains("dinner") } ?: false
                if (matchesOccasion) {
                    score += 30
                    reasons.add("Occasion matches Dinner/Date")
                }
                val hasClassyItem = outfitClothes.any {
                    it.category.lowercase().run { contains("dress") || contains("skirt") || contains("blazer") || contains("áo vest") }
                }
                if (hasClassyItem) {
                    score += 20
                    reasons.add("Contains elegant items (Dress/Skirt)")
                }
            }
            event.contains("Hangout") -> {
                val matchesOccasion = outfit.occasion?.lowercase()?.run { contains("casual") || contains("hangout") || contains("daily") } ?: false
                if (matchesOccasion) {
                    score += 30
                    reasons.add("Occasion matches Hangout/Cafe")
                }
                val hasCasualItem = outfitClothes.any {
                    it.category.lowercase().run { contains("t-shirt") || contains("jeans") || contains("denim") }
                }
                if (hasCasualItem) {
                    score += 20
                    reasons.add("Contains casual items (T-shirt/Jeans)")
                }
            }
            else -> { // Daily Life
                val matchesOccasion = outfit.occasion?.lowercase()?.run { contains("casual") || contains("daily") } ?: false
                if (matchesOccasion) {
                    score += 30
                    reasons.add("Occasion matches Daily Life")
                }
            }
        }

        // 2. Weather Temperature Matching (up to 30 points)
        if (tempVal < 18f) { // Cold
            val hasJackets = outfitClothes.any {
                it.category.lowercase().run { contains("jacket") || contains("coat") || contains("sweater") || contains("hoodie") || contains("áo khoác") }
            }
            if (hasJackets) {
                score += 30
                reasons.add("Warm jacket for cold weather (${tempVal}°C)")
            } else {
                score -= 10
            }
        } else if (tempVal > 28f) { // Hot
            val hasThickOuterwear = outfitClothes.any {
                it.category.lowercase().run { contains("coat") || contains("sweater") || contains("áo khoác") }
            }
            if (hasThickOuterwear) {
                score -= 20
                reasons.add("Thick clothes penalized for hot weather (${tempVal}°C)")
            } else {
                score += 30
                reasons.add("Light, breathable clothes for hot weather (${tempVal}°C)")
            }
        } else { // Moderate
            score += 20
            reasons.add("Suited for moderate temperature (${tempVal}°C)")
        }

        // 3. Rain Matching (up to 20 points)
        val isRainy = condition.lowercase().run { contains("rain") || contains("drizzle") || contains("thunder") }
        if (isRainy) {
            val hasOuterwear = outfitClothes.any {
                it.category.lowercase().run { contains("jacket") || contains("coat") || contains("áo khoác") }
            }
            if (hasOuterwear) {
                score += 20
                reasons.add("Outer layer protects from rain 🌧️")
            }
        }

        // 4. Favorite Bonus
        if (outfit.isFavorite) {
            score += 10
            reasons.add("Is one of your favorite combinations ❤️")
        }

        // Filter out outfits with dirty clothes
        val hasDirtyItem = outfitClothes.any { it.status.uppercase() in listOf("WORN", "IN_WASH") }
        if (hasDirtyItem) {
            score = -999
        }

        val explanation = reasons.joinToString(", ")
        outfit to (score to explanation)
    }
    .filter { it.second.first > 0 }
    .sortedByDescending { it.second.first }
    .map { it.first to it.second.second }
    .take(3)

    return result
}
