package com.expensetracker.data.export

import com.expensetracker.domain.model.Account
import com.expensetracker.domain.model.AccountType
import com.expensetracker.domain.model.Category
import com.expensetracker.domain.model.DateRange
import com.expensetracker.domain.model.Transaction
import com.expensetracker.domain.model.TransactionSource
import com.expensetracker.domain.model.TransactionType
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import kotlin.test.assertTrue

/**
 * Unit tests for PdfExporter
 */
class PdfExporterTest {
    
    @get:Rule
    val tempFolder = TemporaryFolder()
    
    private lateinit var pdfExporter: PdfExporter
    
    @Before
    fun setup() {
        pdfExporter = PdfExporter()
    }
    
    @Test
    fun `should export transactions to PDF successfully`() = runTest {
        // Given
        val outputFile = tempFolder.newFile("test_export.pdf")
        val transactions = createSampleTransactions()
        val accounts = createSampleAccounts()
        val dateRange = DateRange.currentMonth()
        
        // When
        val result = pdfExporter.exportTransactions(
            transactions, 
            accounts, 
            dateRange, 
            includeCharts = false, 
            outputFile
        )
        
        // Then
        assertTrue(result)
        assertTrue(outputFile.exists())
        assertTrue(outputFile.length() > 0)
    }
    
    @Test
    fun `should export PDF with charts successfully`() = runTest {
        // Given
        val outputFile = tempFolder.newFile("test_export_with_charts.pdf")
        val transactions = createSampleTransactions()
        val accounts = createSampleAccounts()
        val dateRange = DateRange.currentMonth()
        
        // When
        val result = pdfExporter.exportTransactions(
            transactions, 
            accounts, 
            dateRange, 
            includeCharts = true, 
            outputFile
        )
        
        // Then
        assertTrue(result)
        assertTrue(outputFile.exists())
        assertTrue(outputFile.length() > 0)
    }
    
    @Test
    fun `should handle empty transactions list`() = runTest {
        // Given
        val outputFile = tempFolder.newFile("empty_export.pdf")
        val transactions = emptyList<Transaction>()
        val accounts = emptyMap<Long, Account>()
        val dateRange = DateRange.currentMonth()
        
        // When
        val result = pdfExporter.exportTransactions(
            transactions, 
            accounts, 
            dateRange, 
            includeCharts = false, 
            outputFile
        )
        
        // Then
        assertTrue(result)
        assertTrue(outputFile.exists())
        assertTrue(outputFile.length() > 0) // Should still create PDF with headers
    }
    
    @Test
    fun `should handle file write errors gracefully`() = runTest {
        // Given
        val invalidFile = File("/invalid/path/test.pdf") // Invalid path
        val transactions = createSampleTransactions()
        val accounts = createSampleAccounts()
        val dateRange = DateRange.currentMonth()
        
        // When
        val result = pdfExporter.exportTransactions(
            transactions, 
            accounts, 
            dateRange, 
            includeCharts = false, 
            invalidFile
        )
        
        // Then
        assertTrue(!result) // Should return false on error
    }
    
    private fun createSampleTransactions(): List<Transaction> {
        val shoppingCategory = Category(
            id = 1L,
            name = "Shopping",
            icon = "shopping",
            color = "#FF0000",
            isDefault = true
        )
        
        val foodCategory = Category(
            id = 2L,
            name = "Food",
            icon = "restaurant",
            color = "#00FF00",
            isDefault = true
        )
        
        return listOf(
            Transaction(
                id = 1L,
                amount = BigDecimal("150.00"),
                type = TransactionType.EXPENSE,
                category = shoppingCategory,
                merchant = "Amazon",
                description = "Online shopping",
                date = LocalDateTime.of(2024, 1, 15, 10, 30),
                source = TransactionSource.SMS_AUTO,
                accountId = 1L
            ),
            Transaction(
                id = 2L,
                amount = BigDecimal("50.00"),
                type = TransactionType.EXPENSE,
                category = foodCategory,
                merchant = "McDonald's",
                description = "Lunch",
                date = LocalDateTime.of(2024, 1, 16, 12, 0),
                source = TransactionSource.MANUAL,
                accountId = 1L
            ),
            Transaction(
                id = 3L,
                amount = BigDecimal("2000.00"),
                type = TransactionType.INCOME,
                category = shoppingCategory,
                merchant = "Salary",
                description = "Monthly salary",
                date = LocalDateTime.of(2024, 1, 1, 9, 0),
                source = TransactionSource.SMS_AUTO,
                accountId = 1L
            )
        )
    }
    
    private fun createSampleAccounts(): Map<Long, Account> {
        return mapOf(
            1L to Account(
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