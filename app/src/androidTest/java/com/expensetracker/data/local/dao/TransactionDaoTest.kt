package com.expensetracker.data.local.dao

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.expensetracker.data.local.database.ExpenseDatabase
import com.expensetracker.data.local.entities.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.Assert.*

/**
 * Integration tests for TransactionDao
 */
@RunWith(AndroidJUnit4::class)
class TransactionDaoTest {
    
    private lateinit var database: ExpenseDatabase
    private lateinit var transactionDao: TransactionDao
    private lateinit var accountDao: AccountDao
    private lateinit var categoryDao: CategoryDao
    
    @Before
    fun setup() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            ExpenseDatabase::class.java
        ).allowMainThreadQueries().build()
        
        transactionDao = database.transactionDao()
        accountDao = database.accountDao()
        categoryDao = database.categoryDao()
    }
    
    @After
    fun teardown() {
        database.close()
    }
    
    private suspend fun setupTestData(): Pair<Long, Long> {
        val account = AccountEntity(
            bankName = "Test Bank",
            accountType = "SAVINGS",
            accountNumber = "1234567890",
            nickname = "Test Account",
            currentBalance = "1000.00",
            isActive = true,
            createdAt = System.currentTimeMillis()
        )
        val accountId = accountDao.insertAccount(account)
        
        val category = CategoryEntity(
            name = "Test Category",
            icon = "test_icon",
            color = "#FF0000",
            isDefault = false,
            parentCategoryId = null
        )
        val categoryId = categoryDao.insertCategory(category)
        
        return Pair(accountId, categoryId)
    }
    
    @Test
    fun insertAndRetrieveTransaction() = runTest {
        val (accountId, categoryId) = setupTestData()
        
        val transaction = TransactionEntity(
            amount = "100.50",
            type = "EXPENSE",
            categoryId = categoryId,
            accountId = accountId,
            merchant = "Test Merchant",
            description = "Test transaction",
            date = System.currentTimeMillis(),
            source = "MANUAL",
            transferAccountId = null,
            transferTransactionId = null,
            isRecurring = false,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
        
        val transactionId = transactionDao.insertTransaction(transaction)
        val retrievedTransaction = transactionDao.getTransactionById(transactionId)
        
        assertNotNull(retrievedTransaction)
        assertEquals(transaction.amount, retrievedTransaction?.amount)
        assertEquals(transaction.merchant, retrievedTransaction?.merchant)
    }
    
    @Test
    fun getTransactionsByAccount() = runTest {
        val (accountId, categoryId) = setupTestData()
        
        // Insert multiple transactions for the account
        val transactions = listOf(
            TransactionEntity(
                amount = "100.00",
                type = "EXPENSE",
                categoryId = categoryId,
                accountId = accountId,
                merchant = "Merchant 1",
                description = null,
                date = System.currentTimeMillis(),
                source = "MANUAL",
                transferAccountId = null,
                transferTransactionId = null,
                isRecurring = false,
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            ),
            TransactionEntity(
                amount = "200.00",
                type = "INCOME",
                categoryId = categoryId,
                accountId = accountId,
                merchant = "Merchant 2",
                description = null,
                date = System.currentTimeMillis(),
                source = "SMS_AUTO",
                transferAccountId = null,
                transferTransactionId = null,
                isRecurring = false,
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )
        )
        
        transactionDao.insertTransactions(transactions)
        
        val accountTransactions = transactionDao.observeTransactionsByAccount(accountId).first()
        assertEquals(2, accountTransactions.size)
    }
    
    @Test
    fun getTransactionsByDateRange() = runTest {
        val (accountId, categoryId) = setupTestData()
        
        val now = System.currentTimeMillis()
        val oneDayAgo = now - (24 * 60 * 60 * 1000)
        val twoDaysAgo = now - (2 * 24 * 60 * 60 * 1000)
        
        val transactions = listOf(
            TransactionEntity(
                amount = "100.00",
                type = "EXPENSE",
                categoryId = categoryId,
                accountId = accountId,
                merchant = "Recent",
                description = null,
                date = now,
                source = "MANUAL",
                transferAccountId = null,
                transferTransactionId = null,
                isRecurring = false,
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            ),
            TransactionEntity(
                amount = "200.00",
                type = "EXPENSE",
                categoryId = categoryId,
                accountId = accountId,
                merchant = "Old",
                description = null,
                date = twoDaysAgo,
                source = "MANUAL",
                transferAccountId = null,
                transferTransactionId = null,
                isRecurring = false,
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )
        )
        
        transactionDao.insertTransactions(transactions)
        
        val recentTransactions = transactionDao.getTransactionsByDateRange(oneDayAgo, now)
        assertEquals(1, recentTransactions.size)
        assertEquals("Recent", recentTransactions[0].merchant)
    }
    
    @Test
    fun updateTransaction() = runTest {
        val (accountId, categoryId) = setupTestData()
        
        val transaction = TransactionEntity(
            amount = "100.00",
            type = "EXPENSE",
            categoryId = categoryId,
            accountId = accountId,
            merchant = "Original Merchant",
            description = "Original description",
            date = System.currentTimeMillis(),
            source = "MANUAL",
            transferAccountId = null,
            transferTransactionId = null,
            isRecurring = false,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
        
        val transactionId = transactionDao.insertTransaction(transaction)
        
        val updatedTransaction = transaction.copy(
            id = transactionId,
            merchant = "Updated Merchant",
            description = "Updated description"
        )
        
        transactionDao.updateTransaction(updatedTransaction)
        
        val retrievedTransaction = transactionDao.getTransactionById(transactionId)
        assertEquals("Updated Merchant", retrievedTransaction?.merchant)
        assertEquals("Updated description", retrievedTransaction?.description)
    }
    
    @Test
    fun deleteTransaction() = runTest {
        val (accountId, categoryId) = setupTestData()
        
        val transaction = TransactionEntity(
            amount = "100.00",
            type = "EXPENSE",
            categoryId = categoryId,
            accountId = accountId,
            merchant = "Test Merchant",
            description = null,
            date = System.currentTimeMillis(),
            source = "MANUAL",
            transferAccountId = null,
            transferTransactionId = null,
            isRecurring = false,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
        
        val transactionId = transactionDao.insertTransaction(transaction)
        
        // Verify transaction exists
        assertNotNull(transactionDao.getTransactionById(transactionId))
        
        // Delete transaction
        transactionDao.deleteTransactionById(transactionId)
        
        // Verify transaction is deleted
        assertNull(transactionDao.getTransactionById(transactionId))
    }
    
    @Test
    fun getTransactionsByMerchant() = runTest {
        val (accountId, categoryId) = setupTestData()
        
        val transactions = listOf(
            TransactionEntity(
                amount = "100.00",
                type = "EXPENSE",
                categoryId = categoryId,
                accountId = accountId,
                merchant = "Amazon India",
                description = null,
                date = System.currentTimeMillis(),
                source = "MANUAL",
                transferAccountId = null,
                transferTransactionId = null,
                isRecurring = false,
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            ),
            TransactionEntity(
                amount = "200.00",
                type = "EXPENSE",
                categoryId = categoryId,
                accountId = accountId,
                merchant = "Flipkart",
                description = null,
                date = System.currentTimeMillis(),
                source = "MANUAL",
                transferAccountId = null,
                transferTransactionId = null,
                isRecurring = false,
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )
        )
        
        transactionDao.insertTransactions(transactions)
        
        val amazonTransactions = transactionDao.getTransactionsByMerchant("Amazon")
        assertEquals(1, amazonTransactions.size)
        assertEquals("Amazon India", amazonTransactions[0].merchant)
    }
    
    @Test
    fun testTransferTransactions() = runTest {
        val (accountId1, categoryId) = setupTestData()
        
        // Create second account for transfer
        val account2 = AccountEntity(
            bankName = "Transfer Bank",
            accountType = "SAVINGS",
            accountNumber = "9876543210",
            nickname = "Transfer Account",
            currentBalance = "500.00",
            isActive = true,
            createdAt = System.currentTimeMillis()
        )
        val accountId2 = accountDao.insertAccount(account2)
        
        // Create transfer out transaction
        val transferOut = TransactionEntity(
            amount = "100.00",
            type = "TRANSFER_OUT",
            categoryId = categoryId,
            accountId = accountId1,
            merchant = "Transfer",
            description = "Transfer to savings",
            date = System.currentTimeMillis(),
            source = "MANUAL",
            transferAccountId = accountId2,
            transferTransactionId = null,
            isRecurring = false,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
        
        val transferOutId = transactionDao.insertTransaction(transferOut)
        
        // Create corresponding transfer in transaction
        val transferIn = TransactionEntity(
            amount = "100.00",
            type = "TRANSFER_IN",
            categoryId = categoryId,
            accountId = accountId2,
            merchant = "Transfer",
            description = "Transfer from checking",
            date = System.currentTimeMillis(),
            source = "MANUAL",
            transferAccountId = accountId1,
            transferTransactionId = transferOutId,
            isRecurring = false,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
        
        val transferInId = transactionDao.insertTransaction(transferIn)
        
        // Link the transactions
        transactionDao.linkTransferTransactions(transferOutId, transferInId)
        
        // Verify transfer relationships
        val transfersToAccount2 = transactionDao.getTransfersToAccount(accountId2)
        assertEquals(1, transfersToAccount2.size)
        assertEquals("TRANSFER_OUT", transfersToAccount2[0].type)
        
        val transfersFromAccount1 = transactionDao.getTransfersFromAccount(accountId2)
        assertEquals(1, transfersFromAccount1.size)
        assertEquals("TRANSFER_IN", transfersFromAccount1[0].type)
        
        // Verify linked transaction
        val linkedTransaction = transactionDao.getLinkedTransferTransaction(transferInId)
        assertNotNull(linkedTransaction)
        assertEquals(transferOutId, linkedTransaction?.id)
    }
    
    @Test
    fun testForeignKeyConstraintOnAccountDeletion() = runTest {
        val (accountId, categoryId) = setupTestData()
        
        // Insert transaction
        val transaction = TransactionEntity(
            amount = "100.00",
            type = "EXPENSE",
            categoryId = categoryId,
            accountId = accountId,
            merchant = "Test Merchant",
            description = null,
            date = System.currentTimeMillis(),
            source = "MANUAL",
            transferAccountId = null,
            transferTransactionId = null,
            isRecurring = false,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
        
        val transactionId = transactionDao.insertTransaction(transaction)
        
        // Verify transaction exists
        assertNotNull(transactionDao.getTransactionById(transactionId))
        
        // Delete account (should cascade delete transaction)
        accountDao.deleteAccountById(accountId)
        
        // Verify transaction is deleted due to CASCADE
        assertNull(transactionDao.getTransactionById(transactionId))
    }
}