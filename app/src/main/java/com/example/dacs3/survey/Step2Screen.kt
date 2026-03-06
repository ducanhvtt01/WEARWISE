package com.example.dacs3.survey

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun Step2Screen(viewModel: SurveyViewModel) {
    Column(modifier = Modifier
        .fillMaxSize()
        .verticalScroll(rememberScrollState())) {
        SurveyTitle("Your Body Profile", "Highlighting your best features with precise matching.")

        SectionLabel("BODY SHAPE")
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            listOf(
                "Hourglass",
                "Pear",
                "Rectangle",
                "Inverted Triangle",
                "Apple"
            ).forEach { shape ->
                SingleChoiceChip(
                    text = shape,
                    selected = viewModel.bodyShape == shape,
                    onClick = { viewModel.bodyShape = shape }
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        SectionLabel("SKIN TONE")
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            listOf("Fair", "Medium", "Olive", "Tan", "Deep").forEach { tone ->
                SingleChoiceChip(
                    text = tone,
                    selected = viewModel.skinTone == tone,
                    onClick = { viewModel.skinTone = tone }
                )
            }
        }
    }
}