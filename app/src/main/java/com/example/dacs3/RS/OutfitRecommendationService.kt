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
        socialTrendContext: String = "",
        recentClothingIds: Set<String> = emptySet(),
        isExplorationMode: Boolean = false
    ): List<Outfit> {

        // Bước 1: Lọc đồ theo mùa (Pre-filtering)
        val availableItems = userWardrobe.filter { item ->
            item.seasons?.contains(currentSeason) == true || item.seasons.isNullOrEmpty()
        }

        // Bước 2: Tối ưu hiệu năng - Thu hẹp danh sách ứng viên (Candidate Pruning)
        val tops = rankIndividualItems(availableItems.filter { it.category.equals("Top", true) }, userProfile, frequencyMap, feedbackMap, socialTrendContext, recentClothingIds, isExplorationMode).take(20)
        val bottoms = rankIndividualItems(availableItems.filter { it.category.equals("Bottom", true) }, userProfile, frequencyMap, feedbackMap, socialTrendContext, recentClothingIds, isExplorationMode).take(20)
        val shoes = rankIndividualItems(availableItems.filter { it.category.equals("Shoes", true) }, userProfile, frequencyMap, feedbackMap, socialTrendContext, recentClothingIds, isExplorationMode).take(20)

        val rankedOutfits = mutableListOf<Pair<Outfit, Float>>()

        // Bước 3: Duyệt và tính điểm (Scoring) - Tối đa 20 * 20 * 20 = 8,000 lần
        for (top in tops) {
            for (bottom in bottoms) {
                for (shoe in shoes) {
                    val score = calculateScore(userProfile, top, bottom, shoe, frequencyMap, feedbackMap, socialTrendContext, recentClothingIds, isExplorationMode)

                    if (score > 0.3f) { // Adjusted from 0.4f to be slightly more permissive for exploration
                        rankedOutfits.add(Outfit(listOf(top, bottom, shoe)) to score)
                    }
                }
            }
        }

        // Bước 4: Sắp xếp và Áp dụng thuật toán Re-ranking MMR để đa dạng hóa
        val sortedCandidates = rankedOutfits.sortedByDescending { it.second }
        return applyMMR(sortedCandidates, topK = 15, lambda = if (isExplorationMode) 0.5f else 0.7f)
    }

    /**
     * Hàm phụ trợ để xếp hạng sơ bộ từng món đồ riêng lẻ.
     */
    private fun rankIndividualItems(
        items: List<ClothingItem>,
        profile: Profile,
        frequencyMap: Map<String, Int>,
        feedbackMap: Map<String, Int>,
        socialTrendContext: String,
        recentClothingIds: Set<String>,
        isExplorationMode: Boolean
    ): List<ClothingItem> {
        return items.map { item ->
            var itemScore = 0f
            
            // Điểm cơ bản: Màu sắc yêu thích và độ tương đồng
            val idealVector = FeatureEncoder.getIdealProfileVector(profile, item.category)
            val sim = SimilarityMath.cosineSimilarity(FeatureEncoder.getItemVector(item), idealVector)
            if (!sim.isNaN()) {
                // Trong chế độ khám phá, giảm 50% tầm ảnh hưởng của sở thích cố định
                itemScore += if (isExplorationMode) sim * 0.5f else sim
            }

            // Điểm phản hồi: Ưu tiên Like, loại bỏ Dislike
            when (feedbackMap[item.id]) {
                1 -> itemScore += 0.5f
                -1 -> itemScore -= 2.0f
            }

            // Điểm xu hướng
            if (socialTrendContext.contains(item.mainColor ?: "", true)) itemScore += 0.2f
            
            // Phạt đồ vừa mới mặc gần đây (Recency Penalty)
            if (item.id != null && recentClothingIds.contains(item.id)) {
                itemScore -= 0.4f
            }

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
        socialTrendContext: String = "",
        recentClothingIds: Set<String> = emptySet(),
        isExplorationMode: Boolean = false
    ): Float {
        var score = 0f

        // 1. Độ tương đồng với Sở thích (Dùng Cosine Similarity)
        val idealTopVector = FeatureEncoder.getIdealProfileVector(profile, "Top")
        val idealBottomVector = FeatureEncoder.getIdealProfileVector(profile, "Bottom")
        
        val topSim = SimilarityMath.cosineSimilarity(FeatureEncoder.getItemVector(top), idealTopVector)
        val bottomSim = SimilarityMath.cosineSimilarity(FeatureEncoder.getItemVector(bottom), idealBottomVector)
        
        // Giảm trọng số sở thích cũ trong chế độ khám phá để tăng cơ hội cho phong cách mới
        val preferenceWeight = if (isExplorationMode) 0.05f else 0.1f
        if (!topSim.isNaN()) score += topSim * preferenceWeight
        if (!bottomSim.isNaN()) score += bottomSim * preferenceWeight

        // 2. Phối màu hài hòa (0.4)
        if (colorRules[top.mainColor]?.contains(bottom.mainColor) == true) score += 0.3f
        if (colorRules[bottom.mainColor]?.contains(shoe.mainColor) == true) score += 0.1f

        // 3. Phù hợp dáng người (0.1)
        if (profile.bodyShape == "Inverted Triangle" && top.category.contains("V-neck", true)) score += 0.1f
        
        // 4. Tần suất mặc đồ (0.3)
        val topFreq = frequencyMap[top.id] ?: 0
        val bottomFreq = frequencyMap[bottom.id] ?: 0
        if (isExplorationMode) {
            // Chế độ khám phá: Phạt các món đồ mặc quá nhiều để lôi đồ ít mặc ra sử dụng
            if (topFreq > 0) score -= 0.15f * (topFreq.toFloat() / 10f).coerceAtMost(1f)
            if (bottomFreq > 0) score -= 0.15f * (bottomFreq.toFloat() / 10f).coerceAtMost(1f)
        } else {
            // Chế độ bình thường: Ưu tiên đồ hay mặc
            if (topFreq > 0) score += 0.15f * (topFreq.toFloat() / 10f).coerceAtMost(1f)
            if (bottomFreq > 0) score += 0.15f * (bottomFreq.toFloat() / 10f).coerceAtMost(1f)
        }
        
        // 5. Phản hồi từ người dùng (Feedback Loop)
        if (feedbackMap[top.id] == -1) score -= 1.0f
        if (feedbackMap[bottom.id] == -1) score -= 1.0f
        if (feedbackMap[shoe.id] == -1) score -= 1.0f
        
        if (feedbackMap[top.id] == 1) score += 0.2f
        if (feedbackMap[bottom.id] == 1) score += 0.2f
        if (feedbackMap[shoe.id] == 1) score += 0.2f
        
        // Phạt đồ mới mặc gần đây để tránh lặp (Temporal Recency Penalty)
        if (top.id != null && recentClothingIds.contains(top.id)) score -= 0.4f
        if (bottom.id != null && recentClothingIds.contains(bottom.id)) score -= 0.4f
        if (shoe.id != null && recentClothingIds.contains(shoe.id)) score -= 0.4f
        
        // 6. Social Trend (0.15)
        if (socialTrendContext.isNotBlank()) {
            if (socialTrendContext.contains(top.mainColor ?: "", ignoreCase = true)) score += 0.05f
            if (socialTrendContext.contains(bottom.mainColor ?: "", ignoreCase = true)) score += 0.05f
            if (socialTrendContext.contains(top.category, ignoreCase = true)) score += 0.05f
        }

        return score
    }

    /**
     * Tính toán độ tương đồng giữa hai bộ Outfit (Top, Bottom, Shoe).
     * Dựa trên trung bình cộng độ tương đồng Cosine của các món đồ thành phần.
     */
    fun calculateOutfitSimilarity(a: Outfit, b: Outfit): Float {
        var totalSim = 0.0f
        var count = 0

        val aTop = a.items.find { it.category.equals("Top", true) }
        val bTop = b.items.find { it.category.equals("Top", true) }
        if (aTop != null && bTop != null) {
            val sim = SimilarityMath.cosineSimilarity(FeatureEncoder.getItemVector(aTop), FeatureEncoder.getItemVector(bTop))
            if (!sim.isNaN()) {
                totalSim += sim
                count++
            }
        }

        val aBottom = a.items.find { it.category.equals("Bottom", true) }
        val bBottom = b.items.find { it.category.equals("Bottom", true) }
        if (aBottom != null && bBottom != null) {
            val sim = SimilarityMath.cosineSimilarity(FeatureEncoder.getItemVector(aBottom), FeatureEncoder.getItemVector(bBottom))
            if (!sim.isNaN()) {
                totalSim += sim
                count++
            }
        }

        val aShoes = a.items.find { it.category.equals("Shoes", true) }
        val bShoes = b.items.find { it.category.equals("Shoes", true) }
        if (aShoes != null && bShoes != null) {
            val sim = SimilarityMath.cosineSimilarity(FeatureEncoder.getItemVector(aShoes), FeatureEncoder.getItemVector(bShoes))
            if (!sim.isNaN()) {
                totalSim += sim
                count++
            }
        }

        return if (count > 0) totalSim / count else 0.0f
    }

    /**
     * Thuật toán Maximal Marginal Relevance (MMR) để xếp hạng lại và đa dạng hóa gợi ý.
     */
    fun applyMMR(
        candidates: List<Pair<Outfit, Float>>,
        topK: Int = 15,
        lambda: Float = 0.7f
    ): List<Outfit> {
        if (candidates.isEmpty()) return emptyList()

        val selected = mutableListOf<Pair<Outfit, Float>>()
        val remaining = candidates.toMutableList()

        // 1. Chọn bộ đầu tiên có Relevance cao nhất
        val first = remaining.maxByOrNull { it.second }!!
        selected.add(first)
        remaining.remove(first)

        // 2. Chọn các bộ tiếp theo dựa trên công thức MMR để tăng tính khám phá
        while (selected.size < topK && remaining.isNotEmpty()) {
            var bestCandidate: Pair<Outfit, Float>? = null
            var bestMmrScore = -Float.MAX_VALUE

            for (candidate in remaining) {
                val relevance = candidate.second
                
                // Tìm độ tương đồng cao nhất với bất kỳ bộ đồ nào đã được chọn trước đó
                var maxSimilarity = 0.0f
                for (chosen in selected) {
                    val sim = calculateOutfitSimilarity(candidate.first, chosen.first)
                    if (sim > maxSimilarity) {
                        maxSimilarity = sim
                    }
                }

                // Công thức MMR
                val mmrScore = lambda * relevance - (1f - lambda) * maxSimilarity

                if (mmrScore > bestMmrScore) {
                    bestMmrScore = mmrScore
                    bestCandidate = candidate
                }
            }

            if (bestCandidate != null) {
                selected.add(bestCandidate)
                remaining.remove(bestCandidate)
            } else {
                break
            }
        }

        return selected.map { it.first }
    }

    // Tính điểm tương đồng (Dùng cho Style Matching) bằng Cosine Similarity
    fun getSimilarityScore(item: ClothingItem, targetCategory: String, targetColor: String): Float {
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
