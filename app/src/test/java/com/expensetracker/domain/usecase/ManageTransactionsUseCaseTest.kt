package com.expensetracker.domain.usecase

import com.expensetracker.domain.model.*
import com.expensetracker.domain.repository.AccountRepository
import com.expensetracker.domain.repository.TransactionRepository
import io.mockk.*
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal
import java.time.LocalDateTime
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Unit tests for ManageTransactionsUseCase
 */
class ManageTransactionsUseCaseTest {
    
    private val transactionRepository = mockk<TransactionRepository>()
    private val accountRepository = mockk<AccountRepository>()
    private lateinit var useCase: ManageTransactionsUseCase
    
    private val testAccount = Account(
        id = 1L,
        bankName = "Test Bank",
        accountType = AccountType.CHECKING,
        accountNumber = "123456789",
        nickname = "Test Account",
        currentBalance = BigDecimal("1000.00"),
        isActive = true,
        createdAt = LocalDateTime.now()
    )
    
    private val testCategory = Category(
        id = 1L,
        name = "Food",
        color = "#FF0000",
        icon = "food"
    )
    
    @Before
    fun setup() {
        useCase = ManageTransactionsUseCase(transactionRepository, accountRepository)
    }
    
    @Test
    fun `createTransaction with valid data should succeed`() = runTest {
        // Given
        coEvery { accountRepository.getAccountById(1L) } returns testAccount
        coEvery { transactionRepository.insertTransaction(any()) } returns 1L
        
        // When
        val result = useCase.createTransaction(
            amount = BigDecimal("50.00"),
            type = TransactionType.EXPENSE,
            category = testCategory,
            merchant = "Test Restaurant",
            description = "Lunch",
            date = LocalDateTime.now().minusHours(1),
            accountId = 1L
        )
        
        // Then
        assertTrue(result.isSuccess)
        assertEquals(1L, result.getOrNull())
        
        coVerify {
            transactionRepository.insertTransaction(
                match { transaction ->
                    transaction.amount == BigDecimal("50.00") &&
                    transaction.type == TransactionType.EXPENSE &&
                    transaction.category == testCategory &&
                    transaction.merchant == "Test Restaurant" &&
                    transaction.description == "Lunch" &&
                    transaction.source == TransactionSource.MANUAL &&
                    transaction.accountId == 1L
                }
            )
        }
    }
    
    @Test
    fun `createTransaction with negative amount should fail`() = runTest {
        // When
        val result = useCase.createTransaction(
            amount = BigDecimal("-50.00"),
            type = TransactionType.EXPENSE,
            category = testCategory,
            merchant = "Test Restaurant",
            description = null,
            date = LocalDateTime.now(),
            accountId = 1L
        )
        
        // Then
        assertTrue(result.isFailure)
        assertEquals("Amount must be positive", result.exceptionOrNull()?.message)
    }
    
    @Test
    fun `createTransaction with future date should fail`() = runTest {
        // When
        val result = useCase.createTransaction(
            amount = BigDecimal("50.00"),
            type = TransactionType.EXPENSE,
            category = testCategory,
            merchant = "Test Restaurant",
            description = null,
            date = LocalDateTime.now().plusDays(1),
            accountId = 1L
        )
        
        // Then
        assertTrue(result.isFailure)
        assertEquals("Date cannot be in the future", result.exceptionOrNull()?.message)
    }
    
    @Test
    fun `createTransaction with non-existent account should fail`() = runTest {
        // Given
        coEvery { accountRepository.getAccountById(1L) } returns null
        
        // When
        val result = useCase.createTransaction(
            amount = BigDecimal("50.00"),
            type = TransactionType.EXPENSE,
            category = testCategory,
            merchant = "Test Restaurant",
            description = null,
            date = LocalDateTime.now(),
            accountId = 1L
        )
        
        // Then
        assertTrue(result.isFailure)
        assertEquals("Account not found", result.exceptionOrNull()?.message)
    }
    
    @Test
    fun `createTransaction with inactive account should fail`() = runTest {
        // Given
        val inactiveAccount = testAccount.copy(isActive = false)
        coEvery { accountRepository.getAccountById(1L) } returns inactiveAccount
        
        // When
        val result = useCase.createTransaction(
            amount = BigDecimal("50.00"),
            type = TransactionType.EXPENSE,
            category = testCategory,
            merchant = "Test Restaurant",
            description = null,
            date = LocalDateTime.now(),
            accountId = 1L
        )
        
        // Then
        assertTrue(result.isFailure)
        assertEquals("Cannot add transaction to inactive account", result.exceptionOrNull()?.message)
    }
    
    @Test
    fun `updateTransaction with valid data should succeed`() = runTest {
        // Given
        val existingTransaction = Transaction(
            id = 1L,
            amount = BigDecimal("50.00"),
            type = TransactionType.EXPENSE,
            category = testCategory,
            merchant = "Old Restaurant",
            description = "Old description",
            date = LocalDateTime.now().minusHours(2),
            source = TransactionSource.MANUAL,
            accountId = 1L
        )
        
        coEvery { transactionRepository.getTransactionById(1L) } returns existingTransaction
        coEvery { accountRepository.getAccountById(1L) } returns testAccount
        coEvery { transactionRepository.updateTransaction(any()) } just Runs
        
        // When
        val result = useCase.updateTransaction(
            transactionId = 1L,
            amount = BigDecimal("75.00"),
            type = TransactionType.EXPENSE,
            category = testCategory,
            merchant = "New Restaurant",
            description = "New description",
            date = LocalDateTime.now().minusHours(1),
            accountId = 1L
        )
        
        // Then
        assertTrue(result.isSuccess)
        
        coVerify {
            transactionRepository.updateTransaction(
                match { transaction ->
                    transaction.id == 1L &&
                    transaction.amount == BigDecimal("75.00") &&
                    transaction.merchant == "New Restaurant" &&
                    transaction.description == "New description"
                }
            )
        }
    }
    
    @Test
    fun `deleteTransaction should succeed and handle linked transfers`() = runTest {
        // Given
        val transaction = Transaction(
            id = 1L,
            amount = BigDecimal("100.00"),
            type = TransactionType.TRANSFER_OUT,
            category = testCategory,
            merchant = "Transfer",
            description = null,
            date = LocalDateTime.now(),
            source = TransactionSource.MANUAL,
            accountId = 1L,
            transferTransactionId = 2L
        )
        
        coEvery { transactionRepository.getTransactionById(1L) } returns transaction
        coEvery { transactionRepository.deleteTransaction(1L) } just Runs
        coEvery { transactionRepository.deleteTransaction(2L) } just Runs
        
        // When
        val result = useCase.deleteTransaction(1L)
        
        // Then
        assertTrue(result.isSuccess)
        
        coVerify {
            transactionRepository.deleteTransaction(2L) // Linked transaction
            transactionRepository.deleteTransaction(1L) // Original transaction
        }
    }
    
    @Test
    fun `createTransfer with valid data should succeed`() = runTest {
        // Given
        val fromAccount = testAccount.copy(id = 1L, nickname = "From Account")
        val toAccount = testAccount.copy(id = 2L, nickname = "To Account")
        
        coEvery { accountRepository.getAccountById(1L) } returns fromAccount
        coEvery { accountRepository.getAccountById(2L) } returns toAccount
        coEvery { 
            transactionRepository.createTransfer(1L, 2L, "100.00", "Transfer", any()) 
        } returns Pair(1L, 2L)
        
        // When
        val result = useCase.createTransfer(
            fromAccountId = 1L,
            toAccountId = 2L,
            amount = BigDecimal("100.00"),
            description = "Transfer",
            date = LocalDateTime.now().minusHours(1)
        )
        
        // Then
        assertTrue(result.isSuccess)
        assertEquals(Pair(1L, 2L), result.getOrNull())
    }
    
    @Test
    fun `createTransfer with same account should fail`() = runTest {
        // When
        val result = useCase.createTransfer(
            fromAccountId = 1L,
            toAccountId = 1L,
            amount = BigDecimal("100.00"),
            description = "Transfer",
            date = LocalDateTime.now()
        )
        
        // Then
        assertTrue(result.isFailure)
        assertEquals("Cannot transfer to the same account", result.exceptionOrNull()?.message)
    }
    
    @Test
    fun `createTransfer with negative amount should fail`() = runTest {
        // When
        val result = useCase.createTransfer(
            fromAccountId = 1L,
            toAccountId = 2L,
            amount = BigDecimal("-100.00"),
            description = "Transfer",
            date = LocalDateTime.now()
        )
        
        // Then
        assertTrue(result.isFailure)
        assertEquals("Transfer amount must be positive", result.exceptionOrNull()?.message)
    }
    
    @Test
    fun `createTransfer with inactive accounts should fail`() = runTest {
        // Given
        val inactiveAccount = testAccount.copy(isActive = false)
        coEvery { accountRepository.getAccountById(1L) } returns inactiveAccount
        coEvery { accountRepository.getAccountById(2L) } returns testAccount
        
        // When
        val result = useCase.createTransfer(
            fromAccountId = 1L,
            toAccountId = 2L,
            amount = BigDecimal("100.00"),
            description = "Transfer",
            date = LocalDateTime.now()
        )
        
        // Then
        assertTrue(result.isFailure)
        assertEquals("Cannot transfer between inactive accounts", result.exceptionOrNull()?.message)
    }
}