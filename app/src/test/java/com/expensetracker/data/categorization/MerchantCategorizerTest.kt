package com.expensetracker.data.categorization

import com.expensetracker.domain.model.Category
import com.expensetracker.domain.model.CategorizationReason
import com.expensetracker.domain.model.MerchantInfo
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
import kotlin.test.assertTrue

class MerchantCategorizerTest {
    
    private lateinit var categorizationRepository: CategorizationRepository
    private lateinit var merchantCategorizer: MerchantCategorizerImpl
    
    private val testCategories = listOf(
        Category(1L, "Food & Dining", "restaurant", "#FF9800", true),
        Category(2L, "Shopping", "shopping_cart", "#2196F3", true),
        Category(3L, "Transportation", "directions_car", "#4CAF50", true)
    )
    
    @Before
    fun setup() {
        categorizationRepository = mockk()
        merchantCategorizer = MerchantCategorizerImpl(categorizationRepository)
    }
    
    @Test
    fun `categorizeByMerchant should return exact match with high confidence`() = runTest {
        // Given
        val merchant = "Amazon"
        val normalizedMerchant = "amazon"
        val merchantInfo = MerchantInfo(
            name = merchant,
            normalizedName = normalizedMerchant,
            categoryId = 2L,
            confidence = 0.9f,
            transactionCount = 10
        )
        coEvery { categorizationRepository.getMerchantByNormalizedName(normalizedMerchant) } returns merchantInfo
        
        // When
        val result = merchantCategorizer.categorizeByMerchant(merchant, testCategories)
        
        // Then
        assertNotNull(result)
        assertEquals("Shopping", result.category.name)
        assertEquals(0.9f, result.confidence)
        assertEquals(CategorizationReason.MerchantHistory, result.reason)
    }
    
    @Test
    fun `categorizeByMerchant should return null for low confidence exact match`() = runTest {
        // Given
        val merchant = "Amazon"
        val normalizedMerchant = "amazon"
        val merchantInfo = MerchantInfo(
            name = merchant,
            normalizedName = normalizedMerchant,
            categoryId = 2L,
            confidence = 0.5f, // Below threshold
            transactionCount = 10
        )
        coEvery { categorizationRepository.getMerchantByNormalizedName(normalizedMerchant) } returns merchantInfo
        coEvery { categorizationRepository.findSimilarMerchants(normalizedMerchant) } returns emptyList()
        
        // When
        val result = merchantCategorizer.categorizeByMerchant(merchant, testCategories)
        
        // Then
        assertNull(result)
    }
    
    @Test
    fun `categorizeByMerchant should use similar merchants when exact match not found`() = runTest {
        // Given
        val merchant = "Amazon India"
        val normalizedMerchant = "amazon india"
        val similarMerchant = MerchantInfo(
            name = "Amazon",
            normalizedName = "amazon",
            categoryId = 2L,
            confidence = 0.8f,
            transactionCount = 5
        )
        
        coEvery { categorizationRepository.getMerchantByNormalizedName(normalizedMerchant) } returns null
        coEvery { categorizationRepository.findSimilarMerchants(normalizedMerchant) } returns listOf(similarMerchant)
        
        // When
        val result = merchantCategorizer.categorizeByMerchant(merchant, testCategories)
        
        // Then
        assertNotNull(result)
        assertEquals("Shopping", result.category.name)
        assertTrue(result.confidence > 0.6f) // Should be adjusted based on similarity
        assertTrue(result.reason is CategorizationReason.MachineLearning)
    }
    
    @Test
    fun `categorizeByMerchant should filter out low transaction count similar merchants`() = runTest {
        // Given
        val merchant = "Amazon India"
        val normalizedMerchant = "amazon india"
        val similarMerchant = MerchantInfo(
            name = "Amazon",
            normalizedName = "amazon",
            categoryId = 2L,
            confidence = 0.8f,
            transactionCount = 1 // Below threshold
        )
        
        coEvery { categorizationRepository.getMerchantByNormalizedName(normalizedMerchant) } returns null
        coEvery { categorizationRepository.findSimilarMerchants(normalizedMerchant) } returns listOf(similarMerchant)
        
        // When
        val result = merchantCategorizer.categorizeByMerchant(merchant, testCategories)
        
        // Then
        assertNull(result)
    }
    
    @Test
    fun `updateMerchantCategory should create new merchant when not exists`() = runTest {
        // Given
        val merchant = "New Merchant"
        val category = testCategories[0]
        coEvery { categorizationRepository.getMerchantByNormalizedName(any()) } returns null
        coEvery { categorizationRepository.insertMerchant(any()) } returns 1L
        
        // When
        merchantCategorizer.updateMerchantCategory(merchant, category)
        
        // Then
        coVerify { categorizationRepository.insertMerchant(any()) }
    }
    
    @Test
    fun `updateMerchantCategory should update existing merchant`() = runTest {
        // Given
        val merchant = "Existing Merchant"
        val category = testCategories[0]
        val existingMerchant = MerchantInfo(
            name = merchant,
            normalizedName = "existing merchant",
            categoryId = 2L,
            confidence = 0.7f,
            transactionCount = 3
        )
        
        coEvery { categorizationRepository.getMerchantByNormalizedName(any()) } returns existingMerchant
        coEvery { categorizationRepository.updateMerchantCategory(any(), any(), any()) } returns Unit
        
        // When
        merchantCategorizer.updateMerchantCategory(merchant, category)
        
        // Then
        coVerify { categorizationRepository.updateMerchantCategory(merchant, 1L, any()) }
    }
    
    @Test
    fun `normalizeMerchantName should remove special characters and company suffixes`() {
        // Given
        val merchant = "Amazon.com Pvt Ltd!!!"
        
        // When
        val result = merchantCategorizer.normalizeMerchantName(merchant)
        
        // Then
        assertEquals("amazon com", result)
    }
    
    @Test
    fun `normalizeMerchantName should handle multiple company suffixes`() {
        // Given
        val merchant = "Test Company Inc Corp Limited"
        
        // When
        val result = merchantCategorizer.normalizeMerchantName(merchant)
        
        // Then
        assertEquals("test", result)
    }
    
    @Test
    fun `findSimilarMerchants should return merchants containing keywords`() = runTest {
        // Given
        val merchant = "Amazon Prime"
        val similarMerchants = listOf(
            MerchantInfo("Amazon", "amazon", 2L, 0.8f, 5),
            MerchantInfo("Prime Video", "prime video", 5L, 0.7f, 3)
        )
        
        coEvery { categorizationRepository.findSimilarMerchants("amazon") } returns listOf(similarMerchants[0])
        coEvery { categorizationRepository.findSimilarMerchants("prime") } returns listOf(similarMerchants[1])
        
        // When
        val result = merchantCategorizer.findSimilarMerchants(merchant)
        
        // Then
        assertEquals(2, result.size)
        assertTrue(result.contains("Amazon"))
        assertTrue(result.contains("Prime Video"))
    }
    
    @Test
    fun `getMerchantInfo should return merchant info from repository`() = runTest {
        // Given
        val merchant = "Test Merchant"
        val merchantInfo = MerchantInfo(merchant, "test merchant", 1L, 0.8f, 5)
        coEvery { categorizationRepository.getMerchantByName(merchant) } returns merchantInfo
        
        // When
        val result = merchantCategorizer.getMerchantInfo(merchant)
        
        // Then
        assertEquals(merchantInfo, result)
    }
}