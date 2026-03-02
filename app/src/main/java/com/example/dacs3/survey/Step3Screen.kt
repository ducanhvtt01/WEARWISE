package com.example.dacs3.survey

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun Step3Screen(
    viewModel: SurveyViewModel,
    onBack: () -> Unit,
    onComplete: () -> Unit
) {
    SurveyLayout(
        currentStep = 3,
        onBack = onBack,
        onNext = onComplete
    ) {
        SurveyTitle(
            title = "Style & Sizing",
            subtitle = "What defines your daily look?"
        )

        // ==========================================
        // 1. FAVORITE STYLES (Chọn nhiều)
        // ==========================================
        SectionLabel(text = "FAVORITE STYLES (Select multiple)")

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            val styles = listOf(
                "Minimalist", "Vintage", "Streetwear",
                "Formal", "Casual", "Y2K", "Boho"
            )

            styles.forEach { style ->
                MultiChoiceChip(
                    text = style,
                    selected = viewModel.favoriteStyles.contains(style),
                    onClick = {
                        // Thêm hoặc bớt style khỏi Set trong ViewModel
                        if (viewModel.favoriteStyles.contains(style)) {
                            viewModel.favoriteStyles -= style
                        } else {
                            viewModel.favoriteStyles += style
                        }
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // ==========================================
        // 2. FAVORITE COLORS (Chọn nhiều)
        // ==========================================
        SectionLabel(text = "FAVORITE COLORS")

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            val colors = listOf(
                "Black", "White", "Earth Tones",
                "Pastel", "Neon", "Navy"
            )

            colors.forEach { color ->
                MultiChoiceChip(
                    text = color,
                    selected = viewModel.favoriteColors.contains(color),
                    onClick = {
                        // Thêm hoặc bớt color khỏi Set trong ViewModel
                        if (viewModel.favoriteColors.contains(color)) {
                            viewModel.favoriteColors -= color
                        } else {
                            viewModel.favoriteColors += color
                        }
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // ==========================================
        // 3. DEFAULT SIZES (Kích cỡ áo quần)
        // ==========================================
        SectionLabel(text = "DEFAULT SIZES")

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Cột Size Áo (Top)
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = "Top",
                    fontSize = 12.sp,
                    color = SilverMist,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                DropdownSizeSelector(
                    selected = viewModel.topSize,
                    options = listOf("XS", "S", "M", "L", "XL", "XXL"),
                    onSelect = { selectedSize ->
                        viewModel.topSize = selectedSize
                    }
                )
            }

            // Cột Size Quần (Bottom)
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = "Bottom",
                    fontSize = 12.sp,
                    color = SilverMist,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                DropdownSizeSelector(
                    selected = viewModel.bottomSize,
                    options = listOf("S", "M", "L", "XL", "28", "30", "32", "34"),
                    onSelect = { selectedSize ->
                        viewModel.bottomSize = selectedSize
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // ==========================================
        // 4. SHOE SIZE (Kích cỡ giày)
        // ==========================================
        SectionLabel(text = "SHOE SIZE (EU): ${viewModel.shoeSize.roundToInt()}")

        Slider(
            value = viewModel.shoeSize,
            onValueChange = { newSize ->
                viewModel.shoeSize = newSize
            },
            valueRange = 35f..46f,
            steps = 10, // Tạo các nấc dừng để chọn size chẵn (35, 36, 37...)
            colors = SliderDefaults.colors(
                thumbColor = MidnightBlue,
                activeTrackColor = MidnightBlue,
                inactiveTrackColor = SoftTeal
            ),
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
fun DropdownSizeSelector(
    selected: String,
    options: List<String>,
    onSelect: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Box {
        OutlinedButton(
            onClick = { expanded = true },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = MidnightBlue
            )
        ) {
            Text(
                text = if (selected.isEmpty()) "Select" else selected
            )
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.background(Color.White)
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(text = option) },
                    onClick = {
                        onSelect(option)
                        expanded = false
                    }
                )
            }
        }
    }
}