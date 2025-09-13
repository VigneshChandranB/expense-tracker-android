package com.expensetracker.presentation.settings

import com.expensetracker.domain.usecase.settings.GetAppSettingsUseCase
import com.expensetracker.domain.usecase.settings.UpdateAppSettingsUseCase
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*

/**
 * Unit tests for SettingsViewModel
 */
@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {
    
    private lateinit var getAppSettingsUseCase: GetAppSettingsUseCase
    private lateinit var updateAppSettingsUseCase: UpdateAppSettingsUseCase
    private lateinit var viewModel: SettingsViewModel
    
    private val testDispatcher = StandardTestDispatcher()
    
    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        getAppSettingsUseCase = mockk()
        updateAppSettingsUseCase = mockk()
        viewModel = SettingsViewModel(getAppSettingsUseCase, updateAppSettingsUseCase)
    }
    
    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }
    
    @Test
    fun `initial state is loading with default values`() {
        // When
        val initialState = viewModel.uiState.value
        
        // Then
        assertTrue(initialState.isLoading)
        assertEquals("", initialState.appVersion)
        assertEquals("", initialState.buildNumber)
        assertNull(initialState.error)
    }
    
    @Test
    fun `loadAppInfo updates state with app information`() {
        // When
        testDispatcher.scheduler.advanceUntilIdle()
        val state = viewModel.uiState.value
        
        // Then
        assertFalse(state.isLoading)
        assertEquals("1.0.0", state.appVersion)
        assertEquals("1", state.buildNumber)
    }
}