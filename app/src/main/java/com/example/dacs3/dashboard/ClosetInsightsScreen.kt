package com.example.dacs3.dashboard

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.AttachMoney
import androidx.compose.material.icons.outlined.Checkroom
import androidx.compose.material.icons.outlined.TrendingDown
import androidx.compose.material.icons.outlined.TrendingUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.dacs3.connectDB.ClothingItem
import com.example.dacs3.connectDB.DashboardViewModel
import com.example.dacs3.connectDB.OutfitItemDbModel
import com.example.dacs3.connectDB.UsageHistoryDbModel
import com.example.dacs3.connectDB.supabase
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.gotrue.auth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClosetInsightsScreen(
    viewModel: DashboardViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // State from ViewModel
    val closetItems by viewModel.clothingItems.collectAsState()

    // Local insights states
    var wearCounts by remember { mutableStateOf<Map<String, Int>>(emptyMap()) }
    var isLoadingInsights by remember { mutableStateOf(true) }

    // Edit price state
    var editingItem by remember { mutableStateOf<ClothingItem?>(null) }
    var editPriceText by remember { mutableStateOf("") }
    var sortByCpw by remember { mutableStateOf(false) }
    var selectedTabIndex by remember { mutableIntStateOf(0) }

    // Fetch wear history from Supabase
    LaunchedEffect(closetItems) {
        if (closetItems.isNotEmpty()) {
            isLoadingInsights = true
            
            // Fetch wear counts from Supabase
            val userId = supabase.auth.currentUserOrNull()?.id ?: ""
            scope.launch(Dispatchers.IO) {
                try {
                    // Fetch usage histories
                    val histories = supabase.from("usage_history")
                        .select { filter { eq("user_id", userId) } }
                        .decodeList<UsageHistoryDbModel>()

                    if (histories.isNotEmpty()) {
                        val outfitIds = histories.map { it.outfitId }
                        // Fetch items linked to these outfits
                        val outfitItems = supabase.from("outfit_items")
                            .select { filter { isIn("outfit_id", outfitIds) } }
                            .decodeList<OutfitItemDbModel>()

                        val counts = outfitItems.groupingBy { it.clothingId }.eachCount()
                        withContext(Dispatchers.Main) {
                            wearCounts = counts
                            isLoadingInsights = false
                        }
                    } else {
                        withContext(Dispatchers.Main) {
                            isLoadingInsights = false
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    withContext(Dispatchers.Main) {
                        isLoadingInsights = false
                    }
                }
            }
        } else {
            isLoadingInsights = false
        }
    }

    // Mathematical calculations
    val totalItems = closetItems.size
    val totalValue = closetItems.sumOf { it.price?.toDouble() ?: 0.0 }
    
    // Category counts
    val categoryCounts = closetItems.groupBy { it.category }.mapValues { it.value.size }
    
    // Color counts
    val colorCounts = closetItems.groupBy { it.mainColor ?: "Other" }.mapValues { it.value.size }
    val totalColorItems = colorCounts.values.sum().coerceAtLeast(1)

    // Cost-per-wear calculations
    val cpwList = closetItems.map { item ->
        val price = item.price ?: 0f
        val wears = wearCounts[item.id] ?: 0
        val cpw = if (wears > 0) price / wears else price
        Triple(item, wears, cpw)
    }

    val displayedItems = remember(closetItems, wearCounts, sortByCpw) {
        if (sortByCpw) {
            closetItems.sortedByDescending { item ->
                val price = item.price ?: 0f
                val wears = wearCounts[item.id] ?: 0
                if (wears > 0) price / wears else price
            }
        } else {
            closetItems
        }
    }

    // Top investments (lowest CPW, must be worn at least once)
    val topInvestments = cpwList
        .filter { it.second > 0 && (it.first.price ?: 0f) > 0 }
        .sortedBy { it.third }
        .take(5)

    // Wasted budget (high price, 0 or low wears)
    val wastedBudget = cpwList
        .filter { (it.first.price ?: 0f) > 0 }
        .sortedWith(compareByDescending<Triple<ClothingItem, Int, Float>> { it.first.price ?: 0f }
            .thenBy { it.second })
        .take(5)

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Wardrobe Insights",
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        if (isLoadingInsights) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (closetItems.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Outlined.Checkroom, null, modifier = Modifier.size(72.dp), tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f))
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("No clothes in your closet to analyze.", fontWeight = FontWeight.Bold)
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                TabRow(
                    selectedTabIndex = selectedTabIndex,
                    containerColor = MaterialTheme.colorScheme.background,
                    contentColor = MaterialTheme.colorScheme.primary
                ) {
                    Tab(
                        selected = selectedTabIndex == 0,
                        onClick = { selectedTabIndex = 0 },
                        text = { Text("My Closet CPW", fontWeight = FontWeight.Bold) }
                    )
                    Tab(
                        selected = selectedTabIndex == 1,
                        onClick = { selectedTabIndex = 1 },
                        text = { Text("Statistics", fontWeight = FontWeight.Bold) }
                    )
                }

                if (selectedTabIndex == 0) {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(start = 24.dp, end = 24.dp, bottom = 32.dp, top = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(24.dp)
                    ) {
                // Wardrobe Net Worth Card
                item {
                    val isDark = isSystemInDarkTheme()
                    val brush = if (isDark) {
                        Brush.horizontalGradient(
                            colors = listOf(
                                Color(0xFF1A237E), // Deep Midnight Blue
                                Color(0xFF0B0E14)  // Dark Background
                            )
                        )
                    } else {
                        Brush.horizontalGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primary,
                                MaterialTheme.colorScheme.secondary.copy(alpha = 0.85f)
                            )
                        )
                    }
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .shadow(12.dp, RoundedCornerShape(24.dp), spotColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)),
                        shape = RoundedCornerShape(24.dp),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.15f)),
                        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
                    ) {
                        Box(
                            modifier = Modifier
                                .background(brush)
                                .fillMaxWidth()
                                .padding(24.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(56.dp)
                                        .background(Color.White.copy(alpha = 0.2f), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Outlined.AttachMoney, null, tint = Color.White, modifier = Modifier.size(32.dp))
                                }
                                Spacer(modifier = Modifier.width(16.dp))
                                Column {
                                    Text(
                                        "WARDROBE VALUE",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White.copy(alpha = 0.8f),
                                        letterSpacing = 1.sp
                                    )
                                    Text(
                                        text = String.format("%,.0f VND", totalValue),
                                        fontSize = 24.sp,
                                        fontWeight = FontWeight.Black,
                                        color = Color.White
                                    )
                                    Text(
                                        text = "$totalItems items in total",
                                        fontSize = 12.sp,
                                        color = Color.White.copy(alpha = 0.8f)
                                    )
                                }
                            }
                        }
                    }
                }

                // Color Distribution Donut Chart & Legend
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
                    ) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            Text(
                                "Color Palette Analyzer",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(16.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Draw beautiful Donut Chart
                                val colorMap = remember(colorCounts) {
                                    val map = mutableMapOf<String, Color>()
                                    colorCounts.keys.forEach { name ->
                                        map[name] = parseFashionColor(name)
                                    }
                                    map
                                }

                                Box(
                                    modifier = Modifier.size(120.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Canvas(modifier = Modifier.fillMaxSize()) {
                                        var startAngle = -90f
                                        colorCounts.forEach { (colorName, count) ->
                                            val sweepAngle = (count.toFloat() / totalColorItems) * 360f
                                            drawArc(
                                                color = colorMap[colorName] ?: Color.Gray,
                                                startAngle = startAngle,
                                                sweepAngle = sweepAngle,
                                                useCenter = false,
                                                style = Stroke(width = 24.dp.toPx()),
                                                size = Size(size.width - 24.dp.toPx(), size.height - 24.dp.toPx()),
                                                topLeft = Offset(12.dp.toPx(), 12.dp.toPx())
                                            )
                                            startAngle += sweepAngle
                                        }
                                    }
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text("${colorCounts.size}", fontSize = 20.sp, fontWeight = FontWeight.Black)
                                        Text("Colors", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }

                                Spacer(modifier = Modifier.width(24.dp))

                                // Donut legend list
                                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    colorCounts.entries.sortedByDescending { it.value }.take(5).forEach { (colorName, count) ->
                                        val percent = (count.toFloat() / totalColorItems * 100).toInt()
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(12.dp)
                                                    .clip(CircleShape)
                                                    .background(colorMap[colorName] ?: Color.Gray)
                                                    .border(1.dp, Color.LightGray, CircleShape)
                                            )
                                            Text(
                                                text = "$colorName ($percent%)",
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Medium
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Cost-Per-Wear Management Table
                item {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                "Wardrobe CPW Manager",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            IconButton(
                                onClick = {
                                    Toast.makeText(context, "CPW (Cost Per Wear) = Price / Wears. Lower is better value!", Toast.LENGTH_LONG).show()
                                },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Info,
                                    contentDescription = "What is CPW?",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Start
                        ) {
                            SortChip(
                                selected = !sortByCpw,
                                text = "Default",
                                onClick = { sortByCpw = false }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            SortChip(
                                selected = sortByCpw,
                                text = "Highest CPW",
                                onClick = { sortByCpw = true }
                            )
                        }
                    }
                }

                items(displayedItems, key = { it.id ?: "" }) { item ->
                    val price = item.price ?: 0f
                    val wears = wearCounts[item.id] ?: 0
                    val cpw = if (wears > 0) price / wears else price

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                editingItem = item
                                editPriceText = if (price > 0) price.toInt().toString() else ""
                            },
                        shape = RoundedCornerShape(20.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (item.imageUrl.isNotEmpty()) {
                                AsyncImage(
                                    model = item.imageUrl,
                                    contentDescription = item.clothes_name,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .size(60.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                )
                            } else {
                                Box(
                                    modifier = Modifier
                                        .size(60.dp)
                                        .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Outlined.Checkroom, null)
                                }
                            }

                            Spacer(modifier = Modifier.width(16.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = item.clothes_name,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "${item.category} | Worn $wears ${if (wears == 1) "time" else "times"}",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = if (price > 0) String.format("%,.0f đ", price) else "Set Price 🏷️",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (price > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary
                                )
                                if (price > 0) {
                                    Text(
                                        text = String.format("CPW: %,.0f đ", cpw),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = if (wears > 2) Color(0xFF4CAF50) else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }
        } else {
            LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 24.dp, end = 24.dp, bottom = 32.dp, top = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    // Line Chart
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(20.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
                        ) {
                            Column(modifier = Modifier.padding(20.dp)) {
                                Text(
                                    "CPW Optimization Trend",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    "Shows how Cost-Per-Wear drops for your top most worn items.",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(24.dp))

                                val chartData = cpwList
                                    .filter { it.second > 0 && (it.first.price ?: 0f) > 0 }
                                    .sortedByDescending { it.second } // Sort by most worn
                                    .take(8) // Top 8 most worn

                                if (chartData.size >= 2) {
                                    val maxCpw = chartData.maxOf { it.third }.coerceAtLeast(1f)
                                    val minCpw = 0f
                                    val primaryColor = MaterialTheme.colorScheme.primary

                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(200.dp)
                                            .padding(bottom = 16.dp, start = 8.dp, end = 8.dp)
                                    ) {
                                        Canvas(modifier = Modifier.fillMaxSize()) {
                                            val width = size.width
                                            val height = size.height
                                            val points = mutableListOf<Offset>()

                                            // Draw horizontal grid lines
                                            val lineCount = 4
                                            for (i in 0..lineCount) {
                                                val y = height - (i * height / lineCount)
                                                drawLine(
                                                    color = Color.Gray.copy(alpha = 0.2f),
                                                    start = Offset(0f, y),
                                                    end = Offset(width, y),
                                                    strokeWidth = 1f
                                                )
                                            }

                                            // Calculate points
                                            chartData.forEachIndexed { index, data ->
                                                val x = index * (width / (chartData.size - 1))
                                                val y = height - ((data.third / maxCpw) * height)
                                                points.add(Offset(x, y))
                                            }

                                            // Draw the line
                                            for (i in 0 until points.size - 1) {
                                                drawLine(
                                                    color = primaryColor,
                                                    start = points[i],
                                                    end = points[i + 1],
                                                    strokeWidth = 6f
                                                )
                                            }

                                            // Draw data points
                                            points.forEach { point ->
                                                drawCircle(
                                                    color = primaryColor,
                                                    radius = 12f,
                                                    center = point
                                                )
                                                drawCircle(
                                                    color = Color.White,
                                                    radius = 6f,
                                                    center = point
                                                )
                                            }
                                        }
                                    }
                                } else {
                                    Box(modifier = Modifier.fillMaxWidth().height(150.dp), contentAlignment = Alignment.Center) {
                                        Text(
                                            text = "Not enough data for chart. Wear more items with price!",
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Best Investments & Wasted Budget
                    if (topInvestments.isNotEmpty()) {
                        item {
                            Text(
                                "Best Wardrobe Investments 💎",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        items(topInvestments, key = { it.first.id ?: "" }) { (item, wears, cpw) ->
                            InsightItemRow(item = item, wears = wears, cpw = cpw, isBest = true)
                        }
                    }

                    if (wastedBudget.isNotEmpty()) {
                        item {
                            Text(
                                "Highest Cost Per Wear (Waste Alert) ⚠️",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.error
                            )
                        }

                        items(wastedBudget, key = { it.first.id ?: "" }) { (item, wears, cpw) ->
                            InsightItemRow(item = item, wears = wears, cpw = cpw, isBest = false)
                        }
                    }
                }
            }
        }
    }
}

    // Edit Price Dialog
    if (editingItem != null) {
        AlertDialog(
            onDismissRequest = { editingItem = null },
            shape = RoundedCornerShape(24.dp),
            containerColor = MaterialTheme.colorScheme.surface,
            titleContentColor = MaterialTheme.colorScheme.primary,
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Outlined.AttachMoney,
                        contentDescription = null,
                        modifier = Modifier.size(28.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Set Price", fontSize = 22.sp, fontWeight = FontWeight.ExtraBold)
                }
            },
            text = {
                Column(modifier = Modifier.padding(top = 8.dp)) {
                    Text(
                        text = "Enter the purchase price for",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = editingItem!!.clothes_name,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    
                    Spacer(modifier = Modifier.height(20.dp))
                    
                    OutlinedTextField(
                        value = editPriceText,
                        onValueChange = { editPriceText = it },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        leadingIcon = {
                            Text(
                                "₫",
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                fontSize = 18.sp,
                                modifier = Modifier.padding(start = 8.dp)
                            )
                        },
                        textStyle = androidx.compose.ui.text.TextStyle(
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f),
                            focusedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.05f),
                            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                        ),
                        placeholder = { Text("e.g., 250000", fontSize = 16.sp) },
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val newPrice = editPriceText.toFloatOrNull()
                        if (newPrice != null && newPrice >= 0) {
                            val updatedItem = editingItem!!.copy(price = newPrice)
                            viewModel.updateClothingItem(updatedItem)
                        }
                        editingItem = null
                        Toast.makeText(context, "Price updated!", Toast.LENGTH_SHORT).show()
                    },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text("Save Price", fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { editingItem = null },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.onSurfaceVariant)
                ) {
                    Text("Cancel", fontWeight = FontWeight.Medium)
                }
            }
        )
    }
}

@Composable
fun InsightItemRow(
    item: ClothingItem,
    wears: Int,
    cpw: Float,
    isBest: Boolean
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val isDark = isSystemInDarkTheme()
            val badgeBg = if (isBest) {
                if (isDark) Color(0xFF1B3A24) else Color(0xFFE8F5E9)
            } else {
                if (isDark) Color(0xFF3B1E1E) else Color(0xFFFFEBEE)
            }
            val badgeTint = if (isBest) {
                if (isDark) Color(0xFF81C784) else Color(0xFF1B5E20)
            } else {
                if (isDark) Color(0xFFE57373) else Color(0xFFC62828)
            }
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(badgeBg),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isBest) Icons.Outlined.TrendingDown else Icons.Outlined.TrendingUp,
                    contentDescription = null,
                    tint = badgeTint
                )
            }
            
            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.clothes_name,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "Worn $wears times",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Text(
                text = String.format("CPW: %,.0f đ", cpw),
                fontSize = 13.sp,
                fontWeight = FontWeight.Black,
                color = badgeTint
            )
        }
    }
}

// Helper to convert color names into actual Jetpack Compose Colors
fun parseFashionColor(name: String): Color {
    return when (name.lowercase()) {
        "white" -> Color.White
        "black" -> Color(0xFF111111)
        "blue" -> Color(0xFF2196F3)
        "navy" -> Color(0xFF1A237E)
        "red" -> Color(0xFFE53935)
        "pink" -> Color(0xFFEC407A)
        "gray", "grey" -> Color(0xFF757575)
        "beige" -> Color(0xFFF5F5DC)
        "green" -> Color(0xFF4CAF50)
        "yellow" -> Color(0xFFFFEB3B)
        "brown" -> Color(0xFF795548)
        "purple" -> Color(0xFF9C27B0)
        "denim" -> Color(0xFF3F51B5)
        "olive" -> Color(0xFF556B2F)
        else -> Color.LightGray
    }
}

@Composable
fun SortChip(
    selected: Boolean,
    text: String,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
            .border(1.dp, if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f), RoundedCornerShape(8.dp))
            .clickable { onClick() }
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Text(
            text = text,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            softWrap = false
        )
    }
}
