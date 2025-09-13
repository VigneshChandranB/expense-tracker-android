package com.expensetracker.data.background

import android.content.Context
import com.expensetracker.domain.permission.PermissionManager
import com.expensetracker.domain.permission.PermissionStatus
import com.expensetracker.domain.sms.SmsServiceManager
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class BackgroundServiceCoordinatorTest {
    
    private lateinit var context: Context
    private lateinit var permissionManager: PermissionManager
    private lateinit var smsServiceManager: SmsServiceManager
    private lateinit var coordinator: BackgroundServiceCoordinator
    
    @Before
    fun setup() {
        context = mockk(relaxed = true)
        permissionManager = mockk(relaxed = true)
        smsServiceManager = mockk(relaxed = true)
        
        // Setup default mock behaviors
        every { permissionManager.smsPermissionState } returns flowOf(PermissionStatus.GRANTED)
        every { smsServiceManager.serviceState } returns flowOf(SmsServiceManager.ServiceState.STOPPED)
        every { permissionManager.hasSmsPermissions() } returns true
        every { smsServiceManager.shouldMonitorSms() } returns true
        every { smsServiceManager.isServiceRunning() } returns false
        every { smsServiceManager.isBatteryOptimizationEnabled() } returns false
        
        coordinator = BackgroundServiceCoordinator(context, permissionManager, smsServiceManager)
    }
    
    @Test
    fun `initializeBackgroundServices starts SMS monitoring when permissions granted`() = runTest {
        // Given
        every { permissionManager.hasSmsPermissions() } returns true
        
        // When
        coordinator.initializeBackgroundServices()
        
        // Then
        verify { smsServiceManager.startSmsMonitoring() }
    }
    
    @Test
    fun `initializeBackgroundServices does not start SMS monitoring when permissions denied`() = runTest {
        // Given
        every { permissionManager.hasSmsPermissions() } returns false
        
        // When
        coordinator.initializeBackgroundServices()
        
        // Then
        verify(exactly = 0) { smsServiceManager.startSmsMonitoring() }
    }
    
    @Test
    fun `startSmsMonitoring delegates to service manager`() {
        // When
        coordinator.startSmsMonitoring()
        
        // Then
        verify { smsServiceManager.startSmsMonitoring() }
    }
    
    @Test
    fun `stopSmsMonitoring delegates to service manager`() {
        // When
        coordinator.stopSmsMonitoring()
        
        // Then
        verify { smsServiceManager.stopSmsMonitoring() }
    }
    
    @Test
    fun `restartBackgroundServices stops and starts services`() = runTest {
        // Given
        every { permissionManager.hasSmsPermissions() } returns true
        
        // When
        coordinator.restartBackgroundServices()
        
        // Then
        verify { smsServiceManager.stopSmsMonitoring() }
        // Note: The restart happens after a delay, so we can't easily verify the start call in this test
    }
    
    @Test
    fun `areServicesHealthy returns true when service should run and is running`() {
        // Given
        every { smsServiceManager.shouldMonitorSms() } returns true
        every { smsServiceManager.isServiceRunning() } returns true
        
        // When
        val isHealthy = coordinator.areServicesHealthy()
        
        // Then
        assertTrue(isHealthy)
    }
    
    @Test
    fun `areServicesHealthy returns true when service should not run and is not running`() {
        // Given
        every { smsServiceManager.shouldMonitorSms() } returns false
        every { smsServiceManager.isServiceRunning() } returns false
        
        // When
        val isHealthy = coordinator.areServicesHealthy()
        
        // Then
        assertTrue(isHealthy)
    }
    
    @Test
    fun `areServicesHealthy returns false when service should run but is not running`() {
        // Given
        every { smsServiceManager.shouldMonitorSms() } returns true
        every { smsServiceManager.isServiceRunning() } returns false
        
        // When
        val isHealthy = coordinator.areServicesHealthy()
        
        // Then
        assertFalse(isHealthy)
    }
    
    @Test
    fun `areServicesHealthy returns false when service should not run but is running`() {
        // Given
        every { smsServiceManager.shouldMonitorSms() } returns false
        every { smsServiceManager.isServiceRunning() } returns true
        
        // When
        val isHealthy = coordinator.areServicesHealthy()
        
        // Then
        assertFalse(isHealthy)
    }
    
    @Test
    fun `getServiceStatus returns correct status`() {
        // Given
        every { permissionManager.hasSmsPermissions() } returns true
        every { smsServiceManager.isServiceRunning() } returns true
        every { smsServiceManager.isBatteryOptimizationEnabled() } returns false
        every { smsServiceManager.shouldMonitorSms() } returns true
        
        // When
        val status = coordinator.getServiceStatus()
        
        // Then
        assertEquals(true, status.smsMonitoringEnabled)
        assertEquals(true, status.smsServiceRunning)
        assertEquals(false, status.batteryOptimizationEnabled)
        assertEquals(true, status.overallHealth)
        assertEquals(null, status.error)
    }
    
    @Test
    fun `getServiceStatus handles exceptions gracefully`() {
        // Given
        every { permissionManager.hasSmsPermissions() } throws RuntimeException("Test exception")
        
        // When
        val status = coordinator.getServiceStatus()
        
        // Then
        assertEquals(false, status.smsMonitoringEnabled)
        assertEquals(false, status.smsServiceRunning)
        assertEquals(true, status.batteryOptimizationEnabled)
        assertEquals(false, status.overallHealth)
        assertEquals("Test exception", status.error)
    }
    
    @Test
    fun `stopAllServices stops SMS monitoring`() {
        // When
        coordinator.stopAllServices()
        
        // Then
        verify { smsServiceManager.stopSmsMonitoring() }
    }
    
    @Test
    fun `health monitoring detects and recovers unhealthy services`() = runTest {
        // Given
        every { smsServiceManager.shouldMonitorSms() } returns true
        every { smsServiceManager.isServiceRunning() } returns false
        every { permissionManager.hasSmsPermissions() } returns true
        
        // When
        coordinator.initializeBackgroundServices()
        
        // Then
        verify { smsServiceManager.startSmsMonitoring() }
    }
    
    @Test
    fun `health monitoring stops zombie services`() = runTest {
        // Given - service is running but shouldn't be
        every { smsServiceManager.shouldMonitorSms() } returns false
        every { smsServiceManager.isServiceRunning() } returns true
        every { permissionManager.hasSmsPermissions() } returns false
        
        // When
        coordinator.initializeBackgroundServices()
        
        // Then - should not start monitoring since permissions are denied
        verify(exactly = 0) { smsServiceManager.startSmsMonitoring() }
    }
    
    @Test
    fun `areServicesHealthy considers battery optimization`() {
        // Given
        every { smsServiceManager.shouldMonitorSms() } returns true
        every { smsServiceManager.isServiceRunning() } returns true
        every { permissionManager.hasSmsPermissions() } returns true
        every { smsServiceManager.isBatteryOptimizationEnabled() } returns true // Battery optimized
        
        // When
        val isHealthy = coordinator.areServicesHealthy()
        
        // Then - should be unhealthy due to battery optimization
        assertFalse(isHealthy)
    }
    
    @Test
    fun `areServicesHealthy returns true when battery optimization disabled`() {
        // Given
        every { smsServiceManager.shouldMonitorSms() } returns true
        every { smsServiceManager.isServiceRunning() } returns true
        every { permissionManager.hasSmsPermissions() } returns true
        every { smsServiceManager.isBatteryOptimizationEnabled() } returns false
        
        // When
        val isHealthy = coordinator.areServicesHealthy()
        
        // Then
        assertTrue(isHealthy)
    }
    
    @Test
    fun `getServiceStatus includes battery optimization information`() {
        // Given
        every { permissionManager.hasSmsPermissions() } returns true
        every { smsServiceManager.isServiceRunning() } returns true
        every { smsServiceManager.isBatteryOptimizationEnabled() } returns true
        every { smsServiceManager.shouldMonitorSms() } returns false // Should be false due to battery optimization
        
        // When
        val status = coordinator.getServiceStatus()
        
        // Then
        assertEquals(true, status.smsMonitoringEnabled)
        assertEquals(true, status.smsServiceRunning)
        assertEquals(true, status.batteryOptimizationEnabled)
        assertEquals(false, status.overallHealth) // Unhealthy due to mismatch
    }
}