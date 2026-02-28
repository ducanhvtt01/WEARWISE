package com.example.dacs3.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.Checkroom
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// Dữ liệu giả lập cho quần áo
data class WardrobeItem(val id: Int, val name: String, val brand: String, val colorTag: Color)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClosetScreen() {
    var selectedCategory by remember { mutableStateOf("All") }
    val categories = listOf("All", "Tops", "Bottoms", "Outerwear", "Shoes", "Accs")

    // Danh sách đồ giả lập
    val items = listOf(
        WardrobeItem(1, "White Oxford Shirt", "Zara", Color(0xFFF5F5F5)),
        WardrobeItem(2, "Navy Tailored Trousers", "Mango", MidnightBlue),
        WardrobeItem(3, "Beige Trench Coat", "Burberry", Color(0xFFD7CCC8)),
        WardrobeItem(4, "Classic Denim Jacket", "Levi's", Color(0xFF64B5F6)),
        WardrobeItem(5, "Brown Leather Loafers", "Clarks", Color(0xFF8D6E63)),
        WardrobeItem(6, "Black Turtleneck", "Uniqlo", Color(0xFF424242))
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(OffWhite)
            .padding(horizontal = 24.dp)
    ) {
        // --- HEADER ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 40.dp, bottom = 20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "My Closet",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = MidnightBlue
            )

            // Nút Tìm kiếm
            IconButton(
                onClick = { /* Mở tìm kiếm */ },
                modifier = Modifier.background(Color.White, CircleShape).shadow(2.dp, CircleShape)
            ) {
                Icon(Icons.Filled.Search, contentDescription = "Search", tint = MidnightBlue)
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
                    border = null,

                    // 1. THÊM DÒNG NÀY: Dùng hiệu ứng đổ bóng (elevation) chuẩn của Material 3
                    elevation = FilterChipDefaults.filterChipElevation(
                        elevation = if (isSelected) 4.dp else 1.dp
                    ),

                    // 2. SỬA DÒNG NÀY: Chỉ để lại Modifier trống, XÓA đoạn .shadow(...) cũ đi
                    modifier = Modifier
                )
            }
        }

        // --- GRID ITEMS ---
        LazyVerticalGrid(
            columns = GridCells.Fixed(2), // Lưới 2 cột
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(bottom = 120.dp) // Tránh thanh BottomBar
        ) {
            // Nút Thêm mới nằm ở ô đầu tiên
            item {
                Card(
                    modifier = Modifier.height(200.dp),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = SoftTeal),
                    onClick = { /* TODO: Mở form thêm quần áo */ }
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
                            Icon(Icons.Filled.Add, contentDescription = "Add", tint = Color.White)
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("Add New", color = MidnightBlue, fontWeight = FontWeight.SemiBold)
                    }
                }
            }

            // Các thẻ quần áo
            items(items) { item ->
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
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            // Vùng hiển thị ảnh (Dùng icon giả lập)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(LightGray.copy(alpha = 0.5f)),
                contentAlignment = Alignment.Center
            ) {
                // TODO: Thay bằng AsyncImage để load ảnh thật
                Icon(Icons.Outlined.Checkroom, contentDescription = null, modifier = Modifier.size(40.dp), tint = SilverMist)

                // Chấm màu của quần áo
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

            // Thông tin
            Text(text = item.brand, fontSize = 11.sp, color = SilverMist, fontWeight = FontWeight.Medium)
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