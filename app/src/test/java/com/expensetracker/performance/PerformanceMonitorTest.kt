package com.expensetracker.performance

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.expensetracker.data.performance.MemoryManager
import com.expensetracker.data.performance.PerformanceMonitor
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Performance tests for the performance monitoring system
 */
@RunWith(AndroidJUnit4::class)
class PerformanceMonitorTest {
    
    private lateinit var performanceMonitor: PerformanceMonitor
    private lateinit var memoryManager: MemoryManager
    private lateinit var context: Context
    
    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        memoryManager = MemoryManager(context)
        performanceMonitor = PerformanceMonitor(context, memoryManager)
    }
    
    @After
    fun teardown() {
        performanceMonitor.stopMonitoring()
        memoryManager.stopMemoryMonitoring()
        performanceMonitor.clearPerformanceData()
    }
    
    @Test
    fun `should monitor performance metrics accurately`() = runBlocking {
        performanceMonitor.startMonitoring()
        
        // Let monitoring run for a short period
        delay(1000)
        
        val performanceState = performanceMonitor.performanceState.first()
        
        // Verify performance state contains valid data
        assertTrue("Memory usage should be non-negative", performanceState.memoryUsage >= 0)
        assertTrue("CPU usage should be non-negative", performanceState.cpuUsage >= 0)
        assertTrue("Frame rate should be non-negative", performanceState.frameRate >= 0)
        assertTrue("Uptime should be positive", performanceState.uptime > 0)
        
        performanceMonitor.stopMonitoring()
    }
    
    @Test
    fun `should record performance metrics efficiently`() {
        val metricName = "test_operation"
        val values = listOf(10L, 20L, 30L, 40L, 50L)
        
        val startTime = System.currentTimeMillis()
        values.forEach { value ->
            performanceMonitor.recordMetric(metricName, value)
        }
        val recordTime = System.currentTimeMillis() - startTime
        
        // Recording should be very fast
        assertTrue("Recording metrics should be fast", recordTime < 50)
        
        val report = performanceMonitor.getPerformanceReport()
        val testMetric = report.currentState.metrics.find { it.name == metricName }
        
        assertNotNull("Test metric should be recorded", testMetric)
        assertEquals("Should record correct count", 5, testMetric!!.count)
        assertEquals("Should calculate correct average", 30.0, testMetric.averageValue, 0.1)
        assertEquals("Should record correct max", 50L, testMetric.maxValue)
        assertEquals("Should record correct min", 10L, testMetric.minValue)
    }
    
    @Test
    fun `should measure operation time accurately`() {
        val operationName = "timed_operation"
        
        val result = performanceMonitor.measureTime(operationName) {
            Thread.sleep(100) // Simulate 100ms operation
            "test_result"
        }
        
        assertEquals("Should return operation result", "test_result", result)
        
        val report = performanceMonitor.getPerformanceReport()
        val timedMetric = report.currentState.metrics.find { it.name == operationName }
        
        assertNotNull("Timed metric should be recorded", timedMetric)
        assertTrue("Should measure time approximately correctly", 
            timedMetric!!.averageValue >= 90 && timedMetric.averageValue <= 150)
    }
    
    @Test
    fun `should measure async operation time accurately`() = runBlocking {
        val operationName = "async_timed_operation"
        
        val result = performanceMonitor.measureTimeAsync(operationName) {
            delay(100) // Simulate 100ms async operation
            "async_test_result"
        }
        
        assertEquals("Should return async operation result", "async_test_result", result)
        
        val report = performanceMonitor.getPerformanceReport()
        val timedMetric = report.currentState.metrics.find { it.name == operationName }
        
        assertNotNull("Async timed metric should be recorded", timedMetric)
        assertTrue("Should measure async time approximately correctly", 
            timedMetric!!.averageValue >= 90 && timedMetric.averageValue <= 150)
    }
    
    @Test
    fun `should handle timing context correctly`() {
        val operationName = "context_timed_operation"
        
        val timingContext = performanceMonitor.startTiming(operationName)
        Thread.sleep(50) // Simulate operation
        timingContext.end(performanceMonitor)
        
        val report = performanceMonitor.getPerformanceReport()
        val contextMetric = report.currentState.metrics.find { it.name == operationName }
        
        assertNotNull("Context timed metric should be recorded", contextMetric)
        assertTrue("Should measure context time approximately correctly", 
            contextMetric!!.averageValue >= 40 && contextMetric.averageValue <= 80)
    }
    
    @Test
    fun `should generate performance recommendations`() = runBlocking {
        // Record some metrics that should trigger recommendations
        performanceMonitor.recordMetric("slow_operation", 500L) // Slow operation
        performanceMonitor.recordMetric("slow_operation", 600L)
        performanceMonitor.recordMetric("slow_operation", 700L)
        
        performanceMonitor.startMonitoring()
        delay(500) // Let monitoring collect some data
        performanceMonitor.stopMonitoring()
        
        val report = performanceMonitor.getPerformanceReport()
        
        // Should have performance recommendations
        assertNotNull("Report should contain recommendations", report.recommendations)
        
        // Should identify slow operations
        assertNotNull("Report should contain slow operations", report.topSlowOperations)
        val slowOps = report.topSlowOperations.find { it.name == "slow_operation" }
        assertNotNull("Should identify slow operation", slowOps)
        assertTrue("Should correctly identify average time", slowOps!!.averageTime > 500)
    }
    
    @Test
    fun `should handle concurrent metric recording safely`() = runBlocking {
        val concurrentOperations = 100
        val metricsPerOperation = 50
        
        val jobs = (1..concurrentOperations).map { operationId ->
            kotlinx.coroutines.async {
                repeat(metricsPerOperation) { metricId ->
                    val metricName = "concurrent_metric_$operationId"
                    val value = (metricId * 10).toLong()
                    performanceMonitor.recordMetric(metricName, value)
                }
            }
        }
        
        // Wait for all operations to complete
        jobs.forEach { it.await() }
        
        val report = performanceMonitor.getPerformanceReport()
        
        // Should have recorded metrics from all concurrent operations
        val concurrentMetrics = report.currentState.metrics.filter { 
            it.name.startsWith("concurrent_metric_") 
        }
        
        assertEquals("Should record metrics from all operations", concurrentOperations, concurrentMetrics.size)
        
        // Each metric should have correct count
        concurrentMetrics.forEach { metric ->
            assertEquals("Each metric should have correct count", metricsPerOperation, metric.count)
        }
    }
    
    @Test
    fun `should handle performance monitoring under load`() = runBlocking {
        performanceMonitor.startMonitoring()
        
        // Generate load while monitoring
        repeat(1000) { index ->
            performanceMonitor.recordMetric("load_test_metric", index.toLong())
            
            // Simulate some work
            if (index % 100 == 0) {
                delay(1)
            }
        }
        
        delay(1000) // Let monitoring run under load
        
        val report = performanceMonitor.getPerformanceReport()
        
        // Monitoring should still work under load
        assertNotNull("Should generate report under load", report)
        assertTrue("Should record metrics under load", report.currentState.metrics.isNotEmpty())
        
        val loadMetric = report.currentState.metrics.find { it.name == "load_test_metric" }
        assertNotNull("Should record load test metrics", loadMetric)
        assertEquals("Should record all load test metrics", 1000, loadMetric!!.count)
        
        performanceMonitor.stopMonitoring()
    }
    
    @Test
    fun `should log errors without affecting performance`() {
        val errorCount = 100
        
        val startTime = System.currentTimeMillis()
        repeat(errorCount) { index ->
            performanceMonitor.logError("Test error $index", RuntimeException("Test exception $index"))
        }
        val logTime = System.currentTimeMillis() - startTime
        
        // Error logging should be fast and not block
        assertTrue("Error logging should be fast", logTime < 500)
        
        // Performance monitoring should still work after errors
        performanceMonitor.recordMetric("post_error_metric", 100L)
        val report = performanceMonitor.getPerformanceReport()
        
        val postErrorMetric = report.currentState.metrics.find { it.name == "post_error_metric" }
        assertNotNull("Should record metrics after errors", postErrorMetric)
    }
    
    @Test
    fun `should clear performance data correctly`() {
        // Record some metrics
        performanceMonitor.recordMetric("test_metric_1", 100L)
        performanceMonitor.recordMetric("test_metric_2", 200L)
        
        val reportBefore = performanceMonitor.getPerformanceReport()
        assertTrue("Should have metrics before clear", reportBefore.currentState.metrics.isNotEmpty())
        
        // Clear performance data
        performanceMonitor.clearPerformanceData()
        
        val reportAfter = performanceMonitor.getPerformanceReport()
        assertTrue("Should have no metrics after clear", reportAfter.currentState.metrics.isEmpty())
        
        // Should be able to record new metrics after clear
        performanceMonitor.recordMetric("new_metric", 300L)
        val reportNew = performanceMonitor.getPerformanceReport()
        
        val newMetric = reportNew.currentState.metrics.find { it.name == "new_metric" }
        assertNotNull("Should record new metrics after clear", newMetric)
    }
    
    @Test
    fun `should maintain performance during extended monitoring`() = runBlocking {
        performanceMonitor.startMonitoring()
        
        val monitoringDuration = 5000L // 5 seconds
        val startTime = System.currentTimeMillis()
        
        // Continuously record metrics during monitoring
        while (System.currentTimeMillis() - startTime < monitoringDuration) {
            performanceMonitor.recordMetric("extended_test", 50L)
            delay(10)
        }
        
        performanceMonitor.stopMonitoring()
        
        val report = performanceMonitor.getPerformanceReport()
        
        // Should have collected substantial data
        val extendedMetric = report.currentState.metrics.find { it.name == "extended_test" }
        assertNotNull("Should record extended metrics", extendedMetric)
        assertTrue("Should record many metrics during extended monitoring", extendedMetric!!.count > 100)
        
        // Performance should remain stable
        assertTrue("Average time should remain stable", extendedMetric.averageValue < 100)
    }
}