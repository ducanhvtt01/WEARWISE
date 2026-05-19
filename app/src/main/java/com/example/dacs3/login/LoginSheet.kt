package com.example.dacs3.login.ui

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.dacs3.R
import com.example.dacs3.login.be.LoginResult
import com.example.dacs3.login.be.logincheck
import com.example.dacs3.login.be.signInWithSocial
import com.example.dacs3.login.ui.theme.* // Import các màu chủ đạo
import io.github.jan.supabase.gotrue.providers.Facebook
import io.github.jan.supabase.gotrue.providers.Github
import io.github.jan.supabase.gotrue.providers.Google
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginSheet(onNavigateToSignUp: () -> Unit, onLoginSuccess: () -> Unit) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isVisibled by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(OffWhite) // Sử dụng màu nền Off-White
            .padding(horizontal = 32.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.Start
    ) {
        // --- TIÊU ĐỀ ---
        Text(
            text = "Sign In",
            fontSize = 26.sp,
            fontWeight = FontWeight.Light,
            color = MidnightBlue,
            letterSpacing = 1.sp
        )

        Text(
            text = "Enter your space of curation",
            fontSize = 13.sp,
            color = SilverMist,
            modifier = Modifier.padding(top = 8.dp, bottom = 32.dp),
            letterSpacing = 0.5.sp
        )

        // --- EMAIL FIELD ---
        Text(
            text = "EMAIL ADDRESS",
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = MidnightBlue.copy(alpha = 0.6f),
            letterSpacing = 1.5.sp
        )
        TextField(
            value = email,
            onValueChange = { email = it },
            placeholder = { Text("name@essence.com", color = SilverMist.copy(alpha = 0.5f), fontSize = 14.sp) },
            modifier = Modifier.fillMaxWidth(),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent,
                focusedIndicatorColor = MidnightBlue,
                unfocusedIndicatorColor = SilverMist.copy(alpha = 0.3f),
                cursorColor = MidnightBlue,
                focusedTextColor = MidnightBlue,
                unfocusedTextColor = MidnightBlue
            ),
            leadingIcon = {Icon(imageVector = Icons.Default.Person, contentDescription = "")},
            trailingIcon = {
                IconButton(onClick = {
                    email = ""
                }) { Icon(imageVector = Icons.Default.Close, contentDescription = "")}
            },
            singleLine = true

        )

        Spacer(modifier = Modifier.height(24.dp))

        // --- PASSWORD FIELD ---
        Text(
            text = "PASSWORD",
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = MidnightBlue.copy(alpha = 0.6f),
            letterSpacing = 1.5.sp
        )
        TextField(
            value = password,
            onValueChange = { password = it },
            placeholder = { Text("••••••••", color = SilverMist.copy(alpha = 0.5f), fontSize = 14.sp) },
            modifier = Modifier.fillMaxWidth(),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent,
                focusedIndicatorColor = MidnightBlue,
                unfocusedIndicatorColor = SilverMist.copy(alpha = 0.3f),
                cursorColor = MidnightBlue,
                focusedTextColor = MidnightBlue,
                unfocusedTextColor = MidnightBlue
            ),
            leadingIcon = {Icon(imageVector = Icons.Default.Lock, contentDescription = "")},
            trailingIcon = {
                IconButton(onClick = {
                    isVisibled = !isVisibled
                }) {
                when (isVisibled) {
                false -> Icon(imageVector = Icons.Default.Visibility, contentDescription = "")
                true -> Icon(imageVector = Icons.Default.VisibilityOff, contentDescription = "")
                }
                 }
            },
            visualTransformation = when (isVisibled) {
            false -> PasswordVisualTransformation()
            true -> VisualTransformation.None
            },
            singleLine = true
        )

        Spacer(modifier = Modifier.height(40.dp))
        val scope = rememberCoroutineScope()
        val context = LocalContext.current
        var showForgotPassword by remember { mutableStateOf(false) }

        if (showForgotPassword) {
            ForgotPasswordDialog(onDismiss = { showForgotPassword = false })
        }

        // --- NÚT QUÊN MẬT KHẨU ---
        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
            Text(
                text = "Forgot Password?",
                fontSize = 13.sp,
                color = MidnightBlue.copy(alpha = 0.6f),
                modifier = Modifier
                    .padding(bottom = 16.dp)
                    .clickable { showForgotPassword = true }
            )
        }

        // --- NÚT ĐĂNG NHẬP CHÍNH ---

        if (errorMessage != null) {
            Text(
                text = errorMessage!!,
                color = Color.Red,
                fontSize = 13.sp,
                modifier = Modifier.padding(bottom = 16.dp)
            )
        }

        Button(
            onClick = {
                errorMessage = null
                if (email.isBlank()) {
                    errorMessage = "Please enter your email address."
                    return@Button
                }
                if (password.isBlank()) {
                    errorMessage = "Please enter your password."
                    return@Button
                }

                scope.launch {
                    val result = logincheck(email, password)

                    when (result) {
                        LoginResult.SUCCESS -> {
                            onLoginSuccess()
                        }
                        LoginResult.EMAIL_NOT_CONFIRMED -> {
                            errorMessage = "This email address is not verified. Please check your email."
                        }
                        LoginResult.INVALID_CREDENTIALS -> {
                            errorMessage = "Incorrect email or password."
                        }
                        is LoginResult.ERROR -> {
                            errorMessage = result.message
                        }
                    }
                }
            },
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(6.dp), // Góc vuông vức sang trọng
            colors = ButtonDefaults.buttonColors(
                containerColor = MidnightBlue,
                contentColor = OffWhite
            ),
            elevation = ButtonDefaults.buttonElevation(0.dp)
        ) {
            Text("SIGN IN", letterSpacing = 2.sp, fontWeight = FontWeight.Light)
        }

        Spacer(modifier = Modifier.height(20.dp))

        // --- CHUYỂN SANG ĐĂNG KÝ ---
        TextButton(
            onClick = onNavigateToSignUp, // Kích hoạt callback chuyển sheet
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "New here? Create an account",
                color = MidnightBlue.copy(alpha = 0.7f),
                fontSize = 13.sp,
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        // --- SOCIAL SECTION ---
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.weight(1f).height(0.5.dp).background(SilverMist.copy(alpha = 0.4f)))
            Text("   OR   ", color = SilverMist, fontSize = 10.sp, letterSpacing = 1.sp)
            Box(modifier = Modifier.weight(1f).height(0.5.dp).background(SilverMist.copy(alpha = 0.4f)))
        }

        Spacer(modifier = Modifier.height(24.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center
        ) {
            SocialButton(
                icon = painterResource(id = R.drawable.ic_google),
                tint = Color.Unspecified
            ) {
                scope.launch {
                    try {
                        val started = signInWithSocial(Google)

                        if (!started) {
                            Toast.makeText(
                                context,
                                "Cannot open Google login",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    } catch (e: Exception) {
                        Toast.makeText(
                            context,
                            "Google login error: ${e.message}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }

            Spacer(modifier = Modifier.width(20.dp))

            // Nút Facebook (Đổ màu xanh đặc trưng)
            SocialButton(
                icon = painterResource(id = R.drawable.ic_facebook)
            ) {
                scope.launch {
                    try {
                        val started = signInWithSocial(Facebook)

                        if (!started) {
                            Toast.makeText(
                                context,
                                "Cannot open Facebook login",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    } catch (e: Exception) {
                        Toast.makeText(
                            context,
                            "Facebook login error: ${e.message}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }

            Spacer(modifier = Modifier.width(20.dp))

            // Nút GitHub (Đổ màu đen)
            SocialButton(
                icon = painterResource(id = R.drawable.ic_github),
                tint = MidnightBlue
            ) {
                scope.launch {
                    try {
                        val started = signInWithSocial(Github)

                        if (!started) {
                            Toast.makeText(
                                context,
                                "Cannot open GitHub login",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    } catch (e: Exception) {
                        Toast.makeText(
                            context,
                            "GitHub login error: ${e.message}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(20.dp))
    }
}

@Composable
fun SocialButton(
    icon: Painter,
    tint: Color = Color.Unspecified,
    onClick: () -> Unit
) {
    OutlinedCard(
        shape = CircleShape, // CHUYỂN THÀNH HÌNH TRÒN
        modifier = Modifier
            .size(54.dp)
            .clip(CircleShape)
            .clickable { onClick() },
        border = androidx.compose.foundation.BorderStroke(1.dp, SilverMist.copy(alpha = 0.2f)),
        colors = CardDefaults.outlinedCardColors(containerColor = Color.White)
    ) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Icon(
                painter = icon,
                contentDescription = null,
                modifier = Modifier.size(30.dp),
                tint = tint
            )
        }
    }
}