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
 * Accuracy tests for categorization system
 * Tests the correctness of categorization decisions
 */
class CategorizationAccuracyTest {
    
    private lateinit var categoryRepository: CategoryRepository
    private lateinit var categorizationRepository: CategorizationRepository
    private lateinit var smartCategorizer: SmartTransactionCategorizer
    
    private val testCategories = DefaultCategorySetup.DEFAULT_CATEGORIES
    
    @Before
    fun setup() {
        categoryRepository = mockk()
        categorizationRepository = mockk()
        
        val keywordCategorizer = KeywordCategorizerImpl(categorizationRepository)
        val merchantCategorizer = MerchantCategorizerImpl(categorizationRepository)
        smartCategorizer = SmartTransactionCategorizer(
            keywordCategorizer,
            merchantCategorizer,
            categoryRepository,
            categorizationRepository
        )
        
        // Setup common mocks
        coEvery { categoryRepository.getAllCategories() } returns testCategories
        coEvery { categoryRepository.getUncategorizedCategory() } returns testCategories.find { it.name == "Uncategorized" }!!
        
        setupDefaultKeywordMappings()
    }
    
    @Test
    fun `should correctly categorize food delivery services`() = runTest {
        val testCases = mapOf(
            "Swiggy" to "Food & Dining",
            "Zomato" to "Food & Dining",
            "Uber Eats" to "Food & Dining",
            "McDonald's" to "Food & Dining",
            "KFC" to "Food & Dining",
            "Domino's Pizza" to "Food & Dining",
            "Starbucks Coffee" to "Food & Dining"
        )
        
        testCases.forEach { (merchant, expectedCategory) ->
            val transaction = createTransaction(merchant)
            val result = smartCategorizer.categorizeTransaction(transaction)
            
            assertEquals(
                expectedCategory, 
                result.category.name,
                "Merchant '$merchant' should be categorized as '$expectedCategory'"
            )
            assertTrue(
                result.confidence >= 0.6f,
                "Confidence for '$merchant' should be at least 0.6, got ${result.confidence}"
            )
        }
    }
    
    @Test
    fun `should correctly categorize e-commerce platforms`() = runTest {
        val testCases = mapOf(
            "Amazon" to "Shopping",
            "Flipkart" to "Shopping",
            "Myntra" to "Shopping",
            "Amazon.in" to "Shopping",
            "Flipkart Fashion" to "Shopping",
            "Myntra Store" to "Shopping"
        )
        
        testCases.forEach { (merchant, expectedCategory) ->
            val transaction = createTransaction(merchant)
            val result = smartCategorizer.categorizeTransaction(transaction)
            
            assertEquals(
                expectedCategory,
                result.category.name,
                "Merchant '$merchant' should be categorized as '$expectedCategory'"
            )
        }
    }
    
    @Test
    fun `should correctly categorize transportation services`() = runTest {
        val testCases = mapOf(
            "Uber" to "Transportation",
            "Ola Cabs" to "Transportation",
            "Indian Oil Petrol" to "Transportation",
            "HP Petrol Pump" to "Transportation",
            "Metro Card Recharge" to "Transportation",
            "Bus Ticket" to "Transportation"
        )
        
        testCases.forEach { (merchant, expectedCategory) ->
            val transaction = createTransaction(merchant)
            val result = smartCategorizer.categorizeTransaction(transaction)
            
            assertEquals(
                expectedCategory,
                result.category.name,
                "Merchant '$merchant' should be categorized as '$expectedCategory'"
            )
        }
    }
    
    @Test
    fun `should correctly categorize utility bills`() = runTest {
        val testCases = mapOf(
            "Electricity Bill" to "Bills & Utilities",
            "Water Bill Payment" to "Bills & Utilities",
            "Internet Bill" to "Bills & Utilities",
            "Mobile Recharge" to "Bills & Utilities",
            "Broadband Payment" to "Bills & Utilities",
            "Insurance Premium" to "Bills & Utilities"
        )
        
        testCases.forEach { (merchant, expectedCategory) ->
            val transaction = createTransaction(merchant)
            val result = smartCategorizer.categorizeTransaction(transaction)
            
            assertEquals(
                expectedCategory,
                result.category.name,
                "Merchant '$merchant' should be categorized as '$expectedCategory'"
            )
        }
    }
    
    @Test
    fun `should correctly categorize entertainment services`() = runTest {
        val testCases = mapOf(
            "Netflix" to "Entertainment",
            "Spotify Premium" to "Entertainment",
            "YouTube Premium" to "Entertainment",
            "Movie Ticket" to "Entertainment",
            "Cinema Hall" to "Entertainment",
            "Gaming Store" to "Entertainment"
        )
        
        testCases.forEach { (merchant, expectedCategory) ->
            val transaction = createTransaction(merchant)
            val result = smartCategorizer.categorizeTransaction(transaction)
            
            assertEquals(
                expectedCategory,
                result.category.name,
                "Merchant '$merchant' should be categorized as '$expectedCategory'"
            )
        }
    }
    
    @Test
    fun `should correctly categorize healthcare services`() = runTest {
        val testCases = mapOf(
            "Apollo Hospital" to "Healthcare",
            "Medical Store" to "Healthcare",
            "Pharmacy" to "Healthcare",
            "Doctor Consultation" to "Healthcare",
            "Dental Clinic" to "Healthcare",
            "Lab Test" to "Healthcare"
        )
        
        testCases.forEach { (merchant, expectedCategory) ->
            val transaction = createTransaction(merchant)
            val result = smartCategorizer.categorizeTransaction(transaction)
            
            assertEquals(
                expectedCategory,
                result.category.name,
                "Merchant '$merchant' should be categorized as '$expectedCategory'"
            )
        }
    }
    
    @Test
    fun `should correctly categorize investment services`() = runTest {
        val testCases = mapOf(
            "Zerodha" to "Investment",
            "Groww App" to "Investment",
            "Mutual Fund SIP" to "Investment",
            "Stock Trading" to "Investment",
            "Fixed Deposit" to "Investment"
        )
        
        testCases.forEach { (merchant, expectedCategory) ->
            val transaction = createTransaction(merchant)
            val result = smartCategorizer.categorizeTransaction(transaction)
            
            assertEquals(
                expectedCategory,
                result.category.name,
                "Merchant '$merchant' should be categorized as '$expectedCategory'"
            )
        }
    }
    
    @Test
    fun `should correctly categorize income transactions`() = runTest {
        val testCases = mapOf(
            "Salary Credit" to "Income",
            "Bonus Payment" to "Income",
            "Refund" to "Income",
            "Cashback" to "Income",
            "Interest Credit" to "Income",
            "Dividend" to "Income"
        )
        
        testCases.forEach { (merchant, expectedCategory) ->
            val transaction = createTransaction(merchant, TransactionType.INCOME)
            val result = smartCategorizer.categorizeTransaction(transaction)
            
            assertEquals(
                expectedCategory,
                result.category.name,
                "Merchant '$merchant' should be categorized as '$expectedCategory'"
            )
        }
    }
    
    @Test
    fun `should correctly categorize transfer transactions`() = runTest {
        val testCases = mapOf(
            "UPI Transfer" to "Transfer",
            "Paytm Payment" to "Transfer",
            "Google Pay" to "Transfer",
            "PhonePe" to "Transfer",
            "NEFT Transfer" to "Transfer",
            "RTGS Payment" to "Transfer"
        )
        
        testCases.forEach { (merchant, expectedCategory) ->
            val transaction = createTransaction(merchant)
            val result = smartCategorizer.categorizeTransaction(transaction)
            
            assertEquals(
                expectedCategory,
                result.category.name,
                "Merchant '$merchant' should be categorized as '$expectedCategory'"
            )
        }
    }
    
    @Test
    fun `should handle case insensitive matching`() = runTest {
        val testCases = listOf(
            "AMAZON" to "Shopping",
            "amazon" to "Shopping",
            "AmAzOn" to "Shopping",
            "SWIGGY" to "Food & Dining",
            "swiggy" to "Food & Dining"
        )
        
        testCases.forEach { (merchant, expectedCategory) ->
            val transaction = createTransaction(merchant)
            val result = smartCategorizer.categorizeTransaction(transaction)
            
            assertEquals(
                expectedCategory,
                result.category.name,
                "Case insensitive matching failed for '$merchant'"
            )
        }
    }
    
    @Test
    fun `should handle merchants with special characters`() = runTest {
        val testCases = listOf(
            "McDonald's" to "Food & Dining",
            "Amazon.com" to "Shopping",
            "Uber - Ride" to "Transportation",
            "Netflix (Premium)" to "Entertainment"
        )
        
        testCases.forEach { (merchant, expectedCategory) ->
            val transaction = createTransaction(merchant)
            val result = smartCategorizer.categorizeTransaction(transaction)
            
            assertEquals(
                expectedCategory,
                result.category.name,
                "Special character handling failed for '$merchant'"
            )
        }
    }
    
    @Test
    fun `should prioritize more specific keywords`() = runTest {
        // Test that more specific keywords take precedence
        val transaction = createTransaction("Amazon Prime Video")
        val result = smartCategorizer.categorizeTransaction(transaction)
        
        // Should be categorized as Shopping (Amazon) rather than Entertainment (Video)
        // because Amazon is a more specific/stronger keyword
        assertEquals("Shopping", result.category.name)
    }
    
    @Test
    fun `should achieve high accuracy on test dataset`() = runTest {
        val testDataset = createTestDataset()
        var correctPredictions = 0
        
        testDataset.forEach { (merchant, expectedCategory) ->
            val transaction = createTransaction(merchant)
            val result = smartCategorizer.categorizeTransaction(transaction)
            
            if (result.category.name == expectedCategory) {
                correctPredictions++
            } else {
                println("Incorrect: '$merchant' -> Expected: '$expectedCategory', Got: '${result.category.name}'")
            }
        }
        
        val accuracy = correctPredictions.toDouble() / testDataset.size
        println("Categorization accuracy: ${(accuracy * 100).toInt()}% ($correctPredictions/${testDataset.size})")
        
        assertTrue(
            accuracy >= 0.85,
            "Categorization accuracy should be at least 85%, got ${(accuracy * 100).toInt()}%"
        )
    }
    
    private fun createTransaction(
        merchant: String, 
        type: TransactionType = TransactionType.EXPENSE
    ): Transaction {
        return Transaction(
            id = 1L,
            amount = BigDecimal("100.00"),
            type = type,
            category = testCategories.find { it.name == "Uncategorized" }!!,
            merchant = merchant,
            description = "Test transaction",
            date = LocalDateTime.now(),
            source = TransactionSource.SMS_AUTO,
            accountId = 1L
        )
    }
    
    private fun setupDefaultKeywordMappings() {
        coEvery { categorizationRepository.getRulesForMerchant(any()) } returns emptyList()
        coEvery { categorizationRepository.getMerchantByNormalizedName(any()) } returns null
        coEvery { categorizationRepository.findSimilarMerchants(any()) } returns emptyList()
        
        val keywordMappings = DefaultCategorySetup.DEFAULT_KEYWORD_MAPPINGS
        keywordMappings.forEach { (keyword, categoryId) ->
            coEvery { categorizationRepository.getKeywordMapping(keyword) } returns Pair(categoryId, true)
        }
        coEvery { categorizationRepository.getKeywordMapping(any()) } returns null
        coEvery { categorizationRepository.getDefaultKeywordMappings() } returns keywordMappings
    }
    
    private fun createTestDataset(): List<Pair<String, String>> {
        return listOf(
            // Food & Dining
            "Swiggy" to "Food & Dining",
            "Zomato" to "Food & Dining",
            "McDonald's" to "Food & Dining",
            "KFC" to "Food & Dining",
            "Domino's" to "Food & Dining",
            "Subway" to "Food & Dining",
            "Pizza Hut" to "Food & Dining",
            "Cafe Coffee Day" to "Food & Dining",
            
            // Shopping
            "Amazon" to "Shopping",
            "Flipkart" to "Shopping",
            "Myntra" to "Shopping",
            "Walmart" to "Shopping",
            "Target" to "Shopping",
            "Costco" to "Shopping",
            
            // Transportation
            "Uber" to "Transportation",
            "Ola" to "Transportation",
            "Indian Oil" to "Transportation",
            "HP Petrol" to "Transportation",
            "Metro" to "Transportation",
            "Bus Ticket" to "Transportation",
            
            // Bills & Utilities
            "Electricity Bill" to "Bills & Utilities",
            "Water Bill" to "Bills & Utilities",
            "Internet" to "Bills & Utilities",
            "Mobile Recharge" to "Bills & Utilities",
            "Insurance" to "Bills & Utilities",
            
            // Entertainment
            "Netflix" to "Entertainment",
            "Spotify" to "Entertainment",
            "YouTube" to "Entertainment",
            "Movie" to "Entertainment",
            "Gaming" to "Entertainment",
            
            // Healthcare
            "Hospital" to "Healthcare",
            "Pharmacy" to "Healthcare",
            "Doctor" to "Healthcare",
            "Medical" to "Healthcare",
            
            // Investment
            "Zerodha" to "Investment",
            "Groww" to "Investment",
            "Mutual Fund" to "Investment",
            "SIP" to "Investment",
            
            // Income
            "Salary" to "Income",
            "Bonus" to "Income",
            "Refund" to "Income",
            "Cashback" to "Income",
            
            // Transfer
            "UPI" to "Transfer",
            "Paytm" to "Transfer",
            "GPay" to "Transfer",
            "PhonePe" to "Transfer"
        )
    }
}