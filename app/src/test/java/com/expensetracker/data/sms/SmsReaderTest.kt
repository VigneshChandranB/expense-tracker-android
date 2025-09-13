package com.expensetracker.data.sms

import android.content.Context
import com.expensetracker.domain.permission.PermissionManager
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.util.Date

/**
 * Unit tests for SmsReader
 */
class SmsReaderTest {
    
    private lateinit var context: Context
    private lateinit var permissionManager: PermissionManager
    private lateinit var smsReader: SmsReader
    
    @Before
    fun setup() {
        context = mockk()
        permissionManager = mockk()
        smsReader = SmsReader(context, permissionManager)
    }
    
    @Test
    fun `readSmsMessages returns empty list when permissions are denied`() = runTest {
        // Given
        every { permissionManager.hasSmsPermissions() } returns false
        
        // When
        val result = smsReader.readSmsMessages()
        
        // Then
        assertTrue(result.isEmpty())
    }
    
    @Test
    fun `readRecentSmsMessages returns empty list when permissions are denied`() = runTest {
        // Given
        every { permissionManager.hasSmsPermissions() } returns false
        
        // When
        val result = smsReader.readRecentSmsMessages(10)
        
        // Then
        assertTrue(result.isEmpty())
    }
    
    @Test
    fun `isSmsReadingAvailable returns false when permissions are denied`() {
        // Given
        every { permissionManager.hasSmsPermissions() } returns false
        
        // When
        val result = smsReader.isSmsReadingAvailable()
        
        // Then
        assertFalse(result)
    }
    
    @Test
    fun `isSmsReadingAvailable returns true when permissions are granted`() {
        // Given
        every { permissionManager.hasSmsPermissions() } returns true
        
        // When
        val result = smsReader.isSmsReadingAvailable()
        
        // Then
        assertTrue(result)
    }
    
    @Test
    fun `readSmsMessages handles exceptions gracefully`() = runTest {
        // Given
        every { permissionManager.hasSmsPermissions() } returns true
        every { context.contentResolver } throws RuntimeException("Database error")
        
        // When
        val result = smsReader.readSmsMessages()
        
        // Then
        assertTrue(result.isEmpty())
    }
}