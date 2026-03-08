package com.example.dacs3.login

import android.net.Uri
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.example.dacs3.R
import com.example.dacs3.login.be.isUserLoggedIn
import com.example.dacs3.login.ui.LoginSheet
import com.example.dacs3.login.ui.RegisterSheet
import com.example.dacs3.login.ui.theme.*

enum class AuthSheetType {LOGIN, REGISTER}

@androidx.annotation.OptIn(UnstableApi::class)
@Composable
fun LoginScreen(onLoginSuccess: () -> Unit) { // Nhận callback từ MainActivity
    val context = LocalContext.current

    var showIntro by remember { mutableStateOf(true) }

    // Quản lý ExoPlayer trong Compose
    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            val videoResources = listOf(R.raw.video1, R.raw.video2, R.raw.video3)
            val mediaItems = videoResources.map { resId ->
                MediaItem.fromUri(Uri.parse("android.resource://${context.packageName}/$resId"))
            }
            setMediaItems(mediaItems)
            shuffleModeEnabled = true
            repeatMode = Player.REPEAT_MODE_ALL
            videoScalingMode = C.VIDEO_SCALING_MODE_SCALE_TO_FIT_WITH_CROPPING
            prepare()
            playWhenReady = true
            volume = 0f
        }
    }

    // Tự động tắt video khi màn hình này bị đóng (chuyển sang Home)
    DisposableEffect(Unit) {
        onDispose {
            exoPlayer.release()
        }
    }

    Surface(modifier = Modifier.fillMaxSize(), color = MidnightBlue) {
        AnimatedContent(
            targetState = showIntro,
            label = "SlidePushTransition",
            transitionSpec = {
                if (!targetState) {
                    slideInVertically(
                        animationSpec = tween(1000),
                        initialOffsetY = { fullHeight -> fullHeight }
                    ) togetherWith slideOutVertically(
                        animationSpec = tween(1000),
                        targetOffsetY = { fullHeight -> -fullHeight }
                    )
                } else {
                    fadeIn() togetherWith fadeOut()
                }
            }
        ) { isShowingIntro ->
            if (isShowingIntro) {
                SplashScreen(onAnimationFinished = { showIntro = false })
            } else {
                WelcomeScreen(exoPlayer = exoPlayer, onLoginSuccess = onLoginSuccess)
            }
        }
    }
}

@androidx.annotation.OptIn(UnstableApi::class)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WelcomeScreen(exoPlayer: ExoPlayer?, onLoginSuccess: () -> Unit) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var showSheet by remember { mutableStateOf(false) }
    var currentSheet by remember { mutableStateOf(AuthSheetType.LOGIN) }

    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    player = exoPlayer
                    useController = false
                    resizeMode = AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.verticalGradient(
                        0.0f to Color.Black.copy(alpha = 0.8f),
                        0.5f to Color.Transparent,
                        1.0f to MidnightBlue.copy(alpha = 0.9f)
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 40.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Phần LOGO
            Column(
                modifier = Modifier.padding(top = 100.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "WEARWISE",
                    fontSize = 36.sp,
                    fontWeight = FontWeight.Light,
                    color = TextLight,
                    letterSpacing = 12.sp,
                    style = TextStyle(shadow = Shadow(color = Color.White.copy(alpha = 0.8f), blurRadius = 45f))
                )
                Spacer(modifier = Modifier.height(20.dp))
                Box(modifier = Modifier.width(40.dp).height(1.dp).background(SilverMist.copy(alpha = 0.6f)))
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Refine wardrobe - define essence!",
                    fontSize = 15.sp,
                    color = TextLight,
                    textAlign = TextAlign.Center,
                    letterSpacing = 1.sp,
                    style = TextStyle(shadow = Shadow(color = SilverMist.copy(alpha = 0.7f), blurRadius = 30f))
                )
            }

            val scope = rememberCoroutineScope()

            // Phần NÚT BẤM
            Column(
                modifier = Modifier.padding(bottom = 50.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Button(
                    onClick = {
                        currentSheet = AuthSheetType.REGISTER
                        showSheet = true
                    },
                    modifier = Modifier.fillMaxWidth().height(58.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = OffWhite.copy(alpha = 0.95f), contentColor = MidnightBlue)
                ) {
                    Text(text = "GET STARTED", fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
                }

                Spacer(modifier = Modifier.height(24.dp))

                TextButton(onClick = {
                    currentSheet = AuthSheetType.LOGIN
                    showSheet = true
                }) {
                    Text(
                        text = buildAnnotatedString {
                            append("Already have an account? ")
                            withStyle(style = SpanStyle(textDecoration = TextDecoration.Underline, fontWeight = FontWeight.Bold)) {
                                append("Log in")
                            }
                        },
                        color = TextLight,
                        fontSize = 15.sp
                    )
                }
                Spacer(modifier = Modifier.height(20.dp))
                Box(modifier = Modifier.width(1.dp).height(30.dp).background(SilverMist.copy(alpha = 0.4f)))
            }
        }

        if (showSheet) {
            ModalBottomSheet(
                onDismissRequest = { showSheet = false },
                sheetState = sheetState,
                containerColor = OffWhite,
                dragHandle = { BottomSheetDefaults.DragHandle(color = SilverMist.copy(alpha = 0.5f)) },
                shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(700.dp)
                        .background(OffWhite)
                        .navigationBarsPadding()
                ) {
                    AnimatedContent(
                        targetState = currentSheet,
                        label = "AuthSheetTransition",
                        transitionSpec = {
                            if (targetState == AuthSheetType.REGISTER) {
                                (slideInHorizontally(animationSpec = tween(500)) { it } + fadeIn())
                                    .togetherWith(slideOutHorizontally(animationSpec = tween(500)) { -it } + fadeOut())
                            } else {
                                (slideInHorizontally(animationSpec = tween(500)) { -it } + fadeIn())
                                    .togetherWith(slideOutHorizontally(animationSpec = tween(500)) { it } + fadeOut())
                            }
                        }
                    ) { targetSheet ->
                        when (targetSheet) {
                            AuthSheetType.LOGIN -> LoginSheet(
                                onNavigateToSignUp = { currentSheet = AuthSheetType.REGISTER },
                                onLoginSuccess = onLoginSuccess // TRUYỀN CALLBACK VÀO ĐÂY
                            )
                            AuthSheetType.REGISTER -> RegisterSheet(
                                onBackToLogin = { currentSheet = AuthSheetType.LOGIN },
                                onRegisterSuccess = { currentSheet = AuthSheetType.LOGIN } // TRUYỀN CALLBACK VÀO ĐÂY
                            )
                        }
                    }
                }
            }
        }
    }
}