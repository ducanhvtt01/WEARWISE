package com.example.dacs3.RS

import android.graphics.Bitmap
import com.example.dacs3.connectDB.ClothingItem
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content

class StyleMatcherService(private val geminiApiKey: String) {

    /**
     * Phân tích ảnh Idol và tìm các món đồ tương đồng trong tủ đồ của người dùng.
     */
    suspend fun matchCelebrityStyle(
        image: Bitmap,
        userWardrobe: List<ClothingItem>,
        feedbackMap: Map<String, Int> = emptyMap()
    ): List<ClothingItem> {
        val generativeModel = GenerativeModel(
            modelName = "gemini-3.1-flash-lite", // Updated to Gemini 3.1 Flash Lite
            apiKey = geminiApiKey
        )

        val prompt = """
            Role: You are a high-end AI fashion consultant.
            Task: Analyze the outfit worn by the person in the provided image with surgical precision.
            
            Instructions:
            1. Deconstruct the outfit into 3 core categories: 'Top', 'Bottom', and 'Shoes'.
            2. For each category, identify the single most accurate dominant color. Use professional color names (e.g., 'Navy' instead of 'Dark Blue', 'Beige' instead of 'Light Brown', 'Khaki', 'Charcoal').
            
            Output Requirements (CRITICAL):
            - Return ONLY a comma-separated list in the format CATEGORY:COLOR.
            - Valid Categories: Top, Bottom, Shoes.
            - Do NOT add markdown, explanations, or any extra characters.
            
            Example Output: Top:Navy, Bottom:Beige, Shoes:White
        """.trimIndent()

        val inputContent = content {
            image(image)
            text(prompt)
        }

        try {
            val response = generativeModel.generateContent(inputContent)
            val analysis = response.text ?: ""
            
            // Tách kết quả: Top:Blue, Bottom:Black...
            val targetItems = analysis.split(",").mapNotNull { 
                val parts = it.trim().split(":")
                if (parts.size == 2) parts[0].trim() to parts[1].trim() else null
            }

            val rs = OutfitRecommendationService()
            val matches = mutableListOf<ClothingItem>()

            for ((category, color) in targetItems) {
                // Tìm món đồ trong tủ đồ có Category khớp, KHÔNG bị Dislike (-1), và độ tương đồng màu sắc cao nhất
                val bestMatch = userWardrobe
                    .filter { it.category.equals(category, true) && (feedbackMap[it.id] ?: 0) >= 0 }
                    .maxByOrNull { rs.getSimilarityScore(it, category, color) }
                
                if (bestMatch != null && !matches.contains(bestMatch)) {
                    matches.add(bestMatch)
                }
            }
            
            return matches
        } catch (e: Exception) {
            e.printStackTrace()
            return emptyList()
        }
    }
}
