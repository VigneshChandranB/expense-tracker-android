package com.expensetracker.domain.permission

import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for GracefulDegradationHandler
 */
class GracefulDegradationHandlerTest {
    
    private lateinit var permissionManager: PermissionManager
    private lateinit var gracefulDegradationHandler: GracefulDegradationHandler
    private lateinit var permissionStateFlow: MutableStateFlow<PermissionStatus>
    
    @Before
    fun setup() {
        permissionManager = mockk()
        permissionStateFlow = MutableStateFlow(PermissionStatus.DENIED)
        
        every { permissionManager.smsPermissionState } returns permissionStateFlow
        
        gracefulDegradationHandler = GracefulDegradationHandler(permissionManager)
    }
    
    @Test
    fun `getAppMode returns AUTOMATIC_WITH_SMS when permissions granted`() = runTest {
        // Given
        permissionStateFlow.value = PermissionStatus.GRANTED
        
        // When
        val appMode = gracefulDegradationHandler.getAppMode().first()
        
        // Then
        assertEquals(AppMode.AUTOMATIC_WITH_SMS, appMode)
    }
    
    @Test
    fun `getAppMode returns MANUAL_ONLY when permissions denied`() = runTest {
        // Given
        permissionStateFlow.value = PermissionStatus.DENIED
        
        // When
        val appMode = gracefulDegradationHandler.getAppMode().first()
        
        // Then
        assertEquals(AppMode.MANUAL_ONLY, appMode)
    }
    
    @Test
    fun `getAppMode returns MANUAL_ONLY when permissions permanently denied`() = runTest {
        // Given
        permissionStateFlow.value = PermissionStatus.PERMANENTLY_DENIED
        
        // When
        val appMode = gracefulDegradationHandler.getAppMode().first()
        
        // Then
        assertEquals(AppMode.MANUAL_ONLY, appMode)
    }
    
    @Test
    fun `isAutomaticDetectionAvailable returns true when permissions granted`() {
        // Given
        every { permissionManager.hasSmsPermissions() } returns true
        
        // When
        val result = gracefulDegradationHandler.isAutomaticDetectionAvailable()
        
        // Then
        assertTrue(result)
    }
    
    @Test
    fun `isAutomaticDetectionAvailable returns false when permissions denied`() {
        // Given
        every { permissionManager.hasSmsPermissions() } returns false
        
        // When
        val result = gracefulDegradationHandler.isAutomaticDetectionAvailable()
        
        // Then
        assertFalse(result)
    }
    
    @Test
    fun `getCapabilityMessage returns automatic message when permissions granted`() {
        // Given
        every { permissionManager.hasSmsPermissions() } returns true
        
        // When
        val message = gracefulDegradationHandler.getCapabilityMessage()
        
        // Then
        assertTrue(message.contains("Automatic transaction detection is active"))
    }
    
    @Test
    fun `getCapabilityMessage returns manual message when permissions denied`() {
        // Given
        every { permissionManager.hasSmsPermissions() } returns false
        
        // When
        val message = gracefulDegradationHandler.getCapabilityMessage()
        
        // Then
        assertTrue(message.contains("Manual mode is active"))
    }
    
    @Test
    fun `getAvailableFeatures includes SMS detection when permissions granted`() {
        // Given
        every { permissionManager.hasSmsPermissions() } returns true
        
        // When
        val features = gracefulDegradationHandler.getAvailableFeatures()
        
        // Then
        assertTrue(features.contains(AppFeature.AUTOMATIC_SMS_DETECTION))
        assertTrue(features.contains(AppFeature.MANUAL_TRANSACTION_ENTRY))
        assertTrue(features.contains(AppFeature.SPENDING_ANALYTICS))
    }
    
    @Test
    fun `getAvailableFeatures excludes SMS detection when permissions denied`() {
        // Given
        every { permissionManager.hasSmsPermissions() } returns false
        
        // When
        val features = gracefulDegradationHandler.getAvailableFeatures()
        
        // Then
        assertFalse(features.contains(AppFeature.AUTOMATIC_SMS_DETECTION))
        assertTrue(features.contains(AppFeature.MANUAL_TRANSACTION_ENTRY))
        assertTrue(features.contains(AppFeature.SPENDING_ANALYTICS))
    }
    
    @Test
    fun `getDisabledFeatures returns empty list when permissions granted`() {
        // Given
        every { permissionManager.hasSmsPermissions() } returns true
        
        // When
        val disabledFeatures = gracefulDegradationHandler.getDisabledFeatures()
        
        // Then
        assertTrue(disabledFeatures.isEmpty())
    }
    
    @Test
    fun `getDisabledFeatures includes SMS detection when permissions denied`() {
        // Given
        every { permissionManager.hasSmsPermissions() } returns false
        
        // When
        val disabledFeatures = gracefulDegradationHandler.getDisabledFeatures()
        
        // Then
        assertTrue(disabledFeatures.contains(AppFeature.AUTOMATIC_SMS_DETECTION))
        assertEquals(1, disabledFeatures.size)
    }
}