package com.example.dacs3.RS

import com.example.dacs3.connectDB.ClothingItem
import com.example.dacs3.connectDB.Profile

object FeatureEncoder {
    
    // --- SCALAR ENCODING (For KotlinDL Sequential Model) ---
    fun encodeCategory(cat: String): Float = when(cat.lowercase()) {
        "top" -> 0.0f
        "bottom" -> 0.5f
        "shoes" -> 1.0f
        else -> -1.0f
    }

    fun encodeBodyShape(shape: String): Float = when(shape.lowercase()) {
        "rectangle" -> 0.1f
        "pear" -> 0.2f
        "apple" -> 0.3f
        "hourglass" -> 0.4f
        "inverted triangle" -> 0.5f
        else -> 0.0f
    }

    fun encodeColor(color: String?): Float {
        val colors = listOf("white", "black", "blue", "red", "gray", "beige", "denim")
        val index = colors.indexOf(color?.lowercase())
        return if (index != -1) index.toFloat() / colors.size else 0.5f
    }

    // --- VECTOR ENCODING (For Cosine Similarity) ---
    
    private val ALL_CATEGORIES = listOf("top", "bottom", "shoes", "outerwear", "accessories", "dresses")
    private val ALL_COLORS = listOf("white", "black", "blue", "navy", "red", "pink", "gray", "beige", "green", "yellow", "brown", "purple", "denim")

    /**
     * Tạo One-Hot Vector cho một món đồ quần áo.
     * Vector = [One-Hot Category] + [One-Hot Color]
     * Kích thước = size(ALL_CATEGORIES) + size(ALL_COLORS)
     */
    fun getItemVector(item: ClothingItem): FloatArray {
        val vector = FloatArray(ALL_CATEGORIES.size + ALL_COLORS.size)
        
        // Encode Category
        val catIndex = ALL_CATEGORIES.indexOf(item.category.lowercase())
        if (catIndex != -1) vector[catIndex] = 1.0f
        
        // Encode Color
        val colorIndex = ALL_COLORS.indexOf(item.mainColor?.lowercase() ?: "")
        if (colorIndex != -1) vector[ALL_CATEGORIES.size + colorIndex] = 1.0f
        
        return vector
    }

    /**
     * Tạo "Ideal Vector" cho người dùng dựa trên sở thích màu sắc của họ.
     * Nếu món đồ sắp được gợi ý khớp với Ideal Vector, Cosine Similarity sẽ cao.
     */
    fun getIdealProfileVector(profile: Profile, targetCategory: String): FloatArray {
        val vector = FloatArray(ALL_CATEGORIES.size + ALL_COLORS.size)
        
        // Thiết lập Category mà AI đang muốn tìm kiếm làm mục tiêu (trọng số 1.0)
        val catIndex = ALL_CATEGORIES.indexOf(targetCategory.lowercase())
        if (catIndex != -1) vector[catIndex] = 1.0f
        
        // Thiết lập các màu yêu thích của người dùng (chia đều trọng số)
        val favColorsCount = profile.favoriteColors.size
        if (favColorsCount > 0) {
            val weight = 1.0f / favColorsCount
            profile.favoriteColors.forEach { color ->
                val colorIndex = ALL_COLORS.indexOf(color.lowercase())
                if (colorIndex != -1) {
                    vector[ALL_CATEGORIES.size + colorIndex] = weight
                }
            }
        }
        
        return vector
    }
}