package com.expensetracker.performance

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.expensetracker.data.local.dao.TransactionDao
import com.expensetracker.data.local.database.ExpenseDatabase
import com.expensetracker.data.local.entity.TransactionEntity
import com.expensetracker.data.local.optimization.DatabaseOptimizer
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.math.BigDecimal

/**
 * Performance tests for database operations
 */
@RunWith(AndroidJUnit4::class)
class DatabasePerformanceTest {
    
    private lateinit var database: ExpenseDatabase
    private lateinit var transactionDao: TransactionDao
    private lateinit var databaseOptimizer: DatabaseOptimizer
    
    @Before
    fun setup() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            ExpenseDatabase::class.java
        ).allowMainThreadQueries().build()
        
        transactionDao = database.transactionDao()
        databaseOptimizer = DatabaseOptimizer(database)
    }
    
    @After
    fun teardown() {
        database.close()
    }
    
    @Test
    fun `should insert 10000 transactions within 2 seconds`() = runBlocking {
        val transactions = generateTransactions(10000)
        
        val startTime = System.currentTimeMillis()
        transactionDao.insertTransactions(transactions)
        val endTime = System.currentTimeMillis()
        
        val insertTime = endTime - startTime
        
        // Verify all transactions were inserted
        val count = transactionDao.getTransactionCount()
        assertEquals(10000, count)
        
        // Verify insertion time is under 2 seconds
        assertTrue("Insert time $insertTime ms should be under 2000ms", insertTime < 2000)
    }
    
    @Test
    fun `should query large dataset efficiently with indexes`() = runBlocking {
        // Create performance indexes first
        databaseOptimizer.createPerformanceIndexes()
        
        // Insert test data
        val transactions = generateTransactions(50000)
        transactionDao.insertTransactions(transactions)
        
        // Test various query patterns
        val queryTests = listOf(
            { transactionDao.getTransactionsByDateRange(System.currentTimeMillis() - 86400000, System.currentTimeMillis()) },
            { transactionDao.getTransactionsByAccountId(1L) },
            { transactionDao.getTransactionsByCategoryId(1L) },
            { transactionDao.searchTransactions("merchant") }
        )
        
        queryTests.forEach { queryFunction ->
            val startTime = System.currentTimeMillis()
            val results = queryFunction()
            val endTime = System.currentTimeMillis()
            
            val queryTime = endTime - startTime
            
            // Each query should complete within 500ms even with 50k records
            assertTrue("Query time $queryTime ms should be under 500ms", queryTime < 500)
            assertNotNull("Query should return results", results)
        }
    }
    
    @Test
    fun `should handle concurrent database operations efficiently`() = runBlocking {
        val concurrentOperations = 20
        val transactionsPerOperation = 1000
        
        val startTime = System.currentTimeMillis()
        
        // Run concurrent insert operations
        val jobs = (1..concurrentOperations).map { operationId ->
            kotlinx.coroutines.async {
                val transactions = generateTransactions(transactionsPerOperation, operationId * 1000L)
                transactionDao.insertTransactions(transactions)
            }
        }
        
        // Wait for all operations to complete
        jobs.forEach { it.await() }
        
        val endTime = System.currentTimeMillis()
        val totalTime = endTime - startTime
        
        // Verify all transactions were inserted
        val totalCount = transactionDao.getTransactionCount()
        assertEquals(concurrentOperations * transactionsPerOperation, totalCount)
        
        // Concurrent operations should not take excessively long
        assertTrue("Concurrent operations time $totalTime ms should be reasonable", totalTime < 10000)
    }
    
    @Test
    fun `should optimize database performance with VACUUM and ANALYZE`() = runBlocking {
        // Insert and delete data to create fragmentation
        val transactions = generateTransactions(10000)
        transactionDao.insertTransactions(transactions)
        
        // Delete half the transactions to create fragmentation
        val transactionsToDelete = transactionDao.getAllTransactions().take(5000)
        transactionsToDelete.forEach { transaction ->
            transactionDao.deleteTransaction(transaction.id)
        }
        
        // Get metrics before optimization
        val metricsBefore = databaseOptimizer.getDatabaseMetrics()
        
        // Optimize database
        databaseOptimizer.optimizeDatabase()
        
        // Get metrics after optimization
        val metricsAfter = databaseOptimizer.getDatabaseMetrics()
        
        // Database should be more compact after optimization
        assertTrue("Database should be more compact after optimization", 
            metricsAfter.fragmentationPercentage <= metricsBefore.fragmentationPercentage)
        
        // Test query performance after optimization
        val startTime = System.currentTimeMillis()
        val results = transactionDao.getAllTransactions()
        val endTime = System.currentTimeMillis()
        
        val queryTime = endTime - startTime
        assertTrue("Query after optimization should be fast", queryTime < 200)
        assertEquals(5000, results.size)
    }
    
    @Test
    fun `should validate index effectiveness`() = runBlocking {
        // Insert test data
        val transactions = generateTransactions(20000)
        transactionDao.insertTransactions(transactions)
        
        // Test query performance without indexes
        val startTimeWithoutIndexes = System.currentTimeMillis()
        val resultsWithoutIndexes = transactionDao.getTransactionsByDateRange(
            System.currentTimeMillis() - 86400000, 
            System.currentTimeMillis()
        )
        val endTimeWithoutIndexes = System.currentTimeMillis()
        val timeWithoutIndexes = endTimeWithoutIndexes - startTimeWithoutIndexes
        
        // Create indexes
        databaseOptimizer.createPerformanceIndexes()
        
        // Test same query with indexes
        val startTimeWithIndexes = System.currentTimeMillis()
        val resultsWithIndexes = transactionDao.getTransactionsByDateRange(
            System.currentTimeMillis() - 86400000, 
            System.currentTimeMillis()
        )
        val endTimeWithIndexes = System.currentTimeMillis()
        val timeWithIndexes = endTimeWithIndexes - startTimeWithIndexes
        
        // Verify results are the same
        assertEquals(resultsWithoutIndexes.size, resultsWithIndexes.size)
        
        // Query with indexes should be faster (or at least not significantly slower)
        assertTrue("Query with indexes should not be significantly slower", 
            timeWithIndexes <= timeWithoutIndexes * 1.5)
        
        // Validate that indexes exist
        val indexValidation = databaseOptimizer.validateIndexes()
        assertTrue("All recommended indexes should exist", indexValidation.isOptimal)
    }
    
    @Test
    fun `should handle batch operations efficiently`() = runBlocking {
        val batchSizes = listOf(100, 500, 1000, 5000, 10000)
        val insertTimes = mutableListOf<Long>()
        
        for (batchSize in batchSizes) {
            // Clear database
            database.clearAllTables()
            
            val transactions = generateTransactions(batchSize)
            
            val startTime = System.currentTimeMillis()
            transactionDao.insertTransactions(transactions)
            val endTime = System.currentTimeMillis()
            
            insertTimes.add(endTime - startTime)
            
            // Verify all transactions were inserted
            val count = transactionDao.getTransactionCount()
            assertEquals(batchSize, count)
        }
        
        // Verify that batch operations scale reasonably
        for (i in 1 until insertTimes.size) {
            val previousTime = insertTimes[i - 1]
            val currentTime = insertTimes[i]
            val previousBatch = batchSizes[i - 1]
            val currentBatch = batchSizes[i]
            
            val timeRatio = currentTime.toDouble() / previousTime
            val batchRatio = currentBatch.toDouble() / previousBatch
            
            // Insert time should scale roughly linearly with batch size
            assertTrue(
                "Time ratio $timeRatio should not exceed 3x batch ratio $batchRatio",
                timeRatio <= batchRatio * 3
            )
        }
    }
    
    @Test
    fun `should maintain performance under memory pressure`() = runBlocking {
        // Force garbage collection to start with clean slate
        System.gc()
        
        val initialMemory = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()
        
        // Insert large amount of data
        val largeDataSet = generateTransactions(100000)
        
        val startTime = System.currentTimeMillis()
        
        // Insert in chunks to simulate real-world usage
        largeDataSet.chunked(1000).forEach { chunk ->
            transactionDao.insertTransactions(chunk)
        }
        
        val endTime = System.currentTimeMillis()
        val insertTime = endTime - startTime
        
        // Verify all data was inserted
        val count = transactionDao.getTransactionCount()
        assertEquals(100000, count)
        
        // Check memory usage
        System.gc() // Force GC to get accurate reading
        val finalMemory = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()
        val memoryIncrease = finalMemory - initialMemory
        
        // Memory increase should be reasonable (less than 100MB for 100k records)
        assertTrue("Memory increase $memoryIncrease bytes should be reasonable", 
            memoryIncrease < 100 * 1024 * 1024)
        
        // Insert time should still be reasonable
        assertTrue("Insert time under memory pressure should be reasonable", insertTime < 30000)
        
        // Test query performance under memory pressure
        val queryStartTime = System.currentTimeMillis()
        val recentTransactions = transactionDao.getTransactionsByDateRange(
            System.currentTimeMillis() - 86400000, 
            System.currentTimeMillis()
        )
        val queryEndTime = System.currentTimeMillis()
        val queryTime = queryEndTime - queryStartTime
        
        assertTrue("Query time under memory pressure should be reasonable", queryTime < 1000)
        assertNotNull("Query should return results", recentTransactions)
    }
    
    private fun generateTransactions(count: Int, startId: Long = 1L): List<TransactionEntity> {
        return (startId until startId + count).map { id ->
            TransactionEntity(
                id = id,
                amount = BigDecimal(100 + (id % 1000)).toString(),
                type = if (id % 2 == 0L) "EXPENSE" else "INCOME",
                categoryId = (id % 10) + 1,
                accountId = (id % 5) + 1,
                merchant = "Merchant$id",
                description = "Transaction $id",
                date = System.currentTimeMillis() - (id * 1000),
                source = "SMS_AUTO",
                transferAccountId = null,
                transferTransactionId = null,
                isRecurring = false,
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )
        }
    }
}