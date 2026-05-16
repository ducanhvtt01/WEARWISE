package com.example.dacs3.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

// ─── 1. GLASSMORPHISM CARD: Thẻ kính mờ trong suốt ───
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 20.dp,
    alpha: Float = 0.08f,
    borderAlpha: Float = 0.15f,
    content: @Composable BoxScope.() -> Unit
) {
    val isDark = MaterialTheme.colorScheme.background.red < 0.5f
    val glassColor = if (isDark) Color.White.copy(alpha = alpha) else Color.White.copy(alpha = 0.55f)
    val borderColor = if (isDark) Color.White.copy(alpha = borderAlpha) else Color.White.copy(alpha = 0.6f)

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(cornerRadius))
            .background(Brush.verticalGradient(listOf(glassColor, glassColor.copy(alpha = glassColor.alpha * 0.7f))))
            .border(1.dp, Brush.verticalGradient(listOf(borderColor, Color.Transparent)), RoundedCornerShape(cornerRadius)),
        content = content
    )
}

// ─── 2. SHIMMER BRUSH: Tạo hiệu ứng ánh kim quét qua khi loading ───
@Composable
fun shimmerBrush(targetValue: Float = 1000f): Brush {
    val shimmerColors = listOf(
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f),
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
    )
    val transition = rememberInfiniteTransition(label = "shimmer")
    val translateAnimation by transition.animateFloat(
        initialValue = 0f,
        targetValue = targetValue,
        animationSpec = infiniteRepeatable(tween(1200, easing = FastOutSlowInEasing), RepeatMode.Restart),
        label = "shimmerTranslate"
    )
    return Brush.linearGradient(shimmerColors, Offset.Zero, Offset(translateAnimation, translateAnimation))
}

// ─── ShimmerCard: Khung Shimmer hình chữ nhật dùng làm placeholder ───
@Composable
fun ShimmerCard(modifier: Modifier = Modifier, cornerRadius: Dp = 16.dp) {
    Box(modifier = modifier.clip(RoundedCornerShape(cornerRadius)).background(shimmerBrush()))
}

// ─── 3. EMPTY WARDROBE STATE: Màn hình trống đẹp khi tủ đồ chưa có gì ───
@Composable
fun EmptyWardrobeState(modifier: Modifier = Modifier, onAddClicked: () -> Unit = {}) {
    val gradientColors = listOf(
        MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
        MaterialTheme.colorScheme.secondary.copy(alpha = 0.04f)
    )
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(Brush.verticalGradient(gradientColors))
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f), RoundedCornerShape(24.dp))
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("✨", style = MaterialTheme.typography.displayMedium) // Icon minh họa
        Spacer(Modifier.height(16.dp))
        Text("Your closet is empty", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
        Spacer(Modifier.height(8.dp))
        Text(
            "Scan your clothes with AI or add them manually to unlock personalized outfit recommendations.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(24.dp))
        Button(
            onClick = onAddClicked,
            shape = RoundedCornerShape(50),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
            modifier = Modifier.height(48.dp)
        ) {
            Text("Add first item", fontWeight = FontWeight.SemiBold)
        }
    }
}

// ─── 4. PREMIUM BADGE: Nhãn gradient dùng để gắn "Mới" hoặc "Ít mặc" lên thẻ đồ ───
@Composable
fun PremiumBadge(text: String, gradientColors: List<Color>, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(Brush.horizontalGradient(gradientColors))
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Text(text, color = Color.White, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
    }
}
