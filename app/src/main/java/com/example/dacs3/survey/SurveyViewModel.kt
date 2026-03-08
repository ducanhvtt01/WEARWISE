package com.example.dacs3.survey

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.dacs3.connectDB.ProfileUpdate
import com.example.dacs3.connectDB.supabase
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant

class SurveyViewModel : ViewModel() {
    var gender by mutableStateOf("")
    var height by mutableFloatStateOf(165f)
    var weight by mutableFloatStateOf(55f)
    var bodyShape by mutableStateOf("")
    var skinTone by mutableStateOf("")
    var favoriteStyles by mutableStateOf(setOf<String>())
    var favoriteColors by mutableStateOf(setOf<String>())
    var topSize by mutableStateOf("")
    var bottomSize by mutableStateOf("")
    var shoeSize by mutableFloatStateOf(39f)

    var isUpdating by mutableStateOf(false)
    var updateSuccess by mutableStateOf(false)

    fun updateProfile(userId: String) {
        viewModelScope.launch {
            isUpdating = true
            try {
                val updateData = ProfileUpdate(
                    gender = gender,
                    heightCm = height,
                    weightKg = weight,
                    bodyShape = bodyShape,
                    skinTone = skinTone,
                    favoriteStyles = favoriteStyles.toList(),
                    favoriteColors = favoriteColors.toList(),
                    topSize = topSize,
                    bottomSize = bottomSize,
                    shoeSizeEu = shoeSize.toInt(),
                    updatedAt = Clock.System.now().toString() // Lấy thời gian hiện tại
                )

                // THỰC HIỆN UPDATE
                supabase.from("profiles")
                    .update(updateData) {
                        filter {
                            eq("id", userId) // Lọc đúng ID người dùng
                        }
                    }

                updateSuccess = true
            } catch (e: Exception) {
                println("Lỗi update: ${e.message}")
                updateSuccess = false
            } finally {
                isUpdating = false
            }
        }
    }
}
data class SurveyData(
    val gender: String, val heightCm: Int, val weightKg: Int,
    val bodyShape: String, val skinTone: String,
    val favoriteStyles: List<String>, val favoriteColors: List<String>,
    val topSize: String, val bottomSize: String, val shoeSizeEu: Int
)