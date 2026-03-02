package com.example.dacs3.dashboard

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StylistScreen() {
    var promptText by remember { mutableStateOf("") }

    // SỬ DỤNG SCAFFOLD ĐỂ TỰ ĐỘNG CĂN CHỈNH KHOẢNG CÁCH CHUẨN
    Scaffold(
        containerColor = OffWhite,
        bottomBar = {
            // Thanh nhập liệu luôn nằm cố định ở đáy màn hình
            ChatBarSection(
                promptText = promptText,
                onValueChange = { promptText = it }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding) // innerPadding giúp nội dung không bị thanh BottomBar đè lên
                .fillMaxSize()
                .padding(horizontal = 24.dp)
        ) {
            // --- HEADER ---
            StylistHeader()

            // --- QUICK PROMPTS ---
            QuickPromptsSection(onPromptClick = { promptText = it })

            // --- OUTFIT CANVAS ---
            OutfitCanvasSection()

            // Khoảng trống nhỏ cuối cùng để cách ChatBar một chút
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

// --- CÁC COMPONENT TÁCH RIÊNG ĐỂ CODE SẠCH HƠN ---

@Composable
fun StylistHeader() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 32.dp, bottom = 20.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "AI Stylist",
                    fontSize = 30.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = MidnightBlue,
                    letterSpacing = (-0.5).sp
                )
                Spacer(modifier = Modifier.width(8.dp))
                Icon(
                    imageVector = Icons.Filled.AutoAwesome,
                    contentDescription = null,
                    tint = AccentTeal,
                    modifier = Modifier.size(28.dp)
                )
            }
            Text(
                text = "Your personal fashion director",
                fontSize = 14.sp,
                color = SilverMist,
                fontWeight = FontWeight.Medium
            )
        }

        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(Color.White)
                .clickable { },
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Filled.Bookmarks, contentDescription = null, tint = MidnightBlue)
        }
    }
}

@Composable
fun QuickPromptsSection(onPromptClick: (String) -> Unit) {
    val quickPrompts = listOf("Date Night", "Casual Friday", "Rainy Day", "Gym Setup")
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.padding(bottom = 24.dp)
    ) {
        items(quickPrompts.size) { index ->
            AssistChip(
                onClick = { onPromptClick("Give me an outfit for ${quickPrompts[index]}") },
                label = { Text(quickPrompts[index], fontWeight = FontWeight.SemiBold) },
                leadingIcon = {
                    Icon(Icons.Filled.ElectricBolt, null, modifier = Modifier.size(16.dp))
                },
                colors = AssistChipDefaults.assistChipColors(
                    containerColor = Color.White,
                    labelColor = MidnightBlue,
                    leadingIconContentColor = AccentTeal
                ),
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, Color(0xFFF0F0F0))
            )
        }
    }
}

@Composable
fun OutfitCanvasSection() {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight(0.85f), // Giới hạn chiều cao tương đối để tránh tràn màn hình nhỏ
        shape = RoundedCornerShape(32.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(vertical = 24.dp, horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "The 'Smart Casual' Look",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = MidnightBlue
                )
                Spacer(modifier = Modifier.height(6.dp))
                Surface(color = SoftTeal, shape = RoundedCornerShape(12.dp)) {
                    Text(
                        text = "98% match with your vibe",
                        fontSize = 12.sp,
                        color = AccentTeal,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutfitItemPlaceholder("Beige Trench Coat", Icons.Outlined.Checkroom, "Outerwear", SoftOrange, Color(0xFFFF9800))
                OutfitItemPlaceholder("Black Turtleneck", Icons.Filled.Checkroom, "Top", LightGray, MidnightBlue)
                OutfitItemPlaceholder("Navy Tailored Trousers", Icons.Outlined.Checkroom, "Bottom", SoftTeal, AccentTeal)
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = { },
                    modifier = Modifier.size(56.dp).background(OffWhite, CircleShape)
                ) {
                    Icon(Icons.Filled.Close, null, tint = SilverMist)
                }

                Button(
                    onClick = { },
                    colors = ButtonDefaults.buttonColors(containerColor = MidnightBlue),
                    shape = RoundedCornerShape(28.dp),
                    modifier = Modifier.height(56.dp).weight(1f).padding(horizontal = 12.dp)
                ) {
                    Text("WEAR IT", fontWeight = FontWeight.Bold, color = Color.White)
                }

                IconButton(
                    onClick = { },
                    modifier = Modifier.size(56.dp).background(SoftTeal, CircleShape)
                ) {
                    Icon(Icons.Outlined.FavoriteBorder, null, tint = AccentTeal)
                }
            }
        }
    }
}

@Composable
fun ChatBarSection(promptText: String, onValueChange: (String) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 20.dp) // Khoảng cách cố định với đáy
            .shadow(elevation = 8.dp, shape = RoundedCornerShape(32.dp))
            .background(Color.White, RoundedCornerShape(32.dp))
            .padding(horizontal = 12.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        TextField(
            value = promptText,
            onValueChange = onValueChange,
            placeholder = { Text("Ask AI for an outfit...", color = SilverMist, fontSize = 15.sp) },
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

        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(CircleShape)
                .background(AccentTeal)
                .clickable { },
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Filled.AutoAwesome, null, tint = Color.White, modifier = Modifier.size(20.dp))
        }
    }
}

@Preview(showBackground = true, device = "spec:width=411dp,height=891dp")
@Composable
fun PreviewStylistScreen() {
    StylistScreen()
}