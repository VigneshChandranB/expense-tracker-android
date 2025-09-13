package com.expensetracker.data.export

import com.expensetracker.domain.model.Account
import com.expensetracker.domain.model.AccountType
import com.expensetracker.domain.model.Category
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
import java.time.LocalDateTime
import kotlin.test.assertTrue

/**
 * Unit tests for CsvExporter
 */
class CsvExporterTest {
    
    @get:Rule
    val tempFolder = TemporaryFolder()
    
    private lateinit var csvExporter: CsvExporter
    
    @Before
    fun setup() {
        csvExporter = CsvExporter()
    }
    
    @Test
    fun `should export transactions to CSV successfully`() = runTest {
        // Given
        val outputFile = tempFolder.newFile("test_export.csv")
        val transactions = createSampleTransactions()
        val accounts = createSampleAccounts()
        
        // When
        val result = csvExporter.exportTransactions(transactions, accounts, outputFile)
        
        // Then
        assertTrue(result)
        assertTrue(outputFile.exists())
        assertTrue(outputFile.length() > 0)
        
        // Verify CSV content
        val csvContent = outputFile.readText()
        assertTrue(csvContent.contains("ID,Date,Amount,Type,Category,Merchant"))
        assertTrue(csvContent.contains("Amazon"))
        assertTrue(csvContent.contains("EXPENSE"))
        assertTrue(csvContent.contains("Shopping"))
    }
    
    @Test
    fun `should handle empty transactions list`() = runTest {
        // Given
        val outputFile = tempFolder.newFile("empty_export.csv")
        val transactions = emptyList<Transaction>()
        val accounts = emptyMap<Long, Account>()
        
        // When
        val result = csvExporter.exportTransactions(transactions, accounts, outputFile)
        
        // Then
        assertTrue(result)
        assertTrue(outputFile.exists())
        
        // Should still have header
        val csvContent = outputFile.readText()
        assertTrue(csvContent.contains("ID,Date,Amount,Type,Category,Merchant"))
    }
    
    @Test
    fun `should handle transactions with missing account information`() = runTest {
        // Given
        val outputFile = tempFolder.newFile("missing_account_export.csv")
        val transactions = createSampleTransactions()
        val accounts = emptyMap<Long, Account>() // No account info
        
        // When
        val result = csvExporter.exportTransactions(transactions, accounts, outputFile)
        
        // Then
        assertTrue(result)
        assertTrue(outputFile.exists())
        
        // Should handle missing account gracefully
        val csvContent = outputFile.readText()
        assertTrue(csvContent.contains("Amazon"))
        // Account fields should be empty
        assertTrue(csvContent.contains(",,"))
    }
    
    @Test
    fun `should handle file write errors gracefully`() = runTest {
        // Given
        val invalidFile = File("/invalid/path/test.csv") // Invalid path
        val transactions = createSampleTransactions()
        val accounts = createSampleAccounts()
        
        // When
        val result = csvExporter.exportTransactions(transactions, accounts, invalidFile)
        
        // Then
        assertTrue(!result) // Should return false on error
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
            ),
            Transaction(
                id = 2L,
                amount = BigDecimal("50.00"),
                type = TransactionType.INCOME,
                category = category,
                merchant = "Freelance",
                description = "Project payment",
                date = LocalDateTime.of(2024, 1, 16, 14, 0),
                source = TransactionSource.MANUAL,
                accountId = 2L
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
            ),
            2L to Account(
                id = 2L,
                bankName = "ICICI Bank",
                accountType = AccountType.CHECKING,
                accountNumber = "****5678",
                nickname = "Salary Account",
                currentBalance = BigDecimal("5000.00"),
                isActive = true,
                createdAt = LocalDateTime.now()
            )
        )
    }
}