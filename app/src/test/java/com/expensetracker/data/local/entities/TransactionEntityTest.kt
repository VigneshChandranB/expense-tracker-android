package com.expensetracker.data.local.entities

import org.junit.Test
import org.junit.Assert.*

/**
 * Unit tests for TransactionEntity data class
 */
class TransactionEntityTest {
    
    @Test
    fun `transaction entity creation with all fields`() {
        val transaction = TransactionEntity(
            id = 1L,
            amount = "100.50",
            type = "EXPENSE",
            categoryId = 1L,
            accountId = 1L,
            merchant = "Amazon",
            description = "Online shopping",
            date = System.currentTimeMillis(),
            source = "SMS_AUTO",
            transferAccountId = null,
            transferTransactionId = null,
            isRecurring = false,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
        
        assertEquals(1L, transaction.id)
        assertEquals("100.50", transaction.amount)
        assertEquals("EXPENSE", transaction.type)
        assertEquals(1L, transaction.categoryId)
        assertEquals(1L, transaction.accountId)
        assertEquals("Amazon", transaction.merchant)
        assertEquals("Online shopping", transaction.description)
        assertEquals("SMS_AUTO", transaction.source)
        assertFalse(transaction.isRecurring)
    }
    
    @Test
    fun `transaction entity with transfer fields`() {
        val transaction = TransactionEntity(
            id = 1L,
            amount = "500.00",
            type = "TRANSFER_OUT",
            categoryId = 1L,
            accountId = 1L,
            merchant = "Transfer",
            description = "Transfer to savings",
            date = System.currentTimeMillis(),
            source = "MANUAL",
            transferAccountId = 2L,
            transferTransactionId = 2L,
            isRecurring = false,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
        
        assertEquals("TRANSFER_OUT", transaction.type)
        assertEquals(2L, transaction.transferAccountId)
        assertEquals(2L, transaction.transferTransactionId)
    }
    
    @Test
    fun `transaction entity with minimal fields`() {
        val transaction = TransactionEntity(
            amount = "50.00",
            type = "INCOME",
            categoryId = 1L,
            accountId = 1L,
            merchant = "Salary",
            description = null,
            date = System.currentTimeMillis(),
            source = "MANUAL",
            transferAccountId = null,
            transferTransactionId = null,
            isRecurring = false,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
        
        assertEquals(0L, transaction.id) // Default value
        assertNull(transaction.description)
        assertNull(transaction.transferAccountId)
        assertNull(transaction.transferTransactionId)
    }
}