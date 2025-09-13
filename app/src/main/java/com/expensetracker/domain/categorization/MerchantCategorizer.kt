package com.expensetracker.domain.categorization

import com.expensetracker.domain.model.Category
import com.expensetracker.domain.model.CategorizationResult
import com.expensetracker.domain.model.MerchantInfo

/**
 * Machine learning-based merchant categorizer
 * Uses historical data and patterns to categorize merchants
 */
interface MerchantCategorizer {
    
    /**
     * Categorize based on merchant history and patterns
     * @param merchant The merchant name
     * @param availableCategories List of available categories
     * @return CategorizationResult if a match is found, null otherwise
     */
    suspend fun categorizeByMerchant(
        merchant: String,
        availableCategories: List<Category>
    ): CategorizationResult?
    
    /**
     * Update merchant information based on user feedback
     * @param merchant The merchant name
     * @param category The category assigned by user
     */
    suspend fun updateMerchantCategory(merchant: String, category: Category)
    
    /**
     * Get merchant information
     * @param merchant The merchant name
     * @return MerchantInfo if found, null otherwise
     */
    suspend fun getMerchantInfo(merchant: String): MerchantInfo?
    
    /**
     * Normalize merchant name for better matching
     * @param merchant The raw merchant name
     * @return Normalized merchant name
     */
    fun normalizeMerchantName(merchant: String): String
    
    /**
     * Find similar merchants based on name patterns
     * @param merchant The merchant to find similarities for
     * @return List of similar merchant names
     */
    suspend fun findSimilarMerchants(merchant: String): List<String>
}