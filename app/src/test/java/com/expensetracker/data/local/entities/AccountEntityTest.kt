package com.expensetracker.data.local.entities

import org.junit.Test
import org.junit.Assert.*

/**
 * Unit tests for AccountEntity data class
 */
class AccountEntityTest {
    
    @Test
    fun `account entity creation with all fields`() {
        val account = AccountEntity(
            id = 1L,
            bankName = "HDFC Bank",
            accountType = "SAVINGS",
            accountNumber = "1234567890",
            nickname = "Primary Savings",
            currentBalance = "10000.50",
            isActive = true,
            createdAt = System.currentTimeMillis()
        )
        
        assertEquals(1L, account.id)
        assertEquals("HDFC Bank", account.bankName)
        assertEquals("SAVINGS", account.accountType)
        assertEquals("1234567890", account.accountNumber)
        assertEquals("Primary Savings", account.nickname)
        assertEquals("10000.50", account.currentBalance)
        assertTrue(account.isActive)
    }
    
    @Test
    fun `account entity with credit card type`() {
        val account = AccountEntity(
            id = 2L,
            bankName = "ICICI Bank",
            accountType = "CREDIT_CARD",
            accountNumber = "4567890123456789",
            nickname = "Travel Card",
            currentBalance = "-2500.00",
            isActive = true,
            createdAt = System.currentTimeMillis()
        )
        
        assertEquals("CREDIT_CARD", account.accountType)
        assertEquals("-2500.00", account.currentBalance)
    }
    
    @Test
    fun `account entity with inactive status`() {
        val account = AccountEntity(
            id = 3L,
            bankName = "SBI",
            accountType = "CHECKING",
            accountNumber = "9876543210",
            nickname = "Old Account",
            currentBalance = "0.00",
            isActive = false,
            createdAt = System.currentTimeMillis()
        )
        
        assertFalse(account.isActive)
        assertEquals("0.00", account.currentBalance)
    }
    
    @Test
    fun `account entity with default id`() {
        val account = AccountEntity(
            bankName = "Axis Bank",
            accountType = "INVESTMENT",
            accountNumber = "1111222233334444",
            nickname = "Investment Account",
            currentBalance = "50000.00",
            isActive = true,
            createdAt = System.currentTimeMillis()
        )
        
        assertEquals(0L, account.id) // Default value
    }
}