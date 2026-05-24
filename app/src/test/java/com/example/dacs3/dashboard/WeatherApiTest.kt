package com.example.dacs3.dashboard

import org.junit.Assert.assertEquals
import org.junit.Test

class WeatherApiTest {

    @Test
    fun `removeAccent converts Vietnamese characters correctly`() {
        val input = "Đà Nẵng"
        val expected = "Da Nang"
        assertEquals(expected, input.removeAccent())
    }

    @Test
    fun `removeAccent handles complex uppercase and lowercase`() {
        val input = "ĐẮK LẮK đắk lắk"
        val expected = "DAK LAK dak lak"
        assertEquals(expected, input.removeAccent())
    }

    @Test
    fun `removeAccent handles all Vietnamese tones`() {
        val input = "á à ả ã ạ â ấ ầ ẩ ẫ ậ ă ắ ằ ẳ ẵ ặ đ é è ẻ ẽ ẹ ê ế ề ể ễ ệ í ì ỉ ĩ ị ó ò ỏ õ ọ ô ố ồ ổ ỗ ộ ơ ớ ờ ở ỡ ợ ú ù ủ ũ ụ ư ứ ừ ử ữ ự ý ỳ tỷ ỹ ỵ"
        val expected = "a a a a a a a a a a a a a a a a a d e e e e e e e e e e e i i i i i o o o o o o o o o o o o o o o o o u u u u u u u u u u u y y ty y y"
        assertEquals(expected, input.removeAccent())
    }

    @Test
    fun `removeAccent returns same string if no accents`() {
        val input = "Ho Chi Minh"
        val expected = "Ho Chi Minh"
        assertEquals(expected, input.removeAccent())
    }

    @Test
    fun `removeAccent handles empty string`() {
        val input = ""
        val expected = ""
        assertEquals(expected, input.removeAccent())
    }
}
