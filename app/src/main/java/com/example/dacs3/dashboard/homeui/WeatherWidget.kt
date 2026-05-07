package com.example.dacs3.dashboard.homeui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOff
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.animateLottieCompositionAsState
import com.airbnb.lottie.compose.rememberLottieComposition
import com.example.dacs3.R
import com.example.dacs3.dashboard.LocationHelper
import com.example.dacs3.dashboard.WeatherViewModel
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun WeatherWidget(
    viewModel: WeatherViewModel = viewModel(),
    isLocationDenied: Boolean,
    onDeniedChange: (Boolean) -> Unit
) {
    val context = LocalContext.current

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val isGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true ||
                permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true

        if (isGranted) {
            onDeniedChange(false)
            LocationHelper.getCurrentLocation(
                context = context,
                onLocationFetched = { lat, lon ->
                    viewModel.fetchWeatherByLocation(
                        context,
                        lat,
                        lon
                    )
                },
                onFailed = { viewModel.fetchWeather() }
            )
        } else {
            onDeniedChange(true)
            viewModel.fetchWeather()
        }
    }

    LaunchedEffect(Unit) {
        val hasCoarse = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val hasFine = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        if (hasCoarse || hasFine) {
            onDeniedChange(false)
            LocationHelper.getCurrentLocation(
                context = context,
                onLocationFetched = { lat, lon ->
                    viewModel.fetchWeatherByLocation(
                        context,
                        lat,
                        lon
                    )
                },
                onFailed = { viewModel.fetchWeather() }
            )
        } else {
            onDeniedChange(true)
            locationPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                    Manifest.permission.ACCESS_FINE_LOCATION
                )
            )
        }
    }

    val temperature by viewModel.temperature.collectAsState()
    val condition by viewModel.condition.collectAsState()
    val isNight by viewModel.isNight.collectAsState()
    val cityName by viewModel.cityName.collectAsState()

    var currentTime by remember { mutableLongStateOf(System.currentTimeMillis()) }

    LaunchedEffect(Unit) {
        while (true) {
            delay(60000L)
            currentTime = System.currentTimeMillis()
        }
    }

    val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
    val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
    val timeStr = timeFormat.format(Date(currentTime))
    val dateStr = dateFormat.format(Date(currentTime))

    val lottieResId = when {
        condition == "Clear" && isNight -> R.raw.night
        condition == "Clear" && !isNight -> R.raw.sunny
        condition == "Clouds" && isNight -> R.raw.cloudy_night
        condition == "Clouds" && !isNight -> R.raw.cloudy_day
        (condition == "Rain" || condition == "Drizzle") && isNight -> R.raw.rainy_night
        (condition == "Rain" || condition == "Drizzle") && !isNight -> R.raw.rainy_day
        condition == "Thunderstorm" -> R.raw.storm
        condition in listOf(
            "Sand",
            "Dust",
            "Ash",
            "Haze",
            "Fog",
            "Mist",
            "Smoke",
            "Sand",
            "Squall",
            "Tornado"
        ) -> R.raw.sandy

        else -> if (isNight) R.raw.night else R.raw.sunny
    }

    val widgetBgColor = when {
        condition == "Clear" && !isNight -> MaterialTheme.colorScheme.tertiaryContainer
        isNight -> MaterialTheme.colorScheme.surfaceVariant
        condition in listOf("Rain", "Drizzle", "Thunderstorm") -> Color(0xFFE0F7FA)
        else -> MaterialTheme.colorScheme.surfaceVariant
    }

    val widgetTextColor = when {
        condition == "Clear" && !isNight -> Color(0xFFE65100)
        isNight -> MaterialTheme.colorScheme.primary
        condition in listOf("Rain", "Drizzle", "Thunderstorm") -> Color(0xFF006064)
        else -> MaterialTheme.colorScheme.primary
    }

    Column(
        modifier = Modifier.wrapContentWidth(),
        horizontalAlignment = Alignment.End
    ) {
        Column(
            horizontalAlignment = Alignment.End,
            modifier = Modifier.padding(end = 8.dp)
        ) {
            Text(
                text = if (isLocationDenied) "Loading..." else cityName,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(5.dp))
            Text(
                text = "$timeStr • $dateStr",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Medium
            )
        }

        Spacer(modifier = Modifier.height(5.dp))

        if (isLocationDenied) {
            Column(horizontalAlignment = Alignment.End) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .shadow(
                            4.dp,
                            RoundedCornerShape(16.dp),
                            spotColor = MaterialTheme.colorScheme.error.copy(alpha = 0.3f)
                        )
                        .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(16.dp))
                        .clickable {
                            val intent = Intent(
                                Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                            ).apply {
                                data =
                                    Uri.fromParts("package", context.packageName, null)
                            }
                            context.startActivity(intent)
                        }
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Icon(
                        Icons.Filled.LocationOff,
                        "Location Off",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        "Enable Location",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        } else {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .shadow(
                        6.dp,
                        RoundedCornerShape(16.dp),
                        spotColor = Color.Gray.copy(alpha = 0.2f)
                    )
                    .background(widgetBgColor, RoundedCornerShape(16.dp))
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                val composition by rememberLottieComposition(
                    LottieCompositionSpec.RawRes(
                        lottieResId
                    )
                )
                val progress by animateLottieCompositionAsState(
                    composition,
                    iterations = LottieConstants.IterateForever
                )

                if (composition != null) {
                    LottieAnimation(composition, { progress }, modifier = Modifier.size(36.dp))
                } else {
                    Icon(
                        Icons.Filled.WbSunny,
                        null,
                        tint = Color(0xFFFF9800),
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(modifier = Modifier.width(4.dp))

                Text(
                    temperature,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 18.sp,
                    color = widgetTextColor
                )
            }
        }
    }
}