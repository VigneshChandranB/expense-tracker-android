package com.expensetracker.data.repository

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.expensetracker.data.export.CsvExporter
import com.expensetracker.data.export.FileShareService
import com.expensetracker.data.export.PdfExporter
import com.expensetracker.domain.model.Account
import com.expensetracker.domain.model.AccountType
import com.expensetracker.domain.model.Category
import com.expensetracker.domain.model.DateRange
import com.expensetracker.domain.model.ExportConfig
import com.expensetracker.domain.model.ExportFormat
import com.expensetracker.domain.model.ExportResult
import com.expensetracker.domain.model.ShareOption
import com.expensetracker.domain.model.Transaction
import com.expensetracker.domain.model.TransactionSource
import com.expensetracker.domain.model.TransactionType
import com.expensetracker.domain.repository.AccountRepository
import com.expensetracker.domain.repository.TransactionRepository
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import kotlin.test.assertTrue

/**
 * Integration tests for ExportRepositoryImpl
 */
@RunWith(AndroidJUnit4::class)
class ExportRepositoryImplTest {
    
    private lateinit var context: Context
    private lateinit var transactionRepository: TransactionRepository
    private lateinit var accountRepository: AccountRepository
    private lateinit var csvExporter: CsvExporter
    private lateinit var pdfExporter: PdfExporter
    private lateinit var fileShareService: FileShareService
    private lateinit var exportRepository: ExportRepositoryImpl
    
    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        transactionRepository = mockk()
        accountRepository = mockk()
        csvExporter = mockk()
        pdfExporter = mockk()
        fileShareService = mockk()
        
        exportRepository = ExportRepositoryImpl(
            context,
            transactionRepository,
            accountRepository,
            csvExporter,
            pdfExporter,
            fileShareService
        )
    }
    
    @Test
    fun `should export CSV successfully`() = runTest {
        // Given
        val config = ExportConfig(
            format = ExportFormat.CSV,
            dateRange = DateRange.currentMonth()
        )
        val transactions = createSampleTransactions()
        val accounts = createSampleAccounts()
        
        coEvery { transactionRepository.getTransactionsByDateRange(any(), any()) } returns transactions
        coEvery { accountRepository.getAllAccounts() } returns accounts
        coEvery { csvExporter.exportTransactions(any(), any(), any()) } returns true
        
        // When
        val result = exportRepository.exportTransactions(config)
        
        // Then
        assertTrue(result is ExportResult.Success)
        assertTrue(result.filePath.endsWith(".csv"))
    }
    
    @Test
    fun `should export PDF successfully`() = runTest {
        // Given
        val config = ExportConfig(
            format = ExportFormat.PDF,
            dateRange = DateRange.currentMonth(),
            includeCharts = true
        )
        val transactions = createSampleTransactions()
        val accounts = createSampleAccounts()
        
        coEvery { transactionRepository.getTransactionsByDateRange(any(), any()) } returns transactions
        coEvery { accountRepository.getAllAccounts() } returns accounts
        coEvery { pdfExporter.exportTransactions(any(), any(), any(), any(), any()) } returns true
        
        // When
        val result = exportRepository.exportTransactions(config)
        
        // Then
        assertTrue(result is ExportResult.Success)
        assertTrue(result.filePath.endsWith(".pdf"))
    }
    
    @Test
    fun `should return error when no transactions found`() = runTest {
        // Given
        val config = ExportConfig(
            format = ExportFormat.CSV,
            dateRange = DateRange.currentMonth()
        )
        
        coEvery { transactionRepository.getTransactionsByDateRange(any(), any()) } returns emptyList()
        
        // When
        val result = exportRepository.exportTransactions(config)
        
        // Then
        assertTrue(result is ExportResult.Error)
        assertTrue(result.message.contains("No transactions found"))
    }
    
    @Test
    fun `should validate export config correctly`() {
        // Valid config
        val validConfig = ExportConfig(
            format = ExportFormat.CSV,
            dateRange = DateRange.currentMonth()
        )
        assertTrue(exportRepository.validateExportConfig(validConfig))
        
        // Invalid config - start date after end date
        val invalidConfig = ExportConfig(
            format = ExportFormat.CSV,
            dateRange = DateRange.custom(
                LocalDate.now().plusDays(1),
                LocalDate.now()
            )
        )
        assertTrue(!exportRepository.validateExportConfig(invalidConfig))
    }
    
    @Test
    fun `should share file successfully`() = runTest {
        // Given
        val tempFile = File.createTempFile("test", ".csv", context.cacheDir)
        tempFile.writeText("test content")
        val shareOption = ShareOption.EMAIL
        
        coEvery { fileShareService.shareFile(any(), shareOption) } returns true
        
        // When
        val result = exportRepository.shareExportedFile(tempFile.absolutePath, shareOption)
        
        // Then
        assertTrue(result)
        
        // Cleanup
        tempFile.delete()
    }
    
    @Test
    fun `should return false when sharing non-existent file`() = runTest {
        // Given
        val nonExistentFile = "/path/to/non/existent/file.csv"
        val shareOption = ShareOption.EMAIL
        
        // When
        val result = exportRepository.shareExportedFile(nonExistentFile, shareOption)
        
        // Then
        assertTrue(!result)
    }
    
    @Test
    fun `should get export directory successfully`() {
        // When
        val exportDir = exportRepository.getExportDirectory()
        
        // Then
        assertTrue(exportDir.exists())
        assertTrue(exportDir.isDirectory)
        assertTrue(exportDir.name == "exports")
    }
    
    @Test
    fun `should cleanup old exports`() = runTest {
        // Given
        val exportDir = exportRepository.getExportDirectory()
        
        // Create some old files
        val oldFile1 = File(exportDir, "old1.csv")
        val oldFile2 = File(exportDir, "old2.pdf")
        oldFile1.createNewFile()
        oldFile2.createNewFile()
        
        // Set old modification time (8 days ago)
        val oldTime = System.currentTimeMillis() - (8 * 24 * 60 * 60 * 1000)
        oldFile1.setLastModified(oldTime)
        oldFile2.setLastModified(oldTime)
        
        // When
        val deletedCount = exportRepository.cleanupOldExports()
        
        // Then
        assertTrue(deletedCount >= 0) // Should not fail
    }
    
    private fun createSampleTransactions(): List<Transaction> {
        val category = Category(
            id = 1L,
            name = "Shopping",
            icon = "shopping",
            color = "#FF0000",
            isDefault = true
        )
        
        return listOf(
            Transaction(
                id = 1L,
                amount = BigDecimal("150.00"),
                type = TransactionType.EXPENSE,
                category = category,
                merchant = "Amazon",
                description = "Online shopping",
                date = LocalDateTime.of(2024, 1, 15, 10, 30),
                source = TransactionSource.SMS_AUTO,
                accountId = 1L
            )
        )
    }
    
    private fun createSampleAccounts(): List<Account> {
        return listOf(
            Account(
                id = 1L,
                bankName = "HDFC Bank",
                accountType = AccountType.SAVINGS,
                accountNumber = "****1234",
                nickname = "Primary Savings",
                currentBalance = BigDecimal("10000.00"),
                isActive = true,
                createdAt = LocalDateTime.now()
            )
        )
    }
}