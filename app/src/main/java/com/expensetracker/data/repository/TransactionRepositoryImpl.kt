package com.expensetracker.data.repository

import com.expensetracker.data.local.dao.TransactionDao
import com.expensetracker.data.local.dao.CategoryDao
import com.expensetracker.data.local.entities.TransactionEntity
import com.expensetracker.data.mapper.toDomainModel
import com.expensetracker.data.mapper.toEntity
import com.expensetracker.domain.model.Transaction
import com.expensetracker.domain.model.TransactionType
import com.expensetracker.domain.model.TransactionSource
import com.expensetracker.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneOffset
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of TransactionRepository with caching and data synchronization
 */
@Singleton
class TransactionRepositoryImpl @Inject constructor(
    private val transactionDao: TransactionDao,
    private val categoryDao: CategoryDao
) : TransactionRepository {
    
    // Mutex for thread-safe operations
    private val mutex = Mutex()
    
    // Simple in-memory cache for frequently accessed data
    private val recentTransactionsCache = mutableMapOf<Long, Transaction>()
    private val cacheMaxSize = 100
    
    override fun observeAllTransactions(): Flow<List<Transaction>> {
        return transactionDao.observeAllTransactions()
            .map { entities -> 
                entities.map { entity ->
                    val category = categoryDao.getCategoryById(entity.categoryId)?.toDomainModel()
                        ?: throw IllegalStateException("Category with ID ${entity.categoryId} not found")
                    entity.toDomainModel(category)
                }
            }
    }
    
    override fun observeTransactionsByAccount(accountId: Long): Flow<List<Transaction>> {
        return transactionDao.observeTransactionsByAccount(accountId)
            .map { entities -> 
                entities.map { entity ->
                    val category = categoryDao.getCategoryById(entity.categoryId)?.toDomainModel()
                        ?: throw IllegalStateException("Category with ID ${entity.categoryId} not found")
                    entity.toDomainModel(category)
                }
            }
    }
    
    override suspend fun getTransactionById(id: Long): Transaction? {
        // Check cache first
        recentTransactionsCache[id]?.let { return it }
        
        return transactionDao.getTransactionById(id)?.let { entity ->
            val category = categoryDao.getCategoryById(entity.categoryId)?.toDomainModel()
                ?: throw IllegalStateException("Category with ID ${entity.categoryId} not found")
            val transaction = entity.toDomainModel(category)
            // Cache the result
            cacheTransaction(transaction)
            transaction
        }
    }
    
    override suspend fun getTransactionsByDateRange(
        startDate: LocalDate,
        endDate: LocalDate
    ): List<Transaction> {
        val startTimestamp = startDate.atStartOfDay().toEpochSecond(ZoneOffset.UTC)
        val endTimestamp = endDate.atTime(23, 59, 59).toEpochSecond(ZoneOffset.UTC)
        
        return transactionDao.getTransactionsByDateRange(startTimestamp, endTimestamp)
            .map { entity ->
                val category = categoryDao.getCategoryById(entity.categoryId)?.toDomainModel()
                    ?: throw IllegalStateException("Category with ID ${entity.categoryId} not found")
                entity.toDomainModel(category)
            }
    }
    
    override suspend fun getTransactionsByCategory(categoryId: Long): List<Transaction> {
        return transactionDao.getTransactionsByCategory(categoryId)
            .map { entity ->
                val category = categoryDao.getCategoryById(entity.categoryId)?.toDomainModel()
                    ?: throw IllegalStateException("Category with ID ${entity.categoryId} not found")
                entity.toDomainModel(category)
            }
    }
    
    override suspend fun getTransactionsByMerchant(merchantName: String): List<Transaction> {
        return transactionDao.getTransactionsByMerchant(merchantName)
            .map { entity ->
                val category = categoryDao.getCategoryById(entity.categoryId)?.toDomainModel()
                    ?: throw IllegalStateException("Category with ID ${entity.categoryId} not found")
                entity.toDomainModel(category)
            }
    }
    
    override suspend fun getTransactionsBySource(source: String): List<Transaction> {
        return transactionDao.getTransactionsBySource(source)
            .map { entity ->
                val category = categoryDao.getCategoryById(entity.categoryId)?.toDomainModel()
                    ?: throw IllegalStateException("Category with ID ${entity.categoryId} not found")
                entity.toDomainModel(category)
            }
    }
    
    override suspend fun insertTransaction(transaction: Transaction): Long = mutex.withLock {
        validateTransaction(transaction)
        
        val entity = transaction.toEntity()
        val id = transactionDao.insertTransaction(entity)
        
        // Cache the inserted transaction
        cacheTransaction(transaction.copy(id = id))
        
        return id
    }
    
    override suspend fun insertTransactions(transactions: List<Transaction>): List<Long> = mutex.withLock {
        // Validate all transactions first
        transactions.forEach { validateTransaction(it) }
        
        val entities = transactions.map { it.toEntity() }
        val ids = transactionDao.insertTransactions(entities)
        
        // Cache the inserted transactions
        transactions.zip(ids).forEach { (transaction, id) ->
            cacheTransaction(transaction.copy(id = id))
        }
        
        return ids
    }
    
    override suspend fun updateTransaction(transaction: Transaction) = mutex.withLock {
        validateTransaction(transaction)
        
        val entity = transaction.toEntity()
        transactionDao.updateTransaction(entity)
        
        // Update cache
        cacheTransaction(transaction)
    }
    
    override suspend fun deleteTransaction(id: Long) = mutex.withLock {
        transactionDao.deleteTransactionById(id)
        
        // Remove from cache
        recentTransactionsCache.remove(id)
    }
    
    override suspend fun deleteTransaction(transaction: Transaction) = mutex.withLock {
        val entity = transaction.toEntity()
        transactionDao.deleteTransaction(entity)
        
        // Remove from cache
        recentTransactionsCache.remove(transaction.id)
    }
    
    override suspend fun getLinkedTransferTransaction(transferId: Long): Transaction? {
        return transactionDao.getLinkedTransferTransaction(transferId)?.let { entity ->
            val category = categoryDao.getCategoryById(entity.categoryId)?.toDomainModel()
                ?: throw IllegalStateException("Category with ID ${entity.categoryId} not found")
            entity.toDomainModel(category)
        }
    }
    
    override suspend fun getTransactionCountByAccount(accountId: Long): Int {
        return transactionDao.getTransactionCountByAccount(accountId)
    }
    
    override suspend fun getTotalIncomeByAccount(accountId: Long): Double {
        return transactionDao.getTotalIncomeByAccount(accountId) ?: 0.0
    }
    
    override suspend fun getTotalExpensesByAccount(accountId: Long): Double {
        return transactionDao.getTotalExpensesByAccount(accountId) ?: 0.0
    }
    
    override suspend fun getAllTransfersByAccount(accountId: Long): List<Transaction> {
        return transactionDao.getAllTransfersByAccount(accountId)
            .map { entity ->
                val category = categoryDao.getCategoryById(entity.categoryId)?.toDomainModel()
                    ?: throw IllegalStateException("Category with ID ${entity.categoryId} not found")
                entity.toDomainModel(category)
            }
    }
    
    override suspend fun createTransfer(
        fromAccountId: Long,
        toAccountId: Long,
        amount: String,
        description: String?,
        date: LocalDateTime
    ): Pair<Long, Long> = mutex.withLock {
        
        // Validate amount
        val amountDecimal = try {
            BigDecimal(amount)
        } catch (e: NumberFormatException) {
            throw IllegalArgumentException("Invalid amount format: $amount")
        }
        
        if (amountDecimal <= BigDecimal.ZERO) {
            throw IllegalArgumentException("Transfer amount must be positive")
        }
        
        if (fromAccountId == toAccountId) {
            throw IllegalArgumentException("Cannot transfer to the same account")
        }
        
        val timestamp = date.toEpochSecond(ZoneOffset.UTC)
        
        // Create outgoing transaction
        val outgoingTransaction = TransactionEntity(
            amount = amount,
            type = TransactionType.TRANSFER_OUT.name,
            categoryId = 10, // Transfer category ID
            accountId = fromAccountId,
            merchant = "Transfer",
            description = description,
            date = timestamp,
            source = TransactionSource.MANUAL.name,
            transferAccountId = toAccountId,
            transferTransactionId = null,
            isRecurring = false,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
        
        // Create incoming transaction
        val incomingTransaction = TransactionEntity(
            amount = amount,
            type = TransactionType.TRANSFER_IN.name,
            categoryId = 10, // Transfer category ID
            accountId = toAccountId,
            merchant = "Transfer",
            description = description,
            date = timestamp,
            source = TransactionSource.MANUAL.name,
            transferAccountId = fromAccountId,
            transferTransactionId = null,
            isRecurring = false,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
        
        // Insert both transactions
        val outgoingId = transactionDao.insertTransaction(outgoingTransaction)
        val incomingId = transactionDao.insertTransaction(incomingTransaction)
        
        // Link the transactions
        transactionDao.linkTransferTransactions(outgoingId, incomingId)
        transactionDao.linkTransferTransactions(incomingId, outgoingId)
        
        return Pair(outgoingId, incomingId)
    }
    
    override suspend fun linkTransferTransactions(transactionId: Long, linkedTransactionId: Long) {
        transactionDao.linkTransferTransactions(transactionId, linkedTransactionId)
    }
    
    override suspend fun searchTransactions(
        query: String?,
        accountIds: List<Long>?,
        categoryIds: List<Long>?,
        types: List<TransactionType>?,
        startDate: LocalDate?,
        endDate: LocalDate?
    ): List<Transaction> {
        // For now, implement basic search functionality
        // This can be enhanced with more sophisticated search capabilities
        
        var transactions = transactionDao.observeAllTransactions()
        
        // Apply filters (this is a simplified implementation)
        // In a real-world scenario, you'd want to create specific DAO methods for complex queries
        
        return emptyList() // Placeholder - would need custom DAO methods for complex search
    }
    
    /**
     * Validates transaction data before database operations
     */
    private suspend fun validateTransaction(transaction: Transaction) {
        // Validate amount
        if (transaction.amount <= BigDecimal.ZERO) {
            throw IllegalArgumentException("Transaction amount must be positive")
        }
        
        // Validate account exists (simplified - in real implementation, check with AccountRepository)
        if (transaction.accountId <= 0) {
            throw IllegalArgumentException("Invalid account ID")
        }
        
        // Validate category exists
        val category = categoryDao.getCategoryById(transaction.category.id)
        if (category == null) {
            throw IllegalArgumentException("Category with ID ${transaction.category.id} does not exist")
        }
        
        // Validate transfer accounts if it's a transfer transaction
        if (transaction.type in listOf(TransactionType.TRANSFER_IN, TransactionType.TRANSFER_OUT)) {
            if (transaction.transferAccountId == null) {
                throw IllegalArgumentException("Transfer transactions must have a transfer account ID")
            }
            if (transaction.transferAccountId == transaction.accountId) {
                throw IllegalArgumentException("Cannot transfer to the same account")
            }
        }
    }
    
    override suspend fun getRecentTransactions(limit: Int): List<Transaction> {
        return transactionDao.getRecentTransactions(limit)
            .map { entity ->
                val category = categoryDao.getCategoryById(entity.categoryId)?.toDomainModel()
                    ?: throw IllegalStateException("Category with ID ${entity.categoryId} not found")
                entity.toDomainModel(category)
            }
    }
    
    override suspend fun getTransactionsByAccount(accountId: Long, limit: Int?): List<Transaction> {
        return if (limit != null) {
            transactionDao.getTransactionsByAccountWithLimit(accountId, limit)
        } else {
            transactionDao.getTransactionsByAccount(accountId)
        }.map { entity ->
            val category = categoryDao.getCategoryById(entity.categoryId)?.toDomainModel()
                ?: throw IllegalStateException("Category with ID ${entity.categoryId} not found")
            entity.toDomainModel(category)
        }
    }

    /**
     * Cache a transaction for quick access
     */
    private fun cacheTransaction(transaction: Transaction) {
        if (recentTransactionsCache.size >= cacheMaxSize) {
            // Remove oldest entry (simple LRU-like behavior)
            val oldestKey = recentTransactionsCache.keys.first()
            recentTransactionsCache.remove(oldestKey)
        }
        recentTransactionsCache[transaction.id] = transaction
    }
}