package com.example.dacs3.dashboard

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.assertIsDisplayed
import org.junit.Rule
import org.junit.Test
import org.junit.Assert.assertTrue

class CustomChipTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun customChip_displaysCorrectText() {
        // Render Component
        composeTestRule.setContent {
            CustomChip(
                text = "T-Shirt",
                isSelected = false,
                onClick = {}
            )
        }

        // Kiểm tra xem chữ "T-Shirt" có xuất hiện trên màn hình ảo không
        composeTestRule.onNodeWithText("T-Shirt").assertIsDisplayed()
    }

    @Test
    fun customChip_triggersOnClick_whenClicked() {
        var isClicked = false
        
        // Render Component
        composeTestRule.setContent {
            CustomChip(
                text = "Top",
                isSelected = false,
                onClick = { isClicked = true }
            )
        }

        // Mô phỏng người dùng bấm vào component có chữ "Top"
        composeTestRule.onNodeWithText("Top").performClick()

        // Kiểm tra biến isClicked đã được chuyển thành true
        assertTrue("Click action was not triggered!", isClicked)
    }
}
