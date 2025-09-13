package com.expensetracker.domain.repository

import com.expensetracker.domain.model.Transaction
import com.expensetracker.domain.model.TransactionType
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * Repository interface for Transaction operations
 * Provides abstraction layer for transaction data access
 */
interface TransactionRepository {
    
    /**
     * Observe all transactions as a Flow
     */
    fun observeAllTransactions(): Flow<List<Transaction>>
    
    /**
     * Observe transactions for a specific account
     */
    fun observeTransactionsByAccount(accountId: Long): Flow<List<Transaction>>
    
    /**
     * Get a transaction by its ID
     */
    suspend fun getTransactionById(id: Long): Transaction?
    
    /**
     * Get transactions within a date range
     */
    suspend fun getTransactionsByDateRange(startDate: LocalDate, endDate: LocalDate): List<Transaction>
    
    /**
     * Get transactions by category
     */
    suspend fun getTransactionsByCategory(categoryId: Long): List<Transaction>
    
    /**
     * Get transactions by merchant name (partial match)
     */
    suspend fun getTransactionsByMerchant(merchantName: String): List<Transaction>
    
    /**
     * Get transactions by source type
     */
    suspend fun getTransactionsBySource(source: String): List<Transaction>
    
    /**
     * Insert a new transaction
     * @return the ID of the inserted transaction
     */
    suspend fun insertTransaction(transaction: Transaction): Long
    
    /**
     * Insert multiple transactions
     * @return list of IDs of inserted transactions
     */
    suspend fun insertTransactions(transactions: List<Transaction>): List<Long>
    
    /**
     * Update an existing transaction
     */
    suspend fun updateTransaction(transaction: Transaction)
    
    /**
     * Delete a transaction by ID
     */
    suspend fun deleteTransaction(id: Long)
    
    /**
     * Delete a transaction entity
     */
    suspend fun deleteTransaction(transaction: Transaction)
    
    /**
     * Get linked transfer transaction
     */
    suspend fun getLinkedTransferTransaction(transferId: Long): Transaction?
    
    /**
     * Get transaction count for an account
     */
    suspend fun getTransactionCountByAccount(accountId: Long): Int
    
    /**
     * Get total income for an account
     */
    suspend fun getTotalIncomeByAccount(accountId: Long): Double
    
    /**
     * Get total expenses for an account
     */
    suspend fun getTotalExpensesByAccount(accountId: Long): Double
    
    /**
     * Get all transfers involving an account
     */
    suspend fun getAllTransfersByAccount(accountId: Long): List<Transaction>
    
    /**
     * Create a transfer between two accounts
     * This creates two linked transactions (outgoing and incoming)
     */
    suspend fun createTransfer(
        fromAccountId: Long,
        toAccountId: Long,
        amount: String,
        description: String?,
        date: LocalDateTime
    ): Pair<Long, Long>
    
    /**
     * Link two transfer transactions
     */
    suspend fun linkTransferTransactions(transactionId: Long, linkedTransactionId: Long)
    
    /**
     * Search transactions by multiple criteria
     */
    suspend fun searchTransactions(
        query: String? = null,
        accountIds: List<Long>? = null,
        categoryIds: List<Long>? = null,
        types: List<TransactionType>? = null,
        startDate: LocalDate? = null,
        endDate: LocalDate? = null
    ): List<Transaction>
    
    /**
     * Get recent transactions with limit
     */
    suspend fun getRecentTransactions(limit: Int = 10): List<Transaction>
    
    /**
     * Get transactions by account with limit
     */
    suspend fun getTransactionsByAccount(accountId: Long, limit: Int? = null): List<Transaction>
}