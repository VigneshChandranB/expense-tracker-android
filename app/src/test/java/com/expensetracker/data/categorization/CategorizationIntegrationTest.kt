package com.expensetracker.data.categorization

import com.expensetracker.domain.model.*
import com.expensetracker.domain.repository.CategoryRepository
import com.expensetracker.domain.repository.CategorizationRepository
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal
import java.time.LocalDateTime
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Integration test for the complete categorization system
 */
class CategorizationIntegrationTest {
    
    private lateinit var categoryRepository: CategoryRepository
    private lateinit var categorizationRepository: CategorizationRepository
    private lateinit var keywordCategorizer: KeywordCategorizerImpl
    private lateinit var merchantCategorizer: MerchantCategorizerImpl
    private lateinit var smartCategorizer: SmartTransactionCategorizer
    
    private val testCategories = listOf(
        Category(1L, "Food & Dining", "restaurant", "#FF9800", true),
        Category(2L, "Shopping", "shopping_cart", "#2196F3", true),
        Category(3L, "Transportation", "directions_car", "#4CAF50", true),
        Category(10L, "Uncategorized", "help_outline", "#9E9E9E", true)
    )
    
    @Before
    fun setup() {
        categoryRepository = mockk()
        categorizationRepository = mockk()
        
        keywordCategorizer = KeywordCategorizerImpl(categorizationRepository)
        merchantCategorizer = MerchantCategorizerImpl(categorizationRepository)
        smartCategorizer = SmartTransactionCategorizer(
            keywordCategorizer,
            merchantCategorizer,
            categoryRepository,
            categorizationRepository
        )
        
        // Setup common mocks
        coEvery { categoryRepository.getAllCategories() } returns testCategories
        coEvery { categoryRepository.getUncategorizedCategory() } returns testCategories[3]
    }
    
    @Test
    fun `should categorize Amazon transaction as Shopping using keywords`() = runTest {
        // Given
        val transaction = createTransaction("Amazon")
        
        coEvery { categorizationRepository.getRulesForMerchant("Amazon") } returns emptyList()
        coEvery { categorizationRepository.getMerchantByNormalizedName("amazon") } returns null
        coEvery { categorizationRepository.findSimilarMerchants("amazon") } returns emptyList()
        coEvery { categorizationRepository.getKeywordMapping("amazon") } returns Pair(2L, true)
        coEvery { categorizationRepository.getDefaultKeywordMappings() } returns emptyMap()
        
        // When
        val result = smartCategorizer.categorizeTransaction(transaction)
        
        // Then
        assertEquals("Shopping", result.category.name)
        assertEquals(CategorizationReason.KeywordMatch, result.reason)
        assertTrue(result.confidence >= 0.8f)
    }
    
    @Test
    fun `should learn from user input and improve future categorization`() = runTest {
        // Given
        val transaction = createTransaction("Uber Eats")
        val userCategory = testCategories[0] // Food & Dining
        
        // Setup initial state - no existing rules
        coEvery { categorizationRepository.getRulesForMerchant("Uber Eats") } returns emptyList()
        coEvery { categorizationRepository.insertRule(any()) } returns 1L
        coEvery { categorizationRepository.incrementMerchantTransactionCount("Uber Eats") } returns Unit
        coEvery { categorizationRepository.getMerchantByNormalizedName("uber eats") } returns null
        coEvery { categorizationRepository.insertMerchant(any()) } returns 1L
        
        // When - learn from user input
        smartCategorizer.learnFromUserInput(transaction, userCategory)
        
        // Setup for second categorization - now with learned rule
        val userRule = CategoryRule(
            id = 1L,
            merchantPattern = "Uber Eats",
            categoryId = 1L,
            confidence = 0.9f,
            isUserDefined = true,
            usageCount = 1,
            lastUsed = System.currentTimeMillis()
        )
        coEvery { categorizationRepository.getRulesForMerchant("Uber Eats") } returns listOf(userRule)
        coEvery { categorizationRepository.incrementRuleUsage(1L) } returns Unit
        
        // When - categorize again
        val result = smartCategorizer.categorizeTransaction(transaction)
        
        // Then - should use learned rule
        assertEquals("Food & Dining", result.category.name)
        assertEquals(CategorizationReason.UserRule, result.reason)
        assertEquals(0.9f, result.confidence)
    }
    
    @Test
    fun `should use merchant history for categorization`() = runTest {
        // Given
        val transaction = createTransaction("Starbucks")
        val merchantInfo = MerchantInfo(
            name = "Starbucks",
            normalizedName = "starbucks",
            categoryId = 1L,
            confidence = 0.85f,
            transactionCount = 10
        )
        
        coEvery { categorizationRepository.getRulesForMerchant("Starbucks") } returns emptyList()
        coEvery { categorizationRepository.getMerchantByNormalizedName("starbucks") } returns merchantInfo
        
        // When
        val result = smartCategorizer.categorizeTransaction(transaction)
        
        // Then
        assertEquals("Food & Dining", result.category.name)
        assertEquals(CategorizationReason.MerchantHistory, result.reason)
        assertEquals(0.85f, result.confidence)
    }
    
    @Test
    fun `should use similar merchants for new merchant categorization`() = runTest {
        // Given
        val transaction = createTransaction("Starbucks Coffee")
        val similarMerchant = MerchantInfo(
            name = "Starbucks",
            normalizedName = "starbucks",
            categoryId = 1L,
            confidence = 0.8f,
            transactionCount = 5
        )
        
        coEvery { categorizationRepository.getRulesForMerchant("Starbucks Coffee") } returns emptyList()
        coEvery { categorizationRepository.getMerchantByNormalizedName("starbucks coffee") } returns null
        coEvery { categorizationRepository.findSimilarMerchants("starbucks coffee") } returns listOf(similarMerchant)
        
        // When
        val result = smartCategorizer.categorizeTransaction(transaction)
        
        // Then
        assertEquals("Food & Dining", result.category.name)
        assertTrue(result.reason is CategorizationReason.MachineLearning)
        assertTrue(result.confidence >= 0.6f) // Should be adjusted based on similarity
    }
    
    @Test
    fun `should provide multiple category suggestions`() = runTest {
        // Given
        val merchant = "McDonald's"
        val merchantResult = CategorizationResult(testCategories[0], 0.9f, CategorizationReason.MerchantHistory)
        val keywordResult = CategorizationResult(testCategories[0], 0.8f, CategorizationReason.KeywordMatch)
        
        coEvery { categorizationRepository.getMerchantByNormalizedName("mcdonald s") } returns MerchantInfo(
            name = merchant,
            normalizedName = "mcdonald s",
            categoryId = 1L,
            confidence = 0.9f,
            transactionCount = 15
        )
        coEvery { categorizationRepository.getKeywordMapping("mcdonald") } returns Pair(1L, true)
        coEvery { categorizationRepository.getDefaultKeywordMappings() } returns emptyMap()
        coEvery { categorizationRepository.findSimilarMerchants("mcdonald s") } returns emptyList()
        
        // When
        val suggestions = smartCategorizer.suggestCategories(merchant)
        
        // Then
        assertTrue(suggestions.isNotEmpty())
        assertTrue(suggestions.any { it.category.name == "Food & Dining" })
        assertTrue(suggestions.all { it.confidence > 0.0f })
    }
    
    @Test
    fun `should handle edge case with special characters in merchant name`() = runTest {
        // Given
        val transaction = createTransaction("McDonald's - Drive Thru!!!")
        
        coEvery { categorizationRepository.getRulesForMerchant("McDonald's - Drive Thru!!!") } returns emptyList()
        coEvery { categorizationRepository.getMerchantByNormalizedName("mcdonald s drive thru") } returns null
        coEvery { categorizationRepository.findSimilarMerchants("mcdonald s drive thru") } returns emptyList()
        coEvery { categorizationRepository.getKeywordMapping("mcdonald") } returns Pair(1L, true)
        coEvery { categorizationRepository.getDefaultKeywordMappings() } returns emptyMap()
        
        // When
        val result = smartCategorizer.categorizeTransaction(transaction)
        
        // Then
        assertEquals("Food & Dining", result.category.name)
        assertEquals(CategorizationReason.KeywordMatch, result.reason)
    }
    
    @Test
    fun `should fallback to uncategorized when no matches found`() = runTest {
        // Given
        val transaction = createTransaction("Unknown Merchant XYZ")
        
        coEvery { categorizationRepository.getRulesForMerchant("Unknown Merchant XYZ") } returns emptyList()
        coEvery { categorizationRepository.getMerchantByNormalizedName("unknown merchant xyz") } returns null
        coEvery { categorizationRepository.findSimilarMerchants("unknown merchant xyz") } returns emptyList()
        coEvery { categorizationRepository.getKeywordMapping(any()) } returns null
        coEvery { categorizationRepository.getDefaultKeywordMappings() } returns emptyMap()
        
        // When
        val result = smartCategorizer.categorizeTransaction(transaction)
        
        // Then
        assertEquals("Uncategorized", result.category.name)
        assertEquals(CategorizationReason.DefaultCategory, result.reason)
        assertEquals(0.1f, result.confidence)
    }
    
    private fun createTransaction(merchant: String): Transaction {
        return Transaction(
            id = 1L,
            amount = BigDecimal("50.00"),
            type = TransactionType.EXPENSE,
            category = testCategories[3], // Will be recategorized
            merchant = merchant,
            description = "Test transaction",
            date = LocalDateTime.now(),
            source = TransactionSource.SMS_AUTO,
            accountId = 1L
        )
    }
}