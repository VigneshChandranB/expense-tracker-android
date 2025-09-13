package com.expensetracker.data.sms

import com.expensetracker.domain.model.SmsMessage
import com.expensetracker.domain.model.Transaction
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Optimized SMS processor for handling large volumes of SMS messages efficiently
 */
@Singleton
class OptimizedSmsProcessor @Inject constructor(
    private val smsProcessor: SmsProcessor,
    private val dispatcher: CoroutineDispatcher = Dispatchers.Default
) {
    
    private val processingQueue = Channel<SmsMessage>(capacity = Channel.UNLIMITED)
    private val processedCache = ConcurrentHashMap<String, Transaction?>()
    private val processingStats = ProcessingStats()
    
    companion object {
        private const val BATCH_SIZE = 50
        private const val CACHE_SIZE_LIMIT = 1000
        private const val PROCESSING_TIMEOUT_MS = 5000L
    }
    
    /**
     * Process SMS messages in batches for optimal performance
     */
    suspend fun processSmsMessagesBatch(messages: List<SmsMessage>): List<Transaction?> {
        return withContext(dispatcher) {
            val startTime = System.currentTimeMillis()
            
            try {
                val results = messages.chunked(BATCH_SIZE).flatMap { batch ->
                    processBatch(batch)
                }
                
                val processingTime = System.currentTimeMillis() - startTime
                processingStats.recordBatchProcessing(messages.size, processingTime)
                
                results
            } catch (e: Exception) {
                processingStats.recordError()
                throw e
            }
        }
    }
    
    /**
     * Process SMS messages as a flow for memory-efficient streaming
     */
    fun processSmsMessagesFlow(messages: Flow<SmsMessage>): Flow<Transaction?> = flow {
        messages
            .buffer(BATCH_SIZE)
            .flowOn(dispatcher)
            .collect { message ->
                val result = processSingleMessage(message)
                emit(result)
            }
    }
    
    private suspend fun processBatch(batch: List<SmsMessage>): List<Transaction?> {
        return coroutineScope {
            batch.map { message ->
                async {
                    processSingleMessage(message)
                }
            }.awaitAll()
        }
    }
    
    private suspend fun processSingleMessage(message: SmsMessage): Transaction? {
        // Check cache first
        val cacheKey = generateCacheKey(message)
        processedCache[cacheKey]?.let { return it }
        
        return withTimeoutOrNull(PROCESSING_TIMEOUT_MS) {
            val result = smsProcessor.processSmsMessage(message)
            
            // Cache result if cache isn't full
            if (processedCache.size < CACHE_SIZE_LIMIT) {
                processedCache[cacheKey] = result
            }
            
            processingStats.recordMessageProcessed()
            result
        }
    }
    
    private fun generateCacheKey(message: SmsMessage): String {
        return "${message.sender}_${message.body.hashCode()}_${message.timestamp}"
    }
    
    /**
     * Clear processing cache to free memory
     */
    fun clearCache() {
        processedCache.clear()
    }
    
    /**
     * Get processing statistics
     */
    fun getProcessingStats(): ProcessingStats = processingStats.copy()
    
    /**
     * Optimize memory usage by clearing old cache entries
     */
    fun optimizeMemory() {
        if (processedCache.size > CACHE_SIZE_LIMIT * 0.8) {
            // Remove oldest entries (simple LRU-like behavior)
            val entriesToRemove = processedCache.size - (CACHE_SIZE_LIMIT / 2)
            processedCache.keys.take(entriesToRemove).forEach { key ->
                processedCache.remove(key)
            }
        }
    }
}

/**
 * Statistics for SMS processing performance monitoring
 */
data class ProcessingStats(
    private val messagesProcessed: AtomicInteger = AtomicInteger(0),
    private val batchesProcessed: AtomicInteger = AtomicInteger(0),
    private val totalProcessingTime: AtomicInteger = AtomicInteger(0),
    private val errors: AtomicInteger = AtomicInteger(0)
) {
    
    fun recordMessageProcessed() {
        messagesProcessed.incrementAndGet()
    }
    
    fun recordBatchProcessing(batchSize: Int, processingTime: Long) {
        batchesProcessed.incrementAndGet()
        totalProcessingTime.addAndGet(processingTime.toInt())
    }
    
    fun recordError() {
        errors.incrementAndGet()
    }
    
    fun getMessagesProcessed(): Int = messagesProcessed.get()
    fun getBatchesProcessed(): Int = batchesProcessed.get()
    fun getTotalProcessingTime(): Long = totalProcessingTime.get().toLong()
    fun getErrors(): Int = errors.get()
    fun getAverageProcessingTime(): Double {
        val messages = getMessagesProcessed()
        return if (messages > 0) getTotalProcessingTime().toDouble() / messages else 0.0
    }
    
    fun copy(): ProcessingStats = ProcessingStats(
        AtomicInteger(messagesProcessed.get()),
        AtomicInteger(batchesProcessed.get()),
        AtomicInteger(totalProcessingTime.get()),
        AtomicInteger(errors.get())
    )
}