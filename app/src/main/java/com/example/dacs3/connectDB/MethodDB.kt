package com.example.dacs3.connectDB

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock

class DashboardViewModel : ViewModel() {
    var userProfile by mutableStateOf<Profile?>(null)
    var isUpdating by mutableStateOf(false)

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