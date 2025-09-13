package com.expensetracker.domain.categorization

import com.expensetracker.domain.model.Category
import com.expensetracker.domain.model.CategorizationResult
import com.expensetracker.domain.model.Transaction

/**
 * Interface for transaction categorization
 * Provides automatic categorization based on various strategies
 */
interface TransactionCategorizer {
    
    /**
     * Categorize a transaction automatically
     * @param transaction The transaction to categorize
     * @return CategorizationResult with suggested category and confidence
     */
    suspend fun categorizeTransaction(transaction: Transaction): CategorizationResult
    
    /**
     * Learn from user input to improve future categorization
     * @param transaction The transaction that was categorized
     * @param userCategory The category chosen by the user
     */
    suspend fun learnFromUserInput(transaction: Transaction, userCategory: Category)
    
    /**
     * Suggest multiple categories for a merchant
     * @param merchant The merchant name
     * @return List of suggested categories with confidence scores
     */
    suspend fun suggestCategories(merchant: String): List<CategorizationResult>
    
    /**
     * Get categorization confidence for a specific merchant-category combination
     * @param merchant The merchant name
     * @param category The category to check
     * @return Confidence score between 0.0 and 1.0
     */
    suspend fun getConfidence(merchant: String, category: Category): Float
}