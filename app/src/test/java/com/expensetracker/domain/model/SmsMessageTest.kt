package com.expensetracker.domain.model

import org.junit.Assert.*
import org.junit.Test
import java.util.Date

/**
 * Unit tests for SmsMessage domain model
 */
class SmsMessageTest {
    
    @Test
    fun `isPotentialTransactionSms returns true for debit message`() {
        // Given
        val smsMessage = SmsMessage(
            id = 1L,
            sender = "HDFC-BANK",
            body = "Your account has been debited with Rs. 1500.00 at AMAZON",
            timestamp = Date(),
            type = SmsMessage.Type.RECEIVED
        )
        
        // When
        val result = smsMessage.isPotentialTransactionSms()
        
        // Then
        assertTrue(result)
    }
    
    @Test
    fun `isPotentialTransactionSms returns true for credit message`() {
        // Given
        val smsMessage = SmsMessage(
            id = 1L,
            sender = "ICICI-BANK",
            body = "Your account has been credited with Rs. 5000.00 salary",
            timestamp = Date(),
            type = SmsMessage.Type.RECEIVED
        )
        
        // When
        val result = smsMessage.isPotentialTransactionSms()
        
        // Then
        assertTrue(result)
    }
    
    @Test
    fun `isPotentialTransactionSms returns true for UPI transaction`() {
        // Given
        val smsMessage = SmsMessage(
            id = 1L,
            sender = "PAYTM",
            body = "UPI transaction successful. Rs. 250 paid to Swiggy",
            timestamp = Date(),
            type = SmsMessage.Type.RECEIVED
        )
        
        // When
        val result = smsMessage.isPotentialTransactionSms()
        
        // Then
        assertTrue(result)
    }
    
    @Test
    fun `isPotentialTransactionSms returns false for non-transaction message`() {
        // Given
        val smsMessage = SmsMessage(
            id = 1L,
            sender = "FRIEND",
            body = "Hey, how are you doing today?",
            timestamp = Date(),
            type = SmsMessage.Type.RECEIVED
        )
        
        // When
        val result = smsMessage.isPotentialTransactionSms()
        
        // Then
        assertFalse(result)
    }
    
    @Test
    fun `isFromBankOrFinancialInstitution returns true for HDFC bank`() {
        // Given
        val smsMessage = SmsMessage(
            id = 1L,
            sender = "HDFC-BANK",
            body = "Transaction alert",
            timestamp = Date(),
            type = SmsMessage.Type.RECEIVED
        )
        
        // When
        val result = smsMessage.isFromBankOrFinancialInstitution()
        
        // Then
        assertTrue(result)
    }
    
    @Test
    fun `isFromBankOrFinancialInstitution returns true for PayTM`() {
        // Given
        val smsMessage = SmsMessage(
            id = 1L,
            sender = "PAYTM",
            body = "Payment successful",
            timestamp = Date(),
            type = SmsMessage.Type.RECEIVED
        )
        
        // When
        val result = smsMessage.isFromBankOrFinancialInstitution()
        
        // Then
        assertTrue(result)
    }
    
    @Test
    fun `isFromBankOrFinancialInstitution returns false for regular contact`() {
        // Given
        val smsMessage = SmsMessage(
            id = 1L,
            sender = "JOHN",
            body = "Hello there",
            timestamp = Date(),
            type = SmsMessage.Type.RECEIVED
        )
        
        // When
        val result = smsMessage.isFromBankOrFinancialInstitution()
        
        // Then
        assertFalse(result)
    }
    
    @Test
    fun `isFromBankOrFinancialInstitution is case insensitive`() {
        // Given
        val smsMessage = SmsMessage(
            id = 1L,
            sender = "hdfc-bank",
            body = "Transaction alert",
            timestamp = Date(),
            type = SmsMessage.Type.RECEIVED
        )
        
        // When
        val result = smsMessage.isFromBankOrFinancialInstitution()
        
        // Then
        assertTrue(result)
    }
    
    @Test
    fun `isPotentialTransactionSms is case insensitive`() {
        // Given
        val smsMessage = SmsMessage(
            id = 1L,
            sender = "BANK",
            body = "Your account has been DEBITED with Rs. 100",
            timestamp = Date(),
            type = SmsMessage.Type.RECEIVED
        )
        
        // When
        val result = smsMessage.isPotentialTransactionSms()
        
        // Then
        assertTrue(result)
    }
}