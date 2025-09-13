package com.expensetracker.domain.usecase

import com.expensetracker.domain.model.Account
import com.expensetracker.domain.model.Category
import com.expensetracker.domain.model.Transaction
import com.expensetracker.domain.model.TransactionSource
import com.expensetracker.domain.model.TransactionType
import com.expensetracker.domain.repository.AccountRepository
import com.expensetracker.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.Flow
import java.math.BigDecimal
import java.time.LocalDateTime
import javax.inject.Inject

/**
 * Use case for managing transactions (create, update, delete, transfer)
 * Encapsulates business logic for transaction management operations
 */
class ManageTransactionsUseCase @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val accountRepository: AccountRepository
) {
    
    /**
     * Create a new manual transaction
     */
    suspend fun createTransaction(
        amount: BigDecimal,
        type: TransactionType,
        category: Category,
        merchant: String,
        description: String?,
        date: LocalDateTime,
        accountId: Long
    ): Result<Long> {
        return try {
            // Validate amount is positive
            if (amount <= BigDecimal.ZERO) {
                return Result.failure(IllegalArgumentException("Amount must be positive"))
            }
            
            // Validate date is not in future
            if (date.isAfter(LocalDateTime.now())) {
                return Result.failure(IllegalArgumentException("Date cannot be in the future"))
            }
            
            // Validate account exists
            val account = accountRepository.getAccountById(accountId)
                ?: return Result.failure(IllegalArgumentException("Account not found"))
            
            if (!account.isActive) {
                return Result.failure(IllegalArgumentException("Cannot add transaction to inactive account"))
            }
            
            val transaction = Transaction(
                amount = amount,
                type = type,
                category = category,
                merchant = merchant,
                description = description,
                date = date,
                source = TransactionSource.MANUAL,
                accountId = accountId
            )
            
            val transactionId = transactionRepository.insertTransaction(transaction)
            Result.success(transactionId)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Update an existing transaction
     */
    suspend fun updateTransaction(
        transactionId: Long,
        amount: BigDecimal,
        type: TransactionType,
        category: Category,
        merchant: String,
        description: String?,
        date: LocalDateTime,
        accountId: Long
    ): Result<Unit> {
        return try {
            // Validate amount is positive
            if (amount <= BigDecimal.ZERO) {
                return Result.failure(IllegalArgumentException("Amount must be positive"))
            }
            
            // Validate date is not in future
            if (date.isAfter(LocalDateTime.now())) {
                return Result.failure(IllegalArgumentException("Date cannot be in the future"))
            }
            
            // Get existing transaction
            val existingTransaction = transactionRepository.getTransactionById(transactionId)
                ?: return Result.failure(IllegalArgumentException("Transaction not found"))
            
            // Validate account exists
            val account = accountRepository.getAccountById(accountId)
                ?: return Result.failure(IllegalArgumentException("Account not found"))
            
            if (!account.isActive) {
                return Result.failure(IllegalArgumentException("Cannot update transaction for inactive account"))
            }
            
            val updatedTransaction = existingTransaction.copy(
                amount = amount,
                type = type,
                category = category,
                merchant = merchant,
                description = description,
                date = date,
                accountId = accountId
            )
            
            transactionRepository.updateTransaction(updatedTransaction)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Delete a transaction
     */
    suspend fun deleteTransaction(transactionId: Long): Result<Unit> {
        return try {
            val transaction = transactionRepository.getTransactionById(transactionId)
                ?: return Result.failure(IllegalArgumentException("Transaction not found"))
            
            // If it's a transfer transaction, also delete the linked transaction
            if (transaction.transferTransactionId != null) {
                transactionRepository.deleteTransaction(transaction.transferTransactionId)
            }
            
            transactionRepository.deleteTransaction(transactionId)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Create a transfer between two accounts
     */
    suspend fun createTransfer(
        fromAccountId: Long,
        toAccountId: Long,
        amount: BigDecimal,
        description: String?,
        date: LocalDateTime
    ): Result<Pair<Long, Long>> {
        return try {
            // Validate amount is positive
            if (amount <= BigDecimal.ZERO) {
                return Result.failure(IllegalArgumentException("Transfer amount must be positive"))
            }
            
            // Validate date is not in future
            if (date.isAfter(LocalDateTime.now())) {
                return Result.failure(IllegalArgumentException("Date cannot be in the future"))
            }
            
            // Validate accounts are different
            if (fromAccountId == toAccountId) {
                return Result.failure(IllegalArgumentException("Cannot transfer to the same account"))
            }
            
            // Validate both accounts exist and are active
            val fromAccount = accountRepository.getAccountById(fromAccountId)
                ?: return Result.failure(IllegalArgumentException("Source account not found"))
            
            val toAccount = accountRepository.getAccountById(toAccountId)
                ?: return Result.failure(IllegalArgumentException("Destination account not found"))
            
            if (!fromAccount.isActive || !toAccount.isActive) {
                return Result.failure(IllegalArgumentException("Cannot transfer between inactive accounts"))
            }
            
            val transferIds = transactionRepository.createTransfer(
                fromAccountId = fromAccountId,
                toAccountId = toAccountId,
                amount = amount.toString(),
                description = description,
                date = date
            )
            
            Result.success(transferIds)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Get transaction by ID
     */
    suspend fun getTransaction(transactionId: Long): Transaction? {
        return transactionRepository.getTransactionById(transactionId)
    }
    
    /**
     * Observe all transactions
     */
    fun observeAllTransactions(): Flow<List<Transaction>> {
        return transactionRepository.observeAllTransactions()
    }
    
    /**
     * Observe transactions for a specific account
     */
    fun observeTransactionsByAccount(accountId: Long): Flow<List<Transaction>> {
        return transactionRepository.observeTransactionsByAccount(accountId)
    }
}