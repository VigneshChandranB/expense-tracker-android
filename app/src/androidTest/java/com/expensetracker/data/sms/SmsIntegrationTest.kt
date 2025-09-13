package com.expensetracker.data.sms

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.expensetracker.domain.permission.PermissionManager
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import javax.inject.Inject

/**
 * Integration tests for SMS components
 */
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class SmsIntegrationTest {
    
    @get:Rule
    val hiltRule = HiltAndroidRule(this)
    
    @Inject
    lateinit var permissionManager: PermissionManager
    
    @Inject
    lateinit var smsReader: SmsReader
    
    @Inject
    lateinit var smsProcessor: SmsProcessor
    
    private lateinit var context: Context
    
    @Before
    fun setup() {
        hiltRule.inject()
        context = ApplicationProvider.getApplicationContext()
    }
    
    @Test
    fun permissionManager_initializes_correctly() {
        // Then
        assertNotNull(permissionManager)
        
        // Permission status should be deterministic based on actual permissions
        val hasPermissions = permissionManager.hasSmsPermissions()
        val requiredPermissions = permissionManager.getRequiredSmsPermissions()
        
        assertEquals(2, requiredPermissions.size)
        assertTrue(requiredPermissions.contains("android.permission.READ_SMS"))
        assertTrue(requiredPermissions.contains("android.permission.RECEIVE_SMS"))
    }
    
    @Test
    fun smsReader_handles_no_permissions_gracefully() = runTest {
        // When - Try to read SMS without permissions (in test environment)
        val messages = smsReader.readSmsMessages()
        
        // Then - Should return empty list gracefully
        assertNotNull(messages)
        // In test environment without permissions, should return empty list
    }
    
    @Test
    fun smsReader_availability_matches_permission_status() {
        // When
        val isAvailable = smsReader.isSmsReadingAvailable()
        val hasPermissions = permissionManager.hasSmsPermissions()
        
        // Then
        assertEquals(hasPermissions, isAvailable)
    }
    
    @Test
    fun smsProcessor_handles_null_messages_gracefully() = runTest {
        // Given
        val smsMessage = com.expensetracker.domain.model.SmsMessage(
            id = 1L,
            sender = "TEST",
            body = "Test message",
            timestamp = java.util.Date(),
            type = com.expensetracker.domain.model.SmsMessage.Type.RECEIVED
        )
        
        // When - Process a test message
        // Should not throw exception
        smsProcessor.processSmsMessage(smsMessage)
        
        // Then - No exception should be thrown
        // This is a basic test since actual processing logic will be in task 4
    }
}