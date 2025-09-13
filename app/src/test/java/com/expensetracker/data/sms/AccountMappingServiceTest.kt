package com.expensetracker.data.sms

import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*

class AccountMappingServiceTest {

    private lateinit var mappingService: InMemoryAccountMappingService

    @Before
    fun setup() {
        mappingService = InMemoryAccountMappingService()
    }

    @Test
    fun `should create account mapping successfully`() = runTest {
        // When
        mappingService.createMapping(1L, "HDFC Bank", "XXXX1234")
        val mappings = mappingService.getAllMappings()

        // Then
        assertEquals(1, mappings.size)
        val mapping = mappings.first()
        assertEquals(1L, mapping.accountId)
        assertEquals("HDFC Bank", mapping.bankName)
        assertEquals("XXXX1234", mapping.accountIdentifier)
        assertTrue(mapping.isActive)
    }

    @Test
    fun `should not create duplicate mappings`() = runTest {
        // Given
        mappingService.createMapping(1L, "HDFC Bank", "XXXX1234")

        // When - Try to create the same mapping again
        mappingService.createMapping(1L, "HDFC Bank", "XXXX1234")
        val mappings = mappingService.getAllMappings()

        // Then
        assertEquals(1, mappings.size)
    }

    @Test
    fun `should find account by identifier`() = runTest {
        // Given
        mappingService.createMapping(1L, "HDFC Bank", "XXXX1234")
        mappingService.createMapping(2L, "ICICI Bank", "YYYY5678")

        // When
        val accountId1 = mappingService.findAccountByIdentifier("HDFC Bank", "XXXX1234")
        val accountId2 = mappingService.findAccountByIdentifier("ICICI Bank", "YYYY5678")
        val accountId3 = mappingService.findAccountByIdentifier("SBI", "ZZZZ9999")

        // Then
        assertEquals(1L, accountId1)
        assertEquals(2L, accountId2)
        assertNull(accountId3)
    }

    @Test
    fun `should handle case insensitive bank name matching`() = runTest {
        // Given
        mappingService.createMapping(1L, "HDFC Bank", "XXXX1234")

        // When
        val accountId1 = mappingService.findAccountByIdentifier("hdfc bank", "XXXX1234")
        val accountId2 = mappingService.findAccountByIdentifier("HDFC BANK", "XXXX1234")
        val accountId3 = mappingService.findAccountByIdentifier("Hdfc Bank", "XXXX1234")

        // Then
        assertEquals(1L, accountId1)
        assertEquals(1L, accountId2)
        assertEquals(1L, accountId3)
    }

    @Test
    fun `should get mappings for specific account`() = runTest {
        // Given
        mappingService.createMapping(1L, "HDFC Bank", "XXXX1234")
        mappingService.createMapping(1L, "HDFC Bank", "YYYY5678")
        mappingService.createMapping(2L, "ICICI Bank", "ZZZZ9999")

        // When
        val account1Mappings = mappingService.getMappingsForAccount(1L)
        val account2Mappings = mappingService.getMappingsForAccount(2L)
        val account3Mappings = mappingService.getMappingsForAccount(3L)

        // Then
        assertEquals(2, account1Mappings.size)
        assertEquals(1, account2Mappings.size)
        assertEquals(0, account3Mappings.size)
        assertTrue(account1Mappings.all { it.accountId == 1L })
        assertTrue(account2Mappings.all { it.accountId == 2L })
    }

    @Test
    fun `should update mapping successfully`() = runTest {
        // Given
        mappingService.createMapping(1L, "HDFC Bank", "XXXX1234")
        val mappings = mappingService.getAllMappings()
        val originalMapping = mappings.first()

        val updatedMapping = originalMapping.copy(
            accountIdentifier = "YYYY5678"
        )

        // When
        mappingService.updateMapping(updatedMapping)
        val updatedMappings = mappingService.getAllMappings()
        val retrievedMapping = updatedMappings.find { it.id == originalMapping.id }

        // Then
        assertNotNull(retrievedMapping)
        assertEquals("YYYY5678", retrievedMapping.accountIdentifier)
    }

    @Test
    fun `should deactivate mapping`() = runTest {
        // Given
        mappingService.createMapping(1L, "HDFC Bank", "XXXX1234")
        val mappings = mappingService.getAllMappings()
        val mapping = mappings.first()

        // When
        mappingService.deactivateMapping(mapping.id)
        val updatedMappings = mappingService.getAllMappings()
        val deactivatedMapping = updatedMappings.find { it.id == mapping.id }

        // Then
        assertNotNull(deactivatedMapping)
        assertTrue(!deactivatedMapping.isActive)
    }

    @Test
    fun `should activate mapping`() = runTest {
        // Given
        mappingService.createMapping(1L, "HDFC Bank", "XXXX1234")
        val mappings = mappingService.getAllMappings()
        val mapping = mappings.first()
        
        // Deactivate first
        mappingService.deactivateMapping(mapping.id)

        // When
        mappingService.activateMapping(mapping.id)
        val updatedMappings = mappingService.getAllMappings()
        val activatedMapping = updatedMappings.find { it.id == mapping.id }

        // Then
        assertNotNull(activatedMapping)
        assertTrue(activatedMapping.isActive)
    }

    @Test
    fun `should delete mapping`() = runTest {
        // Given
        mappingService.createMapping(1L, "HDFC Bank", "XXXX1234")
        val mappings = mappingService.getAllMappings()
        val mapping = mappings.first()

        // When
        mappingService.deleteMapping(mapping.id)
        val updatedMappings = mappingService.getAllMappings()

        // Then
        assertEquals(0, updatedMappings.size)
    }

    @Test
    fun `should not find inactive mappings`() = runTest {
        // Given
        mappingService.createMapping(1L, "HDFC Bank", "XXXX1234")
        val mappings = mappingService.getAllMappings()
        val mapping = mappings.first()
        
        // Deactivate the mapping
        mappingService.deactivateMapping(mapping.id)

        // When
        val accountId = mappingService.findAccountByIdentifier("HDFC Bank", "XXXX1234")

        // Then
        assertNull(accountId)
    }

    @Test
    fun `should handle multiple accounts for same bank`() = runTest {
        // Given
        mappingService.createMapping(1L, "HDFC Bank", "XXXX1234")
        mappingService.createMapping(2L, "HDFC Bank", "YYYY5678")
        mappingService.createMapping(3L, "HDFC Bank", "ZZZZ9999")

        // When
        val accountId1 = mappingService.findAccountByIdentifier("HDFC Bank", "XXXX1234")
        val accountId2 = mappingService.findAccountByIdentifier("HDFC Bank", "YYYY5678")
        val accountId3 = mappingService.findAccountByIdentifier("HDFC Bank", "ZZZZ9999")

        // Then
        assertEquals(1L, accountId1)
        assertEquals(2L, accountId2)
        assertEquals(3L, accountId3)
    }

    @Test
    fun `should handle same account identifier for different banks`() = runTest {
        // Given
        mappingService.createMapping(1L, "HDFC Bank", "1234")
        mappingService.createMapping(2L, "ICICI Bank", "1234")

        // When
        val hdfcAccountId = mappingService.findAccountByIdentifier("HDFC Bank", "1234")
        val iciciAccountId = mappingService.findAccountByIdentifier("ICICI Bank", "1234")

        // Then
        assertEquals(1L, hdfcAccountId)
        assertEquals(2L, iciciAccountId)
    }

    @Test
    fun `should create multiple mappings for same account`() = runTest {
        // Given - One account can have multiple identifiers (e.g., different card numbers)
        mappingService.createMapping(1L, "HDFC Bank", "XXXX1234")
        mappingService.createMapping(1L, "HDFC Bank", "YYYY5678")

        // When
        val mappings = mappingService.getMappingsForAccount(1L)

        // Then
        assertEquals(2, mappings.size)
        assertTrue(mappings.any { it.accountIdentifier == "XXXX1234" })
        assertTrue(mappings.any { it.accountIdentifier == "YYYY5678" })
    }
}