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
import kotlin.system.measureTimeMillis
import kotlin.test.assertTrue

/**
 * Performance tests for categorization system
 */
class CategorizationPerformanceTest {
    
    private lateinit var categoryRepository: CategoryRepository
    private lateinit var categorizationRepository: CategorizationRepository
    private lateinit var keywordCategorizer: KeywordCategorizerImpl
    private lateinit var merchantCategorizer: MerchantCategorizerImpl
    private lateinit var smartCategorizer: SmartTransactionCategorizer
    
    private val testCategories = DefaultCategorySetup.DEFAULT_CATEGORIES
    
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
        coEvery { categoryRepository.getUncategorizedCategory() } returns testCategories.last()
    }
    
    @Test
    fun `should categorize 1000 transactions within reasonable time`() = runTest {
        // Given
        val transactions = generateTestTransactions(1000)
        setupMocksForPerformanceTest()
        
        // When
        val timeMillis = measureTimeMillis {
            transactions.forEach { transaction ->
                smartCategorizer.categorizeTransaction(transaction)
            }
        }
        
        // Then
        println("Categorized 1000 transactions in ${timeMillis}ms (${timeMillis.toDouble() / 1000}ms per transaction)")
        assertTrue(timeMillis < 5000, "Should categorize 1000 transactions within 5 seconds")
    }
    
    @Test
    fun `keyword categorization should be fast for large keyword sets`() = runTest {
        // Given
        val merchants = generateMerchantNames(500)
        setupKeywordMocks()
        
        // When
        val timeMillis = measureTimeMillis {
            merchants.forEach { merchant ->
                keywordCategorizer.categorizeByKeywords(merchant, testCategories)
            }
        }
        
        // Then
        println("Keyword categorization for 500 merchants in ${timeMillis}ms")
        assertTrue(timeMillis < 2000, "Keyword categorization should be fast")
    }
    
    @Test
    fun `merchant categorization should handle large merchant database efficiently`() = runTest {
        // Given
        val merchants = generateMerchantNames(200)
        setupMerchantMocks()
        
        // When
        val timeMillis = measureTimeMillis {
            merchants.forEach { merchant ->
                merchantCategorizer.categorizeByMerchant(merchant, testCategories)
            }
        }
        
        // Then
        println("Merchant categorization for 200 merchants in ${timeMillis}ms")
        assertTrue(timeMillis < 3000, "Merchant categorization should be efficient")
    }
    
    @Test
    fun `learning from user input should be fast`() = runTest {
        // Given
        val transactions = generateTestTransactions(100)
        val categories = testCategories.take(5)
        setupLearningMocks()
        
        // When
        val timeMillis = measureTimeMillis {
            transactions.forEachIndexed { index, transaction ->
                val category = categories[index % categories.size]
                smartCategorizer.learnFromUserInput(transaction, category)
            }
        }
        
        // Then
        println("Learning from 100 user inputs in ${timeMillis}ms")
        assertTrue(timeMillis < 1000, "Learning should be fast")
    }
    
    @Test
    fun `category suggestions should be generated quickly`() = runTest {
        // Given
        val merchants = generateMerchantNames(100)
        setupSuggestionMocks()
        
        // When
        val timeMillis = measureTimeMillis {
            merchants.forEach { merchant ->
                smartCategorizer.suggestCategories(merchant)
            }
        }
        
        // Then
        println("Generated suggestions for 100 merchants in ${timeMillis}ms")
        assertTrue(timeMillis < 1500, "Suggestion generation should be quick")
    }
    
    private fun generateTestTransactions(count: Int): List<Transaction> {
        val merchants = listOf(
            "Amazon", "Flipkart", "Swiggy", "Zomato", "Uber", "Ola",
            "McDonald's", "Starbucks", "Netflix", "Spotify", "Paytm",
            "PhonePe", "Google Pay", "HDFC Bank", "ICICI Bank"
        )
        
        return (1..count).map { index ->
            Transaction(
                id = index.toLong(),
                amount = BigDecimal((10..1000).random().toString()),
                type = TransactionType.EXPENSE,
                category = testCategories.random(),
                merchant = merchants.random() + if (index % 10 == 0) " Store $index" else "",
                description = "Test transaction $index",
                date = LocalDateTime.now().minusDays((0..30).random().toLong()),
                source = TransactionSource.SMS_AUTO,
                accountId = (1..3).random().toLong()
            )
        }
    }
    
    private fun generateMerchantNames(count: Int): List<String> {
        val baseNames = listOf(
            "Amazon", "Flipkart", "Swiggy", "Zomato", "Uber", "Ola",
            "McDonald's", "Starbucks", "Netflix", "Spotify", "Paytm",
            "Restaurant", "Store", "Shop", "Market", "Cafe", "Hotel"
        )
        
        return (1..count).map { index ->
            val baseName = baseNames.random()
            when {
                index % 5 == 0 -> "$baseName Store $index"
                index % 7 == 0 -> "$baseName - Branch $index"
                index % 11 == 0 -> "$baseName Pvt Ltd"
                else -> baseName
            }
        }
    }
    
    private fun setupMocksForPerformanceTest() {
        // Setup for fast categorization
        coEvery { categorizationRepository.getRulesForMerchant(any()) } returns emptyList()
        coEvery { categorizationRepository.getMerchantByNormalizedName(any()) } returns null
        coEvery { categorizationRepository.findSimilarMerchants(any()) } returns emptyList()
        
        // Setup keyword mappings for common merchants
        val keywordMappings = DefaultCategorySetup.DEFAULT_KEYWORD_MAPPINGS
        keywordMappings.forEach { (keyword, categoryId) ->
            coEvery { categorizationRepository.getKeywordMapping(keyword) } returns Pair(categoryId, true)
        }
        coEvery { categorizationRepository.getKeywordMapping(any()) } returns null
        coEvery { categorizationRepository.getDefaultKeywordMappings() } returns keywordMappings
    }
    
    private fun setupKeywordMocks() {
        val keywordMappings = DefaultCategorySetup.DEFAULT_KEYWORD_MAPPINGS
        keywordMappings.forEach { (keyword, categoryId) ->
            coEvery { categorizationRepository.getKeywordMapping(keyword) } returns Pair(categoryId, true)
        }
        coEvery { categorizationRepository.getKeywordMapping(any()) } returns null
        coEvery { categorizationRepository.getDefaultKeywordMappings() } returns keywordMappings
    }
    
    private fun setupMerchantMocks() {
        coEvery { categorizationRepository.getMerchantByNormalizedName(any()) } returns null
        coEvery { categorizationRepository.findSimilarMerchants(any()) } returns emptyList()
    }
    
    private fun setupLearningMocks() {
        coEvery { categorizationRepository.getRulesForMerchant(any()) } returns emptyList()
        coEvery { categorizationRepository.insertRule(any()) } returns 1L
        coEvery { categorizationRepository.incrementMerchantTransactionCount(any()) } returns Unit
        coEvery { categorizationRepository.getMerchantByNormalizedName(any()) } returns null
        coEvery { categorizationRepository.insertMerchant(any()) } returns 1L
    }
    
    private fun setupSuggestionMocks() {
        coEvery { categorizationRepository.getMerchantByNormalizedName(any()) } returns null
        coEvery { categorizationRepository.getKeywordMapping(any()) } returns null
        coEvery { categorizationRepository.getDefaultKeywordMappings() } returns DefaultCategorySetup.DEFAULT_KEYWORD_MAPPINGS
        coEvery { categorizationRepository.findSimilarMerchants(any()) } returns emptyList()
    }
}