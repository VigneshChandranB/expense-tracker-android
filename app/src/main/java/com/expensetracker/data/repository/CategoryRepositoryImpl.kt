package com.expensetracker.data.repository

import com.expensetracker.data.categorization.DefaultCategorySetup
import com.expensetracker.data.local.dao.CategoryDao
import com.expensetracker.data.local.entities.CategoryEntity
import com.expensetracker.domain.model.Category
import com.expensetracker.domain.repository.CategoryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of CategoryRepository
 */
@Singleton
class CategoryRepositoryImpl @Inject constructor(
    private val categoryDao: CategoryDao
) : CategoryRepository {
    
    override suspend fun getAllCategories(): List<Category> {
        return categoryDao.getAllCategories().map { it.toDomainModel() }
    }
    
    override suspend fun getCategoryById(id: Long): Category? {
        return categoryDao.getCategoryById(id)?.toDomainModel()
    }
    
    override suspend fun getDefaultCategories(): List<Category> {
        return categoryDao.getDefaultCategories().map { it.toDomainModel() }
    }
    
    override suspend fun getCustomCategories(): List<Category> {
        return categoryDao.getAllCategories()
            .filter { !it.isDefault }
            .map { it.toDomainModel() }
    }
    
    override suspend fun insertCategory(category: Category): Long {
        return categoryDao.insertCategory(category.toEntity())
    }
    
    override suspend fun updateCategory(category: Category) {
        categoryDao.updateCategory(category.toEntity())
    }
    
    override suspend fun deleteCategory(category: Category) {
        categoryDao.deleteCategory(category.toEntity())
    }
    
    override fun observeCategories(): Flow<List<Category>> {
        return categoryDao.observeAllCategories().map { entities ->
            entities.map { it.toDomainModel() }
        }
    }
    
    override suspend fun initializeDefaultCategories() {
        val existingCategories = categoryDao.getAllCategories()
        if (existingCategories.isEmpty()) {
            val defaultCategories = DefaultCategorySetup.DEFAULT_CATEGORIES.map { it.toEntity() }
            categoryDao.insertCategories(defaultCategories)
        }
    }
    
    override suspend fun getUncategorizedCategory(): Category {
        return categoryDao.getCategoryById(10L)?.toDomainModel() 
            ?: Category(10L, "Uncategorized", "help_outline", "#9E9E9E", true)
    }
    
    private fun CategoryEntity.toDomainModel(): Category {
        return Category(
            id = id,
            name = name,
            icon = icon,
            color = color,
            isDefault = isDefault,
            parentCategory = null // TODO: Implement parent category lookup if needed
        )
    }
    
    private fun Category.toEntity(): CategoryEntity {
        return CategoryEntity(
            id = id,
            name = name,
            icon = icon,
            color = color,
            isDefault = isDefault,
            parentCategoryId = parentCategory?.id
        )
    }
}