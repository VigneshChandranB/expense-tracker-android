package com.expensetracker.data.repository

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.expensetracker.data.local.database.ExpenseDatabase
import com.expensetracker.data.local.entities.AccountEntity
import com.expensetracker.data.local.entities.CategoryEntity
import com.expensetracker.domain.model.Category
import com.expensetracker.domain.model.Transaction
import com.expensetracker.domain.model.TransactionSource
import com.expensetracker.domain.model.TransactionType
import com.expensetracker.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * Integration tests for TransactionRepository
 * Tests the complete data flow from repository to database
 */
@RunWith(AndroidJUnit4::class)
class TransactionRepositoryIntegrationTest {
    
    private lateinit var database: ExpenseDatabase
    private lateinit var repository: TransactionRepository
    
    private val testCategory = Category(
        id = 1,
        name = "Test Category",
        icon = "test_icon",
        color = "#FF0000",
        isDefault = false
    )
    
    private val testAccount = AccountEntity(
        id = 1,
        bankName = "Test Bank",
        accountType = "CHECKING",
        accountNumber = "1234567890",
        nickname = "Test Account",
        currentBalance = "1000.00",
        isActive = true,
        createdAt = System.currentTimeMillis()
    )
    
    @Before
    fun setup() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            ExpenseDatabase::class.java
        ).allowMainThreadQueries().build()
        
        repository = TransactionRepositoryImpl(
            database.transactionDao(),
            database.categoryDao()
        )
    }
    
    @After
    fun tearDown() {
        database.close()
    }
    
    @Test
    fun insertAndRetrieveTransaction() = runTest {
        // Setup test data
        setupTestData()
        
        val transaction = createTestTransaction()
        
        // Insert transaction
        val insertedId = repository.insertTransaction(transaction)
        assertTrue("Transaction ID should be positive", insertedId > 0)
        
        // Retrieve transaction
        val retrievedTransaction = repository.getTransactionById(insertedId)
        assertNotNull("Retrieved transaction should not be null", retrievedTransaction)
        assertEquals("Transaction amounts should match", transaction.amount, retrievedTransaction!!.amount)
        assertEquals("Transaction types should match", transaction.type, retrievedTransaction.type)
        assertEquals("Transaction merchants should match", transaction.merchant, retrievedTransaction.merchant)
    }
    
    @Test
    fun updateTransaction() = runTest {
        setupTestData()
        
        val transaction = createTestTransaction()
        val insertedId = repository.insertTransaction(transaction)
        
        // Update transaction
        val updatedTransaction = transaction.copy(
            id = insertedId,
            amount = BigDecimal("200.00"),
            merchant = "Updated Merchant"
        )
        repository.updateTransaction(updatedTransaction)
        
        // Verify update
        val retrievedTransaction = repository.getTransactionById(insertedId)
        assertNotNull("Updated transaction should exist", retrievedTransaction)
        assertEquals("Amount should be updated", BigDecimal("200.00"), retrievedTransaction!!.amount)
        assertEquals("Merchant should be updated", "Updated Merchant", retrievedTransaction.merchant)
    }
    
    @Test
    fun deleteTransaction() = runTest {
        setupTestData()
        
        val transaction = createTestTransaction()
        val insertedId = repository.insertTransaction(transaction)
        
        // Verify transaction exists
        assertNotNull("Transaction should exist before deletion", repository.getTransactionById(insertedId))
        
        // Delete transaction
        repository.deleteTransaction(insertedId)
        
        // Verify deletion
        assertNull("Transaction should not exist after deletion", repository.getTransactionById(insertedId))
    }
    
    @Test
    fun observeTransactions() = runTest {
        setupTestData()
        
        // Insert multiple transactions
        val transactions = listOf(
            createTestTransaction(amount = "100.00", merchant = "Merchant 1"),
            createTestTransaction(amount = "200.00", merchant = "Merchant 2"),
            createTestTransaction(amount = "300.00", merchant = "Merchant 3")
        )
        
        repository.insertTransactions(transactions)
        
        // Observe transactions
        val observedTransactions = repository.observeAllTransactions().first()
        assertEquals("Should have 3 transactions", 3, observedTransactions.size)
        
        // Verify transactions are sorted by date (newest first)
        assertTrue("Transactions should be sorted by date", 
            observedTransactions[0].date.isAfter(observedTransactions[1].date) ||
            observedTransactions[0].date.isEqual(observedTransactions[1].date))
    }
    
    @Test
    fun getTransactionsByDateRange() = runTest {
        setupTestData()
        
        val today = LocalDateTime.now()
        val yesterday = today.minusDays(1)
        val tomorrow = today.plusDays(1)
        
        // Insert transactions with different dates
        val transactions = listOf(
            createTestTransaction(date = yesterday, merchant = "Yesterday"),
            createTestTransaction(date = today, merchant = "Today"),
            createTestTransaction(date = tomorrow, merchant = "Tomorrow")
        )
        
        repository.insertTransactions(transactions)
        
        // Get transactions for today only
        val todayTransactions = repository.getTransactionsByDateRange(
            today.toLocalDate(),
            today.toLocalDate()
        )
        
        assertEquals("Should have 1 transaction for today", 1, todayTransactions.size)
        assertEquals("Should be today's transaction", "Today", todayTransactions[0].merchant)
    }
    
    @Test
    fun getTransactionsByCategory() = runTest {
        setupTestData()
        
        // Create another category
        val category2 = CategoryEntity(
            id = 2,
            name = "Category 2",
            icon = "icon2",
            color = "#00FF00",
            isDefault = false,
            parentCategoryId = null
        )
        database.categoryDao().insertCategory(category2)
        
        val domainCategory2 = Category(
            id = 2,
            name = "Category 2",
            icon = "icon2",
            color = "#00FF00",
            isDefault = false
        )
        
        // Insert transactions with different categories
        val transactions = listOf(
            createTestTransaction(category = testCategory, merchant = "Cat1 Merchant1"),
            createTestTransaction(category = testCategory, merchant = "Cat1 Merchant2"),
            createTestTransaction(category = domainCategory2, merchant = "Cat2 Merchant1")
        )
        
        repository.insertTransactions(transactions)
        
        // Get transactions by category
        val category1Transactions = repository.getTransactionsByCategory(1)
        val category2Transactions = repository.getTransactionsByCategory(2)
        
        assertEquals("Category 1 should have 2 transactions", 2, category1Transactions.size)
        assertEquals("Category 2 should have 1 transaction", 1, category2Transactions.size)
    }
    
    @Test
    fun createTransfer() = runTest {
        setupTestData()
        
        // Create second account
        val account2 = AccountEntity(
            id = 2,
            bankName = "Test Bank 2",
            accountType = "SAVINGS",
            accountNumber = "0987654321",
            nickname = "Savings Account",
            currentBalance = "500.00",
            isActive = true,
            createdAt = System.currentTimeMillis()
        )
        database.accountDao().insertAccount(account2)
        
        // Create transfer
        val transferAmount = "150.00"
        val transferDate = LocalDateTime.now()
        val (outgoingId, incomingId) = repository.createTransfer(
            fromAccountId = 1,
            toAccountId = 2,
            amount = transferAmount,
            description = "Test Transfer",
            date = transferDate
        )
        
        // Verify both transactions were created
        val outgoingTransaction = repository.getTransactionById(outgoingId)
        val incomingTransaction = repository.getTransactionById(incomingId)
        
        assertNotNull("Outgoing transaction should exist", outgoingTransaction)
        assertNotNull("Incoming transaction should exist", incomingTransaction)
        
        assertEquals("Outgoing transaction type", TransactionType.TRANSFER_OUT, outgoingTransaction!!.type)
        assertEquals("Incoming transaction type", TransactionType.TRANSFER_IN, incomingTransaction!!.type)
        
        assertEquals("Outgoing amount", BigDecimal(transferAmount), outgoingTransaction.amount)
        assertEquals("Incoming amount", BigDecimal(transferAmount), incomingTransaction.amount)
        
        assertEquals("Outgoing account", 1L, outgoingTransaction.accountId)
        assertEquals("Incoming account", 2L, incomingTransaction.accountId)
        
        assertEquals("Outgoing transfer account", 2L, outgoingTransaction.transferAccountId)
        assertEquals("Incoming transfer account", 1L, incomingTransaction.transferAccountId)
    }
    
    @Test
    fun getTransactionsByAccount() = runTest {
        setupTestData()
        
        // Create second account
        val account2 = AccountEntity(
            id = 2,
            bankName = "Test Bank 2",
            accountType = "SAVINGS",
            accountNumber = "0987654321",
            nickname = "Savings Account",
            currentBalance = "500.00",
            isActive = true,
            createdAt = System.currentTimeMillis()
        )
        database.accountDao().insertAccount(account2)
        
        // Insert transactions for different accounts
        val transactions = listOf(
            createTestTransaction(accountId = 1, merchant = "Account1 Transaction1"),
            createTestTransaction(accountId = 1, merchant = "Account1 Transaction2"),
            createTestTransaction(accountId = 2, merchant = "Account2 Transaction1")
        )
        
        repository.insertTransactions(transactions)
        
        // Get transactions by account
        val account1Transactions = repository.observeTransactionsByAccount(1).first()
        val account2Transactions = repository.observeTransactionsByAccount(2).first()
        
        assertEquals("Account 1 should have 2 transactions", 2, account1Transactions.size)
        assertEquals("Account 2 should have 1 transaction", 1, account2Transactions.size)
    }
    
    @Test
    fun validateTransactionAmount() = runTest {
        setupTestData()
        
        val invalidTransaction = createTestTransaction(amount = "-100.00")
        
        try {
            repository.insertTransaction(invalidTransaction)
            fail("Should throw exception for negative amount")
        } catch (e: IllegalArgumentException) {
            assertTrue("Should contain amount validation message", 
                e.message?.contains("amount must be positive") == true)
        }
    }
    
    @Test
    fun validateTransferSameAccount() = runTest {
        setupTestData()
        
        try {
            repository.createTransfer(
                fromAccountId = 1,
                toAccountId = 1,
                amount = "100.00",
                description = "Invalid transfer",
                date = LocalDateTime.now()
            )
            fail("Should throw exception for same account transfer")
        } catch (e: IllegalArgumentException) {
            assertTrue("Should contain same account validation message",
                e.message?.contains("Cannot transfer to the same account") == true)
        }
    }
    
    @Test
    fun cacheRecentTransactions() = runTest {
        setupTestData()
        
        val transaction = createTestTransaction()
        val insertedId = repository.insertTransaction(transaction)
        
        // First retrieval (from database)
        val firstRetrieval = repository.getTransactionById(insertedId)
        assertNotNull("First retrieval should succeed", firstRetrieval)
        
        // Second retrieval (should be from cache)
        val secondRetrieval = repository.getTransactionById(insertedId)
        assertNotNull("Second retrieval should succeed", secondRetrieval)
        
        // Verify both retrievals return the same data
        assertEquals("Cached transaction should match", firstRetrieval, secondRetrieval)
    }
    
    private suspend fun setupTestData() {
        // Insert test category
        val categoryEntity = CategoryEntity(
            id = 1,
            name = testCategory.name,
            icon = testCategory.icon,
            color = testCategory.color,
            isDefault = testCategory.isDefault,
            parentCategoryId = null
        )
        database.categoryDao().insertCategory(categoryEntity)
        
        // Insert test account
        database.accountDao().insertAccount(testAccount)
    }
    
    private fun createTestTransaction(
        amount: String = "100.00",
        merchant: String = "Test Merchant",
        date: LocalDateTime = LocalDateTime.now(),
        category: Category = testCategory,
        accountId: Long = 1
    ): Transaction {
        return Transaction(
            id = 0,
            amount = BigDecimal(amount),
            type = TransactionType.EXPENSE,
            category = category,
            merchant = merchant,
            description = "Test transaction",
            date = date,
            source = TransactionSource.MANUAL,
            accountId = accountId,
            transferAccountId = null,
            transferTransactionId = null,
            isRecurring = false
        )
    }
}