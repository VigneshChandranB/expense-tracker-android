package com.expensetracker.domain.repository

import com.expensetracker.domain.model.CategoryRule
import com.expensetracker.domain.model.MerchantInfo
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for categorization data operations
 */
interface CategorizationRepository {
    
    // Category Rules
    suspend fun getRulesForMerchant(merchant: String): List<CategoryRule>
    suspend fun getRulesForCategory(categoryId: Long): List<CategoryRule>
    suspend fun getUserDefinedRules(): List<CategoryRule>
    suspend fun insertRule(rule: CategoryRule): Long
    suspend fun updateRule(rule: CategoryRule)
    suspend fun deleteRule(rule: CategoryRule)
    suspend fun incrementRuleUsage(ruleId: Long)
    
    // Merchant Information
    suspend fun getMerchantByName(merchantName: String): MerchantInfo?
    suspend fun getMerchantByNormalizedName(normalizedName: String): MerchantInfo?
    suspend fun findSimilarMerchants(pattern: String): List<MerchantInfo>
    suspend fun getMerchantsForCategory(categoryId: Long): List<MerchantInfo>
    suspend fun insertMerchant(merchant: MerchantInfo): Long
    suspend fun updateMerchant(merchant: MerchantInfo)
    suspend fun updateMerchantCategory(merchantName: String, categoryId: Long, confidence: Float)
    suspend fun incrementMerchantTransactionCount(merchantName: String)
    
    // Keyword Mappings
    suspend fun getKeywordMapping(keyword: String): Pair<Long, Boolean>? // categoryId, isDefault
    suspend fun getKeywordsForCategory(categoryId: Long): List<String>
    suspend fun addKeywordMapping(keyword: String, categoryId: Long, isDefault: Boolean = false)
    suspend fun removeKeywordMapping(keyword: String)
    suspend fun getDefaultKeywordMappings(): Map<String, Long>
    
    // Initialization
    suspend fun initializeDefaultKeywords()
    
    // Observables
    fun observeRules(): Flow<List<CategoryRule>>
    fun observeMerchants(): Flow<List<MerchantInfo>>
}