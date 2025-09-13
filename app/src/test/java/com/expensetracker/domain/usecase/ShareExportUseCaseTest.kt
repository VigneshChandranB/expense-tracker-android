package com.expensetracker.domain.usecase

import com.expensetracker.domain.model.ShareOption
import com.expensetracker.domain.repository.ExportRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Unit tests for ShareExportUseCase
 */
class ShareExportUseCaseTest {
    
    private lateinit var exportRepository: ExportRepository
    private lateinit var useCase: ShareExportUseCase
    
    @Before
    fun setup() {
        exportRepository = mockk()
        useCase = ShareExportUseCase(exportRepository)
    }
    
    @Test
    fun `should return true when sharing is successful`() = runTest {
        // Given
        val filePath = "/path/to/file.csv"
        val shareOption = ShareOption.EMAIL
        
        coEvery { exportRepository.shareExportedFile(filePath, shareOption) } returns true
        
        // When
        val result = useCase(filePath, shareOption)
        
        // Then
        assertTrue(result)
        coVerify { exportRepository.shareExportedFile(filePath, shareOption) }
    }
    
    @Test
    fun `should return false when sharing fails`() = runTest {
        // Given
        val filePath = "/path/to/file.csv"
        val shareOption = ShareOption.EMAIL
        
        coEvery { exportRepository.shareExportedFile(filePath, shareOption) } returns false
        
        // When
        val result = useCase(filePath, shareOption)
        
        // Then
        assertFalse(result)
        coVerify { exportRepository.shareExportedFile(filePath, shareOption) }
    }
    
    @Test
    fun `should return false when repository throws exception`() = runTest {
        // Given
        val filePath = "/path/to/file.csv"
        val shareOption = ShareOption.EMAIL
        
        coEvery { exportRepository.shareExportedFile(filePath, shareOption) } throws RuntimeException("Share failed")
        
        // When
        val result = useCase(filePath, shareOption)
        
        // Then
        assertFalse(result)
        coVerify { exportRepository.shareExportedFile(filePath, shareOption) }
    }
}