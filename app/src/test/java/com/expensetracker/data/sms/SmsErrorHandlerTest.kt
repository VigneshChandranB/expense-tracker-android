package com.expensetracker.data.sms

import com.expensetracker.domain.error.ErrorResult
import com.expensetracker.domain.error.ErrorType
import com.expensetracker.domain.model.SmsMessage
import com.expensetracker.domain.model.Transaction
import com.expensetracker.domain.model.TransactionType
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal
import java.time.LocalDateTime

class SmsErrorHandlerTest {
    
    private lateinit var smsErrorHandler: SmsErrorHandler
    
    @Before
    fun setup() {
        smsErrorHandler = SmsErrorHandler()
    }
    
    @Test
    fun `processWithRetry should succeed on first attempt`() = runTest {
        val smsMessage = createValidSmsMessage()
        val expectedTransaction = createSampleTransaction()
        
        val result = smsErrorHandler.processWithRetry(smsMessage) { expectedTransaction }
        
        assertTrue(result is ErrorResult.Success)
        assertEquals(expectedTransaction, (result as ErrorResult.Success).data)
    }
    
    @Test
    fun `processWithRetry should retry on failure and eventually succeed`() = runTest {
        val smsMessage = createValidSmsMessage()
        val expectedTransaction = createSampleTransaction()
        var attemptCount = 0
        
        val result = smsErrorHandler.processWithRetry(smsMessage) {
            attemptCount++
            if (attemptCount < 3) {
                throw RuntimeException("Temporary failure")
            }
            expectedTransaction
        }
        
        assertTrue(result is ErrorResult.Success)
        assertEquals(expectedTransaction, (result as ErrorResult.Success).data)
        assertEquals(3, attemptCount)
    }
    
    @Test
    fun `parseAmountWithFallback should parse amount with first pattern`() {
        val text = "Amount debited: Rs 1,500.00 from your account"
        val patterns = listOf(
            Regex("""Rs\s*([\d,]+\.?\d*)"""),
            Regex("""INR\s*([\d,]+\.?\d*)""")
        )
        
        val result = smsErrorHandler.parseAmountWithFallback(text, patterns)
        
        assertTrue(result is ErrorResult.Success)
        assertEquals(1500.0, (result as ErrorResult.Success).data, 0.01)
    }
    
    @Test
    fun `parseAmountWithFallback should use fallback pattern when first fails`() {
        val text = "Debited INR 2,000.50 from account"
        val patterns = listOf(
            Regex("""Rs\s*([\d,]+\.?\d*)"""), // This will fail
            Regex("""INR\s*([\d,]+\.?\d*)""") // This will succeed
        )
        
        val result = smsErrorHandler.parseAmountWithFallback(text, patterns)
        
        assertTrue(result is ErrorResult.Success)
        assertEquals(2000.5, (result as ErrorResult.Success).data, 0.01)
    }
    
    @Test
    fun `parseAmountWithFallback should use manual extraction when patterns fail`() {
        val text = "Transaction of 3500.75 completed successfully"
        val patterns = listOf(
            Regex("""Rs\s*([\d,]+\.?\d*)"""),
            Regex("""INR\s*([\d,]+\.?\d*)""")
        )
        
        val result = smsErrorHandler.parseAmountWithFallback(text, patterns)
        
        assertTrue(result is ErrorResult.Success)
        assertEquals(3500.75, (result as ErrorResult.Success).data, 0.01)
    }
    
    @Test
    fun `parseAmountWithFallback should return error when no amount found`() {
        val text = "No amount information in this message"
        val patterns = listOf(
            Regex("""Rs\s*([\d,]+\.?\d*)"""),
            Regex("""INR\s*([\d,]+\.?\d*)""")
        )
        
        val result = smsErrorHandler.parseAmountWithFallback(text, patterns)
        
        assertTrue(result is ErrorResult.Error)
        assertEquals(ErrorType.SmsError.AmountParsingFailed, (result as ErrorResult.Error).errorType)
    }
    
    @Test
    fun `extractMerchantWithFallback should extract merchant with first pattern`() {
        val text = "Payment to AMAZON INDIA successful"
        val patterns = listOf(
            Regex("""to\s+([A-Z][A-Z0-9\s]{2,20})""", RegexOption.IGNORE_CASE),
            Regex("""at\s+([A-Z][A-Z0-9\s]{2,20})""", RegexOption.IGNORE_CASE)
        )
        
        val result = smsErrorHandler.extractMerchantWithFallback(text, patterns)
        
        assertTrue(result is ErrorResult.Success)
        assertEquals("AMAZON INDIA", (result as ErrorResult.Success).data)
    }
    
    @Test
    fun `extractMerchantWithFallback should use fallback when patterns fail`() {
        val text = "Transaction at STARBUCKS completed"
        val patterns = listOf(
            Regex("""to\s+([A-Z][A-Z0-9\s]{2,20})""", RegexOption.IGNORE_CASE) // This will fail
        )
        
        val result = smsErrorHandler.extractMerchantWithFallback(text, patterns)
        
        assertTrue(result is ErrorResult.Success)
        assertEquals("STARBUCKS", (result as ErrorResult.Success).data)
    }
    
    @Test
    fun `extractMerchantWithFallback should return unknown merchant when no pattern matches`() {
        val text = "Transaction completed successfully"
        val patterns = listOf(
            Regex("""to\s+([A-Z][A-Z0-9\s]{2,20})""", RegexOption.IGNORE_CASE),
            Regex("""at\s+([A-Z][A-Z0-9\s]{2,20})""", RegexOption.IGNORE_CASE)
        )
        
        val result = smsErrorHandler.extractMerchantWithFallback(text, patterns)
        
        assertTrue(result is ErrorResult.Success)
        assertEquals("Unknown Merchant", (result as ErrorResult.Success).data)
    }
    
    @Test
    fun `validateSmsMessage should succeed for valid SMS`() {
        val validSms = createValidSmsMessage()
        
        val result = smsErrorHandler.validateSmsMessage(validSms)
        
        assertTrue(result is ErrorResult.Success)
        assertEquals(validSms, (result as ErrorResult.Success).data)
    }
    
    @Test
    fun `validateSmsMessage should fail for empty SMS body`() {
        val emptySms = SmsMessage(
            id = "1",
            sender = "BANK",
            body = "",
            timestamp = LocalDateTime.now()
        )
        
        val result = smsErrorHandler.validateSmsMessage(emptySms)
        
        assertTrue(result is ErrorResult.Error)
        assertEquals(ErrorType.SmsError.InvalidFormat, (result as ErrorResult.Error).errorType)
    }
    
    @Test
    fun `validateSmsMessage should fail for SMS without financial keywords`() {
        val nonFinancialSms = SmsMessage(
            id = "1",
            sender = "FRIEND",
            body = "Hey, how are you doing today?",
            timestamp = LocalDateTime.now()
        )
        
        val result = smsErrorHandler.validateSmsMessage(nonFinancialSms)
        
        assertTrue(result is ErrorResult.Error)
        assertEquals(ErrorType.SmsError.InvalidFormat, (result as ErrorResult.Error).errorType)
    }
    
    @Test
    fun `validateSmsMessage should succeed for SMS with financial keywords`() {
        val financialSms = SmsMessage(
            id = "1",
            sender = "BANK",
            body = "Amount debited from your account for payment to merchant",
            timestamp = LocalDateTime.now()
        )
        
        val result = smsErrorHandler.validateSmsMessage(financialSms)
        
        assertTrue(result is ErrorResult.Success)
    }
    
    @Test
    fun `logSmsError should not throw exception`() {
        val smsMessage = createValidSmsMessage()
        val error = ErrorResult.Error(
            ErrorType.SmsError.AmountParsingFailed,
            "Test error message",
            cause = RuntimeException("Test cause")
        )
        
        // Should not throw any exception
        assertDoesNotThrow {
            smsErrorHandler.logSmsError(smsMessage, error, "Test context")
        }
    }
    
    private fun createValidSmsMessage(): SmsMessage {
        return SmsMessage(
            id = "1",
            sender = "HDFC-BANK",
            body = "Amount Rs 1,500.00 debited from account ending 1234 for payment to AMAZON on 01-Jan-2024",
            timestamp = LocalDateTime.now()
        )
    }
    
    private fun createSampleTransaction(): Transaction {
        return Transaction(
            id = 1L,
            amount = BigDecimal("1500.00"),
            type = TransactionType.EXPENSE,
            categoryId = 1L,
            accountId = 1L,
            merchant = "AMAZON",
            description = "Online purchase",
            date = LocalDateTime.now(),
            source = com.expensetracker.domain.model.TransactionSource.SMS_AUTO,
            transferAccountId = null,
            transferTransactionId = null,
            isRecurring = false
        )
    }
    
    private fun assertDoesNotThrow(block: () -> Unit) {
        try {
            block()
        } catch (e: Exception) {
            fail("Expected no exception but got: ${e.message}")
        }
    }
  