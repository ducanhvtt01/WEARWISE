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
    val gender: String? = null,
    @SerialName("height_cm") val height: Int? = null,
    @SerialName("weight_kg") val weight: Int? = null,
    @SerialName("body_shape") val bodyShape: String? = null,
    @SerialName("favorite_styles") val favoriteStyles: List<String>? = null
)

// 2. Món đồ cá nhân (Dùng cho ML Kit quét)
@Serializable
data class ClothingItem(
    val id: String? = null,
    @SerialName("user_id") val userId: String,
    @SerialName("image_url") val imageUrl: String,
    val category: String, // Top, Bottom, Shoes...
    @SerialName("main_color") val mainColor: String? = null,
    val seasons: List<String>? = null,
    val occasions: List<String>? = null
)

// 3. Bộ phối đồ
@Serializable
data class Outfit(
    val id: String? = null,
    @SerialName("user_id") val userId: String,
    val name: String,
    @SerialName("image_url") val imageUrl: String? = null,
    val occasion: String? = null
)