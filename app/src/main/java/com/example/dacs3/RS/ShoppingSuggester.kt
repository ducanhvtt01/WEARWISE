package com.example.dacs3.RS

import com.example.dacs3.connectDB.ClothingItem
import com.example.dacs3.connectDB.Profile
import java.net.URLEncoder

data class ShoppingRecommendation(
    val title: String,
    val message: String,
    val searchKeyword: String,
    val shopeeUrl: String,
    val mapSearchQuery: String
)

class ShoppingSuggester {

    /**
     * Phân tích tủ đồ hiện tại để tìm ra những món đồ còn thiếu (Gap Analysis)
     * và gợi ý mua sắm tích hợp với Shopee/Bản đồ.
     */
    fun suggestMissingItems(profile: Profile, wardrobe: List<ClothingItem>): List<ShoppingRecommendation> {
        val recommendations = mutableListOf<ShoppingRecommendation>()
        
        // 1. Phân tích các danh mục phổ biến
        val categories = wardrobe.map { it.category.lowercase() }
        val hasJacket = categories.any { it.contains("jacket") || it.contains("áo khoác") }
        val hasOuterwear = categories.any { it.contains("blazer") || it.contains("hoodie") }
        val hasAccessories = categories.any { it.contains("bag") || it.contains("watch") || it.contains("phụ kiện") }
        
        // 2. Lấy sở thích cá nhân
        val favoriteStyle = profile.favoriteStyles.firstOrNull() ?: "Casual"
        val favoriteColor = profile.favoriteColors.firstOrNull() ?: "Black"

        // Gợi ý Áo khoác nếu chưa có
        if (!hasJacket && !hasOuterwear) {
            val keyword = "$favoriteColor $favoriteStyle Jacket"
            recommendations.add(
                ShoppingRecommendation(
                    title = "Elevate your $favoriteStyle style!",
                    message = "We noticed you have many t-shirts but lack a suitable jacket. A $favoriteColor jacket would make your outfit look much more professional and stylish.",
                    searchKeyword = keyword,
                    shopeeUrl = "https://shopee.vn/search?keyword=${URLEncoder.encode(keyword, "UTF-8")}",
                    mapSearchQuery = "fashion store"
                )
            )
        }

        // Gợi ý Phụ kiện nếu tủ đồ đã lớn nhưng chưa có phụ kiện
        if (wardrobe.size > 5 && !hasAccessories) {
            val keyword = "Men's $favoriteStyle Watch"
            recommendations.add(
                ShoppingRecommendation(
                    title = "Add some accessories",
                    message = "Your wardrobe is already very diverse! Try adding a watch or a bag to create a unique highlight for your personal style.",
                    searchKeyword = keyword,
                    shopeeUrl = "https://shopee.vn/search?keyword=${URLEncoder.encode(keyword, "UTF-8")}",
                    mapSearchQuery = "watch store"
                )
            )
        }

        return recommendations
    }
}
