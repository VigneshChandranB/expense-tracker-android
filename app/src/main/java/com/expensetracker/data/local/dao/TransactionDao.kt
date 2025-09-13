package com.expensetracker.data.local.dao

import androidx.room.*
import com.expensetracker.data.local.entities.TransactionEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for Transaction operations
 */
@Dao
interface TransactionDao {
    
    @Query("SELECT * FROM transactions ORDER BY date DESC")
    fun observeAllTransactions(): Flow<List<TransactionEntity>>
    
    @Query("SELECT * FROM transactions WHERE accountId = :accountId ORDER BY date DESC")
    fun observeTransactionsByAccount(accountId: Long): Flow<List<TransactionEntity>>
    
    @Query("SELECT * FROM transactions WHERE id = :id")
    suspend fun getTransactionById(id: Long): TransactionEntity?
    
    @Query("SELECT * FROM transactions WHERE date BETWEEN :startDate AND :endDate ORDER BY date DESC")
    suspend fun getTransactionsByDateRange(startDate: Long, endDate: Long): List<TransactionEntity>
    
    @Query("SELECT * FROM transactions WHERE categoryId = :categoryId ORDER BY date DESC")
    suspend fun getTransactionsByCategory(categoryId: Long): List<TransactionEntity>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: TransactionEntity): Long
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransactions(transactions: List<TransactionEntity>): List<Long>
    
    @Update
    suspend fun updateTransaction(transaction: TransactionEntity)
    
    @Delete
    suspend fun deleteTransaction(transaction: TransactionEntity)
    
    @Query("DELETE FROM transactions WHERE id = :id")
    suspend fun deleteTransactionById(id: Long)
    
    @Query("SELECT * FROM transactions WHERE transferTransactionId = :transferId")
    suspend fun getLinkedTransferTransaction(transferId: Long): TransactionEntity?
    
    @Query("SELECT * FROM transactions WHERE merchant LIKE '%' || :merchantName || '%' ORDER BY date DESC")
    suspend fun getTransactionsByMerchant(merchantName: String): List<TransactionEntity>
    
    @Query("SELECT * FROM transactions WHERE source = :source ORDER BY date DESC")
    suspend fun getTransactionsBySource(source: String): List<TransactionEntity>
    
    @Query("SELECT COUNT(*) FROM transactions WHERE accountId = :accountId")
    suspend fun getTransactionCountByAccount(accountId: Long): Int
    
    @Query("SELECT SUM(CAST(amount AS REAL)) FROM transactions WHERE accountId = :accountId AND type IN ('INCOME', 'TRANSFER_IN')")
    suspend fun getTotalIncomeByAccount(accountId: Long): Double?
    
    @Query("SELECT SUM(CAST(amount AS REAL)) FROM transactions WHERE accountId = :accountId AND type IN ('EXPENSE', 'TRANSFER_OUT')")
    suspend fun getTotalExpensesByAccount(accountId: Long): Double?
    
    @Query("SELECT * FROM transactions WHERE transferAccountId = :accountId AND type = 'TRANSFER_OUT'")
    suspend fun getTransfersToAccount(accountId: Long): List<TransactionEntity>
    
    @Query("SELECT * FROM transactions WHERE accountId = :accountId AND type = 'TRANSFER_IN'")
    suspend fun getTransfersFromAccount(accountId: Long): List<TransactionEntity>
    
    @Query("SELECT * FROM transactions WHERE (accountId = :accountId OR transferAccountId = :accountId) AND type IN ('TRANSFER_IN', 'TRANSFER_OUT')")
    suspend fun getAllTransfersByAccount(accountId: Long): List<TransactionEntity>
    
    @Query("UPDATE transactions SET transferTransactionId = :linkedTransactionId WHERE id = :transactionId")
    suspend fun linkTransferTransactions(transactionId: Long, linkedTransactionId: Long)
    
    @Query("SELECT * FROM transactions ORDER BY date DESC LIMIT :limit")
    suspend fun getRecentTransactions(limit: Int): List<TransactionEntity>
    
    @Query("SELECT * FROM transactions WHERE accountId = :accountId ORDER BY date DESC")
    suspend fun getTransactionsByAccount(accountId: Long): List<TransactionEntity>
    
    @Query("SELECT * FROM transactions WHERE accountId = :accountId ORDER BY date DESC LIMIT :limit")
    suspend fun getTransactionsByAccountWithLimit(accountId: Long, limit: Int): List<TransactionEntity>
    
    // Security and integrity methods
    @Query("SELECT * FROM transactions ORDER BY id")
    suspend fun getAllTransactions(): List<TransactionEntity>
    
    @Query("SELECT * FROM transactions WHERE accountId NOT IN (SELECT id FROM accounts)")
    suspend fun getOrphanedTransactions(): List<TransactionEntity>
    
    @Query("SELECT * FROM transactions WHERE categoryId NOT IN (SELECT id FROM categories)")
    suspend fun getTransactionsWithInvalidCategories(): List<TransactionEntity>
    
    @Query("SELECT * FROM transactions WHERE transferAccountId IS NOT NULL AND transferTransactionId IS NULL")
    suspend fun getUnlinkedTransferTransactions(): List<TransactionEntity>
    
    @Query("DELETE FROM transactions WHERE accountId NOT IN (SELECT id FROM accounts)")
    suspend fun deleteOrphanedTransactions(): Int
    
    // Data management methods
    @Query("DELETE FROM transactions")
    suspend fun deleteAllTransactions()
    
    @Query("DELETE FROM transactions WHERE source = 'SMS_AUTO'")
    suspend fun deleteSmsTransactions()
    
    @Query("DELETE FROM transactions WHERE date < :cutoffDate")
    suspend fun deleteTransactionsOlderThan(cutoffDate: Long)
}