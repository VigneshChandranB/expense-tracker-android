package com.expensetracker.data.local.entities

import org.junit.Test
import org.junit.Assert.*

/**
 * Unit tests for SmsPatternEntity data class
 */
class SmsPatternEntityTest {
    
    @Test
    fun `sms pattern entity creation with all fields`() {
        val smsPattern = SmsPatternEntity(
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
        assertEquals("at ([A-Z0-9\\s]+)", smsPattern.merchantPattern)
        assertEquals("on ([0-9]{2}-[0-9]{2}-[0-9]{4})", smsPattern.datePattern)
        assertEquals("(debited|credited)", smsPattern.typePattern)
        assertEquals("A/c \\*([0-9]{4})", smsPattern.accountPattern)
        assertTrue(smsPattern.isActive)
    }
    
    @Test
    fun `sms pattern entity without account pattern`() {
        val smsPattern = SmsPatternEntity(
            id = 2L,
            bankName = "ICICI Bank",
            senderPattern = "ICICI",
            amountPattern = "INR ([0-9,]+\\.?[0-9]*)",
            merchantPattern = "to ([A-Z\\s]+)",
            datePattern = "([0-9]{2}/[0-9]{2}/[0-9]{4})",
            typePattern = "(Dr|Cr)",
            accountPattern = null,
            isActive = true
        )
        
        assertEquals("ICICI Bank", smsPattern.bankName)
        assertNull(smsPattern.accountPattern)
        assertTrue(smsPattern.isActive)
    }
    
    @Test
    fun `sms pattern entity with inactive status`() {
        val smsPattern = SmsPatternEntity(
            id = 3L,
            bankName = "Old Bank",
            senderPattern = "OLDBNK",
            amountPattern = "Amount: ([0-9,]+)",
            merchantPattern = "Merchant: ([A-Z\\s]+)",
            datePattern = "Date: ([0-9]{2}-[0-9]{2}-[0-9]{4})",
            typePattern = "(debit|credit)",
            accountPattern = null,
            isActive = false
        )
        
        assertFalse(smsPattern.isActive)
        assertEquals("Old Bank", smsPattern.bankName)
    }
    
    @Test
    fun `sms pattern entity with default id`() {
        val smsPattern = SmsPatternEntity(
            bankName = "SBI",
            senderPattern = "SBIINB",
            amountPattern = "Rs ([0-9,]+\\.?[0-9]*)",
            merchantPattern = "at ([A-Z0-9\\s]+)",
            datePattern = "on ([0-9]{2}-[A-Z]{3}-[0-9]{2})",
            typePattern = "(debited|credited)",
            accountPattern = "XX([0-9]{4})",
            isActive = true
        )
        
        assertEquals(0L, smsPattern.id) // Default value
        assertEquals("SBI", smsPattern.bankName)
    }
}