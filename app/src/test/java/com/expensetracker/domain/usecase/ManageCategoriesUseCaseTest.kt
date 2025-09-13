package com.expensetracker.domain.usecase

import com.expensetracker.domain.categorization.KeywordCategorizer
import com.expensetracker.domain.model.Category
import com.expensetracker.domain.repository.CategoryRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals

class ManageCategoriesUseCaseTest {
    
    private lateinit var categoryRepository: CategoryRepository
    private lateinit var keywordCategorizer: KeywordCategorizer
    private lateinit var useCase: ManageCategoriesUseCase
    
    private val testCategories = listOf(
        Category(1L, "Food & Dining", "restaurant", "#FF9800", true),
        Category(2L, "Shopping", "shopping_cart", "#2196F3", true),
        Category(3L, "Custom Category", "custom", "#123456", false)
    )
    
    @Before
    fun setup() {
        categoryRepository = mockk()
        keywordCategorizer = mockk()
        useCase = ManageCategoriesUseCase(categoryRepository, keywordCategorizer)
    }
    
    @Test
    fun `getAllCategories should return categories from repository`() = runTest {
        // Given
        coEvery { categoryRepository.getAllCategories() } returns testCategories
        
        // When
        val result = useCase.getAllCategories()
        
        // Then
        assertEquals(testCategories, result)
        coVerify { categoryRepository.getAllCategories() }
    }
    
    @Test
    fun `observeCategories should return flow from repository`() = runTest {
        // Given
        val categoriesFlow = flowOf(testCategories)
        coEvery { categoryRepository.observeCategories() } returns categoriesFlow
        
        // When
        val result = useCase.observeCategories()
        
        // Then
        assertEquals(categoriesFlow, result)
        coVerify { categoryRepository.observeCategories() }
    }
    
    @Test
    fun `createCategory should create new custom category`() = runTest {
        // Given
        val expectedId = 4L
        coEvery { categoryRepository.insertCategory(any()) } returns expectedId
        
        // When
        val result = useCase.createCategory("New Category", "new_icon", "#ABCDEF")
        
        // Then
        assertEquals(expectedId, result)
        coVerify { 
            categoryRepository.insertCategory(
                match { category ->
                    category.name == "New Category" &&
                    category.icon == "new_icon" &&
                    category.color == "#ABCDEF" &&
                    !category.isDefault
                }
            )
        }
    }
    
    @Test
    fun `createCategory should support parent category`() = runTest {
        // Given
        val parentCategory = testCategories[0]
        val expectedId = 4L
        coEvery { categoryRepository.insertCategory(any()) } returns expectedId
        
        // When
        val result = useCase.createCategory("Sub Category", "sub_icon", "#FEDCBA", parentCategory)
        
        // Then
        assertEquals(expectedId, result)
        coVerify { 
            categoryRepository.insertCategory(
                match { category ->
                    category.parentCategory == parentCategory
                }
            )
        }
    }
    
    @Test
    fun `updateCategory should delegate to repository`() = runTest {
        // Given
        val category = testCategories[2]
        coEvery { categoryRepository.updateCategory(category) } returns Unit
        
        // When
        useCase.updateCategory(category)
        
        // Then
        coVerify { categoryRepository.updateCategory(category) }
    }
    
    @Test
    fun `deleteCategory should only delete custom categories`() = runTest {
        // Given
        val customCategory = testCategories[2] // isDefault = false
        val defaultCategory = testCategories[0] // isDefault = true
        coEvery { categoryRepository.deleteCategory(any()) } returns Unit
        
        // When
        useCase.deleteCategory(customCategory)
        useCase.deleteCategory(defaultCategory)
        
        // Then
        coVerify(exactly = 1) { categoryRepository.deleteCategory(customCategory) }
        coVerify(exactly = 0) { categoryRepository.deleteCategory(defaultCategory) }
    }
    
    @Test
    fun `addKeywordToCategory should delegate to keyword categorizer`() = runTest {
        // Given
        val keyword = "restaurant"
        val category = testCategories[0]
        coEvery { keywordCategorizer.addKeywordMapping(keyword, category) } returns Unit
        
        // When
        useCase.addKeywordToCategory(keyword, category)
        
        // Then
        coVerify { keywordCategorizer.addKeywordMapping(keyword, category) }
    }
    
    @Test
    fun `removeKeyword should delegate to keyword categorizer`() = runTest {
        // Given
        val keyword = "restaurant"
        coEvery { keywordCategorizer.removeKeywordMapping(keyword) } returns Unit
        
        // When
        useCase.removeKeyword(keyword)
        
        // Then
        coVerify { keywordCategorizer.removeKeywordMapping(keyword) }
    }
    
    @Test
    fun `getKeywordsForCategory should delegate to keyword categorizer`() = runTest {
        // Given
        val category = testCategories[0]
        val expectedKeywords = listOf("restaurant", "food", "dining")
        coEvery { keywordCategorizer.getKeywordsForCategory(category) } returns expectedKeywords
        
        // When
        val result = useCase.getKeywordsForCategory(category)
        
        // Then
        assertEquals(expectedKeywords, result)
        coVerify { keywordCategorizer.getKeywordsForCategory(category) }
    }
    
    @Test
    fun `initializeDefaultCategories should delegate to repository`() = runTest {
        // Given
        coEvery { categoryRepository.initializeDefaultCategories() } returns Unit
        
        // When
        useCase.initializeDefaultCategories()
        
        // Then
        coVerify { categoryRepository.initializeDefaultCategories() }
    }
}