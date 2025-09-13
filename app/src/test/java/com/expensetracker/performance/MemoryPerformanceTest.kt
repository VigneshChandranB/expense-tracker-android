package com.expensetracker.performance

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.expensetracker.data.performance.MemoryManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Performance tests for memory management and optimization
 */
@RunWith(AndroidJUnit4::class)
class MemoryPerformanceTest {
    
    private lateinit var memoryManager: MemoryManager
    private lateinit var context: Context
    
    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        memoryManager = MemoryManager(context)
    }
    
    @After
    fun teardown() {
        memoryManager.stopMemoryMonitoring()
    }
    
    @Test
    fun `should monitor memory usage efficiently`() = runBlocking {
        memoryManager.startMemoryMonitoring()
        
        // Let monitoring run for a short period
        delay(2000)
        
        val memoryInfo = memoryManager.getMemoryInfo()
        
        // Verify memory info is reasonable
        assertTrue("Total memory should be positive", memoryInfo.totalMemory > 0)
        assertTrue("Max memory should be positive", memoryInfo.maxMemory > 0)
        assertTrue("Used memory should be positive", memoryInfo.usedMemory > 0)
        assertTrue("Memory usage percentage should be reasonable", 
            memoryInfo.memoryUsagePercentage in 0.0..100.0)
        
        memoryManager.stopMemoryMonitoring()
    }
    
    @Test
    fun `should handle memory cache efficiently`() {
        val testObjects = (1..1000).map { "TestObject$it" }
        
        // Cache objects
        val startTime = System.currentTimeMillis()
        testObjects.forEach { obj ->
            memoryManager.cacheObject("key_$obj", obj)
        }
        val cacheTime = System.currentTimeMillis() - startTime
        
        // Caching should be fast
        assertTrue("Caching 1000 objects should be fast", cacheTime < 100)
        
        // Retrieve objects
        val retrieveStartTime = System.currentTimeMillis()
        val retrievedObjects = testObjects.mapNotNull { obj ->
            memoryManager.getCachedObject<String>("key_$obj")
        }
        val retrieveTime = System.currentTimeMillis() - retrieveStartTime
        
        // Retrieval should be fast
        assertTrue("Retrieving 1000 objects should be fast", retrieveTime < 50)
        
        // All objects should be retrieved
        assertEquals(1000, retrievedObjects.size)
        assertEquals(testObjects, retrievedObjects)
    }
    
    @Test
    fun `should handle memory pressure gracefully`() = runBlocking {
        // Create memory pressure by allocating large objects
        val largeObjects = mutableListOf<ByteArray>()
        
        try {
            // Allocate memory until we approach limits
            repeat(100) {
                val largeObject = ByteArray(1024 * 1024) // 1MB each
                largeObjects.add(largeObject)
                memoryManager.cacheObject("large_$it", largeObject)
            }
            
            val memoryInfo = memoryManager.getMemoryInfo()
            
            // Memory usage should be high but not cause crashes
            assertTrue("Memory usage should be measurable", memoryInfo.memoryUsagePercentage > 10)
            
            // Get recommendations under memory pressure
            val recommendations = memoryManager.getMemoryOptimizationRecommendations()
            
            // Should provide recommendations when memory usage is high
            if (memoryInfo.memoryUsagePercentage > 50) {
                assertFalse("Should provide recommendations under memory pressure", 
                    recommendations.isEmpty())
            }
            
        } finally {
            // Clean up to prevent OOM in other tests
            largeObjects.clear()
            System.gc()
        }
    }
    
    @Test
    fun `should optimize memory cache when threshold is reached`() {
        // Fill cache beyond normal capacity
        repeat(200) { index ->
            val testObject = "TestObject$index"
            memoryManager.cacheObject("test_$index", testObject)
        }
        
        // Force memory optimization
        memoryManager.forceGarbageCollection()
        
        // Verify system is still responsive
        val memoryInfo = memoryManager.getMemoryInfo()
        assertNotNull("Memory info should be available after GC", memoryInfo)
        
        // Cache should still work after optimization
        memoryManager.cacheObject("post_gc_test", "TestValue")
        val retrieved = memoryManager.getCachedObject<String>("post_gc_test")
        assertEquals("TestValue", retrieved)
    }
    
    @Test
    fun `should provide accurate memory recommendations`() {
        val initialMemoryInfo = memoryManager.getMemoryInfo()
        val initialRecommendations = memoryManager.getMemoryOptimizationRecommendations()
        
        // Create some memory pressure
        val testObjects = (1..500).map { ByteArray(1024 * 100) } // 100KB each
        testObjects.forEachIndexed { index, obj ->
            memoryManager.cacheObject("pressure_$index", obj)
        }
        
        val pressureMemoryInfo = memoryManager.getMemoryInfo()
        val pressureRecommendations = memoryManager.getMemoryOptimizationRecommendations()
        
        // Memory usage should have increased
        assertTrue("Memory usage should increase under pressure",
            pressureMemoryInfo.usedMemory > initialMemoryInfo.usedMemory)
        
        // Should provide more recommendations under pressure
        if (pressureMemoryInfo.memoryUsagePercentage > initialMemoryInfo.memoryUsagePercentage + 10) {
            assertTrue("Should provide more recommendations under memory pressure",
                pressureRecommendations.size >= initialRecommendations.size)
        }
        
        // Recommendations should be relevant
        pressureRecommendations.forEach { recommendation ->
            assertNotNull("Recommendation message should not be null", recommendation.message)
            assertNotNull("Recommendation type should not be null", recommendation.type)
            assertNotNull("Recommendation priority should not be null", recommendation.priority)
        }
    }
    
    @Test
    fun `should handle concurrent memory operations safely`() = runBlocking {
        val concurrentOperations = 50
        
        // Run concurrent cache operations
        val jobs = (1..concurrentOperations).map { operationId ->
            kotlinx.coroutines.async {
                repeat(100) { index ->
                    val key = "concurrent_${operationId}_$index"
                    val value = "Value_${operationId}_$index"
                    
                    // Cache object
                    memoryManager.cacheObject(key, value)
                    
                    // Retrieve object
                    val retrieved = memoryManager.getCachedObject<String>(key)
                    assertEquals(value, retrieved)
                    
                    // Remove object
                    memoryManager.removeCachedObject(key)
                }
            }
        }
        
        // Wait for all operations to complete
        jobs.forEach { it.await() }
        
        // System should still be responsive
        val memoryInfo = memoryManager.getMemoryInfo()
        assertNotNull("Memory info should be available after concurrent operations", memoryInfo)
        
        // Cache should still work
        memoryManager.cacheObject("post_concurrent_test", "TestValue")
        val retrieved = memoryManager.getCachedObject<String>("post_concurrent_test")
        assertEquals("TestValue", retrieved)
    }
    
    @Test
    fun `should detect memory leaks and provide warnings`() = runBlocking {
        val initialMemoryInfo = memoryManager.getMemoryInfo()
        
        // Simulate potential memory leak by creating objects that might not be GC'd
        val potentialLeaks = mutableListOf<ByteArray>()
        repeat(50) {
            val leakyObject = ByteArray(1024 * 1024) // 1MB each
            potentialLeaks.add(leakyObject)
            memoryManager.cacheObject("potential_leak_$it", leakyObject)
        }
        
        // Force GC and check if memory is still high
        System.gc()
        delay(100) // Give GC time to work
        System.gc()
        
        val afterGcMemoryInfo = memoryManager.getMemoryInfo()
        val recommendations = memoryManager.getMemoryOptimizationRecommendations()
        
        // If memory usage is still very high after GC, should recommend investigation
        if (afterGcMemoryInfo.memoryUsagePercentage > 80) {
            val hasMemoryWarning = recommendations.any { recommendation ->
                recommendation.message.contains("memory", ignoreCase = true) ||
                recommendation.message.contains("cache", ignoreCase = true)
            }
            assertTrue("Should warn about high memory usage", hasMemoryWarning)
        }
        
        // Clean up
        potentialLeaks.clear()
        System.gc()
    }
    
    @Test
    fun `should maintain performance during memory monitoring`() = runBlocking {
        // Start memory monitoring
        memoryManager.startMemoryMonitoring()
        
        // Perform operations while monitoring is active
        val operationStartTime = System.currentTimeMillis()
        
        repeat(1000) { index ->
            memoryManager.cacheObject("perf_test_$index", "Value$index")
            val retrieved = memoryManager.getCachedObject<String>("perf_test_$index")
            assertEquals("Value$index", retrieved)
        }
        
        val operationEndTime = System.currentTimeMillis()
        val operationTime = operationEndTime - operationStartTime
        
        // Operations should still be fast even with monitoring
        assertTrue("Operations should be fast during monitoring", operationTime < 1000)
        
        // Stop monitoring
        memoryManager.stopMemoryMonitoring()
        
        // Verify monitoring can be restarted
        memoryManager.startMemoryMonitoring()
        delay(100)
        memoryManager.stopMemoryMonitoring()
    }
}