package com.expensetracker.data.categorization

import com.expensetracker.domain.model.Category
import com.expensetracker.domain.model.CategorizationReason
import com.expensetracker.domain.repository.CategorizationRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class KeywordCategorizerTest {
    
    private lateinit var categorizationRepository: CategorizationRepository
    private lateinit var keywordCategorizer: KeywordCategorizerImpl
    
    private val testCategories = listOf(
        Category(1L, "Food & Dining", "restaurant", "#FF9800", true),
        Category(2L, "Shopping", "shopping_cart", "#2196F3", true),
        Category(3L, "Transportation", "directions_car", "#4CAF50", true)
    )
    
    @Before
    fun setup() {
        categorizationRepository = mockk()
        keywordCategorizer = KeywordCategorizerImpl(categorizationRepository)
    }
    
    @Test
    fun `categorizeByKeywords should return correct category for exact keyword match`() = runTest {
        // Given
        val merchant = "Amazon Shopping"
        coEvery { categorizationRepository.getKeywordMapping("amazon") } returns Pair(2L, true)
        coEvery { categorizationRepository.getDefaultKeywordMappings() } returns emptyMap()
        
        // When
        val result = keywordCategorizer.categorizeByKeywords(merchant, testCategories)
        
        // Then
        assertNotNull(result)
        assertEquals("Shopping", result.category.name)
        assertEquals(0.8f, result.confidence)
        assertEquals(CategorizationReason.KeywordMatch, result.reason)
    }
    
    @Test
    fun `categorizeByKeywords should return null for no keyword match`() = runTest {
        // Given
        val merchant = "Unknown Merchant"
        coEvery { categorizationRepository.getKeywordMapping(any()) } returns null
        coEvery { categorizationRepository.getDefaultKeywordMappings() } returns emptyMap()
        
        // When
        val result = keywordCategorizer.categorizeByKeywords(merchant, testCategories)
        
        // Then
        assertNull(result)
    }
    
    @Test
    fun `categorizeByKeywords should handle partial matches with lower confidence`() = runTest {
        // Given
        val merchant = "Uber Eats Food Delivery"
        coEvery { categorizationRepository.getKeywordMapping(any()) } returns null
        coEvery { categorizationRepository.getDefaultKeywordMappings() } returns mapOf(
            "food" to 1L,
            "uber" to 3L
        )
        
        // When
        val result = keywordCategorizer.categorizeByKeywords(merchant, testCategories)
        
        // Then
        assertNotNull(result)
        assertEquals("Food & Dining", result.category.name)
        assertEquals(0.56f, result.confidence, 0.01f) // 0.8 * 0.7
        assertEquals(CategorizationReason.KeywordMatch, result.reason)
    }
    
    @Test
    fun `categorizeByKeywords should normalize merchant name correctly`() = runTest {
        // Given
        val merchant = "AMAZON.COM - Shopping!!!"
        coEvery { categorizationRepository.getKeywordMapping("amazon") } returns Pair(2L, true)
        coEvery { categorizationRepository.getDefaultKeywordMappings() } returns emptyMap()
        
        // When
        val result = keywordCategorizer.categorizeByKeywords(merchant, testCategories)
        
        // Then
        assertNotNull(result)
        assertEquals("Shopping", result.category.name)
    }
    
    @Test
    fun `addKeywordMapping should add normalized keyword`() = runTest {
        // Given
        val keyword = "  RESTAURANT  "
        val category = testCategories[0]
        coEvery { categorizationRepository.addKeywordMapping(any(), any(), any()) } returns Unit
        
        // When
        keywordCategorizer.addKeywordMapping(keyword, category)
        
        // Then
        coVerify { categorizationRepository.addKeywordMapping("restaurant", 1L, false) }
    }
    
    @Test
    fun `removeKeywordMapping should remove normalized keyword`() = runTest {
        // Given
        val keyword = "  RESTAURANT  "
        coEvery { categorizationRepository.removeKeywordMapping(any()) } returns Unit
        
        // When
        keywordCategorizer.removeKeywordMapping(keyword)
        
        // Then
        coVerify { categorizationRepository.removeKeywordMapping("restaurant") }
    }
    
    @Test
    fun `getKeywordsForCategory should return keywords for category`() = runTest {
        // Given
        val category = testCategories[0]
        val expectedKeywords = listOf("restaurant", "food", "dining")
        coEvery { categorizationRepository.getKeywordsForCategory(1L) } returns expectedKeywords
        
        // When
        val result = keywordCategorizer.getKeywordsForCategory(category)
        
        // Then
        assertEquals(expectedKeywords, result)
    }
    
    @Test
    fun `categorizeByKeywords should filter out short words`() = runTest {
        // Given
        val merchant = "A B CD Restaurant"
        coEvery { categorizationRepository.getKeywordMapping("cd") } returns null
        coEvery { categorizationRepository.getKeywordMapping("restaurant") } returns Pair(1L, true)
        coEvery { categorizationRepository.getDefaultKeywordMappings() } returns emptyMap()
        
        // When
        val result = keywordCategorizer.categorizeByKeywords(merchant, testCategories)
        
        // Then
        assertNotNull(result)
        assertEquals("Food & Dining", result.category.name)
        coVerify(exactly = 0) { categorizationRepository.getKeywordMapping("a") }
        coVerify(exactly = 0) { categorizationRepository.getKeywordMapping("b") }
    }
}