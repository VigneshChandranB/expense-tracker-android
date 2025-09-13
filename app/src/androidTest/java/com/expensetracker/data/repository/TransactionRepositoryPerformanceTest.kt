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
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.math.BigDecimal
import java.time.LocalDateTime
import kotlin.system.measureTimeMillis

/**
 * Performance tests for TransactionRepository
 * Tests repository performance under various load conditions
 */
@RunWith(AndroidJUnit4::class)
class TransactionRepositoryPerformanceTest {
    
    private lateinit var database: ExpenseDatabase
    private lateinit var repository: TransactionRepository
    
    private val testCategory = Category(
        id = 1,
        name = "Test Category",
        icon = "test_icon",
        color = "#FF0000",
        isDefault = false
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
        
        setupTestData()
    }
    
    @After
    fun tearDown() {
        database.close()
    }
    
    @Test
    fun insertLargeNumberOfTransactions() = runTest {
        val transactionCount = 1000
        val transactions = generateTransactions(transactionCount)
        
        val insertTime = measureTimeMillis {
            repository.insertTransactions(transactions)
        }
        
        // Should complete within reasonable time (adjust threshold as needed)
        assertTrue("Insert of $transactionCount transactions should complete within 5 seconds", 
            insertTime < 5000)
        
        println("Inserted $transactionCount transactions in ${insertTime}ms")
    }
    
    @Test
    fun batchInsertVsIndividualInsert() = runTest {
        val transactionCount = 100
        val transactions = generateTransactions(transactionCount)
        
        // Test batch insert
        val batchTime = measureTimeMillis {
            repository.insertTransactions(transactions)
        }
        
        // Clear database for individual insert test
        database.clearAllTables()
        setupTestData()
        
        // Test individual inserts
        val individualTime = measureTimeMillis {
            transactions.forEach { transaction ->
                repository.insertTransaction(transaction)
            }
        }
        
        // Batch insert should be significantly faster
        assertTrue("Batch insert should be faster than individual inserts", 
            batchTime < individualTime)
        
        println("Batch insert: ${batchTime}ms, Individual inserts: ${individualTime}ms")
        println("Batch insert is ${individualTime / batchTime.toFloat()}x faster")
    }
    
    @Test
    fun queryPerformanceWithLargeDataset() = runTest {
        // Insert large dataset
        val transactionCount = 5000
        val transactions = generateTransactions(transactionCount)
        repository.insertTransactions(transactions)
        
        // Test various query operations
        val queryTimes = mutableMapOf<String, Long>()
        
        queryTimes["getById"] = measureTimeMillis {
            repository.getTransactionById(transactionCount / 2L)
        }
        
        queryTimes["getByCategory"] = measureTimeMillis {
            repository.getTransactionsByCategory(1)
        }
        
        queryTimes["getByDateRange"] = measureTimeMillis {
            val today = LocalDateTime.now().toLocalDate()
            repository.getTransactionsByDateRange(today.minusDays(30), today)
        }
        
        queryTimes["getByMerchant"] = measureTimeMillis {
            repository.getTransactionsByMerchant("Merchant")
        }
        
        // All queries should complete within reasonable time
        queryTimes.forEach { (operation, time) ->
            assertTrue("$operation should complete within 1 second with $transactionCount records", 
                time < 1000)
            println("$operation: ${time}ms")
        }
    }
    
    @Test
    fun cachePerformance() = runTest {
        val transaction = generateTransactions(1)[0]
        val insertedId = repository.insertTransaction(transaction)
        
        // First access (database hit)
        val firstAccessTime = measureTimeMillis {
            repository.getTransactionById(insertedId)
        }
        
        // Second access (cache hit)
        val secondAccessTime = measureTimeMillis {
            repository.getTransactionById(insertedId)
        }
        
        // Cache access should be significantly faster
        assertTrue("Cache access should be faster than database access",
            secondAccessTime < firstAccessTime)
        
        println("Database access: ${firstAccessTime}ms, Cache access: ${secondAccessTime}ms")
        println("Cache is ${firstAccessTime / secondAccessTime.toFloat()}x faster")
    }
    
    @Test
    fun concurrentOperationsPerformance() = runTest {
        val transactionCount = 100
        val transactions = generateTransactions(transactionCount)
        
        // Insert initial data
        repository.insertTransactions(transactions)
        
        // Measure concurrent read operations
        val concurrentTime = measureTimeMillis {
            // Simulate concurrent operations
            val operations = (1..50).map { index ->
                when (index % 4) {
                    0 -> repository.getTransactionById(index.toLong())
                    1 -> repository.getTransactionsByCategory(1)
                    2 -> repository.getTransactionsByMerchant("Merchant $index")
                    else -> repository.getTransactionCountByAccount(1)
                }
            }
        }
        
        assertTrue("Concurrent operations should complete within 2 seconds", 
            concurrentTime < 2000)
        
        println("50 concurrent operations completed in ${concurrentTime}ms")
    }
    
    @Test
    fun transferCreationPerformance() = runTest {
        // Create additional accounts
        for (i in 2..10) {
            val account = AccountEntity(
                id = i.toLong(),
                bankName = "Bank $i",
                accountType = "CHECKING",
                accountNumber = "123456789$i",
                nickname = "Account $i",
                currentBalance = "1000.00",
                isActive = true,
                createdAt = System.currentTimeMillis()
            )
            database.accountDao().insertAccount(account)
        }
        
        val transferCount = 100
        val transferTime = measureTimeMillis {
            repeat(transferCount) { index ->
                val fromAccount = (index % 9) + 1L
                val toAccount = ((index + 1) % 9) + 1L
                if (fromAccount != toAccount) {
                    repository.createTransfer(
                        fromAccountId = fromAccount,
                        toAccountId = toAccount,
                        amount = "100.00",
                        description = "Transfer $index",
                        date = LocalDateTime.now()
                    )
                }
            }
        }
        
        assertTrue("$transferCount transfers should complete within 3 seconds", 
            transferTime < 3000)
        
        println("Created $transferCount transfers in ${transferTime}ms")
    }
    
    @Test
    fun memoryUsageWithLargeDataset() = runTest {
        val runtime = Runtime.getRuntime()
        
        // Measure initial memory
        runtime.gc()
        val initialMemory = runtime.totalMemory() - runtime.freeMemory()
        
        // Insert large dataset
        val transactionCount = 2000
        val transactions = generateTransactions(transactionCount)
        repository.insertTransactions(transactions)
        
        // Perform various operations
        repeat(100) { index ->
            repository.getTransactionById((index % transactionCount).toLong() + 1)
        }
        
        // Measure final memory
        runtime.gc()
        val finalMemory = runtime.totalMemory() - runtime.freeMemory()
        
        val memoryIncrease = finalMemory - initialMemory
        val memoryIncreaseKB = memoryIncrease / 1024
        
        println("Memory increase: ${memoryIncreaseKB}KB for $transactionCount transactions")
        
        // Memory increase should be reasonable (adjust threshold as needed)
        assertTrue("Memory increase should be less than 50MB", 
            memoryIncreaseKB < 50 * 1024)
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
        val testAccount = AccountEntity(
            id = 1,
            bankName = "Test Bank",
            accountType = "CHECKING",
            accountNumber = "1234567890",
            nickname = "Test Account",
            currentBalance = "10000.00",
            isActive = true,
            createdAt = System.currentTimeMillis()
        )
        database.accountDao().insertAccount(testAccount)
    }
    
    private fun generateTransactions(count: Int): List<Transaction> {
        return (1..count).map { index ->
            Transaction(
                id = 0,
                amount = BigDecimal("${(index % 1000) + 1}.00"),
                type = if (index % 4 == 0) TransactionType.INCOME else TransactionType.EXPENSE,
                category = testCategory,
                merchant = "Merchant ${index % 100}",
                description = "Transaction $index",
                date = LocalDateTime.now().minusDays((index % 365).toLong()),
                source = TransactionSource.MANUAL,
                accountId = 1,
                transferAccountId = null,
                transferTransactionId = null,
                isRecurring = index % 10 == 0
            )
        }
    }
}