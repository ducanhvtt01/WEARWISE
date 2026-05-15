package com.example.dacs3.RS

import com.example.dacs3.connectDB.ClothingItem
import com.example.dacs3.connectDB.Outfit
import com.example.dacs3.connectDB.Profile

class OutfitRecommendationService {

    // 1. Quy tắc phối màu cơ bản (Color Harmony)
    private val colorRules = mapOf(
        "White" to listOf("Black", "Blue", "Navy", "Gray", "Beige"),
        "Black" to listOf("White", "Gray", "Red", "Blue", "Yellow"),
        "Blue" to listOf("White", "Beige", "Black", "Gray"),
        "Gray" to listOf("White", "Black", "Pink", "Blue", "Red"),
        "Beige" to listOf("White", "Navy", "Brown", "Emerald", "Black"),
        "Navy" to listOf("White", "Beige", "Gold", "Gray", "Red"),
        "Red" to listOf("Black", "White", "Gray", "Navy", "Beige"),
        "Pink" to listOf("Gray", "White", "Navy", "Brown", "Mint"),
        "Green" to listOf("White", "Beige", "Black", "Brown", "Yellow"),
        "Yellow" to listOf("Black", "Navy", "Gray", "White", "Purple"),
        "Brown" to listOf("Beige", "White", "Green", "Gold", "Turquoise"),
        "Purple" to listOf("Gray", "White", "Beige", "Yellow", "Silver")
    )

    fun getRecommendations(
        userProfile: Profile,
        userWardrobe: List<ClothingItem>,
        currentSeason: String
    ): List<Outfit> {

        // Bước 1: Lọc đồ theo mùa và sở thích (Pre-filtering)
        val availableItems = userWardrobe.filter { item ->
            item.seasons?.contains(currentSeason) == true
        }

        val tops = availableItems.filter { it.category.equals("Top", true) }
        val bottoms = availableItems.filter { it.category.equals("Bottom", true) }
        val shoes = availableItems.filter { it.category.equals("Shoes", true) }

        val possibleOutfits = mutableListOf<Outfit>()

        // Bước 2: Duyệt và tính điểm (Scoring)
        for (top in tops) {
            for (bottom in bottoms) {
                for (shoe in shoes) {
                    val score = calculateScore(userProfile, top, bottom, shoe)

                    // Nếu điểm > ngưỡng nhất định (ví dụ 0.5) thì thêm vào danh sách
                    if (score > 0.6) {
                        possibleOutfits.add(Outfit(listOf(top, bottom, shoe)))
                    }
                }
            }
        }

        // Bước 3: Sắp xếp theo điểm cao nhất
        return possibleOutfits.sortedByDescending { /* Logic điểm ở đây */ 1 }
    }

    private fun calculateScore(profile: Profile, top: ClothingItem, bottom: ClothingItem, shoe: ClothingItem): Float {
        var score = 0f

        // Cộng điểm nếu màu sắc thuộc danh sách màu yêu thích của User
        if (profile.favoriteColors.contains(top.mainColor)) score += 0.2f

        // Cộng điểm nếu màu sắc top và bottom phối hợp tốt (dựa trên colorRules)
        if (colorRules[top.mainColor]?.contains(bottom.mainColor) == true) score += 0.4f

        // Cộng điểm nếu item phù hợp với dịp (Occasions) - Giả sử user đang cần đi làm
        if (top.occasions?.contains("Work") == true && bottom.occasions?.contains("Work") == true) score += 0.3f

        // Logic dựa trên Body Shape (Ví dụ: Dáng người tam giác ngược hợp với loại áo nào đó)
        if (profile.bodyShape == "Inverted Triangle" && top.category == "V-neck") score += 0.1f

        return score
    }
}