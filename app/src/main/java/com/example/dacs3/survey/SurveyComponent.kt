package com.example.dacs3.survey

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// BẢNG MÀU
val OffWhite = Color(0xFFFBFBFC)
val MidnightBlue = Color(0xFF1A237E)
val SilverMist = Color(0xFF8D99AE)
val AccentTeal = Color(0xFF00BFA5)
val SoftTeal = Color(0xFFE0F2F1)

@Composable
fun SurveyTitle(title: String, subtitle: String) {
    Text(text = title, fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, color = MidnightBlue, modifier = Modifier.padding(bottom = 8.dp))
    Text(text = subtitle, fontSize = 14.sp, color = SilverMist, modifier = Modifier.padding(bottom = 32.dp))
}

@Composable
fun SectionLabel(text: String) {
    Text(text = text, fontSize = 11.sp, fontWeight = FontWeight.Black, color = SilverMist, letterSpacing = 1.sp, modifier = Modifier.padding(bottom = 12.dp))
}

@Composable
fun SingleChoiceChip(text: String, selected: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(if (selected) MidnightBlue else Color.White)
            .border(1.dp, if (selected) Color.Transparent else SilverMist.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text = text, color = if (selected) Color.White else MidnightBlue, fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium, fontSize = 14.sp)
    }
}

@Composable
fun MultiChoiceChip(text: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(if (selected) SoftTeal else Color.White)
            .border(1.dp, if (selected) AccentTeal else SilverMist.copy(alpha = 0.3f), RoundedCornerShape(20.dp))
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (selected) {
                Icon(Icons.Filled.Check, null, tint = AccentTeal, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
            }
            Text(text = text, color = if (selected) MidnightBlue else SilverMist, fontWeight = FontWeight.Medium, fontSize = 13.sp)
        }
    }
}