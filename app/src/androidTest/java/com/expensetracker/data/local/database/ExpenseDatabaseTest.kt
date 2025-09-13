package com.expensetracker.data.local.database

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.expensetracker.data.local.entities.*
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.Assert.*

/**
 * Integration tests for ExpenseDatabase
 */
@RunWith(AndroidJUnit4::class)
class ExpenseDatabaseTest {
    
    private lateinit var database: ExpenseDatabase
    
    @Before
    fun setup() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            ExpenseDatabase::class.java
        ).allowMainThreadQueries().build()
    }
    
    @After
    fun teardown() {
        database.close()
    }
    
    @Test
    fun insertAndRetrieveAccount() = runTest {
        val account = AccountEntity(
            bankName = "Test Bank",
            accountType = "SAVINGS",
            accountNumber = "1234567890",
            nickname = "Test Account",
            currentBalance = "1000.00",
            isActive = true,
            createdAt = System.currentTimeMillis()
        )
        
        val accountId = database.accountDao().insertAccount(account)
        val retrievedAccount = database.accountDao().getAccountById(accountId)
        
        assertNotNull(retrievedAccount)
        assertEquals(account.bankName, retrievedAccount?.bankName)
        assertEquals(account.accountType, retrievedAccount?.accountType)
        assertEquals(account.accountNumber, retrievedAccount?.accountNumber)
    }
    
    @Test
    fun insertAndRetrieveCategory() = runTest {
        val category = CategoryEntity(
            name = "Test Category",
            icon = "test_icon",
            color = "#FF0000",
            isDefault = false,
            parentCategoryId = null
        )
        
        val categoryId = database.categoryDao().insertCategory(category)
        val retrievedCategory = database.categoryDao().getCategoryById(categoryId)
        
        assertNotNull(retrievedCategory)
        assertEquals(category.name, retrievedCategory?.name)
        assertEquals(category.icon, retrievedCategory?.icon)
        assertEquals(category.color, retrievedCategory?.color)
    }
    
    @Test
    fun insertAndRetrieveTransaction() = runTest {
        // First insert account and category
        val account = AccountEntity(
            bankName = "Test Bank",
            accountType = "SAVINGS",
            accountNumber = "1234567890",
            nickname = "Test Account",
            currentBalance = "1000.00",
            isActive = true,
            createdAt = System.currentTimeMillis()
        )
        val accountId = database.accountDao().insertAccount(account)
        
        val category = CategoryEntity(
            name = "Test Category",
            icon = "test_icon",
            color = "#FF0000",
            isDefault = false,
            parentCategoryId = null
        )
        val categoryId = database.categoryDao().insertCategory(category)
        
        // Now insert transaction
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
        
        val transactionId = database.transactionDao().insertTransaction(transaction)
        val retrievedTransaction = database.transactionDao().getTransactionById(transactionId)
        
        assertNotNull(retrievedTransaction)
        assertEquals(transaction.amount, retrievedTransaction?.amount)
        assertEquals(transaction.type, retrievedTransaction?.type)
        assertEquals(transaction.merchant, retrievedTransaction?.merchant)
    }
    
    @Test
    fun insertAndRetrieveSmsPattern() = runTest {
        val smsPattern = SmsPatternEntity(
            bankName = "Test Bank",
            senderPattern = "TESTBNK",
            amountPattern = "Rs\\.([0-9,]+\\.?[0-9]*)",
            merchantPattern = "at ([A-Z0-9\\s]+)",
            datePattern = "on ([0-9]{2}-[0-9]{2}-[0-9]{4})",
            typePattern = "(debited|credited)",
            accountPattern = "A/c \\*([0-9]{4})",
            isActive = true
        )
        
        val patternId = database.smsPatternDao().insertSmsPattern(smsPattern)
        val retrievedPattern = database.smsPatternDao().getSmsPatternById(patternId)
        
        assertNotNull(retrievedPattern)
        assertEquals(smsPattern.bankName, retrievedPattern?.bankName)
        assertEquals(smsPattern.senderPattern, retrievedPattern?.senderPattern)
        assertEquals(smsPattern.amountPattern, retrievedPattern?.amountPattern)
    }
    
    @Test
    fun testForeignKeyConstraints() = runTest {
        // Insert account and category first
        val account = AccountEntity(
            bankName = "Test Bank",
            accountType = "SAVINGS",
            accountNumber = "1234567890",
            nickname = "Test Account",
            currentBalance = "1000.00",
            isActive = true,
            createdAt = System.currentTimeMillis()
        )
        val accountId = database.accountDao().insertAccount(account)
        
        val category = CategoryEntity(
            name = "Test Category",
            icon = "test_icon",
            color = "#FF0000",
            isDefault = false,
            parentCategoryId = null
        )
        val categoryId = database.categoryDao().insertCategory(category)
        
        // Insert transaction with valid foreign keys
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
        
        val transactionId = database.transactionDao().insertTransaction(transaction)
        assertTrue(transactionId > 0)
        
        // Verify transaction exists
        val retrievedTransaction = database.transactionDao().getTransactionById(transactionId)
        assertNotNull(retrievedTransaction)
    }
    
    @Test
    fun testDatabaseInitializationWithDefaults() = runTest {
        // Create a new database instance to trigger onCreate callback
        val testDatabase = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            ExpenseDatabase::class.java
        ).addCallback(ExpenseDatabase.databaseCallback)
         .allowMainThreadQueries()
         .build()
        
        try {
            // Verify default categories are created
            val categories = testDatabase.categoryDao().getAllCategories()
            assertTrue("Should have default categories", categories.isNotEmpty())
            
            // Verify "Uncategorized" category exists with ID 1
            val uncategorized = testDatabase.categoryDao().getCategoryById(1)
            assertNotNull("Uncategorized category should exist", uncategorized)
            assertEquals("Uncategorized", uncategorized?.name)
            assertTrue("Uncategorized should be default", uncategorized?.isDefault ?: false)
            
            // Verify other default categories exist
            val defaultCategories = testDatabase.categoryDao().getDefaultCategories()
            assertTrue("Should have multiple default categories", defaultCategories.size >= 9)
            
            // Verify default SMS patterns are created
            val smsPatterns = testDatabase.smsPatternDao().getAllSmsPatterns()
            assertTrue("Should have default SMS patterns", smsPatterns.isNotEmpty())
            
            // Verify specific bank patterns exist
            val hdfcPatterns = testDatabase.smsPatternDao().getSmsPatternsByBank("HDFC Bank")
            assertTrue("Should have HDFC patterns", hdfcPatterns.isNotEmpty())
            
        } finally {
            testDatabase.close()
        }
    }
    
    @Test
    fun testTransferForeignKeyConstraints() = runTest {
        // Create two accounts
        val account1 = AccountEntity(
            bankName = "Bank 1",
            accountType = "SAVINGS",
            accountNumber = "1111111111",
            nickname = "Account 1",
            currentBalance = "1000.00",
            isActive = true,
            createdAt = System.currentTimeMillis()
        )
        val accountId1 = database.accountDao().insertAccount(account1)
        
        val account2 = AccountEntity(
            bankName = "Bank 2",
            accountType = "SAVINGS",
            accountNumber = "2222222222",
            nickname = "Account 2",
            currentBalance = "500.00",
            isActive = true,
            createdAt = System.currentTimeMillis()
        )
        val accountId2 = database.accountDao().insertAccount(account2)
        
        val category = CategoryEntity(
            name = "Transfer",
            icon = "swap_horiz",
            color = "#607D8B",
            isDefault = true,
            parentCategoryId = null
        )
        val categoryId = database.categoryDao().insertCategory(category)
        
        // Create transfer transaction with transferAccountId
        val transferTransaction = TransactionEntity(
            amount = "200.00",
            type = "TRANSFER_OUT",
            categoryId = categoryId,
            accountId = accountId1,
            merchant = "Transfer",
            description = "Transfer to Account 2",
            date = System.currentTimeMillis(),
            source = "MANUAL",
            transferAccountId = accountId2,
            transferTransactionId = null,
            isRecurring = false,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
        
        val transactionId = database.transactionDao().insertTransaction(transferTransaction)
        val retrievedTransaction = database.transactionDao().getTransactionById(transactionId)
        
        assertNotNull(retrievedTransaction)
        assertEquals(accountId2, retrievedTransaction?.transferAccountId)
        
        // Test that deleting the transfer account sets transferAccountId to null
        database.accountDao().deleteAccountById(accountId2)
        
        val updatedTransaction = database.transactionDao().getTransactionById(transactionId)
        assertNotNull("Transaction should still exist", updatedTransaction)
        assertNull("Transfer account ID should be null after account deletion", updatedTransaction?.transferAccountId)
    }
}