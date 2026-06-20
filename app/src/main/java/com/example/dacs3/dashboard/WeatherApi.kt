package com.example.dacs3.dashboard

import android.content.Context
import android.location.Geocoder
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
import java.util.Calendar
import java.util.Locale
import java.util.regex.Pattern
import kotlin.math.roundToInt

// --- HÀM TIỆN ÍCH: BỎ DẤU TIẾNG VIỆT ---
fun String.removeAccent(): String {
    val temp = Normalizer.normalize(this, Normalizer.Form.NFD)
    val pattern = Pattern.compile("\\p{InCombiningDiacriticalMarks}+")
    val result = pattern.matcher(temp).replaceAll("")
    return result.replace('đ', 'd').replace('Đ', 'D')
}

// 1. CÁC LỚP DỮ LIỆU
data class WeatherResponse(
    val name: String,
    val main: MainData,
    val weather: List<WeatherCondition>
)
data class MainData(val temp: Float)
data class WeatherCondition(val main: String, val icon: String)

data class ForecastResponse(
    val list: List<ForecastItem>
)

data class ForecastItem(
    val dt: Long,
    val main: MainData,
    val weather: List<WeatherCondition>,
    val dt_txt: String
)

data class DailyForecast(
    val dateStr: String,
    val tempStr: String,
    val condition: String
)

// 2. INTERFACE API
interface OpenWeatherApi {
    @GET("data/2.5/weather")
    suspend fun getCurrentWeather(
        @Query("q") cityName: String,
        @Query("appid") apiKey: String,
        @Query("units") units: String = "metric"
    ): WeatherResponse

    @GET("data/2.5/weather")
    suspend fun getWeatherByLocation(
        @Query("lat") lat: Double,
        @Query("lon") lon: Double,
        @Query("appid") apiKey: String,
        @Query("units") units: String = "metric"
    ): WeatherResponse

    @GET("data/2.5/forecast")
    suspend fun getCurrentForecast(
        @Query("q") cityName: String,
        @Query("appid") apiKey: String,
        @Query("units") units: String = "metric"
    ): ForecastResponse

    @GET("data/2.5/forecast")
    suspend fun getForecastByLocation(
        @Query("lat") lat: Double,
        @Query("lon") lon: Double,
        @Query("appid") apiKey: String,
        @Query("units") units: String = "metric"
    ): ForecastResponse
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

    private val _tomorrowTemperature = MutableStateFlow("--°C")
    val tomorrowTemperature: StateFlow<String> = _tomorrowTemperature

    private val _tomorrowCondition = MutableStateFlow("Clear")
    val tomorrowCondition: StateFlow<String> = _tomorrowCondition

    private val _weeklyForecast = MutableStateFlow<List<DailyForecast>>(emptyList())
    val weeklyForecast: StateFlow<List<DailyForecast>> = _weeklyForecast

    private val myApiKey = com.example.dacs3.BuildConfig.WEATHER_API_KEY

    private fun extractTomorrowWeather(forecastResponse: ForecastResponse) {
        try {
            val calendar = Calendar.getInstance()
            calendar.add(Calendar.DAY_OF_YEAR, 1)
            val tomorrowDateStr = java.text.SimpleDateFormat("yyyy-MM-dd", Locale.US).format(calendar.time)

            // Tìm dự báo vào khoảng 9:00 hoặc 12:00 ngày mai
            val tomorrowMorningItem = forecastResponse.list.find { it.dt_txt.contains("$tomorrowDateStr 09:00:00") }
                ?: forecastResponse.list.find { it.dt_txt.contains("$tomorrowDateStr 12:00:00") }
                ?: forecastResponse.list.find { it.dt_txt.startsWith(tomorrowDateStr) }

            if (tomorrowMorningItem != null) {
                _tomorrowTemperature.value = "${tomorrowMorningItem.main.temp.roundToInt()}°C"
                _tomorrowCondition.value = tomorrowMorningItem.weather.firstOrNull()?.main ?: "Clear"
            } else {
                _tomorrowTemperature.value = _temperature.value
                _tomorrowCondition.value = _condition.value
            }
        } catch (e: Exception) {
            e.printStackTrace()
            _tomorrowTemperature.value = _temperature.value
            _tomorrowCondition.value = _condition.value
        }
    }

    private fun extractWeeklyForecast(forecastResponse: ForecastResponse) {
        try {
            val list = mutableListOf<DailyForecast>()
            val groups = forecastResponse.list.groupBy { it.dt_txt.substringBefore(" ") }
            for ((dateStr, items) in groups) {
                val representativeItem = items.find { it.dt_txt.contains("12:00:00") } ?: items.first()
                list.add(
                    DailyForecast(
                        dateStr = dateStr,
                        tempStr = "${representativeItem.main.temp.roundToInt()}°C",
                        condition = representativeItem.weather.firstOrNull()?.main ?: "Clear"
                    )
                )
            }
            _weeklyForecast.value = list
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun fetchWeather() {
        viewModelScope.launch {
            try {
                val response = RetrofitInstance.api.getCurrentWeather(
                    cityName = "Da Nang",
                    apiKey = myApiKey
                )

                val weatherData = response.weather.firstOrNull()
                _temperature.value = "${response.main.temp.roundToInt()}°C"
                _condition.value = weatherData?.main ?: "Clear"
                _isNight.value = weatherData?.icon?.endsWith("n") ?: false

                val rawName = response.name
                val cleanName = if (rawName.equals("Turan", ignoreCase = true)) "Da Nang" else rawName.removeAccent()
                _cityName.value = cleanName

                // Fetch forecast
                try {
                    val forecastResponse = RetrofitInstance.api.getCurrentForecast(
                        cityName = "Da Nang",
                        apiKey = myApiKey
                    )
                    extractTomorrowWeather(forecastResponse)
                    extractWeeklyForecast(forecastResponse)
                } catch (e: Exception) {
                    _tomorrowTemperature.value = _temperature.value
                    _tomorrowCondition.value = _condition.value
                }

            } catch (e: Exception) {
                _temperature.value = "26°C"
                _condition.value = "Clear"
                _isNight.value = false
                _cityName.value = "Da Nang"
                _tomorrowTemperature.value = "26°C"
                _tomorrowCondition.value = "Clear"
            }
        }
    }

    // HÀM MỚI: Truyền thêm context để dùng Geocoder dịch tọa độ ra tên Thành phố
    fun fetchWeatherByLocation(context: Context, lat: Double, lon: Double) {
        viewModelScope.launch {
            try {
                val response = RetrofitInstance.api.getWeatherByLocation(
                    lat = lat,
                    lon = lon,
                    apiKey = myApiKey
                )

                val weatherData = response.weather.firstOrNull()
                _temperature.value = "${response.main.temp.roundToInt()}°C"
                _condition.value = weatherData?.main ?: "Clear"
                _isNight.value = weatherData?.icon?.endsWith("n") ?: false

                // Dùng Geocoder để lấy tên Thành Phố/Tỉnh thay vì tên Phường/Xã
                try {
                    val geocoder = Geocoder(context, Locale.ENGLISH)
                    val addresses = geocoder.getFromLocation(lat, lon, 1)

                    if (!addresses.isNullOrEmpty()) {
                        val address = addresses[0]
                        // Ưu tiên lấy adminArea (Tỉnh/Thành phố trực thuộc TW), nếu không có thì lấy locality (Thành phố thuộc tỉnh)
                        val city = address.adminArea ?: address.locality ?: response.name
                        val cleanName = if (city.equals("Turan", ignoreCase = true)) "Da Nang" else city.removeAccent()

                        // Cắt bỏ chữ "Province" hoặc "City" nếu có để tên gọn hơn (Ví dụ: "Da Nang City" -> "Da Nang")
                        _cityName.value = cleanName.replace(" Province", "").replace(" City", "")
                    } else {
                        _cityName.value = response.name.removeAccent()
                    }
                } catch (e: Exception) {
                    _cityName.value = response.name.removeAccent()
                }

                // Fetch forecast
                try {
                    val forecastResponse = RetrofitInstance.api.getForecastByLocation(
                        lat = lat,
                        lon = lon,
                        apiKey = myApiKey
                    )
                    extractTomorrowWeather(forecastResponse)
                    extractWeeklyForecast(forecastResponse)
                } catch (e: Exception) {
                    _tomorrowTemperature.value = _temperature.value
                    _tomorrowCondition.value = _condition.value
                }

            } catch (e: Exception) {
                fetchWeather()
            }
        }
    }
}