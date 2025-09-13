package com.expensetracker.data.categorization

import com.expensetracker.domain.categorization.KeywordCategorizer
import com.expensetracker.domain.categorization.MerchantCategorizer
import com.expensetracker.domain.model.*
import com.expensetracker.domain.repository.CategoryRepository
import com.expensetracker.domain.repository.CategorizationRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal
import java.time.LocalDateTime
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class SmartTransactionCategorizerTest {
    
    private lateinit var keywordCategorizer: KeywordCategorizer
    private lateinit var merchantCategorizer: MerchantCategorizer
    private lateinit var categoryRepository: CategoryRepository
    private lateinit var categorizationRepository: CategorizationRepository
    private lateinit var smartCategorizer: SmartTransactionCategorizer
    
    private val testCategories = listOf(
        Category(1L, "Food & Dining", "restaurant", "#FF9800", true),
        Category(2L, "Shopping", "shopping_cart", "#2196F3", true),
        Category(3L, "Transportation", "directions_car", "#4CAF50", true),
        Category(10L, "Uncategorized", "help_outline", "#9E9E9E", true)
    )
    
    private val testTransaction = Transaction(
        id = 1L,
        amount = BigDecimal("100.00"),
        type = TransactionType.EXPENSE,
        category = testCategories[0],
        merchant = "Test Merchant",
        description = "Test transaction",
        date = LocalDateTime.now(),
        source = TransactionSource.SMS_AUTO,
        accountId = 1L
    )
    
    @Before
    fun setup() {
        keywordCategorizer = mockk()
        merchantCategorizer = mockk()
        categoryRepository = mockk()
        categorizationRepository = mockk()
        smartCategorizer = SmartTransactionCategorizer(
            keywordCategorizer,
            merchantCategorizer,
            categoryRepository,
            categorizationRepository
        )
    }
    
    @Test
    fun `categorizeTransaction should prioritize user-defined rules`() = runTest {
        // Given
        val userRule = CategoryRule(
            id = 1L,
            merchantPattern = "Test Merchant",
            categoryId = 1L,
            confidence = 0.9f,
            isUserDefined = true,
            usageCount = 5,
            lastUsed = System.currentTimeMillis()
        )
        
        coEvery { categoryRepository.getAllCategories() } returns testCategories
        coEvery { categorizationRepository.getRulesForMerchant("Test Merchant") } returns listOf(userRule)
        coEvery { categorizationRepository.incrementRuleUsage(1L) } returns Unit
        
        // When
        val result = smartCategorizer.categorizeTransaction(testTransaction)
        
        // Then
        assertEquals("Food & Dining", result.category.name)
        assertEquals(0.9f, result.confidence)
        assertEquals(CategorizationReason.UserRule, result.reason)
        coVerify { categorizationRepository.incrementRuleUsage(1L) }
    }
    
    @Test
    fun `categorizeTransaction should use merchant categorizer when no user rules`() = runTest {
        // Given
        val merchantResult = CategorizationResult(
            category = testCategories[1],
            confidence = 0.85f,
            reason = CategorizationReason.MerchantHistory
        )
        
        coEvery { categoryRepository.getAllCategories() } returns testCategories
        coEvery { categorizationRepository.getRulesForMerchant("Test Merchant") } returns emptyList()
        coEvery { merchantCategorizer.categorizeByMerchant("Test Merchant", testCategories) } returns merchantResult
        
        // When
        val result = smartCategorizer.categorizeTransaction(testTransaction)
        
        // Then
        assertEquals("Shopping", result.category.name)
        assertEquals(0.85f, result.confidence)
        assertEquals(CategorizationReason.MerchantHistory, result.reason)
    }
    
    @Test
    fun `categorizeTransaction should use keyword categorizer when merchant fails`() = runTest {
        // Given
        val keywordResult = CategorizationResult(
            category = testCategories[2],
            confidence = 0.7f,
            reason = CategorizationReason.KeywordMatch
        )
        
        coEvery { categoryRepository.getAllCategories() } returns testCategories
        coEvery { categorizationRepository.getRulesForMerchant("Test Merchant") } returns emptyList()
        coEvery { merchantCategorizer.categorizeByMerchant("Test Merchant", testCategories) } returns null
        coEvery { keywordCategorizer.categorizeByKeywords("Test Merchant", testCategories) } returns keywordResult
        
        // When
        val result = smartCategorizer.categorizeTransaction(testTransaction)
        
        // Then
        assertEquals("Transportation", result.category.name)
        assertEquals(0.7f, result.confidence)
        assertEquals(CategorizationReason.KeywordMatch, result.reason)
    }
    
    @Test
    fun `categorizeTransaction should fallback to uncategorized`() = runTest {
        // Given
        coEvery { categoryRepository.getAllCategories() } returns testCategories
        coEvery { categorizationRepository.getRulesForMerchant("Test Merchant") } returns emptyList()
        coEvery { merchantCategorizer.categorizeByMerchant("Test Merchant", testCategories) } returns null
        coEvery { keywordCategorizer.categorizeByKeywords("Test Merchant", testCategories) } returns null
        coEvery { categoryRepository.getUncategorizedCategory() } returns testCategories[3]
        
        // When
        val result = smartCategorizer.categorizeTransaction(testTransaction)
        
        // Then
        assertEquals("Uncategorized", result.category.name)
        assertEquals(0.1f, result.confidence)
        assertEquals(CategorizationReason.DefaultCategory, result.reason)
    }
    
    @Test
    fun `learnFromUserInput should create new user rule when none exists`() = runTest {
        // Given
        val userCategory = testCategories[1]
        coEvery { categorizationRepository.getRulesForMerchant("Test Merchant") } returns emptyList()
        coEvery { merchantCategorizer.updateMerchantCategory("Test Merchant", userCategory) } returns Unit
        coEvery { categorizationRepository.incrementMerchantTransactionCount("Test Merchant") } returns Unit
        coEvery { categorizationRepository.insertRule(any()) } returns 1L
        
        // When
        smartCategorizer.learnFromUserInput(testTransaction, userCategory)
        
        // Then
        coVerify { merchantCategorizer.updateMerchantCategory("Test Merchant", userCategory) }
        coVerify { categorizationRepository.insertRule(any()) }
    }
    
    @Test
    fun `learnFromUserInput should update existing user rule`() = runTest {
        // Given
        val userCategory = testCategories[1]
        val existingRule = CategoryRule(
            id = 1L,
            merchantPattern = "Test Merchant",
            categoryId = 1L,
            confidence = 0.8f,
            isUserDefined = true,
            usageCount = 3,
            lastUsed = System.currentTimeMillis() - 10000
        )
        
        coEvery { categorizationRepository.getRulesForMerchant("Test Merchant") } returns listOf(existingRule)
        coEvery { merchantCategorizer.updateMerchantCategory("Test Merchant", userCategory) } returns Unit
        coEvery { categorizationRepository.incrementMerchantTransactionCount("Test Merchant") } returns Unit
        coEvery { categorizationRepository.updateRule(any()) } returns Unit
        
        // When
        smartCategorizer.learnFromUserInput(testTransaction, userCategory)
        
        // Then
        coVerify { categorizationRepository.updateRule(any()) }
    }
    
    @Test
    fun `suggestCategories should return multiple suggestions`() = runTest {
        // Given
        val merchantResult = CategorizationResult(testCategories[0], 0.8f, CategorizationReason.MerchantHistory)
        val keywordResult = CategorizationResult(testCategories[1], 0.7f, CategorizationReason.KeywordMatch)
        
        coEvery { categoryRepository.getAllCategories() } returns testCategories
        coEvery { merchantCategorizer.categorizeByMerchant("Test Merchant", testCategories) } returns merchantResult
        coEvery { keywordCategorizer.categorizeByKeywords("Test Merchant", testCategories) } returns keywordResult
        coEvery { merchantCategorizer.findSimilarMerchants("Test Merchant") } returns emptyList()
        
        // When
        val result = smartCategorizer.suggestCategories("Test Merchant")
        
        // Then
        assertEquals(2, result.size)
        assertTrue(result.any { it.category.name == "Food & Dining" })
        assertTrue(result.any { it.category.name == "Shopping" })
    }
    
    @Test
    fun `getConfidence should return merchant confidence when available`() = runTest {
        // Given
        val merchantResult = CategorizationResult(testCategories[0], 0.85f, CategorizationReason.MerchantHistory)
        coEvery { merchantCategorizer.categorizeByMerchant("Test Merchant", listOf(testCategories[0])) } returns merchantResult
        
        // When
        val result = smartCategorizer.getConfidence("Test Merchant", testCategories[0])
        
        // Then
        assertEquals(0.85f, result)
    }
    
    @Test
    fun `getConfidence should return keyword confidence when merchant not available`() = runTest {
        // Given
        val keywordResult = CategorizationResult(testCategories[0], 0.7f, CategorizationReason.KeywordMatch)
        coEvery { merchantCategorizer.categorizeByMerchant("Test Merchant", listOf(testCategories[0])) } returns null
        coEvery { keywordCategorizer.categorizeByKeywords("Test Merchant", listOf(testCategories[0])) } returns keywordResult
        
        // When
        val result = smartCategorizer.getConfidence("Test Merchant", testCategories[0])
        
        // Then
        assertEquals(0.7f, result)
    }
    
    @Test
    fun `getConfidence should return zero when no match found`() = runTest {
        // Given
        coEvery { merchantCategorizer.categorizeByMerchant("Test Merchant", listOf(testCategories[0])) } returns null
        coEvery { keywordCategorizer.categorizeByKeywords("Test Merchant", listOf(testCategories[0])) } returns null
        
        // When
        val result = smartCategorizer.getConfidence("Test Merchant", testCategories[0])
        
        // Then
        assertEquals(0.0f, result)
    }
}