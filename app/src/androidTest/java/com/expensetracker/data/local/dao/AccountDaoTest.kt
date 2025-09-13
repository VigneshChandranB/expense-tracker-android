package com.expensetracker.data.local.dao

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.expensetracker.data.local.database.ExpenseDatabase
import com.expensetracker.data.local.entities.AccountEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.Assert.*

/**
 * Integration tests for AccountDao
 */
@RunWith(AndroidJUnit4::class)
class AccountDaoTest {
    
    private lateinit var database: ExpenseDatabase
    private lateinit var accountDao: AccountDao
    
    @Before
    fun setup() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            ExpenseDatabase::class.java
        ).allowMainThreadQueries().build()
        
        accountDao = database.accountDao()
    }
    
    @After
    fun teardown() {
        database.close()
    }
    
    @Test
    fun insertAndRetrieveAccount() = runTest {
        val account = AccountEntity(
            bankName = "HDFC Bank",
            accountType = "SAVINGS",
            accountNumber = "1234567890",
            nickname = "Primary Savings",
            currentBalance = "10000.50",
            isActive = true,
            createdAt = System.currentTimeMillis()
        )
        
        val accountId = accountDao.insertAccount(account)
        val retrievedAccount = accountDao.getAccountById(accountId)
        
        assertNotNull(retrievedAccount)
        assertEquals(account.bankName, retrievedAccount?.bankName)
        assertEquals(account.accountType, retrievedAccount?.accountType)
        assertEquals(account.accountNumber, retrievedAccount?.accountNumber)
        assertEquals(account.nickname, retrievedAccount?.nickname)
        assertEquals(account.currentBalance, retrievedAccount?.currentBalance)
    }
    
    @Test
    fun getAllAccounts() = runTest {
        val accounts = listOf(
            AccountEntity(
                bankName = "HDFC Bank",
                accountType = "SAVINGS",
                accountNumber = "1111111111",
                nickname = "HDFC Savings",
                currentBalance = "5000.00",
                isActive = true,
                createdAt = System.currentTimeMillis()
            ),
            AccountEntity(
                bankName = "ICICI Bank",
                accountType = "CREDIT_CARD",
                accountNumber = "2222222222",
                nickname = "ICICI Credit",
                currentBalance = "-1500.00",
                isActive = true,
                createdAt = System.currentTimeMillis()
            ),
            AccountEntity(
                bankName = "SBI",
                accountType = "CHECKING",
                accountNumber = "3333333333",
                nickname = "SBI Checking",
                currentBalance = "2000.00",
                isActive = false,
                createdAt = System.currentTimeMillis()
            )
        )
        
        accounts.forEach { accountDao.insertAccount(it) }
        
        val allAccounts = accountDao.getAllAccounts()
        assertEquals(3, allAccounts.size)
        
        val activeAccounts = accountDao.getActiveAccounts()
        assertEquals(2, activeAccounts.size)
    }
    
    @Test
    fun observeActiveAccounts() = runTest {
        val accounts = listOf(
            AccountEntity(
                bankName = "Bank 1",
                accountType = "SAVINGS",
                accountNumber = "1111111111",
                nickname = "Account 1",
                currentBalance = "1000.00",
                isActive = true,
                createdAt = System.currentTimeMillis()
            ),
            AccountEntity(
                bankName = "Bank 2",
                accountType = "SAVINGS",
                accountNumber = "2222222222",
                nickname = "Account 2",
                currentBalance = "2000.00",
                isActive = false,
                createdAt = System.currentTimeMillis()
            )
        )
        
        accounts.forEach { accountDao.insertAccount(it) }
        
        val activeAccounts = accountDao.observeActiveAccounts().first()
        assertEquals(1, activeAccounts.size)
        assertEquals("Account 1", activeAccounts[0].nickname)
    }
    
    @Test
    fun getAccountsByBank() = runTest {
        val accounts = listOf(
            AccountEntity(
                bankName = "HDFC Bank",
                accountType = "SAVINGS",
                accountNumber = "1111111111",
                nickname = "HDFC Savings",
                currentBalance = "5000.00",
                isActive = true,
                createdAt = System.currentTimeMillis()
            ),
            AccountEntity(
                bankName = "HDFC Bank",
                accountType = "CREDIT_CARD",
                accountNumber = "2222222222",
                nickname = "HDFC Credit",
                currentBalance = "-1000.00",
                isActive = true,
                createdAt = System.currentTimeMillis()
            ),
            AccountEntity(
                bankName = "ICICI Bank",
                accountType = "SAVINGS",
                accountNumber = "3333333333",
                nickname = "ICICI Savings",
                currentBalance = "3000.00",
                isActive = true,
                createdAt = System.currentTimeMillis()
            )
        )
        
        accounts.forEach { accountDao.insertAccount(it) }
        
        val hdfcAccounts = accountDao.getAccountsByBank("HDFC Bank")
        assertEquals(2, hdfcAccounts.size)
        
        val iciciAccounts = accountDao.getAccountsByBank("ICICI Bank")
        assertEquals(1, iciciAccounts.size)
    }
    
    @Test
    fun getAccountsByType() = runTest {
        val accounts = listOf(
            AccountEntity(
                bankName = "Bank 1",
                accountType = "SAVINGS",
                accountNumber = "1111111111",
                nickname = "Savings 1",
                currentBalance = "1000.00",
                isActive = true,
                createdAt = System.currentTimeMillis()
            ),
            AccountEntity(
                bankName = "Bank 2",
                accountType = "SAVINGS",
                accountNumber = "2222222222",
                nickname = "Savings 2",
                currentBalance = "2000.00",
                isActive = true,
                createdAt = System.currentTimeMillis()
            ),
            AccountEntity(
                bankName = "Bank 3",
                accountType = "CREDIT_CARD",
                accountNumber = "3333333333",
                nickname = "Credit Card",
                currentBalance = "-500.00",
                isActive = true,
                createdAt = System.currentTimeMillis()
            )
        )
        
        accounts.forEach { accountDao.insertAccount(it) }
        
        val savingsAccounts = accountDao.getAccountsByType("SAVINGS")
        assertEquals(2, savingsAccounts.size)
        
        val creditAccounts = accountDao.getAccountsByType("CREDIT_CARD")
        assertEquals(1, creditAccounts.size)
    }
    
    @Test
    fun updateAccount() = runTest {
        val account = AccountEntity(
            bankName = "Test Bank",
            accountType = "SAVINGS",
            accountNumber = "1234567890",
            nickname = "Original Name",
            currentBalance = "1000.00",
            isActive = true,
            createdAt = System.currentTimeMillis()
        )
        
        val accountId = accountDao.insertAccount(account)
        
        val updatedAccount = account.copy(
            id = accountId,
            nickname = "Updated Name",
            currentBalance = "1500.00"
        )
        
        accountDao.updateAccount(updatedAccount)
        
        val retrievedAccount = accountDao.getAccountById(accountId)
        assertEquals("Updated Name", retrievedAccount?.nickname)
        assertEquals("1500.00", retrievedAccount?.currentBalance)
    }
    
    @Test
    fun updateAccountStatus() = runTest {
        val account = AccountEntity(
            bankName = "Test Bank",
            accountType = "SAVINGS",
            accountNumber = "1234567890",
            nickname = "Test Account",
            currentBalance = "1000.00",
            isActive = true,
            createdAt = System.currentTimeMillis()
        )
        
        val accountId = accountDao.insertAccount(account)
        
        // Deactivate account
        accountDao.updateAccountStatus(accountId, false)
        
        val retrievedAccount = accountDao.getAccountById(accountId)
        assertFalse(retrievedAccount?.isActive ?: true)
    }
    
    @Test
    fun updateAccountBalance() = runTest {
        val account = AccountEntity(
            bankName = "Test Bank",
            accountType = "SAVINGS",
            accountNumber = "1234567890",
            nickname = "Test Account",
            currentBalance = "1000.00",
            isActive = true,
            createdAt = System.currentTimeMillis()
        )
        
        val accountId = accountDao.insertAccount(account)
        
        // Update balance
        accountDao.updateAccountBalance(accountId, "2500.75")
        
        val retrievedAccount = accountDao.getAccountById(accountId)
        assertEquals("2500.75", retrievedAccount?.currentBalance)
    }
    
    @Test
    fun deleteAccount() = runTest {
        val account = AccountEntity(
            bankName = "Test Bank",
            accountType = "SAVINGS",
            accountNumber = "1234567890",
            nickname = "Test Account",
            currentBalance = "1000.00",
            isActive = true,
            createdAt = System.currentTimeMillis()
        )
        
        val accountId = accountDao.insertAccount(account)
        
        // Verify account exists
        assertNotNull(accountDao.getAccountById(accountId))
        
        // Delete account
        accountDao.deleteAccountById(accountId)
        
        // Verify account is deleted
        assertNull(accountDao.getAccountById(accountId))
    }
    
    @Test
    fun getActiveAccountCount() = runTest {
        val accounts = listOf(
            AccountEntity(
                bankName = "Bank 1",
                accountType = "SAVINGS",
                accountNumber = "1111111111",
                nickname = "Account 1",
                currentBalance = "1000.00",
                isActive = true,
                createdAt = System.currentTimeMillis()
            ),
            AccountEntity(
                bankName = "Bank 2",
                accountType = "SAVINGS",
                accountNumber = "2222222222",
                nickname = "Account 2",
                currentBalance = "2000.00",
                isActive = true,
                createdAt = System.currentTimeMillis()
            ),
            AccountEntity(
                bankName = "Bank 3",
                accountType = "SAVINGS",
                accountNumber = "3333333333",
                nickname = "Account 3",
                currentBalance = "3000.00",
                isActive = false,
                createdAt = System.currentTimeMillis()
            )
        )
        
        accounts.forEach { accountDao.insertAccount(it) }
        
        val activeCount = accountDao.getActiveAccountCount()
        assertEquals(2, activeCount)
    }
    
    @Test
    fun getTotalBalance() = runTest {
        val accounts = listOf(
            AccountEntity(
                bankName = "Bank 1",
                accountType = "SAVINGS",
                accountNumber = "1111111111",
                nickname = "Savings Account",
                currentBalance = "1000.50",
                isActive = true,
                createdAt = System.currentTimeMillis()
            ),
            AccountEntity(
                bankName = "Bank 2",
                accountType = "CHECKING",
                accountNumber = "2222222222",
                nickname = "Checking Account",
                currentBalance = "2500.75",
                isActive = true,
                createdAt = System.currentTimeMillis()
            ),
            AccountEntity(
                bankName = "Bank 3",
                accountType = "CREDIT_CARD",
                accountNumber = "3333333333",
                nickname = "Credit Card",
                currentBalance = "-500.00",
                isActive = true,
                createdAt = System.currentTimeMillis()
            ),
            AccountEntity(
                bankName = "Bank 4",
                accountType = "SAVINGS",
                accountNumber = "4444444444",
                nickname = "Inactive Account",
                currentBalance = "5000.00",
                isActive = false,
                createdAt = System.currentTimeMillis()
            )
        )
        
        accounts.forEach { accountDao.insertAccount(it) }
        
        // Total balance should exclude credit cards and inactive accounts
        val totalBalance = accountDao.getTotalBalance()
        assertEquals(3501.25, totalBalance ?: 0.0, 0.01)
        
        // Credit card balance
        val creditBalance = accountDao.getTotalCreditCardBalance()
        assertEquals(-500.0, creditBalance ?: 0.0, 0.01)
    }
    
    @Test
    fun searchAccountsByNumber() = runTest {
        val accounts = listOf(
            AccountEntity(
                bankName = "Bank 1",
                accountType = "SAVINGS",
                accountNumber = "1234567890",
                nickname = "Account 1",
                currentBalance = "1000.00",
                isActive = true,
                createdAt = System.currentTimeMillis()
            ),
            AccountEntity(
                bankName = "Bank 2",
                accountType = "SAVINGS",
                accountNumber = "9876543210",
                nickname = "Account 2",
                currentBalance = "2000.00",
                isActive = true,
                createdAt = System.currentTimeMillis()
            ),
            AccountEntity(
                bankName = "Bank 3",
                accountType = "SAVINGS",
                accountNumber = "1111222233",
                nickname = "Account 3",
                currentBalance = "3000.00",
                isActive = true,
                createdAt = System.currentTimeMillis()
            )
        )
        
        accounts.forEach { accountDao.insertAccount(it) }
        
        // Search for accounts containing "1234"
        val searchResults = accountDao.searchAccountsByNumber("1234")
        assertEquals(1, searchResults.size)
        assertEquals("1234567890", searchResults[0].accountNumber)
        
        // Search for accounts containing "1111"
        val searchResults2 = accountDao.searchAccountsByNumber("1111")
        assertEquals(1, searchResults2.size)
        assertEquals("1111222233", searchResults2[0].accountNumber)
    }
}