package com.example.dacs3.dashboard

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.isToggleable
import androidx.compose.ui.test.onNodeWithTag
import org.junit.Rule
import org.junit.Test
import org.junit.Assert.assertTrue

class TodoCardTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val sampleTodo = com.example.dacs3.connectDB.TodoDbModel(
        id = "1",
        userId = "test-user",
        title = "Pack for trip",
        description = "Need to pack 3 shirts and 2 pants",
        type = "packing",
        dueDate = "2026-05-25",
        isCompleted = false
    )

    @Test
    fun todoCard_displaysTitleAndDescription() {
        composeTestRule.setContent {
            TodoCard(
                todo = sampleTodo,
                onCheckChanged = {},
                onDelete = {}
            )
        }

        // Kiểm tra Tiêu đề có hiển thị không
        composeTestRule.onNodeWithText("Pack for trip").assertIsDisplayed()
        
        // Kiểm tra Mô tả có hiển thị không
        composeTestRule.onNodeWithText("Need to pack 3 shirts and 2 pants").assertIsDisplayed()
        
        // Kiểm tra Hạn chót có hiển thị đúng định dạng không
        composeTestRule.onNodeWithText("Due: 2026-05-25").assertIsDisplayed()
    }

    @Test
    fun todoCard_triggersOnDelete_whenDeleteIconClicked() {
        var deleteClicked = false
        
        composeTestRule.setContent {
            TodoCard(
                todo = sampleTodo,
                onCheckChanged = {},
                onDelete = { deleteClicked = true }
            )
        }

        // Giả lập bấm vào biểu tượng thùng rác (Delete)
        // Icon Delete có contentDescription là "Delete" trong code TodoCard.kt
        composeTestRule.onNodeWithContentDescription("Delete").performClick()
        
        assertTrue("Delete callback was not triggered!", deleteClicked)
    }
}
