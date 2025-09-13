package com.expensetracker.data.sms

import com.expensetracker.domain.model.*
import io.mockk.*
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.*
import org.junit.Assert.*

class SmsTransactionExtractorTest {

    private lateinit var bankPatternRegistry: BankPatternRegistry
    private lateinit var accountMappingService: AccountMappingService
    private lateinit var extractor: RegexSmsTransactionExtractor

    @Before
    fun setup() {
        bankPatternRegistry = mockk()
        accountMappingService = mockk()
        extractor = RegexSmsTransactionExtractor(bankPatternRegistry, accountMappingService)
    }

    @Test
    fun `should extract HDFC bank transaction successfully`() = runTest {
        // Given
        val smsMessage = SmsMessage(
            id = 1,
            sender = "VK-HDFCBK",
            body = "Rs.1500.00 debited from A/c no XXXX1234 at AMAZON on 15-01-2024 14:30:25. Avl Bal: Rs.25000.00",
            timestamp = Date()
        )
        
        val hdfcPattern = SmsPattern(
            id = 1,
            bankName = "HDFC Bank",
            senderPattern = ".*HDFC.*|.*VK-HDFCBK.*",
            amountPattern = "Rs\\.([\\d,]+(?:\\.\\d{2})?)",
            merchantPattern = "at\\s+([A-Za-z0-9\\s]+?)(?:\\s+on|\\.|$)",
            datePattern = "(\\d{2}-\\d{2}-\\d{4}\\s+\\d{2}:\\d{2}:\\d{2})",
            typePattern = "(debited|credited)",
            accountPattern = "A/c\\s+no\\s+([X\\d]+)",
            isActive = true
        )

        every { bankPatternRegistry.findPatternBySender("VK-HDFCBK") } returns hdfcPattern
        coEvery { accountMappingService.findAccountByIdentifier("HDFC Bank", "XXXX1234") } returns 1L

        // When
        val result = extractor.extractTransaction(smsMessage)

        // Then
        assertTrue(result.isSuccessful)
        assertNotNull(result.transaction)
        assertEquals(BigDecimal("1500.00"), result.transaction!!.amount)
        assertEquals(TransactionType.EXPENSE, result.transaction!!.type)
        assertEquals("AMAZON", result.transaction!!.merchant)
        assertEquals("XXXX1234", result.transaction!!.accountIdentifier)
        assertTrue(result.confidenceScore > 0.8f)
    }

    @Test
    fun `should extract ICICI bank credit transaction successfully`() = runTest {
        // Given
        val smsMessage = SmsMessage(
            id = 2,
            sender = "ICICIB",
            body = "Rs.5000.00 credited to account ending 5678 from SALARY CREDIT on 01-02-2024 09:15:30",
            timestamp = Date()
        )
        
        val iciciPattern = SmsPattern(
            id = 2,
            bankName = "ICICI Bank",
            senderPattern = ".*ICICI.*|.*ICICIB.*",
            amountPattern = "Rs\\.([\\d,]+(?:\\.\\d{2})?)",
            merchantPattern = "from\\s+([A-Za-z0-9\\s]+?)(?:\\s+on|\\.|$)",
            datePattern = "(\\d{2}-\\d{2}-\\d{4}\\s+\\d{2}:\\d{2}:\\d{2})",
            typePattern = "(debited|credited)",
            accountPattern = "account\\s+ending\\s+([\\d]+)",
            isActive = true
        )

        every { bankPatternRegistry.findPatternBySender("ICICIB") } returns iciciPattern
        coEvery { accountMappingService.findAccountByIdentifier("ICICI Bank", "5678") } returns 2L

        // When
        val result = extractor.extractTransaction(smsMessage)

        // Then
        assertTrue(result.isSuccessful)
        assertNotNull(result.transaction)
        assertEquals(BigDecimal("5000.00"), result.transaction!!.amount)
        assertEquals(TransactionType.INCOME, result.transaction!!.type)
        assertEquals("SALARY CREDIT", result.transaction!!.merchant)
        assertEquals("5678", result.transaction!!.accountIdentifier)
    }

    @Test
    fun `should extract SBI bank transaction with different date format`() = runTest {
        // Given
        val smsMessage = SmsMessage(
            id = 3,
            sender = "SBIIN",
            body = "Rs 2,500 debited from A/c XXXX9876 at GROCERY STORE on 28/01/24 16:45. Balance: Rs 15,000",
            timestamp = Date()
        )
        
        val sbiPattern = SmsPattern(
            id = 3,
            bankName = "State Bank of India",
            senderPattern = ".*SBI.*|.*SBIIN.*",
            amountPattern = "Rs\\s([\\d,]+)",
            merchantPattern = "at\\s+([A-Za-z0-9\\s]+?)(?:\\s+on|\\.|$)",
            datePattern = "(\\d{2}/\\d{2}/\\d{2}\\s+\\d{2}:\\d{2})",
            typePattern = "(debited|credited)",
            accountPattern = "A/c\\s+([X\\d]+)",
            isActive = true
        )

        every { bankPatternRegistry.findPatternBySender("SBIIN") } returns sbiPattern
        coEvery { accountMappingService.findAccountByIdentifier("State Bank of India", "XXXX9876") } returns 3L

        // When
        val result = extractor.extractTransaction(smsMessage)

        // Then
        assertTrue(result.isSuccessful)
        assertNotNull(result.transaction)
        assertEquals(BigDecimal("2500"), result.transaction!!.amount)
        assertEquals(TransactionType.EXPENSE, result.transaction!!.type)
        assertEquals("GROCERY STORE", result.transaction!!.merchant)
    }

    @Test
    fun `should handle UPI transaction from PhonePe`() = runTest {
        // Given
        val smsMessage = SmsMessage(
            id = 4,
            sender = "PHONEPE",
            body = "You paid Rs.299 to NETFLIX via UPI on 15-02-2024. UPI Ref: 123456789",
            timestamp = Date()
        )
        
        val phonePePattern = SmsPattern(
            id = 7,
            bankName = "PhonePe",
            senderPattern = ".*PHONEPE.*",
            amountPattern = "Rs\\.([\\d,]+)",
            merchantPattern = "to\\s+([A-Za-z0-9\\s]+?)(?:\\s+via|\\s+on|\\.|$)",
            datePattern = "(\\d{2}-\\d{2}-\\d{4})",
            typePattern = "(paid|received)",
            isActive = true
        )

        every { bankPatternRegistry.findPatternBySender("PHONEPE") } returns phonePePattern
        coEvery { accountMappingService.findAccountByIdentifier(any(), any()) } returns null

        // When
        val result = extractor.extractTransaction(smsMessage)

        // Then
        assertTrue(result.isSuccessful)
        assertNotNull(result.transaction)
        assertEquals(BigDecimal("299"), result.transaction!!.amount)
        assertEquals(TransactionType.EXPENSE, result.transaction!!.type)
        assertEquals("NETFLIX", result.transaction!!.merchant)
    }

    @Test
    fun `should handle malformed amount gracefully`() = runTest {
        // Given
        val smsMessage = SmsMessage(
            id = 5,
            sender = "VK-HDFCBK",
            body = "Rs.ABC debited from A/c no XXXX1234 at MERCHANT on 15-01-2024",
            timestamp = Date()
        )
        
        val hdfcPattern = SmsPattern(
            id = 1,
            bankName = "HDFC Bank",
            senderPattern = ".*HDFC.*|.*VK-HDFCBK.*",
            amountPattern = "Rs\\.([\\d,]+(?:\\.\\d{2})?)",
            merchantPattern = "at\\s+([A-Za-z0-9\\s]+?)(?:\\s+on|\\.|$)",
            datePattern = "(\\d{2}-\\d{2}-\\d{4})",
            typePattern = "(debited|credited)",
            accountPattern = "A/c\\s+no\\s+([X\\d]+)",
            isActive = true
        )

        every { bankPatternRegistry.findPatternBySender("VK-HDFCBK") } returns hdfcPattern

        // When
        val result = extractor.extractTransaction(smsMessage)

        // Then
        assertFalse(result.isSuccessful)
        assertTrue(result.extractionDetails.failureReason?.contains("validation failed") == true)
    }

    @Test
    fun `should return failure for non-transaction SMS`() = runTest {
        // Given
        val smsMessage = SmsMessage(
            id = 6,
            sender = "FRIEND",
            body = "Hey, how are you doing today?",
            timestamp = Date()
        )

        // When
        val result = extractor.extractTransaction(smsMessage)

        // Then
        assertFalse(result.isSuccessful)
        assertTrue(result.extractionDetails.failureReason?.contains("does not contain transaction keywords") == true)
    }

    @Test
    fun `should return failure for unknown sender`() = runTest {
        // Given
        val smsMessage = SmsMessage(
            id = 7,
            sender = "UNKNOWN-BANK",
            body = "Rs.1000 debited from your account",
            timestamp = Date()
        )

        every { bankPatternRegistry.findPatternBySender("UNKNOWN-BANK") } returns null

        // When
        val result = extractor.extractTransaction(smsMessage)

        // Then
        assertFalse(result.isSuccessful)
        assertTrue(result.extractionDetails.failureReason?.contains("No matching pattern found") == true)
    }

    @Test
    fun `should calculate high confidence score for complete extraction`() = runTest {
        // Given
        val extractionDetails = ExtractionDetails(
            extractedFields = mapOf(
                "amount" to "1500.00",
                "type" to "debited",
                "merchant" to "AMAZON INDIA",
                "date" to "15-01-2024 14:30:25",
                "account" to "XXXX1234"
            ),
            matchedPattern = SmsPattern(
                id = 1,
                bankName = "HDFC Bank",
                senderPattern = ".*HDFC.*",
                amountPattern = "Rs\\.([\\d,]+(?:\\.\\d{2})?)",
                merchantPattern = "at\\s+([A-Za-z0-9\\s]+?)(?:\\s+on|\\.|$)",
                datePattern = "(\\d{2}-\\d{2}-\\d{4}\\s+\\d{2}:\\d{2}:\\d{2})",
                typePattern = "(debited|credited)",
                accountPattern = "A/c\\s+no\\s+([X\\d]+)",
                isActive = true
            ),
            processingTimeMs = 50
        )

        // When
        val confidenceScore = extractor.calculateConfidenceScore(extractionDetails)

        // Then
        assertTrue(confidenceScore > 0.9f)
    }

    @Test
    fun `should calculate low confidence score for incomplete extraction`() = runTest {
        // Given
        val extractionDetails = ExtractionDetails(
            extractedFields = mapOf(
                "amount" to "1500"
            ),
            matchedPattern = null,
            processingTimeMs = 200
        )

        // When
        val confidenceScore = extractor.calculateConfidenceScore(extractionDetails)

        // Then
        assertTrue(confidenceScore < 0.5f)
    }

    @Test
    fun `should register new bank pattern successfully`() = runTest {
        // Given
        val newPattern = SmsPattern(
            id = 10,
            bankName = "New Bank",
            senderPattern = ".*NEWBANK.*",
            amountPattern = "Amount:\\s*Rs\\.([\\d,]+)",
            merchantPattern = "Merchant:\\s+([A-Za-z0-9\\s]+)",
            datePattern = "(\\d{2}-\\d{2}-\\d{4})",
            typePattern = "(debit|credit)",
            isActive = true
        )

        coEvery { bankPatternRegistry.registerPattern(newPattern) } just Runs

        // When
        extractor.registerBankPattern(newPattern)

        // Then
        coVerify { bankPatternRegistry.registerPattern(newPattern) }
    }

    @Test
    fun `should get all bank patterns`() = runTest {
        // Given
        val patterns = listOf(
            SmsPattern(1, "HDFC Bank", ".*HDFC.*", "Rs\\.([\\d,]+)", "at\\s+([A-Za-z0-9\\s]+)", "(\\d{2}-\\d{2}-\\d{4})", "(debited|credited)"),
            SmsPattern(2, "ICICI Bank", ".*ICICI.*", "Rs\\.([\\d,]+)", "at\\s+([A-Za-z0-9\\s]+)", "(\\d{2}-\\d{2}-\\d{4})", "(debited|credited)")
        )

        coEvery { bankPatternRegistry.getAllPatterns() } returns patterns

        // When
        val result = extractor.getBankPatterns()

        // Then
        assertEquals(2, result.size)
        assertEquals("HDFC Bank", result[0].bankName)
        assertEquals("ICICI Bank", result[1].bankName)
    }

    @Test
    fun `should handle transfer transactions correctly`() = runTest {
        // Given
        val smsMessage = SmsMessage(
            id = 8,
            sender = "VK-HDFCBK",
            body = "Rs.10000.00 transferred from A/c XXXX1234 to A/c YYYY5678 on 20-01-2024 10:30:00",
            timestamp = Date()
        )
        
        val hdfcPattern = SmsPattern(
            id = 1,
            bankName = "HDFC Bank",
            senderPattern = ".*HDFC.*|.*VK-HDFCBK.*",
            amountPattern = "Rs\\.([\\d,]+(?:\\.\\d{2})?)",
            merchantPattern = "to\\s+A/c\\s+([X\\d]+)",
            datePattern = "(\\d{2}-\\d{2}-\\d{4}\\s+\\d{2}:\\d{2}:\\d{2})",
            typePattern = "(transferred)",
            accountPattern = "from\\s+A/c\\s+([X\\d]+)",
            isActive = true
        )

        every { bankPatternRegistry.findPatternBySender("VK-HDFCBK") } returns hdfcPattern
        coEvery { accountMappingService.findAccountByIdentifier("HDFC Bank", "XXXX1234") } returns 1L

        // When
        val result = extractor.extractTransaction(smsMessage)

        // Then
        assertTrue(result.isSuccessful)
        assertNotNull(result.transaction)
        assertEquals(BigDecimal("10000.00"), result.transaction!!.amount)
        assertEquals(TransactionType.TRANSFER_OUT, result.transaction!!.type)
    }

    @Test
    fun `should extract transaction with fallback merchant detection`() = runTest {
        // Given
        val smsMessage = SmsMessage(
            id = 9,
            sender = "VK-HDFCBK",
            body = "Rs.500.00 debited from A/c XXXX1234 for payment to SWIGGY DELIVERY on 25-01-2024",
            timestamp = Date()
        )
        
        val hdfcPattern = SmsPattern(
            id = 1,
            bankName = "HDFC Bank",
            senderPattern = ".*HDFC.*|.*VK-HDFCBK.*",
            amountPattern = "Rs\\.([\\d,]+(?:\\.\\d{2})?)",
            merchantPattern = "invalid_pattern_that_wont_match",
            datePattern = "(\\d{2}-\\d{2}-\\d{4})",
            typePattern = "(debited|credited)",
            accountPattern = "A/c\\s+([X\\d]+)",
            isActive = true
        )

        every { bankPatternRegistry.findPatternBySender("VK-HDFCBK") } returns hdfcPattern
        coEvery { accountMappingService.findAccountByIdentifier("HDFC Bank", "XXXX1234") } returns 1L

        // When
        val result = extractor.extractTransaction(smsMessage)

        // Then
        assertTrue(result.isSuccessful)
        assertNotNull(result.transaction)
        assertEquals("SWIGGY DELIVERY", result.transaction!!.merchant)
    }
}