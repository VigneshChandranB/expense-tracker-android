package com.expensetracker.data.local.dao

import androidx.room.*
import com.expensetracker.data.local.entities.MerchantInfoEntity
import kotlinx.coroutines.flow.Flow

/**
 * DAO for merchant information operations
 */
@Dao
interface MerchantInfoDao {
    
    @Query("SELECT * FROM merchant_info WHERE name = :merchantName")
    suspend fun getMerchantByName(merchantName: String): MerchantInfoEntity?
    
    @Query("SELECT * FROM merchant_info WHERE normalizedName = :normalizedName")
    suspend fun getMerchantByNormalizedName(normalizedName: String): MerchantInfoEntity?
    
    @Query("SELECT * FROM merchant_info WHERE normalizedName LIKE '%' || :pattern || '%' ORDER BY confidence DESC LIMIT :limit")
    suspend fun findSimilarMerchants(pattern: String, limit: Int = 10): List<MerchantInfoEntity>
    
    @Query("SELECT * FROM merchant_info WHERE categoryId = :categoryId ORDER BY transactionCount DESC")
    suspend fun getMerchantsForCategory(categoryId: Long): List<MerchantInfoEntity>
    
    @Query("SELECT * FROM merchant_info WHERE categoryId IS NOT NULL ORDER BY confidence DESC LIMIT :limit")
    suspend fun getCategorizedMerchants(limit: Int = 100): List<MerchantInfoEntity>
    
    @Query("SELECT * FROM merchant_info WHERE categoryId IS NULL ORDER BY transactionCount DESC LIMIT :limit")
    suspend fun getUncategorizedMerchants(limit: Int = 50): List<MerchantInfoEntity>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMerchant(merchant: MerchantInfoEntity): Long
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMerchants(merchants: List<MerchantInfoEntity>)
    
    @Update
    suspend fun updateMerchant(merchant: MerchantInfoEntity)
    
    @Delete
    suspend fun deleteMerchant(merchant: MerchantInfoEntity)
    
    @Query("UPDATE merchant_info SET transactionCount = transactionCount + 1, lastUpdated = :timestamp WHERE name = :merchantName")
    suspend fun incrementTransactionCount(merchantName: String, timestamp: Long = System.currentTimeMillis())
    
    @Query("UPDATE merchant_info SET categoryId = :categoryId, confidence = :confidence, lastUpdated = :timestamp WHERE name = :merchantName")
    suspend fun updateMerchantCategory(
        merchantName: String, 
        categoryId: Long, 
        confidence: Float, 
        timestamp: Long = System.currentTimeMillis()
    )
    
    @Query("SELECT COUNT(*) FROM merchant_info")
    suspend fun getMerchantCount(): Int
    
    @Query("SELECT * FROM merchant_info ORDER BY lastUpdated DESC")
    fun observeAllMerchants(): Flow<List<MerchantInfoEntity>>
}