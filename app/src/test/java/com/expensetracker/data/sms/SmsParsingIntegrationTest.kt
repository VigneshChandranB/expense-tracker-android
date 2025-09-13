package com.expensetracker.data.sms

import com.expensetracker.domain.model.*
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*
import java.math.BigDecimal
import java.util.*

class SmsParsingIntegrationTest {

    private lateinit var bankPatternRegistry: BankPatternRegistry
    private lateinit var accountMappingService: AccountMappingService
    private lateinit var extractor: RegexSmsTransactionExtractor

    @Before
    fun setup() {
        bankPatternRegistry = InMemoryBankPatternRegistry()
        accountMappingService = InMemoryAccountMappingService()
        extractor = RegexSmsTransactionExtractor(bankPatternRegistry, accountMappingService)
        
        // Setup account mappings
        setupAccountMappings()
    }

    private suspend fun setupAccountMappings() {
        accountMappingService.createMapping(1L, "HDFC Bank", "XXXX1234")
        accountMappingService.createMapping(2L, "ICICI Bank", "YYYY5678")
        accountMappingService.createMapping(3L, "State Bank of India", "ZZZZ9999")
        accountMappingService.createMapping(4L, "Axis Bank", "AAAA1111")
        accountMappingService.createMapping(5L, "Kotak Mahindra Bank", "BBBB2222")
    }

    @Test
    fun `should parse HDFC debit transaction correctly`() = runTest {
        // Given
        val smsMessage = SmsMessage(
            id = 1,
            sender = "VK-HDFCBK",
            body = "Rs.2500.00 debited from A/c no XXXX1234 at AMAZON INDIA on 15-01-2024 14:30:25. Avl Bal: Rs.45000.00",
            timestamp = Date()
        )

        // When
        val result = extractor.extractTransaction(smsMessage)

        // Then
        assertTrue(result.isSuccessful)
        assertNotNull(result.transaction)
        assertEquals(BigDecimal("2500.00"), result.transaction!!.amount)
        assertEquals(TransactionType.EXPENSE, result.transaction!!.type)
        assertEquals("AMAZON INDIA", result.transaction!!.merchant)
        assertEquals("XXXX1234", result.transaction!!.accountIdentifier)
        assertTrue(result.confidenceScore > 0.8f)
    }

    @Test
    fun `should parse HDFC credit transaction correctly`() = runTest {
        // Given
        val smsMessage = SmsMessage(
            id = 2,
            sender = "HDFCBANK",
            body = "Rs.50000.00 credited to A/c no XXXX1234 from SALARY CREDIT on 01-02-2024 09:15:30. Avl Bal: Rs.95000.00",
            timestamp = Date()
        )

        // When
        val result = extractor.extractTransaction(smsMessage)

        // Then
        assertTrue(result.isSuccessful)
        assertNotNull(result.transaction)
        assertEquals(BigDecimal("50000.00"), result.transaction!!.amount)
        assertEquals(TransactionType.INCOME, result.transaction!!.type)
        assertEquals("SALARY CREDIT", result.transaction!!.merchant)
    }

    @Test
    fun `should parse ICICI transaction with different format`() = runTest {
        // Given
        val smsMessage = SmsMessage(
            id = 3,
            sender = "ICICIB",
            body = "Rs.1200.50 debited from account ending YYYY5678 at SWIGGY DELIVERY on 20/01/2024 19:45:12",
            timestamp = Date()
        )

        // When
        val result = extractor.extractTransaction(smsMessage)

        // Then
        assertTrue(result.isSuccessful)
        assertNotNull(result.transaction)
        assertEquals(BigDecimal("1200.50"), result.transaction!!.amount)
        assertEquals(TransactionType.EXPENSE, result.transaction!!.type)
        assertEquals("SWIGGY DELIVERY", result.transaction!!.merchant)
        assertEquals("YYYY5678", result.transaction!!.accountIdentifier)
    }

    @Test
    fun `should parse SBI transaction with comma in amount`() = runTest {
        // Given
        val smsMessage = SmsMessage(
            id = 4,
            sender = "SBIIN",
            body = "Rs 15,000 debited from A/c ZZZZ9999 at BIG BAZAAR on 25-01-2024. Balance: Rs 85,000",
            timestamp = Date()
        )

        // When
        val result = extractor.extractTransaction(smsMessage)

        // Then
        assertTrue(result.isSuccessful)
        assertNotNull(result.transaction)
        assertEquals(BigDecimal("15000"), result.transaction!!.amount)
        assertEquals(TransactionType.EXPENSE, result.transaction!!.type)
        assertEquals("BIG BAZAAR", result.transaction!!.merchant)
    }

    @Test
    fun `should parse Axis Bank credit card transaction`() = runTest {
        // Given
        val smsMessage = SmsMessage(
            id = 5,
            sender = "AXISBK",
            body = "Rs.899.00 debited from card ending AAAA1111 at NETFLIX SUBSCRIPTION on 10/02/2024 12:00:00",
            timestamp = Date()
        )

        // When
        val result = extractor.extractTransaction(smsMessage)

        // Then
        assertTrue(result.isSuccessful)
        assertNotNull(result.transaction)
        assertEquals(BigDecimal("899.00"), result.transaction!!.amount)
        assertEquals(TransactionType.EXPENSE, result.transaction!!.type)
        assertEquals("NETFLIX SUBSCRIPTION", result.transaction!!.merchant)
    }

    @Test
    fun `should parse Kotak Bank transaction`() = runTest {
        // Given
        val smsMessage = SmsMessage(
            id = 6,
            sender = "KOTAK",
            body = "Rs.3500.00 debited from A/c BBBB2222 at RELIANCE DIGITAL on 18-01-2024 16:20:45",
            timestamp = Date()
        )

        // When
        val result = extractor.extractTransaction(smsMessage)

        // Then
        assertTrue(result.isSuccessful)
        assertNotNull(result.transaction)
        assertEquals(BigDecimal("3500.00"), result.transaction!!.amount)
        assertEquals(TransactionType.EXPENSE, result.transaction!!.type)
        assertEquals("RELIANCE DIGITAL", result.transaction!!.merchant)
    }

    @Test
    fun `should parse PhonePe UPI transaction`() = runTest {
        // Given
        val smsMessage = SmsMessage(
            id = 7,
            sender = "PHONEPE",
            body = "You paid Rs.250 to UBER INDIA via UPI on 22-01-2024. UPI Ref: 123456789012",
            timestamp = Date()
        )

        // When
        val result = extractor.extractTransaction(smsMessage)

        // Then
        assertTrue(result.isSuccessful)
        assertNotNull(result.transaction)
        assertEquals(BigDecimal("250"), result.transaction!!.amount)
        assertEquals(TransactionType.EXPENSE, result.transaction!!.type)
        assertEquals("UBER INDIA", result.transaction!!.merchant)
    }

    @Test
    fun `should parse Google Pay transaction`() = runTest {
        // Given
        val smsMessage = SmsMessage(
            id = 8,
            sender = "GPAY",
            body = "You received Rs.500.00 from JOHN DOE via Google Pay on 28-01-2024",
            timestamp = Date()
        )

        // When
        val result = extractor.extractTransaction(smsMessage)

        // Then
        assertTrue(result.isSuccessful)
        assertNotNull(result.transaction)
        assertEquals(BigDecimal("500.00"), result.transaction!!.amount)
        assertEquals(TransactionType.INCOME, result.transaction!!.type)
        assertEquals("JOHN DOE", result.transaction!!.merchant)
    }

    @Test
    fun `should parse Paytm wallet transaction`() = runTest {
        // Given
        val smsMessage = SmsMessage(
            id = 9,
            sender = "PAYTM",
            body = "Rs.150 debited from wallet ending 7890 at DOMINOS PIZZA on 30-01-2024 20:15:30",
            timestamp = Date()
        )

        // When
        val result = extractor.extractTransaction(smsMessage)

        // Then
        assertTrue(result.isSuccessful)
        assertNotNull(result.transaction)
        assertEquals(BigDecimal("150"), result.transaction!!.amount)
        assertEquals(TransactionType.EXPENSE, result.transaction!!.type)
        assertEquals("DOMINOS PIZZA", result.transaction!!.merchant)
    }

    @Test
    fun `should handle transfer transactions`() = runTest {
        // Given
        val smsMessage = SmsMessage(
            id = 10,
            sender = "VK-HDFCBK",
            body = "Rs.10000.00 transferred from A/c XXXX1234 to A/c YYYY5678 on 05-02-2024 11:30:00. Ref: TXN123456",
            timestamp = Date()
        )

        // When
        val result = extractor.extractTransaction(smsMessage)

        // Then
        assertTrue(result.isSuccessful)
        assertNotNull(result.transaction)
        assertEquals(BigDecimal("10000.00"), result.transaction!!.amount)
        assertEquals(TransactionType.TRANSFER_OUT, result.transaction!!.type)
    }

    @Test
    fun `should handle ATM withdrawal`() = runTest {
        // Given
        val smsMessage = SmsMessage(
            id = 11,
            sender = "ICICIB",
            body = "Rs.5000.00 withdrawn from ATM at KORAMANGALA BRANCH on 12-02-2024 18:45:00. Card ending YYYY5678",
            timestamp = Date()
        )

        // When
        val result = extractor.extractTransaction(smsMessage)

        // Then
        assertTrue(result.isSuccessful)
        assertNotNull(result.transaction)
        assertEquals(BigDecimal("5000.00"), result.transaction!!.amount)
        assertEquals(TransactionType.EXPENSE, result.transaction!!.type)
        assertEquals("KORAMANGALA BRANCH", result.transaction!!.merchant)
    }

    @Test
    fun `should handle bill payment transaction`() = runTest {
        // Given
        val smsMessage = SmsMessage(
            id = 12,
            sender = "AXISBK",
            body = "Rs.2500.00 debited from A/c AAAA1111 for ELECTRICITY BILL payment on 08-02-2024",
            timestamp = Date()
        )

        // When
        val result = extractor.extractTransaction(smsMessage)

        // Then
        assertTrue(result.isSuccessful)
        assertNotNull(result.transaction)
        assertEquals(BigDecimal("2500.00"), result.transaction!!.amount)
        assertEquals(TransactionType.EXPENSE, result.transaction!!.type)
        assertEquals("ELECTRICITY BILL", result.transaction!!.merchant)
    }

    @Test
    fun `should handle online shopping transaction with special characters`() = runTest {
        // Given
        val smsMessage = SmsMessage(
            id = 13,
            sender = "HDFCBANK",
            body = "Rs.1,299.99 debited from A/c XXXX1234 at FLIPKART.COM on 14-02-2024 21:30:15",
            timestamp = Date()
        )

        // When
        val result = extractor.extractTransaction(smsMessage)

        // Then
        assertTrue(result.isSuccessful)
        assertNotNull(result.transaction)
        assertEquals(BigDecimal("1299.99"), result.transaction!!.amount)
        assertEquals(TransactionType.EXPENSE, result.transaction!!.type)
        assertEquals("FLIPKART.COM", result.transaction!!.merchant)
    }

    @Test
    fun `should handle salary credit with detailed description`() = runTest {
        // Given
        val smsMessage = SmsMessage(
            id = 14,
            sender = "SBIIN",
            body = "Rs.75,000.00 credited to A/c ZZZZ9999 from TECH COMPANY SALARY FEB2024 on 01-03-2024 00:05:00",
            timestamp = Date()
        )

        // When
        val result = extractor.extractTransaction(smsMessage)

        // Then
        assertTrue(result.isSuccessful)
        assertNotNull(result.transaction)
        assertEquals(BigDecimal("75000.00"), result.transaction!!.amount)
        assertEquals(TransactionType.INCOME, result.transaction!!.type)
        assertEquals("TECH COMPANY SALARY FEB2024", result.transaction!!.merchant)
    }

    @Test
    fun `should handle transaction without date and use current time`() = runTest {
        // Given
        val smsMessage = SmsMessage(
            id = 15,
            sender = "KOTAK",
            body = "Rs.500.00 debited from A/c BBBB2222 at LOCAL GROCERY STORE",
            timestamp = Date()
        )

        // When
        val result = extractor.extractTransaction(smsMessage)

        // Then
        assertTrue(result.isSuccessful)
        assertNotNull(result.transaction)
        assertEquals(BigDecimal("500.00"), result.transaction!!.amount)
        assertEquals(TransactionType.EXPENSE, result.transaction!!.type)
        assertEquals("LOCAL GROCERY STORE", result.transaction!!.merchant)
        assertNotNull(result.transaction!!.date)
    }

    @Test
    fun `should handle multiple confidence scoring scenarios`() = runTest {
        val testCases = listOf(
            // High confidence: All fields extracted
            Triple(
                "VK-HDFCBK",
                "Rs.1500.00 debited from A/c no XXXX1234 at AMAZON INDIA on 15-01-2024 14:30:25",
                0.8f
            ),
            // Medium confidence: Missing date
            Triple(
                "ICICIB",
                "Rs.1200.50 debited from account ending YYYY5678 at SWIGGY DELIVERY",
                0.6f
            ),
            // Lower confidence: Missing merchant details
            Triple(
                "SBIIN",
                "Rs.500 debited from A/c ZZZZ9999 on 25-01-2024",
                0.4f
            )
        )

        testCases.forEach { (sender, body, expectedMinConfidence) ->
            val smsMessage = SmsMessage(
                id = 1,
                sender = sender,
                body = body,
                timestamp = Date()
            )

            val result = extractor.extractTransaction(smsMessage)
            assertTrue(result.isSuccessful, "Should successfully parse: $body")
            assertTrue(
                result.confidenceScore >= expectedMinConfidence,
                "Confidence score ${result.confidenceScore} should be >= $expectedMinConfidence for: $body"
            )
        }
    }

    @Test
    fun `should register and use custom bank pattern`() = runTest {
        // Given - Register a custom pattern for a new bank
        val customPattern = SmsPattern(
            bankName = "Custom Bank",
            senderPattern = ".*CUSTOMBANK.*",
            amountPattern = "Amount:\\s*Rs\\.([\\d,]+(?:\\.\\d{2})?)",
            merchantPattern = "Merchant:\\s+([A-Za-z0-9\\s&.-]+?)(?:\\s+Date|\\.|$)",
            datePattern = "Date:\\s*(\\d{2}-\\d{2}-\\d{4})",
            typePattern = "(Debit|Credit)",
            accountPattern = "Account:\\s*([X\\d]+)",
            isActive = true
        )

        extractor.registerBankPattern(customPattern)
        accountMappingService.createMapping(6L, "Custom Bank", "CCCC3333")

        val smsMessage = SmsMessage(
            id = 16,
            sender = "CUSTOMBANK",
            body = "Amount: Rs.750.00 Debit from Account: CCCC3333 Merchant: CUSTOM STORE Date: 20-02-2024",
            timestamp = Date()
        )

        // When
        val result = extractor.extractTransaction(smsMessage)

        // Then
        assertTrue(result.isSuccessful)
        assertNotNull(result.transaction)
        assertEquals(BigDecimal("750.00"), result.transaction!!.amount)
        assertEquals(TransactionType.EXPENSE, result.transaction!!.type)
        assertEquals("CUSTOM STORE", result.transaction!!.merchant)
        assertEquals("CCCC3333", result.transaction!!.accountIdentifier)
    }
}