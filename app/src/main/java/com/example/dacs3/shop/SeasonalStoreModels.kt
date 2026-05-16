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
    val project: String = "",
    val description: String = "",
    val country: String = "",
    val provinceCount: Int = 0,

    // Bản JSON cũ dùng field này
    val shopsPerProvince: Int = 0,

    // Bản JSON mới dùng 2 field này
    val otherProvincesShopsPerProvince: Int = 0,
    val daNangShopCount: Int = 0,

    val totalShops: Int = 0,
    val mainFocus: String = "",
    val dataType: String = "",
    val generatedAt: String = "",
    val note: String = ""
)

@Serializable
data class SeasonalStoreDto(
    val id: String = "",
    val name: String = "",
    val type: String = "",
    val provinceCode: String = "",
    val province: String = "",
    val provinceType: String = "",
    val region: String = "",
    val area: String = "",
    val address: String = "",
    val location: ShopLocationDto = ShopLocationDto(),
    val rating: Double = 0.0,
    val reviewCount: Int = 0,
    val priceLevel: Int = 0,
    val phone: String = "",
    val openHours: String = "",
    val isOpenNow: Boolean = false,
    val hasOnlineStore: Boolean = false,
    val onlinePlatform: String = "",
    val onlineUrl: String = "",
    val coverImage: String = "",
    val tags: List<String> = emptyList(),

    @SerialName("seasonCollections")
    val seasons: List<String> = emptyList(),

    val distancePriority: String = "",
    val sourceType: String = "",
    val sourceNote: String = "",
    val products: List<SeasonalProductDto> = emptyList()
) {
    val lat: Double
        get() = location.lat

    val lng: Double
        get() = location.lng

    val distanceText: String
        get() = when (distancePriority) {
            "near_da_nang_center" -> "Near Da Nang Center"
            "near_beach" -> "Near the Beach"
            "da_nang_suburban" -> "Da Nang Suburbs"
            "central_vietnam" -> "Central Vietnam"
            "central_highlands" -> "Central Highlands"
            "mekong" -> "Mekong Delta"
            else -> area
        }
}

@Serializable
data class ShopLocationDto(
    val lat: Double = 0.0,
    val lng: Double = 0.0
)

@Serializable
data class SeasonalProductDto(
    val id: String = "",
    val name: String = "",
    val category: String = "",
    val season: String = "",
    val price: Int = 0,
    val currency: String = "VND",
    val colors: List<String> = emptyList(),
    val sizes: List<String> = emptyList(),
    val stock: Int = 0,
    val soldMonthly: Int = 0,
    val image: String = ""
) {
    val priceRange: String
        get() = "${price / 1000}k"
}