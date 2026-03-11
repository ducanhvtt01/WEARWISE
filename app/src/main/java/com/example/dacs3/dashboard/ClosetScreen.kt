package com.example.dacs3.dashboard

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.outlined.Checkroom
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
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.dacs3.connectDB.ClothingItem
import com.example.dacs3.connectDB.DashboardViewModel
import kotlinx.coroutines.launch
import coil.compose.AsyncImage
import androidx.compose.ui.layout.ContentScale
import kotlinx.serialization.SerialName

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClosetScreen(viewModel: DashboardViewModel) {
    var selectedCategory by remember { mutableStateOf("All") }
    val categories = listOf("All", "Top", "Bottom", "Outerwear", "Shoes", "Accessories")

    var isSearching by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }

    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current

    // --- STATE CHO BOTTOM SHEET ---
    var showItemSheet by remember { mutableStateOf(false) }
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

    val filteredItems = itemsList.filter {
        (selectedCategory == "All" || it.category.equals(selectedCategory, ignoreCase = true)) &&
                (it.clothes_name.contains(searchQuery, ignoreCase = true) || it.mainColor?.contains(
                    searchQuery,
                    ignoreCase = true
                ) == true)
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

            // --- FILTER CATEGORIES ---
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.padding(bottom = 20.dp)
            ) {
                items(categories.size) { index ->
                    val category = categories[index]
                    val isSelected = selectedCategory == category
                    FilterChip(
                        selected = isSelected,
                        onClick = { selectedCategory = category },
                        label = {
                            Text(
                                category,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                            containerColor = MaterialTheme.colorScheme.surface,
                            labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        shape = RoundedCornerShape(16.dp)
                    )
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
                        onClick = { /* TODO: Mở màn hình thêm đồ */ }
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
                            ClosetItemCard(item = item, onClick = {
                                selectedItemToEdit = item
                                showItemSheet = true
                            })
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
fun ClosetItemCard(item: ClothingItem, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp)
            .shadow(
                4.dp,
                RoundedCornerShape(20.dp),
                spotColor = MaterialTheme.colorScheme.surfaceVariant
            )
            .clip(RoundedCornerShape(20.dp))
            .clickable { onClick() },
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                contentAlignment = Alignment.Center
            ) {
                // KIỂM TRA: Nếu có link ảnh thì dùng AsyncImage của Coil để hiển thị
                if (item.imageUrl.isNotEmpty()) {
                    AsyncImage(
                        model = item.imageUrl,
                        contentDescription = item.clothes_name,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop // Cắt ảnh cho vừa vặn với khung bo góc
                    )
                } else {
                    // Nếu chưa có ảnh (hoặc lỗi mạng) thì vẫn hiện cái móc áo dự phòng
                    Icon(
                        Icons.Outlined.Checkroom,
                        null,
                        modifier = Modifier.size(40.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                item.clothes_name,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2
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