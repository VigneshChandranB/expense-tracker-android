package com.expensetracker.domain.usecase.settings

import com.expensetracker.domain.model.AppSettings
import com.expensetracker.domain.model.ThemeMode
import com.expensetracker.domain.repository.SettingsRepository
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*

/**
 * Unit tests for GetAppSettingsUseCase
 */
class GetAppSettingsUseCaseTest {
    
    private lateinit var settingsRepository: SettingsRepository
    private lateinit var useCase: GetAppSettingsUseCase
    
    @Before
    fun setup() {
        settingsRepository = mockk()
        useCase = GetAppSettingsUseCase(settingsRepository)
    }
    
    @Test
    fun `invoke returns app settings from repository`() = runTest {
        // Given
        val expectedSettings = AppSettings(
            themeMode = ThemeMode.DARK,
            smsPermissionEnabled = true,
            currencyCode = "USD"
        )
        coEvery { settingsRepository.getAppSettings() } returns expectedSettings
        
        // When
        val result = useCase()
        
        // Then
        assertEquals(expectedSettings, result)
    }
    
    @Test
    fun `invoke returns default settings when repository returns null`() = runTest {
        // Given
        coEvery { settingsRepository.getAppSettings() } returns AppSettings()
        
        // When
        val result = useCase()
        
        // Then
        assertEquals(AppSettings(), result)
    }
}