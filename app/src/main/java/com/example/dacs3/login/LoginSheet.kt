package com.example.dacs3.login.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.dacs3.login.ui.theme.* // Import các màu chủ đạo

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginSheet(onNavigateToSignUp: () -> Unit, onLoginSuccess: () -> Unit) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isVisibled by remember { mutableStateOf(false) }

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
                focusedTextColor = MidnightBlue
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

        // --- NÚT ĐĂNG NHẬP CHÍNH ---
        Button(
            onClick = { onLoginSuccess() },
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
            SocialButton(text = "G")
            Spacer(modifier = Modifier.width(24.dp))
            SocialButton(text = "A")
            Spacer(modifier = Modifier.width(24.dp))
            SocialButton(text = "F")
        }

        Spacer(modifier = Modifier.height(20.dp))
    }
}

@Composable
fun SocialButton(text: String) {
    OutlinedCard(
        shape = RoundedCornerShape(2.dp), // Đồng bộ góc vuông
        modifier = Modifier.size(50.dp),
        border = androidx.compose.foundation.BorderStroke(0.5.dp, SilverMist.copy(alpha = 0.5f)),
        colors = CardDefaults.outlinedCardColors(containerColor = Color.Transparent)
    ) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(text, fontWeight = FontWeight.ExtraLight, color = MidnightBlue)
        }
    }
}