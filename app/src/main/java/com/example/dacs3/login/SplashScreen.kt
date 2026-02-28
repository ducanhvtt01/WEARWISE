package com.example.dacs3.login

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.airbnb.lottie.compose.*
import com.example.dacs3.R
import com.example.dacs3.login.ui.theme.OffWhite
import com.example.dacs3.login.ui.theme.SilverMist
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(onAnimationFinished: () -> Unit) {
    // 1. Load các file JSON
    val compositionTop by rememberLottieComposition(LottieCompositionSpec.RawRes(R.raw.hi))
    val compositionCenter by rememberLottieComposition(LottieCompositionSpec.RawRes(R.raw.logo))
    val compositionBottom by rememberLottieComposition(LottieCompositionSpec.RawRes(R.raw.loading2))

    // 2. BIẾN NHẠC TRƯỞNG: Đồng bộ tiến trình cho cả 3
    val progress by animateLottieCompositionAsState(
        composition = compositionCenter,
        iterations = 1,
        speed = 1.2f
    )

    // 3. Xử lý chuyển cảnh sau khi hoàn tất
    LaunchedEffect(progress) {
        if (progress == 1f) {
            delay(500) // Độ trễ nhỏ để tạo cảm giác mượt mà
            onAnimationFinished()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(OffWhite),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // --- LOTTIE PHÍA TRÊN ---
        LottieAnimation(
            composition = compositionTop,
            progress = { progress },
            modifier = Modifier
                .size(200.dp)
                .padding(top = 50.dp)
        )

        // --- LOTTIE PHÍA GIỮA ---
        LottieAnimation(
            composition = compositionCenter,
            progress = { progress },
            modifier = Modifier
                .size(300.dp)
        )

        // --- LOTTIE PHÍA DƯỚI ---
        LottieAnimation(
            composition = compositionBottom,
            progress = { progress },
            modifier = Modifier
                .size(300.dp)
        )
    }
}