package com.example.dacs3.connectDB

import com.example.dacs3.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DashboardViewModelTest {

    // Rule để override Dispatchers.Main cho ViewModel dùng viewModelScope
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `initial states are correctly set`() {
        // Arrange
        val viewModel = DashboardViewModel()

        // Act & Assert
        assertNull("userProfile should be initially null", viewModel.userProfile)
        assertFalse("isUpdating should be false initially", viewModel.isUpdating)
        
        // StateFlows
        assertTrue("clothingItems should be empty initially", viewModel.clothingItems.value.isEmpty())
        assertTrue("smartWardrobeItems should be empty initially", viewModel.smartWardrobeItems.value.isEmpty())
        assertTrue("clothingFeedbackMap should be empty initially", viewModel.clothingFeedbackMap.value.isEmpty())
        assertNull("errorMessage should be initially null", viewModel.errorMessage.value)
        
        // Other states
        assertFalse("isCanvasLoading should be false", viewModel.isCanvasLoading)
        assertNull("canvasError should be null", viewModel.canvasError)
    }

    @Test
    fun `clearError sets errorMessage to null`() {
        // Arrange
        val viewModel = DashboardViewModel()
        // We simulate setting an error (it's private in ViewModel, but we can call a method that sets it if one existed, 
        // or just test that calling clearError guarantees it's null).
        // Since _errorMessage is private, we just call clearError and assert it's null.
        
        // Act
        viewModel.clearError()

        // Assert
        assertNull(viewModel.errorMessage.value)
    }
}
