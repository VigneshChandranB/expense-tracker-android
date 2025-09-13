package com.expensetracker.data.repository

import com.expensetracker.data.categorization.DefaultCategorySetup
import com.expensetracker.data.local.dao.CategoryRuleDao
import com.expensetracker.data.local.dao.MerchantInfoDao
import com.expensetracker.data.local.dao.KeywordMappingDao
import com.expensetracker.data.local.entities.CategoryRuleEntity
import com.expensetracker.data.local.entities.MerchantInfoEntity
import com.expensetracker.data.local.entities.KeywordMappingEntity
import com.expensetracker.domain.model.CategoryRule
import com.expensetracker.domain.model.MerchantInfo
import com.expensetracker.domain.repository.CategorizationRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of CategorizationRepository
 */
@Singleton
class CategorizationRepositoryImpl @Inject constructor(
    private val categoryRuleDao: CategoryRuleDao,
    private val merchantInfoDao: MerchantInfoDao,
    private val keywordMappingDao: KeywordMappingDao
) : CategorizationRepository {
    
    // Category Rules
    override suspend fun getRulesForMerchant(merchant: String): List<CategoryRule> {
        return categoryRuleDao.getRulesForMerchant(merchant).map { it.toDomainModel() }
    }
    
    override suspend fun getRulesForCategory(categoryId: Long): List<CategoryRule> {
        return categoryRuleDao.getRulesForCategory(categoryId).map { it.toDomainModel() }
    }
    
    override suspend fun getUserDefinedRules(): List<CategoryRule> {
        return categoryRuleDao.getUserDefinedRules().map { it.toDomainModel() }
    }
    
    override suspend fun insertRule(rule: CategoryRule): Long {
        return categoryRuleDao.insertRule(rule.toEntity())
    }
    
    override suspend fun updateRule(rule: CategoryRule) {
        categoryRuleDao.updateRule(rule.toEntity())
    }
    
    override suspend fun deleteRule(rule: CategoryRule) {
        categoryRuleDao.deleteRule(rule.toEntity())
    }
    
    override suspend fun incrementRuleUsage(ruleId: Long) {
        categoryRuleDao.incrementUsage(ruleId)
    }
    
    // Merchant Information
    override suspend fun getMerchantByName(merchantName: String): MerchantInfo? {
        return merchantInfoDao.getMerchantByName(merchantName)?.toDomainModel()
    }
    
    override suspend fun getMerchantByNormalizedName(normalizedName: String): MerchantInfo? {
        return merchantInfoDao.getMerchantByNormalizedName(normalizedName)?.toDomainModel()
    }
    
    override suspend fun findSimilarMerchants(pattern: String): List<MerchantInfo> {
        return merchantInfoDao.findSimilarMerchants(pattern).map { it.toDomainModel() }
    }
    
    override suspend fun getMerchantsForCategory(categoryId: Long): List<MerchantInfo> {
        return merchantInfoDao.getMerchantsForCategory(categoryId).map { it.toDomainModel() }
    }
    
    override suspend fun insertMerchant(merchant: MerchantInfo): Long {
        return merchantInfoDao.insertMerchant(merchant.toEntity())
    }
    
    override suspend fun updateMerchant(merchant: MerchantInfo) {
        merchantInfoDao.updateMerchant(merchant.toEntity())
    }
    
    override suspend fun updateMerchantCategory(merchantName: String, categoryId: Long, confidence: Float) {
        merchantInfoDao.updateMerchantCategory(merchantName, categoryId, confidence)
    }
    
    override suspend fun incrementMerchantTransactionCount(merchantName: String) {
        merchantInfoDao.incrementTransactionCount(merchantName)
    }
    
    // Keyword Mappings
    override suspend fun getKeywordMapping(keyword: String): Pair<Long, Boolean>? {
        val mapping = keywordMappingDao.getMappingByKeyword(keyword)
        return mapping?.let { Pair(it.categoryId, it.isDefault) }
    }
    
    override suspend fun getKeywordsForCategory(categoryId: Long): List<String> {
        return keywordMappingDao.getMappingsForCategory(categoryId).map { it.keyword }
    }
    
    override suspend fun addKeywordMapping(keyword: String, categoryId: Long, isDefault: Boolean) {
        val mapping = KeywordMappingEntity(
            keyword = keyword,
            categoryId = categoryId,
            isDefault = isDefault
        )
        keywordMappingDao.insertMapping(mapping)
    }
    
    override suspend fun removeKeywordMapping(keyword: String) {
        keywordMappingDao.deleteMappingByKeyword(keyword)
    }
    
    override suspend fun getDefaultKeywordMappings(): Map<String, Long> {
        return keywordMappingDao.getDefaultMappings().associate { it.keyword to it.categoryId }
    }
    
    // Initialization
    override suspend fun initializeDefaultKeywords() {
        val existingMappings = keywordMappingDao.getAllMappings()
        if (existingMappings.isEmpty()) {
            val defaultMappings = DefaultCategorySetup.DEFAULT_KEYWORD_MAPPINGS.map { (keyword, categoryId) ->
                KeywordMappingEntity(
                    keyword = keyword,
                    categoryId = categoryId,
                    isDefault = true
                )
            }
            keywordMappingDao.insertMappings(defaultMappings)
        }
    }
    
    // Observables
    override fun observeRules(): Flow<List<CategoryRule>> {
        return categoryRuleDao.observeAllRules().map { entities ->
            entities.map { it.toDomainModel() }
        }
    }
    
    override fun observeMerchants(): Flow<List<MerchantInfo>> {
        return merchantInfoDao.observeAllMerchants().map { entities ->
            entities.map { it.toDomainModel() }
        }
    }
    
    // Extension functions for mapping
    private fun CategoryRuleEntity.toDomainModel(): CategoryRule {
        return CategoryRule(
            id = id,
            merchantPattern = merchantPattern,
            categoryId = categoryId,
            confidence = confidence,
            isUserDefined = isUserDefined,
            usageCount = usageCount,
            lastUsed = lastUsed
        )
    }
    
    private fun CategoryRule.toEntity(): CategoryRuleEntity {
        return CategoryRuleEntity(
            id = id,
            merchantPattern = merchantPattern,
            categoryId = categoryId,
            confidence = confidence,
            isUserDefined = isUserDefined,
            usageCount = usageCount,
            lastUsed = lastUsed
        )
    }
    
    private fun MerchantInfoEntity.toDomainModel(): MerchantInfo {
        return MerchantInfo(
            name = name,
            normalizedName = normalizedName,
            categoryId = categoryId,
            confidence = confidence,
            transactionCount = transactionCount
        )
    }
    
    private fun MerchantInfo.toEntity(): MerchantInfoEntity {
        return MerchantInfoEntity(
            name = name,
            normalizedName = normalizedName,
            categoryId = categoryId,
            confidence = confidence,
            transactionCount = transactionCount
        )
    }
}