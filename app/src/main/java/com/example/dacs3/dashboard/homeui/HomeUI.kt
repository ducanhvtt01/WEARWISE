package com.example.dacs3.dashboard.homeui

import android.graphics.Bitmap
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.airbnb.lottie.compose.*
import com.example.dacs3.R
import com.example.dacs3.connectDB.DashboardViewModel
import com.example.dacs3.connectDB.Profile
import com.example.dacs3.connectDB.ClothingItem
import com.example.dacs3.connectDB.supabase
import com.example.dacs3.dashboard.ClosetScreen
import com.example.dacs3.dashboard.ProfileScreen
import com.example.dacs3.dashboard.StylistScreen
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import com.google.ai.client.generativeai.type.generationConfig
import io.github.jan.supabase.gotrue.auth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.util.Calendar
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeUI(
    isDarkMode: Boolean = false,
    onThemeChange: (Boolean) -> Unit = {},
    onLogoutSuccess: () -> Unit,
    viewModel: DashboardViewModel = viewModel()
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }

    val scope = rememberCoroutineScope()
    var isAiScanning by remember { mutableStateOf(false) }
    var aiScanResultText by remember { mutableStateOf<String?>(null) }
    var rawScannedJson by remember { mutableStateOf<JSONObject?>(null) }
    var scannedBitmap by remember { mutableStateOf<Bitmap?>(null) }

    var isCheckingDupe by remember { mutableStateOf(false) } // State khi đang hỏi "Có nên mua không"
    var antiImpulseAdvice by remember { mutableStateOf<String?>(null) } // Lưu lời khuyên của AI
    val closetItems by viewModel.clothingItems.collectAsState() // Lấy dữ liệu tủ đồ hiện tại

    val userId = supabase.auth.currentUserOrNull()?.id ?: ""
    LaunchedEffect(userId) {
        if (userId.isNotEmpty()) {
            viewModel.getProfile(userId)
            viewModel.getClothingItems(userId) // Gọi load dữ liệu tủ đồ lúc vào app
        }
    }

    val userProfile = viewModel.userProfile
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap ->
        if (bitmap != null) {
            scannedBitmap = bitmap
            isAiScanning = true
            scope.launch(Dispatchers.IO) {
                try {
                    val generativeModel = GenerativeModel(
                        modelName = "gemini-2.5-flash",
                        apiKey = com.example.dacs3.BuildConfig.GEMINI_API_KEY,
                        generationConfig = generationConfig {
                            temperature = 0.2f
                            responseMimeType = "application/json"
                        }
                    )

                    val prompt = """
                        Analyze this clothing item in the image. 
                        Return strictly valid JSON with the following schema:
                        {
                            "name": "Brief brand and item name (e.g. Uniqlo Blue Shirt)",
                            "category": "Choose one: Top, Bottom, Shoes, Outerwear, Accessories",
                            "main_color": "Dominant color (e.g. Blue, Black, White)",
                            "seasons": ["Spring", "Summer", "Autumn", "Winter"],
                            "occasions": ["Casual", "Work", "Party", "Sport", "Formal"]
                        }
                    """.trimIndent()

                    val response = generativeModel.generateContent(
                        content {
                            image(bitmap)
                            text(prompt)
                        }
                    )

                    val jsonString = response.text ?: "{}"
                    val json = JSONObject(jsonString)

                    rawScannedJson = json

                    val itemName = json.optString("name", "Unknown Item")
                    val itemCategory = json.optString("category", "N/A")
                    val itemColor = json.optString("main_color", "N/A")
                    val occasionsArray = json.optJSONArray("occasions")
                    val itemOccasions = if (occasionsArray != null && occasionsArray.length() > 0) {
                        List(occasionsArray.length()) { occasionsArray.getString(it) }.joinToString(
                            ", "
                        )
                    } else {
                        "N/A"
                    }

                    val seasonsArray = json.optJSONArray("seasons")
                    val itemSeasons = if (seasonsArray != null && seasonsArray.length() > 0) {
                        List(seasonsArray.length()) { seasonsArray.getString(it) }.joinToString(", ")
                    } else {
                        "N/A"
                    }

                    aiScanResultText =
                        "$itemName\n• Category: $itemCategory\n• Color: $itemColor\n• Seasons: $itemSeasons\n• Occasions: $itemOccasions"

                } catch (e: Exception) {
                    aiScanResultText = "AI Scan Error: ${e.localizedMessage}"
                    rawScannedJson = null
                } finally {
                    isAiScanning = false
                }
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            bottomBar = {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 8.dp,
                    modifier = Modifier
                        .height(84.dp)
                        .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                ) {
                    val items = listOf("Home", "Closet", "Stylist", "Profile")
                    val icons = listOf(
                        Icons.Outlined.Home,
                        Icons.Outlined.Checkroom,
                        Icons.Outlined.AutoAwesome,
                        Icons.Outlined.Person
                    )
                    val selectedIcons = listOf(
                        Icons.Filled.Home,
                        Icons.Filled.Checkroom,
                        Icons.Filled.AutoAwesome,
                        Icons.Filled.Person
                    )

                    items.forEachIndexed { index, item ->
                        NavigationBarItem(
                            selected = selectedTab == index,
                            onClick = { selectedTab = index },
                            icon = {
                                Icon(
                                    if (selectedTab == index) selectedIcons[index] else icons[index],
                                    contentDescription = item,
                                    modifier = Modifier.size(if (selectedTab == index) 28.dp else 24.dp)
                                )
                            },
                            label = {
                                Text(
                                    item,
                                    fontSize = 12.sp,
                                    fontWeight = if (selectedTab == index) FontWeight.SemiBold else FontWeight.Normal
                                )
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.primary,
                                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                selectedTextColor = MaterialTheme.colorScheme.primary,
                                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                indicatorColor = MaterialTheme.colorScheme.secondaryContainer
                            )
                        )
                    }
                }
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                when (selectedTab) {
                    0 -> DashboardContent(userProfile, closetItems)
                    1 -> ClosetScreen(viewModel = viewModel) // [SỬA ĐỔI] TRUYỀN VIEWMODEL VÀO CLOSET SCREEN
                    2 -> StylistScreen()
                    3 -> ProfileScreen(
                        isDarkMode = isDarkMode,
                        onThemeChange = onThemeChange,
                        onLogoutSuccess = onLogoutSuccess,
                        viewModel = viewModel,
                        userProfile = userProfile
                    )
                }
            }
        }

        // --- HỘP THOẠI HIỂN THỊ KẾT QUẢ AI SCAN & CHỐNG MUA SẮM BỐC ĐỒNG ---
        if (isAiScanning || aiScanResultText != null) {
            AlertDialog(
                onDismissRequest = {
                    if (!isAiScanning && !isCheckingDupe) {
                        aiScanResultText = null
                        rawScannedJson = null
                        antiImpulseAdvice = null // Reset lời khuyên
                    }
                },
                containerColor = MaterialTheme.colorScheme.surface,
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Psychology, contentDescription = "AI")
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isAiScanning) "AI is analyzing..."
                            else if (isCheckingDupe) "Stylist is thinking..."
                            else "AI Stylist Result",
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                },
                text = {
                    if (isAiScanning || isCheckingDupe) {
                        // Hiển thị Lottie loading khi đang quét hoặc đang hỏi ý kiến
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Spacer(modifier = Modifier.padding(top = 20.dp))
                            val comp1 by rememberLottieComposition(LottieCompositionSpec.RawRes(R.raw.ai_loading))
                            val prog1 by animateLottieCompositionAsState(
                                comp1,
                                iterations = LottieConstants.IterateForever
                            )
                            if (comp1 != null) LottieAnimation(
                                comp1,
                                { prog1 },
                                modifier = Modifier.size(120.dp)
                            )
                        }
                    } else {
                        // Hiển thị thông tin món đồ
                        Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                            Text(
                                text = aiScanResultText ?: "No results found",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface
                            )

                            // NẾU CÓ LỜI KHUYÊN TỪ AI THÌ HIỂN THỊ Ở ĐÂY (Màu cam cảnh báo)
                            if (antiImpulseAdvice != null) {
                                Spacer(modifier = Modifier.height(16.dp))
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Column(modifier = Modifier.padding(16.dp)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                Icons.Filled.Warning,
                                                null,
                                                tint = MaterialTheme.colorScheme.secondary
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                "Anti-Impulse Advice",
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.secondary
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            text = antiImpulseAdvice!!,
                                            fontSize = 14.sp,
                                            color = MaterialTheme.colorScheme.onSecondaryContainer
                                        )
                                    }
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    if (!isAiScanning && !isCheckingDupe) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            //NÚT "SHOULD I BUY THIS?" (NẰM TRÊN CÙNG)
                            if (rawScannedJson != null && scannedBitmap != null && antiImpulseAdvice == null) {
                                Button(
                                    onClick = {
                                        isCheckingDupe = true
                                        scope.launch(Dispatchers.IO) {
                                            try {
                                                val generativeModel = GenerativeModel(
                                                    modelName = "gemini-2.5-flash",
                                                    apiKey = com.example.dacs3.BuildConfig.GEMINI_API_KEY
                                                )

                                                // Gom dữ liệu tủ đồ hiện tại thành chuỗi Text
                                                val closetSummary =
                                                    closetItems.joinToString(separator = "; ") {
                                                        "${it.clothes_name} (${it.mainColor}, ${it.category})"
                                                    }

                                                val prompt = """
                                                    You are an elite fashion director and high-end personal stylist. I need your expert eye to curate my wardrobe.
                                                    Here is my current closet inventory: $closetSummary.
                                                    Analyze the clothing item in the image I'm about to buy.
                                                    Give me a highly fashionable, magazine-style critique strictly formatted in 3 bullet points:
                                                
                                                    1. Aesthetic & Vibe: What is the core style profile of this item (e.g., Old Money, Gorpcore, Minimalist, Y2K, elevated Smart Casual)? Is it a timeless capsule staple or a fleeting trend? Does the texture and silhouette look expensive and versatile?
                                                    2. Outfit Alchemy: Act like a stylist on a photoshoot. Create 2 specific, runway-ready outfit formulas combining this new item with the exact pieces I ALREADY own in my closet. Talk about color blocking, layering, or proportions. (If it completely clashes with my current wardrobe's color palette, call it out!).
                                                    3. The Stylist's Verdict: Choose strictly one of the following:
                                                       - 💎 BUY: It's a chic investment piece that significantly elevates your rotation.
                                                       - ⏳ THINK TWICE: It's a nice statement piece, but only buy it if it truly fits your core personal style. 
                                                       - 🚫 SKIP: Fashion faux pas, it disrupts your closet's harmony, or you already own a piece that does the exact same job better.
                                                
                                                    Keep your tone chic, brutally honest, and deeply fashionable. Be concise.
                                                """.trimIndent()

                                                val response = generativeModel.generateContent(
                                                    content {
                                                        image(scannedBitmap!!)
                                                        text(prompt)
                                                    }
                                                )
                                                antiImpulseAdvice = response.text
                                            } catch (e: Exception) {
                                                antiImpulseAdvice =
                                                    "Error analyzing closet. Better save your money just in case!"
                                            } finally {
                                                isCheckingDupe = false
                                            }
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                                    modifier = Modifier
                                        .fillMaxWidth() // Kéo dãn nút ra cho bự và đẹp
                                        .padding(bottom = 12.dp)
                                        .height(48.dp)
                                ) {
                                    Icon(
                                        Icons.Outlined.AutoAwesome,
                                        null,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        "Should I buy this?",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp
                                    )
                                }
                            }
                            // NÚT "CLOSE" VÀ "ADD TO CLOSET" NẰM DƯỚI
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // --- NÚT CLOSE (BÊN TRÁI) ---
                                TextButton(
                                    onClick = {
                                        aiScanResultText = null
                                        rawScannedJson = null
                                        antiImpulseAdvice = null
                                    }
                                ) {
                                    Text(
                                        "Close",
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                // --- NÚT ADD TO CLOSET (BÊN PHẢI) ---
                                if (rawScannedJson != null && scannedBitmap != null) {
                                    Button(
                                        onClick = {
                                            val json = rawScannedJson!!
                                            val currentUserId =
                                                supabase.auth.currentUserOrNull()?.id ?: ""

                                            val itemToSave = ClothingItem(
                                                userId = currentUserId,
                                                clothes_name = json.optString(
                                                    "name",
                                                    "Unknown Item"
                                                ),
                                                category = json.optString("category", "Other"),
                                                mainColor = json.optString("main_color", "Unknown"),
                                                seasons = List(
                                                    json.optJSONArray("seasons")?.length() ?: 0
                                                ) {
                                                    json.optJSONArray("seasons")?.getString(it)
                                                        ?: ""
                                                },
                                                occasions = List(
                                                    json.optJSONArray("occasions")?.length() ?: 0
                                                ) {
                                                    json.optJSONArray("occasions")?.getString(it)
                                                        ?: ""
                                                },
                                                imageUrl = ""
                                            )

                                            viewModel.uploadAndSaveClothes(
                                                scannedBitmap!!,
                                                itemToSave
                                            ) {
                                                aiScanResultText = null
                                                rawScannedJson = null
                                                scannedBitmap = null
                                                antiImpulseAdvice = null
                                            }
                                        }
                                    ) {
                                        Text(if (antiImpulseAdvice == null) "Add to Closet" else "Buy & Add to Closet")
                                    }
                                }
                            }
                        }
                    }
                }
            )
        }

        // CHỈ HIỂN THỊ NÚT CAMERA KHI ĐANG Ở TAB CLOSET (Tab số 1)
        if (selectedTab == 1) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(bottom = 110.dp, end = 16.dp)
                    .offset { IntOffset(offsetX.roundToInt(), offsetY.roundToInt()) }
                    .shadow(8.dp, CircleShape)
                    .background(
                        brush = Brush.horizontalGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.secondary,
                                MaterialTheme.colorScheme.primary
                            )
                        ), shape = CircleShape
                    )
                    .clip(CircleShape)
                    .clickable { cameraLauncher.launch(null) }
                    .pointerInput(Unit) {
                        detectDragGestures { change, dragAmount ->
                            change.consume()
                            offsetX += dragAmount.x
                            offsetY += dragAmount.y
                        }
                    }
                    .padding(horizontal = 20.dp, vertical = 16.dp)
            ) {
                Icon(
                    Icons.Filled.CameraAlt,
                    "AI Scan",
                    tint = if (isDarkMode) Color.DarkGray else Color.White
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "AI Scan",
                    fontWeight = FontWeight.Bold,
                    color = if (isDarkMode) Color.DarkGray else Color.White
                )
            }
        }
    }
}





