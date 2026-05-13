package com.example.dacs3.shop

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SeasonalShopResponseDto(
    val meta: SeasonalShopMetaDto,
    val shops: List<SeasonalStoreDto>
)

@Serializable
data class SeasonalShopMetaDto(
    val project: String,
    val description: String,
    val country: String,
    val provinceCount: Int,
    val shopsPerProvince: Int,
    val totalShops: Int,
    val mainFocus: String,
    val dataType: String,
    val generatedAt: String,
    val note: String
)

@Serializable
data class SeasonalStoreDto(
    val id: String,
    val name: String,
    val type: String,
    val provinceCode: String,
    val province: String,
    val provinceType: String,
    val region: String,
    val area: String,
    val address: String,
    val location: ShopLocationDto,
    val rating: Double,
    val reviewCount: Int,
    val priceLevel: Int,
    val phone: String,
    val openHours: String,
    val isOpenNow: Boolean,
    val hasOnlineStore: Boolean,
    val onlinePlatform: String,
    val onlineUrl: String,
    val coverImage: String,
    val tags: List<String>,

    @SerialName("seasonCollections")
    val seasons: List<String>,

    val distancePriority: String,
    val products: List<SeasonalProductDto>
) {
    val lat: Double
        get() = location.lat

    val lng: Double
        get() = location.lng

    val distanceText: String
        get() = when (distancePriority) {
            "near_da_nang_center" -> "Gần trung tâm Đà Nẵng"
            "near_beach" -> "Gần biển"
            "da_nang_suburban" -> "Ngoại ô Đà Nẵng"
            "central_vietnam" -> "Miền Trung"
            "central_highlands" -> "Tây Nguyên"
            "mekong" -> "Miền Tây"
            else -> area
        }
}

@Serializable
data class ShopLocationDto(
    val lat: Double,
    val lng: Double
)

@Serializable
data class SeasonalProductDto(
    val id: String,
    val name: String,
    val category: String,
    val season: String,
    val price: Int,
    val currency: String,
    val colors: List<String>,
    val sizes: List<String>,
    val stock: Int,
    val soldMonthly: Int,
    val image: String
) {
    val priceRange: String
        get() = "${price / 1000}k"
}