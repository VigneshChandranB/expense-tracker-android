package com.expensetracker.domain.usecase

import com.expensetracker.domain.categorization.TransactionCategorizer
import com.expensetracker.domain.model.*
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal
import java.time.LocalDateTime
import kotlin.test.assertEquals

class CategorizeTransactionUseCaseTest {
    
    private lateinit var transactionCategorizer: TransactionCategorizer
    private lateinit var useCase: CategorizeTransactionUseCase
    
    private val testCategory = Category(1L, "Food & Dining", "restaurant", "#FF9800", true)
    private val testTransaction = Transaction(
        id = 1L,
        amount = BigDecimal("100.00"),
        type = TransactionType.EXPENSE,
        category = testCategory,
        merchant = "Test Restaurant",
        description = "Test transaction",
        date = LocalDateTime.now(),
        source = TransactionSource.SMS_AUTO,
        accountId = 1L
    )
    
    @Before
    fun setup() {
        transactionCategorizer = mockk()
        useCase = CategorizeTransactionUseCase(transactionCategorizer)
    }
    
    @Test
    fun `categorizeTransaction should delegate to transaction categorizer`() = runTest {
        // Given
        val expectedResult = CategorizationResult(
            category = testCategory,
            confidence = 0.8f,
            reason = CategorizationReason.KeywordMatch
        )
        coEvery { transactionCategorizer.categorizeTransaction(testTransaction) } returns expectedResult
        
        // When
        val result = useCase.categorizeTransaction(testTransaction)
        
        // Then
        assertEquals(expectedResult, result)
        coVerify { transactionCategorizer.categorizeTransaction(testTransaction) }
    }
    
    @Test
    fun `learnFromUserInput should delegate to transaction categorizer`() = runTest {
        // Given
        coEvery { transactionCategorizer.learnFromUserInput(testTransaction, testCategory) } returns Unit
        
        // When
        useCase.learnFromUserInput(testTransaction, testCategory)
        
        // Then
        coVerify { transactionCategorizer.learnFromUserInput(testTransaction, testCategory) }
    }
    
    @Test
    fun `suggestCategories should delegate to transaction categorizer`() = runTest {
        // Given
        val expectedSuggestions = listOf(
            CategorizationResult(testCategory, 0.8f, CategorizationReason.KeywordMatch)
        )
        coEvery { transactionCategorizer.suggestCategories("Test Restaurant") } returns expectedSuggestions
        
        // When
        val result = useCase.suggestCategories("Test Restaurant")
        
        // Then
        assertEquals(expectedSuggestions, result)
        coVerify { transactionCategorizer.suggestCategories("Test Restaurant") }
    }
    
    @Test
    fun `getConfidence should delegate to transaction categorizer`() = runTest {
        // Given
        val expectedConfidence = 0.75f
        coEvery { transactionCategorizer.getConfidence("Test Restaurant", testCategory) } returns expectedConfidence
        
        // When
        val result = useCase.getConfidence("Test Restaurant", testCategory)
        
        // Then
        assertEquals(expectedConfidence, result)
        coVerify { transactionCategorizer.getConfidence("Test Restaurant", testCategory) }
    }
}