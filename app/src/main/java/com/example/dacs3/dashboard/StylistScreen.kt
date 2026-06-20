package com.example.dacs3.dashboard

import android.app.DatePickerDialog
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.material.icons.Icons
import androidx.compose.ui.layout.ContentScale

import android.app.Activity
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.Checkroom
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Mood
import androidx.compose.material.icons.outlined.Event
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
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
import io.github.jan.supabase.gotrue.auth
import java.time.LocalTime

// --- MESSAGE DATA CLASS ---
data class ChatMessage(
    val text: String,
    val isFromUser: Boolean,
    val isError: Boolean = false,
    val outfitIds: List<String>? = null,
    val image: Bitmap? = null
) {
    companion object {
        fun fromText(rawText: String, isFromUser: Boolean, isError: Boolean = false, image: Bitmap? = null): ChatMessage {
            if (isFromUser || isError) return ChatMessage(text = rawText, isFromUser = isFromUser, isError = isError, image = image)
            
            val outfitIdRegex = Regex("\\[OUTFIT_IDS:\\s*(.+?)\\]")
            val matchResult = outfitIdRegex.find(rawText)
            val outfitIds = matchResult?.groupValues?.get(1)?.split(",")?.map { it.trim() }
            
            // Chấp nhận mọi số lượng món đồ (miễn là không có N/A)
            val validOutfitIds = if (outfitIds != null && outfitIds.isNotEmpty() && !outfitIds.contains("N/A") && !outfitIds.any { it.isBlank() }) {
                outfitIds
            } else null

            val cleanText = if (matchResult != null) {
                rawText.replace(matchResult.value, "").trim()
            } else {
                rawText
            }
            
            return ChatMessage(text = cleanText, isFromUser = false, outfitIds = validOutfitIds)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StylistScreen(
    weatherViewModel: WeatherViewModel = viewModel(),
    dashboardViewModel: DashboardViewModel = viewModel(),
    onNavigateToSwiper: () -> Unit = {}
) {
    val view = LocalView.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // Thêm DrawerState để điều khiển thanh trượt lịch sử
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)

    var showLogOotdDialog by remember { mutableStateOf(false) }
    var ootdItemIdsToLog by remember { mutableStateOf<List<String>>(emptyList()) }
    
    // Style Matcher Service
    val styleMatcherService = remember { 
        com.example.dacs3.RS.StyleMatcherService(com.example.dacs3.BuildConfig.GEMINI_API_KEY) 
    }

    var promptText by remember { mutableStateOf("") }
    var socialTrends by remember { mutableStateOf("") }

    // Fetch Social Trends when profile is available
    LaunchedEffect(dashboardViewModel.userProfile) {
        dashboardViewModel.userProfile?.favoriteStyles?.let {
            socialTrends = dashboardViewModel.getSocialStyleTrends(it)
        }
    }
    var isAiTyping by remember { mutableStateOf(false) }
    var showChat by remember { mutableStateOf(false) }

    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    val chatMessages = remember { mutableStateListOf<ChatMessage>() }
    val listState = rememberLazyListState()

    val currentTemp by weatherViewModel.temperature.collectAsState()
    val currentCondition by weatherViewModel.condition.collectAsState()
    val tomorrowTemp by weatherViewModel.tomorrowTemperature.collectAsState()
    val tomorrowCondition by weatherViewModel.tomorrowCondition.collectAsState()
    val myClosetItems by dashboardViewModel.clothingItems.collectAsState()
    val feedbackMap by dashboardViewModel.clothingFeedbackMap.collectAsState()
    val chatSessions by dashboardViewModel.chatSessions.collectAsState()

    var selectedEvent by remember { mutableStateOf("University") }
    var selectedMood by remember { mutableStateOf("Confident") }

    var showMoodSheet by remember { mutableStateOf(false) }
    var showEventSheet by remember { mutableStateOf(false) }

    val eventCategories = mapOf(
        "Academic & Work" to listOf("University", "Presentation", "Library", "Internship"),
        "Social & Dating" to listOf("Coffee Date", "Dinner Date", "Party", "First Date"),
        "Active & Trip" to listOf("Gym Session", "Weekend Trip", "Hiking", "Beach Day")
    )

    val moodCategories = mapOf(
        "Energetic" to listOf("Confident", "Productive", "Bold", "Creative"),
        "Relaxed" to listOf("Chill", "Cozy", "Peaceful", "Effortless"),
        "Emotional" to listOf("Elegant", "Romantic", "Nostalgic", "Edgy")
    )

    // 1. THÊM BIẾN TRIGGER ĐỂ ÉP AI XÓA TRÍ NHỚ
    var chatSessionTrigger by remember { mutableIntStateOf(0) }

    // --- VẤN ĐỀ 1: BIẾN LƯU TRỮ LỊCH SỬ CHO BỘ NÃO AI ---
    var chatHistoryContext by remember { mutableStateOf<List<Content>>(emptyList()) }

    DisposableEffect(Unit) {
        val window = (context as? Activity)?.window
        val originalMode = window?.attributes?.softInputMode

        window?.let {
            WindowCompat.setDecorFitsSystemWindows(it, false)
            it.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
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


    val generativeModel = remember(myClosetItems, currentTemp, tomorrowTemp, tomorrowCondition) {
        val cleanItems = myClosetItems.filter { it.status.uppercase() !in listOf("WORN", "IN_WASH") }
        val closetData = if (cleanItems.isNotEmpty()) {
            cleanItems.joinToString("\n") { "- ID: ${it.id} | ${it.clothes_name} (${it.category}, Color: ${it.mainColor})" }
        } else {
            "User's closet has no clean items available right now."
        }

        GenerativeModel(
            modelName = "gemini-3.1-flash-lite",
            apiKey = com.example.dacs3.BuildConfig.GEMINI_API_KEY,
            systemInstruction = content {
                text(
                    """
                    You are an expert personal fashion stylist named WearWise AI.
                    Your goal is to help the user pick outfits, answer fashion questions, and give style advice.
                    
                    CURRENT CONTEXT:
                    - Current Weather: $currentTemp
                    - Tomorrow's Weather Forecast: $tomorrowTemp ($tomorrowCondition)
                    - Community Fashion Trends: $socialTrends
                    - User's actual closet inventory: 
                    $closetData
                    
                    STRICT RULES:
                    1. When recommending specific clothing items to wear, YOU MUST ONLY suggest items that exist in the user's closet inventory above.
                    2. If they ask for general advice, you can answer freely.
                    3. Reply concisely, friendly, and logically. Format with bullet points if listing items.
                    4. IMPORTANT: Always reply in the exact same language the user uses.
                    5. When recommending an outfit, YOU MUST suggest a complete set of items (at minimum 1 Top and 1 Bottom). To be professional and comprehensive, YOU SHOULD also suggest relevant Shoes and Accessories (Bags, Watches, etc.) from the inventory.
                    6. For every outfit recommendation, YOU MUST include ALL selected item IDs at the very end of your message in this exact format: [OUTFIT_IDS: id1, id2, id3, ...]
                    7. Do NOT include the [OUTFIT_IDS: ...] tag if you are not recommending specific items from the closet.
                    8. NEVER use "N/A" inside the tag. If an item is missing from the closet, do not suggest it.
                    9. CRITICAL: When describing outfit items in your text response, ONLY show the item name (e.g., "Beige Camp Collar Shirt"). NEVER display or mention the item's ID in your visible text response. IDs must ONLY appear inside the [OUTFIT_IDS: ...] tag at the end.
                """.trimIndent()
                )
            }
        )
    }

    // 2. CẬP NHẬT REMEMBER CỦA CHAT SESSION THEO TRIGGER VÀ HISTORY
    val chatSession = remember(generativeModel, chatSessionTrigger, chatHistoryContext) {
        generativeModel.startChat(history = chatHistoryContext)
    }

    // --- STYLE MATCH: TRÌNH CHỌN ẢNH IDOL ---
    val photoLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            isAiTyping = true
            showChat = true
            
            scope.launch {
                try {
                    // Chuyển việc giải mã ảnh sang Dispatchers.IO để tránh làm treo UI
                    val bitmap = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                        if (Build.VERSION.SDK_INT < 28) {
                            MediaStore.Images.Media.getBitmap(context.contentResolver, it)
                        } else {
                            val source = ImageDecoder.createSource(context.contentResolver, it)
                            ImageDecoder.decodeBitmap(source)
                        }
                    }

                    // 1. Hiện hình ảnh và câu lệnh của người dùng vào chat bằng tiếng Anh
                    chatMessages.add(ChatMessage(
                        text = "Find items in my closet similar to the person in this photo.",
                        isFromUser = true,
                        image = bitmap
                    ))

                    val cleanCelebrityItems = myClosetItems.filter { it.status.uppercase() !in listOf("WORN", "IN_WASH") }
                    val matches = styleMatcherService.matchCelebrityStyle(bitmap, cleanCelebrityItems, feedbackMap)
    if (matches.isNotEmpty()) {
                        val ids = matches.mapNotNull { it.id }
                        val itemDetails = matches.joinToString("\n") { 
                            "- ${it.clothes_name} (Category: ${it.category}, Color: ${it.mainColor})" 
                        }
                        
                        // Improved professional prompt for the AI Stylist's response
                        val hiddenPrompt = """
                            COMPARE & CONTRAST TASK:
                            I want to recreate the celebrity's outfit in the attached photo using my own items.
                            Here are the specific items from my closet that we are considering:
                            $itemDetails
                        
                            Please provide a STRICT and HONEST professional analysis:
                            1. Similarity: How well do my items match the celebrity's look in terms of color, silhouette, and vibe?
                            2. Differences (The Gap): Be brutally honest about the differences. (e.g., "Your top is cotton while the celebrity wears silk", or "The shade of blue in your closet is much darker").
                            3. Stylist's Advice: How should I wear or tweak these items to bridge the gap and get as close as possible to the inspiration photo?
                            
                            Style Verdict: Give a score out of 10 for the "Match Accuracy".
                            
                            CRITICAL: End your response with the tag: [OUTFIT_IDS: ${ids.joinToString(", ")}]
                        """.trimIndent()
                        
                        val response = chatSession.sendMessage(
                            com.google.ai.client.generativeai.type.content {
                                image(bitmap)
                                text(hiddenPrompt)
                            }
                        )
                        val aiText = response.text ?: ""
                        
                        chatMessages.add(ChatMessage.fromText(rawText = aiText, isFromUser = false))
                        
                        // Lưu vào database
                        dashboardViewModel.saveChatToDatabase(
                            userMessage = "[Style Match Image]", 
                            aiMessage = aiText
                        )
                    } else {
                        chatMessages.add(ChatMessage(
                            text = "Sorry, I couldn't find any items in your closet that match this celebrity style.",
                            isFromUser = false,
                            isError = true
                        ))
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    chatMessages.add(ChatMessage(text = "Error analyzing image. Please try again.", isFromUser = false, isError = true))
                } finally {
                    isAiTyping = false
                }
            }
        }
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

                chatMessages.add(ChatMessage.fromText(rawText = aiText, isFromUser = false))

                dashboardViewModel.saveChatToDatabase(
                    userMessage = userPrompt,
                    aiMessage = aiText // Lưu text gốc vào DB
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

    val prefilledPrompt by dashboardViewModel.stylistPrefilledPrompt
    LaunchedEffect(prefilledPrompt) {
        val prompt = prefilledPrompt
        if (prompt != null) {
            sendMessageToAI(prompt)
            dashboardViewModel.stylistPrefilledPrompt.value = null
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
    var isAILoading by remember { mutableStateOf(false) }
    var packingItems by remember { mutableStateOf(listOf<String>()) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // --- CÁC BIẾN CHỌN LỊCH VÀ CHUYỂN TAB DU LỊCH ---
    var activeTab by remember { mutableIntStateOf(0) }
    var selectedStartDateMillis by remember { mutableStateOf<Long?>(null) }
    var selectedEndDateMillis by remember { mutableStateOf<Long?>(null) }

    LaunchedEffect(dashboardViewModel.showTravelHistoryTrigger) {
        if (dashboardViewModel.showTravelHistoryTrigger) {
            showPackingSheet = true
            activeTab = 1
            dashboardViewModel.showTravelHistoryTrigger = false
        }
    }

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
                            .windowInsetsPadding(WindowInsets.safeDrawing)
                    ) {
                        Box(modifier = Modifier.padding(horizontal = 24.dp)) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 16.dp, bottom = 20.dp),
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

                        Box(modifier = Modifier.weight(1f)) {
                            if (!showChat) {
                                Column(
                                    modifier = Modifier
                                        .padding(horizontal = 24.dp)
                                        .fillMaxSize()
                                        .verticalScroll(rememberScrollState())
                                ) {
                                    Card(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(bottom = 16.dp)
                                            .clickable { onNavigateToSwiper() }
                                            .shadow(8.dp, RoundedCornerShape(20.dp)),
                                        shape = RoundedCornerShape(20.dp),
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(16.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(48.dp)
                                                    .background(MaterialTheme.colorScheme.secondary, CircleShape),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    Icons.Filled.AutoAwesome,
                                                    null,
                                                    tint = Color.White,
                                                    modifier = Modifier.size(24.dp)
                                                )
                                            }
                                            Spacer(modifier = Modifier.width(16.dp))
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    "Outfit Matcher Game 🎮",
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 15.sp,
                                                    color = MaterialTheme.colorScheme.secondary
                                                )
                                                Text(
                                                    "Swipe to match and train your AI!",
                                                    fontSize = 12.sp,
                                                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f)
                                                )
                                            }
                                            Icon(
                                                Icons.Filled.ArrowForward,
                                                null,
                                                tint = MaterialTheme.colorScheme.secondary
                                            )
                                        }
                                    }

                                    ContextBriefingCard(
                                        currentTemperature = currentTemp,
                                        onActionClick = { aiPrompt ->
                                            sendMessageToAI(aiPrompt)
                                        }
                                    )

                                    QuickPromptsSection(onPromptClick = { selectedPrompt ->
                                        sendMessageToAI(selectedPrompt)
                                    })

                                    Column(modifier = Modifier.padding(bottom = 20.dp)) {
                                        Text(
                                            "What's the plan today?",
                                            fontSize = 15.sp,
                                            color = MaterialTheme.colorScheme.primary,
                                            fontWeight = FontWeight.SemiBold,
                                            modifier = Modifier.padding(bottom = 12.dp)
                                        )
                                        FilterChip(
                                            selected = true,
                                            onClick = { showEventSheet = true },
                                            label = { Text("Event: $selectedEvent", fontWeight = FontWeight.Medium) },
                                            leadingIcon = {
                                                Icon(
                                                    Icons.Outlined.Event,
                                                    null,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            },
                                            trailingIcon = {
                                                Icon(
                                                    Icons.Filled.KeyboardArrowDown,
                                                    null,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            },
                                            colors = FilterChipDefaults.filterChipColors(
                                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                                selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                                                selectedLeadingIconColor = MaterialTheme.colorScheme.onPrimary,
                                                selectedTrailingIconColor = MaterialTheme.colorScheme.onPrimary
                                            ),
                                            shape = RoundedCornerShape(12.dp), border = null
                                        )

                                        Spacer(modifier = Modifier.height(16.dp))

                                        Column(modifier = Modifier.padding(bottom = 5.dp)) {
                                            Text(
                                                "How do you feel today?",
                                                fontSize = 15.sp,
                                                color = MaterialTheme.colorScheme.primary,
                                                fontWeight = FontWeight.SemiBold,
                                                modifier = Modifier.padding(bottom = 12.dp)
                                            )
                                            FilterChip(
                                                selected = true,
                                                onClick = { showMoodSheet = true },
                                                label = { Text("Mood: $selectedMood", fontWeight = FontWeight.Medium) },
                                                leadingIcon = {
                                                    Icon(
                                                        Icons.Outlined.Mood,
                                                        null,
                                                        modifier = Modifier.size(18.dp)
                                                    )
                                                },
                                                trailingIcon = {
                                                    Icon(
                                                        Icons.Filled.KeyboardArrowDown,
                                                        null,
                                                        modifier = Modifier.size(18.dp)
                                                    )
                                                },
                                                colors = FilterChipDefaults.filterChipColors(
                                                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                                                    selectedLeadingIconColor = MaterialTheme.colorScheme.onPrimary,
                                                    selectedTrailingIconColor = MaterialTheme.colorScheme.onPrimary
                                                ),
                                                shape = RoundedCornerShape(12.dp), border = null
                                            )
                                        }
                                    }

                                    OutfitCanvasSection(
                                        viewModel = dashboardViewModel,
                                        weatherContext = "$currentTemp, $currentCondition | Mood: $selectedMood | Event: $selectedEvent",
                                        onWearClick = { ids ->
                                            ootdItemIdsToLog = ids
                                            showLogOotdDialog = true
                                        }
                                    )

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
                                        ChatBubble(
                                            message = message,
                                            onOutfitSelect = { ids ->
                                                val selectedItems = ids.mapNotNull { id ->
                                                    myClosetItems.find { it.id == id }
                                                }

                                                if (selectedItems.isNotEmpty()) {
                                                    dashboardViewModel.aiCanvasOutfit.value = com.example.dacs3.connectDB.Outfit(selectedItems)
                                                    showChat = false
                                                    Toast.makeText(context, "Outfit selected on Canvas!", Toast.LENGTH_SHORT).show()
                                                } else {
                                                    Toast.makeText(context, "Could not find these items in your closet.", Toast.LENGTH_SHORT).show()
                                                }
                                            },
                                            onLogOotd = { ids ->
                                                ootdItemIdsToLog = ids
                                                showLogOotdDialog = true
                                            },
                                            onItemFeedback = { id, delta ->
                                                dashboardViewModel.saveClothingFeedback(id, delta)
                                                val currentRating = dashboardViewModel.clothingFeedbackMap.value[id] ?: 0
                                                val newRating = currentRating + delta
                                                Toast.makeText(
                                                    context,
                                                    if (delta > 0) "Glad you like it! 👍 (Rating: $newRating)"
                                                    else "Got it, I'll avoid this! 👎 (Rating: $newRating)",
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                            }
                                        )
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

                        ChatBarSection(
                            promptText = promptText,
                            isTyping = isAiTyping,
                            onValueChange = { promptText = it },
                            onSend = { sendMessageToAI(promptText) },
                            onFocus = { showChat = true },
                            onImageSelect = { photoLauncher.launch("image/*") }
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
            val userId = com.example.dacs3.connectDB.supabase.auth.currentUserOrNull()?.id ?: ""
            var isSavingList by remember { mutableStateOf(false) }

            LaunchedEffect(userId) {
                if (userId.isNotEmpty()) {
                    dashboardViewModel.fetchPackingLists(userId)
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.safeDrawing)
                    .heightIn(max = 650.dp)
                    .padding(horizontal = 24.dp, vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "AI Travel Assistant ✈️",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                TabRow(
                    selectedTabIndex = activeTab,
                    containerColor = Color.Transparent,
                    contentColor = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 16.dp)
                ) {
                    Tab(
                        selected = activeTab == 0,
                        onClick = { activeTab = 0 },
                        text = { Text("Plan Trip", fontWeight = FontWeight.Bold) },
                        icon = { Icon(Icons.Filled.FlightTakeoff, null) }
                    )
                    Tab(
                        selected = activeTab == 1,
                        onClick = { activeTab = 1 },
                        text = { Text("Trip History", fontWeight = FontWeight.Bold) },
                        icon = { Icon(Icons.Filled.History, null) }
                    )
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    if (activeTab == 0) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState()),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            OutlinedTextField(
                                value = destination,
                                onValueChange = { destination = it },
                                label = { Text("Where are you going?") },
                                placeholder = { Text("e.g., Da Lat, Hanoi, Tokyo...") },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp)
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            val departureDateStr = selectedStartDateMillis?.let {
                                java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date(it))
                            } ?: ""
                            val returnDateStr = selectedEndDateMillis?.let {
                                java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date(it))
                            } ?: ""
                            val calculatedTripDays = if (selectedStartDateMillis != null && selectedEndDateMillis != null) {
                                val diff = selectedEndDateMillis!! - selectedStartDateMillis!!
                                if (diff >= 0) {
                                    (diff / (1000 * 60 * 60 * 24)).toInt() + 1
                                } else 0
                            } else 0

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                // 🛫 DEPARTURE DATE
                                OutlinedButton(
                                    onClick = {
                                        val startCalendar = java.util.Calendar.getInstance()
                                        selectedStartDateMillis?.let { startCalendar.timeInMillis = it }

                                        DatePickerDialog(
                                            context,
                                            { _, year, month, dayOfMonth ->
                                                val selectedCal = java.util.Calendar.getInstance().apply {
                                                    set(java.util.Calendar.YEAR, year)
                                                    set(java.util.Calendar.MONTH, month)
                                                    set(java.util.Calendar.DAY_OF_MONTH, dayOfMonth)
                                                    set(java.util.Calendar.HOUR_OF_DAY, 0)
                                                    set(java.util.Calendar.MINUTE, 0)
                                                    set(java.util.Calendar.SECOND, 0)
                                                    set(java.util.Calendar.MILLISECOND, 0)
                                                }
                                                selectedStartDateMillis = selectedCal.timeInMillis
                                                if (selectedEndDateMillis != null && selectedCal.timeInMillis > selectedEndDateMillis!!) {
                                                    selectedEndDateMillis = null
                                                }
                                            },
                                            startCalendar.get(java.util.Calendar.YEAR),
                                            startCalendar.get(java.util.Calendar.MONTH),
                                            startCalendar.get(java.util.Calendar.DAY_OF_MONTH)
                                        ).show()
                                    },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(58.dp),
                                    shape = RoundedCornerShape(12.dp),
                                    contentPadding = PaddingValues(horizontal = 8.dp),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text("Departure 🛫", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Filled.CalendarMonth, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = departureDateStr.ifEmpty { "Select Date" },
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 14.sp,
                                                color = if (departureDateStr.isNotEmpty()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }

                                // 🛬 RETURN DATE
                                OutlinedButton(
                                    onClick = {
                                        val endCalendar = java.util.Calendar.getInstance()
                                        selectedEndDateMillis?.let { endCalendar.timeInMillis = it } ?: selectedStartDateMillis?.let { endCalendar.timeInMillis = it }

                                        DatePickerDialog(
                                            context,
                                            { _, year, month, dayOfMonth ->
                                                val selectedCal = java.util.Calendar.getInstance().apply {
                                                    set(java.util.Calendar.YEAR, year)
                                                    set(java.util.Calendar.MONTH, month)
                                                    set(java.util.Calendar.DAY_OF_MONTH, dayOfMonth)
                                                    set(java.util.Calendar.HOUR_OF_DAY, 0)
                                                    set(java.util.Calendar.MINUTE, 0)
                                                    set(java.util.Calendar.SECOND, 0)
                                                    set(java.util.Calendar.MILLISECOND, 0)
                                                }
                                                val selectedTime = selectedCal.timeInMillis
                                                if (selectedStartDateMillis != null && selectedTime < selectedStartDateMillis!!) {
                                                    Toast.makeText(context, "Return date cannot be before departure date!", Toast.LENGTH_SHORT).show()
                                                } else {
                                                    selectedEndDateMillis = selectedTime
                                                }
                                            },
                                            endCalendar.get(java.util.Calendar.YEAR),
                                            endCalendar.get(java.util.Calendar.MONTH),
                                            endCalendar.get(java.util.Calendar.DAY_OF_MONTH)
                                        ).show()
                                    },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(58.dp),
                                    shape = RoundedCornerShape(12.dp),
                                    contentPadding = PaddingValues(horizontal = 8.dp),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text("Return 🛬", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Filled.CalendarMonth, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = returnDateStr.ifEmpty { "Select Date" },
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 14.sp,
                                                color = if (returnDateStr.isNotEmpty()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            }

                            if (calculatedTripDays > 0) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Total Duration: $calculatedTripDays Days",
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }

                            Spacer(modifier = Modifier.height(24.dp))

                            Button(
                                onClick = {
                                    sheetKeyboardController?.hide()
                                    sheetFocusManager.clearFocus()

                                    if (destination.isBlank() || selectedStartDateMillis == null || selectedEndDateMillis == null) {
                                        errorMessage = "Please enter your destination and select dates on the calendar."
                                        return@Button
                                    }

                                    errorMessage = null
                                    packingItems = emptyList()

                                    if (myClosetItems.isEmpty()) {
                                        errorMessage = "Your closet is empty! Please add some clothes to your Closet first."
                                        return@Button
                                    }

                                    isAILoading = true
                                    scope.launch {
                                        try {
                                            val generativeModelPack = GenerativeModel(
                                                modelName = "gemini-3.1-flash-lite",
                                                apiKey = com.example.dacs3.BuildConfig.GEMINI_API_KEY
                                            )

                                            val cleanPackingItems = myClosetItems.filter { it.status.uppercase() !in listOf("WORN", "IN_WASH") }
                                            val myClosetInventory = cleanPackingItems.joinToString("\n") { "- ${it.clothes_name} (${it.category})" }

                                            val prompt = """
                                                You are an elite AI travel stylist and packing expert.
                                                Task: Create a precise packing checklist for an upcoming trip.
                                                
                                                TRIP CONTEXT:
                                                - Destination: $destination
                                                - Duration: $calculatedTripDays days
                                                - Real Weather Forecast: $currentTemp
                                                
                                                USER'S ACTUAL CLOSET INVENTORY:
                                                $myClosetInventory
                                                
                                                LOGIC REQUIREMENTS (STRICT):
                                                1. Use this strict mathematical formula for quantities based on $calculatedTripDays days:
                                                   - Tops/Shirts: 1 item per day (Max 5).
                                                   - Bottoms/Pants: 1 item per 2 days (Min 1, Max 3).
                                                   - Underwear & Socks: $calculatedTripDays + 1 (Always add 1 extra for backup).
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
                                                errorMessage = "AI couldn't generate the list. Please try again."
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
                                HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))
                                Text(
                                    text = "Your Packing Checklist:",
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.align(Alignment.Start).padding(bottom = 8.dp)
                                )

                                packingItems.forEach { item ->
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 4.dp)
                                    ) {
                                        Icon(Icons.Filled.Check, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Text(text = item, color = MaterialTheme.colorScheme.onSurface)
                                    }
                                }

                                Spacer(modifier = Modifier.height(20.dp))

                                Button(
                                    onClick = {
                                        if (userId.isNotEmpty()) {
                                            isSavingList = true
                                            dashboardViewModel.savePackingList(
                                                userId = userId,
                                                destination = destination,
                                                tripDays = calculatedTripDays,
                                                weatherTemp = currentTemp,
                                                items = packingItems,
                                                departureDate = departureDateStr,
                                                returnDate = returnDateStr,
                                                onComplete = { success, errorMsg ->
                                                     isSavingList = false
                                                     if (success) {
                                                         Toast.makeText(context, "Checklist saved successfully!", Toast.LENGTH_SHORT).show()
                                                         destination = ""
                                                         selectedStartDateMillis = null
                                                         selectedEndDateMillis = null
                                                         packingItems = emptyList()
                                                         activeTab = 1
                                                     } else {
                                                         Toast.makeText(context, "Error: ${errorMsg ?: "Unknown error"}", Toast.LENGTH_LONG).show()
                                                     }
                                                }
                                            )
                                        } else {
                                            Toast.makeText(context, "Please sign in to save your checklist!", Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(50.dp),
                                    shape = RoundedCornerShape(12.dp),
                                    enabled = !isSavingList
                                ) {
                                    if (isSavingList) {
                                        CircularProgressIndicator(
                                            color = MaterialTheme.colorScheme.onPrimary,
                                            modifier = Modifier.size(24.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Saving Checklist...", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimary)
                                    } else {
                                        Icon(Icons.Filled.Save, null, tint = MaterialTheme.colorScheme.onPrimary)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Save Checklist & Schedule Departure", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimary)
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(32.dp))
                        }
                    } else {
                        val trips by dashboardViewModel.packingListsHistory.collectAsState()
                        if (trips.isEmpty()) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .verticalScroll(rememberScrollState()),
                                verticalArrangement = Arrangement.Center,
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    Icons.Filled.FlightTakeoff,
                                    null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                                    modifier = Modifier.size(64.dp)
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = "No trip history yet.",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "Go to 'Plan Trip' to create a packing list!",
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                )
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                items(trips) { trip ->
                                    var isExpanded by remember { mutableStateOf(false) }
                                    val packedCount = trip.items.count { it.isPacked }
                                    val totalCount = trip.items.size

                                    Card(
                                        shape = RoundedCornerShape(16.dp),
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { isExpanded = !isExpanded }
                                    ) {
                                        Column(modifier = Modifier.padding(16.dp)) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(
                                                        text = trip.list.destination,
                                                        fontSize = 18.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = MaterialTheme.colorScheme.primary
                                                    )
                                                    Text(
                                                        text = "${trip.list.departureDate} ➔ ${trip.list.returnDate} (${trip.list.tripDays} Days)",
                                                        fontSize = 12.sp,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }
                                                IconButton(onClick = {
                                                    dashboardViewModel.deletePackingList(trip.list.id!!, userId)
                                                }) {
                                                    Icon(
                                                        Icons.Filled.Delete,
                                                        "Delete",
                                                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f)
                                                    )
                                                }
                                            }

                                            Spacer(modifier = Modifier.height(10.dp))

                                            val progress = if (totalCount > 0) packedCount.toFloat() / totalCount else 0f
                                            LinearProgressIndicator(
                                                progress = progress,
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height(6.dp)
                                                    .clip(RoundedCornerShape(3.dp)),
                                                color = MaterialTheme.colorScheme.primary,
                                                trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                                            )
                                            Spacer(modifier = Modifier.height(6.dp))
                                            Text(
                                                text = "$packedCount / $totalCount Packed Items",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.primary
                                            )

                                            if (isExpanded && totalCount > 0) {
                                                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
                                                trip.items.forEach { item ->
                                                    Row(
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .padding(vertical = 2.dp)
                                                            .clickable {
                                                                dashboardViewModel.updatePackingItemStatus(item.id!!, !item.isPacked, userId)
                                                            }
                                                    ) {
                                                        Checkbox(
                                                            checked = item.isPacked,
                                                            onCheckedChange = { isChecked ->
                                                                dashboardViewModel.updatePackingItemStatus(item.id!!, isChecked ?: false, userId)
                                                            },
                                                            colors = CheckboxDefaults.colors(checkedColor = MaterialTheme.colorScheme.primary)
                                                        )
                                                        Text(
                                                            text = item.name,
                                                            textDecoration = if (item.isPacked) TextDecoration.LineThrough else null,
                                                            color = if (item.isPacked) Color.Gray else MaterialTheme.colorScheme.onSurface,
                                                            fontSize = 13.sp
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
                }
            }
        }
    }

    LogOotdConfirmDialog(
        showDialog = showLogOotdDialog,
        onDismissRequest = {
            showLogOotdDialog = false
            ootdItemIdsToLog = emptyList()
        },
        onConfirm = { occasions, seasons ->
            dashboardViewModel.userProfile?.id?.let { userId ->
                val tempVal = currentTemp.removeSuffix("°C").replace(",", ".").toFloatOrNull()
                dashboardViewModel.logOotd(
                    userId = userId,
                    clothingIds = ootdItemIdsToLog,
                    weatherMain = "AI Recommendation",
                    temp = tempVal,
                    occasions = occasions,
                    season = seasons.joinToString(", ").takeIf { it.isNotBlank() },
                    onSuccess = {
                        Toast.makeText(
                            context,
                            "✅ OOTD saved successfully!",
                            Toast.LENGTH_SHORT
                        ).show()
                    },
                    onError = { errMsg ->
                        Toast.makeText(
                            context,
                            "❌ Failed to save OOTD: $errMsg",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                )
            }
            showLogOotdDialog = false
            ootdItemIdsToLog = emptyList()
        }
    )

    if (showMoodSheet) {
        ModalBottomSheet(
            onDismissRequest = { showMoodSheet = false },
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.background
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 8.dp)
            ) {
                item {
                    Text(
                        "Select your feeling!",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                }

                moodCategories.forEach { (category, moods) ->
                    item {
                        Text(
                            category.uppercase(),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                    }
                    item {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.padding(bottom = 24.dp)
                        ) {
                            moods.chunked(2).forEach { rowMoods ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    rowMoods.forEach { mood ->
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .clip(RoundedCornerShape(12.dp))
                                                .background(if (selectedMood == mood) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface)
                                                .clickable {
                                                    selectedMood = mood
                                                    showMoodSheet = false
                                                }
                                                .padding(vertical = 12.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                mood,
                                                color = if (selectedMood == mood) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary,
                                                fontWeight = if (selectedMood == mood) FontWeight.Bold else FontWeight.Medium,
                                                fontSize = 13.sp
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                item { Spacer(modifier = Modifier.height(40.dp)) }
            }
        }
    }

    if (showEventSheet) {
        ModalBottomSheet(
            onDismissRequest = { showEventSheet = false },
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.background
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 8.dp)
            ) {
                item {
                    Text(
                        "Select your occasion",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                }
                eventCategories.forEach { (category, events) ->
                    item {
                        Text(
                            category.uppercase(),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                    }
                    item {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.padding(bottom = 24.dp)
                        ) {
                            events.chunked(2).forEach { rowEvents ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    rowEvents.forEach { event ->
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .clip(RoundedCornerShape(12.dp))
                                                .background(if (selectedEvent == event) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface)
                                                .clickable {
                                                    selectedEvent = event
                                                    showEventSheet = false
                                                }
                                                .padding(vertical = 12.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                event,
                                                color = if (selectedEvent == event) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary,
                                                fontWeight = if (selectedEvent == event) FontWeight.Bold else FontWeight.Medium,
                                                fontSize = 13.sp
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                item { Spacer(modifier = Modifier.height(40.dp)) }
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
fun OutfitCanvasSection(viewModel: DashboardViewModel, weatherContext: String, onWearClick: (List<String>) -> Unit) {
    val aiCanvasOutfit by viewModel.aiCanvasOutfit.collectAsState()
    val isLoading = viewModel.isCanvasLoading
    val canvasError = viewModel.canvasError
    val isExplorationMode by viewModel.isExplorationModeEnabled.collectAsState()
    val feedbackMap by viewModel.clothingFeedbackMap.collectAsState()
    val context = LocalContext.current

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight(),
        shape = RoundedCornerShape(32.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        var showResetDialog by remember { mutableStateOf(false) }
        val itemsToKeep = remember { mutableStateListOf<String>() }
        
        if (showResetDialog && aiCanvasOutfit != null) {
            androidx.compose.ui.window.Dialog(onDismissRequest = { showResetDialog = false }) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(28.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 12.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp)
                    ) {
                        // Header
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(28.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                "Custom Refresh",
                                fontSize = 22.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Text(
                            "Select the items you want to KEEP. Unselected items will be replaced.",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 20.sp
                        )
                        
                        Spacer(modifier = Modifier.height(20.dp))
                        
                        // Item List
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            aiCanvasOutfit!!.items.forEach { item ->
                                val isKept = itemsToKeep.contains(item.id)
                                val bgColor = if (isKept) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                                val borderColor = if (isKept) MaterialTheme.colorScheme.primary else Color.Transparent
                                val iconTint = if (isKept) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                
                                Surface(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            val id = item.id ?: return@clickable
                                            if (isKept) itemsToKeep.remove(id) else itemsToKeep.add(id)
                                        },
                                    shape = RoundedCornerShape(16.dp),
                                    color = bgColor,
                                    border = BorderStroke(1.dp, borderColor)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        // Thumbnail
                                        Box(
                                            modifier = Modifier
                                                .size(48.dp)
                                                .clip(RoundedCornerShape(12.dp))
                                                .background(MaterialTheme.colorScheme.surface),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            if (!item.imageUrl.isNullOrEmpty()) {
                                                coil.compose.AsyncImage(
                                                    model = item.imageUrl,
                                                    contentDescription = null,
                                                    contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                                                    modifier = Modifier.fillMaxSize()
                                                )
                                            } else {
                                                Icon(Icons.Outlined.Checkroom, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                            }
                                        }
                                        
                                        Spacer(modifier = Modifier.width(12.dp))
                                        
                                        // Text Info
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = item.category.uppercase(),
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.primary,
                                                letterSpacing = 1.sp
                                            )
                                            Text(
                                                text = item.clothes_name,
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = MaterialTheme.colorScheme.onSurface,
                                                maxLines = 1,
                                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                            )
                                        }
                                        
                                        // Check/Refresh Icon
                                        Box(
                                            modifier = Modifier
                                                .size(28.dp)
                                                .clip(CircleShape)
                                                .background(if (isKept) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = if (isKept) Icons.Default.Check else Icons.Default.Refresh,
                                                contentDescription = null,
                                                tint = if (isKept) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(28.dp))
                        
                        // Actions
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            TextButton(
                                onClick = { showResetDialog = false },
                                modifier = Modifier.padding(end = 8.dp)
                            ) {
                                Text("Cancel", fontWeight = FontWeight.Bold)
                            }
                            Button(
                                onClick = {
                                    showResetDialog = false
                                    val replaceIds = aiCanvasOutfit!!.items.mapNotNull { it.id }.filter { it !in itemsToKeep }
                                    replaceIds.forEach { id -> viewModel.canvasExcludedIds.add(id) }
                                    viewModel.generateCanvasOutfit(weatherContext, itemsToKeep.toSet())
                                },
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Text("Regenerate", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(vertical = 24.dp, horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    "AI Outfit Canvas",
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
                        "Your daily tailored look",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.secondary,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Premium Animated Toggle Pill
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Regular Style Tab
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(16.dp))
                            .background(if (!isExplorationMode) MaterialTheme.colorScheme.primary else Color.Transparent)
                            .clickable {
                                if (isExplorationMode) {
                                    viewModel.isExplorationModeEnabled.value = false
                                    viewModel.aiCanvasOutfit.value = null
                                }
                            }
                            .padding(vertical = 10.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Outlined.Checkroom,
                                contentDescription = null,
                                tint = if (!isExplorationMode) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                "Regular Style",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (!isExplorationMode) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    // Style Adventure Tab
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(16.dp))
                            .background(if (isExplorationMode) MaterialTheme.colorScheme.secondary else Color.Transparent)
                            .clickable {
                                if (!isExplorationMode) {
                                    viewModel.isExplorationModeEnabled.value = true
                                    viewModel.aiCanvasOutfit.value = null
                                }
                            }
                            .padding(vertical = 10.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = if (isExplorationMode) MaterialTheme.colorScheme.onSecondary else MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                "Style Adventure",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isExplorationMode) MaterialTheme.colorScheme.onSecondary else MaterialTheme.colorScheme.secondary
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            if (isLoading) {
                // Loading State
                CircularProgressIndicator(
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(32.dp)
                )
                Text(
                    "Stylist is choosing your outfit...",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(32.dp))
            } else if (aiCanvasOutfit != null) {
                // Hiển thị đồ thật
                val outfit = aiCanvasOutfit!!
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    outfit.items.forEach { item ->
                        val itemRating = feedbackMap[item.id ?: ""] ?: 0
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.weight(1f)) {
                                OutfitItemPlaceholder(
                                    name = item.clothes_name,
                                    icon = Icons.Outlined.Checkroom,
                                    subtext = "${item.category} • ${item.mainColor}",
                                    iconBgColor = when(item.category.lowercase()) {
                                        "top" -> MaterialTheme.colorScheme.surfaceVariant
                                        "bottom" -> MaterialTheme.colorScheme.secondaryContainer
                                        "shoes" -> MaterialTheme.colorScheme.tertiaryContainer
                                        else -> MaterialTheme.colorScheme.primaryContainer
                                    },
                                    iconColor = when(item.category.lowercase()) {
                                        "top" -> MaterialTheme.colorScheme.primary
                                        "bottom" -> MaterialTheme.colorScheme.secondary
                                        "shoes" -> Color(0xFFFF9800)
                                        else -> MaterialTheme.colorScheme.tertiary
                                    },
                                    imageUrl = item.imageUrl
                                )
                            }
                            Column(
                                                                modifier = Modifier.padding(start = 8.dp),
                                                                horizontalAlignment = Alignment.CenterHorizontally
                                                            ) {
                                                                Row(
                                                                    verticalAlignment = Alignment.CenterVertically,
                                                                    horizontalArrangement = Arrangement.Start
                                                                ) {
                                                                    IconButton(onClick = {
                                                                        viewModel.saveClothingFeedback(item.id ?: "", 1)
                                                                        Toast.makeText(
                                                                            context,
                                                                            "Glad you like it! 👍 (Rating: ${itemRating + 1})",
                                                                            Toast.LENGTH_SHORT
                                                                        ).show()
                                                                    }) {
                                                                        Icon(
                                                                            imageVector = Icons.Default.ThumbUp,
                                                                            contentDescription = null,
                                                                            modifier = Modifier.size(18.dp),
                                                                            tint = if (itemRating > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                                                                        )
                                                                    }
                                                                    if (itemRating > 0) {
                                                                        Text(
                                                                            text = "+$itemRating",
                                                                            color = MaterialTheme.colorScheme.primary,
                                                                            fontSize = 11.sp,
                                                                            fontWeight = FontWeight.Bold,
                                                                            modifier = Modifier.padding(end = 4.dp)
                                                                        )
                                                                    }
                                                                }
                                                                Row(
                                                                    verticalAlignment = Alignment.CenterVertically,
                                                                    horizontalArrangement = Arrangement.Start
                                                                ) {
                                                                    IconButton(onClick = {
                                                                        viewModel.saveClothingFeedback(item.id ?: "", -1)
                                                                        Toast.makeText(
                                                                            context,
                                                                            "Got it, I'll avoid this! 👎 (Rating: ${itemRating - 1})",
                                                                            Toast.LENGTH_SHORT
                                                                        ).show()
                                                                    }) {
                                                                        Icon(
                                                                            imageVector = Icons.Default.ThumbDown,
                                                                            contentDescription = null,
                                                                            modifier = Modifier.size(18.dp),
                                                                            tint = if (itemRating < 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                                                                        )
                                                                    }
                                                                    if (itemRating < 0) {
                                                                        Text(
                                                                            text = "$itemRating",
                                                                            color = MaterialTheme.colorScheme.error,
                                                                            fontSize = 11.sp,
                                                                            fontWeight = FontWeight.Bold,
                                                                            modifier = Modifier.padding(end = 4.dp)
                                                                        )
                                                                    }
                                                                }
                                                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { 
                            viewModel.aiCanvasOutfit.value = null 
                            viewModel.canvasExcludedIds.clear()
                        },
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
                        onClick = {
                            viewModel.canvasExcludedIds.clear()
                            val ids = outfit.items.mapNotNull { it.id }
                            onWearClick(ids)
                        },
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
                        onClick = { 
                            // Mở hộp thoại chọn đồ, MẶC ĐỊNH LÀ TICK (GIỮ LẠI) TẤT CẢ
                            itemsToKeep.clear()
                            outfit.items.forEach { item -> item.id?.let { itemsToKeep.add(it) } }
                            showResetDialog = true
                        },
                        modifier = Modifier
                            .size(56.dp)
                            .background(MaterialTheme.colorScheme.secondaryContainer, CircleShape)
                    ) {
                        Icon(
                            Icons.Filled.Refresh,
                            null,
                            tint = MaterialTheme.colorScheme.secondary
                        )
                    }
                }
            } else {
                // Trạng thái rỗng: Cần gợi ý
                if (canvasError != null) {
                    Text(
                        text = canvasError,
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                }

                Button(
                    onClick = { viewModel.generateCanvasOutfit(weatherContext) },
                    modifier = Modifier.padding(vertical = 32.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Icon(Icons.Filled.AutoAwesome, null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Ask AI to Style Me Today", fontWeight = FontWeight.Bold)
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
    onFocus: () -> Unit = {},
    onImageSelect: () -> Unit = {}
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

        IconButton(
            onClick = onImageSelect,
            modifier = Modifier.padding(horizontal = 4.dp),
            enabled = !isTyping
        ) {
            Icon(
                Icons.Filled.PhotoCamera,
                contentDescription = "Style Match",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
        }

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
fun ChatBubble(
    message: ChatMessage,
    onOutfitSelect: (List<String>) -> Unit = {},
    onLogOotd: (List<String>) -> Unit = {},
    onItemFeedback: (String, Int) -> Unit = { _, _ -> }
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (message.isFromUser) Arrangement.End else Arrangement.Start
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(0.85f),
            horizontalAlignment = if (message.isFromUser) Alignment.End else Alignment.Start
        ) {
            val bubbleShape = RoundedCornerShape(
                topStart = 20.dp,
                topEnd = 20.dp,
                bottomStart = if (message.isFromUser) 20.dp else 4.dp,
                bottomEnd = if (message.isFromUser) 4.dp else 20.dp
            )
            Box(
                modifier = Modifier
                    .clip(bubbleShape)
                    .then(
                        if (message.isError) {
                            Modifier.background(MaterialTheme.colorScheme.errorContainer)
                        } else if (message.isFromUser) {
                            Modifier.background(
                                Brush.linearGradient(
                                    colors = listOf(
                                        MaterialTheme.colorScheme.primary,
                                        MaterialTheme.colorScheme.secondary
                                    )
                                )
                            )
                        } else {
                            Modifier
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                .border(
                                    1.dp,
                                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
                                    bubbleShape
                                )
                        }
                    )
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Column {
                    if (message.image != null) {
                        Image(
                            bitmap = message.image.asImageBitmap(),
                            contentDescription = "Uploaded Style",
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 250.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .padding(bottom = 8.dp),
                            contentScale = ContentScale.Fit
                        )
                    }

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

            // Show Buttons if outfit IDs are available
            if (!message.isFromUser && message.outfitIds != null && message.outfitIds.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Nút mặc thử trên Canvas
                    if (message.outfitIds.size >= 3) {
                        Button(
                            onClick = { onOutfitSelect(message.outfitIds) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Filled.Checkroom, null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSecondaryContainer)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Try on Canvas", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    // Nút lưu trực tiếp vào OOTD
                    Button(
                        onClick = { onLogOotd(message.outfitIds) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                            contentColor = MaterialTheme.colorScheme.onTertiaryContainer
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Filled.Favorite, null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Log OOTD", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
                
                // Feedback buttons below the outfit buttons
                if (message.outfitIds != null && message.outfitIds!!.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(message.outfitIds!!.size) { index ->
                            val itemId = message.outfitIds!![index]
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(8.dp))
                                    .padding(horizontal = 4.dp, vertical = 2.dp)
                            ) {
                                Text("#${index + 1}", fontSize = 10.sp, color = MaterialTheme.colorScheme.primary)
                                IconButton(onClick = { onItemFeedback(itemId, 1) }, modifier = Modifier.size(24.dp)) {
                                    Icon(Icons.Default.ThumbUp, null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.primary)
                                }
                                IconButton(onClick = { onItemFeedback(itemId, -1) }, modifier = Modifier.size(24.dp)) {
                                    Icon(Icons.Default.ThumbDown, null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.error)
                                }
                            }
                        }
                    }
                }
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

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun LogOotdConfirmDialog(
    showDialog: Boolean,
    onDismissRequest: () -> Unit,
    onConfirm: (occasions: List<String>, seasons: List<String>) -> Unit
) {
    if (!showDialog) return

    val selectedOccasions = remember { mutableStateListOf<String>() }
    val selectedSeasons = remember { mutableStateListOf<String>() }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = {
            onDismissRequest()
            selectedOccasions.clear()
            selectedSeasons.clear()
        },
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.background,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Log OOTD ✨",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 26.sp,
                        color = MaterialTheme.colorScheme.primary,
                        letterSpacing = (-0.5).sp
                    )
                    Text(
                        text = "Record your outfit of the day details.",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = false)
                    .verticalScroll(rememberScrollState())
            ) {
                // Occasion
                Text(
                    text = "OCCASION",
                    fontWeight = FontWeight.Black,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(12.dp))
                val occasionOptions = listOf("Work", "Casual", "Party", "Sport", "Travel", "Wedding", "Date", "Beach")
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    occasionOptions.forEach { sugg ->
                        val isSelected = selectedOccasions.contains(sugg)
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                .clickable {
                                    if (isSelected) selectedOccasions.remove(sugg)
                                    else selectedOccasions.add(sugg)
                                }
                                .padding(horizontal = 16.dp, vertical = 10.dp)
                        ) {
                            Text(
                                sugg, 
                                color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                fontSize = 13.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(28.dp))

                // Season
                Text(
                    text = "SEASON",
                    fontWeight = FontWeight.Black,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(12.dp))
                val seasonOptions = listOf("Spring", "Summer", "Autumn", "Winter")
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    seasonOptions.forEach { sugg ->
                        val isSelected = selectedSeasons.contains(sugg)
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                .clickable {
                                    if (isSelected) selectedSeasons.remove(sugg)
                                    else selectedSeasons.add(sugg)
                                }
                                .padding(horizontal = 16.dp, vertical = 10.dp)
                        ) {
                            Text(
                                sugg, 
                                color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                fontSize = 13.sp
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(40.dp))

            // Buttons
            Button(
                onClick = {
                    onConfirm(selectedOccasions.toList(), selectedSeasons.toList())
                    selectedOccasions.clear()
                    selectedSeasons.clear()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Text("Save OOTD", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}