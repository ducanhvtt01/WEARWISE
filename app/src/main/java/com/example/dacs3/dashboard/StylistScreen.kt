package com.example.dacs3.dashboard

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.dacs3.dashboard.homeui.OutfitItemPlaceholder
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.LocalTime

// --- MESSAGE DATA CLASS ---
data class ChatMessage(
    val text: String,
    val isFromUser: Boolean,
    val isError: Boolean = false
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StylistScreen(
    weatherViewModel: WeatherViewModel = viewModel() // Integrate WeatherViewModel
) {
    var promptText by remember { mutableStateOf("") }
    var isAiTyping by remember { mutableStateOf(false) }
    var showChat by remember { mutableStateOf(false) }

    val focusManager = LocalFocusManager.current
    val chatMessages = remember { mutableStateListOf<ChatMessage>() }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    // Get real-time temperature from WeatherApi
    val currentTemp by weatherViewModel.temperature.collectAsState()

    // Fetch weather when screen opens
    LaunchedEffect(Unit) {
        weatherViewModel.fetchWeather()
    }

    LaunchedEffect(chatMessages.size, isAiTyping) {
        if (showChat && (chatMessages.isNotEmpty() || isAiTyping)) {
            listState.animateScrollToItem(maxOf(0, listState.layoutInfo.totalItemsCount - 1))
        }
    }

    fun sendFakePrompt(userPrompt: String) {
        if (userPrompt.isBlank() || isAiTyping) return

        chatMessages.add(ChatMessage(text = userPrompt, isFromUser = true))
        promptText = ""
        isAiTyping = true
        showChat = true

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
            Box(modifier = Modifier.imePadding()) {
                ChatBarSection(
                    promptText = promptText,
                    isTyping = isAiTyping,
                    onValueChange = { promptText = it },
                    onSend = { sendFakePrompt(promptText) },
                    onFocus = { showChat = true }
                )
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
        ) {
            Box(modifier = Modifier.padding(horizontal = 24.dp)) {
                StylistHeader(
                    isChatMode = showChat,
                    onBackClick = {
                        showChat = false
                        focusManager.clearFocus()
                    }
                )
            }

            if (!showChat) {
                // ==========================================
                // MAIN UI WITH CONTEXTUAL BRIEFING CARD
                // ==========================================
                Column(
                    modifier = Modifier
                        .padding(horizontal = 24.dp)
                        .weight(1f) // Cấp không gian linh hoạt
                        .verticalScroll(rememberScrollState()) // Đã thêm khả năng cuộn
                ) {

                    // The dynamic card that changes based on time
                    ContextBriefingCard(
                        currentTemperature = currentTemp,
                        onActionClick = { aiPrompt ->
                            sendFakePrompt(aiPrompt)
                        }
                    )

                    QuickPromptsSection(onPromptClick = { selectedPrompt ->
                        sendFakePrompt(selectedPrompt)
                    })
                    OutfitCanvasSection()
                    Spacer(modifier = Modifier.height(24.dp)) // Tăng khoảng trống dưới cùng một chút
                }
            } else {
                // ==========================================
                // CHAT INTERFACE
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
// CONTEXT-AWARE BRIEFING COMPONENT (NEW)
// =======================================================

data class BriefingContent(
    val icon: ImageVector,
    val iconColor: Color,
    val greeting: String,
    val message: String,
    val buttonText: String,
    val aiPrompt: String
)

@Composable
fun ContextBriefingCard(
    currentTemperature: String,
    onActionClick: (String) -> Unit
) {
    val currentHour = LocalTime.now().hour

    val content = when (currentHour) {
        in 5..11 -> BriefingContent(
            icon = Icons.Filled.WbSunny,
            iconColor = Color(0xFFFFA000),
            greeting = "Good morning, Duc Anh!",
            message = "It's a fresh morning ($currentTemperature). You have an OS Exam at 9 AM. Your water-resistant Smart Casual look is ready.",
            buttonText = "View Morning Outfit",
            aiPrompt = "Show me the details of the Smart Casual outfit for my morning exam."
        )

        in 12..17 -> BriefingContent(
            icon = Icons.Filled.Cloud,
            iconColor = Color(0xFF42A5F5),
            greeting = "Good afternoon!",
            message = "The temperature is around $currentTemperature. If you are heading out later, grab a light windbreaker just in case.",
            buttonText = "Find a Jacket",
            aiPrompt = "Suggest a light windbreaker or jacket from my closet."
        )

        else -> BriefingContent(
            icon = Icons.Filled.Nightlight,
            iconColor = Color(0xFF5E35B1),
            greeting = "Good evening!",
            message = "Time to wind down. Let's prep your outfit for tomorrow's Computer Networks lab class so you can sleep in an extra 15 minutes!",
            buttonText = "Plan Tomorrow's Look",
            aiPrompt = "Help me pick a comfortable outfit for my Computer Networks lab class tomorrow morning."
        )
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 24.dp)
            .shadow(4.dp, RoundedCornerShape(20.dp)),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = content.icon,
                    contentDescription = null,
                    tint = content.iconColor,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = content.greeting,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = content.message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                lineHeight = 22.sp
            )

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = { onActionClick(content.aiPrompt) },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    Icons.Outlined.Checkroom,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = content.buttonText, fontWeight = FontWeight.Bold)
            }
        }
    }
}

// =======================================================
// EXISTING UI COMPONENTS
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
            .wrapContentHeight(), // Đã thay đổi: Thẻ tự động co giãn theo nội dung, không bị lỗi tràn viền
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

            Spacer(modifier = Modifier.height(24.dp)) // Thêm Spacer để giao diện thoáng hơn sau khi bỏ fillMaxHeight

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

            Spacer(modifier = Modifier.height(32.dp)) // Thêm Spacer trước phần nút bấm

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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatBarSection(
    promptText: String,
    isTyping: Boolean,
    onValueChange: (String) -> Unit,
    onSend: () -> Unit,
    onFocus: () -> Unit = {}
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