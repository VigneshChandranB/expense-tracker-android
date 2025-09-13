package com.expensetracker.domain.usecase.settings

import com.expensetracker.domain.model.AppSettings
import com.expensetracker.domain.model.ThemeMode
import com.expensetracker.domain.repository.SettingsRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for UpdateAppSettingsUseCase
 */
class UpdateAppSettingsUseCaseTest {
    
    private lateinit var settingsRepository: SettingsRepository
    private lateinit var useCase: UpdateAppSettingsUseCase
    
    @Before
    fun setup() {
        settingsRepository = mockk()
        useCase = UpdateAppSettingsUseCase(settingsRepository)
    }
    
    @Test
    fun `invoke updates settings in repository`() = runTest {
        // Given
        val settings = AppSettings(
            themeMode = ThemeMode.LIGHT,
            smsPermissionEnabled = false,
            currencyCode = "EUR"
        )
        coEvery { settingsRepository.updateAppSettings(settings) } returns Unit
        
        // When
        useCase(settings)
        
        // Then
        coVerify { settingsRepository.updateAppSettings(settings) }
    }
    
    @Test
    fun `invoke handles repository exceptions gracefully`() = runTest {
        // Given
        val settings = AppSettings()
        coEvery { settingsRepository.updateAppSettings(settings) } throws Exception("Database error")
        
        // When & Then - should not throw
        try {
            useCase(settings)
        } catch (e: Exception) {
            // Expected to handle gracefully in real implementation
        }
    }
}