package com.expensetracker.data.repository

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.expensetracker.data.local.database.ExpenseDatabase
import com.expensetracker.data.mapper.AccountMapper
import com.expensetracker.domain.model.Account
import com.expensetracker.domain.model.AccountType
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.math.BigDecimal
import java.time.LocalDateTime

/**
 * Integration tests for AccountRepositoryImpl with real database
 */
@RunWith(AndroidJUnit4::class)
class AccountRepositoryIntegrationTest {

    private lateinit var database: ExpenseDatabase
    private lateinit var accountRepository: AccountRepositoryImpl
    private lateinit var accountMapper: AccountMapper

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            ExpenseDatabase::class.java
        ).build()

        accountMapper = AccountMapper()
        accountRepository = AccountRepositoryImpl(database.accountDao(), accountMapper)
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun insertAndRetrieveAccount() = runTest {
        // Given
        val account = Account(
            bankName = "Test Bank",
            accountType = AccountType.CHECKING,
            accountNumber = "1234567890",
            nickname = "My Checking",
            currentBalance = BigDecimal("1000.00"),
            isActive = true,
            createdAt = LocalDateTime.now()
        )

        // When
        val accountId = accountRepository.insertAccount(account)
        val retrievedAccount = accountRepository.getAccount(accountId)

        // Then
        assertNotNull(retrievedAccount)
        assertEquals(account.bankName, retrievedAccount!!.bankName)
        assertEquals(account.accountType, retrievedAccount.accountType)
        assertEquals(account.accountNumber, retrievedAccount.accountNumber)
        assertEquals(account.nickname, retrievedAccount.nickname)
        assertEquals(account.currentBalance, retrievedAccount.currentBalance)
        assertEquals(account.isActive, retrievedAccount.isActive)
    }

    @Test
    fun updateAccount() = runTest {
        // Given
        val account = Account(
            bankName = "Test Bank",
            accountType = AccountType.CHECKING,
            accountNumber = "1234567890",
            nickname = "My Checking",
            currentBalance = BigDecimal("1000.00"),
            isActive = true,
            createdAt = LocalDateTime.now()
        )

        val accountId = accountRepository.insertAccount(account)
        val updatedAccount = account.copy(
            id = accountId,
            bankName = "Updated Bank",
            nickname = "Updated Checking",
            currentBalance = BigDecimal("2000.00")
        )

        // When
        accountRepository.updateAccount(updatedAccount)
        val retrievedAccount = accountRepository.getAccount(accountId)

        // Then
        assertNotNull(retrievedAccount)
        assertEquals("Updated Bank", retrievedAccount!!.bankName)
        assertEquals("Updated Checking", retrievedAccount.nickname)
        assertEquals(BigDecimal("2000.00"), retrievedAccount.currentBalance)
    }

    @Test
    fun deleteAccount() = runTest {
        // Given
        val account = Account(
            bankName = "Test Bank",
            accountType = AccountType.CHECKING,
            accountNumber = "1234567890",
            nickname = "My Checking",
            currentBalance = BigDecimal("1000.00"),
            isActive = true,
            createdAt = LocalDateTime.now()
        )

        val accountId = accountRepository.insertAccount(account)

        // When
        accountRepository.deleteAccount(accountId)
        val retrievedAccount = accountRepository.getAccount(accountId)

        // Then
        assertNull(retrievedAccount)
    }

    @Test
    fun getAllAccounts() = runTest {
        // Given
        val account1 = Account(
            bankName = "Bank 1",
            accountType = AccountType.CHECKING,
            accountNumber = "1111111111",
            nickname = "Account 1",
            currentBalance = BigDecimal("1000.00"),
            isActive = true,
            createdAt = LocalDateTime.now()
        )

        val account2 = Account(
            bankName = "Bank 2",
            accountType = AccountType.SAVINGS,
            accountNumber = "2222222222",
            nickname = "Account 2",
            currentBalance = BigDecimal("2000.00"),
            isActive = false,
            createdAt = LocalDateTime.now()
        )

        // When
        accountRepository.insertAccount(account1)
        accountRepository.insertAccount(account2)
        val allAccounts = accountRepository.getAllAccounts()

        // Then
        assertEquals(2, allAccounts.size)
        assertTrue(allAccounts.any { it.nickname == "Account 1" })
        assertTrue(allAccounts.any { it.nickname == "Account 2" })
    }

    @Test
    fun getActiveAccounts() = runTest {
        // Given
        val activeAccount = Account(
            bankName = "Bank 1",
            accountType = AccountType.CHECKING,
            accountNumber = "1111111111",
            nickname = "Active Account",
            currentBalance = BigDecimal("1000.00"),
            isActive = true,
            createdAt = LocalDateTime.now()
        )

        val inactiveAccount = Account(
            bankName = "Bank 2",
            accountType = AccountType.SAVINGS,
            accountNumber = "2222222222",
            nickname = "Inactive Account",
            currentBalance = BigDecimal("2000.00"),
            isActive = false,
            createdAt = LocalDateTime.now()
        )

        // When
        accountRepository.insertAccount(activeAccount)
        accountRepository.insertAccount(inactiveAccount)
        val activeAccounts = accountRepository.getActiveAccounts()

        // Then
        assertEquals(1, activeAccounts.size)
        assertEquals("Active Account", activeAccounts[0].nickname)
        assertTrue(activeAccounts[0].isActive)
    }

    @Test
    fun observeAccounts() = runTest {
        // Given
        val account = Account(
            bankName = "Test Bank",
            accountType = AccountType.CHECKING,
            accountNumber = "1234567890",
            nickname = "My Checking",
            currentBalance = BigDecimal("1000.00"),
            isActive = true,
            createdAt = LocalDateTime.now()
        )

        // When
        val initialAccounts = accountRepository.observeAccounts().first()
        accountRepository.insertAccount(account)
        val accountsAfterInsert = accountRepository.observeAccounts().first()

        // Then
        assertEquals(0, initialAccounts.size)
        assertEquals(1, accountsAfterInsert.size)
        assertEquals("My Checking", accountsAfterInsert[0].nickname)
    }

    @Test
    fun observeActiveAccounts() = runTest {
        // Given
        val activeAccount = Account(
            bankName = "Bank 1",
            accountType = AccountType.CHECKING,
            accountNumber = "1111111111",
            nickname = "Active Account",
            currentBalance = BigDecimal("1000.00"),
            isActive = true,
            createdAt = LocalDateTime.now()
        )

        val inactiveAccount = Account(
            bankName = "Bank 2",
            accountType = AccountType.SAVINGS,
            accountNumber = "2222222222",
            nickname = "Inactive Account",
            currentBalance = BigDecimal("2000.00"),
            isActive = false,
            createdAt = LocalDateTime.now()
        )

        // When
        accountRepository.insertAccount(activeAccount)
        accountRepository.insertAccount(inactiveAccount)
        val activeAccounts = accountRepository.observeActiveAccounts().first()

        // Then
        assertEquals(1, activeAccounts.size)
        assertEquals("Active Account", activeAccounts[0].nickname)
        assertTrue(activeAccounts[0].isActive)
    }

    @Test
    fun updateAccountStatus() = runTest {
        // Given
        val account = Account(
            bankName = "Test Bank",
            accountType = AccountType.CHECKING,
            accountNumber = "1234567890",
            nickname = "My Checking",
            currentBalance = BigDecimal("1000.00"),
            isActive = true,
            createdAt = LocalDateTime.now()
        )

        val accountId = accountRepository.insertAccount(account)

        // When
        accountRepository.updateAccountStatus(accountId, false)
        val updatedAccount = accountRepository.getAccount(accountId)

        // Then
        assertNotNull(updatedAccount)
        assertFalse(updatedAccount!!.isActive)
    }

    @Test
    fun updateAccountBalance() = runTest {
        // Given
        val account = Account(
            bankName = "Test Bank",
            accountType = AccountType.CHECKING,
            accountNumber = "1234567890",
            nickname = "My Checking",
            currentBalance = BigDecimal("1000.00"),
            isActive = true,
            createdAt = LocalDateTime.now()
        )

        val accountId = accountRepository.insertAccount(account)
        val newBalance = BigDecimal("2500.75")

        // When
        accountRepository.updateAccountBalance(accountId, newBalance)
        val updatedAccount = accountRepository.getAccount(accountId)

        // Then
        assertNotNull(updatedAccount)
        assertEquals(newBalance, updatedAccount!!.currentBalance)
    }

    @Test
    fun getAccountsByBank() = runTest {
        // Given
        val account1 = Account(
            bankName = "Target Bank",
            accountType = AccountType.CHECKING,
            accountNumber = "1111111111",
            nickname = "Account 1",
            currentBalance = BigDecimal("1000.00"),
            isActive = true,
            createdAt = LocalDateTime.now()
        )

        val account2 = Account(
            bankName = "Target Bank",
            accountType = AccountType.SAVINGS,
            accountNumber = "2222222222",
            nickname = "Account 2",
            currentBalance = BigDecimal("2000.00"),
            isActive = true,
            createdAt = LocalDateTime.now()
        )

        val account3 = Account(
            bankName = "Other Bank",
            accountType = AccountType.CHECKING,
            accountNumber = "3333333333",
            nickname = "Account 3",
            currentBalance = BigDecimal("3000.00"),
            isActive = true,
            createdAt = LocalDateTime.now()
        )

        // When
        accountRepository.insertAccount(account1)
        accountRepository.insertAccount(account2)
        accountRepository.insertAccount(account3)
        val targetBankAccounts = accountRepository.getAccountsByBank("Target Bank")

        // Then
        assertEquals(2, targetBankAccounts.size)
        assertTrue(targetBankAccounts.all { it.bankName == "Target Bank" })
    }

    @Test
    fun searchAccountsByNumber() = runTest {
        // Given
        val account1 = Account(
            bankName = "Bank 1",
            accountType = AccountType.CHECKING,
            accountNumber = "1234567890",
            nickname = "Account 1",
            currentBalance = BigDecimal("1000.00"),
            isActive = true,
            createdAt = LocalDateTime.now()
        )

        val account2 = Account(
            bankName = "Bank 2",
            accountType = AccountType.SAVINGS,
            accountNumber = "9876543210",
            nickname = "Account 2",
            currentBalance = BigDecimal("2000.00"),
            isActive = true,
            createdAt = LocalDateTime.now()
        )

        // When
        accountRepository.insertAccount(account1)
        accountRepository.insertAccount(account2)
        val searchResults = accountRepository.searchAccountsByNumber("1234")

        // Then
        assertEquals(1, searchResults.size)
        assertEquals("Account 1", searchResults[0].nickname)
        assertTrue(searchResults[0].accountNumber.contains("1234"))
    }
}