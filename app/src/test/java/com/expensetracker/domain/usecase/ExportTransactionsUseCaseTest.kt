package com.expensetracker.domain.usecase

import com.expensetracker.domain.model.DateRange
import com.expensetracker.domain.model.ExportConfig
import com.expensetracker.domain.model.ExportFormat
import com.expensetracker.domain.model.ExportResult
import com.expensetracker.domain.repository.ExportRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Unit tests for ExportTransactionsUseCase
 */
class ExportTransactionsUseCaseTest {
    
    private lateinit var exportRepository: ExportRepository
    private lateinit var useCase: ExportTransactionsUseCase
    
    @Before
    fun setup() {
        exportRepository = mockk()
        useCase = ExportTransactionsUseCase(exportRepository)
    }
    
    @Test
    fun `should return success when export is successful`() = runTest {
        // Given
        val config = ExportConfig(
            format = ExportFormat.CSV,
            dateRange = DateRange.currentMonth()
        )
        val expectedResult = ExportResult.Success("/path/to/file.csv", 1024L)
        
        coEvery { exportRepository.validateExportConfig(config) } returns true
        coEvery { exportRepository.getExportSizeEstimate(config) } returns 1024L
        coEvery { exportRepository.exportTransactions(config) } returns expectedResult
        
        // When
        val result = useCase(config)
        
        // Then
        assertEquals(expectedResult, result)
        coVerify { exportRepository.validateExportConfig(config) }
        coVerify { exportRepository.getExportSizeEstimate(config) }
        coVerify { exportRepository.exportTransactions(config) }
    }
    
    @Test
    fun `should return error when config is invalid`() = runTest {
        // Given
        val config = ExportConfig(
            format = ExportFormat.CSV,
            dateRange = DateRange.currentMonth()
        )
        
        coEvery { exportRepository.validateExportConfig(config) } returns false
        
        // When
        val result = useCase(config)
        
        // Then
        assertTrue(result is ExportResult.Error)
        assertEquals("Invalid export configuration", result.message)
        coVerify { exportRepository.validateExportConfig(config) }
        coVerify(exactly = 0) { exportRepository.exportTransactions(any()) }
    }
    
    @Test
    fun `should return error when file size is too large`() = runTest {
        // Given
        val config = ExportConfig(
            format = ExportFormat.CSV,
            dateRange = DateRange.currentMonth()
        )
        val largeFileSize = 60L * 1024 * 1024 // 60MB
        
        coEvery { exportRepository.validateExportConfig(config) } returns true
        coEvery { exportRepository.getExportSizeEstimate(config) } returns largeFileSize
        
        // When
        val result = useCase(config)
        
        // Then
        assertTrue(result is ExportResult.Error)
        assertTrue(result.message.contains("too large"))
        coVerify { exportRepository.validateExportConfig(config) }
        coVerify { exportRepository.getExportSizeEstimate(config) }
        coVerify(exactly = 0) { exportRepository.exportTransactions(any()) }
    }
    
    @Test
    fun `should return error when repository throws exception`() = runTest {
        // Given
        val config = ExportConfig(
            format = ExportFormat.CSV,
            dateRange = DateRange.currentMonth()
        )
        val exception = RuntimeException("Database error")
        
        coEvery { exportRepository.validateExportConfig(config) } returns true
        coEvery { exportRepository.getExportSizeEstimate(config) } returns 1024L
        coEvery { exportRepository.exportTransactions(config) } throws exception
        
        // When
        val result = useCase(config)
        
        // Then
        assertTrue(result is ExportResult.Error)
        assertEquals("Export failed: Database error", result.message)
        assertEquals(exception, result.cause)
    }
}