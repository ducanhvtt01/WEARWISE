package com.example.dacs3.connectDB

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class WardrobeHelperTest {

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault())

    private fun createMockItem(status: String, lastWornDate: String?): ClothingItem {
        return ClothingItem(
            id = "test-id",
            userId = "test-user",
            imageUrl = "http://test.com",
            clothes_name = "T-Shirt",
            category = "Top",
            mainColor = "Red",
            seasons = listOf("Summer"),
            status = status,
            lastWornDate = lastWornDate,
            createdAt = "2026-05-23"
        )
    }

    @Test
    fun `processCooldown returns AVAILABLE when item is WORN and exactly 3 days passed`() {
        val calendar = Calendar.getInstance()
        val today = calendar.time
        
        calendar.add(Calendar.DAY_OF_YEAR, -3)
        val threeDaysAgoStr = dateFormat.format(calendar.time)

        val item = createMockItem("WORN", threeDaysAgoStr)
        
        val result = WardrobeHelper.processCooldown(item, today)
        
        assertEquals("AVAILABLE", result?.status)
    }

    @Test
    fun `processCooldown returns null when item is WORN but only 2 days passed`() {
        val calendar = Calendar.getInstance()
        val today = calendar.time
        
        calendar.add(Calendar.DAY_OF_YEAR, -2)
        val twoDaysAgoStr = dateFormat.format(calendar.time)

        val item = createMockItem("WORN", twoDaysAgoStr)
        
        val result = WardrobeHelper.processCooldown(item, today)
        
        assertNull(result)
    }

    @Test
    fun `processCooldown returns AVAILABLE when item is IN_WASH and 5 days passed`() {
        val calendar = Calendar.getInstance()
        val today = calendar.time
        
        calendar.add(Calendar.DAY_OF_YEAR, -5)
        val fiveDaysAgoStr = dateFormat.format(calendar.time)

        val item = createMockItem("IN_WASH", fiveDaysAgoStr)
        
        val result = WardrobeHelper.processCooldown(item, today)
        
        assertEquals("AVAILABLE", result?.status)
    }

    @Test
    fun `processCooldown returns null when item is already AVAILABLE`() {
        val calendar = Calendar.getInstance()
        val today = calendar.time
        
        calendar.add(Calendar.DAY_OF_YEAR, -5)
        val fiveDaysAgoStr = dateFormat.format(calendar.time)

        // Item is AVAILABLE, not WORN or IN_WASH
        val item = createMockItem("AVAILABLE", fiveDaysAgoStr)
        
        val result = WardrobeHelper.processCooldown(item, today)
        
        assertNull(result)
    }

    @Test
    fun `processCooldown returns null when lastWornDate is null`() {
        val today = java.util.Date()
        val item = createMockItem("WORN", null)
        
        val result = WardrobeHelper.processCooldown(item, today)
        
        assertNull(result)
    }
}
