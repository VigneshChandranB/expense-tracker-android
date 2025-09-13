package com.expensetracker.data.security

import com.expensetracker.data.local.database.ExpenseDatabase
import com.expensetracker.data.local.dao.TransactionDao
import com.expensetracker.data.local.dao.AccountDao
import com.expensetracker.data.local.entities.TransactionEntity
import com.expensetracker.data.local.entities.AccountEntity
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import com.google.common.truth.Truth.assertThat
import java.math.BigDecimal
import java.time.LocalDateTime

/**
 * Unit tests for DataIntegrityValidator
 */
class DataIntegrityValidatorTest {
    
    private lateinit var dataIntegrityValidator: DataIntegrityValidator
    private val mockDatabase = mockk<ExpenseDatabase>()
    private val mockTransactionDao = mockk<TransactionDao>()
    private val mockAccountDao = mockk<AccountDao>()
    
    @Before
    fun setup() {
        coEvery { mockDatabase.transactionDao() } returns mockTransactionDao
        coEvery { mockDatabase.accountDao() } returns mockAccountDao
        
        dataIntegrityValidator = DataIntegrityValidator(mockDatabase)
    }
    
    @Test
    fun `validateAllData returns good integrity for valid data`() = runTest {
        // Given
        val validTransactions = listOf(
            createValidTransaction(1L, BigDecimal("100.00")),
            createValidTransaction(2L, BigDecimal("50.00"))
        )
        val validAccounts = listOf(
            createValidAccount(1L, BigDecimal("150.00")),
            createValidAccount(2L, BigDecimal("200.00"))
        )
        
        coEvery { mockTransactionDao.getAllTransactions() } returns validTransactions
        coEvery { mockAccountDao.getAllAccounts() } returns validAccounts
        coEvery { mockTransactionDao.getOrphanedTransactions() } returns emptyList()
        coEvery { mockTransactionDao.getTransactionsWithInvalidCategories() } returns emptyList()
        coEvery { mockTransactionDao.getUnlinkedTransferTransactions() } returns emptyList()
        coEvery { mockTransactionDao.getTransactionsByAccount(1L) } returns listOf(validTransactions[0])
        coEvery { mockTransactionDao.getTransactionsByAccount(2L) } returns listOf(validTransactions[1])
        
        // When
        val report = dataIntegrityValidator.validateAllData()
        
        // Then
        assertThat(report.overallIntegrity).isEqualTo(IntegrityLevel.GOOD)
        assertThat(report.transactionIntegrity).isEqualTo(IntegrityLevel.GOOD)
        assertThat(report.accountIntegrity).isEqualTo(IntegrityLevel.GOOD)
        assertThat(report.errors).isEmpty()
    }
    
    @Test
    fun `validateAllData detects invalid transactions`() = runTest {
        // Given
        val invalidTransactions = listOf(
            createInvalidTransaction(1L, BigDecimal("-100.00")), // Negative amount
            createValidTransaction(2L, BigDecimal("50.00"))
        )
        val validAccounts = listOf(createValidAccount(1L, BigDecimal("0.00")))
        
        coEvery { mockTransactionDao.getAllTransactions() } returns invalidTransactions
        coEvery { mockAccountDao.getAllAccounts() } returns validAccounts
        coEvery { mockTransactionDao.getOrphanedTransactions() } returns emptyList()
        coEvery { mockTransactionDao.getTransactionsWithInvalidCategories() } returns emptyList()
        coEvery { mockTransactionDao.getUnlinkedTransferTransactions() } returns emptyList()
        coEvery { mockTransactionDao.getTransactionsByAccount(1L) } returns invalidTransactions
        
        // When
        val report = dataIntegrityValidator.validateAllData()
        
        // Then
        assertThat(report.transactionIntegrity).isEqualTo(IntegrityLevel.WARNING)
    }
    
    @Test
    fun `validateAllData detects balance inconsistencies`() = runTest {
        // Given
        val transactions = listOf(
            createValidTransaction(1L, BigDecimal("100.00"))
        )
        val accountsWithWrongBalance = listOf(
            createValidAccount(1L, BigDecimal("200.00")) // Wrong balance
        )
        
        coEvery { mockTransactionDao.getAllTransactions() } returns transactions
        coEvery { mockAccountDao.getAllAccounts() } returns accountsWithWrongBalance
        coEvery { mockTransactionDao.getOrphanedTransactions() } returns emptyList()
        coEvery { mockTransactionDao.getTransactionsWithInvalidCategories() } returns emptyList()
        coEvery { mockTransactionDao.getUnlinkedTransferTransactions() } returns emptyList()
        coEvery { mockTransactionDao.getTransactionsByAccount(1L) } returns transactions
        
        // When
        val report = dataIntegrityValidator.validateAllData()
        
        // Then
        assertThat(report.balanceConsistency).isEqualTo(IntegrityLevel.WARNING)
    }
    
    @Test
    fun `validateAllData detects orphaned transactions`() = runTest {
        // Given
        val orphanedTransactions = listOf(
            createValidTransaction(999L, BigDecimal("100.00")) // Non-existent account
        )
        
        coEvery { mockTransactionDao.getAllTransactions() } returns emptyList()
        coEvery { mockAccountDao.getAllAccounts() } returns emptyList()
        coEvery { mockTransactionDao.getOrphanedTransactions() } returns orphanedTransactions
        coEvery { mockTransactionDao.getTransactionsWithInvalidCategories() } returns emptyList()
        coEvery { mockTransactionDao.getUnlinkedTransferTransactions() } returns emptyList()
        
        // When
        val report = dataIntegrityValidator.validateAllData()
        
        // Then
        assertThat(report.relationshipIntegrity).isEqualTo(IntegrityLevel.WARNING)
    }
    
    @Test
    fun `generateDataHash produces consistent hash for same data`() = runTest {
        // Given
        val transactions = listOf(createValidTransaction(1L, BigDecimal("100.00")))
        val accounts = listOf(createValidAccount(1L, BigDecimal("100.00")))
        
        coEvery { mockTransactionDao.getAllTransactions() } returns transactions
        coEvery { mockAccountDao.getAllAccounts() } returns accounts
        
        // When
        val hash1 = dataIntegrityValidator.generateDataHash()
        val hash2 = dataIntegrityValidator.generateDataHash()
        
        // Then
        assertThat(hash1).isEqualTo(hash2)
        assertThat(hash1).isNotEmpty()
    }
    
    @Test
    fun `generateDataHash produces different hash for different data`() = runTest {
        // Given
        val transactions1 = listOf(createValidTransaction(1L, BigDecimal("100.00")))
        val transactions2 = listOf(createValidTransaction(1L, BigDecimal("200.00")))
        val accounts = listOf(createValidAccount(1L, BigDecimal("100.00")))
        
        coEvery { mockAccountDao.getAllAccounts() } returns accounts
        
        // When
        coEvery { mockTransactionDao.getAllTransactions() } returns transactions1
        val hash1 = dataIntegrityValidator.generateDataHash()
        
        coEvery { mockTransactionDao.getAllTransactions() } returns transactions2
        val hash2 = dataIntegrityValidator.generateDataHash()
        
        // Then
        assertThat(hash1).isNotEqualTo(hash2)
    }
    
    @Test
    fun `verifyDataIntegrity returns true for unchanged data`() = runTest {
        // Given
        val transactions = listOf(createValidTransaction(1L, BigDecimal("100.00")))
        val accounts = listOf(createValidAccount(1L, BigDecimal("100.00")))
        
        coEvery { mockTransactionDao.getAllTransactions() } returns transactions
        coEvery { mockAccountDao.getAllAccounts() } returns accounts
        
        val originalHash = dataIntegrityValidator.generateDataHash()
        
        // When
        val isValid = dataIntegrityValidator.verifyDataIntegrity(originalHash)
        
        // Then
        assertThat(isValid).isTrue()
    }
    
    @Test
    fun `verifyDataIntegrity returns false for changed data`() = runTest {
        // Given
        val transactions1 = listOf(createValidTransaction(1L, BigDecimal("100.00")))
        val transactions2 = listOf(createValidTransaction(1L, BigDecimal("200.00")))
        val accounts = listOf(createValidAccount(1L, BigDecimal("100.00")))
        
        coEvery { mockAccountDao.getAllAccounts() } returns accounts
        
        coEvery { mockTransactionDao.getAllTransactions() } returns transactions1
        val originalHash = dataIntegrityValidator.generateDataHash()
        
        // When - data changes
        coEvery { mockTransactionDao.getAllTransactions() } returns transactions2
        val isValid = dataIntegrityValidator.verifyDataIntegrity(originalHash)
        
        // Then
        assertThat(isValid).isFalse()
    }
    
    @Test
    fun `repairIntegrityIssues fixes balance inconsistencies`() = runTest {
        // Given
        val accounts = listOf(createValidAccount(1L, BigDecimal("200.00"))) // Wrong balance
        val transactions = listOf(createValidTransaction(1L, BigDecimal("100.00"))) // Correct balance should be 100
        
        coEvery { mockAccountDao.getAllAccounts() } returns accounts
        coEvery { mockTransactionDao.getTransactionsByAccount(1L) } returns transactions
        coEvery { mockAccountDao.updateAccountBalance(1L, BigDecimal("100.00")) } returns Unit
        coEvery { mockTransactionDao.deleteOrphanedTransactions() } returns 0
        
        // When
        val result = dataIntegrityValidator.repairIntegrityIssues()
        
        // Then
        assertThat(result.success).isTrue()
        assertThat(result.repairedBalances).isEqualTo(1)
        coVerify { mockAccountDao.updateAccountBalance(1L, BigDecimal("100.00")) }
    }
    
    @Test
    fun `repairIntegrityIssues removes orphaned transactions`() = runTest {
        // Given
        coEvery { mockAccountDao.getAllAccounts() } returns emptyList()
        coEvery { mockTransactionDao.deleteOrphanedTransactions() } returns 5
        
        // When
        val result = dataIntegrityValidator.repairIntegrityIssues()
        
        // Then
        assertThat(result.success).isTrue()
        assertThat(result.removedOrphanedRecords).isEqualTo(5)
        coVerify { mockTransactionDao.deleteOrphanedTransactions() }
    }
    
    private fun createValidTransaction(accountId: Long, amount: BigDecimal): TransactionEntity {
        return TransactionEntity(
            id = 1L,
            amount = amount.toString(),
            type = "EXPENSE",
            categoryId = 1L,
            accountId = accountId,
            merchant = "Test Merchant",
            description = "Test transaction",
            date = LocalDateTime.now().minusDays(1).toEpochSecond(java.time.ZoneOffset.UTC) * 1000,
            source = "MANUAL",
            transferAccountId = null,
            transferTransactionId = null,
            isRecurring = false,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
    }
    
    private fun createInvalidTransaction(accountId: Long, amount: BigDecimal): TransactionEntity {
        return TransactionEntity(
            id = 1L,
            amount = amount.toString(),
            type = "EXPENSE",
            categoryId = 0L, // Invalid category
            accountId = accountId,
            merchant = "", // Empty merchant
            description = null,
            date = LocalDateTime.now().plusDays(1).toEpochSecond(java.time.ZoneOffset.UTC) * 1000, // Future date
            source = "MANUAL",
            transferAccountId = null,
            transferTransactionId = null,
            isRecurring = false,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
    }
    
    private fun createValidAccount(id: Long, balance: BigDecimal): AccountEntity {
        return AccountEntity(
            id = id,
            bankName = "Test Bank",
            accountType = "SAVINGS",
            accountNumber = "1234567890",
            nickname = "Test Account",
            currentBalance = balance.toString(),
            isActive = true,
            createdAt = System.currentTimeMillis()
        )
    }
}