package com.expensetracker.data.categorization

import com.expensetracker.domain.categorization.MerchantCategorizer
import com.expensetracker.domain.model.Category
import com.expensetracker.domain.model.CategorizationResult
import com.expensetracker.domain.model.CategorizationReason
import com.expensetracker.domain.model.MerchantInfo
import com.expensetracker.domain.repository.CategorizationRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.max

/**
 * Implementation of machine learning-based merchant categorizer
 */
@Singleton
class MerchantCategorizerImpl @Inject constructor(
    private val categorizationRepository: CategorizationRepository
) : MerchantCategorizer {
    
    companion object {
        private const val MIN_CONFIDENCE_THRESHOLD = 0.6f
        private const val EXACT_MATCH_CONFIDENCE = 0.9f
        private const val SIMILAR_MATCH_CONFIDENCE = 0.7f
        private const val MIN_TRANSACTION_COUNT = 2
    }
    
    override suspend fun categorizeByMerchant(
        merchant: String,
        availableCategories: List<Category>
    ): CategorizationResult? {
        val normalizedMerchant = normalizeMerchantName(merchant)
        
        // Try exact merchant match first
        val exactMatch = categorizationRepository.getMerchantByNormalizedName(normalizedMerchant)
        if (exactMatch != null && exactMatch.categoryId != null && exactMatch.confidence >= MIN_CONFIDENCE_THRESHOLD) {
            val category = availableCategories.find { it.id == exactMatch.categoryId }
            if (category != null) {
                return CategorizationResult(
                    category = category,
                    confidence = exactMatch.confidence,
                    reason = CategorizationReason.MerchantHistory
                )
            }
        }
        
        // Try similar merchant matches
        val similarMerchants = categorizationRepository.findSimilarMerchants(normalizedMerchant)
        val categorizedSimilar = similarMerchants.filter { 
            it.categoryId != null && 
            it.confidence >= MIN_CONFIDENCE_THRESHOLD &&
            it.transactionCount >= MIN_TRANSACTION_COUNT
        }
        
        if (categorizedSimilar.isNotEmpty()) {
            // Find the most confident similar merchant
            val bestMatch = categorizedSimilar.maxByOrNull { it.confidence * it.transactionCount }
            if (bestMatch != null) {
                val category = availableCategories.find { it.id == bestMatch.categoryId }
                if (category != null) {
                    val similarity = calculateSimilarity(normalizedMerchant, bestMatch.normalizedName)
                    val adjustedConfidence = bestMatch.confidence * similarity
                    
                    if (adjustedConfidence >= MIN_CONFIDENCE_THRESHOLD) {
                        return CategorizationResult(
                            category = category,
                            confidence = adjustedConfidence,
                            reason = CategorizationReason.MachineLearning(
                                features = listOf("similar_merchant:${bestMatch.name}")
                            )
                        )
                    }
                }
            }
        }
        
        return null
    }
    
    override suspend fun updateMerchantCategory(merchant: String, category: Category) {
        val normalizedMerchant = normalizeMerchantName(merchant)
        
        // Get existing merchant info or create new
        val existingMerchant = categorizationRepository.getMerchantByNormalizedName(normalizedMerchant)
        
        if (existingMerchant != null) {
            // Update existing merchant
            val newConfidence = calculateNewConfidence(
                existingMerchant.confidence,
                existingMerchant.transactionCount,
                category.id == existingMerchant.categoryId
            )
            
            categorizationRepository.updateMerchantCategory(
                merchantName = merchant,
                categoryId = category.id,
                confidence = newConfidence
            )
        } else {
            // Create new merchant info
            val newMerchant = MerchantInfo(
                name = merchant,
                normalizedName = normalizedMerchant,
                categoryId = category.id,
                confidence = EXACT_MATCH_CONFIDENCE,
                transactionCount = 1
            )
            categorizationRepository.insertMerchant(newMerchant)
        }
    }
    
    override suspend fun getMerchantInfo(merchant: String): MerchantInfo? {
        return categorizationRepository.getMerchantByName(merchant)
    }
    
    override fun normalizeMerchantName(merchant: String): String {
        return merchant
            .lowercase()
            .replace(Regex("[^a-z0-9\\s]"), " ") // Remove special characters
            .replace(Regex("\\b(pvt|ltd|llc|inc|corp|co|company|limited)\\b"), "") // Remove company suffixes
            .replace(Regex("\\s+"), " ") // Normalize whitespace
            .trim()
    }
    
    override suspend fun findSimilarMerchants(merchant: String): List<String> {
        val normalizedMerchant = normalizeMerchantName(merchant)
        val words = normalizedMerchant.split(" ").filter { it.length >= 3 }
        
        val similarMerchants = mutableSetOf<String>()
        
        // Find merchants containing any of the words
        for (word in words) {
            val matches = categorizationRepository.findSimilarMerchants(word)
            similarMerchants.addAll(matches.map { it.name })
        }
        
        return similarMerchants.toList()
    }
    
    private fun calculateSimilarity(merchant1: String, merchant2: String): Float {
        val words1 = merchant1.split(" ").toSet()
        val words2 = merchant2.split(" ").toSet()
        
        val intersection = words1.intersect(words2).size
        val union = words1.union(words2).size
        
        return if (union == 0) 0f else intersection.toFloat() / union.toFloat()
    }
    
    private fun calculateNewConfidence(
        currentConfidence: Float,
        transactionCount: Int,
        isSameCategory: Boolean
    ): Float {
        val weight = 1.0f / (transactionCount + 1)
        val newValue = if (isSameCategory) 1.0f else 0.0f
        
        return max(
            MIN_CONFIDENCE_THRESHOLD,
            currentConfidence * (1 - weight) + newValue * weight
        )
    }
}