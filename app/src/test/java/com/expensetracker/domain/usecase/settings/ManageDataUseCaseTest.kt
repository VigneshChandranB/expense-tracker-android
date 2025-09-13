package com.expensetracker.domain.usecase.settings

import com.expensetracker.domain.repository.SettingsRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for ManageDataUseCase
 */
class ManageDataUseCaseTest {
    
    private lateinit var settingsRepository: SettingsRepository
    private lateinit var useCase: ManageDataUseCase
    
    @Before
    fun setup() {
        settingsRepository = mockk()
        useCase = ManageDataUseCase(settingsRepository)
    }
    
    @Test
    fun `deleteAllData calls repository deleteAllData`() = runTest {
        // Given
        coEvery { settingsRepository.deleteAllData() } returns Unit
        
        // When
        useCase.deleteAllData()
        
        // Then
        coVerify { settingsRepository.deleteAllData() }
    }
    
    @Test
    fun `deleteSmsData calls repository deleteSmsData`() = runTest {
        // Given
        coEvery { settingsRepository.deleteSmsData() } returns Unit
        
        // When
        useCase.deleteSmsData()
        
        // Then
        coVerify { settingsRepository.deleteSmsData() }
    }
    
    @Test
    fun `deleteOldTransactions calls repository with correct parameters`() = runTest {
        // Given
        val retentionMonths = 12
        coEvery { settingsRepository.deleteOldTransactions(retentionMonths) } returns Unit
        
        // When
        useCase.deleteOldTransactions(retentionMonths)
        
        // Then
        coVerify { settingsRepository.deleteOldTransactions(retentionMonths) }
    }
    
    @Test
    fun `deleteOldTransactions handles invalid retention period`() = runTest {
        // Given
        val invalidRetentionMonths = -1
        
        // When & Then - should handle gracefully
        try {
            useCase.deleteOldTransactions(invalidRetentionMonths)
        } catch (e: IllegalArgumentException) {
            // Expected for negative values
        }
    }
}