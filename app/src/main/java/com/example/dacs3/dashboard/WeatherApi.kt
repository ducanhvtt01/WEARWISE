package com.example.dacs3.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query
import java.text.Normalizer
import java.util.regex.Pattern
import kotlin.math.roundToInt

// --- HÀM TIỆN ÍCH: BỎ DẤU TIẾNG VIỆT ---
fun String.removeAccent(): String {
    val temp = Normalizer.normalize(this, Normalizer.Form.NFD)
    val pattern = Pattern.compile("\\p{InCombiningDiacriticalMarks}+")
    val result = pattern.matcher(temp).replaceAll("")
    return result.replace('đ', 'd').replace('Đ', 'D')
}

// 1. CÁC LỚP DỮ LIỆU (Giữ nguyên hoặc cập nhật name)
data class WeatherResponse(
    val name: String,
    val main: MainData,
    val weather: List<WeatherCondition>
)
data class MainData(val temp: Float)
data class WeatherCondition(val main: String, val icon: String)

// 2. INTERFACE API
interface OpenWeatherApi {
    @GET("data/2.5/weather")
    suspend fun getCurrentWeather(
        @Query("q") cityName: String,
        @Query("appid") apiKey: String,
        @Query("units") units: String = "metric"
    ): WeatherResponse
}

// 3. RETROFIT INSTANCE
object RetrofitInstance {
    private const val BASE_URL = "https://api.openweathermap.org/"
    val api: OpenWeatherApi by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(OpenWeatherApi::class.java)
    }
}

// 4. VIEWMODEL - NƠI XỬ LÝ LOGIC CHÍNH
class WeatherViewModel : ViewModel() {
    private val _temperature = MutableStateFlow("--°C")
    val temperature: StateFlow<String> = _temperature

    private val _condition = MutableStateFlow("Clear")
    val condition: StateFlow<String> = _condition

    private val _isNight = MutableStateFlow(false)
    val isNight: StateFlow<Boolean> = _isNight

    private val _cityName = MutableStateFlow("Loading...")
    val cityName: StateFlow<String> = _cityName

    fun fetchWeather() {
        viewModelScope.launch {
            try {
                val myApiKey = "a2ae8f3c11d5bff557f2e81f52a543db"

                // Gọi API cho thành phố bạn muốn
                val response = RetrofitInstance.api.getCurrentWeather(
                    cityName = "Da Nang",
                    apiKey = myApiKey
                )

                val weatherData = response.weather.firstOrNull()
                _temperature.value = "${response.main.temp.roundToInt()}°C"
                _condition.value = weatherData?.main ?: "Clear"
                _isNight.value = weatherData?.icon?.endsWith("n") ?: false

                val rawName = response.name
                val cleanName = if (rawName.equals("Turan", ignoreCase = true)) {
                    "Da Nang" // Xử lý riêng lỗi tên cổ của Đà Nẵng
                } else {
                    rawName.removeAccent() // Các thành phố khác tự động bỏ dấu
                }

                _cityName.value = cleanName

            } catch (e: Exception) {
                _temperature.value = "26°C"
                _condition.value = "Clear"
                _isNight.value = false
                _cityName.value = "Da Nang"
            }
        }
    }
}