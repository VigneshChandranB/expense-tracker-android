package com.expensetracker.domain.usecase.onboarding

import com.expensetracker.domain.model.*
import com.expensetracker.domain.repository.AccountRepository
import com.expensetracker.domain.repository.CategoryRepository
import com.expensetracker.domain.repository.TransactionRepository
import io.mockk.*
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal
import kotlin.test.assertTrue

class CreateSampleDataUseCaseTest {

    private lateinit var accountRepository: AccountRepository
    private lateinit var categoryRepository: CategoryRepository
    private lateinit var transactionRepository: TransactionRepository
    private lateinit var createSampleDataUseCase: CreateSampleDataUseCase

    @Before
    fun setup() {
        accountRepository = mockk()
        categoryRepository = mockk()
        transactionRepository = mockk()
        
        createSampleDataUseCase = CreateSampleDataUseCase(
            accountRepository,
            categoryRepository,
            transactionRepository
        )
    }

    @Test
    fun `createSampleData should create accounts when enabled`() = runTest {
        // Arrange
        val config = SampleDataConfig(
            createSampleAccounts = true,
            createSampleTransactions = false,
            createSampleCategories = false
        )
        
        coEvery { accountRepository.insertAccount(any()) } returns 1L
        
        // Act
        val result = createSampleDataUseCase.createSampleData(config)
        
        // Assert
        assertTrue(result.isSuccess)
        coVerify(exactly = 3) { accountRepository.insertAccount(any()) }
    }

    @Test
    fun `createSampleData should create categories when enabled`() = runTest {
        // Arrange
        val config = SampleDataConfig(
            createSampleAccounts = false,
            createSampleTransactions = false,
            createSampleCategories = true
        )
        
        coEvery { categoryRepository.insertCategory(any()) } just Runs
        
        // Act
        val result = createSampleDataUseCase.createSampleData(config)
        
        // Assert
        assertTrue(result.isSuccess)
        coVerify(atLeast = 10) { categoryRepository.insertCategory(any()) }
    }

    @Test
    fun `createSampleData should create transactions when enabled and accounts exist`() = runTest {
        // Arrange
        val config = SampleDataConfig(
            createSampleAccounts = true,
            createSampleTransactions = true,
            createSampleCategories = true,
            numberOfSampleTransactions = 5
        )
        
        val sampleCategories = listOf(
            Category(1, "Food & Dining", "restaurant", "#FF9800", true),
            Category(2, "Shopping", "shopping_cart", "#2196F3", true),
            Category(8, "Income", "attach_money", "#8BC34A", true),
            Category(9, "Transfer", "swap_horiz", "#607D8B", true)
        )
        
        coEvery { accountRepository.insertAccount(any()) } returnsMany listOf(1L, 2L, 3L)
        coEvery { categoryRepository.insertCategory(any()) } just Runs
        coEvery { categoryRepository.getAllCategories() } returns sampleCategories
        coEvery { transactionRepository.insertTransaction(any()) } returns 1L
        
        // Act
        val result = createSampleDataUseCase.createSampleData(config)
        
        // Assert
        assertTrue(result.isSuccess)
        coVerify(atLeast = 5) { transactionRepository.insertTransaction(any()) }
    }

    @Test
    fun `createSampleData should handle repository exceptions`() = runTest {
        // Arrange
        val config = SampleDataConfig(createSampleAccounts = true)
        
        coEvery { accountRepository.insertAccount(any()) } throws Exception("Database error")
        
        // Act
        val result = createSampleDataUseCase.createSampleData(config)
        
        // Assert
        assertTrue(result.isFailure)
    }

    @Test
    fun `createSampleData should not create transactions without accounts`() = runTest {
        // Arrange
        val config = SampleDataConfig(
            createSampleAccounts = false,
            createSampleTransactions = true,
            createSampleCategories = true
        )
        
        coEvery { categoryRepository.insertCategory(any()) } just Runs
        
        // Act
        val result = createSampleDataUseCase.createSampleData(config)
        
        // Assert
        assertTrue(result.isSuccess)
        coVerify(exactly = 0) { transactionRepository.insertTransaction(any()) }
    }

    @Test
    fun `sample accounts should have correct properties`() = runTest {
        // Arrange
        val config = SampleDataConfig(createSampleAccounts = true)
        val capturedAccounts = mutableListOf<Account>()
        
        coEvery { accountRepository.insertAccount(capture(capturedAccounts)) } returns 1L
        
        // Act
        createSampleDataUseCase.createSampleData(config)
        
        // Assert
        assertTrue(capturedAccounts.size == 3)
        
        val hdfcAccount = capturedAccounts.find { it.bankName == "HDFC Bank" }
        assertTrue(hdfcAccount != null)
        assertTrue(hdfcAccount.accountType == AccountType.CHECKING)
        assertTrue(hdfcAccount.currentBalance == BigDecimal("25000.00"))
        
        val sbiAccount = capturedAccounts.find { it.bankName == "SBI" }
        assertTrue(sbiAccount != null)
        assertTrue(sbiAccount.accountType == AccountType.SAVINGS)
        
        val iciciAccount = capturedAccounts.find { it.bankName == "ICICI Bank" }
        assertTrue(iciciAccount != null)
        assertTrue(iciciAccount.accountType == AccountType.CREDIT_CARD)
    }

    @Test
    fun `sample categories should include all default categories`() = runTest {
        // Arrange
        val config = SampleDataConfig(createSampleCategories = true)
        val capturedCategories = mutableListOf<Category>()
        
        coEvery { categoryRepository.insertCategory(capture(capturedCategories)) } just Runs
        
        // Act
        createSampleDataUseCase.createSampleData(config)
        
        // Assert
        val categoryNames = capturedCategories.map { it.name }
        assertTrue(categoryNames.contains("Food & Dining"))
        assertTrue(categoryNames.contains("Shopping"))
        assertTrue(categoryNames.contains("Transportation"))
        assertTrue(categoryNames.contains("Bills & Utilities"))
        assertTrue(categoryNames.contains("Entertainment"))
        assertTrue(categoryNames.contains("Healthcare"))
        assertTrue(categoryNames.contains("Investment"))
        assertTrue(categoryNames.contains("Income"))
        assertTrue(categoryNames.contains("Transfer"))
        assertTrue(categoryNames.contains("Other"))
    }
}