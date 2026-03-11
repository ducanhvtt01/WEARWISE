package com.example.dacs3.connectDB

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.jan.supabase.postgrest.from
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
                                eq(
                                    "user_id",
                                    userId
                                )
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
                bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 80, baos)
                val imageBytes = baos.toByteArray()

                // 2. Tạo tên file duy nhất (vd: clothes_123456789.jpg)
                val fileName = "clothes_${System.currentTimeMillis()}.jpg"

                // 3. Upload lên bucket có tên là "clothing_images"
                val bucket = supabase.storage.from("clothing_images")
                bucket.upload(fileName, imageBytes)

                // 4. Lấy Public URL của ảnh vừa upload
                val publicUrl = bucket.publicUrl(fileName)

                // 5. Cập nhật URL vào object và insert vào Database
                val finalItem = clothingItem.copy(imageUrl = publicUrl)
                supabase.from("clothes").insert(finalItem)

                // Cập nhật lại UI List ngay lập tức sau khi insert DB thành công
                val currentList = _clothingItems.value.toMutableList()
                currentList.add(0, finalItem) // Thêm áo mới lên đầu danh sách
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
                supabase.from("clothes").insert(item)

                // Cập nhật lại UI List
                val currentList = _clothingItems.value.toMutableList()
                currentList.add(0, item)
                _clothingItems.value = currentList

                launch(Dispatchers.Main) { onSuccess() }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // Hàm cập nhật quần áo (dùng cho BottomSheet khi người dùng muốn điều chỉnh thông số quần áo)
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

    // Hàm xóa quần áo (dùng cho BottomSheet)
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

    // Hàm cập nhật profile
    fun updateMeasurements(userId: String, height: Int, weight: Int, shape: String) {
        viewModelScope.launch {
            isUpdating = true
            try {
                // Tạo map dữ liệu cập nhật khớp với tên cột trong SQL
                val updateMap = mapOf(
                    "height_cm" to height,
                    "weight_kg" to weight,
                    "body_shape" to shape,
                    "updated_at" to Clock.System.now().toString()
                )

                supabase.from("profiles").update(updateMap) {
                    filter { eq("id", userId) }
                }

                // Cập nhật lại state cục bộ để giao diện đổi ngay lập tức
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
}