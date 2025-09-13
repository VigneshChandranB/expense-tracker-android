package com.expensetracker.performance

import com.expensetracker.data.sms.OptimizedSmsProcessor
import com.expensetracker.data.sms.SmsProcessor
import com.expensetracker.domain.model.SmsMessage
import com.expensetracker.domain.model.Transaction
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal
import java.time.LocalDateTime

/**
 * Performance tests for SMS processing optimization
 */
class SmsProcessingPerformanceTest {
    
    private lateinit var mockSmsProcessor: SmsProcessor
    private lateinit var optimizedSmsProcessor: OptimizedSmsProcessor
    
    @Before
    fun setup() {
        mockSmsProcessor = mockk()
        optimizedSmsProcessor = OptimizedSmsProcessor(mockSmsProcessor, Dispatchers.Default)
        
        // Mock SMS processor to return a transaction
        coEvery { mockSmsProcessor.processSmsMessage(any()) } returns createMockTransaction()
    }
    
    @Test
    fun `should process 1000 SMS messages within 5 seconds`() = runTest {
        val messages = generateSmsMessages(1000)
        
        val startTime = System.currentTimeMillis()
        val results = optimizedSmsProcessor.processSmsMessagesBatch(messages)
        val endTime = System.currentTimeMillis()
        
        val processingTime = endTime - startTime
        
        // Verify all messages were processed
        assertEquals(1000, results.size)
        
        // Verify processing time is under 5 seconds
        assertTrue("Processing time $processingTime ms should be under 5000ms", processingTime < 5000)
        
        // Verify processing stats
        val stats = optimizedSmsProcessor.getProcessingStats()
        assertEquals(1000, stats.getMessagesProcessed())
        assertTrue("Average processing time should be reasonable", stats.getAverageProcessingTime() < 10.0)
    }
    
    @Test
    fun `should handle large batch processing efficiently`() = runTest {
        val batchSizes = listOf(10, 50, 100, 500, 1000)
        val processingTimes = mutableListOf<Long>()
        
        for (batchSize in batchSizes) {
            val messages = generateSmsMessages(batchSize)
            
            val startTime = System.currentTimeMillis()
            optimizedSmsProcessor.processSmsMessagesBatch(messages)
            val endTime = System.currentTimeMillis()
            
            processingTimes.add(endTime - startTime)
        }
        
        // Verify that processing time scales reasonably (not exponentially)
        for (i in 1 until processingTimes.size) {
            val previousTime = processingTimes[i - 1]
            val currentTime = processingTimes[i]
            val previousBatch = batchSizes[i - 1]
            val currentBatch = batchSizes[i]
            
            val timeRatio = currentTime.toDouble() / previousTime
            val batchRatio = currentBatch.toDouble() / previousBatch
            
            // Processing time should not increase more than 2x the batch size increase
            assertTrue(
                "Time ratio $timeRatio should not exceed 2x batch ratio $batchRatio",
                timeRatio <= batchRatio * 2
            )
        }
    }
    
    @Test
    fun `should process SMS flow efficiently with memory constraints`() = runTest {
        val messageCount = 5000
        val messages = generateSmsMessagesFlow(messageCount)
        
        val startTime = System.currentTimeMillis()
        val results = optimizedSmsProcessor.processSmsMessagesFlow(messages).toList()
        val endTime = System.currentTimeMillis()
        
        val processingTime = endTime - startTime
        
        // Verify all messages were processed
        assertEquals(messageCount, results.size)
        
        // Verify processing time is reasonable for streaming
        assertTrue("Streaming processing time $processingTime ms should be reasonable", processingTime < 10000)
        
        // Verify memory usage didn't explode (this is more of a smoke test)
        val runtime = Runtime.getRuntime()
        val usedMemory = runtime.totalMemory() - runtime.freeMemory()
        val maxMemory = runtime.maxMemory()
        val memoryUsagePercentage = (usedMemory.toDouble() / maxMemory) * 100
        
        assertTrue("Memory usage $memoryUsagePercentage% should be reasonable", memoryUsagePercentage < 80)
    }
    
    @Test
    fun `should handle concurrent processing efficiently`() = runTest {
        val messages = generateSmsMessages(500)
        val concurrentTasks = 10
        
        val startTime = System.currentTimeMillis()
        
        // Process multiple batches concurrently
        val results = (1..concurrentTasks).map {
            kotlinx.coroutines.async {
                optimizedSmsProcessor.processSmsMessagesBatch(messages)
            }
        }.map { it.await() }
        
        val endTime = System.currentTimeMillis()
        val processingTime = endTime - startTime
        
        // Verify all batches were processed
        assertEquals(concurrentTasks, results.size)
        results.forEach { batch ->
            assertEquals(500, batch.size)
        }
        
        // Concurrent processing should not take much longer than sequential
        assertTrue("Concurrent processing time $processingTime ms should be efficient", processingTime < 15000)
    }
    
    @Test
    fun `should maintain cache efficiency under load`() = runTest {
        val messages = generateSmsMessages(100)
        
        // Process same messages multiple times to test caching
        repeat(5) {
            optimizedSmsProcessor.processSmsMessagesBatch(messages)
        }
        
        val stats = optimizedSmsProcessor.getProcessingStats()
        
        // Should have processed 500 messages total (100 x 5)
        assertEquals(500, stats.getMessagesProcessed())
        
        // Average processing time should be low due to caching
        assertTrue("Average processing time should benefit from caching", stats.getAverageProcessingTime() < 5.0)
    }
    
    @Test
    fun `should handle memory optimization correctly`() = runTest {
        val messages = generateSmsMessages(2000)
        
        // Fill up cache
        optimizedSmsProcessor.processSmsMessagesBatch(messages)
        
        // Trigger memory optimization
        optimizedSmsProcessor.optimizeMemory()
        
        // Process more messages
        val additionalMessages = generateSmsMessages(1000)
        val results = optimizedSmsProcessor.processSmsMessagesBatch(additionalMessages)
        
        // Verify processing still works after optimization
        assertEquals(1000, results.size)
        
        // Verify stats are still accurate
        val stats = optimizedSmsProcessor.getProcessingStats()
        assertEquals(3000, stats.getMessagesProcessed())
    }
    
    @Test
    fun `should handle timeout scenarios gracefully`() = runTest {
        // Mock processor to simulate slow processing
        coEvery { mockSmsProcessor.processSmsMessage(any()) } coAnswers {
            kotlinx.coroutines.delay(6000) // Longer than timeout
            createMockTransaction()
        }
        
        val messages = generateSmsMessages(10)
        
        val startTime = System.currentTimeMillis()
        val results = optimizedSmsProcessor.processSmsMessagesBatch(messages)
        val endTime = System.currentTimeMillis()
        
        val processingTime = endTime - startTime
        
        // Should timeout and return nulls for timed out messages
        assertTrue("Should handle timeouts", results.any { it == null })
        
        // Should not take much longer than timeout period
        assertTrue("Should respect timeout", processingTime < 8000)
    }
    
    private fun generateSmsMessages(count: Int): List<SmsMessage> {
        return (1..count).map { index ->
            SmsMessage(
                id = index.toLong(),
                sender = "BANK$index",
                body = "Transaction of Rs.$index at MERCHANT$index on ${LocalDateTime.now()}",
                timestamp = System.currentTimeMillis() - (index * 1000),
                isRead = false
            )
        }
    }
    
    private fun generateSmsMessagesFlow(count: Int) = flowOf(*generateSmsMessages(count).toTypedArray())
    
    private fun createMockTransaction(): Transaction {
        return Transaction(
            id = 1L,
            amount = BigDecimal("100.00"),
            type = com.expensetracker.domain.model.TransactionType.EXPENSE,
            category = mockk(),
            merchant = "Test Merchant",
            description = "Test transaction",
            date = LocalDateTime.now(),
            source = com.expensetracker.domain.model.TransactionSource.SMS_AUTO,
            accountId = 1L
        )
    }
}