package com.expensetracker.domain.usecase

import com.expensetracker.domain.model.Transaction
import com.expensetracker.domain.model.TransactionType
import com.expensetracker.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import javax.inject.Inject

/**
 * Use case for retrieving transactions
 * Encapsulates business logic for transaction retrieval
 */
class GetTransactionsUseCase @Inject constructor(
    private val transactionRepository: TransactionRepository
) {
    operator fun invoke(): Flow<List<Transaction>> {
        return transactionRepository.observeAllTransactions()
    }
    
    suspend fun getTransactionsByAccount(accountId: Long): Flow<List<Transaction>> {
        return transactionRepository.observeTransactionsByAccount(accountId)
    }
    
    suspend fun searchTransactions(
        query: String? = null,
        accountIds: List<Long>? = null,
        categoryIds: List<Long>? = null,
        types: List<TransactionType>? = null,
        startDate: LocalDate? = null,
        endDate: LocalDate? = null
    ): List<Transaction> {
        return transactionRepository.searchTransactions(
            query = query,
            accountIds = accountIds,
            categoryIds = categoryIds,
            types = types,
            startDate = startDate,
            endDate = endDate
        )
    }
    
    suspend fun getTransactionsByDateRange(startDate: LocalDate, endDate: LocalDate): List<Transaction> {
        return transactionRepository.getTransactionsByDateRange(startDate, endDate)
    }
    
    suspend fun getTransactionsByCategory(categoryId: Long): List<Transaction> {
        return transactionRepository.getTransactionsByCategory(categoryId)
    }
    
    suspend fun getTransactionsByMerchant(merchantName: String): List<Transaction> {
        return transactionRepository.getTransactionsByMerchant(merchantName)
    }
}