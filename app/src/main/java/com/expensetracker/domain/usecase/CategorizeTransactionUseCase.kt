package com.expensetracker.domain.usecase

import com.expensetracker.domain.categorization.TransactionCategorizer
import com.expensetracker.domain.model.Category
import com.expensetracker.domain.model.CategorizationResult
import com.expensetracker.domain.model.Transaction
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Use case for categorizing transactions
 */
@Singleton
class CategorizeTransactionUseCase @Inject constructor(
    private val transactionCategorizer: TransactionCategorizer
) {
    
    /**
     * Categorize a transaction automatically
     */
    suspend fun categorizeTransaction(transaction: Transaction): CategorizationResult {
        return transactionCategorizer.categorizeTransaction(transaction)
    }
    
    /**
     * Learn from user input to improve categorization
     */
    suspend fun learnFromUserInput(transaction: Transaction, userCategory: Category) {
        transactionCategorizer.learnFromUserInput(transaction, userCategory)
    }
    
    /**
     * Get category suggestions for a merchant
     */
    suspend fun suggestCategories(merchant: String): List<CategorizationResult> {
        return transactionCategorizer.suggestCategories(merchant)
    }
    
    /**
     * Get confidence score for a merchant-category combination
     */
    suspend fun getConfidence(merchant: String, category: Category): Float {
        return transactionCategorizer.getConfidence(merchant, category)
    }
}