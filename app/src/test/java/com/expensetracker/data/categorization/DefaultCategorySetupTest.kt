package com.expensetracker.data.categorization

import com.expensetracker.domain.repository.CategoryRepository
import com.expensetracker.domain.repository.CategorizationRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DefaultCategorySetupTest {
    
    private lateinit var categoryRepository: CategoryRepository
    private lateinit var categorizationRepository: CategorizationRepository
    private lateinit var defaultCategorySetup: DefaultCategorySetup
    
    @Before
    fun setup() {
        categoryRepository = mockk()
        categorizationRepository = mockk()
        defaultCategorySetup = DefaultCategorySetup(categoryRepository, categorizationRepository)
    }
    
    @Test
    fun `initializeDefaultCategories should initialize both categories and keywords`() = runTest {
        // Given
        coEvery { categoryRepository.initializeDefaultCategories() } returns Unit
        coEvery { categorizationRepository.initializeDefaultKeywords() } returns Unit
        
        // When
        defaultCategorySetup.initializeDefaultCategories()
        
        // Then
        coVerify { categoryRepository.initializeDefaultCategories() }
        coVerify { categorizationRepository.initializeDefaultKeywords() }
    }
    
    @Test
    fun `DEFAULT_CATEGORIES should contain all required categories`() {
        // Given
        val categories = DefaultCategorySetup.DEFAULT_CATEGORIES
        
        // Then
        assertEquals(10, categories.size)
        
        val categoryNames = categories.map { it.name }
        assertTrue(categoryNames.contains("Food & Dining"))
        assertTrue(categoryNames.contains("Shopping"))
        assertTrue(categoryNames.contains("Transportation"))
        assertTrue(categoryNames.contains("Bills & Utilities"))
        assertTrue(categoryNames.contains("Entertainment"))
        assertTrue(categoryNames.contains("Healthcare"))
        assertTrue(categoryNames.contains("Investment"))
        assertTrue(categoryNames.contains("Income"))
        assertTrue(categoryNames.contains("Transfer"))
        assertTrue(categoryNames.contains("Uncategorized"))
        
        // All should be default categories
        assertTrue(categories.all { it.isDefault })
    }
    
    @Test
    fun `DEFAULT_KEYWORD_MAPPINGS should contain comprehensive keyword mappings`() {
        // Given
        val mappings = DefaultCategorySetup.DEFAULT_KEYWORD_MAPPINGS
        
        // Then
        assertTrue(mappings.isNotEmpty())
        
        // Check Food & Dining keywords (categoryId = 1L)
        val foodKeywords = mappings.filterValues { it == 1L }.keys
        assertTrue(foodKeywords.contains("restaurant"))
        assertTrue(foodKeywords.contains("swiggy"))
        assertTrue(foodKeywords.contains("zomato"))
        assertTrue(foodKeywords.contains("mcdonalds"))
        
        // Check Shopping keywords (categoryId = 2L)
        val shoppingKeywords = mappings.filterValues { it == 2L }.keys
        assertTrue(shoppingKeywords.contains("amazon"))
        assertTrue(shoppingKeywords.contains("flipkart"))
        assertTrue(shoppingKeywords.contains("myntra"))
        
        // Check Transportation keywords (categoryId = 3L)
        val transportKeywords = mappings.filterValues { it == 3L }.keys
        assertTrue(transportKeywords.contains("uber"))
        assertTrue(transportKeywords.contains("ola"))
        assertTrue(transportKeywords.contains("fuel"))
        
        // Check Bills & Utilities keywords (categoryId = 4L)
        val billsKeywords = mappings.filterValues { it == 4L }.keys
        assertTrue(billsKeywords.contains("electricity"))
        assertTrue(billsKeywords.contains("internet"))
        assertTrue(billsKeywords.contains("rent"))
        
        // Check Entertainment keywords (categoryId = 5L)
        val entertainmentKeywords = mappings.filterValues { it == 5L }.keys
        assertTrue(entertainmentKeywords.contains("netflix"))
        assertTrue(entertainmentKeywords.contains("movie"))
        assertTrue(entertainmentKeywords.contains("spotify"))
        
        // Check Healthcare keywords (categoryId = 6L)
        val healthKeywords = mappings.filterValues { it == 6L }.keys
        assertTrue(healthKeywords.contains("hospital"))
        assertTrue(healthKeywords.contains("pharmacy"))
        assertTrue(healthKeywords.contains("doctor"))
        
        // Check Investment keywords (categoryId = 7L)
        val investmentKeywords = mappings.filterValues { it == 7L }.keys
        assertTrue(investmentKeywords.contains("mutual"))
        assertTrue(investmentKeywords.contains("zerodha"))
        assertTrue(investmentKeywords.contains("sip"))
        
        // Check Income keywords (categoryId = 8L)
        val incomeKeywords = mappings.filterValues { it == 8L }.keys
        assertTrue(incomeKeywords.contains("salary"))
        assertTrue(incomeKeywords.contains("bonus"))
        assertTrue(incomeKeywords.contains("refund"))
        
        // Check Transfer keywords (categoryId = 9L)
        val transferKeywords = mappings.filterValues { it == 9L }.keys
        assertTrue(transferKeywords.contains("upi"))
        assertTrue(transferKeywords.contains("paytm"))
        assertTrue(transferKeywords.contains("gpay"))
    }
    
    @Test
    fun `DEFAULT_CATEGORIES should have unique IDs`() {
        // Given
        val categories = DefaultCategorySetup.DEFAULT_CATEGORIES
        
        // Then
        val ids = categories.map { it.id }
        assertEquals(ids.size, ids.distinct().size, "Category IDs should be unique")
    }
    
    @Test
    fun `DEFAULT_CATEGORIES should have unique names`() {
        // Given
        val categories = DefaultCategorySetup.DEFAULT_CATEGORIES
        
        // Then
        val names = categories.map { it.name }
        assertEquals(names.size, names.distinct().size, "Category names should be unique")
    }
    
    @Test
    fun `DEFAULT_KEYWORD_MAPPINGS should have valid category IDs`() {
        // Given
        val mappings = DefaultCategorySetup.DEFAULT_KEYWORD_MAPPINGS
        val validCategoryIds = DefaultCategorySetup.DEFAULT_CATEGORIES.map { it.id }.toSet()
        
        // Then
        val mappingCategoryIds = mappings.values.toSet()
        assertTrue(
            validCategoryIds.containsAll(mappingCategoryIds),
            "All keyword mappings should reference valid category IDs"
        )
    }
    
    @Test
    fun `DEFAULT_KEYWORD_MAPPINGS should not have duplicate keywords`() {
        // Given
        val mappings = DefaultCategorySetup.DEFAULT_KEYWORD_MAPPINGS
        
        // Then
        val keywords = mappings.keys.toList()
        assertEquals(keywords.size, keywords.distinct().size, "Keywords should be unique")
    }
    
    @Test
    fun `DEFAULT_CATEGORIES should have proper color format`() {
        // Given
        val categories = DefaultCategorySetup.DEFAULT_CATEGORIES
        
        // Then
        categories.forEach { category ->
            assertTrue(
                category.color.matches(Regex("^#[0-9A-Fa-f]{6}$")),
                "Category ${category.name} should have valid hex color format"
            )
        }
    }
}