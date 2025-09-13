package com.expensetracker.data.local.dao

import androidx.room.*
import com.expensetracker.data.local.entities.CategoryRuleEntity
import kotlinx.coroutines.flow.Flow

/**
 * DAO for category rules operations
 */
@Dao
interface CategoryRuleDao {
    
    @Query("SELECT * FROM category_rules WHERE merchantPattern LIKE '%' || :merchant || '%' ORDER BY confidence DESC")
    suspend fun getRulesForMerchant(merchant: String): List<CategoryRuleEntity>
    
    @Query("SELECT * FROM category_rules WHERE categoryId = :categoryId ORDER BY confidence DESC")
    suspend fun getRulesForCategory(categoryId: Long): List<CategoryRuleEntity>
    
    @Query("SELECT * FROM category_rules WHERE isUserDefined = 1 ORDER BY lastUsed DESC")
    suspend fun getUserDefinedRules(): List<CategoryRuleEntity>
    
    @Query("SELECT * FROM category_rules ORDER BY confidence DESC LIMIT :limit")
    suspend fun getTopRules(limit: Int = 100): List<CategoryRuleEntity>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRule(rule: CategoryRuleEntity): Long
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRules(rules: List<CategoryRuleEntity>)
    
    @Update
    suspend fun updateRule(rule: CategoryRuleEntity)
    
    @Delete
    suspend fun deleteRule(rule: CategoryRuleEntity)
    
    @Query("DELETE FROM category_rules WHERE categoryId = :categoryId")
    suspend fun deleteRulesForCategory(categoryId: Long)
    
    @Query("UPDATE category_rules SET usageCount = usageCount + 1, lastUsed = :timestamp WHERE id = :ruleId")
    suspend fun incrementUsage(ruleId: Long, timestamp: Long = System.currentTimeMillis())
    
    @Query("SELECT COUNT(*) FROM category_rules")
    suspend fun getRuleCount(): Int
    
    @Query("SELECT * FROM category_rules")
    fun observeAllRules(): Flow<List<CategoryRuleEntity>>
}