package com.example.dacs3.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// 1. BẢNG MÀU SÁNG (Gắn các màu gốc của bạn vào chuẩn của Material 3)
private val LightColors = lightColorScheme(
    background = OffWhite,
    surface = Color.White,
    primary = MidnightBlue,
    onPrimary = Color.White,
    secondary = AccentTeal,
    secondaryContainer = SoftTeal,
    tertiaryContainer = SoftOrange,
    surfaceVariant = LightGray,
    onSurface = MidnightBlue,
    onSurfaceVariant = SilverMist,
    error = Color(0xFFD32F2F)
)

// 2. BẢNG MÀU TỐI
private val DarkColors = darkColorScheme(
    background = DarkBackground,
    surface = DarkSurface,
    primary = TextDarkPrimary,
    onPrimary = DarkBackground,
    secondary = DarkAccentTeal,
    secondaryContainer = DarkSoftTeal,
    tertiaryContainer = DarkSoftOrange,
    surfaceVariant = DarkSurfaceVariant,
    onSurface = TextDarkPrimary,
    onSurfaceVariant = TextDarkSecondary,
    error = Color(0xFFEF5350)
)

// 3. HÀM ÁP DỤNG THEME
@Composable
fun WearwiseTheme(
    darkTheme: Boolean = isSystemInDarkTheme(), // Tự động lấy theo chế độ máy
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColors else LightColors

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}