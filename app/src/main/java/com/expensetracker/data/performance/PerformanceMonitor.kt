package com.expensetracker.data.performance

import android.content.Context
import android.os.Build
import android.os.Debug
import android.os.Process
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Performance monitoring system for tracking app performance metrics
 */
@Singleton
class PerformanceMonitor @Inject constructor(
    private val context: Context,
    private val memoryManager: MemoryManager
) {
    
    private val performanceMetrics = ConcurrentHashMap<String, PerformanceMetric>()
    private val _performanceState = MutableStateFlow(PerformanceState())
    val performanceState: StateFlow<PerformanceState> = _performanceState.asStateFlow()
    
    private var monitoringJob: Job? = null
    private val startTime = System.currentTimeMillis()
    
    companion object {
        private const val MONITORING_INTERVAL_MS = 5000L // 5 seconds
        private const val PERFORMANCE_LOG_FILE = "performance_log.txt"
        private const val MAX_LOG_ENTRIES = 1000
    }
    
    /**
     * Start performance monitoring
     */
    fun startMonitoring() {
        monitoringJob?.cancel()
        monitoringJob = CoroutineScope(Dispatchers.Default).launch {
            while (isActive) {
                try {
                    collectPerformanceMetrics()
                    delay(MONITORING_INTERVAL_MS)
                } catch (e: Exception) {
                    logError("Performance monitoring error", e)
                }
            }
        }
    }
    
    /**
     * Stop performance monitoring
     */
    fun stopMonitoring() {
        monitoringJob?.cancel()
        monitoringJob = null
    }
    
    /**
     * Record a performance metric
     */
    fun recordMetric(name: String, value: Long, unit: MetricUnit = MetricUnit.MILLISECONDS) {
        val metric = performanceMetrics.getOrPut(name) {
            PerformanceMetric(name, unit)
        }
        metric.addValue(value)
    }
    
    /**
     * Start timing an operation
     */
    fun startTiming(operationName: String): TimingContext {
        return TimingContext(operationName, System.currentTimeMillis())
    }
    
    /**
     * Record method execution time
     */
    inline fun <T> measureTime(operationName: String, block: () -> T): T {
        val startTime = System.currentTimeMillis()
        return try {
            block()
        } finally {
            val endTime = System.currentTimeMillis()
            recordMetric(operationName, endTime - startTime)
        }
    }
    
    /**
     * Record async method execution time
     */
    suspend inline fun <T> measureTimeAsync(operationName: String, crossinline block: suspend () -> T): T {
        val startTime = System.currentTimeMillis()
        return try {
            block()
        } finally {
            val endTime = System.currentTimeMillis()
            recordMetric(operationName, endTime - startTime)
        }
    }
    
    /**
     * Collect current performance metrics
     */
    private suspend fun collectPerformanceMetrics() = withContext(Dispatchers.Default) {
        val memoryInfo = memoryManager.getMemoryInfo()
        val cpuUsage = getCpuUsage()
        val frameRate = getFrameRate()
        
        val currentState = PerformanceState(
            memoryUsage = memoryInfo.memoryUsagePercentage,
            cpuUsage = cpuUsage,
            frameRate = frameRate,
            uptime = System.currentTimeMillis() - startTime,
            isLowMemory = memoryInfo.isLowMemory,
            metrics = performanceMetrics.values.toList()
        )
        
        _performanceState.value = currentState
        
        // Log performance data
        logPerformanceData(currentState)
    }
    
    /**
     * Get CPU usage percentage
     */
    private fun getCpuUsage(): Double {
        return try {
            val pid = Process.myPid()
            val statFile = File("/proc/$pid/stat")
            if (statFile.exists()) {
                // Simplified CPU usage calculation
                // In a real implementation, you'd need to calculate delta over time
                val stats = statFile.readText().split(" ")
                val utime = stats[13].toLongOrNull() ?: 0L
                val stime = stats[14].toLongOrNull() ?: 0L
                val totalTime = utime + stime
                
                // This is a simplified calculation - real CPU usage requires time deltas
                (totalTime / 1000.0) % 100.0
            } else {
                0.0
            }
        } catch (e: Exception) {
            0.0
        }
    }
    
    /**
     * Get approximate frame rate
     */
    private fun getFrameRate(): Double {
        return try {
            // This is a simplified implementation
            // Real frame rate monitoring would require Choreographer
            60.0 // Assume 60fps for now
        } catch (e: Exception) {
            0.0
        }
    }
    
    /**
     * Log performance data to file
     */
    private suspend fun logPerformanceData(state: PerformanceState) = withContext(Dispatchers.IO) {
        try {
            val logFile = File(context.filesDir, PERFORMANCE_LOG_FILE)
            val logEntry = createLogEntry(state)
            
            if (logFile.exists() && logFile.readLines().size > MAX_LOG_ENTRIES) {
                // Rotate log file
                rotateLogFile(logFile)
            }
            
            logFile.appendText("$logEntry\n")
        } catch (e: Exception) {
            // Ignore logging errors to prevent infinite loops
        }
    }
    
    /**
     * Create log entry from performance state
     */
    private fun createLogEntry(state: PerformanceState): String {
        val timestamp = System.currentTimeMillis()
        return "$timestamp,${state.memoryUsage},${state.cpuUsage},${state.frameRate},${state.uptime},${state.isLowMemory}"
    }
    
    /**
     * Rotate log file when it gets too large
     */
    private fun rotateLogFile(logFile: File) {
        try {
            val lines = logFile.readLines()
            val keepLines = lines.takeLast(MAX_LOG_ENTRIES / 2)
            logFile.writeText(keepLines.joinToString("\n") + "\n")
        } catch (e: Exception) {
            // If rotation fails, just clear the file
            logFile.writeText("")
        }
    }
    
    /**
     * Get performance report
     */
    fun getPerformanceReport(): PerformanceReport {
        val currentState = _performanceState.value
        val recommendations = generateRecommendations(currentState)
        
        return PerformanceReport(
            currentState = currentState,
            recommendations = recommendations,
            topSlowOperations = getTopSlowOperations(),
            memoryRecommendations = memoryManager.getMemoryOptimizationRecommendations()
        )
    }
    
    /**
     * Get top slow operations
     */
    private fun getTopSlowOperations(): List<SlowOperation> {
        return performanceMetrics.values
            .filter { it.averageValue > 100 } // Operations taking more than 100ms
            .sortedByDescending { it.averageValue }
            .take(10)
            .map { metric ->
                SlowOperation(
                    name = metric.name,
                    averageTime = metric.averageValue,
                    maxTime = metric.maxValue,
                    callCount = metric.count
                )
            }
    }
    
    /**
     * Generate performance recommendations
     */
    private fun generateRecommendations(state: PerformanceState): List<PerformanceRecommendation> {
        val recommendations = mutableListOf<PerformanceRecommendation>()
        
        if (state.memoryUsage > 80) {
            recommendations.add(
                PerformanceRecommendation(
                    type = "MEMORY",
                    message = "High memory usage detected. Consider optimizing data structures.",
                    severity = Severity.HIGH
                )
            )
        }
        
        if (state.cpuUsage > 80) {
            recommendations.add(
                PerformanceRecommendation(
                    type = "CPU",
                    message = "High CPU usage detected. Consider optimizing algorithms.",
                    severity = Severity.HIGH
                )
            )
        }
        
        if (state.frameRate < 30) {
            recommendations.add(
                PerformanceRecommendation(
                    type = "UI",
                    message = "Low frame rate detected. Optimize UI rendering.",
                    severity = Severity.MEDIUM
                )
            )
        }
        
        return recommendations
    }
    
    /**
     * Log error for crash reporting
     */
    fun logError(message: String, throwable: Throwable? = null) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val errorFile = File(context.filesDir, "error_log.txt")
                val timestamp = System.currentTimeMillis()
                val errorEntry = "$timestamp: $message${throwable?.let { "\n${it.stackTraceToString()}" } ?: ""}\n"
                errorFile.appendText(errorEntry)
            } catch (e: Exception) {
                // Ignore logging errors
            }
        }
    }
    
    /**
     * Clear all performance data
     */
    fun clearPerformanceData() {
        performanceMetrics.clear()
        _performanceState.value = PerformanceState()
        
        // Clear log files
        CoroutineScope(Dispatchers.IO).launch {
            try {
                File(context.filesDir, PERFORMANCE_LOG_FILE).delete()
                File(context.filesDir, "error_log.txt").delete()
            } catch (e: Exception) {
                // Ignore cleanup errors
            }
        }
    }
}

/**
 * Timing context for measuring operation duration
 */
class TimingContext(
    private val operationName: String,
    private val startTime: Long
) {
    fun end(performanceMonitor: PerformanceMonitor) {
        val endTime = System.currentTimeMillis()
        performanceMonitor.recordMetric(operationName, endTime - startTime)
    }
}

/**
 * Performance metric tracking
 */
class PerformanceMetric(
    val name: String,
    val unit: MetricUnit
) {
    private val values = mutableListOf<Long>()
    private val totalValue = AtomicLong(0)
    private val maxVal = AtomicLong(0)
    private val minVal = AtomicLong(Long.MAX_VALUE)
    
    @Synchronized
    fun addValue(value: Long) {
        values.add(value)
        totalValue.addAndGet(value)
        
        if (value > maxVal.get()) {
            maxVal.set(value)
        }
        if (value < minVal.get()) {
            minVal.set(value)
        }
        
        // Keep only recent values to prevent memory issues
        if (values.size > 1000) {
            values.removeAt(0)
        }
    }
    
    val count: Int get() = values.size
    val averageValue: Double get() = if (count > 0) totalValue.get().toDouble() / count else 0.0
    val maxValue: Long get() = maxVal.get()
    val minValue: Long get() = if (minVal.get() == Long.MAX_VALUE) 0L else minVal.get()
}

enum class MetricUnit {
    MILLISECONDS, BYTES, COUNT, PERCENTAGE
}

enum class Severity {
    LOW, MEDIUM, HIGH, CRITICAL
}

/**
 * Current performance state
 */
data class PerformanceState(
    val memoryUsage: Double = 0.0,
    val cpuUsage: Double = 0.0,
    val frameRate: Double = 0.0,
    val uptime: Long = 0L,
    val isLowMemory: Boolean = false,
    val metrics: List<PerformanceMetric> = emptyList()
)

/**
 * Performance report
 */
data class PerformanceReport(
    val currentState: PerformanceState,
    val recommendations: List<PerformanceRecommendation>,
    val topSlowOperations: List<SlowOperation>,
    val memoryRecommendations: List<MemoryRecommendation>
)

/**
 * Performance recommendation
 */
data class PerformanceRecommendation(
    val type: String,
    val message: String,
    val severity: Severity
)

/**
 * Slow operation information
 */
data class SlowOperation(
    val name: String,
    val averageTime: Double,
    val maxTime: Long,
    val callCount: Int
)