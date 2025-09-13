package com.expensetracker.data.local.dao

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.expensetracker.data.local.database.ExpenseDatabase
import com.expensetracker.data.local.entities.CategoryEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.Assert.*

/**
 * Integration tests for CategoryDao
 */
@RunWith(AndroidJUnit4::class)
class CategoryDaoTest {
    
    private lateinit var database: ExpenseDatabase
    private lateinit var categoryDao: CategoryDao
    
    @Before
    fun setup() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            ExpenseDatabase::class.java
        ).allowMainThreadQueries().build()
        
        categoryDao = database.categoryDao()
    }
    
    @After
    fun teardown() {
        database.close()
    }
    
    @Test
    fun insertAndRetrieveCategory() = runTest {
        val category = CategoryEntity(
            name = "Food & Dining",
            icon = "restaurant",
            color = "#FF9800",
            isDefault = true,
            parentCategoryId = null
        )
        
        val categoryId = categoryDao.insertCategory(category)
        val retrievedCategory = categoryDao.getCategoryById(categoryId)
        
        assertNotNull(retrievedCategory)
        assertEquals(category.name, retrievedCategory?.name)
        assertEquals(category.icon, retrievedCategory?.icon)
        assertEquals(category.color, retrievedCategory?.color)
        assertEquals(category.isDefault, retrievedCategory?.isDefault)
    }
    
    @Test
    fun insertMultipleCategories() = runTest {
        val categories = listOf(
            CategoryEntity(
                name = "Food & Dining",
                icon = "restaurant",
                color = "#FF9800",
                isDefault = true,
                parentCategoryId = null
            ),
            CategoryEntity(
                name = "Transportation",
                icon = "directions_car",
                color = "#2196F3",
                isDefault = true,
                parentCategoryId = null
            ),
            CategoryEntity(
                name = "Custom Category",
                icon = "custom",
                color = "#4CAF50",
                isDefault = false,
                parentCategoryId = null
            )
        )
        
        val categoryIds = categoryDao.insertCategories(categories)
        assertEquals(3, categoryIds.size)
        
        val allCategories = categoryDao.getAllCategories()
        assertEquals(3, allCategories.size)
    }
    
    @Test
    fun getDefaultCategories() = runTest {
        val categories = listOf(
            CategoryEntity(
                name = "Food & Dining",
                icon = "restaurant",
                color = "#FF9800",
                isDefault = true,
                parentCategoryId = null
            ),
            CategoryEntity(
                name = "Custom Category",
                icon = "custom",
                color = "#4CAF50",
                isDefault = false,
                parentCategoryId = null
            )
        )
        
        categories.forEach { categoryDao.insertCategory(it) }
        
        val defaultCategories = categoryDao.getDefaultCategories()
        assertEquals(1, defaultCategories.size)
        assertEquals("Food & Dining", defaultCategories[0].name)
    }
    
    @Test
    fun observeDefaultCategories() = runTest {
        val categories = listOf(
            CategoryEntity(
                name = "Food & Dining",
                icon = "restaurant",
                color = "#FF9800",
                isDefault = true,
                parentCategoryId = null
            ),
            CategoryEntity(
                name = "Transportation",
                icon = "directions_car",
                color = "#2196F3",
                isDefault = true,
                parentCategoryId = null
            ),
            CategoryEntity(
                name = "Custom Category",
                icon = "custom",
                color = "#4CAF50",
                isDefault = false,
                parentCategoryId = null
            )
        )
        
        categories.forEach { categoryDao.insertCategory(it) }
        
        val defaultCategories = categoryDao.observeDefaultCategories().first()
        assertEquals(2, defaultCategories.size)
    }
    
    @Test
    fun getSubCategories() = runTest {
        // Insert parent category
        val parentCategory = CategoryEntity(
            name = "Food & Dining",
            icon = "restaurant",
            color = "#FF9800",
            isDefault = true,
            parentCategoryId = null
        )
        val parentId = categoryDao.insertCategory(parentCategory)
        
        // Insert sub-categories
        val subCategories = listOf(
            CategoryEntity(
                name = "Fast Food",
                icon = "fastfood",
                color = "#FF5722",
                isDefault = false,
                parentCategoryId = parentId
            ),
            CategoryEntity(
                name = "Restaurants",
                icon = "restaurant_menu",
                color = "#FF7043",
                isDefault = false,
                parentCategoryId = parentId
            )
        )
        
        subCategories.forEach { categoryDao.insertCategory(it) }
        
        val retrievedSubCategories = categoryDao.getSubCategories(parentId)
        assertEquals(2, retrievedSubCategories.size)
    }
    
    @Test
    fun getParentCategories() = runTest {
        // Insert parent category
        val parentCategory = CategoryEntity(
            name = "Food & Dining",
            icon = "restaurant",
            color = "#FF9800",
            isDefault = true,
            parentCategoryId = null
        )
        val parentId = categoryDao.insertCategory(parentCategory)
        
        // Insert sub-category
        val subCategory = CategoryEntity(
            name = "Fast Food",
            icon = "fastfood",
            color = "#FF5722",
            isDefault = false,
            parentCategoryId = parentId
        )
        categoryDao.insertCategory(subCategory)
        
        val parentCategories = categoryDao.getParentCategories()
        assertEquals(1, parentCategories.size)
        assertEquals("Food & Dining", parentCategories[0].name)
    }
    
    @Test
    fun searchCategoriesByName() = runTest {
        val categories = listOf(
            CategoryEntity(
                name = "Food & Dining",
                icon = "restaurant",
                color = "#FF9800",
                isDefault = true,
                parentCategoryId = null
            ),
            CategoryEntity(
                name = "Fast Food",
                icon = "fastfood",
                color = "#FF5722",
                isDefault = false,
                parentCategoryId = null
            ),
            CategoryEntity(
                name = "Transportation",
                icon = "directions_car",
                color = "#2196F3",
                isDefault = true,
                parentCategoryId = null
            )
        )
        
        categories.forEach { categoryDao.insertCategory(it) }
        
        val foodCategories = categoryDao.searchCategoriesByName("Food")
        assertEquals(2, foodCategories.size)
        
        val transportCategories = categoryDao.searchCategoriesByName("Transport")
        assertEquals(1, transportCategories.size)
    }
    
    @Test
    fun updateCategory() = runTest {
        val category = CategoryEntity(
            name = "Original Name",
            icon = "original_icon",
            color = "#000000",
            isDefault = false,
            parentCategoryId = null
        )
        
        val categoryId = categoryDao.insertCategory(category)
        
        val updatedCategory = category.copy(
            id = categoryId,
            name = "Updated Name",
            icon = "updated_icon",
            color = "#FFFFFF"
        )
        
        categoryDao.updateCategory(updatedCategory)
        
        val retrievedCategory = categoryDao.getCategoryById(categoryId)
        assertEquals("Updated Name", retrievedCategory?.name)
        assertEquals("updated_icon", retrievedCategory?.icon)
        assertEquals("#FFFFFF", retrievedCategory?.color)
    }
    
    @Test
    fun deleteCategory() = runTest {
        val category = CategoryEntity(
            name = "Test Category",
            icon = "test_icon",
            color = "#FF0000",
            isDefault = false,
            parentCategoryId = null
        )
        
        val categoryId = categoryDao.insertCategory(category)
        
        // Verify category exists
        assertNotNull(categoryDao.getCategoryById(categoryId))
        
        // Delete category
        categoryDao.deleteCategoryById(categoryId)
        
        // Verify category is deleted
        assertNull(categoryDao.getCategoryById(categoryId))
    }
    
    @Test
    fun getCustomCategoryCount() = runTest {
        val categories = listOf(
            CategoryEntity(
                name = "Default Category",
                icon = "default",
                color = "#FF9800",
                isDefault = true,
                parentCategoryId = null
            ),
            CategoryEntity(
                name = "Custom Category 1",
                icon = "custom1",
                color = "#FF5722",
                isDefault = false,
                parentCategoryId = null
            ),
            CategoryEntity(
                name = "Custom Category 2",
                icon = "custom2",
                color = "#4CAF50",
                isDefault = false,
                parentCategoryId = null
            )
        )
        
        categories.forEach { categoryDao.insertCategory(it) }
        
        val customCount = categoryDao.getCustomCategoryCount()
        assertEquals(2, customCount)
    }
}