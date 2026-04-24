package com.example.dacs3.dashboard

import android.app.Activity
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.Checkroom
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.History
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.dacs3.connectDB.DashboardViewModel
import com.example.dacs3.dashboard.homeui.OutfitItemPlaceholder
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.Content
import com.google.ai.client.generativeai.type.content
import dev.jeziellago.compose.markdowntext.MarkdownText
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
    weatherViewModel: WeatherViewModel = viewModel(),
    dashboardViewModel: DashboardViewModel = viewModel()
) {
    val view = LocalView.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // Thêm DrawerState để điều khiển thanh trượt lịch sử
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)

    var promptText by remember { mutableStateOf("") }
    var isAiTyping by remember { mutableStateOf(false) }
    var showChat by remember { mutableStateOf(false) }

    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    val chatMessages = remember { mutableStateListOf<ChatMessage>() }
    val listState = rememberLazyListState()

    val currentTemp by weatherViewModel.temperature.collectAsState()
    val myClosetItems by dashboardViewModel.clothingItems.collectAsState()
    val chatSessions by dashboardViewModel.chatSessions.collectAsState()

    // 1. THÊM BIẾN TRIGGER ĐỂ ÉP AI XÓA TRÍ NHỚ
    var chatSessionTrigger by remember { mutableIntStateOf(0) }

    // --- VẤN ĐỀ 1: BIẾN LƯU TRỮ LỊCH SỬ CHO BỘ NÃO AI ---
    var chatHistoryContext by remember { mutableStateOf<List<Content>>(emptyList()) }

    DisposableEffect(Unit) {
        val window = (context as? Activity)?.window
        val originalMode = window?.attributes?.softInputMode

        window?.let {
            WindowCompat.setDecorFitsSystemWindows(it, false)
            it.setSoftInputMode(android.view.WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
        }

        onDispose {
            originalMode?.let {
                window?.setSoftInputMode(it)
            }
        }
    }

    LaunchedEffect(Unit) {
        weatherViewModel.fetchWeather()
        dashboardViewModel.userProfile?.id?.let {
            dashboardViewModel.fetchChatSessions(it)
        }
    }

    // Tự động cuộn xuống cuối khi có tin nhắn mới
    LaunchedEffect(chatMessages.size, isAiTyping) {
        if (showChat && (chatMessages.isNotEmpty() || isAiTyping)) {
            listState.animateScrollToItem(maxOf(0, listState.layoutInfo.totalItemsCount - 1))
        }
    }

    val generativeModel = remember(myClosetItems, currentTemp) {
        val closetData = if (myClosetItems.isNotEmpty()) {
            myClosetItems.joinToString("\n") { "- ${it.clothes_name} (${it.category}, Color: ${it.mainColor})" }
        } else {
            "User's closet is currently empty."
        }

        GenerativeModel(
            modelName = "gemini-2.5-flash",
            apiKey = com.example.dacs3.BuildConfig.GEMINI_API_KEY,
            systemInstruction = content {
                text(
                    """
                    You are an expert personal fashion stylist named WearWise AI.
                    Your goal is to help the user pick outfits, answer fashion questions, and give style advice.
                    
                    CURRENT CONTEXT:
                    - Local Weather: $currentTemp
                    - User's actual closet inventory: 
                    $closetData
                    
                    STRICT RULES:
                    1. When recommending specific clothing items to wear, YOU MUST ONLY suggest items that exist in the user's closet inventory above.
                    2. If they ask for general advice, you can answer freely.
                    3. Reply concisely, friendly, and logically. Format with bullet points if listing items.
                    4. IMPORTANT: Always reply in the exact same language the user uses.
                """.trimIndent()
                )
            }
        )
    }

    // 2. CẬP NHẬT REMEMBER CỦA CHAT SESSION THEO TRIGGER VÀ HISTORY
    val chatSession = remember(generativeModel, chatSessionTrigger, chatHistoryContext) {
        generativeModel.startChat(history = chatHistoryContext)
    }

    fun sendMessageToAI(userPrompt: String) {
        if (userPrompt.isBlank() || isAiTyping) return

        chatMessages.add(ChatMessage(text = userPrompt, isFromUser = true))
        promptText = ""
        isAiTyping = true
        showChat = true
        keyboardController?.hide()
        focusManager.clearFocus()

        scope.launch {
            try {
                val response = chatSession.sendMessage(userPrompt)
                val aiText = response.text ?: "Sorry, I couldn't process this request."

                chatMessages.add(ChatMessage(text = aiText, isFromUser = false))

                dashboardViewModel.saveChatToDatabase(
                    userMessage = userPrompt,
                    aiMessage = aiText
                )

                dashboardViewModel.userProfile?.id?.let {
                    dashboardViewModel.fetchChatSessions(it)
                }

            } catch (e: Exception) {
                e.printStackTrace()
                val errorText = e.localizedMessage ?: ""
                val friendlyMessage =
                    if (errorText.contains("503") || errorText.contains("high demand") || errorText.contains(
                            "MissingFieldException"
                        )
                    ) {
                        "The AI system is experiencing high demand. Please try again in a few seconds! ⏳"
                    } else {
                        "AI Connection Error: Please check your network or API Key."
                    }

                chatMessages.add(
                    ChatMessage(
                        text = friendlyMessage,
                        isFromUser = false,
                        isError = true
                    )
                )
            } finally {
                isAiTyping = false
            }
        }
    }

    // 3. HÀM TẠO CUỘC TRÒ CHUYỆN MỚI
    fun startNewChat() {
        dashboardViewModel.resetChatSession()
        chatMessages.clear()
        chatHistoryContext = emptyList()

        chatMessages.add(
            ChatMessage(
                text = "Hi! I'm WearWise AI. How can I help with your style today? ✨",
                isFromUser = false
            )
        )

        chatSessionTrigger++
        showChat = true
        scope.launch { drawerState.close() }
    }

    var showPackingSheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var destination by remember { mutableStateOf("") }
    var tripDays by remember { mutableStateOf("") }
    var isAILoading by remember { mutableStateOf(false) }
    var packingItems by remember { mutableStateOf(listOf<String>()) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        ModalNavigationDrawer(
            drawerState = drawerState,
            drawerContent = {
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                    ModalDrawerSheet(
                        modifier = Modifier
                            .fillMaxHeight()
                            .width(300.dp),
                        drawerContainerColor = MaterialTheme.colorScheme.surface
                    ) {
                        Text(
                            text = "Chat History",
                            modifier = Modifier.padding(24.dp),
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )

                        Button(
                            onClick = { startNewChat() },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 24.dp, vertical = 8.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Icon(Icons.Filled.Add, contentDescription = "New Chat")
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("New Chat", fontWeight = FontWeight.Bold)
                        }

                        HorizontalDivider(modifier = Modifier.padding(top = 8.dp))

                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(top = 8.dp)
                        ) {
                            items(chatSessions) { session ->
                                NavigationDrawerItem(
                                    label = {
                                        Text(
                                            text = session.title,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    },
                                    selected = dashboardViewModel.currentChatSessionId == session.id,
                                    icon = {
                                        Icon(
                                            Icons.Outlined.ChatBubbleOutline,
                                            contentDescription = null
                                        )
                                    },
                                    badge = {
                                        IconButton(onClick = {
                                            session.id?.let { id ->
                                                dashboardViewModel.userProfile?.id?.let { userId ->
                                                    dashboardViewModel.deleteChatSession(id, userId)
                                                    if (dashboardViewModel.currentChatSessionId == id) {
                                                        startNewChat()
                                                    }
                                                }
                                            }
                                        }) {
                                            Icon(
                                                Icons.Outlined.Delete,
                                                contentDescription = "Delete Chat",
                                                tint = MaterialTheme.colorScheme.error
                                            )
                                        }
                                    },
                                    onClick = {
                                        session.id?.let { id ->
                                            dashboardViewModel.loadChatMessages(id) { loadedMsgs ->
                                                chatMessages.clear()
                                                chatMessages.addAll(loadedMsgs)

                                                chatHistoryContext = loadedMsgs.map {
                                                    content(role = if (it.isFromUser) "user" else "model") {
                                                        text(it.text)
                                                    }
                                                }

                                                showChat = true
                                            }
                                        }
                                        scope.launch { drawerState.close() }
                                    },
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                }
            }
        ) {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                Scaffold(
                    containerColor = MaterialTheme.colorScheme.background,
                    contentWindowInsets = WindowInsets(0, 0, 0, 0)
                ) { innerPadding ->
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                            // THẦN CHÚ SAFE DRAWING: Tự động tránh Status Bar và Bàn phím
                            .windowInsetsPadding(WindowInsets.safeDrawing)
                    ) {
                        // --- PHẦN HEADER ---
                        Box(modifier = Modifier.padding(horizontal = 24.dp)) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 32.dp, bottom = 20.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    if (showChat) {
                                        IconButton(
                                            onClick = {
                                                showChat = false
                                                focusManager.clearFocus()
                                                keyboardController?.hide()
                                            },
                                            modifier = Modifier
                                                .padding(end = 12.dp)
                                                .size(40.dp)
                                                .background(
                                                    MaterialTheme.colorScheme.surfaceVariant,
                                                    CircleShape
                                                )
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

                                if (showChat) {
                                    Box(
                                        modifier = Modifier
                                            .size(48.dp)
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.surface)
                                            .clickable {
                                                scope.launch { drawerState.open() }
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            Icons.Outlined.History,
                                            contentDescription = "History",
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            }
                        }

                        // --- PHẦN NỘI DUNG CHÍNH ---
                        Box(modifier = Modifier.weight(1f)) {
                            if (!showChat) {
                                Column(
                                    modifier = Modifier
                                        .padding(horizontal = 24.dp)
                                        .fillMaxSize()
                                        .verticalScroll(rememberScrollState())
                                ) {
                                    ContextBriefingCard(
                                        currentTemperature = currentTemp,
                                        onActionClick = { aiPrompt ->
                                            sendMessageToAI(aiPrompt)
                                        }
                                    )

                                    QuickPromptsSection(onPromptClick = { selectedPrompt ->
                                        sendMessageToAI(selectedPrompt)
                                    })

                                    OutfitCanvasSection()

                                    Spacer(modifier = Modifier.height(16.dp))

                                    SmartPackingCard(
                                        onPlanTripClick = {
                                            showPackingSheet = true
                                        }
                                    )

                                    Spacer(modifier = Modifier.height(24.dp))
                                }
                            } else {
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

                        // --- PHẦN NHẬP CHAT ---
                        ChatBarSection(
                            promptText = promptText,
                            isTyping = isAiTyping,
                            onValueChange = { promptText = it },
                            onSend = { sendMessageToAI(promptText) },
                            onFocus = { showChat = true }
                        )
                    }
                }
            }
        }
    }

    if (showPackingSheet) {
        ModalBottomSheet(
            onDismissRequest = {
                showPackingSheet = false
                isAILoading = false
                packingItems = emptyList()
                errorMessage = null
            },
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            val sheetFocusManager = LocalFocusManager.current
            val sheetKeyboardController = LocalSoftwareKeyboardController.current

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.safeDrawing)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "AI Packing Assistant ✈️",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                OutlinedTextField(
                    value = destination,
                    onValueChange = { destination = it },
                    label = { Text("Where are you going?") },
                    placeholder = { Text("e.g., Da Lat, Hanoi, Tokyo...") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = tripDays,
                    onValueChange = { tripDays = it },
                    label = { Text("Duration (days)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = {
                        sheetKeyboardController?.hide()
                        sheetFocusManager.clearFocus()

                        if (destination.isBlank() || tripDays.isBlank()) {
                            errorMessage = "Please enter both your destination and trip duration."
                            return@Button
                        }

                        errorMessage = null
                        packingItems = emptyList()

                        if (myClosetItems.isEmpty()) {
                            errorMessage =
                                "Your closet is empty! Please add some clothes to your Closet first."
                            return@Button
                        }

                        isAILoading = true
                        scope.launch {
                            try {
                                val generativeModelPack = GenerativeModel(
                                    modelName = "gemini-2.5-flash",
                                    apiKey = com.example.dacs3.BuildConfig.GEMINI_API_KEY
                                )

                                val myClosetInventory =
                                    myClosetItems.joinToString("\n") { "- ${it.clothes_name} (${it.category})" }

                                val prompt = """
                                    You are an elite AI travel stylist and packing expert.
                                    Task: Create a precise packing checklist for an upcoming trip.
                                    
                                    TRIP CONTEXT:
                                    - Destination: $destination
                                    - Duration: $tripDays days
                                    - Real Weather Forecast: $currentTemp
                                    
                                    USER'S ACTUAL CLOSET INVENTORY:
                                    $myClosetInventory
                                    
                                    LOGIC REQUIREMENTS (STRICT):
                                    1. Use this strict mathematical formula for quantities based on $tripDays days:
                                       - Tops/Shirts: 1 item per day (Max 5).
                                       - Bottoms/Pants: 1 item per 2 days (Min 1, Max 3).
                                       - Underwear & Socks: $tripDays + 1 (Always add 1 extra for backup).
                                       - Shoes: Max 2 pairs.
                                    2. For CLOTHING, OUTERWEAR, and SHOES: You MUST ONLY select items that explicitly exist in the "USER'S ACTUAL CLOSET INVENTORY" above. DO NOT invent clothing items.
                                    3. For ESSENTIALS (underwear, toiletries, chargers, etc.): You may suggest these freely.
                                    4. Do not exceed a total of 15 clothing items to prevent overpacking.
                                    
                                    CRITICAL OUTPUT FORMATTING (MUST OBEY):
                                    - Return ONLY the raw list items. Absolutely NO Markdown formatting.
                                    - Every single line MUST start with exactly "- " (a dash followed by a space).
                                """.trimIndent()

                                val response = generativeModelPack.generateContent(prompt)
                                val responseText = response.text ?: ""

                                val items = responseText.lines()
                                    .filter { it.isNotBlank() }
                                    .map { it.removePrefix("-").trim() }

                                if (items.isNotEmpty()) {
                                    packingItems = items
                                } else {
                                    errorMessage =
                                        "AI couldn't generate the list. Please try again."
                                }
                            } catch (e: Exception) {
                                e.printStackTrace()
                                errorMessage = "Error Details: ${e.localizedMessage}"
                            } finally {
                                isAILoading = false
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = RoundedCornerShape(12.dp),
                    enabled = !isAILoading
                ) {
                    if (isAILoading) {
                        CircularProgressIndicator(
                            color = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("AI is generating list...", fontWeight = FontWeight.Bold)
                    } else {
                        Icon(Icons.Filled.AutoAwesome, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Generate Packing List", fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                if (errorMessage != null) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Filled.ErrorOutline,
                                null,
                                tint = MaterialTheme.colorScheme.error
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = errorMessage!!,
                                color = MaterialTheme.colorScheme.error,
                                fontSize = 14.sp
                            )
                        }
                    }
                }

                if (packingItems.isNotEmpty()) {
                    Divider(modifier = Modifier.padding(vertical = 16.dp))
                    Text(
                        text = "Your Checklist for $destination:",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .align(Alignment.Start)
                            .padding(bottom = 8.dp)
                    )

                    packingItems.forEach { item ->
                        var isChecked by remember { mutableStateOf(false) }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { isChecked = !isChecked }
                                .padding(vertical = 4.dp)
                        ) {
                            Checkbox(
                                checked = isChecked,
                                onCheckedChange = { isChecked = it },
                                colors = CheckboxDefaults.colors(checkedColor = MaterialTheme.colorScheme.primary)
                            )
                            Text(
                                text = item,
                                textDecoration = if (isChecked) TextDecoration.LineThrough else null,
                                color = if (isChecked) Color.Gray else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

// =======================================================
// CÁC COMPONENT GIAO DIỆN PHỤ TRỢ BÊN DƯỚI
// =======================================================
@Composable
fun SmartPackingCard(onPlanTripClick: () -> Unit) {
    val gradientBrush =
        Brush.horizontalGradient(colors = listOf(Color(0xFF6A1B9A), Color(0xFFAB47BC)))
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(8.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(gradientBrush)
                .padding(20.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Filled.FlightTakeoff,
                    null,
                    tint = Color.White,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    "Going Somewhere?",
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "Tell AI your destination and dates. We'll pack your bags in seconds.",
                color = Color.White.copy(0.9f),
                fontSize = 14.sp
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = onPlanTripClick,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.White,
                    contentColor = Color(0xFF6A1B9A)
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Filled.AutoAwesome, null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Plan AI Packing List", fontWeight = FontWeight.Bold)
            }
        }
    }
}

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
            greeting = "Good morning!",
            message = "It's a fresh morning ($currentTemperature). Let's pick a great outfit to start your day right!",
            buttonText = "View Morning Outfit",
            aiPrompt = "Show me the details of the Smart Casual outfit for my morning."
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
            message = "Time to wind down. Let's prep your outfit for tomorrow so you can sleep in an extra 15 minutes!",
            buttonText = "Plan Tomorrow's Look",
            aiPrompt = "Help me pick a comfortable outfit for tomorrow morning."
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
            .wrapContentHeight(),
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

            Spacer(modifier = Modifier.height(24.dp))

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

            Spacer(modifier = Modifier.height(32.dp))

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

// XÓA HOÀN TOÀN bringIntoViewRequester Ở ĐÂY ĐỂ TRỊ DỨT ĐIỂM LỖI BAY BÀN PHÍM
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
            .padding(horizontal = 24.dp)
            .padding(top = 8.dp, bottom = 4.dp) // Giảm khoảng trống để ô chat nằm sát hơn
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
            if (message.isFromUser || message.isError) {
                Text(
                    text = message.text,
                    color = if (message.isError) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onPrimary,
                    fontSize = 15.sp,
                    lineHeight = 22.sp
                )
            } else {
                MarkdownText(
                    markdown = message.text,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 15.sp
                )
            }
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

@Preview(showBackground = true)
@Composable
fun PreviewChatBarSection() {
    MaterialTheme {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.Bottom
        ) {
            ChatBarSection(
                promptText = "",
                isTyping = false,
                onValueChange = {},
                onSend = {}
            )
        }
    }
}