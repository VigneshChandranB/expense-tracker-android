package com.expensetracker.data.categorization

import com.expensetracker.domain.categorization.KeywordCategorizer
import com.expensetracker.domain.categorization.MerchantCategorizer
import com.expensetracker.domain.categorization.TransactionCategorizer
import com.expensetracker.domain.model.Category
import com.expensetracker.domain.model.CategorizationResult
import com.expensetracker.domain.model.CategorizationReason
import com.expensetracker.domain.model.Transaction
import com.expensetracker.domain.repository.CategoryRepository
import com.expensetracker.domain.repository.CategorizationRepository
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Smart transaction categorizer that combines multiple categorization strategies
 */
@Singleton
class SmartTransactionCategorizer @Inject constructor(
    private val keywordCategorizer: KeywordCategorizer,
    private val merchantCategorizer: MerchantCategorizer,
    private val categoryRepository: CategoryRepository,
    private val categorizationRepository: CategorizationRepository
) : TransactionCategorizer {
    
    companion object {
        private const val HIGH_CONFIDENCE_THRESHOLD = 0.8f
        private const val MEDIUM_CONFIDENCE_THRESHOLD = 0.6f
    }
    
    override suspend fun categorizeTransaction(transaction: Transaction): CategorizationResult {
        val availableCategories = categoryRepository.getAllCategories()
        val merchant = transaction.merchant
        
        // Strategy 1: Check for user-defined rules first (highest priority)
        val userRules = categorizationRepository.getRulesForMerchant(merchant)
        val userRule = userRules.firstOrNull { it.isUserDefined }
        if (userRule != null) {
            val category = availableCategories.find { it.id == userRule.categoryId }
            if (category != null) {
                // Update rule usage
                categorizationRepository.incrementRuleUsage(userRule.id)
                return CategorizationResult(
                    category = category,
                    confidence = userRule.confidence,
                    reason = CategorizationReason.UserRule
                )
            }
        }
        
        // Strategy 2: Try merchant-based categorization (machine learning)
        val merchantResult = merchantCategorizer.categorizeByMerchant(merchant, availableCategories)
        if (merchantResult != null && merchantResult.confidence >= HIGH_CONFIDENCE_THRESHOLD) {
            return merchantResult
        }
        
        // Strategy 3: Try keyword-based categorization
        val keywordResult = keywordCategorizer.categorizeByKeywords(merchant, availableCategories)
        if (keywordResult != null && keywordResult.confidence >= MEDIUM_CONFIDENCE_THRESHOLD) {
            return keywordResult
        }
        
        // Strategy 4: Use merchant result if available (even with lower confidence)
        if (merchantResult != null && merchantResult.confidence >= MEDIUM_CONFIDENCE_THRESHOLD) {
            return merchantResult
        }
        
        // Strategy 5: Use keyword result if available (even with lower confidence)
        if (keywordResult != null) {
            return keywordResult
        }
        
        // Strategy 6: Use merchant result as last resort
        if (merchantResult != null) {
            return merchantResult
        }
        
        // Fallback: Return uncategorized
        val uncategorizedCategory = categoryRepository.getUncategorizedCategory()
        return CategorizationResult(
            category = uncategorizedCategory,
            confidence = 0.1f,
            reason = CategorizationReason.DefaultCategory
        )
    }
    
    override suspend fun learnFromUserInput(transaction: Transaction, userCategory: Category) {
        val merchant = transaction.merchant
        
        // Update merchant categorization
        merchantCategorizer.updateMerchantCategory(merchant, userCategory)
        
        // Increment merchant transaction count
        categorizationRepository.incrementMerchantTransactionCount(merchant)
        
        // Create or update user-defined rule
        val existingRules = categorizationRepository.getRulesForMerchant(merchant)
        val userRule = existingRules.firstOrNull { it.isUserDefined }
        
        if (userRule != null) {
            // Update existing rule
            val updatedRule = userRule.copy(
                categoryId = userCategory.id,
                confidence = calculateUpdatedConfidence(userRule.confidence, userRule.usageCount),
                usageCount = userRule.usageCount + 1,
                lastUsed = System.currentTimeMillis()
            )
            categorizationRepository.updateRule(updatedRule)
        } else {
            // Create new user rule
            val newRule = com.expensetracker.domain.model.CategoryRule(
                merchantPattern = merchant,
                categoryId = userCategory.id,
                confidence = 0.9f,
                isUserDefined = true,
                usageCount = 1,
                lastUsed = System.currentTimeMillis()
            )
            categorizationRepository.insertRule(newRule)
        }
    }
    
    override suspend fun suggestCategories(merchant: String): List<CategorizationResult> {
        val availableCategories = categoryRepository.getAllCategories()
        val suggestions = mutableListOf<CategorizationResult>()
        
        // Get merchant-based suggestions
        val merchantResult = merchantCategorizer.categorizeByMerchant(merchant, availableCategories)
        if (merchantResult != null) {
            suggestions.add(merchantResult)
        }
        
        // Get keyword-based suggestions
        val keywordResult = keywordCategorizer.categorizeByKeywords(merchant, availableCategories)
        if (keywordResult != null && !suggestions.any { it.category.id == keywordResult.category.id }) {
            suggestions.add(keywordResult)
        }
        
        // Get similar merchants' categories
        val similarMerchants = merchantCategorizer.findSimilarMerchants(merchant)
        for (similarMerchant in similarMerchants.take(3)) {
            val similarResult = merchantCategorizer.categorizeByMerchant(similarMerchant, availableCategories)
            if (similarResult != null && !suggestions.any { it.category.id == similarResult.category.id }) {
                suggestions.add(similarResult.copy(confidence = similarResult.confidence * 0.8f))
            }
        }
        
        return suggestions.sortedByDescending { it.confidence }.take(5)
    }
    
    override suspend fun getConfidence(merchant: String, category: Category): Float {
        val availableCategories = listOf(category)
        
        // Check merchant-based confidence
        val merchantResult = merchantCategorizer.categorizeByMerchant(merchant, availableCategories)
        if (merchantResult != null) {
            return merchantResult.confidence
        }
        
        // Check keyword-based confidence
        val keywordResult = keywordCategorizer.categorizeByKeywords(merchant, availableCategories)
        if (keywordResult != null) {
            return keywordResult.confidence
        }
        
        return 0.0f
    }
    
    private fun calculateUpdatedConfidence(currentConfidence: Float, usageCount: Int): Float {
        // Increase confidence with usage, but with diminishing returns
        val learningRate = 1.0f / (usageCount + 1)
        return kotlin.math.min(0.95f, currentConfidence + learningRate * 0.1f)
    }
}