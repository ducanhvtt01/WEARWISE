package com.example.dacs3.connectDB

import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName

// 1. Thông tin người dùng
@Serializable
data class Profile(
    val id: String, // Khớp với UUID của Auth
    val username: String? = null,
    @SerialName("full_name") val fullName: String? = null,
    @SerialName("avatar_url") val avatarUrl: String? = null,
    val gender: String,
    @SerialName("height_cm") val heightCm: Float,
    @SerialName("weight_kg") val weightKg: Float,
    @SerialName("body_shape") val bodyShape: String,
    @SerialName("skin_tone") val skinTone: String,
    @SerialName("favorite_styles") val favoriteStyles: List<String>,
    @SerialName("favorite_colors") val favoriteColors: List<String>,
    @SerialName("top_size") val topSize: String,
    @SerialName("bottom_size") val bottomSize: String,
    @SerialName("shoe_size_eu") val shoeSizeEu: Int,
    @SerialName("updated_at") val updatedAt: String
)

@Serializable
data class ProfileUpdate(
    val gender: String,
    @SerialName("height_cm") val heightCm: Float,
    @SerialName("weight_kg") val weightKg: Float,
    @SerialName("body_shape") val bodyShape: String,
    @SerialName("skin_tone") val skinTone: String,
    @SerialName("favorite_styles") val favoriteStyles: List<String>,
    @SerialName("favorite_colors") val favoriteColors: List<String>,
    @SerialName("top_size") val topSize: String,
    @SerialName("bottom_size") val bottomSize: String,
    @SerialName("shoe_size_eu") val shoeSizeEu: Int,
    @SerialName("updated_at") val updatedAt: String // Supabase sẽ tự hiểu định dạng ISO
)

// 2. Món đồ cá nhân (Dùng cho ML Kit quét)
@Serializable
data class ClothingItem(
    val id: String? = null,
    @SerialName("user_id") val userId: String,
    @SerialName("image_url") val imageUrl: String = "",
    val clothes_name: String = "Unknown",
    val category: String,
    @SerialName("main_color") val mainColor: String? = null,
    val seasons: List<String>? = null,
    val occasions: List<String>? = null,
    val status: String = "active",
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("last_worn_date") val lastWornDate: String? = null
)

// 3. Bộ phối đồ (Outfit)
data class Outfit(
    val items: List<ClothingItem>
)

@Serializable
data class ClothingFeedback(
    val id: String? = null,
    @SerialName("user_id") val userId: String,
    @SerialName("clothing_id") val clothingId: String,
    val rating: Int, // 1 for Like, -1 for Dislike
    @SerialName("created_at") val createdAt: String? = null
)

@Serializable
data class ChatSession(
    val id: String? = null, // Khóa chính (UUID)
    @SerialName("user_id") val userId: String, // ID người dùng
    val title: String = "New Conversation", // Tên cuộc trò chuyện
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null
)

@Serializable
data class ChatMessageModel(
    val id: String? = null, // Khóa chính
    @SerialName("session_id") val sessionId: String, // Thuộc phiên chat nào
    val role: String, // Chỉ nhận 'user' hoặc 'model'
    val content: String, // Nội dung tin nhắn
    @SerialName("created_at") val createdAt: String? = null
)

// 4. Các model cho Nhật Ký Phối Đồ (OOTD) chuẩn 3NF
@Serializable
data class OutfitDbModel(
    val id: String? = null,
    @SerialName("user_id") val userId: String,
    val name: String = "My Outfit",
    val description: String? = null,
    @SerialName("image_url") val imageUrl: String? = null,
    val occasion: String? = null,
    val season: String? = null,
    @SerialName("is_favorite") val isFavorite: Boolean = false,
    @SerialName("created_at") val createdAt: String? = null
)

@Serializable
data class OutfitItemDbModel(
    val id: String? = null,
    @SerialName("outfit_id") val outfitId: String,
    @SerialName("clothing_id") val clothingId: String
)

@Serializable
data class UsageHistoryDbModel(
    val id: String? = null,
    @SerialName("user_id") val userId: String,
    @SerialName("outfit_id") val outfitId: String,
    @SerialName("weather_main") val weatherMain: String? = null,
    @SerialName("temperature_c") val temperatureC: Float? = null,
    val humidity: String? = null,
    @SerialName("worn_date") val wornDate: String? = null, // yyyy-MM-dd
    val location: String? = null,
    val rating: Int? = null,
    val note: String? = null,
    @SerialName("created_at") val createdAt: String? = null
)