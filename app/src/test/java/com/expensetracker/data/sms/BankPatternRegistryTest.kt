package com.expensetracker.data.sms

import com.expensetracker.domain.model.SmsPattern
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*

class BankPatternRegistryTest {

    private lateinit var registry: InMemoryBankPatternRegistry

    @Before
    fun setup() {
        registry = InMemoryBankPatternRegistry()
    }

    @Test
    fun `should initialize with default patterns`() = runTest {
        // When
        val patterns = registry.getAllPatterns()

        // Then
        assertTrue(patterns.isNotEmpty())
        assertTrue(patterns.any { it.bankName == "HDFC Bank" })
        assertTrue(patterns.any { it.bankName == "ICICI Bank" })
        assertTrue(patterns.any { it.bankName == "State Bank of India" })
        assertTrue(patterns.any { it.bankName == "Axis Bank" })
        assertTrue(patterns.any { it.bankName == "Kotak Mahindra Bank" })
    }

    @Test
    fun `should register new pattern successfully`() = runTest {
        // Given
        val newPattern = SmsPattern(
            id = 0,
            bankName = "Test Bank",
            senderPattern = ".*TESTBANK.*",
            amountPattern = "Rs\\.([\\d,]+)",
            merchantPattern = "at\\s+([A-Za-z0-9\\s]+)",
            datePattern = "(\\d{2}-\\d{2}-\\d{4})",
            typePattern = "(debited|credited)",
            isActive = true
        )

        // When
        registry.registerPattern(newPattern)
        val patterns = registry.getAllPatterns()

        // Then
        assertTrue(patterns.any { it.bankName == "Test Bank" })
    }

    @Test
    fun `should assign ID to pattern without ID`() = runTest {
        // Given
        val patternWithoutId = SmsPattern(
            bankName = "New Bank",
            senderPattern = ".*NEWBANK.*",
            amountPattern = "Rs\\.([\\d,]+)",
            merchantPattern = "at\\s+([A-Za-z0-9\\s]+)",
            datePattern = "(\\d{2}-\\d{2}-\\d{4})",
            typePattern = "(debited|credited)",
            isActive = true
        )

        // When
        registry.registerPattern(patternWithoutId)
        val patterns = registry.getAllPatterns()
        val registeredPattern = patterns.find { it.bankName == "New Bank" }

        // Then
        assertNotNull(registeredPattern)
        assertTrue(registeredPattern.id > 0)
    }

    @Test
    fun `should get patterns by bank name`() = runTest {
        // Given
        val hdfcPattern = SmsPattern(
            bankName = "HDFC Bank",
            senderPattern = ".*HDFC.*",
            amountPattern = "Rs\\.([\\d,]+)",
            merchantPattern = "at\\s+([A-Za-z0-9\\s]+)",
            datePattern = "(\\d{2}-\\d{2}-\\d{4})",
            typePattern = "(debited|credited)",
            isActive = true
        )
        registry.registerPattern(hdfcPattern)

        // When
        val hdfcPatterns = registry.getPatternsByBank("HDFC Bank")

        // Then
        assertTrue(hdfcPatterns.isNotEmpty())
        assertTrue(hdfcPatterns.all { it.bankName.equals("HDFC Bank", ignoreCase = true) })
        assertTrue(hdfcPatterns.all { it.isActive })
    }

    @Test
    fun `should find pattern by sender`() = runTest {
        // When
        val pattern = registry.findPatternBySender("VK-HDFCBK")

        // Then
        assertNotNull(pattern)
        assertEquals("HDFC Bank", pattern.bankName)
    }

    @Test
    fun `should return null for unknown sender`() = runTest {
        // When
        val pattern = registry.findPatternBySender("UNKNOWN-SENDER")

        // Then
        assertNull(pattern)
    }

    @Test
    fun `should update existing pattern`() = runTest {
        // Given
        val originalPattern = SmsPattern(
            id = 999,
            bankName = "Test Bank",
            senderPattern = ".*TEST.*",
            amountPattern = "Rs\\.([\\d,]+)",
            merchantPattern = "at\\s+([A-Za-z0-9\\s]+)",
            datePattern = "(\\d{2}-\\d{2}-\\d{4})",
            typePattern = "(debited|credited)",
            isActive = true
        )
        registry.registerPattern(originalPattern)

        val updatedPattern = originalPattern.copy(
            amountPattern = "Amount:\\s*Rs\\.([\\d,]+)"
        )

        // When
        registry.updatePattern(updatedPattern)
        val patterns = registry.getAllPatterns()
        val retrievedPattern = patterns.find { it.id == 999L }

        // Then
        assertNotNull(retrievedPattern)
        assertEquals("Amount:\\s*Rs\\.([\\d,]+)", retrievedPattern.amountPattern)
    }

    @Test
    fun `should deactivate pattern`() = runTest {
        // Given
        val pattern = SmsPattern(
            id = 888,
            bankName = "Test Bank",
            senderPattern = ".*TEST.*",
            amountPattern = "Rs\\.([\\d,]+)",
            merchantPattern = "at\\s+([A-Za-z0-9\\s]+)",
            datePattern = "(\\d{2}-\\d{2}-\\d{4})",
            typePattern = "(debited|credited)",
            isActive = true
        )
        registry.registerPattern(pattern)

        // When
        registry.deactivatePattern(888)
        val patterns = registry.getAllPatterns()
        val deactivatedPattern = patterns.find { it.id == 888L }

        // Then
        assertNotNull(deactivatedPattern)
        assertTrue(!deactivatedPattern.isActive)
    }

    @Test
    fun `should activate pattern`() = runTest {
        // Given
        val pattern = SmsPattern(
            id = 777,
            bankName = "Test Bank",
            senderPattern = ".*TEST.*",
            amountPattern = "Rs\\.([\\d,]+)",
            merchantPattern = "at\\s+([A-Za-z0-9\\s]+)",
            datePattern = "(\\d{2}-\\d{2}-\\d{4})",
            typePattern = "(debited|credited)",
            isActive = false
        )
        registry.registerPattern(pattern)

        // When
        registry.activatePattern(777)
        val patterns = registry.getAllPatterns()
        val activatedPattern = patterns.find { it.id == 777L }

        // Then
        assertNotNull(activatedPattern)
        assertTrue(activatedPattern.isActive)
    }

    @Test
    fun `should delete pattern`() = runTest {
        // Given
        val pattern = SmsPattern(
            id = 666,
            bankName = "Test Bank",
            senderPattern = ".*TEST.*",
            amountPattern = "Rs\\.([\\d,]+)",
            merchantPattern = "at\\s+([A-Za-z0-9\\s]+)",
            datePattern = "(\\d{2}-\\d{2}-\\d{4})",
            typePattern = "(debited|credited)",
            isActive = true
        )
        registry.registerPattern(pattern)

        // When
        registry.deletePattern(666)
        val patterns = registry.getAllPatterns()
        val deletedPattern = patterns.find { it.id == 666L }

        // Then
        assertNull(deletedPattern)
    }

    @Test
    fun `should handle case insensitive bank name search`() = runTest {
        // When
        val patterns1 = registry.getPatternsByBank("hdfc bank")
        val patterns2 = registry.getPatternsByBank("HDFC BANK")
        val patterns3 = registry.getPatternsByBank("Hdfc Bank")

        // Then
        assertTrue(patterns1.isNotEmpty())
        assertEquals(patterns1.size, patterns2.size)
        assertEquals(patterns2.size, patterns3.size)
    }

    @Test
    fun `should handle regex patterns correctly`() = runTest {
        // Given - Test with actual HDFC sender variations
        val senders = listOf(
            "VK-HDFCBK",
            "HDFCBANK",
            "HDFC-BANK",
            "VM-HDFC"
        )

        // When & Then
        senders.forEach { sender ->
            val pattern = registry.findPatternBySender(sender)
            assertNotNull(pattern, "Should find pattern for sender: $sender")
            assertEquals("HDFC Bank", pattern.bankName)
        }
    }

    @Test
    fun `should not return inactive patterns in bank search`() = runTest {
        // Given
        val activePattern = SmsPattern(
            id = 100,
            bankName = "Active Bank",
            senderPattern = ".*ACTIVE.*",
            amountPattern = "Rs\\.([\\d,]+)",
            merchantPattern = "at\\s+([A-Za-z0-9\\s]+)",
            datePattern = "(\\d{2}-\\d{2}-\\d{4})",
            typePattern = "(debited|credited)",
            isActive = true
        )
        
        val inactivePattern = SmsPattern(
            id = 101,
            bankName = "Active Bank",
            senderPattern = ".*INACTIVE.*",
            amountPattern = "Rs\\.([\\d,]+)",
            merchantPattern = "at\\s+([A-Za-z0-9\\s]+)",
            datePattern = "(\\d{2}-\\d{2}-\\d{4})",
            typePattern = "(debited|credited)",
            isActive = false
        )

        registry.registerPattern(activePattern)
        registry.registerPattern(inactivePattern)

        // When
        val patterns = registry.getPatternsByBank("Active Bank")

        // Then
        assertEquals(1, patterns.size)
        assertTrue(patterns.all { it.isActive })
    }
}