package com.example.dacs3.login.ui

import android.widget.Toast
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.dacs3.login.be.onRegister
import com.example.dacs3.login.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterSheet(onBackToLogin: () -> Unit, onRegisterSuccess: () -> Unit) {
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    // 1. Thêm biến state cho xác nhận mật khẩu
    var confirmPassword by remember { mutableStateOf("") }
    var isVisibled by remember { mutableStateOf(false) }
    var isConfirmVisibled by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }


    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(OffWhite)
            .padding(horizontal = 32.dp, vertical = 24.dp)
    ) {
        Text("Create Account", fontSize = 26.sp, fontWeight = FontWeight.Light, color = MidnightBlue)
        Text("Join the essence of smart curation", fontSize = 13.sp, color = SilverMist, modifier = Modifier.padding(top = 8.dp, bottom = 32.dp))

        // --- NAME FIELD ---
        Text("FULL NAME", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MidnightBlue.copy(alpha = 0.6f), letterSpacing = 1.5.sp)
        OutlinedTextField(
            value = name, onValueChange = { name = it },
            placeholder = { Text("Your name", color = SilverMist.copy(alpha = 0.6f), fontSize = 14.sp) },
            modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
            shape = RoundedCornerShape(14.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = Color.White,
                unfocusedContainerColor = Color.White.copy(alpha = 0.6f),
                focusedBorderColor = MidnightBlue,
                unfocusedBorderColor = SilverMist.copy(alpha = 0.4f),
                cursorColor = MidnightBlue,
                focusedTextColor = MidnightBlue,
                unfocusedTextColor = MidnightBlue,
                focusedLeadingIconColor = MidnightBlue,
                unfocusedLeadingIconColor = SilverMist,
                focusedTrailingIconColor = MidnightBlue,
                unfocusedTrailingIconColor = SilverMist
            ),
            leadingIcon = {Icon(imageVector = Icons.Default.Person, contentDescription = "")},
            trailingIcon = {
                if (name.isNotEmpty()) {
                    IconButton(onClick = { name = "" }) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "")
                    }
                }
            },
            singleLine = true
        )

        Spacer(modifier = Modifier.height(20.dp))

        // --- EMAIL FIELD ---
        Text("EMAIL ADDRESS", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MidnightBlue.copy(alpha = 0.6f), letterSpacing = 1.5.sp)
        OutlinedTextField(
            value = email, onValueChange = { email = it },
            placeholder = { Text("name@essence.com", color = SilverMist.copy(alpha = 0.6f), fontSize = 14.sp) },
            modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
            shape = RoundedCornerShape(14.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = Color.White,
                unfocusedContainerColor = Color.White.copy(alpha = 0.6f),
                focusedBorderColor = MidnightBlue,
                unfocusedBorderColor = SilverMist.copy(alpha = 0.4f),
                cursorColor = MidnightBlue,
                focusedTextColor = MidnightBlue,
                unfocusedTextColor = MidnightBlue,
                focusedLeadingIconColor = MidnightBlue,
                unfocusedLeadingIconColor = SilverMist,
                focusedTrailingIconColor = MidnightBlue,
                unfocusedTrailingIconColor = SilverMist
            ),
            leadingIcon = {Icon(imageVector = Icons.Default.Person, contentDescription = "")},
            trailingIcon = {
                if (email.isNotEmpty()) {
                    IconButton(onClick = { email = "" }) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "")
                    }
                }
            },
            singleLine = true
        )

        Spacer(modifier = Modifier.height(20.dp))

        // --- PASSWORD FIELD ---
        Text("PASSWORD", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MidnightBlue.copy(alpha = 0.6f), letterSpacing = 1.5.sp)
        OutlinedTextField(
            value = password, onValueChange = { password = it },
            placeholder = { Text("••••••••", color = SilverMist.copy(alpha = 0.6f), fontSize = 14.sp) },
            modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
            shape = RoundedCornerShape(14.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = Color.White,
                unfocusedContainerColor = Color.White.copy(alpha = 0.6f),
                focusedBorderColor = MidnightBlue,
                unfocusedBorderColor = SilverMist.copy(alpha = 0.4f),
                cursorColor = MidnightBlue,
                focusedTextColor = MidnightBlue,
                unfocusedTextColor = MidnightBlue,
                focusedLeadingIconColor = MidnightBlue,
                unfocusedLeadingIconColor = SilverMist,
                focusedTrailingIconColor = MidnightBlue,
                unfocusedTrailingIconColor = SilverMist
            ),
            leadingIcon = {Icon(imageVector = Icons.Default.Lock, contentDescription = "")},
            trailingIcon = {
                IconButton(onClick = {isVisibled = !isVisibled}) {
                    Icon(
                        imageVector = if (isVisibled) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                        contentDescription = ""
                    )
                }
            },
            visualTransformation = if (isVisibled) VisualTransformation.None else PasswordVisualTransformation(),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(20.dp))

        // 2. --- CONFIRM PASSWORD FIELD ---
        Text("CONFIRM PASSWORD", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MidnightBlue.copy(alpha = 0.6f), letterSpacing = 1.5.sp)
        OutlinedTextField(
            value = confirmPassword,
            onValueChange = { confirmPassword = it },
            placeholder = { Text("••••••••", color = SilverMist.copy(alpha = 0.6f), fontSize = 14.sp) },
            modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
            shape = RoundedCornerShape(14.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = Color.White,
                unfocusedContainerColor = Color.White.copy(alpha = 0.6f),
                focusedBorderColor = MidnightBlue,
                unfocusedBorderColor = SilverMist.copy(alpha = 0.4f),
                cursorColor = MidnightBlue,
                focusedTextColor = MidnightBlue,
                unfocusedTextColor = MidnightBlue,
                focusedLeadingIconColor = MidnightBlue,
                unfocusedLeadingIconColor = SilverMist,
                focusedTrailingIconColor = MidnightBlue,
                unfocusedTrailingIconColor = SilverMist
            ),
            leadingIcon = {Icon(imageVector = Icons.Default.Lock, contentDescription = "")},
            trailingIcon = {
                IconButton(onClick = {isConfirmVisibled = !isConfirmVisibled}) {
                    Icon(
                        imageVector = if (isConfirmVisibled) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                        contentDescription = ""
                    )
                }
            },
            visualTransformation = if (isConfirmVisibled) VisualTransformation.None else PasswordVisualTransformation(),
            singleLine = true
        )

        if (errorMessage != null) {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = errorMessage!!,
                color = Color.Red,
                fontSize = 13.sp,
                modifier = Modifier.fillMaxWidth()
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        val scope = rememberCoroutineScope()
        val context = LocalContext.current

        // 👇 [THI] SỬA MÀU SẮC / CHỮ / NÚT ĐĂNG KÝ Ở ĐÂY 👇
        Button(
            onClick = {
                errorMessage = null
                if (name.isBlank()) {
                    errorMessage = "Please enter your full name."
                    return@Button
                }
                if (email.isBlank()) {
                    errorMessage = "Please enter your email address."
                    return@Button
                }
                if (password.isBlank()) {
                    errorMessage = "Please enter your password."
                    return@Button
                }
                if (password.length < 6) {
                    errorMessage = "Password must be at least 6 characters."
                    return@Button
                }
                if (password != confirmPassword) {
                    errorMessage = "Passwords do not match."
                    return@Button
                }

                scope.launch {
                    val error = onRegister(email, password, name)
                    if (error == null) {
                        // 👇 [THI] THÊM TOAST/SNACKBAR THÔNG BÁO Ở ĐÂY 👇
                        Toast.makeText(context, "Registration successful! Please check your email to confirm your account.", Toast.LENGTH_LONG).show()
                        onRegisterSuccess()
                    } else {
                        // 👇 [THI] BẮT LỖI / HIỂN THỊ GIAO DIỆN LỖI Ở ĐÂY 👇
                        // Hiển thị text báo lỗi khi đăng ký thất bại
                        errorMessage = error
                    }
                }
            },
            modifier = Modifier.fillMaxWidth().height(54.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MidnightBlue, contentColor = OffWhite)
        ) {
            Text("CREATE ACCOUNT", letterSpacing = 2.sp, fontWeight = FontWeight.Light)
        }

        TextButton(onClick = onBackToLogin, modifier = Modifier.fillMaxWidth()) {
            Text("Already a member? Sign In", color = MidnightBlue.copy(alpha = 0.7f), fontSize = 13.sp)
        }
    }
}