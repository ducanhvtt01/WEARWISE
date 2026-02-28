package com.example.dacs3.dashboard

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Checkroom
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StylistScreen() {
    var promptText by remember { mutableStateOf("") }
    var isGenerating by remember { mutableStateOf(false) } // Giả lập trạng thái AI đang suy nghĩ

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
                .padding(top = 40.dp, bottom = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "AI Stylist",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        color = MidnightBlue
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(Icons.Filled.AutoAwesome, contentDescription = "AI", tint = AccentTeal, modifier = Modifier.size(24.dp))
                }
                Text(
                    text = "Your personal fashion director",
                    fontSize = 14.sp,
                    color = SilverMist,
                    fontWeight = FontWeight.Light
                )
            }

            // Nút Lịch sử / Lookbook đã lưu
            IconButton(
                onClick = { /* Mở Lookbook */ },
                modifier = Modifier.background(Color.White, CircleShape).shadow(2.dp, CircleShape)
            ) {
                Icon(Icons.Filled.Bookmarks, contentDescription = "Saved Looks", tint = MidnightBlue)
            }
        }

        // --- QUICK PROMPTS ---
        val quickPrompts = listOf("Date Night", "Casual Friday", "Rainy Day", "Gym Setup")
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.padding(bottom = 24.dp)
        ) {
            items(quickPrompts.size) { index ->
                AssistChip(
                    onClick = { promptText = "Give me an outfit for ${quickPrompts[index]}" },
                    label = { Text(quickPrompts[index], fontWeight = FontWeight.Medium) },
                    leadingIcon = { Icon(Icons.Filled.ElectricBolt, contentDescription = null, modifier = Modifier.size(16.dp)) },
                    colors = AssistChipDefaults.assistChipColors(
                        containerColor = Color.White,
                        labelColor = MidnightBlue,
                        leadingIconContentColor = AccentTeal
                    ),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(
                        width = 1.dp,
                        color = Color.LightGray
                    )                )
            }
        }

        // --- OUTFIT CANVAS (Khung hiển thị kết quả AI) ---
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f) // Chiếm phần lớn không gian còn lại
                .shadow(16.dp, RoundedCornerShape(32.dp), spotColor = LightGray),
            shape = RoundedCornerShape(32.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Tiêu đề của Look này
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("The 'Smart Casual' Look", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MidnightBlue)
                    Text("98% match with your vibe", fontSize = 12.sp, color = AccentTeal, fontWeight = FontWeight.Medium)
                }

                // Hình ảnh/Icon minh họa cho bộ đồ (Dùng lại hàm OutfitItemPlaceholder từ HomeUI)
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutfitItemPlaceholder("Beige Trench Coat", Icons.Outlined.Checkroom, "Outerwear", SoftOrange, Color(0xFFFF9800))
                    OutfitItemPlaceholder("Black Turtleneck", Icons.Filled.Checkroom, "Top", Color(0xFFEEEEEE), MidnightBlue)
                    OutfitItemPlaceholder("Navy Tailored Trousers", Icons.Outlined.Checkroom, "Bottom", SoftTeal, AccentTeal)
                }

                // Nhóm nút tương tác (Reject - Save - Wear)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Nút Discard / Regenerate
                    IconButton(
                        onClick = { /* Tạo lại bộ khác */ },
                        modifier = Modifier.size(56.dp).background(LightGray, CircleShape)
                    ) {
                        Icon(Icons.Filled.Close, contentDescription = "Discard", tint = SilverMist, modifier = Modifier.size(28.dp))
                    }

                    // Nút Wear It (Nút chính giữa to nhất)
                    Button(
                        onClick = { /* Chọn mặc bộ này */ },
                        colors = ButtonDefaults.buttonColors(containerColor = MidnightBlue),
                        shape = CircleShape,
                        modifier = Modifier.height(56.dp).padding(horizontal = 8.dp)
                    ) {
                        Text("WEAR IT", fontWeight = FontWeight.Bold, letterSpacing = 1.sp, color = OffWhite)
                    }

                    // Nút Save / Favorite
                    IconButton(
                        onClick = { /* Lưu vào Lookbook */ },
                        modifier = Modifier.size(56.dp).background(SoftTeal, CircleShape)
                    ) {
                        Icon(Icons.Outlined.FavoriteBorder, contentDescription = "Save", tint = AccentTeal, modifier = Modifier.size(28.dp))
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // --- INPUT PROMPT CHATBAR ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 100.dp) // Tránh thanh Bottom Bar
                .background(Color.White, RoundedCornerShape(24.dp))
                .padding(horizontal = 8.dp, vertical = 4.dp)
                .shadow(2.dp, RoundedCornerShape(24.dp), spotColor = LightGray),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextField(
                value = promptText,
                onValueChange = { promptText = it },
                placeholder = { Text("Ask AI for an outfit...", color = SilverMist, fontSize = 14.sp) },
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    cursorColor = MidnightBlue
                ),
                modifier = Modifier.weight(1f),
                singleLine = true
            )

            // Nút Send (Gửi yêu cầu cho AI)
            IconButton(
                onClick = { /* TODO: Gọi API AI xử lý prompt */ },
                modifier = Modifier
                    .size(40.dp)
                    .background(AccentTeal, CircleShape)
            ) {
                Icon(Icons.Filled.AutoAwesome, contentDescription = "Generate", tint = Color.White, modifier = Modifier.size(20.dp))
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewStylistScreen() {
    StylistScreen()
}