package com.example.dacs3.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun ProfileScreen() {
    var notificationsEnabled by remember { mutableStateOf(true) }
    var darkModeEnabled by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(OffWhite)
            .padding(horizontal = 24.dp)
    ) {
        // --- HEADER ---
        item {
            Text(
                text = "Profile",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = MidnightBlue,
                modifier = Modifier.padding(top = 40.dp, bottom = 24.dp)
            )
        }

        // --- USER INFO CARD ---
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(8.dp, RoundedCornerShape(24.dp), spotColor = LightGray),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Row(
                    modifier = Modifier.padding(24.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(100.dp)
                            .background(SoftTeal, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("A", fontSize = 40.sp, fontWeight = FontWeight.Bold, color = AccentTeal)

                        // Nút Edit Avatar nhỏ ở góc
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .offset(x = 4.dp, y = 4.dp)
                                .size(32.dp)
                                .background(MidnightBlue, CircleShape)
                                .clickable { /* TODO: Đổi ảnh đại diện */ },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Filled.Edit, contentDescription = "Edit", tint = Color.White, modifier = Modifier.size(16.dp))
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    Column(
                    modifier = Modifier.padding(start = 55.dp),
                    horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Alexander", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = MidnightBlue)
                        Spacer(modifier = Modifier.height(10.dp))

                        Text("alexander@style.com", fontSize = 14.sp, color = SilverMist)
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = { /* TODO: Chỉnh sửa hồ sơ */ },
                            colors = ButtonDefaults.buttonColors(containerColor = LightGray, contentColor = MidnightBlue),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Edit Profile", fontWeight = FontWeight.SemiBold)
                        }
                     }



                }
            }
        }

        item { Spacer(modifier = Modifier.height(32.dp)) }

        // --- PERSONALIZATION SECTION ---
        item {
            SectionTitle("AI Personalization")
            SettingRow(icon = Icons.Outlined.Straighten, title = "My Measurements", subtitle = "Size M, 175cm, 70kg")
            SettingRow(icon = Icons.Outlined.Style, title = "Style Preferences", subtitle = "Minimalist, Smart Casual")
        }

        item { Spacer(modifier = Modifier.height(24.dp)) }

        // --- SETTINGS SECTION ---
        item {
            SectionTitle("App Settings")
            SettingToggleRow(icon = Icons.Outlined.Notifications, title = "Notifications", isChecked = notificationsEnabled, onCheckedChange = { notificationsEnabled = it })
            SettingToggleRow(icon = Icons.Outlined.DarkMode, title = "Dark Mode", isChecked = darkModeEnabled, onCheckedChange = { darkModeEnabled = it })
            SettingRow(icon = Icons.Outlined.Language, title = "Language", subtitle = "English (US)")
        }

        item { Spacer(modifier = Modifier.height(24.dp)) }

        // --- SUPPORT SECTION ---
        item {
            SectionTitle("Support")
            SettingRow(icon = Icons.Outlined.HelpOutline, title = "Help Center")
            SettingRow(icon = Icons.Outlined.PrivacyTip, title = "Privacy Policy")
        }

        item { Spacer(modifier = Modifier.height(32.dp)) }

        // --- LOGOUT BUTTON ---
        item {
            OutlinedButton(
                onClick = { /* TODO: Xử lý Đăng xuất và quay về LoginScreen */ },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFE53935)), // Màu đỏ báo hiệu thoát
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE53935).copy(alpha = 0.5f))
            ) {
                Icon(Icons.Outlined.ExitToApp, contentDescription = "Log out")
                Spacer(modifier = Modifier.width(8.dp))
                Text("Log Out", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        }

        item { Spacer(modifier = Modifier.height(120.dp)) } // Tránh bị Bottom Bar che
    }
}

@Composable
fun SectionTitle(title: String) {
    Text(
        text = title,
        fontSize = 14.sp,
        fontWeight = FontWeight.Bold,
        color = SilverMist,
        modifier = Modifier.padding(bottom = 8.dp, start = 4.dp),
        letterSpacing = 1.sp
    )
}

@Composable
fun SettingRow(icon: ImageVector, title: String, subtitle: String? = null) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable { /* TODO: Xử lý khi bấm vào mục này */ }
            .padding(vertical = 12.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(40.dp).background(Color.White, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = MidnightBlue, modifier = Modifier.size(20.dp))
        }

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, fontSize = 16.sp, fontWeight = FontWeight.Medium, color = MidnightBlue)
            if (subtitle != null) {
                Text(text = subtitle, fontSize = 12.sp, color = SilverMist)
            }
        }

        Icon(Icons.Filled.ChevronRight, contentDescription = "Go", tint = SilverMist)
    }
}

@Composable
fun SettingToggleRow(icon: ImageVector, title: String, isChecked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(40.dp).background(Color.White, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = MidnightBlue, modifier = Modifier.size(20.dp))
        }

        Spacer(modifier = Modifier.width(16.dp))

        Text(text = title, fontSize = 16.sp, fontWeight = FontWeight.Medium, color = MidnightBlue, modifier = Modifier.weight(1f))

        Switch(
            checked = isChecked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = AccentTeal, uncheckedTrackColor = Color.Gray, uncheckedThumbColor = Color.White))
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewProfileScreen() {
    ProfileScreen()
}