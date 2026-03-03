package com.example.dacs3.survey

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun SurveyLayout(
    currentStep: Int,
    onBack: (() -> Unit)? = null,
    onNext: () -> Unit,
    content: @Composable () -> Unit
) {
    Scaffold(
        containerColor = OffWhite,
        topBar = {
            Column(modifier = Modifier.fillMaxWidth().padding(top = 48.dp, start = 24.dp, end = 24.dp)) {
                LinearProgressIndicator(
                    progress = currentStep / 3f,
                    modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                    color = AccentTeal, trackColor = SoftTeal
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text("Step $currentStep of 3", fontSize = 12.sp, color = SilverMist, fontWeight = FontWeight.Bold)
            }
        },
        bottomBar = {
            Row(
                modifier = Modifier.fillMaxWidth().padding(24.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (onBack != null) {
                    TextButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, null, tint = SilverMist)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Back", color = SilverMist, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                } else {
                    Spacer(modifier = Modifier.width(80.dp))
                }

                Button(
                    onClick = onNext,
                    colors = ButtonDefaults.buttonColors(containerColor = MidnightBlue),
                    shape = RoundedCornerShape(16.dp),
                    contentPadding = PaddingValues(horizontal = 24.dp, vertical = 16.dp)
                ) {
                    Text(if (currentStep == 3) "Complete" else "Next", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(if (currentStep == 3) Icons.Filled.Check else Icons.Filled.ArrowForward, null, tint = Color.White)
                }
            }
        }
    ) { innerPadding ->
        // Box này chứa NavHost, không có verticalScroll ở đây
        Box(modifier = Modifier.padding(innerPadding).fillMaxSize().padding(horizontal = 24.dp, vertical = 16.dp)) {
            content()
        }
    }
}