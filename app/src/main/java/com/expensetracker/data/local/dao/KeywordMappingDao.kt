package com.expensetracker.data.local.dao

import androidx.room.*
import com.expensetracker.data.local.entities.KeywordMappingEntity
import kotlinx.coroutines.flow.Flow

/**
 * DAO for keyword mapping operations
 */
@Dao
interface KeywordMappingDao {
    
    @Query("SELECT * FROM keyword_mappings WHERE keyword = :keyword")
    suspend fun getMappingByKeyword(keyword: String): KeywordMappingEntity?
    
    @Query("SELECT * FROM keyword_mappings WHERE categoryId = :categoryId")
    suspend fun getMappingsForCategory(categoryId: Long): List<KeywordMappingEntity>
    
    @Query("SELECT * FROM keyword_mappings WHERE isDefault = 1")
    suspend fun getDefaultMappings(): List<KeywordMappingEntity>
    
    @Query("SELECT * FROM keyword_mappings WHERE isDefault = 0")
    suspend fun getCustomMappings(): List<KeywordMappingEntity>
    
    @Query("SELECT * FROM keyword_mappings ORDER BY createdAt DESC")
    suspend fun getAllMappings(): List<KeywordMappingEntity>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMapping(mapping: KeywordMappingEntity): Long
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMappings(mappings: List<KeywordMappingEntity>)
    
    @Update
    suspend fun updateMapping(mapping: KeywordMappingEntity)
    
    @Delete
    suspend fun deleteMapping(mapping: KeywordMappingEntity)
    
    @Query("DELETE FROM keyword_mappings WHERE keyword = :keyword")
    suspend fun deleteMappingByKeyword(keyword: String)
    
    @Query("DELETE FROM keyword_mappings WHERE categoryId = :categoryId")
    suspend fun deleteMappingsForCategory(categoryId: Long)
    
    @Query("SELECT COUNT(*) FROM keyword_mappings")
    suspend fun getMappingCount(): Int
    
    @Query("SELECT * FROM keyword_mappings")
    fun observeAllMappings(): Flow<List<KeywordMappingEntity>>
}