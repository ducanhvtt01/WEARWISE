package com.example.dacs3.RS

import kotlin.math.sqrt

object SimilarityMath {

    /**
     * Tính Jaccard Similarity giữa 2 tập hợp (Set).
     * Phù hợp để so sánh danh sách sở thích rời rạc (Ví dụ: Các phong cách yêu thích).
     * Giá trị trả về từ 0.0 (hoàn toàn khác biệt) đến 1.0 (hoàn toàn giống nhau).
     */
    fun <T> jaccardSimilarity(setA: Set<T>, setB: Set<T>): Float {
        if (setA.isEmpty() && setB.isEmpty()) return 1.0f
        if (setA.isEmpty() || setB.isEmpty()) return 0.0f

        val intersection = setA.intersect(setB).size.toFloat()
        val union = setA.union(setB).size.toFloat()

        return intersection / union
    }

    /**
     * Tính Cosine Similarity giữa 2 vector.
     * Phù hợp để tính sự tương đồng trong không gian n-chiều (Ví dụ: Vector màu sắc, loại đồ).
     * Giá trị trả về từ -1.0 (ngược hướng) đến 1.0 (cùng hướng).
     */
    fun cosineSimilarity(vectorA: FloatArray, vectorB: FloatArray): Float {
        require(vectorA.size == vectorB.size) { "Vectors must have the same length" }
        
        var dotProduct = 0.0f
        var normA = 0.0f
        var normB = 0.0f

        for (i in vectorA.indices) {
            dotProduct += vectorA[i] * vectorB[i]
            normA += vectorA[i] * vectorA[i]
            normB += vectorB[i] * vectorB[i]
        }

        if (normA == 0.0f || normB == 0.0f) return 0.0f

        return dotProduct / (sqrt(normA) * sqrt(normB))
    }
}
