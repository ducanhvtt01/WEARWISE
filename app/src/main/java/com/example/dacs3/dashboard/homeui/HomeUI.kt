package com.example.dacs3.dashboard.homeui

import android.Manifest
import android.provider.MediaStore
import android.widget.Toast
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext

import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.os.Build
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.graphics.graphicsLayer
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
import dev.jeziellago.compose.markdowntext.MarkdownText
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import com.google.ai.client.generativeai.type.generationConfig
import io.github.jan.supabase.gotrue.auth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import org.json.JSONObject
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.segmentation.subject.SubjectSegmentation
import com.google.mlkit.vision.segmentation.subject.SubjectSegmenterOptions
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import java.util.Calendar
import kotlin.math.roundToInt

data class ScannedItemState(
    val bitmap: Bitmap,
    val json: JSONObject,
    val displayText: String,
    val isSelected: Boolean = true,
    val antiImpulseAdvice: String? = null,
    val isAdviceLoading: Boolean = false,
    val priceInput: String = ""
)

@OptIn(ExperimentalMaterial3Api::class)
// ==========================================
// MÀN HÌNH CHÍNH (HOMEUI)
// - Quản lý cấu trúc giao diện chính bao gồm Drawer, Bottom Navigation, định vị và quét ảnh AI.
// ==========================================
@Composable
fun HomeUI(
    isDarkMode: Boolean = false,
    onThemeChange: (Boolean) -> Unit = {},
    onLogoutSuccess: () -> Unit,
    onOpenSeasonStores: (String) -> Unit = {},
    onNavigateToTodo: () -> Unit = {},
    onNavigateToCalendar: () -> Unit = {},
    onNavigateToLaundry: () -> Unit = {},
    onNavigateToScheduler: () -> Unit = {},
    onNavigateToInsights: () -> Unit = {},
    onNavigateToSwiper: () -> Unit = {},
    onNavigateToStyleStudio: () -> Unit = {},
    viewModel: DashboardViewModel = viewModel()
) {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val selectedTab = viewModel.activeTab
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }

    val scope = rememberCoroutineScope()
    val uriHandler = LocalUriHandler.current
    val hapticFeedback = LocalHapticFeedback.current
    var isAiScanning by remember { mutableStateOf(false) }
    var aiScanResultText by remember { mutableStateOf<String?>(null) }
    var rawScannedJson by remember { mutableStateOf<JSONObject?>(null) }
    var scannedBitmap by remember { mutableStateOf<Bitmap?>(null) }
    
    // Trạng thái quét hàng loạt (Batch Scan States)
    var scannedItemsBatch by remember { mutableStateOf<List<ScannedItemState>>(emptyList()) }
    var isBatchMode by remember { mutableStateOf(false) }
    var batchScanProgress by remember { mutableStateOf(0) }
    var batchTotal by remember { mutableStateOf(0) }

    var isCheckingDupe by remember { mutableStateOf(false) } // State khi đang hỏi "Có nên mua không"
    var antiImpulseAdvice by remember { mutableStateOf<String?>(null) } // Lưu lời khuyên của AI
    var showAdvicePager by remember { mutableStateOf(false) } // Hiển thị Pager lời khuyên
    val closetItems by viewModel.clothingItems.collectAsState() // Lấy dữ liệu tủ đồ hiện tại

    val userId = supabase.auth.currentUserOrNull()?.id ?: ""
    // Tự động tải thông tin hồ sơ và danh sách tủ đồ khi nhận được mã người dùng (userId)
    LaunchedEffect(userId) {
        if (userId.isNotEmpty()) {
            viewModel.getProfile(userId)
            viewModel.getClothingItems(userId) // Gọi load dữ liệu tủ đồ lúc vào app
        }
    }

    val userProfile = viewModel.userProfile
    val context = LocalContext.current

    var showScanSourceSheet by remember { mutableStateOf(false) }

    // Trình khởi chạy bộ chọn ảnh từ thư viện để gửi lên AI phân tích trang phục
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris ->
        if (uris.isNotEmpty()) {
            isBatchMode = true
            isAiScanning = true
            batchTotal = uris.size
            batchScanProgress = 0
            scannedItemsBatch = emptyList()

            scope.launch(Dispatchers.IO) {
                val results = mutableListOf<ScannedItemState>()
                val generativeModel = GenerativeModel(
                    modelName = "gemini-3.1-flash-lite",
                    apiKey = com.example.dacs3.BuildConfig.GEMINI_API_KEY,
                    generationConfig = generationConfig {
                        temperature = 0.2f
                        responseMimeType = "application/json"
                    }
                )

                for (uri in uris) {
                    try {
                        var bitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                            ImageDecoder.decodeBitmap(ImageDecoder.createSource(context.contentResolver, uri)) { decoder, _, _ ->
                                decoder.isMutableRequired = true
                                decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
                            }
                        } else {
                            @Suppress("DEPRECATION")
                            MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
                        }

                        if (bitmap != null) {
                            // Resize bitmap to max 1024px to prevent OutOfMemory and white screen crashes
                            val maxSize = 1024
                            if (bitmap.width > maxSize || bitmap.height > maxSize) {
                                val ratio = bitmap.width.toFloat() / bitmap.height.toFloat()
                                val newWidth = if (ratio > 1) maxSize else (maxSize * ratio).toInt()
                                val newHeight = if (ratio > 1) (maxSize / ratio).toInt() else maxSize
                                val scaledBitmap = Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
                                if (bitmap != scaledBitmap) {
                                    bitmap.recycle()
                                    bitmap = scaledBitmap
                                }
                            }

                            val prompt = """
                                You are a high-end fashion AI analyzer. Inspect the clothing item in this image with extreme care and absolute precision.
                                
                                CRITICAL INSTRUCTIONS FOR ACCURACY:
                                1. "name": Construct a descriptive, premium, and highly accurate name for the item (e.g. "Sleek Charcoal Crewneck Sweater", "Classic Olive Cargo Pants", "Minimalist White Leather Sneakers"). 
                                   - WARNING: DO NOT hallucinate brand names (like "Uniqlo", "Zara", "Nike") unless a brand logo is clearly visible in the image. If no logo is visible, use a pure, premium descriptive fashion name.
                                2. "category": You must choose strictly from these exact standard categories:
                                   - "Top" (for shirts, t-shirts, blouses, sweaters, hoodies)
                                   - "Bottom" (for pants, jeans, shorts, skirts)
                                   - "Shoes" (for sneakers, boots, formal shoes, sandals)
                                   - "Outerwear" (for jackets, heavy coats, cardigans, blazers)
                                   - "Accessories" (for hats, bags, belts, sunglasses)
                                3. "main_color": Identify the dominant base color with high accuracy (choose simple, standard, elegant names like: Black, White, Grey, Navy, Blue, Beige, Brown, Olive, Red, Pink, Yellow, Green, Purple). If the item has a pattern, select the primary background color.
                                4. "seasons": Select the most appropriate seasons (choose from: "Spring", "Summer", "Autumn", "Winter"). Be logical: heavy coats belong to Winter/Autumn; shorts and tank tops belong to Summer/Spring; versatile tees belong to all.
                                5. "occasions": Select logical occasions (choose from: "Casual", "Work", "Party", "Sport", "Formal") that perfectly fit the item's design style.

                                Return strictly valid JSON with this schema:
                                {
                                    "name": "Descriptive, brand-accurate item name",
                                    "category": "Top / Bottom / Shoes / Outerwear / Accessories",
                                    "main_color": "Highly accurate base color",
                                    "seasons": ["Season1", "Season2"],
                                    "occasions": ["Occasion1", "Occasion2"]
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

                            val itemName = json.optString("name", "Unknown Item")
                            val itemCategory = json.optString("category", "N/A")
                            val itemColor = json.optString("main_color", "N/A")
                            val occasionsArray = json.optJSONArray("occasions")
                            val itemOccasions = if (occasionsArray != null && occasionsArray.length() > 0) {
                                List(occasionsArray.length()) { occasionsArray.getString(it) }.joinToString(", ")
                            } else {
                                "N/A"
                            }
                            val seasonsArray = json.optJSONArray("seasons")
                            val itemSeasons = if (seasonsArray != null && seasonsArray.length() > 0) {
                                List(seasonsArray.length()) { seasonsArray.getString(it) }.joinToString(", ")
                            } else {
                                "N/A"
                            }
                            val displayText = "$itemName\n• Category: $itemCategory\n• Color: $itemColor\n• Seasons: $itemSeasons\n• Occasions: $itemOccasions"

                            results.add(ScannedItemState(bitmap, json, displayText))
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    } finally {
                        batchScanProgress++
                    }
                }
                scannedItemsBatch = results
                isAiScanning = false
            }
        }
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap ->
        if (bitmap != null) {
            isBatchMode = true
            isAiScanning = true
            if (scannedItemsBatch.isEmpty()) {
                batchTotal = 1
                batchScanProgress = 0
            } else {
                batchTotal += 1
            }
            scope.launch(Dispatchers.IO) {
                try {
                    // Resize bitmap to max 1024px to prevent OutOfMemory and white screen crashes
                    var cleanBitmap = bitmap
                    val maxSize = 1024
                    if (cleanBitmap.width > maxSize || cleanBitmap.height > maxSize) {
                        val ratio = cleanBitmap.width.toFloat() / cleanBitmap.height.toFloat()
                        val newWidth = if (ratio > 1) maxSize else (maxSize * ratio).toInt()
                        val newHeight = if (ratio > 1) (maxSize / ratio).toInt() else maxSize
                        val scaledBitmap = Bitmap.createScaledBitmap(cleanBitmap, newWidth, newHeight, true)
                        if (cleanBitmap != scaledBitmap) {
                            cleanBitmap = scaledBitmap
                        }
                    }

                    val generativeModel = GenerativeModel(
                        modelName = "gemini-3.1-flash-lite",
                        apiKey = com.example.dacs3.BuildConfig.GEMINI_API_KEY,
                        generationConfig = generationConfig {
                            temperature = 0.2f
                            responseMimeType = "application/json"
                        }
                    )

                    val prompt = """
                        You are a high-end fashion AI analyzer. Inspect the clothing item in this image with extreme care and absolute precision.
                        
                        CRITICAL INSTRUCTIONS FOR ACCURACY:
                        1. "name": Construct a descriptive, premium, and highly accurate name for the item (e.g. "Sleek Charcoal Crewneck Sweater", "Classic Olive Cargo Pants", "Minimalist White Leather Sneakers"). 
                           - WARNING: DO NOT hallucinate brand names (like "Uniqlo", "Zara", "Nike") unless a brand logo is clearly visible in the image. If no logo is visible, use a pure, premium descriptive fashion name.
                         2. "category": You must choose strictly from these exact standard categories:
                           - "Top" (for shirts, t-shirts, blouses, sweaters, hoodies)
                           - "Bottom" (for pants, jeans, shorts, skirts)
                           - "Shoes" (for sneakers, boots, formal shoes, sandals)
                           - "Outerwear" (for jackets, heavy coats, cardigans, blazers)
                           - "Accessories" (for hats, bags, belts, sunglasses)
                        3. "main_color": Identify the dominant base color with high accuracy (choose simple, standard, elegant names like: Black, White, Grey, Navy, Blue, Beige, Brown, Olive, Red, Pink, Yellow, Green, Purple). If the item has a pattern, select the primary background color.
                        4. "seasons": Select the most appropriate seasons (choose from: "Spring", "Summer", "Autumn", "Winter"). Be logical: heavy coats belong to Winter/Autumn; shorts and tank tops belong to Summer/Spring; versatile tees belong to all.
                        5. "occasions": Select logical occasions (choose from: "Casual", "Work", "Party", "Sport", "Formal") that perfectly fit the item's design style.

                        Return strictly valid JSON with this schema:
                        {
                            "name": "Descriptive, brand-accurate item name",
                            "category": "Top / Bottom / Shoes / Outerwear / Accessories",
                            "main_color": "Highly accurate base color",
                            "seasons": ["Season1", "Season2"],
                            "occasions": ["Occasion1", "Occasion2"]
                        }
                    """.trimIndent()

                    val response = generativeModel.generateContent(
                        content {
                            image(cleanBitmap)
                            text(prompt)
                        }
                    )

                    val jsonString = response.text ?: "{}"
                    val json = JSONObject(jsonString)

                    val itemName = json.optString("name", "Unknown Item")
                    val itemCategory = json.optString("category", "N/A")
                    val itemColor = json.optString("main_color", "N/A")
                    val occasionsArray = json.optJSONArray("occasions")
                    val itemOccasions = if (occasionsArray != null && occasionsArray.length() > 0) {
                        List(occasionsArray.length()) { occasionsArray.getString(it) }.joinToString(", ")
                    } else {
                        "N/A"
                    }

                    val seasonsArray = json.optJSONArray("seasons")
                    val itemSeasons = if (seasonsArray != null && seasonsArray.length() > 0) {
                        List(seasonsArray.length()) { seasonsArray.getString(it) }.joinToString(", ")
                    } else {
                        "N/A"
                    }

                    val displayText = "$itemName\n• Category: $itemCategory\n• Color: $itemColor\n• Seasons: $itemSeasons\n• Occasions: $itemOccasions"

                    val newItem = ScannedItemState(cleanBitmap, json, displayText)
                    scannedItemsBatch = scannedItemsBatch + newItem

                } catch (e: Exception) {
                    e.printStackTrace()
                } finally {
                    batchScanProgress++
                    isAiScanning = false
                }
            }
        }
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            try {
                cameraLauncher.launch(null)
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(
                    context,
                    "Cannot open camera: ${e.localizedMessage}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        } else {
            Toast.makeText(
                context,
                "Camera permission is required for AI Scan",
                Toast.LENGTH_SHORT
            ).show()
        }
    }


    // ==========================================
    // CỤM 3: KHUNG ĐIỀU HƯỚNG TRƯỢT (NAVIGATION DRAWER)
    // - Menu trượt từ cạnh bên chứa các lối tắt đến Calendar, Laundry và To-Do List.
    // ==========================================
    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = drawerState.isOpen,
        drawerContent = {
            ModalDrawerSheet(
                drawerContainerColor = MaterialTheme.colorScheme.background,
                modifier = Modifier.width(300.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp)
                ) {
                    Text(
                        text = "WEARWISE",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    Text(
                        text = supabase.auth.currentUserOrNull()?.email ?: "Smart Wardrobe Assistant",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 32.dp)
                    )

                    HorizontalDivider(modifier = Modifier.padding(bottom = 24.dp), color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))

                    // Lối tắt đi đến giao diện Nhật ký & Lịch trình
                    NavigationDrawerItem(
                        label = { Text("🗓️  Calendar & Outfit Diary", fontWeight = FontWeight.Bold) },
                        selected = false,
                        onClick = {
                            scope.launch { drawerState.close() }
                            onNavigateToCalendar()
                        },
                        colors = NavigationDrawerItemDefaults.colors(unselectedContainerColor = Color.Transparent),
                        modifier = Modifier.padding(vertical = 4.dp)
                    )

                    // Lối tắt đi đến giao diện Theo dõi giặt là
                    NavigationDrawerItem(
                        label = { Text("🧺  Laundry Tracker", fontWeight = FontWeight.Bold) },
                        selected = false,
                        onClick = {
                            scope.launch { drawerState.close() }
                            onNavigateToLaundry()
                        },
                        colors = NavigationDrawerItemDefaults.colors(unselectedContainerColor = Color.Transparent),
                        modifier = Modifier.padding(vertical = 4.dp)
                    )

                    // Lối tắt đi đến giao diện Danh sách việc cần làm
                    NavigationDrawerItem(
                        label = { Text("📝  Wardrobe To-Do List", fontWeight = FontWeight.Bold) },
                        selected = false,
                        onClick = {
                            scope.launch { drawerState.close() }
                            onNavigateToTodo()
                        },
                        colors = NavigationDrawerItemDefaults.colors(unselectedContainerColor = Color.Transparent),
                        modifier = Modifier.padding(vertical = 4.dp)
                    )

                    Spacer(modifier = Modifier.weight(1f))

                    Text(
                        text = "WearWise v1.0.0",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Scaffold(
                containerColor = MaterialTheme.colorScheme.background,
            bottomBar = {
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

                // ==========================================
                // CỤM 4: THANH ĐIỀU HƯỚNG DƯỚI CÙNG (FLOATING GLASSY NAVIGATION BAR)
                // - Thanh Bottom Navigation phong cách kính mờ (Glassmorphism) chứa 4 Tab chính.
                // ==========================================
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(68.dp)
                            .shadow(
                                elevation = 24.dp,
                                shape = RoundedCornerShape(34.dp),
                                ambientColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                spotColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                            )
                            .clip(RoundedCornerShape(34.dp))
                            .background(
                                brush = Brush.verticalGradient(
                                    colors = listOf(
                                        MaterialTheme.colorScheme.surface.copy(alpha = 0.97f),
                                        MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
                                    )
                                )
                            )
                            .border(
                                width = 1.dp,
                                brush = Brush.verticalGradient(
                                    colors = listOf(
                                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f),
                                        Color.Transparent
                                    )
                                ),
                                shape = RoundedCornerShape(34.dp)
                            )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 8.dp),
                            horizontalArrangement = Arrangement.SpaceAround,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            items.forEachIndexed { index, label ->
                                val isSelected = selectedTab == index
                                val animatedScale by animateFloatAsState(
                                    targetValue = if (isSelected) 1.1f else 1.0f,
                                    animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                                    label = "navScale$index"
                                )

                                Column(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable(
                                            indication = null,
                                            interactionSource = remember { MutableInteractionSource() }
                                        ) {
                                            viewModel.activeTab = index
                                            hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                                        }
                                        .padding(vertical = 8.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    // Glowing pill indicator for selected item
                                    Box(
                                        contentAlignment = Alignment.Center,
                                        modifier = Modifier
                                            .size(width = 48.dp, height = 32.dp)
                                            .clip(RoundedCornerShape(16.dp))
                                            .background(
                                                if (isSelected)
                                                    Brush.horizontalGradient(
                                                        listOf(
                                                            MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                                                            MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f)
                                                        )
                                                    )
                                                else
                                                    Brush.horizontalGradient(listOf(Color.Transparent, Color.Transparent))
                                            )
                                    ) {
                                        Icon(
                                            imageVector = if (isSelected) selectedIcons[index] else icons[index],
                                            contentDescription = label,
                                            modifier = Modifier
                                                .size(if (isSelected) 24.dp else 22.dp)
                                                .graphicsLayer(scaleX = animatedScale, scaleY = animatedScale),
                                            tint = if (isSelected)
                                                MaterialTheme.colorScheme.primary
                                            else
                                                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                        )
                                    }
                                    Spacer(Modifier.height(2.dp))
                                    Text(
                                        text = label,
                                        fontSize = 10.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSelected)
                                            MaterialTheme.colorScheme.primary
                                        else
                                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                // Điều hướng hiển thị màn hình tương ứng với tab được chọn
                when (selectedTab) {
                    0 -> {
                        val topFavoriteClothes by viewModel.topFavoriteClothes.collectAsState()
                        LaunchedEffect(userId) {
                            if (userId.isNotEmpty()) {
                                viewModel.fetchTopFavoriteClothes(userId)
                                viewModel.fetchPackingLists(userId)
                            }
                        }
                        DashboardContent(
                            currentProfile = userProfile,
                            closetItems = closetItems,
                            topFavoriteClothes = topFavoriteClothes,
                            onLogOotd = { ids, occasions, season ->
                                viewModel.logOotd(
                                    userId = userId,
                                    clothingIds = ids,
                                    occasions = occasions,
                                    season = season,
                                    onSuccess = {}
                                )
                            },
                            onNavigateToStylist = { viewModel.activeTab = 2 },
                            onNavigateToSeasonStores = { season ->
                                onOpenSeasonStores(season)
                            },
                            onMenuClick = {
                                scope.launch {
                                    if (drawerState.isClosed) drawerState.open()
                                }
                            },
                            onNavigateToTodo = onNavigateToTodo,
                            onNavigateToCalendar = onNavigateToCalendar,
                            onNavigateToLaundry = onNavigateToLaundry,
                            dashboardViewModel = viewModel
                        )
                    }

                    1 -> ClosetScreen(
                        viewModel = viewModel,
                        onNavigateToStylist = { viewModel.activeTab = 2 },
                        onNavigateToScheduler = onNavigateToScheduler,
                        onNavigateToInsights = onNavigateToInsights,
                        onNavigateToStyleStudio = onNavigateToStyleStudio
                    )
                    2 -> StylistScreen(
                        dashboardViewModel = viewModel,
                        onNavigateToSwiper = onNavigateToSwiper
                    )
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
        if (isAiScanning || aiScanResultText != null || (isBatchMode && scannedItemsBatch.isNotEmpty())) {
            AlertDialog(
                onDismissRequest = {
                    if (!isAiScanning && !isCheckingDupe) {
                        aiScanResultText = null
                        rawScannedJson = null
                        antiImpulseAdvice = null // Reset lời khuyên
                        showAdvicePager = false
                        isBatchMode = false
                        scannedItemsBatch = emptyList()
                        batchTotal = 0
                        batchScanProgress = 0
                    }
                },
                containerColor = MaterialTheme.colorScheme.surface,
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Psychology, contentDescription = "AI")
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isAiScanning) {
                                if (isBatchMode && batchTotal > 1) "AI is analyzing... ($batchScanProgress/$batchTotal)"
                                else "AI is analyzing..."
                            }
                            else if (isCheckingDupe && !isBatchMode) "Stylist is thinking..."
                            else if (showAdvicePager) "Individual Stylist Advice"
                            else if (isBatchMode) "Batch AI Stylist Result"
                            else "AI Stylist Result",
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                },
                text = {
                    if (isAiScanning || (!isBatchMode && isCheckingDupe)) {
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
                    } else if (showAdvicePager) {
                        val selectedItems = scannedItemsBatch.filter { it.isSelected }
                        val pagerState = rememberPagerState(pageCount = { selectedItems.size })
                        Column(
                            modifier = Modifier.fillMaxWidth().heightIn(max = 500.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            HorizontalPager(
                                state = pagerState,
                                modifier = Modifier.weight(1f)
                            ) { page ->
                                val item = selectedItems[page]
                                Column(
                                    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp).verticalScroll(rememberScrollState()),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    androidx.compose.foundation.Image(
                                        bitmap = item.bitmap.asImageBitmap(),
                                        contentDescription = null,
                                        modifier = Modifier.size(150.dp).clip(RoundedCornerShape(12.dp)),
                                        contentScale = ContentScale.Crop
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text(
                                        text = item.displayText,
                                        fontSize = 14.sp,
                                        lineHeight = 18.sp,
                                        textAlign = TextAlign.Center
                                    )
                                    Spacer(modifier = Modifier.height(16.dp))

                                    if (item.isAdviceLoading) {
                                        val comp1 by rememberLottieComposition(LottieCompositionSpec.RawRes(R.raw.ai_loading))
                                        val prog1 by animateLottieCompositionAsState(comp1, iterations = LottieConstants.IterateForever)
                                        if (comp1 != null) LottieAnimation(comp1, { prog1 }, modifier = Modifier.size(100.dp))
                                    } else if (item.antiImpulseAdvice != null) {
                                        Card(
                                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                                            shape = RoundedCornerShape(12.dp)
                                        ) {
                                            Column(modifier = Modifier.padding(16.dp)) {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Icon(Icons.Filled.Warning, null, tint = MaterialTheme.colorScheme.secondary)
                                                    Spacer(modifier = Modifier.width(8.dp))
                                                    Text("Anti-Impulse Advice", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary)
                                                }
                                                Spacer(modifier = Modifier.height(8.dp))
                                                MarkdownText(
                                                    markdown = item.antiImpulseAdvice,
                                                    fontSize = 14.sp,
                                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                            
                            // Dấu chấm tròn chỉ báo trang (Page Indicator)
                            if (selectedItems.size > 1) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(
                                    modifier = Modifier.wrapContentHeight().fillMaxWidth().padding(bottom = 8.dp),
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    repeat(selectedItems.size) { iteration ->
                                        val color = if (pagerState.currentPage == iteration) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)
                                        Box(
                                            modifier = Modifier
                                                .padding(horizontal = 3.dp)
                                                .clip(androidx.compose.foundation.shape.CircleShape)
                                                .background(color)
                                                .size(8.dp)
                                        )
                                    }
                                }
                            }
                        }
                    } else if (isBatchMode) {
                        // Hiển thị danh sách batch scan
                        LazyColumn(
                            modifier = Modifier.fillMaxWidth().heightIn(max = 400.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(scannedItemsBatch.size) { index ->
                                val item = scannedItemsBatch[index]
                                Row(
                                    modifier = Modifier.fillMaxWidth().clickable {
                                        val newList = scannedItemsBatch.toMutableList()
                                        newList[index] = item.copy(isSelected = !item.isSelected)
                                        scannedItemsBatch = newList
                                    },
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Checkbox(
                                        checked = item.isSelected,
                                        onCheckedChange = { checked ->
                                            val newList = scannedItemsBatch.toMutableList()
                                            newList[index] = item.copy(isSelected = checked)
                                            scannedItemsBatch = newList
                                        }
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    androidx.compose.foundation.Image(
                                        bitmap = item.bitmap.asImageBitmap(),
                                        contentDescription = null,
                                        modifier = Modifier.size(60.dp).clip(RoundedCornerShape(8.dp)),
                                        contentScale = ContentScale.Crop
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = item.displayText,
                                            fontSize = 12.sp,
                                            lineHeight = 16.sp,
                                            maxLines = 4,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        OutlinedTextField(
                                            value = item.priceInput,
                                            onValueChange = { newValue ->
                                                val newList = scannedItemsBatch.toMutableList()
                                                newList[index] = item.copy(priceInput = newValue)
                                                scannedItemsBatch = newList
                                            },
                                            label = { Text("Price (VND)", fontSize = 10.sp) },
                                            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
                                            modifier = Modifier.fillMaxWidth().height(54.dp),
                                            textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp),
                                            singleLine = true
                                        )
                                    }
                                }
                            }
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
                                        MarkdownText(
                                            markdown = antiImpulseAdvice!!,
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
                            if (isBatchMode && scannedItemsBatch.isNotEmpty() && antiImpulseAdvice == null && !showAdvicePager) {
                                Button(
                                    onClick = {
                                        try {
                                            cameraLauncher.launch(null)
                                        } catch (e: Exception) {
                                            e.printStackTrace()
                                            // 👇 [THI] BẮT LỖI / HIỂN THỊ GIAO DIỆN LỖI Ở ĐÂY 👇
                                            Toast.makeText(context, "Cannot open camera", Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp).height(48.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primaryContainer, contentColor = MaterialTheme.colorScheme.onPrimaryContainer)
                                ) {
                                    Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(20.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Scan More 📷", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                }
                            }

                            //NÚT "SHOULD I BUY THIS?" (NẰM TRÊN CÙNG)
                            val hasSelectedItems = isBatchMode && scannedItemsBatch.any { it.isSelected }
                            val canAskAdvice = (!isBatchMode && rawScannedJson != null && scannedBitmap != null) || hasSelectedItems
                            
                            if (canAskAdvice && antiImpulseAdvice == null && !showAdvicePager) {
                                val selectedCount = if (isBatchMode) scannedItemsBatch.count { it.isSelected } else 1
                                val buttonText = if (selectedCount > 1) "Should I buy these?" else "Should I buy this?"
                                
                                Button(
                                    onClick = {
                                        if (isBatchMode) {
                                            showAdvicePager = true
                                        } else {
                                            isCheckingDupe = true
                                        }
                                        scope.launch(Dispatchers.IO) {
                                            try {
                                                val generativeModel = GenerativeModel(
                                                    modelName = "gemini-3.1-flash-lite",
                                                    apiKey = com.example.dacs3.BuildConfig.GEMINI_API_KEY
                                                )

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

                                                if (isBatchMode) {
                                                    val deferredList = scannedItemsBatch.mapIndexed { index, item ->
                                                        if (item.isSelected) {
                                                            async {
                                                                val updatedList1 = scannedItemsBatch.toMutableList()
                                                                updatedList1[index] = item.copy(isAdviceLoading = true)
                                                                scannedItemsBatch = updatedList1

                                                                try {
                                                                    val response = generativeModel.generateContent(
                                                                        content {
                                                                            image(item.bitmap)
                                                                            text(prompt)
                                                                        }
                                                                    )
                                                                    val updatedList2 = scannedItemsBatch.toMutableList()
                                                                    updatedList2[index] = updatedList2[index].copy(isAdviceLoading = false, antiImpulseAdvice = response.text)
                                                                    scannedItemsBatch = updatedList2
                                                                } catch (e: Exception) {
                                                                    val updatedList3 = scannedItemsBatch.toMutableList()
                                                                    updatedList3[index] = updatedList3[index].copy(isAdviceLoading = false, antiImpulseAdvice = "Error analyzing item.")
                                                                    scannedItemsBatch = updatedList3
                                                                }
                                                            }
                                                        } else {
                                                            null
                                                        }
                                                    }
                                                    deferredList.filterNotNull().awaitAll()
                                                } else {
                                                    val response = generativeModel.generateContent(
                                                        content {
                                                            image(scannedBitmap!!)
                                                            text(prompt)
                                                        }
                                                    )
                                                    antiImpulseAdvice = response.text
                                                }
                                            } catch (e: Exception) {
                                                if (!isBatchMode) antiImpulseAdvice = "Error analyzing closet."
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
                                        buttonText,
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
                                        if (showAdvicePager) {
                                            showAdvicePager = false
                                            // Reset các trạng thái loading
                                            scannedItemsBatch = scannedItemsBatch.map { it.copy(isAdviceLoading = false, antiImpulseAdvice = null) }
                                        } else {
                                            aiScanResultText = null
                                            rawScannedJson = null
                                            antiImpulseAdvice = null
                                            isBatchMode = false
                                            scannedItemsBatch = emptyList()
                                            batchTotal = 0
                                            batchScanProgress = 0
                                        }
                                    }
                                ) {
                                    Text(
                                        if (showAdvicePager) "Back to List" else "Close",
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                // --- NÚT ADD TO CLOSET (BÊN PHẢI) ---
                                if (isBatchMode && scannedItemsBatch.isNotEmpty()) {
                                    Button(
                                        onClick = {
                                            val selectedItems = scannedItemsBatch.filter { it.isSelected }
                                            if (selectedItems.isEmpty()) {
                                                Toast.makeText(context, "Please select at least 1 item", Toast.LENGTH_SHORT).show()
                                                return@Button
                                            }
                                            
                                            val currentUserId = supabase.auth.currentUserOrNull()?.id ?: ""
                                            scope.launch(Dispatchers.IO) {
                                                selectedItems.forEach { item ->
                                                    val json = item.json
                                                    val itemToSave = ClothingItem(
                                                        userId = currentUserId,
                                                        clothes_name = json.optString("name", "Unknown Item"),
                                                        category = json.optString("category", "Other"),
                                                        mainColor = json.optString("main_color", "Unknown"),
                                                        seasons = List(json.optJSONArray("seasons")?.length() ?: 0) { json.optJSONArray("seasons")?.getString(it) ?: "" },
                                                        occasions = List(json.optJSONArray("occasions")?.length() ?: 0) { json.optJSONArray("occasions")?.getString(it) ?: "" },
                                                        imageUrl = "",
                                                        price = item.priceInput.toFloatOrNull()
                                                    )
                                                    viewModel.uploadAndSaveClothes(item.bitmap, itemToSave) {}
                                                }
                                                
                                                launch(Dispatchers.Main) {
                                                    aiScanResultText = null
                                                    rawScannedJson = null
                                                    scannedBitmap = null
                                                    showAdvicePager = false
                                                    antiImpulseAdvice = null
                                                    isBatchMode = false
                                                    scannedItemsBatch = emptyList()
                                                    batchTotal = 0
                                                    batchScanProgress = 0
                                                    Toast.makeText(context, "Added ${selectedItems.size} items to closet", Toast.LENGTH_SHORT).show()
                                                }
                                            }
                                        }
                                    ) {
                                        val count = scannedItemsBatch.count { it.isSelected }
                                        Text("Add Selected ($count)")
                                    }
                                } else if (!isBatchMode && rawScannedJson != null && scannedBitmap != null) {
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
                                                isBatchMode = false
                                                scannedItemsBatch = emptyList()
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
                    .clickable {
                        showScanSourceSheet = true
                    }
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
                    tint = MaterialTheme.colorScheme.onPrimary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "AI Scan",
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimary
                )
            }
        }

        if (showScanSourceSheet) {
            ModalBottomSheet(
                onDismissRequest = { showScanSourceSheet = false },
                containerColor = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Scan Your Clothes 📸",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(bottom = 20.dp)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                            modifier = Modifier
                                .weight(1f)
                                .height(110.dp)
                                .clickable {
                                    showScanSourceSheet = false
                                    cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                                }
                        ) {
                            Column(
                                modifier = Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.Center,
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.PhotoCamera,
                                    contentDescription = "Camera",
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.size(32.dp)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Take Photo",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }

                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                            modifier = Modifier
                                .weight(1f)
                                .height(110.dp)
                                .clickable {
                                    showScanSourceSheet = false
                                    galleryLauncher.launch("image/*")
                                }
                        ) {
                            Column(
                                modifier = Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.Center,
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Collections,
                                    contentDescription = "Gallery",
                                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                    modifier = Modifier.size(32.dp)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "From Gallery",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }
    }
}
}

suspend fun removeBackgroundLocal(bitmap: Bitmap): Bitmap = kotlinx.coroutines.suspendCancellableCoroutine { continuation ->
    try {
        val options = SubjectSegmenterOptions.Builder()
            .enableForegroundBitmap()
            .build()
        val segmenter = SubjectSegmentation.getClient(options)
        val image = InputImage.fromBitmap(bitmap, 0)
        segmenter.process(image)
            .addOnSuccessListener { result ->
                val fg = result.foregroundBitmap
                if (fg != null) {
                    continuation.resume(fg)
                } else {
                    continuation.resume(bitmap)
                }
            }
            .addOnFailureListener { e ->
                e.printStackTrace()
                continuation.resume(bitmap)
            }
    } catch (e: Exception) {
        e.printStackTrace()
        continuation.resume(bitmap)
    }
}





