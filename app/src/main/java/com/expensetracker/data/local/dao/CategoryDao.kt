package com.expensetracker.data.local.dao

import androidx.room.*
import com.expensetracker.data.local.entities.CategoryEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for Category operations
 */
@Dao
interface CategoryDao {
    
    @Query("SELECT * FROM categories ORDER BY name ASC")
    fun observeAllCategories(): Flow<List<CategoryEntity>>
    
    @Query("SELECT * FROM categories WHERE isDefault = 1 ORDER BY name ASC")
    fun observeDefaultCategories(): Flow<List<CategoryEntity>>
    
    @Query("SELECT * FROM categories WHERE id = :id")
    suspend fun getCategoryById(id: Long): CategoryEntity?
    
    @Query("SELECT * FROM categories")
    suspend fun getAllCategories(): List<CategoryEntity>
    
    @Query("SELECT * FROM categories WHERE isDefault = 1")
    suspend fun getDefaultCategories(): List<CategoryEntity>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategory(category: CategoryEntity): Long
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategories(categories: List<CategoryEntity>): List<Long>
    
    @Update
    suspend fun updateCategory(category: CategoryEntity)
    
    @Delete
    suspend fun deleteCategory(category: CategoryEntity)
    
    @Query("DELETE FROM categories WHERE id = :id")
    suspend fun deleteCategoryById(id: Long)
    
    @Query("SELECT * FROM categories WHERE parentCategoryId = :parentId")
    suspend fun getSubCategories(parentId: Long): List<CategoryEntity>
    
    @Query("SELECT * FROM categories WHERE parentCategoryId IS NULL")
    suspend fun getParentCategories(): List<CategoryEntity>
    
    @Query("SELECT * FROM categories WHERE name LIKE '%' || :name || '%'")
    suspend fun searchCategoriesByName(name: String): List<CategoryEntity>
    
    @Query("SELECT COUNT(*) FROM categories WHERE isDefault = 0")
    suspend fun getCustomCategoryCount(): Int
    
    // Data management methods
    @Query("DELETE FROM categories WHERE isDefault = 0")
    suspend fun deleteAllCustomCategories()
}