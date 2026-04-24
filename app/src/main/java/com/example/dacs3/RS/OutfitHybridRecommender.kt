package com.example.dacs3.RS

import com.example.dacs3.connectDB.ClothingItem
import com.example.dacs3.connectDB.Outfit
import com.example.dacs3.connectDB.Profile
import org.jetbrains.kotlinx.dl.api.core.Sequential

class OutfitHybridRecommender(private val model: Sequential) {

    fun recommend(
        profile: Profile,
        wardrobe: List<ClothingItem>,
        targetSeason: String
    ): List<Pair<Outfit, Float>> {

        // --- BƯỚC 1: LỌC CỨNG (KOTLIN) ---
        // Chỉ lấy đồ đúng mùa
        val filteredItems = wardrobe.filter { it.seasons?.contains(targetSeason) == true }

        val tops = filteredItems.filter { it.category.equals("top", true) }.shuffled().take(10)
        val bottoms = filteredItems.filter { it.category.equals("bottom", true) }.shuffled().take(10)
        val shoes = filteredItems.filter { it.category.equals("shoes", true) }.shuffled().take(5)

        val rankedOutfits = mutableListOf<Pair<Outfit, Float>>()

        // --- BƯỚC 2: AI SCORER (KOTLINDL) ---
        for (t in tops) {
            for (b in bottoms) {
                for (s in shoes) {
                    // Tạo vector đặc trưng cho bộ 3 món này
                    val features = floatArrayOf(
                        FeatureEncoder.encodeBodyShape(profile.bodyShape),
                        FeatureEncoder.encodeColor(t.mainColor),
                        FeatureEncoder.encodeColor(b.mainColor),
                        FeatureEncoder.encodeColor(s.mainColor),
                        FeatureEncoder.encodeCategory(t.category),
                        FeatureEncoder.encodeCategory(b.category)
                    )

                    // AI dự đoán độ phù hợp (0.0 -> 1.0)
                    val score = model.predict(features).toFloat()

                    rankedOutfits.add(Outfit(t, b, s) to score)
                }
            }
        }

        // Sắp xếp theo điểm giảm dần và lấy Top 5
        return rankedOutfits.sortedByDescending { it.second }.take(5)
    }
}