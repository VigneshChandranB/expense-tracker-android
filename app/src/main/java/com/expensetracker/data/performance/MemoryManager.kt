package com.expensetracker.data.performance

import android.app.ActivityManager
import android.content.Context
import android.os.Debug
import kotlinx.coroutines.*
import java.lang.ref.WeakReference
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Memory manager for optimizing memory usage and garbage collection
 */
@Singleton
class MemoryManager @Inject constructor(
    private val context: Context
) {
    
    private val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
    private val memoryCache = ConcurrentHashMap<String, WeakReference<Any>>()
    private val memoryThresholds = MemoryThresholds()
    private var memoryMonitoringJob: Job? = null
    
    companion object {
        private const val MEMORY_CHECK_INTERVAL_MS = 30000L // 30 seconds
        private const val CACHE_CLEANUP_THRESHOLD = 0.8 // 80% memory usage
        private const val FORCE_GC_THRESHOLD = 0.9 // 90% memory usage
    }
    
    /**
     * Start memory monitoring
     */
    fun startMemoryMonitoring() {
        memoryMonitoringJob?.cancel()
        memoryMonitoringJob = CoroutineScope(Dispatchers.Default).launch {
            while (isActive) {
                try {
                    checkMemoryUsage()
                    delay(MEMORY_CHECK_INTERVAL_MS)
                } catch (e: Exception) {
                    // Log error but continue monitoring
                }
            }
        }
    }
    
    /**
     * Stop memory monitoring
     */
    fun stopMemoryMonitoring() {
        memoryMonitoringJob?.cancel()
        memoryMonitoringJob = null
    }
    
    /**
     * Get current memory usage information
     */
    fun getMemoryInfo(): MemoryInfo {
        val memInfo = ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(memInfo)
        
        val runtime = Runtime.getRuntime()
        val usedMemory = runtime.totalMemory() - runtime.freeMemory()
        val maxMemory = runtime.maxMemory()
        val availableMemory = maxMemory - usedMemory
        
        return MemoryInfo(
            totalMemory = runtime.totalMemory(),
            freeMemory = runtime.freeMemory(),
            maxMemory = maxMemory,
            usedMemory = usedMemory,
            availableMemory = availableMemory,
            systemAvailableMemory = memInfo.availMem,
            systemTotalMemory = memInfo.totalMem,
            isLowMemory = memInfo.lowMemory,
            memoryUsagePercentage = (usedMemory.toDouble() / maxMemory) * 100
        )
    }
    
    /**
     * Check memory usage and perform cleanup if necessary
     */
    private suspend fun checkMemoryUsage() = withContext(Dispatchers.Default) {
        val memoryInfo = getMemoryInfo()
        
        when {
            memoryInfo.memoryUsagePercentage > FORCE_GC_THRESHOLD * 100 -> {
                performAggressiveCleanup()
            }
            memoryInfo.memoryUsagePercentage > CACHE_CLEANUP_THRESHOLD * 100 -> {
                performCacheCleanup()
            }
            memoryInfo.isLowMemory -> {
                performLowMemoryCleanup()
            }
        }
    }
    
    /**
     * Perform aggressive memory cleanup
     */
    private suspend fun performAggressiveCleanup() = withContext(Dispatchers.Default) {
        // Clear all caches
        clearAllCaches()
        
        // Force garbage collection
        System.gc()
        
        // Wait a bit and force again if still high
        delay(1000)
        val memoryInfo = getMemoryInfo()
        if (memoryInfo.memoryUsagePercentage > FORCE_GC_THRESHOLD * 100) {
            System.runFinalization()
            System.gc()
        }
    }
    
    /**
     * Perform cache cleanup
     */
    private suspend fun performCacheCleanup() = withContext(Dispatchers.Default) {
        // Clean up weak references
        cleanupWeakReferences()
        
        // Suggest garbage collection
        System.gc()
    }
    
    /**
     * Perform low memory cleanup
     */
    private suspend fun performLowMemoryCleanup() = withContext(Dispatchers.Default) {
        // Clear non-essential caches
        clearNonEssentialCaches()
        
        // Clean up weak references
        cleanupWeakReferences()
        
        // Suggest garbage collection
        System.gc()
    }
    
    /**
     * Add object to memory cache with weak reference
     */
    fun <T> cacheObject(key: String, obj: T) {
        memoryCache[key] = WeakReference(obj)
    }
    
    /**
     * Get object from memory cache
     */
    @Suppress("UNCHECKED_CAST")
    fun <T> getCachedObject(key: String): T? {
        return memoryCache[key]?.get() as? T
    }
    
    /**
     * Remove object from memory cache
     */
    fun removeCachedObject(key: String) {
        memoryCache.remove(key)
    }
    
    /**
     * Clear all caches
     */
    private fun clearAllCaches() {
        memoryCache.clear()
    }
    
    /**
     * Clear non-essential caches (keep critical data)
     */
    private fun clearNonEssentialCaches() {
        val criticalKeys = memoryCache.keys.filter { key ->
            key.startsWith("critical_") || key.startsWith("essential_")
        }
        
        memoryCache.keys.removeAll { key ->
            !criticalKeys.contains(key)
        }
    }
    
    /**
     * Clean up weak references that have been garbage collected
     */
    private fun cleanupWeakReferences() {
        val keysToRemove = memoryCache.entries
            .filter { it.value.get() == null }
            .map { it.key }
        
        keysToRemove.forEach { key ->
            memoryCache.remove(key)
        }
    }
    
    /**
     * Get memory optimization recommendations
     */
    fun getMemoryOptimizationRecommendations(): List<MemoryRecommendation> {
        val memoryInfo = getMemoryInfo()
        val recommendations = mutableListOf<MemoryRecommendation>()
        
        if (memoryInfo.memoryUsagePercentage > 80) {
            recommendations.add(
                MemoryRecommendation(
                    type = RecommendationType.HIGH_MEMORY_USAGE,
                    message = "Memory usage is high (${memoryInfo.memoryUsagePercentage.toInt()}%). Consider clearing caches.",
                    priority = Priority.HIGH
                )
            )
        }
        
        if (memoryInfo.isLowMemory) {
            recommendations.add(
                MemoryRecommendation(
                    type = RecommendationType.LOW_MEMORY,
                    message = "System is low on memory. Reduce background processing.",
                    priority = Priority.CRITICAL
                )
            )
        }
        
        val cacheSize = memoryCache.size
        if (cacheSize > 100) {
            recommendations.add(
                MemoryRecommendation(
                    type = RecommendationType.LARGE_CACHE,
                    message = "Memory cache has $cacheSize entries. Consider cleanup.",
                    priority = Priority.MEDIUM
                )
            )
        }
        
        return recommendations
    }
    
    /**
     * Force garbage collection (use sparingly)
     */
    fun forceGarbageCollection() {
        System.gc()
        System.runFinalization()
        System.gc()
    }
}

/**
 * Memory usage information
 */
data class MemoryInfo(
    val totalMemory: Long,
    val freeMemory: Long,
    val maxMemory: Long,
    val usedMemory: Long,
    val availableMemory: Long,
    val systemAvailableMemory: Long,
    val systemTotalMemory: Long,
    val isLowMemory: Boolean,
    val memoryUsagePercentage: Double
)

/**
 * Memory thresholds for optimization
 */
data class MemoryThresholds(
    val warningThreshold: Double = 70.0,
    val criticalThreshold: Double = 85.0,
    val emergencyThreshold: Double = 95.0
)

/**
 * Memory optimization recommendation
 */
data class MemoryRecommendation(
    val type: RecommendationType,
    val message: String,
    val priority: Priority
)

enum class RecommendationType {
    HIGH_MEMORY_USAGE,
    LOW_MEMORY,
    LARGE_CACHE,
    MEMORY_LEAK_SUSPECTED,
    GC_PRESSURE
}

enum class Priority {
    LOW, MEDIUM, HIGH, CRITICAL
}