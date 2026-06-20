package com.example.dacs3.dashboard

import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Checkroom
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
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
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.roundToInt

data class ColorWheelPin(
    val label: String,
    val colorName: String,
    val color: Color,
    val hue: Float,
    val isNeutral: Boolean
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StyleStudioScreen(
    viewModel: DashboardViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val userId = supabase.auth.currentUserOrNull()?.id ?: ""

    var selectedTabIndex by remember { mutableIntStateOf(0) }
    val tabs = listOf("Color Wheel 🎡", "Capsule Wardrobe 📊")

    // Outfits & Closet data
    val clothesList by viewModel.clothingItems.collectAsState()
    var outfitsList by remember { mutableStateOf<List<OutfitDbModel>>(emptyList()) }
    var outfitItemsList by remember { mutableStateOf<List<OutfitItemDbModel>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }

    // Selected outfit for color wheel analysis
    var selectedOutfitForAnalysis by remember { mutableStateOf<OutfitDbModel?>(null) }

    LaunchedEffect(userId) {
        if (userId.isNotEmpty()) {
            viewModel.getClothingItems(userId)
            isLoading = true
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
                        if (list.isNotEmpty()) {
                            selectedOutfitForAnalysis = list.first()
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                } finally {
                    withContext(Dispatchers.Main) {
                        isLoading = false
                    }
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "AI Style & Color Studio",
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Tab Row selector
            TabRow(
                selectedTabIndex = selectedTabIndex,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.primary,
                divider = {}
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTabIndex == index,
                        onClick = { selectedTabIndex = index },
                        text = { Text(title, fontWeight = FontWeight.Bold, fontSize = 14.sp) }
                    )
                }
            }

            if (isLoading) {
                Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                when (selectedTabIndex) {
                    0 -> ColorWheelTabContent(
                        outfits = outfitsList,
                        outfitItems = outfitItemsList,
                        clothes = clothesList,
                        selectedOutfit = selectedOutfitForAnalysis,
                        onSelectOutfit = { selectedOutfitForAnalysis = it }
                    )
                    1 -> CapsuleWardrobeTabContent(
                        clothes = clothesList,
                        outfits = outfitsList,
                        outfitItems = outfitItemsList
                    )
                }
            }
        }
    }
}

// ==========================================
// TAB 1: COLOR WHEEL HARMONY CONTENT
// ==========================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ColorWheelTabContent(
    outfits: List<OutfitDbModel>,
    outfitItems: List<OutfitItemDbModel>,
    clothes: List<ClothingItem>,
    selectedOutfit: OutfitDbModel?,
    onSelectOutfit: (OutfitDbModel) -> Unit
) {
    if (outfits.isEmpty() && clothes.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                "You don't have any clothing items or outfits yet.\nGo to Closet to add some items!",
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        return
    }

    var isCustomMode by remember { mutableStateOf(outfits.isEmpty()) }
    var customSelectedClothes by remember { mutableStateOf<List<ClothingItem>>(emptyList()) }
    var showOutfitSelectDialog by remember { mutableStateOf(false) }
    var showCustomSelectDialog by remember { mutableStateOf(false) }

    val currentOutfitClothes = if (selectedOutfit != null) {
        outfitItems
            .filter { it.outfitId == selectedOutfit.id }
            .mapNotNull { link -> clothes.find { it.id == link.clothingId } }
    } else {
        emptyList()
    }

    val activeClothes = if (isCustomMode) customSelectedClothes else currentOutfitClothes

    // Map clothing items to color coordinates on wheel
    val pins = remember(activeClothes) {
        activeClothes.mapNotNull { item ->
            val label = when {
                item.category.lowercase().contains("top") -> "Top"
                item.category.lowercase().contains("bottom") -> "Bottom"
                item.category.lowercase().contains("shoes") -> "Shoes"
                item.category.lowercase().contains("outerwear") -> "Outer"
                else -> "Item"
            }
            val colorName = item.mainColor ?: "White"
            val mapped = mapColorToWheel(colorName)
            if (mapped != null) {
                ColorWheelPin(
                    label = label,
                    colorName = colorName,
                    color = mapped.first,
                    hue = mapped.second,
                    isNeutral = mapped.third
                )
            } else {
                null
            }
        }
    }

    val harmonyResults = remember(pins) {
        evaluateColorHarmony(pins)
    }

    val animatedScore by androidx.compose.animation.core.animateIntAsState(
        targetValue = harmonyResults.second,
        animationSpec = androidx.compose.animation.core.tween(durationMillis = 800),
        label = "ScoreAnimation"
    )

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Mode Selector: Saved Outfit vs Custom Mix
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (outfits.isNotEmpty()) {
                    FilterChip(
                        selected = !isCustomMode,
                        onClick = { isCustomMode = false },
                        label = { Text("Saved Outfit 👔") },
                        leadingIcon = if (!isCustomMode) {
                            { Icon(Icons.Filled.Check, null, modifier = Modifier.size(16.dp)) }
                        } else null
                    )
                }
                FilterChip(
                    selected = isCustomMode,
                    onClick = { isCustomMode = true },
                    label = { Text("Custom Mix 🎨") },
                    leadingIcon = if (isCustomMode) {
                        { Icon(Icons.Filled.Check, null, modifier = Modifier.size(16.dp)) }
                    } else null
                )
            }
        }

        // Selector Button / Card
        item {
            if (!isCustomMode) {
                // Outfit Selector Button
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showOutfitSelectDialog = true },
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("Selected Outfit for Analysis", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(selectedOutfit?.name ?: "Select Outfit", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = MaterialTheme.colorScheme.primary)
                            }
                            Icon(Icons.Filled.ArrowDropDown, null, tint = MaterialTheme.colorScheme.primary)
                        }

                        if (currentOutfitClothes.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(12.dp))
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                items(currentOutfitClothes) { item ->
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Box(
                                            modifier = Modifier
                                                .size(50.dp)
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                        ) {
                                            if (!item.imageUrl.isNullOrEmpty()) {
                                                AsyncImage(
                                                    model = item.imageUrl,
                                                    contentDescription = item.clothes_name,
                                                    contentScale = ContentScale.Crop,
                                                    modifier = Modifier.fillMaxSize()
                                                )
                                            } else {
                                                Icon(
                                                    Icons.Outlined.Checkroom,
                                                    null,
                                                    modifier = Modifier.size(24.dp).align(Alignment.Center)
                                                )
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = item.clothes_name ?: "",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Medium,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.width(50.dp),
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                // Custom Mix Card
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showCustomSelectDialog = true },
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("Custom Color Mix Analysis", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = if (customSelectedClothes.isEmpty()) "Tap to select clothes ➕" else "${customSelectedClothes.size} Items Selected",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            Icon(Icons.Filled.Edit, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                        }

                        if (customSelectedClothes.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(12.dp))
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                items(customSelectedClothes) { item ->
                                    Box(contentAlignment = Alignment.TopEnd) {
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            modifier = Modifier.padding(top = 4.dp, end = 4.dp)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(50.dp)
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                                            ) {
                                                if (!item.imageUrl.isNullOrEmpty()) {
                                                    AsyncImage(
                                                        model = item.imageUrl,
                                                        contentDescription = item.clothes_name,
                                                        contentScale = ContentScale.Crop,
                                                        modifier = Modifier.fillMaxSize()
                                                    )
                                                } else {
                                                    Icon(
                                                        Icons.Outlined.Checkroom,
                                                        null,
                                                        modifier = Modifier.size(24.dp).align(Alignment.Center)
                                                    )
                                                }
                                            }
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = item.clothes_name ?: "",
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Medium,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                                modifier = Modifier.width(50.dp),
                                                textAlign = TextAlign.Center
                                            )
                                        }

                                        // Remove Button
                                        Box(
                                            modifier = Modifier
                                                .size(16.dp)
                                                .clip(CircleShape)
                                                .background(MaterialTheme.colorScheme.error)
                                                .clickable {
                                                    customSelectedClothes = customSelectedClothes.filter { it.id != item.id }
                                                },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                Icons.Filled.Close,
                                                null,
                                                tint = Color.White,
                                                modifier = Modifier.size(10.dp)
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

        // Color Wheel Canvas Card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp),
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(20.dp),
                    contentAlignment = Alignment.Center
                ) {
                    ColorWheelCanvas(pins = pins)
                }
            }
        }

        // Analysis Results Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = harmonyResults.first, // Harmony Type Label
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 16.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            CircularProgressIndicator(
                                progress = { animatedScore / 100f },
                                modifier = Modifier.size(24.dp),
                                color = MaterialTheme.colorScheme.primary,
                                strokeWidth = 3.dp,
                                trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                            )
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(MaterialTheme.colorScheme.primaryContainer)
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = "Score: ${animatedScore}%",
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = harmonyResults.third, // AI Styled critique
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Medium,
                        lineHeight = 18.sp
                    )

                    if (pins.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(14.dp))
                        HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
                        Spacer(modifier = Modifier.height(12.dp))

                        // Render mapped color indicators
                        Text("Outfit Color Palette", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            pins.forEach { pin ->
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(12.dp)
                                            .clip(CircleShape)
                                            .background(pin.color)
                                            .border(0.5.dp, Color.Gray, CircleShape)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("${pin.label}: ${pin.colorName}", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // DIALOG: OUTFIT SELECTOR
    if (showOutfitSelectDialog) {
        Dialog(onDismissRequest = { showOutfitSelectDialog = false }) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
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
                        Text("Select Outfit", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        IconButton(onClick = { showOutfitSelectDialog = false }) {
                            Icon(Icons.Filled.Close, null)
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.weight(1f, fill = false)
                    ) {
                        items(outfits) { outfit ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                                    .clickable {
                                        onSelectOutfit(outfit)
                                        showOutfitSelectDialog = false
                                    }
                                    .padding(12.dp)
                            ) {
                                Text(outfit.name, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            }
                        }
                    }
                }
            }
        }
    }

    // DIALOG: CUSTOM SELECTOR
    if (showCustomSelectDialog) {
        Dialog(onDismissRequest = { showCustomSelectDialog = false }) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.85f)
                    .padding(16.dp),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier
                        .padding(20.dp)
                        .fillMaxSize()
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Select Clothes 👕",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                        IconButton(onClick = { showCustomSelectDialog = false }) {
                            Icon(Icons.Filled.Close, null)
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))

                    if (clothes.isEmpty()) {
                        Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                            Text("No clothes available to select.")
                        }
                    } else {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            items(clothes) { item ->
                                val isSelected = customSelectedClothes.any { it.id == item.id }
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(
                                            if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)
                                            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                                        )
                                        .clickable {
                                            customSelectedClothes = if (isSelected) {
                                                customSelectedClothes.filter { it.id != item.id }
                                            } else {
                                                customSelectedClothes + item
                                            }
                                        }
                                        .padding(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(45.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(MaterialTheme.colorScheme.surface)
                                    ) {
                                        if (!item.imageUrl.isNullOrEmpty()) {
                                            AsyncImage(
                                                model = item.imageUrl,
                                                contentDescription = item.clothes_name,
                                                contentScale = ContentScale.Crop,
                                                modifier = Modifier.fillMaxSize()
                                            )
                                        } else {
                                            Icon(
                                                Icons.Outlined.Checkroom,
                                                null,
                                                modifier = Modifier.size(20.dp).align(Alignment.Center)
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            item.clothes_name ?: "Unnamed Item",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp
                                        )
                                        Text(
                                            "${item.category} • ${item.mainColor ?: "No Color"}",
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Checkbox(
                                        checked = isSelected,
                                        onCheckedChange = { checked ->
                                            customSelectedClothes = if (isSelected) {
                                                customSelectedClothes.filter { it.id != item.id }
                                            } else {
                                                customSelectedClothes + item
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = { showCustomSelectDialog = false },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Done (${customSelectedClothes.size} items)")
                    }
                }
            }
        }
    }
}

// DRAW DIGITAL COLOR WHEEL
@Composable
fun ColorWheelCanvas(pins: List<ColorWheelPin>) {
    val ringColor = MaterialTheme.colorScheme.surface
    val primaryColor = MaterialTheme.colorScheme.primary
    val isDarkTheme = ringColor.red < 0.5f
    val labelColorInt = if (isDarkTheme) android.graphics.Color.WHITE else android.graphics.Color.BLACK

    // Pre-calculate gradient colors for optimal draw performance
    val sweepColors = remember {
        (0..36).map { i ->
            val hue = (i * 10 + 90) % 360
            Color.hsv(hue.toFloat(), 0.95f, 0.95f)
        }
    }

    Canvas(modifier = Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        val cX = w / 2f
        val cY = h / 2f
        val radius = size.minDimension / 2f
        val center = Offset(cX, cY)

        // 1. Draw coordinate grid lines (Dashed circles and radial lines)
        val gridColor = Color.LightGray.copy(alpha = 0.22f)
        // Concentric grid circles
        drawCircle(
            color = gridColor,
            radius = radius * 0.78f,
            center = center,
            style = Stroke(width = 1.dp.toPx(), pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(6f, 6f), 0f))
        )
        drawCircle(
            color = gridColor,
            radius = radius * 0.38f,
            center = center,
            style = Stroke(width = 1.dp.toPx(), pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(6f, 6f), 0f))
        )
        // Radial lines at 45 degree intervals
        for (i in 0 until 8) {
            val angleRad = Math.toRadians(i * 45.0)
            val startX = cX + radius * 0.15f * cos(angleRad).toFloat()
            val startY = cY + radius * 0.15f * sin(angleRad).toFloat()
            val endX = cX + radius * 0.95f * cos(angleRad).toFloat()
            val endY = cY + radius * 0.95f * sin(angleRad).toFloat()
            drawLine(
                color = gridColor,
                start = Offset(startX, startY),
                end = Offset(endX, endY),
                strokeWidth = 1.dp.toPx(),
                pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(12f, 12f), 0f)
            )
        }

        // 2. Draw color spectrum using highly optimized SweepGradient
        drawCircle(
            brush = Brush.sweepGradient(
                colors = sweepColors,
                center = center
            ),
            radius = radius,
            center = center
        )

        // Draw center circle mask to make it a ring
        drawCircle(
            color = ringColor,
            radius = radius * 0.55f,
            center = center
        )

        // Draw ring boundary strokes
        drawCircle(
            color = Color.LightGray.copy(alpha = 0.25f),
            radius = radius,
            center = center,
            style = Stroke(width = 1.5.dp.toPx())
        )
        drawCircle(
            color = Color.LightGray.copy(alpha = 0.25f),
            radius = radius * 0.55f,
            center = center,
            style = Stroke(width = 1.5.dp.toPx())
        )

        // Plot pins and coordinates
        val points = pins.map { pin ->
            val angleRad = Math.toRadians(pin.hue.toDouble() - 90.0) // offset 90deg so Red is at top
            val rMult = if (pin.isNeutral) 0.22f else 0.78f // Neutrals sit inside center, vibrants on outer ring
            val pinRadius = radius * rMult
            val pX = cX + pinRadius * cos(angleRad).toFloat()
            val pY = cY + pinRadius * sin(angleRad).toFloat()
            Offset(pX, pY) to pin
        }

        // Draw connection lines between vibrant pins
        val vibrantPoints = points.filter { !it.second.isNeutral }.map { it.first }
        if (vibrantPoints.size == 2) {
            drawLine(
                color = primaryColor.copy(alpha = 0.7f),
                start = vibrantPoints[0],
                end = vibrantPoints[1],
                strokeWidth = 2.5.dp.toPx(),
                pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(12f, 12f), 0f)
            )
        } else if (vibrantPoints.size >= 3) {
            val path = Path().apply {
                moveTo(vibrantPoints[0].x, vibrantPoints[0].y)
                for (i in 1 until vibrantPoints.size) {
                    lineTo(vibrantPoints[i].x, vibrantPoints[i].y)
                }
                close()
            }
            drawPath(
                path = path,
                color = primaryColor.copy(alpha = 0.35f),
                style = Stroke(width = 2.5.dp.toPx())
            )
        }

        // Draw pin nodes with glow, halo and labels
        points.forEach { (point, pin) ->
            // Draw large soft colored glow halo
            drawCircle(
                color = pin.color.copy(alpha = 0.25f),
                radius = 16.dp.toPx(),
                center = point
            )
            // Node outer drop shadow
            drawCircle(
                color = Color.Black.copy(alpha = 0.15f),
                radius = 10.dp.toPx(),
                center = point
            )
            // Outer white circle
            drawCircle(
                color = Color.White,
                radius = 8.dp.toPx(),
                center = point
            )
            // Inner mapped color circle
            drawCircle(
                color = pin.color,
                radius = 5.5.dp.toPx(),
                center = point
            )

            // Draw text label on canvas
            val textPaint = android.graphics.Paint().apply {
                color = labelColorInt
                textSize = 26f
                typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
                textAlign = android.graphics.Paint.Align.CENTER
                setShadowLayer(4f, 0f, 2f, android.graphics.Color.argb(140, 0, 0, 0))
            }
            drawContext.canvas.nativeCanvas.drawText(
                pin.label,
                point.x,
                point.y - 12.dp.toPx(),
                textPaint
            )
        }
    }
}

// Convert Color names to Hue & color values
private fun mapColorToWheel(colorName: String): Triple<Color, Float, Boolean>? {
    val clean = colorName.lowercase().trim()
    return when {
        clean.contains("red") || clean.contains("đỏ") -> Triple(Color.Red, 0f, false)
        clean.contains("orange") || clean.contains("cam") -> Triple(Color(0xFFFF9800), 30f, false)
        clean.contains("yellow") || clean.contains("vàng") -> Triple(Color.Yellow, 60f, false)
        clean.contains("green") || clean.contains("xanh lá") -> Triple(Color.Green, 120f, false)
        clean.contains("teal") || clean.contains("xanh lục") -> Triple(Color(0xFF009688), 160f, false)
        clean.contains("blue") || clean.contains("xanh dương") || clean.contains("xanh biển") -> Triple(Color.Blue, 240f, false)
        clean.contains("purple") || clean.contains("tím") -> Triple(Color(0xFF9C27B0), 280f, false)
        clean.contains("pink") || clean.contains("hồng") -> Triple(Color(0xFFE91E63), 320f, false)
        clean.contains("brown") || clean.contains("nâu") -> Triple(Color(0xFF795548), 25f, false)
        
        // Neutrals
        clean.contains("black") || clean.contains("đen") -> Triple(Color.Black, 0f, true)
        clean.contains("white") || clean.contains("trắng") -> Triple(Color.White, 0f, true)
        clean.contains("grey") || clean.contains("gray") || clean.contains("xám") -> Triple(Color.Gray, 0f, true)
        clean.contains("beige") || clean.contains("be") -> Triple(Color(0xFFF5F5DC), 40f, true)
        clean.contains("navy") || clean.contains("xanh đen") -> Triple(Color(0xFF000080), 220f, true)
        else -> null
    }
}

// Math evaluation color wheel harmony rules
private fun evaluateColorHarmony(pins: List<ColorWheelPin>): Triple<String, Int, String> {
    if (pins.isEmpty()) return Triple("No Colors Plotted", 0, "No clothes colors detected in this combination. Try editing the item properties.")
    
    val vibrants = pins.filter { !it.isNeutral }
    val neutrals = pins.filter { it.isNeutral }
    
    // Rule: Neutrals only
    if (vibrants.isEmpty()) {
        return Triple(
            "Monochromatic Neutral Harmony 🔘",
            100,
            "Your outfit is made entirely of neutral tones (Black, White, Grey, Beige). This creates a timeless, high-contrast, and extremely safe aesthetic. Perfect for minimalist and capsule style layouts!"
        )
    }

    // Rule: Neutral Accent (1 vibrant + neutrals)
    if (vibrants.size == 1) {
        val colorName = vibrants.first().colorName
        return Triple(
            "Neutral Accent Harmony 💫",
            95,
            "You coordinated 1 statement color ($colorName) with clean neutral bases. This is a highly professional styling formula. It lets the $colorName pop elegantly without making the look messy."
        )
    }

    // Evaluate vibrant colors combinations
    if (vibrants.size == 2) {
        val h1 = vibrants[0].hue
        val h2 = vibrants[1].hue
        val diff = abs(h1 - h2)
        val angle = if (diff > 180f) 360f - diff else diff

        return when {
            angle <= 20f -> Triple(
                "Monochromatic Harmony 🎨",
                100,
                "The colors of your top and bottom sit very close on the color wheel (under 20°). This creates a elongated silhouette and cohesive flow. Perfect for premium monochromatic layering!"
            )
            angle in 21f..50f -> Triple(
                "Analogous Harmony 🌿",
                90,
                "Your outfit utilizes Analogous colors (adjacent on the color wheel). This produces a balanced, harmonious look frequently seen in nature and high-fashion collections. Warm, peaceful, and visually soothing."
            )
            angle in 150f..180f -> Triple(
                "Complementary Harmony 🌗",
                95,
                "Excellent choice! You paired Complementary colors (opposite sides of the color wheel). This creates maximum contrast and vibrant energy. A bold, high-fashion statement that catches the eye immediately."
            )
            else -> Triple(
                "Casual Mix Coordination 👕",
                70,
                "Your vibrant colors are placed in a neutral angle (${angle.roundToInt()}°). The look is bold but can feel disconnected. Try matching one of these with a neutral base (Beige or Black) to restore perfect color balance."
            )
        }
    }

    if (vibrants.size >= 3) {
        val h1 = vibrants[0].hue
        val h2 = vibrants[1].hue
        val h3 = vibrants[2].hue

        val d1 = abs(h1 - h2).let { if (it > 180f) 360f - it else it }
        val d2 = abs(h2 - h3).let { if (it > 180f) 360f - it else it }
        val d3 = abs(h3 - h1).let { if (it > 180f) 360f - it else it }

        val isTriadic = abs(d1 - 120f) < 30f && abs(d2 - 120f) < 30f && abs(d3 - 120f) < 30f
        if (isTriadic) {
            return Triple(
                "Triadic Color Harmony 🛞",
                95,
                "Superb styling! Your items form a Triadic Harmony (forming a triangle on the color wheel). This distributes colors evenly, resulting in a rich, vibrant, and perfectly balanced composition."
            )
        }

        val allClose = d1 <= 45f && d2 <= 45f && d3 <= 45f
        if (allClose) {
            return Triple(
                "Extended Analogous Harmony 🍁",
                90,
                "Beautiful adjacent blending! All three vibrant colors sit close together. This creates a rich, themed palette that looks unified and highly curated."
            )
        }
    }

    return Triple(
        "Eclectic Coordination 🌐",
        65,
        "You've combined multiple vibrant tones. While creative, this can overwhelm the eye. Try substituting one item (like your shoes or trousers) with a neutral black, white, or beige item to create a primary focus point."
    )
}

// ==========================================
// TAB 2: CAPSULE WARDROBE CONTENT
// ==========================================
@Composable
fun CapsuleWardrobeTabContent(
    clothes: List<ClothingItem>,
    outfits: List<OutfitDbModel>,
    outfitItems: List<OutfitItemDbModel>
) {
    if (clothes.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
            Text("No clothing items found. Add clothes in Closet first!", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        return
    }

    // Calculate Capsule Wardrobe metrics
    val totalCount = clothes.size
    
    // Neutrals: Black, White, Grey, Beige, Navy
    val neutralColors = listOf("black", "white", "grey", "gray", "beige", "be", "navy", "xanh đen", "đen", "trắng", "xám")
    val neutralItems = clothes.filter { item ->
        val col = item.mainColor?.lowercase() ?: ""
        neutralColors.any { col.contains(it) }
    }

    val neutralRatio = if (totalCount > 0) neutralItems.size.toFloat() / totalCount else 0f
    
    // Core Categories completeness
    val topsNeutrals = neutralItems.filter { it.category.lowercase().run { contains("top") || contains("áo") || contains("shirt") } }
    val bottomsNeutrals = neutralItems.filter { it.category.lowercase().run { contains("bottom") || contains("quần") || contains("pants") } }
    val shoesNeutrals = neutralItems.filter { it.category.lowercase().run { contains("shoes") || contains("giày") } }
    val outerwearNeutrals = neutralItems.filter { it.category.lowercase().run { contains("outerwear") || contains("áo khoác") } }

    var categoryScore = 0
    if (topsNeutrals.isNotEmpty()) categoryScore += 25
    if (bottomsNeutrals.isNotEmpty()) categoryScore += 25
    if (shoesNeutrals.isNotEmpty()) categoryScore += 25
    if (outerwearNeutrals.isNotEmpty()) categoryScore += 25

    // Versatility Index: weighted category coverage and color ratios
    val versatilityIndex = ((categoryScore * 0.7f) + (neutralRatio * 30f)).roundToInt().coerceIn(5, 100)

    val animatedVersatilityIndex by androidx.compose.animation.core.animateIntAsState(
        targetValue = versatilityIndex,
        animationSpec = androidx.compose.animation.core.tween(durationMillis = 800),
        label = "VersatilityAnimation"
    )

    // Calculate missing capsule essentials
    val missingEssentials = remember(clothes) {
        val list = mutableListOf<String>()
        if (topsNeutrals.isEmpty()) list.add("Plain White / Black Tee 👕")
        if (bottomsNeutrals.isEmpty()) list.add("Classic Black Jeans / Chinos 👖")
        if (outerwearNeutrals.isEmpty()) list.add("Grey / Navy Blazer or Hoodie 🧥")
        if (shoesNeutrals.isEmpty()) list.add("Minimalist White Leather Sneakers 👟")
        list
    }

    // Auto-generate Capsule combinations (Tops + Bottoms + Shoes) from clean neutrals
    val capsuleCombos = remember(topsNeutrals, bottomsNeutrals, shoesNeutrals) {
        val list = mutableListOf<Triple<ClothingItem, ClothingItem, ClothingItem?>>()
        val cleanTops = topsNeutrals.filter { it.status.uppercase() !in listOf("WORN", "IN_WASH") }
        val cleanBottoms = bottomsNeutrals.filter { it.status.uppercase() !in listOf("WORN", "IN_WASH") }
        val cleanShoes = shoesNeutrals.filter { it.status.uppercase() !in listOf("WORN", "IN_WASH") }

        val limit = 5
        var count = 0
        for (top in cleanTops) {
            for (bottom in cleanBottoms) {
                if (count >= limit) break
                val shoes = cleanShoes.firstOrNull()
                list.add(Triple(top, bottom, shoes))
                count++
            }
        }
        list
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Versatility Dashboard
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.12f))
            ) {
                Row(
                    modifier = Modifier.padding(20.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "Closet Versatility Index",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 18.sp,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = when {
                                versatilityIndex > 80 -> "Elite Capsule Wardrobe! 💎"
                                versatilityIndex > 50 -> "Balanced Mix Closet. 👍"
                                else -> "High Redundancy Closet. ⚠️"
                            },
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        LinearProgressIndicator(
                            progress = { animatedVersatilityIndex / 100f },
                            modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                            color = MaterialTheme.colorScheme.primary,
                            trackColor = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.2f)
                        )
                    }

                    Spacer(modifier = Modifier.width(20.dp))

                    Box(
                        modifier = Modifier.size(80.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            progress = { animatedVersatilityIndex / 100f },
                            modifier = Modifier.fillMaxSize(),
                            color = MaterialTheme.colorScheme.primary,
                            strokeWidth = 6.dp,
                            trackColor = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.15f)
                        )
                        Text(
                            "${animatedVersatilityIndex}%",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 18.sp,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }
        }

        // Smart Purchase Recommendations (Missing Links)
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.AutoAwesome, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("AI Shopping Advice", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MaterialTheme.colorScheme.primary)
                    }
                    Spacer(modifier = Modifier.height(10.dp))

                    if (missingEssentials.isEmpty()) {
                        Text(
                            "Perfect! Your closet contains all key neutral capsule basics. You have a highly optimized and versatile foundation.",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Medium
                        )
                    } else {
                        Text(
                            "Adding these neutral basics will multiply your outfit combinations exponentially:",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        missingEssentials.forEach { essential ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Filled.Check, null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(essential, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
            }
        }

        // Auto-Generated Capsule Outfits
        if (capsuleCombos.isNotEmpty()) {
            item {
                Text(
                    "Auto-Generated Capsule Outfits",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            items(capsuleCombos) { (top, bottom, shoes) ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f)),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            modifier = Modifier.weight(1f),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOfNotNull(top, bottom, shoes).forEach { cloth ->
                                Box(
                                    modifier = Modifier
                                        .size(50.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(MaterialTheme.colorScheme.surfaceVariant)
                                ) {
                                    if (cloth.imageUrl.isNotEmpty()) {
                                        AsyncImage(
                                            model = cloth.imageUrl,
                                            contentDescription = cloth.clothes_name,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    } else {
                                        Icon(Icons.Outlined.Checkroom, null, modifier = Modifier.size(20.dp).align(Alignment.Center))
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            Column(modifier = Modifier.align(Alignment.CenterVertically)) {
                                Text(
                                    text = "Minimalist Capsule",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                                Text(
                                    text = "Colors: ${top.mainColor ?: "N/A"} + ${bottom.mainColor ?: "N/A"}",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Icon(
                            Icons.Filled.ChevronRight,
                            null,
                            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                        )
                    }
                }
            }
        }
    }
}
