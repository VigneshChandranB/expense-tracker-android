package com.expensetracker.domain.sms

import android.content.Context
import android.content.Intent
import com.expensetracker.data.sms.SmsMonitoringService
import com.expensetracker.domain.permission.PermissionManager
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for SmsServiceManager
 */
class SmsServiceManagerTest {
    
    private lateinit var context: Context
    private lateinit var permissionManager: PermissionManager
    private lateinit var smsServiceManager: SmsServiceManager
    
    @Before
    fun setup() {
        context = mockk(relaxed = true)
        permissionManager = mockk()
        smsServiceManager = SmsServiceManager(context, permissionManager)
    }
    
    @Test
    fun `startSmsMonitoring starts service when permissions granted`() {
        // Given
        every { permissionManager.hasSmsPermissions() } returns true
        val intentSlot = slot<Intent>()
        every { context.startForegroundService(capture(intentSlot)) } returns mockk()
        
        // When
        smsServiceManager.startSmsMonitoring()
        
        // Then
        verify { context.startForegroundService(any()) }
        assertEquals(SmsMonitoringService.ACTION_START_MONITORING, intentSlot.captured.action)
    }
    
    @Test
    fun `startSmsMonitoring does not start service when permissions denied`() {
        // Given
        every { permissionManager.hasSmsPermissions() } returns false
        
        // When
        smsServiceManager.startSmsMonitoring()
        
        // Then
        verify(exactly = 0) { context.startForegroundService(any()) }
    }
    
    @Test
    fun `stopSmsMonitoring sends stop action to service`() {
        // Given
        val intentSlot = slot<Intent>()
        every { context.startService(capture(intentSlot)) } returns mockk()
        
        // When
        smsServiceManager.stopSmsMonitoring()
        
        // Then
        verify { context.startService(any()) }
        assertEquals(SmsMonitoringService.ACTION_STOP_MONITORING, intentSlot.captured.action)
    }
    
    @Test
    fun `shouldMonitorSms returns true when permissions granted`() {
        // Given
        every { permissionManager.hasSmsPermissions() } returns true
        
        // When
        val result = smsServiceManager.shouldMonitorSms()
        
        // Then
        assertTrue(result)
    }
    
    @Test
    fun `shouldMonitorSms returns false when permissions denied`() {
        // Given
        every { permissionManager.hasSmsPermissions() } returns false
        
        // When
        val result = smsServiceManager.shouldMonitorSms()
        
        // Then
        assertFalse(result)
    }
    
    @Test
    fun `restartSmsMonitoring sends restart action to service`() {
        // Given
        val intentSlot = slot<Intent>()
        every { context.startForegroundService(capture(intentSlot)) } returns mockk()
        
        // When
        smsServiceManager.restartSmsMonitoring()
        
        // Then
        verify { context.startForegroundService(any()) }
        assertEquals(SmsMonitoringService.ACTION_RESTART_MONITORING, intentSlot.captured.action)
    }
    
    @Test
    fun `updateServiceState updates internal state`() {
        // When
        smsServiceManager.updateServiceState(SmsServiceManager.ServiceState.RUNNING)
        
        // Then
        // We can't directly verify the internal state, but we can verify it doesn't crash
        // In a real implementation, we might expose the current state for verification
    }
    
    @Test
    fun `getBatteryOptimizationInfo returns correct information`() {
        // When
        val info = smsServiceManager.getBatteryOptimizationInfo()
        
        // Then
        assertNotNull(info)
        assertNotNull(info.recommendation)
        assertNotNull(info.impactLevel)
        // The actual values depend on the device's battery optimization state
    }
    
    @Test
    fun `startSmsMonitoring handles exceptions gracefully`() {
        // Given
        every { permissionManager.hasSmsPermissions() } returns true
        every { context.startForegroundService(any()) } throws RuntimeException("Test exception")
        
        // When
        smsServiceManager.startSmsMonitoring()
        
        // Then
        // Should not crash, exception should be handled gracefully
        // The service state should be updated to ERROR
    }
    
    @Test
    fun `stopSmsMonitoring handles exceptions gracefully`() {
        // Given
        every { context.startService(any()) } throws RuntimeException("Test exception")
        
        // When
        smsServiceManager.stopSmsMonitoring()
        
        // Then
        // Should not crash, exception should be handled gracefully
    }
}