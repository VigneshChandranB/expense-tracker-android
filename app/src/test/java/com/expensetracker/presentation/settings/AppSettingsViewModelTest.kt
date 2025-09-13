package com.expensetracker.presentation.settings

import com.expensetracker.domain.model.AppSettings
import com.expensetracker.domain.model.ThemeMode
import com.expensetracker.domain.usecase.settings.GetAppSettingsUseCase
import com.expensetracker.domain.usecase.settings.UpdateAppSettingsUseCase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*

/**
 * Unit tests for AppSettingsViewModel
 */
@OptIn(ExperimentalCoroutinesApi::class)
class AppSettingsViewModelTest {
    
    private lateinit var getAppSettingsUseCase: GetAppSettingsUseCase
    private lateinit var updateAppSettingsUseCase: UpdateAppSettingsUseCase
    private lateinit var viewModel: AppSettingsViewModel
    
    private val testDispatcher = StandardTestDispatcher()
    
    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        getAppSettingsUseCase = mockk()
        updateAppSettingsUseCase = mockk()
    }
    
    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }
    
    @Test
    fun `initial state is loading`() {
        // Given
        coEvery { getAppSettingsUseCase() } returns AppSettings()
        
        // When
        viewModel = AppSettingsViewModel(getAppSettingsUseCase, updateAppSettingsUseCase)
        val initialState = viewModel.uiState.value
        
        // Then
        assertTrue(initialState.isLoading)
    }
    
    @Test
    fun `loadSettings updates state with settings`() = runTest {
        // Given
        val settings = AppSettings(
            themeMode = ThemeMode.DARK,
            smsPermissionEnabled = true,
            currencyCode = "USD"
        )
        coEvery { getAppSettingsUseCase() } returns settings
        
        // When
        viewModel = AppSettingsViewModel(getAppSettingsUseCase, updateAppSettingsUseCase)
        testDispatcher.scheduler.advanceUntilIdle()
        
        // Then
        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertEquals(settings, state.settings)
    }
    
    @Test
    fun `updateTheme calls updateAppSettingsUseCase`() = runTest {
        // Given
        val initialSettings = AppSettings()
        coEvery { getAppSettingsUseCase() } returns initialSettings
        coEvery { updateAppSettingsUseCase(any()) } returns Unit
        
        viewModel = AppSettingsViewModel(getAppSettingsUseCase, updateAppSettingsUseCase)
        testDispatcher.scheduler.advanceUntilIdle()
        
        // When
        viewModel.updateTheme(ThemeMode.DARK)
        testDispatcher.scheduler.advanceUntilIdle()
        
        // Then
        coVerify {
            updateAppSettingsUseCase(
                match { it.themeMode == ThemeMode.DARK }
            )
        }
    }
    
    @Test
    fun `updateCurrency calls updateAppSettingsUseCase`() = runTest {
        // Given
        val initialSettings = AppSettings()
        coEvery { getAppSettingsUseCase() } returns initialSettings
        coEvery { updateAppSettingsUseCase(any()) } returns Unit
        
        viewModel = AppSettingsViewModel(getAppSettingsUseCase, updateAppSettingsUseCase)
        testDispatcher.scheduler.advanceUntilIdle()
        
        // When
        viewModel.updateCurrency("EUR")
        testDispatcher.scheduler.advanceUntilIdle()
        
        // Then
        coVerify {
            updateAppSettingsUseCase(
                match { it.currencyCode == "EUR" }
            )
        }
    }
    
    @Test
    fun `updateAutoCategorization calls updateAppSettingsUseCase`() = runTest {
        // Given
        val initialSettings = AppSettings()
        coEvery { getAppSettingsUseCase() } returns initialSettings
        coEvery { updateAppSettingsUseCase(any()) } returns Unit
        
        viewModel = AppSettingsViewModel(getAppSettingsUseCase, updateAppSettingsUseCase)
        testDispatcher.scheduler.advanceUntilIdle()
        
        // When
        viewModel.updateAutoCategorization(false)
        testDispatcher.scheduler.advanceUntilIdle()
        
        // Then
        coVerify {
            updateAppSettingsUseCase(
                match { it.autoCategorizationEnabled == false }
            )
        }
    }
}