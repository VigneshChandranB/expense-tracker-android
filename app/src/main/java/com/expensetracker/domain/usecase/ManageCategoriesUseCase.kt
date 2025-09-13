package com.expensetracker.domain.usecase

import com.expensetracker.domain.categorization.KeywordCategorizer
import com.expensetracker.domain.model.Category
import com.expensetracker.domain.repository.CategoryRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Use case for managing categories and keyword mappings
 */
@Singleton
class ManageCategoriesUseCase @Inject constructor(
    private val categoryRepository: CategoryRepository,
    private val keywordCategorizer: KeywordCategorizer
) {
    
    /**
     * Get all categories
     */
    suspend fun getAllCategories(): List<Category> {
        return categoryRepository.getAllCategories()
    }
    
    /**
     * Observe all categories
     */
    fun observeCategories(): Flow<List<Category>> {
        return categoryRepository.observeCategories()
    }
    
    /**
     * Create a new custom category
     */
    suspend fun createCategory(
        name: String,
        icon: String,
        color: String,
        parentCategory: Category? = null
    ): Long {
        val category = Category(
            id = 0,
            name = name,
            icon = icon,
            color = color,
            isDefault = false,
            parentCategory = parentCategory
        )
        return categoryRepository.insertCategory(category)
    }
    
    /**
     * Update an existing category
     */
    suspend fun updateCategory(category: Category) {
        categoryRepository.updateCategory(category)
    }
    
    /**
     * Delete a category (only custom categories)
     */
    suspend fun deleteCategory(category: Category) {
        if (!category.isDefault) {
            categoryRepository.deleteCategory(category)
        }
    }
    
    /**
     * Add keyword mapping to a category
     */
    suspend fun addKeywordToCategory(keyword: String, category: Category) {
        keywordCategorizer.addKeywordMapping(keyword, category)
    }
    
    /**
     * Remove keyword mapping
     */
    suspend fun removeKeyword(keyword: String) {
        keywordCategorizer.removeKeywordMapping(keyword)
    }
    
    /**
     * Get keywords for a category
     */
    suspend fun getKeywordsForCategory(category: Category): List<String> {
        return keywordCategorizer.getKeywordsForCategory(category)
    }
    
    /**
     * Initialize default categories if needed
     */
    suspend fun initializeDefaultCategories() {
        categoryRepository.initializeDefaultCategories()
    }
}