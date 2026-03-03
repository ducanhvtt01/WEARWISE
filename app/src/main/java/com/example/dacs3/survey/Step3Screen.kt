package com.example.dacs3.survey

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun Step3Screen(viewModel: SurveyViewModel) {
    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        SurveyTitle("Style & Sizing", "What defines your daily look?")
        SectionLabel("FAVORITE STYLES")
        FlowRow(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            listOf("Minimalist", "Vintage", "Streetwear", "Formal", "Casual", "Y2K", "Boho").forEach { style ->
                MultiChoiceChip(text = style, selected = viewModel.favoriteStyles.contains(style), onClick = {
                    viewModel.favoriteStyles = if (viewModel.favoriteStyles.contains(style)) viewModel.favoriteStyles - style else viewModel.favoriteStyles + style
                })
            }
        }
        Spacer(modifier = Modifier.height(24.dp))
        SectionLabel("DEFAULT SIZES")
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Top", fontSize = 12.sp, color = SilverMist)
                DropdownSizeSelector(selected = viewModel.topSize, options = listOf("XS", "S", "M", "L", "XL", "XXL"), onSelect = { viewModel.topSize = it })
            }
            Column(modifier = Modifier.weight(1f)) {
                Text("Bottom", fontSize = 12.sp, color = SilverMist)
                DropdownSizeSelector(selected = viewModel.bottomSize, options = listOf("S", "M", "L", "XL", "28", "30", "32", "34"), onSelect = { viewModel.bottomSize = it })
            }
        }
        Spacer(modifier = Modifier.height(24.dp))
        SectionLabel("SHOE SIZE (EU): ${viewModel.shoeSize.roundToInt()}")
        Slider(value = viewModel.shoeSize, onValueChange = { viewModel.shoeSize = it }, valueRange = 35f..46f, steps = 10, colors = SliderDefaults.colors(thumbColor = MidnightBlue, activeTrackColor = MidnightBlue))
    }
}