package com.example.dacs3.RS

object FeatureEncoder {
    // Chuyển đổi Category sang Float
    fun encodeCategory(cat: String): Float = when(cat.lowercase()) {
        "top" -> 0.0f
        "bottom" -> 0.5f
        "shoes" -> 1.0f
        else -> -1.0f
    }

    // Chuyển đổi Body Shape sang Float
    fun encodeBodyShape(shape: String): Float = when(shape.lowercase()) {
        "rectangle" -> 0.1f
        "pear" -> 0.2f
        "apple" -> 0.3f
        "hourglass" -> 0.4f
        "inverted triangle" -> 0.5f
        else -> 0.0f
    }

    // Đơn giản hóa màu sắc bằng cách bóc tách mã màu hoặc dùng ID
    fun encodeColor(color: String?): Float {
        val colors = listOf("white", "black", "blue", "red", "gray", "beige", "denim")
        val index = colors.indexOf(color?.lowercase())
        return if (index != -1) index.toFloat() / colors.size else 0.5f
    }
}