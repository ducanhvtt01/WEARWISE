package com.example.dacs3.dashboard

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.dacs3.dashboard.homeui.OutfitItemPlaceholder
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// --- DATA CLASS CHO TIN NHẮN ---
data class ChatMessage(
    val text: String,
    val isFromUser: Boolean,
    val isError: Boolean = false
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StylistScreen() {
    var promptText by remember { mutableStateOf("") }
    var isAiTyping by remember { mutableStateOf(false) }

    // Biến điều khiển việc hiển thị màn hình chat hay màn hình chính
    var showChat by remember { mutableStateOf(false) }

    // Quản lý focus (để ẩn bàn phím khi bấm nút Back)
    val focusManager = LocalFocusManager.current

    // Danh sách lưu lịch sử tin nhắn
    val chatMessages = remember { mutableStateListOf<ChatMessage>() }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    // Tự động cuộn xuống cuối khi có tin nhắn mới
    LaunchedEffect(chatMessages.size, isAiTyping) {
        if (showChat && (chatMessages.isNotEmpty() || isAiTyping)) {
            listState.animateScrollToItem(maxOf(0, listState.layoutInfo.totalItemsCount - 1))
        }
    }

    // Hàm giả lập gửi tin nhắn (Test UI)
    fun sendFakePrompt(userPrompt: String) {
        if (userPrompt.isBlank() || isAiTyping) return

        // Thêm tin nhắn của User
        chatMessages.add(ChatMessage(text = userPrompt, isFromUser = true))
        promptText = ""
        isAiTyping = true
        showChat = true // Chuyển sang màn hình chat ngay khi gửi

        // Giả lập AI phản hồi sau 1.5 giây
        scope.launch {
            delay(1500)
            val mockAiResponse =
                "That's a great idea! Based on your closet, I suggest pairing your Beige Trench Coat with the Black Turtleneck. It gives a perfect smart-casual look. ✨"
            chatMessages.add(ChatMessage(text = mockAiResponse, isFromUser = false))
            isAiTyping = false
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            // Bao bọc thanh chat với imePadding để nó nảy lên khi mở bàn phím
            Box(modifier = Modifier.imePadding()) {
                ChatBarSection(
                    promptText = promptText,
                    isTyping = isAiTyping,
                    onValueChange = { promptText = it },
                    onSend = { sendFakePrompt(promptText) },
                    onFocus = { showChat = true } // BẬT CHAT KHI BẤM VÀO Ô NHẬP LIỆU
                )
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
        ) {
            // Phần Header giữ cố định ở trên cùng
            Box(modifier = Modifier.padding(horizontal = 24.dp)) {
                StylistHeader(
                    isChatMode = showChat,
                    onBackClick = {
                        showChat = false
                        focusManager.clearFocus() // Ẩn bàn phím và bỏ focus khỏi thanh chat khi quay lại
                    }
                )
            }

            // KIỂM TRA ĐIỀU KIỆN ĐỂ HIỂN THỊ GIAO DIỆN
            if (!showChat) {
                // ==========================================
                // VÙNG 1: HIỂN THỊ GIAO DIỆN CŨ
                // ==========================================
                Column(modifier = Modifier.padding(horizontal = 24.dp)) {
                    QuickPromptsSection(onPromptClick = { selectedPrompt ->
                        sendFakePrompt(selectedPrompt)
                    })
                    OutfitCanvasSection()
                    Spacer(modifier = Modifier.height(16.dp))
                }
            } else {
                // ==========================================
                // VÙNG 2: HIỂN THỊ KHUNG CHAT
                // ==========================================
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    contentPadding = PaddingValues(bottom = 16.dp, top = 8.dp)
                ) {
                    items(chatMessages) { message ->
                        ChatBubble(message)
                        Spacer(modifier = Modifier.height(12.dp))
                    }
                    if (isAiTyping) {
                        item {
                            TypingIndicator()
                            Spacer(modifier = Modifier.height(12.dp))
                        }
                    }
                }
            }
        }
    }
}

// =======================================================
// CÁC THÀNH PHẦN GIAO DIỆN CŨ (ĐƯỢC GIỮ NGUYÊN)
// =======================================================

@Composable
fun StylistHeader(isChatMode: Boolean = false, onBackClick: () -> Unit = {}) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 32.dp, bottom = 20.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            // HIỂN THỊ NÚT BACK NẾU ĐANG Ở MÀN HÌNH CHAT
            if (isChatMode) {
                IconButton(
                    onClick = onBackClick,
                    modifier = Modifier
                        .padding(end = 12.dp)
                        .size(40.dp)
                        .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape)
                ) {
                    Icon(
                        Icons.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "AI Stylist",
                        fontSize = 30.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.primary,
                        letterSpacing = (-0.5).sp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(
                        Icons.Filled.AutoAwesome,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(28.dp)
                    )
                }
                Text(
                    "Your personal fashion director",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surface)
                .clickable { },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Filled.Bookmarks,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
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
                    Icon(
                        Icons.Filled.ElectricBolt,
                        null,
                        modifier = Modifier.size(16.dp)
                    )
                },
                colors = AssistChipDefaults.assistChipColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    labelColor = MaterialTheme.colorScheme.primary,
                    leadingIconContentColor = MaterialTheme.colorScheme.secondary
                ),
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceVariant)
            )
        }
    }
}

@Composable
fun OutfitCanvasSection() {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight(0.85f),
        shape = RoundedCornerShape(32.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
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
                    "The 'Smart Casual' Look",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(6.dp))
                Surface(
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        "98% match with your vibe",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.secondary,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutfitItemPlaceholder(
                    "Beige Trench Coat",
                    Icons.Outlined.Checkroom,
                    "Outerwear",
                    MaterialTheme.colorScheme.tertiaryContainer,
                    Color(0xFFFF9800)
                )
                OutfitItemPlaceholder(
                    "Black Turtleneck",
                    Icons.Filled.Checkroom,
                    "Top",
                    MaterialTheme.colorScheme.surfaceVariant,
                    MaterialTheme.colorScheme.primary
                )
                OutfitItemPlaceholder(
                    "Navy Tailored Trousers",
                    Icons.Outlined.Checkroom,
                    "Bottom",
                    MaterialTheme.colorScheme.secondaryContainer,
                    MaterialTheme.colorScheme.secondary
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = { },
                    modifier = Modifier
                        .size(56.dp)
                        .background(MaterialTheme.colorScheme.background, CircleShape)
                ) {
                    Icon(
                        Icons.Filled.Close,
                        null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Button(
                    onClick = { },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    shape = RoundedCornerShape(28.dp),
                    modifier = Modifier
                        .height(56.dp)
                        .weight(1f)
                        .padding(horizontal = 12.dp)
                ) {
                    Text(
                        "WEAR IT",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                }

                IconButton(
                    onClick = { },
                    modifier = Modifier
                        .size(56.dp)
                        .background(MaterialTheme.colorScheme.secondaryContainer, CircleShape)
                ) {
                    Icon(
                        Icons.Outlined.FavoriteBorder,
                        null,
                        tint = MaterialTheme.colorScheme.secondary
                    )
                }
            }
        }
    }
}

// CẬP NHẬT THANH CHAT CŨ: THÊM TÍNH NĂNG "LẮNG NGHE LÚC BẤM VÀO"
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatBarSection(
    promptText: String,
    isTyping: Boolean,
    onValueChange: (String) -> Unit,
    onSend: () -> Unit,
    onFocus: () -> Unit = {} // Param mới để lắng nghe khi bấm vào
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 20.dp)
            .shadow(elevation = 8.dp, shape = RoundedCornerShape(32.dp))
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(32.dp))
            .padding(horizontal = 12.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        TextField(
            value = promptText,
            onValueChange = onValueChange,
            placeholder = {
                Text(
                    "Ask AI for an outfit...",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 15.sp
                )
            },
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                cursorColor = MaterialTheme.colorScheme.primary,
                focusedTextColor = MaterialTheme.colorScheme.primary
            ),
            modifier = Modifier
                .weight(1f)
                .onFocusChanged { focusState ->
                    // KHI Ổ NHẬP LIỆU ĐƯỢC CHỌN -> BÁO LÊN TRÊN ĐỂ HIỆN CHAT
                    if (focusState.isFocused) {
                        onFocus()
                    }
                },
            singleLine = true
        )

        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(CircleShape)
                .background(if (promptText.isNotBlank() && !isTyping) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary)
                .clickable(enabled = promptText.isNotBlank() && !isTyping) {
                    onSend()
                },
            contentAlignment = Alignment.Center
        ) {
            if (isTyping) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = MaterialTheme.colorScheme.onPrimary,
                    strokeWidth = 2.dp
                )
            } else {
                Icon(
                    Icons.Filled.AutoAwesome,
                    null,
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

// =======================================================
// CÁC THÀNH PHẦN MỚI CHO GIAO DIỆN CHAT BONG BÓNG
// =======================================================

@Composable
fun ChatBubble(message: ChatMessage) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (message.isFromUser) Arrangement.End else Arrangement.Start
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.85f)
                .clip(
                    RoundedCornerShape(
                        topStart = 20.dp,
                        topEnd = 20.dp,
                        bottomStart = if (message.isFromUser) 20.dp else 4.dp,
                        bottomEnd = if (message.isFromUser) 4.dp else 20.dp
                    )
                )
                .background(
                    if (message.isError) MaterialTheme.colorScheme.errorContainer
                    else if (message.isFromUser) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.surfaceVariant
                )
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Text(
                text = message.text,
                color = if (message.isError) MaterialTheme.colorScheme.onErrorContainer
                else if (message.isFromUser) MaterialTheme.colorScheme.onPrimary
                else MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 15.sp,
                lineHeight = 22.sp
            )
        }
    }
}

@Composable
fun TypingIndicator() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Start
    ) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(20.dp, 20.dp, 20.dp, 4.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(14.dp),
                    color = MaterialTheme.colorScheme.primary,
                    strokeWidth = 2.dp
                )
                Text(
                    "Stylist is thinking...",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}