package com.expensetracker.data.repository

import com.expensetracker.data.local.dao.CategoryDao
import com.expensetracker.data.local.dao.TransactionDao
import com.expensetracker.data.local.entities.CategoryEntity
import com.expensetracker.data.local.entities.TransactionEntity
import com.expensetracker.domain.model.Category
import com.expensetracker.domain.model.Transaction
import com.expensetracker.domain.model.TransactionSource
import com.expensetracker.domain.model.TransactionType
import com.expensetracker.domain.repository.TransactionRepository
import io.mockk.*
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneOffset

/**
 * Unit tests for TransactionRepositoryImpl
 * Tests repository logic with mocked dependencies
 */
class TransactionRepositoryTest {
    
    private lateinit var transactionDao: TransactionDao
    private lateinit var categoryDao: CategoryDao
    private lateinit var repository: TransactionRepository
    
    private val testCategoryEntity = CategoryEntity(
        id = 1,
        name = "Test Category",
        icon = "test_icon",
        color = "#FF0000",
        isDefault = false,
        parentCategoryId = null
    )
    
    private val testCategory = Category(
        id = 1,
        name = "Test Category",
        icon = "test_icon",
        color = "#FF0000",
        isDefault = false
    )
    
    @Before
    fun setup() {
        transactionDao = mockk()
        categoryDao = mockk()
        repository = TransactionRepositoryImpl(transactionDao, categoryDao)
        
        // Default mock behavior for category lookup
        coEvery { categoryDao.getCategoryById(1) } returns testCategoryEntity
    }
    
    @Test
    fun `insertTransaction should validate amount and return ID`() = runTest {
        // Arrange
        val transaction = createTestTransaction()
        val expectedId = 123L
        coEvery { transactionDao.insertTransaction(any()) } returns expectedId
        
        // Act
        val result = repository.insertTransaction(transaction)
        
        // Assert
        assertEquals("Should return inserted ID", expectedId, result)
        coVerify { transactionDao.insertTransaction(any()) }
    }
    
    @Test
    fun `insertTransaction should throw exception for negative amount`() = runTest {
        // Arrange
        val invalidTransaction = createTestTransaction(amount = "-100.00")
        
        // Act & Assert
        try {
            repository.insertTransaction(invalidTransaction)
            fail("Should throw exception for negative amount")
        } catch (e: IllegalArgumentException) {
            assertTrue("Should contain validation message", 
                e.message?.contains("amount must be positive") == true)
        }
        
        coVerify(exactly = 0) { transactionDao.insertTransaction(any()) }
    }
    
    @Test
    fun `insertTransaction should throw exception for zero amount`() = runTest {
        // Arrange
        val invalidTransaction = createTestTransaction(amount = "0.00")
        
        // Act & Assert
        try {
            repository.insertTransaction(invalidTransaction)
            fail("Should throw exception for zero amount")
        } catch (e: IllegalArgumentException) {
            assertTrue("Should contain validation message", 
                e.message?.contains("amount must be positive") == true)
        }
    }
    
    @Test
    fun `insertTransaction should validate category exists`() = runTest {
        // Arrange
        val transaction = createTestTransaction()
        coEvery { categoryDao.getCategoryById(1) } returns null
        
        // Act & Assert
        try {
            repository.insertTransaction(transaction)
            fail("Should throw exception for non-existent category")
        } catch (e: IllegalArgumentException) {
            assertTrue("Should contain category validation message",
                e.message?.contains("Category with ID 1 does not exist") == true)
        }
    }
    
    @Test
    fun `updateTransaction should validate and update entity`() = runTest {
        // Arrange
        val transaction = createTestTransaction(id = 1)
        coEvery { transactionDao.updateTransaction(any()) } just Runs
        
        // Act
        repository.updateTransaction(transaction)
        
        // Assert
        coVerify { transactionDao.updateTransaction(any()) }
    }
    
    @Test
    fun `deleteTransaction should remove from database and cache`() = runTest {
        // Arrange
        val transactionId = 1L
        coEvery { transactionDao.deleteTransactionById(transactionId) } just Runs
        
        // Act
        repository.deleteTransaction(transactionId)
        
        // Assert
        coVerify { transactionDao.deleteTransactionById(transactionId) }
    }
    
    @Test
    fun `getTransactionById should return cached transaction if available`() = runTest {
        // Arrange
        val transactionId = 1L
        val transaction = createTestTransaction(id = transactionId)
        
        // First, insert to populate cache
        coEvery { transactionDao.insertTransaction(any()) } returns transactionId
        repository.insertTransaction(transaction)
        
        // Clear DAO interactions for the test
        clearMocks(transactionDao)
        
        // Act
        val result = repository.getTransactionById(transactionId)
        
        // Assert
        assertNotNull("Should return cached transaction", result)
        assertEquals("Should return correct transaction", transactionId, result!!.id)
        
        // Verify DAO was not called (cache hit)
        coVerify(exactly = 0) { transactionDao.getTransactionById(any()) }
    }
    
    @Test
    fun `getTransactionById should query database if not cached`() = runTest {
        // Arrange
        val transactionId = 1L
        val transactionEntity = createTestTransactionEntity(id = transactionId)
        coEvery { transactionDao.getTransactionById(transactionId) } returns transactionEntity
        
        // Act
        val result = repository.getTransactionById(transactionId)
        
        // Assert
        assertNotNull("Should return transaction from database", result)
        assertEquals("Should return correct transaction", transactionId, result!!.id)
        coVerify { transactionDao.getTransactionById(transactionId) }
    }
    
    @Test
    fun `getTransactionsByDateRange should convert dates and query database`() = runTest {
        // Arrange
        val startDate = LocalDate.of(2023, 1, 1)
        val endDate = LocalDate.of(2023, 1, 31)
        val transactionEntities = listOf(createTestTransactionEntity())
        
        coEvery { 
            transactionDao.getTransactionsByDateRange(any(), any()) 
        } returns transactionEntities
        
        // Act
        val result = repository.getTransactionsByDateRange(startDate, endDate)
        
        // Assert
        assertEquals("Should return converted transactions", 1, result.size)
        coVerify { transactionDao.getTransactionsByDateRange(any(), any()) }
    }
    
    @Test
    fun `createTransfer should validate accounts and create linked transactions`() = runTest {
        // Arrange
        val fromAccountId = 1L
        val toAccountId = 2L
        val amount = "100.00"
        val description = "Test transfer"
        val date = LocalDateTime.now()
        
        coEvery { transactionDao.insertTransaction(any()) } returnsMany listOf(10L, 11L)
        coEvery { transactionDao.linkTransferTransactions(any(), any()) } just Runs
        
        // Act
        val result = repository.createTransfer(fromAccountId, toAccountId, amount, description, date)
        
        // Assert
        assertEquals("Should return outgoing transaction ID", 10L, result.first)
        assertEquals("Should return incoming transaction ID", 11L, result.second)
        
        coVerify(exactly = 2) { transactionDao.insertTransaction(any()) }
        coVerify(exactly = 2) { transactionDao.linkTransferTransactions(any(), any()) }
    }
    
    @Test
    fun `createTransfer should throw exception for same account`() = runTest {
        // Arrange
        val accountId = 1L
        val amount = "100.00"
        val date = LocalDateTime.now()
        
        // Act & Assert
        try {
            repository.createTransfer(accountId, accountId, amount, null, date)
            fail("Should throw exception for same account transfer")
        } catch (e: IllegalArgumentException) {
            assertTrue("Should contain validation message",
                e.message?.contains("Cannot transfer to the same account") == true)
        }
    }
    
    @Test
    fun `createTransfer should throw exception for negative amount`() = runTest {
        // Arrange
        val fromAccountId = 1L
        val toAccountId = 2L
        val amount = "-100.00"
        val date = LocalDateTime.now()
        
        // Act & Assert
        try {
            repository.createTransfer(fromAccountId, toAccountId, amount, null, date)
            fail("Should throw exception for negative amount")
        } catch (e: IllegalArgumentException) {
            assertTrue("Should contain validation message",
                e.message?.contains("Transfer amount must be positive") == true)
        }
    }
    
    @Test
    fun `createTransfer should throw exception for invalid amount format`() = runTest {
        // Arrange
        val fromAccountId = 1L
        val toAccountId = 2L
        val amount = "invalid"
        val date = LocalDateTime.now()
        
        // Act & Assert
        try {
            repository.createTransfer(fromAccountId, toAccountId, amount, null, date)
            fail("Should throw exception for invalid amount format")
        } catch (e: IllegalArgumentException) {
            assertTrue("Should contain validation message",
                e.message?.contains("Invalid amount format") == true)
        }
    }
    
    @Test
    fun `observeAllTransactions should return flow of domain models`() = runTest {
        // Arrange
        val transactionEntities = listOf(createTestTransactionEntity())
        every { transactionDao.observeAllTransactions() } returns flowOf(transactionEntities)
        
        // Act
        val result = repository.observeAllTransactions()
        
        // Assert
        result.collect { transactions ->
            assertEquals("Should return converted transactions", 1, transactions.size)
            assertEquals("Should have correct type", TransactionType.EXPENSE, transactions[0].type)
        }
    }
    
    @Test
    fun `insertTransactions should validate all transactions before inserting`() = runTest {
        // Arrange
        val validTransaction = createTestTransaction()
        val invalidTransaction = createTestTransaction(amount = "-50.00")
        val transactions = listOf(validTransaction, invalidTransaction)
        
        // Act & Assert
        try {
            repository.insertTransactions(transactions)
            fail("Should throw exception when any transaction is invalid")
        } catch (e: IllegalArgumentException) {
            assertTrue("Should contain validation message",
                e.message?.contains("amount must be positive") == true)
        }
        
        // Verify no transactions were inserted
        coVerify(exactly = 0) { transactionDao.insertTransactions(any()) }
    }
    
    @Test
    fun `getTotalIncomeByAccount should return correct value`() = runTest {
        // Arrange
        val accountId = 1L
        val expectedIncome = 1500.0
        coEvery { transactionDao.getTotalIncomeByAccount(accountId) } returns expectedIncome
        
        // Act
        val result = repository.getTotalIncomeByAccount(accountId)
        
        // Assert
        assertEquals("Should return correct income", expectedIncome, result, 0.01)
        coVerify { transactionDao.getTotalIncomeByAccount(accountId) }
    }
    
    @Test
    fun `getTotalExpensesByAccount should return correct value`() = runTest {
        // Arrange
        val accountId = 1L
        val expectedExpenses = 800.0
        coEvery { transactionDao.getTotalExpensesByAccount(accountId) } returns expectedExpenses
        
        // Act
        val result = repository.getTotalExpensesByAccount(accountId)
        
        // Assert
        assertEquals("Should return correct expenses", expectedExpenses, result, 0.01)
        coVerify { transactionDao.getTotalExpensesByAccount(accountId) }
    }
    
    private fun createTestTransaction(
        id: Long = 0,
        amount: String = "100.00",
        merchant: String = "Test Merchant"
    ): Transaction {
        return Transaction(
            id = id,
            amount = BigDecimal(amount),
            type = TransactionType.EXPENSE,
            category = testCategory,
            merchant = merchant,
            description = "Test transaction",
            date = LocalDateTime.now(),
            source = TransactionSource.MANUAL,
            accountId = 1,
            transferAccountId = null,
            transferTransactionId = null,
            isRecurring = false
        )
    }
    
    private fun createTestTransactionEntity(
        id: Long = 1,
        amount: String = "100.00"
    ): TransactionEntity {
        return TransactionEntity(
            id = id,
            amount = amount,
            type = TransactionType.EXPENSE.name,
            categoryId = 1,
            accountId = 1,
            merchant = "Test Merchant",
            description = "Test transaction",
            date = LocalDateTime.now().toEpochSecond(ZoneOffset.UTC),
            source = TransactionSource.MANUAL.name,
            transferAccountId = null,
            transferTransactionId = null,
            isRecurring = false,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
    }
}