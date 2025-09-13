package com.expensetracker.domain.repository

import com.expensetracker.domain.model.Category
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for category operations
 */
interface CategoryRepository {
    
    /**
     * Get all categories
     */
    suspend fun getAllCategories(): List<Category>
    
    /**
     * Get category by ID
     */
    suspend fun getCategoryById(id: Long): Category?
    
    /**
     * Get default categories
     */
    suspend fun getDefaultCategories(): List<Category>
    
    /**
     * Get custom categories created by user
     */
    suspend fun getCustomCategories(): List<Category>
    
    /**
     * Insert new category
     */
    suspend fun insertCategory(category: Category): Long
    
    /**
     * Update existing category
     */
    suspend fun updateCategory(category: Category)
    
    /**
     * Delete category
     */
    suspend fun deleteCategory(category: Category)
    
    /**
     * Observe all categories
     */
    fun observeCategories(): Flow<List<Category>>
    
    /**
     * Initialize default categories if not exists
     */
    suspend fun initializeDefaultCategories()
    
    /**
     * Get uncategorized category (fallback)
     */
    suspend fun getUncategorizedCategory(): Category
}