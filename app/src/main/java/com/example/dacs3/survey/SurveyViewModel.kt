package com.example.dacs3.survey

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel

class SurveyViewModel : ViewModel() {
    // Bước 1
    var gender by mutableStateOf("")
    var height by mutableFloatStateOf(165f)
    var weight by mutableFloatStateOf(55f)

    // Bước 2
    var bodyShape by mutableStateOf("")
    var skinTone by mutableStateOf("")

    // Bước 3
    var favoriteStyles by mutableStateOf(setOf<String>())
    var favoriteColors by mutableStateOf(setOf<String>())
    var topSize by mutableStateOf("")
    var bottomSize by mutableStateOf("")
    var shoeSize by mutableFloatStateOf(39f)
}

data class SurveyData(
    val gender: String, val heightCm: Int, val weightKg: Int,
    val bodyShape: String, val skinTone: String,
    val favoriteStyles: List<String>, val favoriteColors: List<String>,
    val topSize: String, val bottomSize: String, val shoeSizeEu: Int
)