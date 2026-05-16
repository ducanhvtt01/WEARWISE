package com.example.dacs3.dashboard

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.dacs3.connectDB.ClothingItem
import com.example.dacs3.connectDB.DashboardViewModel
import kotlinx.coroutines.launch
import coil.compose.AsyncImage
import androidx.compose.ui.layout.ContentScale
import kotlinx.serialization.SerialName
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.os.Build
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.LocalContext
import io.github.jan.supabase.gotrue.auth
import com.example.dacs3.connectDB.supabase
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClosetScreen(viewModel: DashboardViewModel, onNavigateToStylist: () -> Unit = {}) {
    var selectedCategory by remember { mutableStateOf("All") }
    val categories = listOf("All", "Top", "Bottom", "Outerwear", "Shoes", "Accessories")

    var isSearching by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }

    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current

    // --- STATE CHO BOTTOM SHEET ---
    var showItemSheet by remember { mutableStateOf(false) }
    var showAddManualSheet by remember { mutableStateOf(false) }
    var selectedItemToEdit by remember { mutableStateOf<ClothingItem?>(null) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    LaunchedEffect(isSearching) {
        if (isSearching) {
            focusRequester.requestFocus()
            keyboardController?.show()
        } else {
            keyboardController?.hide()
        }
    }

    // Lắng nghe dữ liệu thật từ ViewModel
    val itemsList by viewModel.clothingItems.collectAsState()
    val feedbackMap by viewModel.clothingFeedbackMap.collectAsState() // New

    val filteredItems = itemsList.filter {
        (selectedCategory == "All" || it.category.equals(selectedCategory, ignoreCase = true)) &&
                (it.clothes_name.contains(searchQuery, ignoreCase = true) || it.mainColor?.contains(
                    searchQuery,
                    ignoreCase = true
                ) == true)
    }

    // --- HELPER FUNCTION TÍNH TOÁN "DEAD ITEM" (CHƯA MẶC > 90 NGÀY) ---
    val isItemDead: (ClothingItem) -> Boolean = { item ->
        val formatter = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
        val todayDate = java.util.Date()
        // Dùng last_worn_date, nếu null thì dùng created_at. Nếu vẫn null thì coi như món đồ mới
        val referenceDateString = item.lastWornDate ?: item.createdAt
        if (referenceDateString != null) {
            try {
                val refDate = formatter.parse(referenceDateString.substring(0, 10))
                if (refDate != null) {
                    val diffInMillies = kotlin.math.abs(todayDate.time - refDate.time)
                    val diffInDays = java.util.concurrent.TimeUnit.DAYS.convert(diffInMillies, java.util.concurrent.TimeUnit.MILLISECONDS)
                    diffInDays > 90
                } else false
            } catch (e: Exception) { false }
        } else false
    }

    // --- BOTTOM SHEET HIỂN THỊ CHI TIẾT ĐỂ SỬA ---
    if (showItemSheet && selectedItemToEdit != null) {
        ModalBottomSheet(
            onDismissRequest = { showItemSheet = false },
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.background
        ) {
            ItemDetailSheetContent(
                item = selectedItemToEdit!!,
                onSave = { updatedItem ->
                    // Lưu dữ liệu cập nhật lên Database
                    viewModel.updateClothingItem(updatedItem)
                    showItemSheet = false
                    scope.launch { snackbarHostState.showSnackbar("Updated ${updatedItem.clothes_name}!") }
                },
                onStyleWithAI = {
                    showItemSheet = false
                    onNavigateToStylist()
                },
                onDelete = {
                    val itemToRemove = selectedItemToEdit!!
                    // Xóa dữ liệu khỏi Database
                    viewModel.deleteClothingItem(itemToRemove)
                    showItemSheet = false
                    scope.launch {
                        val result = snackbarHostState.showSnackbar(
                            "Deleted ${itemToRemove.clothes_name}",
                            "Undo", // Nếu có undo thì bạn phải gọi lại viewModel.addClothing() nhé
                            duration = SnackbarDuration.Short
                        )
                        if (result == SnackbarResult.ActionPerformed) {
                            viewModel.addClothing(itemToRemove) {}
                        }
                    }
                }
            )
        }
    }

    // --- BOTTOM SHEET HIỂN THỊ THÊM ĐỒ THỦ CÔNG ---
    if (showAddManualSheet) {
        ModalBottomSheet(
            onDismissRequest = { showAddManualSheet = false },
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.background
        ) {
            AddManualSheetContent(
                onSave = { newItem, bitmap ->
                    if (bitmap != null) {
                        viewModel.uploadAndSaveClothes(bitmap, newItem) {}
                    } else {
                        viewModel.addClothing(newItem) {}
                    }
                    showAddManualSheet = false
                    scope.launch { snackbarHostState.showSnackbar("Added new item!") }
                },
                onCancel = { showAddManualSheet = false }
            )
        }
    }

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
            // --- HEADER ---
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(90.dp)
                    .padding(top = 40.dp, bottom = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (!isSearching) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.DryCleaning,
                            null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(35.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "Closet",
                            fontSize = 35.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.primary,
                            letterSpacing = (-0.5).sp
                        )
                    }
                    IconButton(
                        onClick = { isSearching = true },
                        modifier = Modifier.background(
                            MaterialTheme.colorScheme.surface,
                            CircleShape
                        )
                    ) {
                        Icon(
                            Icons.Filled.Search,
                            "Search",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(30.dp)
                        )
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .background(
                                MaterialTheme.colorScheme.surface,
                                RoundedCornerShape(12.dp)
                            )
                            .padding(horizontal = 12.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        BasicTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            modifier = Modifier
                                .fillMaxWidth()
                                .focusRequester(focusRequester),
                            singleLine = true,
                            textStyle = TextStyle(
                                color = MaterialTheme.colorScheme.primary,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium
                            ),
                            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                            decorationBox = { innerTextField ->
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(
                                        Icons.Filled.Search,
                                        null,
                                        tint = MaterialTheme.colorScheme.secondary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Box(modifier = Modifier.weight(1f)) {
                                        if (searchQuery.isEmpty()) Text(
                                            "Search clothes...",
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            fontSize = 14.sp
                                        )
                                        innerTextField()
                                    }
                                    IconButton(onClick = {
                                        if (searchQuery.isEmpty()) isSearching =
                                            false else searchQuery = ""
                                    }, modifier = Modifier.size(24.dp)) {
                                        Icon(
                                            Icons.Filled.Close,
                                            null,
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        )
                    }
                }
            }

            // ─── OPTION B: PREMIUM ICON-BASED CATEGORY FILTER ───
            val categoryIcons: Map<String, ImageVector> = mapOf(
                "All" to Icons.Filled.GridView,
                "Top" to Icons.Filled.Checkroom,
                "Bottom" to Icons.Filled.Style,
                "Outerwear" to Icons.Filled.AcUnit,
                "Shoes" to Icons.Outlined.DirectionsWalk,
                "Accessories" to Icons.Outlined.Watch
            )
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(end = 8.dp),
                modifier = Modifier.padding(bottom = 20.dp)
            ) {
                items(categories.size) { index ->
                    val category = categories[index]
                    val isSelected = selectedCategory == category
                    val animatedBgAlpha by animateFloatAsState(
                        targetValue = if (isSelected) 1f else 0f,
                        animationSpec = spring(stiffness = Spring.StiffnessMedium),
                        label = "chipBg$index"
                    )
                    Box(
                        modifier = Modifier
                            .height(36.dp)
                            .clip(RoundedCornerShape(50))
                            .background(
                                if (isSelected)
                                    Brush.horizontalGradient(
                                        listOf(
                                            MaterialTheme.colorScheme.primary,
                                            MaterialTheme.colorScheme.secondary.copy(alpha = 0.8f)
                                        )
                                    )
                                else
                                    Brush.horizontalGradient(
                                        listOf(
                                            MaterialTheme.colorScheme.surface,
                                            MaterialTheme.colorScheme.surface
                                        )
                                    )
                            )
                            .clickable(indication = null, interactionSource = remember { MutableInteractionSource() }) {
                                selectedCategory = category
                            }
                            .padding(horizontal = 14.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            categoryIcons[category]?.let { icon ->
                                Icon(
                                    icon, null,
                                    modifier = Modifier.size(14.dp),
                                    tint = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                )
                            }
                            Text(
                                category,
                                fontSize = 13.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            )
                        }
                    }
                }
            }

            // --- GRID ITEMS ---
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(bottom = 120.dp)
            ) {
                item {
                    Card(
                        modifier = Modifier.height(200.dp),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                        onClick = { showAddManualSheet = true }
                    ) {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .background(MaterialTheme.colorScheme.secondary, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Filled.Add,
                                    null,
                                    tint = MaterialTheme.colorScheme.onPrimary
                                )
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                "Add New",
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }

                items(items = filteredItems, key = { it.id ?: "" }) { item ->
                    val dismissState = rememberSwipeToDismissBoxState(
                        confirmValueChange = { dismissValue ->
                            if (dismissValue == SwipeToDismissBoxValue.EndToStart) {
                                // Gọi xóa từ DB khi vuốt
                                viewModel.deleteClothingItem(item)
                                scope.launch {
                                    val result = snackbarHostState.showSnackbar(
                                        "Deleted ${item.clothes_name}",
                                        "Undo",
                                        duration = SnackbarDuration.Short
                                    )
                                    if (result == SnackbarResult.ActionPerformed) {
                                        viewModel.addClothing(item) {}
                                    }
                                }
                                true
                            } else false
                        }
                    )

                    SwipeToDismissBox(
                        state = dismissState,
                        enableDismissFromStartToEnd = false,
                        backgroundContent = {
                            val color by animateColorAsState(if (dismissState.targetValue == SwipeToDismissBoxValue.EndToStart) MaterialTheme.colorScheme.error else Color.Transparent)
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(color)
                                    .padding(end = 20.dp),
                                contentAlignment = Alignment.CenterEnd
                            ) {
                                Icon(
                                    Icons.Filled.Delete,
                                    "Delete",
                                    tint = Color.White,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                        },
                        content = {
                            val isDead = isItemDead(item)
                            val rating = feedbackMap[item.id]
                            ClosetItemCard(
                                item = item, 
                                isDead = isDead,
                                rating = rating,
                                onClick = {
                                    selectedItemToEdit = item
                                    showItemSheet = true
                                }
                            )
                        }
                    )
                }
            }
        }
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 90.dp)
        )
    }
}

@Composable
fun ClosetItemCard(
    item: ClothingItem,
    isDead: Boolean = false,
    rating: Int? = null,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "cardScale"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(220.dp)
            .graphicsLayer(scaleX = scale, scaleY = scale)
            .shadow(
                elevation = if (isDead) 10.dp else 6.dp,
                shape = RoundedCornerShape(20.dp),
                spotColor = if (isDead) MaterialTheme.colorScheme.error.copy(alpha = 0.4f)
                            else MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
            )
            .clip(RoundedCornerShape(20.dp))
            .clickable(interactionSource = interactionSource, indication = null) { onClick() }
    ) {
        // ─── Full-bleed background: image or placeholder ───
        if (item.imageUrl.isNotEmpty()) {
            AsyncImage(
                model = item.imageUrl,
                contentDescription = item.clothes_name,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                MaterialTheme.colorScheme.surfaceVariant,
                                MaterialTheme.colorScheme.surface
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Outlined.Checkroom, null,
                    modifier = Modifier.size(48.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                )
            }
        }

        // ─── Bottom gradient overlay for text readability ───
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(100.dp)
                .align(Alignment.BottomCenter)
                .background(
                    Brush.verticalGradient(
                        listOf(Color.Transparent, Color.Black.copy(alpha = 0.75f))
                    )
                )
        )

        // ─── Item name at the bottom ───
        Text(
            text = item.clothes_name,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color.White,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(horizontal = 12.dp, vertical = 10.dp)
        )

        // ─── Top-right badges: Rarely Worn + Feedback ───
        Row(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(8.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            if (isDead) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(
                            Brush.horizontalGradient(
                                listOf(Color(0xFFFF6B35), Color(0xFFFF3D00))
                            )
                        )
                        .padding(horizontal = 7.dp, vertical = 3.dp)
                ) {
                    Text("Rarely Worn", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                }
            }
            if (rating == 1) {
                Box(
                    modifier = Modifier
                        .background(Color.White.copy(alpha = 0.9f), CircleShape)
                        .padding(5.dp)
                ) {
                    Icon(Icons.Filled.ThumbUp, null, tint = Color(0xFF4CAF50), modifier = Modifier.size(12.dp))
                }
            } else if (rating == -1) {
                Box(
                    modifier = Modifier
                        .background(Color.White.copy(alpha = 0.9f), CircleShape)
                        .padding(5.dp)
                ) {
                    Icon(Icons.Filled.ThumbDown, null, tint = Color(0xFFE53935), modifier = Modifier.size(12.dp))
                }
            }
        }

        // ─── Dead item border glow effect ───
        if (isDead) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color.Transparent)
            )
        }
    }
}

@Composable
fun CustomChip(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(
                if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant.copy(
                    alpha = 0.5f
                )
            )
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            fontSize = 14.sp
        )
    }
}

// --- GIAO DIỆN BOTTOM SHEET CHI TIẾT ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ItemDetailSheetContent(
    item: ClothingItem,
    onSave: (ClothingItem) -> Unit,
    onStyleWithAI: () -> Unit,
    onDelete: () -> Unit
) {
    var editName by remember { mutableStateOf(item.clothes_name) }
    var editCategory by remember { mutableStateOf(item.category) }
    var editColor by remember { mutableStateOf(item.mainColor ?: "") }

    var editSeasons by remember { mutableStateOf(item.seasons?.toSet() ?: emptySet()) }
    var editOccasions by remember { mutableStateOf(item.occasions?.toSet() ?: emptySet()) }

    val suggestedCategories = listOf("Top", "Bottom", "Outerwear", "Shoes", "Accessories")
    val suggestedSeasons = listOf("Spring", "Summer", "Autumn", "Winter", "All")
    val suggestedOccasions = listOf("Casual", "Work", "Party", "Sport", "Formal")

    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .verticalScroll(scrollState),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        Box(
            modifier = Modifier
                .size(100.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(MaterialTheme.colorScheme.secondaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Outlined.Checkroom,
                null,
                modifier = Modifier.size(50.dp),
                tint = MaterialTheme.colorScheme.primary
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        OutlinedTextField(
            value = editName,
            onValueChange = { editName = it },
            label = { Text("Item Name") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        )

        Spacer(modifier = Modifier.height(24.dp))

        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                "Category",
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                fontSize = 14.sp
            )
            Spacer(modifier = Modifier.height(8.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(suggestedCategories) { cat ->
                    CustomChip(
                        text = cat,
                        isSelected = editCategory == cat,
                        onClick = { editCategory = cat }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = editColor,
            onValueChange = { editColor = it },
            label = { Text("Main Color") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(24.dp))

        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                "Seasons",
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                fontSize = 14.sp
            )
            Spacer(modifier = Modifier.height(8.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(suggestedSeasons) { season ->
                    val isSelected = editSeasons.contains(season)
                    CustomChip(
                        text = season,
                        isSelected = isSelected,
                        onClick = {
                            editSeasons =
                                if (isSelected) editSeasons - season else editSeasons + season
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                "Occasions",
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                fontSize = 14.sp
            )
            Spacer(modifier = Modifier.height(8.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(suggestedOccasions) { occasion ->
                    val isSelected = editOccasions.contains(occasion)
                    CustomChip(
                        text = occasion,
                        isSelected = isSelected,
                        onClick = {
                            editOccasions =
                                if (isSelected) editOccasions - occasion else editOccasions + occasion
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = onStyleWithAI,
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp),
            shape = RoundedCornerShape(16.dp),
            contentPadding = PaddingValues(),
            colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        brush = Brush.horizontalGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primary,
                                MaterialTheme.colorScheme.secondary
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.AutoAwesome, null, tint = Color.White)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "Style this with AI",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedButton(
            onClick = {
                val updatedItem = item.copy(
                    clothes_name = editName,
                    category = editCategory,
                    mainColor = editColor,
                    seasons = editSeasons.toList(),
                    occasions = editOccasions.toList()
                )
                onSave(updatedItem)
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.primary),
            border = BorderStroke(
                1.dp,
                MaterialTheme.colorScheme.primary
            )
        ) {
            Text("Save Changes", fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }

        Spacer(modifier = Modifier.height(8.dp))

        TextButton(
            onClick = onDelete,
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp),
            colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
        ) {
            Icon(Icons.Filled.Delete, "Delete")
            Spacer(modifier = Modifier.width(8.dp))
            Text("Delete Item", fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }

        Spacer(modifier = Modifier.height(40.dp))
    }
}

@Composable
fun AddManualSheetContent(
    onSave: (ClothingItem, Bitmap?) -> Unit,
    onCancel: () -> Unit
) {
    val context = LocalContext.current
    var selectedImageUri by remember { mutableStateOf<android.net.Uri?>(null) }
    var selectedBitmap by remember { mutableStateOf<Bitmap?>(null) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            selectedImageUri = uri
            try {
                selectedBitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    val source = ImageDecoder.createSource(context.contentResolver, uri)
                    ImageDecoder.decodeBitmap(source)
                } else {
                    MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    var editName by remember { mutableStateOf("") }
    var editCategory by remember { mutableStateOf("Top") }
    var editColor by remember { mutableStateOf("") }
    var editSeasons by remember { mutableStateOf(setOf<String>()) }
    var editOccasions by remember { mutableStateOf(setOf<String>()) }

    val suggestedCategories = listOf("Top", "Bottom", "Outerwear", "Shoes", "Accessories")
    val suggestedSeasons = listOf("Spring", "Summer", "Autumn", "Winter")
    val suggestedOccasions = listOf("Casual", "Work", "Party", "Sport", "Formal")

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .padding(bottom = 24.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text(
            text = "Add New Clothing",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // IMAGE PREVIEW / PICKER
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(250.dp)
                .clickable {
                    photoPickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                },
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            if (selectedImageUri != null) {
                AsyncImage(
                    model = selectedImageUri,
                    contentDescription = "Selected Image",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Filled.Add, contentDescription = "Add image", modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Tap to select image", color = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        OutlinedTextField(
            value = editName,
            onValueChange = { editName = it },
            label = { Text("Item Name") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text("Category", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, fontSize = 14.sp)
        Spacer(modifier = Modifier.height(8.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(suggestedCategories) { cat ->
                CustomChip(text = cat, isSelected = editCategory == cat, onClick = { editCategory = cat })
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = editColor,
            onValueChange = { editColor = it },
            label = { Text("Main Color") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text("Seasons", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, fontSize = 14.sp)
        Spacer(modifier = Modifier.height(8.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(suggestedSeasons) { season ->
                val isSelected = editSeasons.contains(season)
                CustomChip(text = season, isSelected = isSelected, onClick = { editSeasons = if (isSelected) editSeasons - season else editSeasons + season })
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text("Occasions", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, fontSize = 14.sp)
        Spacer(modifier = Modifier.height(8.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(suggestedOccasions) { occasion ->
                val isSelected = editOccasions.contains(occasion)
                CustomChip(text = occasion, isSelected = isSelected, onClick = { editOccasions = if (isSelected) editOccasions - occasion else editOccasions + occasion })
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = {
                val userId = supabase.auth.currentSessionOrNull()?.user?.id ?: ""
                val newItem = ClothingItem(
                    userId = userId,
                    clothes_name = editName.ifEmpty { "New Item" },
                    category = editCategory,
                    mainColor = editColor,
                    seasons = editSeasons.toList(),
                    occasions = editOccasions.toList(),
                    imageUrl = "" // Image url will be updated after upload
                )
                onSave(newItem, selectedBitmap)
            },
            modifier = Modifier.fillMaxWidth().height(54.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text("Save Item", fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }

        Spacer(modifier = Modifier.height(8.dp))

        TextButton(
            onClick = onCancel,
            modifier = Modifier.fillMaxWidth().height(54.dp),
            colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.secondary)
        ) {
            Text("Cancel", fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }
        
        Spacer(modifier = Modifier.height(40.dp))
    }
}

// --- HÀM PREVIEW CHO BOTTOM SHEET ---
//@Preview(showBackground = true, name = "Bottom Sheet Preview")
//@Composable
//fun PreviewItemDetailSheetContent() {
//    val mockItem = ClothingItem(
//        id = "preview1",
//        name = "Zara White Oxford Shirt",
//        category = "Top",
//        mainColor = "White",
//        seasons = listOf("Spring", "Summer"),
//        occasions = listOf("Casual", "Work")
//    )
//
//    MaterialTheme {
//        Surface(
//            modifier = Modifier.fillMaxWidth(),
//            color = MaterialTheme.colorScheme.background,
//            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
//        ) {
//            ItemDetailSheetContent(
//                item = mockItem,
//                onSave = {},
//                onStyleWithAI = {},
//                onDelete = {}
//            )
//        }
//    }
//}