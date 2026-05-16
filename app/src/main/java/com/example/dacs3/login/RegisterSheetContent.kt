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
        TextField(
            value = name, onValueChange = { name = it },
            placeholder = { Text("Your name", color = SilverMist, fontSize = 14.sp) },
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
            trailingIcon = { IconButton(onClick = {
                name = ""
            }) { Icon(imageVector = Icons.Default.Close, contentDescription = "")} },
            singleLine = true
        )

        Spacer(modifier = Modifier.height(24.dp))

        // --- EMAIL FIELD ---
        Text("EMAIL ADDRESS", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MidnightBlue.copy(alpha = 0.6f), letterSpacing = 1.5.sp)
        TextField(
            value = email, onValueChange = { email = it },
            placeholder = { Text("name@essence.com", color = SilverMist, fontSize = 14.sp) },
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
        Text("PASSWORD", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MidnightBlue.copy(alpha = 0.6f), letterSpacing = 1.5.sp)
        TextField(
            value = password, onValueChange = { password = it },
            placeholder = { Text("••••••••", color = SilverMist, fontSize = 14.sp) },
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
                IconButton(onClick = {isVisibled = !isVisibled}) {
                    when (isVisibled) {
                        false -> Icon(imageVector = Icons.Default.Visibility, contentDescription = "")
                        true -> Icon(imageVector = Icons.Default.VisibilityOff, contentDescription = "")
                    } }

            },
            visualTransformation = when (isVisibled) {
                false -> PasswordVisualTransformation()
                true -> VisualTransformation.None
            },
            singleLine = true
        )

        Spacer(modifier = Modifier.height(24.dp))

        // 2. --- CONFIRM PASSWORD FIELD ---
        Text("CONFIRM PASSWORD", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MidnightBlue.copy(alpha = 0.6f), letterSpacing = 1.5.sp)
        TextField(
            value = confirmPassword,
            onValueChange = { confirmPassword = it },
            placeholder = { Text("••••••••", color = SilverMist, fontSize = 14.sp) },
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
                IconButton(onClick = {isConfirmVisibled = !isConfirmVisibled}) {
                    when (isConfirmVisibled) {
                        false -> Icon(imageVector = Icons.Default.Visibility, contentDescription = "")
                        true -> Icon(imageVector = Icons.Default.VisibilityOff, contentDescription = "")
                    } }

            },
            visualTransformation = when (isConfirmVisibled) {
                false -> PasswordVisualTransformation()
                true -> VisualTransformation.None
            },
            singleLine = true
        )

        Spacer(modifier = Modifier.height(40.dp))

        val scope = rememberCoroutineScope()
        val context = LocalContext.current

        Button(
            onClick = {
                if (password == confirmPassword) {
                    scope.launch {
                        try {
                            onRegister(email, password, name)
                            Toast.makeText(context, "Please check your email for confirmation!", Toast.LENGTH_LONG).show()
                            onRegisterSuccess()
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }

                }
            },
            modifier = Modifier.fillMaxWidth().height(54.dp),
            shape = RoundedCornerShape(6.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MidnightBlue, contentColor = OffWhite)
        ) {
            Text("CREATE ACCOUNT", letterSpacing = 2.sp, fontWeight = FontWeight.Light)
        }

        TextButton(onClick = onBackToLogin, modifier = Modifier.fillMaxWidth()) {
            Text("Already a member? Sign In", color = MidnightBlue.copy(alpha = 0.7f), fontSize = 13.sp)
        }
    }
}