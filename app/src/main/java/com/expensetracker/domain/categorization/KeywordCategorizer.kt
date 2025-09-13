package com.expensetracker.domain.categorization

import com.expensetracker.domain.model.Category
import com.expensetracker.domain.model.CategorizationResult
import com.expensetracker.domain.model.CategorizationReason

/**
 * Keyword-based categorization engine
 * Uses predefined keywords to categorize transactions
 */
interface KeywordCategorizer {
    
    /**
     * Categorize based on merchant name keywords
     * @param merchant The merchant name to analyze
     * @param availableCategories List of available categories
     * @return CategorizationResult if a match is found, null otherwise
     */
    suspend fun categorizeByKeywords(
        merchant: String, 
        availableCategories: List<Category>
    ): CategorizationResult?
    
    /**
     * Add custom keyword mapping
     * @param keyword The keyword to match
     * @param category The category to assign
     */
    suspend fun addKeywordMapping(keyword: String, category: Category)
    
    /**
     * Remove keyword mapping
     * @param keyword The keyword to remove
     */
    suspend fun removeKeywordMapping(keyword: String)
    
    /**
     * Get all keyword mappings for a category
     * @param category The category to get keywords for
     * @return List of keywords mapped to this category
     */
    suspend fun getKeywordsForCategory(category: Category): List<String>
}