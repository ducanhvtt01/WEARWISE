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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
                        apiKey = "AIzaSyCYi0mC2bYHbxy3y1Ynv1xZNfoB5bOmge8",
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
                    0 -> DashboardContent(userProfile)
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

        // --- HỘP THOẠI HIỂN THỊ KẾT QUẢ AI SCAN ---
        if (isAiScanning || aiScanResultText != null) {
            AlertDialog(
                onDismissRequest = {
                    if (!isAiScanning) {
                        aiScanResultText = null
                        rawScannedJson = null
                    }
                },
                containerColor = MaterialTheme.colorScheme.surface,
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {

                        Icon(
                            imageVector = Icons.Default.Psychology,
                            contentDescription = "AI"
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        Text(
                            text = if (isAiScanning) "AI is analyzing..." else "AI Stylist Result",
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                },
                text = {
                    if (isAiScanning) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Spacer(modifier = Modifier.padding(top = 20.dp))
                            // Lottie 1
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

                            // Lottie 2
                            val comp2 by rememberLottieComposition(LottieCompositionSpec.RawRes(R.raw.loading3))
                            val prog2 by animateLottieCompositionAsState(
                                comp2,
                                iterations = LottieConstants.IterateForever
                            )
                            if (comp2 != null) LottieAnimation(
                                comp2,
                                { prog2 },
                                modifier = Modifier.size(120.dp)
                            )
                        }
                    } else {
                        Text(
                            text = aiScanResultText ?: "No results found",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface,
                            lineHeight = 24.sp
                        )
                    }
                },
                confirmButton = {
                    if (!isAiScanning && rawScannedJson != null && scannedBitmap != null) {
                        Button(
                            onClick = {
                                val json = rawScannedJson!!
                                val currentUserId = supabase.auth.currentUserOrNull()?.id ?: ""

                                val seasonsArray = json.optJSONArray("seasons")
                                val seasonsList = List(seasonsArray?.length() ?: 0) {
                                    seasonsArray?.getString(it) ?: ""
                                }

                                val occasionsArray = json.optJSONArray("occasions")
                                val occasionsList = List(occasionsArray?.length() ?: 0) {
                                    occasionsArray?.getString(it) ?: ""
                                }

                                val itemName = json.optString("name", "Unknown Item")

                                val itemToSave = ClothingItem(
                                    userId = currentUserId,
                                    clothes_name = itemName,
                                    category = json.optString("category", "Other"),
                                    mainColor = json.optString("main_color", "Unknown"),
                                    seasons = seasonsList,
                                    occasions = occasionsList,
                                    imageUrl = ""
                                )

                                // Gọi hàm upload và lưu (ViewModel sẽ tự động update lại List cho ClosetScreen)
                                viewModel.uploadAndSaveClothes(scannedBitmap!!, itemToSave) {
                                    aiScanResultText = null
                                    rawScannedJson = null
                                    scannedBitmap = null
                                }
                            }
                        ) {
                            Text("Add to Closet")
                        }
                    }
                },
                dismissButton = {
                    if (!isAiScanning) {
                        TextButton(onClick = {
                            aiScanResultText = null
                            rawScannedJson = null
                        }) {
                            Text("Close", color = MaterialTheme.colorScheme.onSurfaceVariant)
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





