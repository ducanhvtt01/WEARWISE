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
        currentSeason: String,
        frequencyMap: Map<String, Int> = emptyMap(),
        feedbackMap: Map<String, Int> = emptyMap(),
        socialTrendContext: String = ""
    ): List<Outfit> {

        // Bước 1: Lọc đồ theo mùa (Pre-filtering)
        val availableItems = userWardrobe.filter { item ->
            item.seasons?.contains(currentSeason) == true || item.seasons.isNullOrEmpty()
        }

        // Bước 2: Tối ưu hiệu năng - Thu hẹp danh sách ứng viên (Candidate Pruning)
        // Thay vì duyệt hàng nghìn món, ta chỉ lấy Top 20 món tốt nhất ở mỗi loại
        val tops = rankIndividualItems(availableItems.filter { it.category.equals("Top", true) }, userProfile, frequencyMap, feedbackMap, socialTrendContext).take(20)
        val bottoms = rankIndividualItems(availableItems.filter { it.category.equals("Bottom", true) }, userProfile, frequencyMap, feedbackMap, socialTrendContext).take(20)
        val shoes = rankIndividualItems(availableItems.filter { it.category.equals("Shoes", true) }, userProfile, frequencyMap, feedbackMap, socialTrendContext).take(20)

        val rankedOutfits = mutableListOf<Pair<Outfit, Float>>()

        // Bước 3: Duyệt và tính điểm (Scoring) - Tối đa 20 * 20 * 20 = 8,000 lần (Rất nhanh)
        for (top in tops) {
            for (bottom in bottoms) {
                for (shoe in shoes) {
                    val score = calculateScore(userProfile, top, bottom, shoe, frequencyMap, feedbackMap, socialTrendContext)

                    if (score > 0.4) {
                        rankedOutfits.add(Outfit(listOf(top, bottom, shoe)) to score)
                    }
                }
            }
        }

        // Bước 4: Sắp xếp theo điểm cao nhất
        return rankedOutfits
            .sortedByDescending { it.second }
            .map { it.first }
            .take(15)
    }

    /**
     * Hàm phụ trợ để xếp hạng sơ bộ từng món đồ riêng lẻ.
     * Giúp giảm tải cho vòng lặp phối đồ chính.
     */
    private fun rankIndividualItems(
        items: List<ClothingItem>,
        profile: Profile,
        frequencyMap: Map<String, Int>,
        feedbackMap: Map<String, Int>,
        socialTrendContext: String
    ): List<ClothingItem> {
        return items.map { item ->
            var itemScore = 0f
            
            // Điểm cơ bản: Màu sắc yêu thích và độ tương đồng
            val idealVector = FeatureEncoder.getIdealProfileVector(profile, item.category)
            val sim = SimilarityMath.cosineSimilarity(FeatureEncoder.getItemVector(item), idealVector)
            if (!sim.isNaN()) itemScore += sim

            // Điểm phản hồi: Ưu tiên Like, loại bỏ Dislike
            when (feedbackMap[item.id]) {
                1 -> itemScore += 0.5f
                -1 -> itemScore -= 2.0f
            }

            // Điểm xu hướng
            if (socialTrendContext.contains(item.mainColor ?: "", true)) itemScore += 0.2f
            
            item to itemScore
        }.sortedByDescending { it.second }.map { it.first }
    }

    private fun calculateScore(
        profile: Profile, 
        top: ClothingItem, 
        bottom: ClothingItem, 
        shoe: ClothingItem,
        frequencyMap: Map<String, Int>,
        feedbackMap: Map<String, Int>,
        socialTrendContext: String = "" // New
    ): Float {
        var score = 0f

        // 1. Độ tương đồng với Sở thích (Màu sắc & Loại trang phục) (Dùng Cosine Similarity)
        val idealTopVector = FeatureEncoder.getIdealProfileVector(profile, "Top")
        val idealBottomVector = FeatureEncoder.getIdealProfileVector(profile, "Bottom")
        
        val topSim = SimilarityMath.cosineSimilarity(FeatureEncoder.getItemVector(top), idealTopVector)
        val bottomSim = SimilarityMath.cosineSimilarity(FeatureEncoder.getItemVector(bottom), idealBottomVector)
        
        if (!topSim.isNaN()) score += topSim * 0.1f
        if (!bottomSim.isNaN()) score += bottomSim * 0.1f

        // 2. Phối màu hài hòa (0.4)
        if (colorRules[top.mainColor]?.contains(bottom.mainColor) == true) score += 0.3f
        if (colorRules[bottom.mainColor]?.contains(shoe.mainColor) == true) score += 0.1f

        // 3. Phù hợp dáng người (0.1)
        if (profile.bodyShape == "Inverted Triangle" && top.category.contains("V-neck", true)) score += 0.1f
        
        // 4. Tần suất mặc đồ - Ưu tiên những món hay mặc (0.3)
        val topFreq = frequencyMap[top.id] ?: 0
        val bottomFreq = frequencyMap[bottom.id] ?: 0
        if (topFreq > 0) score += 0.15f * (topFreq.toFloat() / 10f).coerceAtMost(1f)
        if (bottomFreq > 0) score += 0.15f * (bottomFreq.toFloat() / 10f).coerceAtMost(1f)
        
        // 5. Phản hồi từ người dùng (Feedback Loop)
        // Nếu có món đồ bị Dislike (-1), trừ điểm cực nặng để không gợi ý món đó nữa
        if (feedbackMap[top.id] == -1) score -= 1.0f
        if (feedbackMap[bottom.id] == -1) score -= 1.0f
        if (feedbackMap[shoe.id] == -1) score -= 1.0f
        
        // Ngược lại, nếu được Like (1), cộng thêm điểm ưu tiên
        if (feedbackMap[top.id] == 1) score += 0.2f
        if (feedbackMap[bottom.id] == 1) score += 0.2f
        if (feedbackMap[shoe.id] == 1) score += 0.2f
        
        // 6. Social Trend (Gợi ý cộng đồng) (0.15)
        // Nếu bộ đồ này chứa các thành phần đang là "Trend", cộng thêm điểm
        if (socialTrendContext.isNotBlank()) {
            if (socialTrendContext.contains(top.mainColor ?: "", ignoreCase = true)) score += 0.05f
            if (socialTrendContext.contains(bottom.mainColor ?: "", ignoreCase = true)) score += 0.05f
            if (socialTrendContext.contains(top.category, ignoreCase = true)) score += 0.05f
        }

        return score
    }

    // Tính điểm tương đồng (Dùng cho Style Matching) bằng Cosine Similarity
    fun getSimilarityScore(item: ClothingItem, targetCategory: String, targetColor: String): Float {
        // Tạo một vector mục tiêu (One-Hot)
        val targetItem = ClothingItem(
            id = "target",
            userId = "target",
            clothes_name = "target",
            imageUrl = "",
            category = targetCategory,
            mainColor = targetColor
        )
        
        val itemVector = FeatureEncoder.getItemVector(item)
        val targetVector = FeatureEncoder.getItemVector(targetItem)
        
        val sim = SimilarityMath.cosineSimilarity(itemVector, targetVector)
        return if (sim.isNaN()) 0f else sim
    }
}