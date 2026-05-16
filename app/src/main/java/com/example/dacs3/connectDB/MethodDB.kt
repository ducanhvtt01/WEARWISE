package com.example.dacs3.connectDB

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.dacs3.dashboard.ChatMessage
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Order
import io.github.jan.supabase.storage.storage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock
import java.io.ByteArrayOutputStream

class DashboardViewModel : ViewModel() {
    var userProfile by mutableStateOf<Profile?>(null)
    var isUpdating by mutableStateOf(false)

    // Biến lưu trữ danh sách quần áo để ClosetScreen quan sát
    private val _clothingItems = MutableStateFlow<List<ClothingItem>>(emptyList())
    val clothingItems: StateFlow<List<ClothingItem>> = _clothingItems.asStateFlow()

    // Smart Wardrobe: Danh sách đồ được sắp xếp theo độ ưu tiên
    private val _smartWardrobeItems = MutableStateFlow<List<ClothingItem>>(emptyList())
    val smartWardrobeItems: StateFlow<List<ClothingItem>> = _smartWardrobeItems.asStateFlow()

    // Danh sách phản hồi (Feedback) cho từng món đồ (Mã đồ -> Đánh giá 1 hoặc -1)
    private val _clothingFeedbackMap = MutableStateFlow<Map<String, Int>>(emptyMap())
    val clothingFeedbackMap: StateFlow<Map<String, Int>> = _clothingFeedbackMap.asStateFlow()

    // State cho Outfit Canvas
    val aiCanvasOutfit = MutableStateFlow<Outfit?>(null)
    var isCanvasLoading by mutableStateOf(false)
    var canvasError by mutableStateOf<String?>(null)

    // Global Error Message for UI feedback
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    fun clearError() { _errorMessage.value = null }

    // Hàm lấy profile từ Supabase
    fun getProfile(userId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val profile = supabase.from("profiles")
                    .select { filter { eq("id", userId) } }
                    .decodeSingleOrNull<Profile>()
                
                withContext(Dispatchers.Main) {
                    userProfile = profile
                }
            } catch (e: Exception) {
                e.printStackTrace()
                _errorMessage.value = "Failed to load profile: ${e.localizedMessage}"
            }
        }
    }

    // Hàm lấy toàn bộ quần áo của User từ DB
    fun getClothingItems(userId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val clothes =
                    supabase.from("clothes")
                        .select {
                            filter {
                                eq("user_id", userId)
                                neq("status", "unactive")
                            }
                        }
                        .decodeList<ClothingItem>()
                _clothingItems.value = clothes

                // Lấy thêm Feedback cho các món đồ này
                val feedbacks = supabase.from("clothing_feedback")
                    .select { filter { eq("user_id", userId) } }
                    .decodeList<ClothingFeedback>()
                
                val feedbackMap = feedbacks.associate { it.clothingId to it.rating }
                _clothingFeedbackMap.value = feedbackMap
            } catch (e: Exception) {
                e.printStackTrace()
                _errorMessage.value = "Failed to load wardrobe: ${e.localizedMessage}"
            }
        }
    }

    fun uploadAndSaveClothes(
        bitmap: android.graphics.Bitmap,
        clothingItem: ClothingItem,
        onSuccess: () -> Unit
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // 1. Tối ưu hóa Bitmap: Resize (giới hạn 1024x1024) và chuyển sang JPEG
                val maxSize = 1024
                var finalBitmap = bitmap
                if (bitmap.width > maxSize || bitmap.height > maxSize) {
                    val ratio = Math.min(maxSize.toFloat() / bitmap.width, maxSize.toFloat() / bitmap.height)
                    val width = (bitmap.width * ratio).toInt()
                    val height = (bitmap.height * ratio).toInt()
                    finalBitmap = android.graphics.Bitmap.createScaledBitmap(bitmap, width, height, true)
                }
                
                val baos = ByteArrayOutputStream()
                finalBitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 80, baos)
                val imageBytes = baos.toByteArray()

                // 2. Tạo tên file duy nhất (dùng đuôi .jpg)
                val fileName = "clothes_${System.currentTimeMillis()}.jpg"

                // 3. Upload lên bucket
                val bucket = supabase.storage.from("clothing_images")
                bucket.upload(fileName, imageBytes)

                // 4. Lấy Public URL của ảnh vừa upload
                val publicUrl = bucket.publicUrl(fileName)

                // 5. Cập nhật URL vào object
                val finalItem = clothingItem.copy(imageUrl = publicUrl)

                // Yêu cầu Supabase trả về item đã có ID thật
                val savedItem = supabase.from("clothes").insert(finalItem) {
                    select()
                }.decodeSingle<ClothingItem>()

                // Cập nhật lại UI List với savedItem
                val currentList = _clothingItems.value.toMutableList()
                currentList.add(0, savedItem)
                _clothingItems.value = currentList

                launch(Dispatchers.Main) {
                    onSuccess()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                _errorMessage.value = "Failed to upload clothing: ${e.localizedMessage}"
            }
        }
    }

    fun addClothing(item: ClothingItem, onSuccess: () -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // Yêu cầu Supabase trả về item đã có ID thật (dùng cho Undo khôi phục đồ)
                val savedItem = supabase.from("clothes").insert(item) {
                    select()
                }.decodeSingle<ClothingItem>()

                // Cập nhật lại UI List
                val currentList = _clothingItems.value.toMutableList()
                currentList.add(0, savedItem)
                _clothingItems.value = currentList

                launch(Dispatchers.Main) { onSuccess() }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun updateClothingItem(item: ClothingItem) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                item.id?.let { itemId ->
                    supabase.from("clothes").update(item) { filter { eq("id", itemId) } }
                    // Cập nhật UI
                    val currentList = _clothingItems.value.toMutableList()
                    val index = currentList.indexOfFirst { it.id == itemId }
                    if (index != -1) {
                        currentList[index] = item
                        _clothingItems.value = currentList
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun deleteClothingItem(item: ClothingItem) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                item.id?.let { itemId ->
                    // THỰC HIỆN XÓA MỀM (Soft Delete): Cập nhật status thành "unactive"
                    val updateMap = mapOf("status" to "unactive")
                    supabase.from("clothes").update(updateMap) { filter { eq("id", itemId) } }
                    // Cập nhật UI
                    val currentList = _clothingItems.value.toMutableList()
                    currentList.remove(item)
                    _clothingItems.value = currentList
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun updateMeasurements(
        userId: String, 
        height: Int, 
        weight: Int, 
        shape: String,
        skinTone: String,
        topSize: String,
        bottomSize: String,
        shoeSizeEu: Int
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            withContext(Dispatchers.Main) { isUpdating = true }
            try {
                val updateMap = mapOf(
                    "height_cm" to height,
                    "weight_kg" to weight,
                    "body_shape" to shape,
                    "skin_tone" to skinTone,
                    "top_size" to topSize,
                    "bottom_size" to bottomSize,
                    "shoe_size_eu" to shoeSizeEu,
                    "updated_at" to Clock.System.now().toString()
                )

                supabase.from("profiles").update(updateMap) {
                    filter { eq("id", userId) }
                }

                withContext(Dispatchers.Main) {
                    userProfile = userProfile?.copy(
                        heightCm = height.toFloat(),
                        weightKg = weight.toFloat(),
                        bodyShape = shape,
                        skinTone = skinTone,
                        topSize = topSize,
                        bottomSize = bottomSize,
                        shoeSizeEu = shoeSizeEu
                    )
                }
            } catch (e: Exception) {
                e.printStackTrace()
                _errorMessage.value = "Failed to update measurements: ${e.localizedMessage}"
            } finally {
                withContext(Dispatchers.Main) { isUpdating = false }
            }
        }
    }

    fun updateStylePreferences(userId: String, styles: List<String>, colors: List<String>) {
        viewModelScope.launch(Dispatchers.IO) {
            withContext(Dispatchers.Main) { isUpdating = true }
            try {
                val updateMap = mapOf(
                    "favorite_styles" to styles,
                    "favorite_colors" to colors,
                    "updated_at" to Clock.System.now().toString()
                )

                supabase.from("profiles").update(updateMap) {
                    filter { eq("id", userId) }
                }

                withContext(Dispatchers.Main) {
                    userProfile = userProfile?.copy(
                        favoriteStyles = styles,
                        favoriteColors = colors
                    )
                }
            } catch (e: Exception) {
                e.printStackTrace()
                _errorMessage.value = "Failed to update styles: ${e.localizedMessage}"
            } finally {
                withContext(Dispatchers.Main) { isUpdating = false }
            }
        }
    }

    fun updateProfile(userId: String, fullName: String) {
        viewModelScope.launch(Dispatchers.IO) {
            withContext(Dispatchers.Main) { isUpdating = true }
            try {
                val updateMap = mapOf(
                    "full_name" to fullName,
                    "updated_at" to Clock.System.now().toString()
                )

                supabase.from("profiles").update(updateMap) {
                    filter { eq("id", userId) }
                }

                withContext(Dispatchers.Main) {
                    userProfile = userProfile?.copy(fullName = fullName)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                _errorMessage.value = "Failed to update profile: ${e.localizedMessage}"
            } finally {
                withContext(Dispatchers.Main) { isUpdating = false }
            }
        }
    }

    fun uploadAvatar(context: android.content.Context, userId: String, bitmap: android.graphics.Bitmap) {
        viewModelScope.launch(Dispatchers.IO) {
            isUpdating = true
            launch(Dispatchers.Main) {
                android.widget.Toast.makeText(context, "Updating avatar...", android.widget.Toast.LENGTH_SHORT).show()
            }
            try {
                // 1. Tối ưu hóa Bitmap: Resize (giới hạn 512x512) và chuyển sang JPEG
                val maxSize = 512
                var finalBitmap = bitmap
                
                // Đảm bảo không dùng Hardware bitmap để có thể nén/resize
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O && 
                    bitmap.config == android.graphics.Bitmap.Config.HARDWARE) {
                    finalBitmap = bitmap.copy(android.graphics.Bitmap.Config.ARGB_8888, false)
                }

                if (finalBitmap.width > maxSize || finalBitmap.height > maxSize) {
                    val ratio = Math.min(maxSize.toFloat() / finalBitmap.width, maxSize.toFloat() / finalBitmap.height)
                    val width = (finalBitmap.width * ratio).toInt()
                    val height = (finalBitmap.height * ratio).toInt()
                    finalBitmap = android.graphics.Bitmap.createScaledBitmap(finalBitmap, width, height, true)
                }
                
                val baos = ByteArrayOutputStream()
                finalBitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 80, baos)
                val imageBytes = baos.toByteArray()

                // 2. Tạo tên file duy nhất dựa trên UserId
                val fileName = "avatar_${userId}.jpg" // Dùng cùng tên để ghi đè (upsert)

                // 3. Upload lên bucket "avatars"
                val bucket = supabase.storage.from("avatars")
                bucket.upload(fileName, imageBytes, upsert = true)

                // 4. Lấy Public URL kèm theo timestamp để tránh cache ảnh cũ
                val publicUrl = bucket.publicUrl(fileName) + "?t=${System.currentTimeMillis()}"

                // 5. Cập nhật vào bảng profiles
                supabase.from("profiles").update(mapOf("avatar_url" to publicUrl)) {
                    filter { eq("id", userId) }
                }

                // 6. Cập nhật State UI
                launch(Dispatchers.Main) {
                    userProfile = userProfile?.copy(avatarUrl = publicUrl)
                    android.widget.Toast.makeText(context, "Avatar updated successfully!", android.widget.Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                launch(Dispatchers.Main) {
                    android.widget.Toast.makeText(context, "Upload failed: ${e.localizedMessage}", android.widget.Toast.LENGTH_LONG).show()
                }
            } finally {
                isUpdating = false
            }
        }
    }

    // ==========================================
    // XỬ LÝ LƯU TRỮ LỊCH SỬ CHAT AI
    // ==========================================

    var currentChatSessionId: String? = null

    // 1. Biến lưu trữ danh sách các phiên chat cũ để hiển thị lên Sidebar
    private val _chatSessions = MutableStateFlow<List<ChatSession>>(emptyList())
    val chatSessions: StateFlow<List<ChatSession>> = _chatSessions.asStateFlow()

    // 2. Hàm lấy danh sách các phiên chat từ Database
    fun fetchChatSessions(userId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val sessions = supabase.from("chat_sessions")
                    .select {
                        filter { eq("user_id", userId) }
                        order("updated_at", order = Order.DESCENDING)
                    }
                    .decodeList<ChatSession>()
                _chatSessions.value = sessions
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // 3. Hàm tải lại tin nhắn của một Session cũ
    fun loadChatMessages(sessionId: String, onLoaded: (List<ChatMessage>) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val messages = supabase.from("chat_messages")
                    .select {
                        filter { eq("session_id", sessionId) }
                        order("created_at", order = Order.ASCENDING)
                    }
                    .decodeList<ChatMessageModel>()

                val uiMessages = messages.map {
                    ChatMessage.fromText(rawText = it.content, isFromUser = it.role == "user")
                }

                launch(Dispatchers.Main) {
                    currentChatSessionId = sessionId
                    onLoaded(uiMessages)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // 4. Hàm lưu tin nhắn vào Database
    fun saveChatToDatabase(userMessage: String, aiMessage: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val userId = supabase.auth.currentUserOrNull()?.id ?: return@launch

                if (currentChatSessionId == null) {
                    // --- BẮT ĐẦU: LOGIC TẠO TIÊU ĐỀ THÔNG MINH ---
                    // Cắt lấy tối đa 35 ký tự đầu tiên của prompt, nếu dài hơn thì thêm dấu "..."
                    val smartTitle = if (userMessage.length > 35) {
                        userMessage.substring(0, 35).trim() + "..."
                    } else {
                        userMessage
                    }.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() } // Viết hoa chữ cái đầu
                    // --- KẾT THÚC LOGIC ---

                    val newSession = ChatSession(
                        userId = userId,
                        title = smartTitle // Sử dụng tiêu đề thông minh vừa tạo
                    )

                    val insertedSession = supabase.from("chat_sessions")
                        .insert(newSession) { select() }.decodeSingle<ChatSession>()
                    currentChatSessionId = insertedSession.id
                }

                currentChatSessionId?.let { sessionId ->
                    val userRecord = ChatMessageModel(
                        sessionId = sessionId,
                        role = "user",
                        content = userMessage
                    )
                    val aiRecord = ChatMessageModel(
                        sessionId = sessionId,
                        role = "model",
                        content = aiMessage
                    )

                    supabase.from("chat_messages").insert(listOf(userRecord, aiRecord))

                    // Làm mới lại Sidebar để hiện cuộc trò chuyện này lên đầu
                    fetchChatSessions(userId)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // 5. Hàm reset phiên chat (Tạo đoạn chat mới)
    fun resetChatSession() {
        currentChatSessionId = null
    }

    // ==========================================
    // TẠO OUTFIT CANVAS BẰNG AI
    // ==========================================
    fun generateCanvasOutfit(weatherTemp: String) {
        viewModelScope.launch(Dispatchers.IO) {
            isCanvasLoading = true
            canvasError = null
            try {
                val currentItems = _clothingItems.value
                if (currentItems.size < 3) {
                    canvasError = "You need at least 3 items in your closet."
                    isCanvasLoading = false
                    return@launch
                }

                val generativeModel = com.google.ai.client.generativeai.GenerativeModel(
                    modelName = "gemini-3.1-flash-lite",
                    apiKey = com.example.dacs3.BuildConfig.GEMINI_API_KEY
                )

                val inventoryData = currentItems.joinToString("\n") { 
                    "- ID: ${it.id} | Name: ${it.clothes_name} | Category: ${it.category} | Color: ${it.mainColor}" 
                }

                // FETCH SOCIAL TRENDS (New)
                val socialTrends = userProfile?.favoriteStyles?.let { getSocialStyleTrends(it) } ?: ""
                
                val prompt = """
                    You are an expert fashion stylist. The current weather is $weatherTemp.
                    
                    COMMUNITY TRENDS:
                    $socialTrends
                    
                    Here is the user's closet inventory:
                    $inventoryData
                    
                    Task: Select 2 to 3 items to create a perfect, stylish outfit for today.
                    CRITICAL RULES:
                    1. You MUST select exactly 1 Top (or Shirt/Áo) and exactly 1 Bottom (or Pants/Quần).
                    2. DO NOT select two Bottoms or two Tops.
                    3. If the user has Shoes or Outerwear available, you may select 1 to complete the outfit (total 3 items).
                    4. The IDs you return MUST exactly match the IDs provided above.
                    
                    Return ONLY the IDs separated by commas. Do NOT return any other text, markdown, or explanations.
                """.trimIndent()

                val response = generativeModel.generateContent(prompt)
                val responseText = response.text?.trim() ?: ""

                // Robust ID matching: Check which actual closet IDs are present in the AI's response text.
                // This is 100% foolproof against AI hallucinations and formatting errors.
                val matchedItems = currentItems.filter { it.id != null && responseText.contains(it.id, ignoreCase = true) }
                
                if (matchedItems.size >= 2) {
                    val top = matchedItems.find { it.category.lowercase().run { contains("top") || contains("áo") || contains("shirt") } }
                    val bottom = matchedItems.find { it.category.lowercase().run { contains("bottom") || contains("quần") || contains("pants") } }
                    val shoes = matchedItems.find { it.category.lowercase().run { contains("shoes") || contains("giày") } }

                    val finalOutfit = mutableListOf<com.example.dacs3.connectDB.ClothingItem>()
                    if (top != null) finalOutfit.add(top)
                    if (bottom != null) finalOutfit.add(bottom)
                    if (shoes != null) finalOutfit.add(shoes)
                    
                    // Fill with remaining matched items if less than 3, but NO duplicate bottoms/tops
                    for (item in matchedItems) {
                        if (finalOutfit.size >= 3) break
                        if (!finalOutfit.contains(item)) {
                            val cat = item.category.lowercase()
                            val isBottom = cat.contains("bottom") || cat.contains("quần") || cat.contains("pants")
                            val isTop = cat.contains("top") || cat.contains("áo") || cat.contains("shirt")
                            
                            // Prevent adding a second top or bottom
                            if ((isBottom && bottom != null) || (isTop && top != null)) continue
                            
                            finalOutfit.add(item)
                        }
                    }

                    if (finalOutfit.size >= 2) {
                        aiCanvasOutfit.value = Outfit(finalOutfit)
                    } else {
                        canvasError = "AI couldn't formulate a proper Top + Bottom combination."
                    }
                } else {
                    canvasError = "AI couldn't find a complete outfit from your closet."
                }
            } catch (e: Exception) {
                e.printStackTrace()
                canvasError = "Connection error: ${e.localizedMessage}"
            } finally {
                isCanvasLoading = false
            }
        }
    }

    /**
     * Saves user feedback (Like/Dislike) for a specific clothing item.
     * rating: 1 for Like, -1 for Dislike
     */
    fun saveClothingFeedback(clothingId: String, rating: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val userId = supabase.auth.currentUserOrNull()?.id ?: return@launch
                
                // Use upsert to handle multiple feedback updates for the same item
                val feedback = ClothingFeedback(
                    userId = userId,
                    clothingId = clothingId,
                    rating = rating
                )
                
                supabase.from("clothing_feedback").upsert(feedback)
                
                // Update local state map immediately for responsive UI
                val currentMap = _clothingFeedbackMap.value.toMutableMap()
                currentMap[clothingId] = rating
                _clothingFeedbackMap.value = currentMap
                
                println("Feedback saved: Item $clothingId rated $rating")
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // 6. Hàm xóa lịch sử chat trên Database
    fun deleteChatSession(sessionId: String, userId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // Xóa tin nhắn trước (để không bị lỗi khóa ngoại reference), sau đó xóa Session
                supabase.from("chat_messages").delete { filter { eq("session_id", sessionId) } }
                supabase.from("chat_sessions").delete { filter { eq("id", sessionId) } }

                // Cập nhật lại danh sách bên Sidebar để dòng chat vừa xóa biến mất
                fetchChatSessions(userId)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // ==========================================
    // XỬ LÝ NHẬT KÝ PHỐI ĐỒ (OOTD)
    // ==========================================
    
    // Biến lưu trữ danh sách đồ mặc nhiều nhất
    private val _topFavoriteClothes = MutableStateFlow<List<Pair<ClothingItem, Int>>>(emptyList())
    val topFavoriteClothes: StateFlow<List<Pair<ClothingItem, Int>>> = _topFavoriteClothes.asStateFlow()

    fun logOotd(
        userId: String,
        clothingIds: List<String>,
        weatherMain: String? = null,
        temp: Float? = null,
        event: String? = null,
        mood: String? = null,
        onSuccess: () -> Unit
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // 1. Tạo Outfit mới
                val newOutfit = OutfitDbModel(
                    userId = userId,
                    name = "OOTD ${java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault()).format(java.util.Date())}"
                )
                val savedOutfit = supabase.from("outfits").insert(newOutfit) { select() }.decodeSingle<OutfitDbModel>()
                
                // 2. Lưu các món đồ vào Outfit Items
                val outfitItems = clothingIds.map { 
                    OutfitItemDbModel(outfitId = savedOutfit.id!!, clothingId = it)
                }
                supabase.from("outfit_items").insert(outfitItems)
                
                // 3. Tạo ghi chú ngữ cảnh (Event & Mood)
                val contextNote = if (event != null || mood != null) {
                    "Occasion: $event | Mood: $mood"
                } else null

                // 4. Lưu vào Usage History
                val usage = UsageHistoryDbModel(
                    userId = userId,
                    outfitId = savedOutfit.id!!,
                    weatherMain = weatherMain,
                    temperatureC = temp,
                    wornDate = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date()),
                    note = contextNote
                )
                supabase.from("usage_history").insert(usage)
                
                // 5. CẬP NHẬT last_worn_date cho quần áo
                val today = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date())
                val updateMap = mapOf("last_worn_date" to today)
                supabase.from("clothes").update(updateMap) {
                    filter { isIn("id", clothingIds) }
                }

                // Cập nhật lại UI Local
                val updatedItems = _clothingItems.value.map {
                    if (it.id in clothingIds) it.copy(lastWornDate = today) else it
                }
                _clothingItems.value = updatedItems
                
                launch(Dispatchers.Main) { 
                    onSuccess()
                    fetchTopFavoriteClothes(userId) // Làm mới danh sách Top Favourites
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // Hàm lấy danh sách đồ được mặc nhiều nhất dựa trên Usage History
    fun fetchTopFavoriteClothes(userId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // Bước 1: Lấy tất cả lịch sử mặc đồ của user
                val histories = supabase.from("usage_history")
                    .select { filter { eq("user_id", userId) } }
                    .decodeList<UsageHistoryDbModel>()
                
                if (histories.isEmpty()) return@launch
                
                val outfitIds = histories.map { it.outfitId }
                
                // Bước 2: Lấy tất cả item thuộc các bộ đồ đó
                val outfitItems = supabase.from("outfit_items")
                    .select { filter { isIn("outfit_id", outfitIds) } }
                    .decodeList<OutfitItemDbModel>()
                    
                // Bước 3: Đếm tần suất xuất hiện của mỗi clothingId
                val frequencyMap = outfitItems.groupingBy { it.clothingId }.eachCount()
                
                // Bước 4: Lấy top 5 clothingId phổ biến nhất
                val topClothingIds = frequencyMap.entries.sortedByDescending { it.value }.take(5).map { it.key }
                
                if (topClothingIds.isEmpty()) return@launch
                
                // Bước 5: Lấy thông tin chi tiết của các món đồ này từ DB
                val clothes = supabase.from("clothes")
                    .select { filter { isIn("id", topClothingIds) } }
                    .decodeList<ClothingItem>()
                    
                // Ghép nối data và tần suất lại với nhau
                val resultList = clothes.mapNotNull { item ->
                    val count = frequencyMap[item.id] ?: 0
                    if (count > 0) Pair(item, count) else null
                }.sortedByDescending { it.second }
                
                _topFavoriteClothes.value = resultList

                // Cập nhật Smart Wardrobe: Ưu tiên đồ hay mặc, sau đó đến đồ mới
                val allItems = _clothingItems.value
                val smartSorted = allItems.sortedByDescending { frequencyMap[it.id] ?: 0 }
                _smartWardrobeItems.value = smartSorted
            } catch(e: Exception) {
                e.printStackTrace()
            }
        }
    }

    /**
     * SOCIAL RS: Finds "Style Twins" (users with similar styles) and identifies what they are wearing/liking.
     */
    suspend fun getSocialStyleTrends(favoriteStyles: List<String>): String {
        return withContext(Dispatchers.IO) {
            try {
                val currentUserId = supabase.auth.currentUserOrNull()?.id ?: return@withContext ""
                
                // 1. Tìm những người dùng có cùng phong cách (Style Twins) bằng Jaccard Similarity
                val allProfiles = supabase.from("profiles").select {
                    filter {
                        neq("id", currentUserId)
                    }
                }.decodeList<Profile>()
                
                val myStylesSet = favoriteStyles.toSet()
                
                val styleTwins = allProfiles
                    .map { profile -> 
                        val sim = com.example.dacs3.RS.SimilarityMath.jaccardSimilarity(myStylesSet, profile.favoriteStyles.toSet())
                        profile to sim
                    }
                    .filter { it.second > 0.0f } // Chỉ lấy những người có ít nhất 1 điểm chung
                    .sortedByDescending { it.second } // Xếp hạng theo độ tương đồng cao nhất
                    .take(5) // Lấy Top 5 Style Twins
                    .map { it.first }
                
                if (styleTwins.isEmpty()) return@withContext "No community trends found for your styles yet."
                
                val twinIds = styleTwins.map { it.id }
                
                // 2. Tìm những món đồ họ thích hoặc dùng nhiều
                val trendingItems = supabase.from("clothes").select {
                    filter {
                        isIn("user_id", twinIds)
                        neq("status", "unactive")
                    }
                }.decodeList<ClothingItem>()
                
                if (trendingItems.isEmpty()) return@withContext "Style twins exist, but no wardrobe data found."

                // 3. Phân tích xem họ đang chuộng màu gì và loại đồ gì nhất
                val topColors = trendingItems.groupingBy { it.mainColor ?: "Neutral" }.eachCount()
                    .entries.sortedByDescending { it.value }.take(2).joinToString { it.key }
                
                val topCategories = trendingItems.groupingBy { it.category }.eachCount()
                    .entries.sortedByDescending { it.value }.take(2).joinToString { it.key }
                
                return@withContext "In your ${favoriteStyles.take(2)} community, $topColors colors and $topCategories are trending today!"
            } catch (e: Exception) {
                e.printStackTrace()
                ""
            }
        }
    }
}