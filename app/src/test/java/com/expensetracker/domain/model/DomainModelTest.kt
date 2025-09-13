package com.expensetracker.domain.model

import org.junit.Test
import org.junit.Assert.*
import java.math.BigDecimal
import java.time.LocalDateTime

/**
 * Unit tests for domain models
 */
class DomainModelTest {
    
    @Test
    fun `transaction domain model creation`() {
        val transaction = Transaction(
            id = 1L,
            amount = BigDecimal("100.50"),
            type = TransactionType.EXPENSE,
            category = Category(
                id = 1L,
                name = "Food",
                icon = "restaurant",
                color = "#FF9800",
                isDefault = true
            ),
            merchant = "McDonald's",
            description = "Lunch",
            date = LocalDateTime.now(),
            source = TransactionSource.SMS_AUTO,
            accountId = 1L,
            transferAccountId = null,
            transferTransactionId = null,
            isRecurring = false
        )
        
        assertEquals(1L, transaction.id)
        assertEquals(BigDecimal("100.50"), transaction.amount)
        assertEquals(TransactionType.EXPENSE, transaction.type)
        assertEquals("McDonald's", transaction.merchant)
        assertEquals(TransactionSource.SMS_AUTO, transaction.source)
        assertFalse(transaction.isRecurring)
    }
    
    @Test
    fun `account domain model creation`() {
        val account = Account(
            id = 1L,
            bankName = "HDFC Bank",
            accountType = AccountType.SAVINGS,
            accountNumber = "1234567890",
            nickname = "Primary Savings",
            currentBalance = BigDecimal("10000.50"),
            isActive = true,
            createdAt = LocalDateTime.now()
        )
        
        assertEquals(1L, account.id)
        assertEquals("HDFC Bank", account.bankName)
        assertEquals(AccountType.SAVINGS, account.accountType)
        assertEquals("1234567890", account.accountNumber)
        assertEquals("Primary Savings", account.nickname)
        assertEquals(BigDecimal("10000.50"), account.currentBalance)
        assertTrue(account.isActive)
    }
    
    @Test
    fun `category domain model creation`() {
        val parentCategory = Category(
            id = 1L,
            name = "Food & Dining",
            icon = "restaurant",
            color = "#FF9800",
            isDefault = true
        )
        
        val subCategory = Category(
            id = 2L,
            name = "Fast Food",
            icon = "fastfood",
            color = "#FF5722",
            isDefault = false,
            parentCategory = parentCategory
        )
        
        assertEquals("Food & Dining", parentCategory.name)
        assertTrue(parentCategory.isDefault)
        assertNull(parentCategory.parentCategory)
        
        assertEquals("Fast Food", subCategory.name)
        assertFalse(subCategory.isDefault)
        assertNotNull(subCategory.parentCategory)
        assertEquals(parentCategory, subCategory.parentCategory)
    }
    
    @Test
    fun `sms pattern domain model creation`() {
        val smsPattern = SmsPattern(
            id = 1L,
            bankName = "HDFC Bank",
            senderPattern = "HDFCBK",
            amountPattern = "Rs\\.([0-9,]+\\.?[0-9]*)",
            merchantPattern = "at ([A-Z0-9\\s]+)",
            datePattern = "on ([0-9]{2}-[0-9]{2}-[0-9]{4})",
            typePattern = "(debited|credited)",
            accountPattern = "A/c \\*([0-9]{4})",
            isActive = true
        )
        
        assertEquals(1L, smsPattern.id)
        assertEquals("HDFC Bank", smsPattern.bankName)
        assertEquals("HDFCBK", smsPattern.senderPattern)
        assertEquals("Rs\\.([0-9,]+\\.?[0-9]*)", smsPattern.amountPattern)
        assertTrue(smsPattern.isActive)
        assertNotNull(smsPattern.accountPattern)
    }
    
    @Test
    fun `transaction type enum values`() {
        val types = TransactionType.values()
        assertEquals(4, types.size)
        assertTrue(types.contains(TransactionType.INCOME))
        assertTrue(types.contains(TransactionType.EXPENSE))
        assertTrue(types.contains(TransactionType.TRANSFER_OUT))
        assertTrue(types.contains(TransactionType.TRANSFER_IN))
    }
    
    @Test
    fun `account type enum values`() {
        val types = AccountType.values()
        assertEquals(5, types.size)
        assertTrue(types.contains(AccountType.SAVINGS))
        assertTrue(types.contains(AccountType.CHECKING))
        assertTrue(types.contains(AccountType.CREDIT_CARD))
        assertTrue(types.contains(AccountType.INVESTMENT))
        assertTrue(types.contains(AccountType.CASH))
    }
    
    @Test
    fun `transaction source enum values`() {
        val sources = TransactionSource.values()
        assertEquals(3, sources.size)
        assertTrue(sources.contains(TransactionSource.SMS_AUTO))
        assertTrue(sources.contains(TransactionSource.MANUAL))
        assertTrue(sources.contains(TransactionSource.IMPORTED))
    }
    
    @Test
    fun `transfer transaction creation`() {
        val transferOut = Transaction(
            id = 1L,
            amount = BigDecimal("500.00"),
            type = TransactionType.TRANSFER_OUT,
            category = Category(
                id = 10L,
                name = "Transfer",
                icon = "swap_horiz",
                color = "#607D8B",
                isDefault = true
            ),
            merchant = "Transfer",
            description = "Transfer to savings account",
            date = LocalDateTime.now(),
            source = TransactionSource.MANUAL,
            accountId = 1L,
            transferAccountId = 2L,
            transferTransactionId = 2L,
            isRecurring = false
        )
        
        assertEquals(TransactionType.TRANSFER_OUT, transferOut.type)
        assertEquals(2L, transferOut.transferAccountId)
        assertEquals(2L, transferOut.transferTransactionId)
        assertEquals("Transfer", transferOut.merchant)
    }
}