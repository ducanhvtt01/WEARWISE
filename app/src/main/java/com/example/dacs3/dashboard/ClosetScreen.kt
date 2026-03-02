package com.example.dacs3.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.Checkroom
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// Dữ liệu giả lập cho quần áo
data class WardrobeItem(val id: Int, val name: String, val brand: String, val colorTag: Color)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClosetScreen() {
    var selectedCategory by remember { mutableStateOf("All") }
    val categories = listOf("All", "Tops", "Bottoms", "Outerwear", "Shoes", "Accs")

    // --- STATE CHO TÌM KIẾM ---
    var isSearching by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }

    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current

    // Tự động bật bàn phím khi nhấn Search
    LaunchedEffect(isSearching) {
        if (isSearching) {
            focusRequester.requestFocus()
            keyboardController?.show()
        } else {
            keyboardController?.hide()
        }
    }

    val items = listOf(
        WardrobeItem(1, "White Oxford Shirt", "Zara", Color(0xFFF5F5F5)),
        WardrobeItem(2, "Navy Tailored Trousers", "Mango", MidnightBlue),
        WardrobeItem(3, "Beige Trench Coat", "Burberry", Color(0xFFD7CCC8)),
        WardrobeItem(4, "Classic Denim Jacket", "Levi's", Color(0xFF64B5F6)),
        WardrobeItem(5, "Brown Leather Loafers", "Clarks", Color(0xFF8D6E63)),
        WardrobeItem(6, "Black Turtleneck", "Uniqlo", Color(0xFF424242))
    )

    val filteredItems = items.filter {
        it.name.contains(searchQuery, ignoreCase = true) ||
                it.brand.contains(searchQuery, ignoreCase = true)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(OffWhite)
            .padding(horizontal = 24.dp)
    ) {
        // --- HEADER CÓ TÍCH HỢP TÌM KIẾM ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(90.dp)
                .padding(top = 40.dp, bottom = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (!isSearching) {
                Text(
                    text = "My Closet",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = MidnightBlue
                )

                IconButton(
                    onClick = { isSearching = true },
                    modifier = Modifier.background(Color.White, CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Search,
                        contentDescription = "Search",
                        tint = MidnightBlue,
                        modifier = Modifier.size(30.dp)
                    )
                }
            } else {
                // --- SỬ DỤNG BASIC TEXT FIELD ĐỂ FIX LỖI CHỮ BỊ CHE ---
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .background(Color.White, RoundedCornerShape(12.dp))
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
                            color = MidnightBlue,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium
                        ),
                        cursorBrush = SolidColor(MidnightBlue),
                        decorationBox = { innerTextField ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Search,
                                    contentDescription = null,
                                    tint = AccentTeal,
                                    modifier = Modifier.size(20.dp)
                                )

                                Spacer(modifier = Modifier.width(8.dp))

                                Box(modifier = Modifier.weight(1f)) {
                                    if (searchQuery.isEmpty()) {
                                        Text(
                                            text = "Search clothes...",
                                            color = SilverMist,
                                            fontSize = 14.sp
                                        )
                                    }
                                    innerTextField() // Nơi hiển thị văn bản nhập vào
                                }

                                IconButton(
                                    onClick = {
                                        if (searchQuery.isEmpty()) isSearching = false
                                        else searchQuery = ""
                                    },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Close,
                                        contentDescription = null,
                                        tint = SilverMist,
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
                            text = category,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MidnightBlue,
                        selectedLabelColor = Color.White,
                        containerColor = Color.White,
                        labelColor = SilverMist
                    ),
                    shape = RoundedCornerShape(16.dp),
                    elevation = FilterChipDefaults.filterChipElevation(
                        elevation = if (isSelected) 4.dp else 1.dp
                    )
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
                    colors = CardDefaults.cardColors(containerColor = SoftTeal),
                    onClick = { /* TODO: Add function */ }
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .background(AccentTeal, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Add,
                                contentDescription = null,
                                tint = Color.White
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Add New",
                            color = MidnightBlue,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            items(filteredItems) { item ->
                ClosetItemCard(item)
            }
        }
    }
}

@Composable
fun ClosetItemCard(item: WardrobeItem) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp)
            .shadow(4.dp, RoundedCornerShape(20.dp), spotColor = LightGray),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(LightGray.copy(alpha = 0.5f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.Checkroom,
                    contentDescription = null,
                    modifier = Modifier.size(40.dp),
                    tint = SilverMist
                )

                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                        .size(16.dp)
                        .background(item.colorTag, CircleShape)
                        .shadow(1.dp, CircleShape)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = item.brand,
                fontSize = 11.sp,
                color = SilverMist,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = item.name,
                fontSize = 14.sp,
                color = MidnightBlue,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1
            )
        }
    }
}