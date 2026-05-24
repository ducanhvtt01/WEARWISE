package com.example.dacs3.connectDB

object WardrobeHelper {
    /**
     * Kiểm tra xem một món đồ có đủ điều kiện để tự động chuyển từ trạng thái
     * WORN hoặc IN_WASH sang AVAILABLE hay không (sau 3 ngày kể từ lần cuối mặc).
     * 
     * @param item Món đồ cần kiểm tra.
     * @param currentDate Ngày hiện tại (có thể truyền ngày giả vào để test).
     * @return Trả về bản sao của ClothingItem với status "AVAILABLE" nếu đủ điều kiện, ngược lại trả về null.
     */
    fun processCooldown(item: ClothingItem, currentDate: java.util.Date = java.util.Date()): ClothingItem? {
        if (item.status.uppercase() !in listOf("WORN", "IN_WASH")) return null
        val lastWorn = item.lastWornDate ?: return null

        try {
            // Lấy 10 ký tự đầu tiên dạng YYYY-MM-DD
            if (lastWorn.length < 10) return null
            val dateString = lastWorn.substring(0, 10)
            
            val formatter = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
            val refDate = formatter.parse(dateString) ?: return null
            
            val diffInMillies = kotlin.math.abs(currentDate.time - refDate.time)
            val diffInDays = java.util.concurrent.TimeUnit.DAYS.convert(
                diffInMillies,
                java.util.concurrent.TimeUnit.MILLISECONDS
            )
            
            if (diffInDays >= 3) {
                return item.copy(status = "AVAILABLE")
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }
}
