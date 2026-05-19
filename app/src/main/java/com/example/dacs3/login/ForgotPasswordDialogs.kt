package com.example.dacs3.login.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.dacs3.login.be.sendResetPasswordEmail
import com.example.dacs3.login.be.updateUserPassword
import com.example.dacs3.login.be.verifyResetOtp
import com.example.dacs3.login.ui.theme.MidnightBlue
import com.example.dacs3.login.ui.theme.OffWhite
import com.example.dacs3.login.ui.theme.SilverMist
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

enum class ResetStep {
    EMAIL, OTP, NEW_PASSWORD, SUCCESS
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ForgotPasswordDialog(
    onDismiss: () -> Unit
) {
    var step by remember { mutableStateOf(ResetStep.EMAIL) }
    var email by remember { mutableStateOf("") }
    var otp by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    
    // States for password visibility
    var passwordVisible by remember { mutableStateOf(false) }
    var confirmPasswordVisible by remember { mutableStateOf(false) }
    
    // State for countdown
    var resendCountdown by remember { mutableStateOf(60) }
    
    val scope = rememberCoroutineScope()

    // Countdown logic
    LaunchedEffect(step, resendCountdown) {
        if (step == ResetStep.OTP && resendCountdown > 0) {
            delay(1000L)
            resendCountdown--
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {},
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(16.dp),
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = when (step) {
                        ResetStep.EMAIL -> "Forgot Password"
                        ResetStep.OTP -> "Verify OTP"
                        ResetStep.NEW_PASSWORD -> "Reset Password"
                        ResetStep.SUCCESS -> "Success"
                    },
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                if (errorMessage != null) {
                    Text(
                        text = errorMessage!!,
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                }

                when (step) {
                    ResetStep.EMAIL -> {
                        Text(
                            "Enter your email to receive a verification code.",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                            modifier = Modifier.padding(bottom = 16.dp)
                        )
                        OutlinedTextField(
                            value = email,
                            onValueChange = { email = it },
                            label = { Text("Email Address") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                                unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                focusedLabelColor = MaterialTheme.colorScheme.primary,
                                unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                    }
                    ResetStep.OTP -> {
                        Text(
                            "A code has been sent to $email",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                            modifier = Modifier.padding(bottom = 16.dp)
                        )
                        
                        OtpInputField(
                            otp = otp,
                            onOtpChange = { otp = it },
                            count = 8
                        )
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        // Resend Countdown UI
                        if (resendCountdown > 0) {
                            Text(
                                text = "Resend code in ${resendCountdown}s",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            )
                        } else {
                            Text(
                                text = "Resend Code",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.clickable {
                                    scope.launch {
                                        isLoading = true
                                        val error = sendResetPasswordEmail(email)
                                        isLoading = false
                                        if (error == null) {
                                            resendCountdown = 60
                                            otp = ""
                                            errorMessage = null
                                        } else {
                                            errorMessage = error
                                        }
                                    }
                                }
                            )
                        }
                    }
                    ResetStep.NEW_PASSWORD -> {
                        OutlinedTextField(
                            value = newPassword,
                            onValueChange = { newPassword = it },
                            label = { Text("New Password") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true,
                            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            trailingIcon = {
                                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                    Icon(
                                        imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                                unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                focusedLabelColor = MaterialTheme.colorScheme.primary,
                                unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedTextField(
                            value = confirmPassword,
                            onValueChange = { confirmPassword = it },
                            label = { Text("Confirm New Password") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true,
                            visualTransformation = if (confirmPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            trailingIcon = {
                                IconButton(onClick = { confirmPasswordVisible = !confirmPasswordVisible }) {
                                    Icon(
                                        imageVector = if (confirmPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                                unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                focusedLabelColor = MaterialTheme.colorScheme.primary,
                                unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                    }
                    ResetStep.SUCCESS -> {
                        Text(
                            "Your password has been reset successfully!",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                            modifier = Modifier.padding(bottom = 16.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                if (step != ResetStep.SUCCESS) {
                    Button(
                        onClick = {
                            errorMessage = null
                            when (step) {
                                ResetStep.EMAIL -> {
                                    if (email.isBlank()) {
                                        errorMessage = "Please enter your email address."
                                        return@Button
                                    }
                                    isLoading = true
                                    scope.launch {
                                        val error = sendResetPasswordEmail(email)
                                        isLoading = false
                                        if (error == null) {
                                            step = ResetStep.OTP
                                            resendCountdown = 60
                                        }
                                        else errorMessage = error
                                    }
                                }
                                ResetStep.OTP -> {
                                    if (otp.length < 6) {
                                        errorMessage = "Please enter the OTP verification code."
                                        return@Button
                                    }
                                    isLoading = true
                                    scope.launch {
                                        val error = verifyResetOtp(email, otp)
                                        isLoading = false
                                        if (error == null) step = ResetStep.NEW_PASSWORD
                                        else errorMessage = error
                                    }
                                }
                                ResetStep.NEW_PASSWORD -> {
                                    if (newPassword.length < 6) {
                                        errorMessage = "Password must be at least 6 characters."
                                        return@Button
                                    }
                                    if (newPassword != confirmPassword) {
                                        errorMessage = "Passwords do not match."
                                        return@Button
                                    }
                                    isLoading = true
                                    scope.launch {
                                        val error = updateUserPassword(newPassword)
                                        isLoading = false
                                        if (error == null) step = ResetStep.SUCCESS
                                        else errorMessage = error
                                    }
                                }
                                else -> {}
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        enabled = !isLoading
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(color = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(24.dp))
                        } else {
                            Text(
                                when (step) {
                                    ResetStep.EMAIL -> "Send Code"
                                    ResetStep.OTP -> "Verify Code"
                                    ResetStep.NEW_PASSWORD -> "Update Password"
                                    else -> ""
                                },
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                } else {
                    Button(
                        onClick = onDismiss,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        )
                    ) {
                        Text("Back to Login", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    )
}

@Composable
fun OtpInputField(
    otp: String,
    onOtpChange: (String) -> Unit,
    count: Int = 8
) {
    val outlineColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
    val activeBorderColor = MaterialTheme.colorScheme.primary
    val boxBgColor = MaterialTheme.colorScheme.surfaceVariant
    val digitTextColor = MaterialTheme.colorScheme.onSurface

    BasicTextField(
        value = otp,
        onValueChange = {
            if (it.all { char -> char.isDigit() } && it.length <= count) {
                onOtpChange(it)
            }
        },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        decorationBox = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterHorizontally)
            ) {
                repeat(count) { index ->
                    val char = when {
                        index >= otp.length -> ""
                        else -> otp[index].toString()
                    }
                    val isFocused = otp.length == index
                    
                    Box(
                        modifier = Modifier
                            .width(32.dp)
                            .height(48.dp)
                            .border(
                                width = if (isFocused) 2.dp else 1.dp,
                                color = if (isFocused) activeBorderColor else outlineColor,
                                shape = RoundedCornerShape(8.dp)
                            )
                            .background(boxBgColor),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = char,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = digitTextColor
                        )
                    }
                }
            }
        }
    )
}
