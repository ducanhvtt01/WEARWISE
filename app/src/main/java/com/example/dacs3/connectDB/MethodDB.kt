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
import kotlinx.datetime.Clock
import java.io.ByteArrayOutputStream

class DashboardViewModel : ViewModel() {
    var userProfile by mutableStateOf<Profile?>(null)
    var isUpdating by mutableStateOf(false)

    // Biến lưu trữ danh sách quần áo để ClosetScreen quan sát
    private val _clothingItems = MutableStateFlow<List<ClothingItem>>(emptyList())
    val clothingItems: StateFlow<List<ClothingItem>> = _clothingItems.asStateFlow()

    // Hàm lấy profile từ Supabase
    fun getProfile(userId: String) {
        viewModelScope.launch {
            try {
                val profile = supabase.from("profiles")
                    .select { filter { eq("id", userId) } }
                    .decodeSingle<Profile>()
                userProfile = profile
            } catch (e: Exception) {
                e.printStackTrace()
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
                            }
                        }
                        .decodeList<ClothingItem>()
                _clothingItems.value = clothes
            } catch (e: Exception) {
                e.printStackTrace()
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
                // 1. Chuyển Bitmap thành ByteArray
                val baos = ByteArrayOutputStream()
                bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, baos)
                val imageBytes = baos.toByteArray()

                // 2. Tạo tên file duy nhất
                val fileName = "clothes_${System.currentTimeMillis()}.png"

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
                    supabase.from("clothes").delete { filter { eq("id", itemId) } }
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

    fun updateMeasurements(userId: String, height: Int, weight: Int, shape: String) {
        viewModelScope.launch {
            isUpdating = true
            try {
                val updateMap = mapOf(
                    "height_cm" to height,
                    "weight_kg" to weight,
                    "body_shape" to shape,
                    "updated_at" to Clock.System.now().toString()
                )

                supabase.from("profiles").update(updateMap) {
                    filter { eq("id", userId) }
                }

                userProfile = userProfile?.copy(
                    heightCm = height.toFloat(),
                    weightKg = weight.toFloat(),
                    bodyShape = shape
                )
            } catch (e: Exception) {
                e.printStackTrace()
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
                    ChatMessage(text = it.content, isFromUser = it.role == "user")
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
}