package com.expensetracker.data.categorization

import com.expensetracker.domain.categorization.KeywordCategorizer
import com.expensetracker.domain.model.Category
import com.expensetracker.domain.model.CategorizationResult
import com.expensetracker.domain.model.CategorizationReason
import com.expensetracker.domain.repository.CategorizationRepository
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of keyword-based categorization engine
 */
@Singleton
class KeywordCategorizerImpl @Inject constructor(
    private val categorizationRepository: CategorizationRepository
) : KeywordCategorizer {
    
    companion object {
        private const val KEYWORD_CONFIDENCE = 0.8f
        private const val MIN_KEYWORD_LENGTH = 3
    }
    
    override suspend fun categorizeByKeywords(
        merchant: String,
        availableCategories: List<Category>
    ): CategorizationResult? {
        val normalizedMerchant = normalizeMerchantName(merchant)
        val words = extractKeywords(normalizedMerchant)
        
        // Try exact keyword matches first
        for (word in words) {
            val mapping = categorizationRepository.getKeywordMapping(word)
            if (mapping != null) {
                val category = availableCategories.find { it.id == mapping.first }
                if (category != null) {
                    return CategorizationResult(
                        category = category,
                        confidence = KEYWORD_CONFIDENCE,
                        reason = CategorizationReason.KeywordMatch
                    )
                }
            }
        }
        
        // Try partial matches for longer keywords
        for (word in words.filter { it.length >= MIN_KEYWORD_LENGTH }) {
            val defaultMappings = categorizationRepository.getDefaultKeywordMappings()
            for ((keyword, categoryId) in defaultMappings) {
                if (word.contains(keyword, ignoreCase = true) || keyword.contains(word, ignoreCase = true)) {
                    val category = availableCategories.find { it.id == categoryId }
                    if (category != null) {
                        return CategorizationResult(
                            category = category,
                            confidence = KEYWORD_CONFIDENCE * 0.7f, // Lower confidence for partial matches
                            reason = CategorizationReason.KeywordMatch
                        )
                    }
                }
            }
        }
        
        return null
    }
    
    override suspend fun addKeywordMapping(keyword: String, category: Category) {
        val normalizedKeyword = keyword.lowercase().trim()
        if (normalizedKeyword.isNotEmpty()) {
            categorizationRepository.addKeywordMapping(normalizedKeyword, category.id, isDefault = false)
        }
    }
    
    override suspend fun removeKeywordMapping(keyword: String) {
        val normalizedKeyword = keyword.lowercase().trim()
        categorizationRepository.removeKeywordMapping(normalizedKeyword)
    }
    
    override suspend fun getKeywordsForCategory(category: Category): List<String> {
        return categorizationRepository.getKeywordsForCategory(category.id)
    }
    
    private fun normalizeMerchantName(merchant: String): String {
        return merchant
            .lowercase()
            .replace(Regex("[^a-z0-9\\s]"), " ") // Remove special characters
            .replace(Regex("\\s+"), " ") // Normalize whitespace
            .trim()
    }
    
    private fun extractKeywords(normalizedMerchant: String): List<String> {
        return normalizedMerchant
            .split(" ")
            .filter { it.length >= 2 } // Filter out very short words
            .distinct()
    }
}