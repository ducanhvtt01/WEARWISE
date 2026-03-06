package com.example.dacs3.survey

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

@Composable
fun Step1Screen(viewModel: SurveyViewModel) {
    Column(modifier = Modifier
        .fillMaxSize()
        .verticalScroll(rememberScrollState())) {
        SurveyTitle(
            "Tell us about yourself",
            "This helps our AI tailor the perfect fit for your body."
        )
        SectionLabel("GENDER")
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            listOf("Male", "Female", "Other").forEach { g ->
                SingleChoiceChip(
                    text = g,
                    selected = viewModel.gender == g.lowercase(),
                    modifier = Modifier.weight(1f),
                    onClick = { viewModel.gender = g.lowercase() })
            }
        }
        Spacer(modifier = Modifier.height(32.dp))
        SectionLabel("HEIGHT: ${viewModel.height.roundToInt()} cm")
        Slider(
            value = viewModel.height,
            onValueChange = { viewModel.height = it },
            valueRange = 140f..210f,
            colors = SliderDefaults.colors(
                thumbColor = MaterialTheme.colorScheme.primary,
                activeTrackColor = MaterialTheme.colorScheme.primary
            )
        )
        Spacer(modifier = Modifier.height(24.dp))
        SectionLabel("WEIGHT: ${viewModel.weight.roundToInt()} kg")
        Slider(
            value = viewModel.weight,
            onValueChange = { viewModel.weight = it },
            valueRange = 40f..150f,
            colors = SliderDefaults.colors(
                thumbColor = MaterialTheme.colorScheme.primary,
                activeTrackColor = MaterialTheme.colorScheme.primary
            )
        )
    }
}