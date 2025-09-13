package com.expensetracker.domain.permission

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for PermissionManager
 */
class PermissionManagerTest {
    
    private lateinit var context: Context
    private lateinit var permissionManager: PermissionManager
    
    @Before
    fun setup() {
        context = mockk()
        permissionManager = PermissionManager(context)
        mockkStatic(ContextCompat::class)
    }
    
    @After
    fun tearDown() {
        unmockkStatic(ContextCompat::class)
    }
    
    @Test
    fun `hasSmsPermissions returns true when both permissions are granted`() {
        // Given
        every { 
            ContextCompat.checkSelfPermission(context, Manifest.permission.READ_SMS) 
        } returns PackageManager.PERMISSION_GRANTED
        
        every { 
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECEIVE_SMS) 
        } returns PackageManager.PERMISSION_GRANTED
        
        // When
        val result = permissionManager.hasSmsPermissions()
        
        // Then
        assertTrue(result)
    }
    
    @Test
    fun `hasSmsPermissions returns false when READ_SMS is denied`() {
        // Given
        every { 
            ContextCompat.checkSelfPermission(context, Manifest.permission.READ_SMS) 
        } returns PackageManager.PERMISSION_DENIED
        
        every { 
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECEIVE_SMS) 
        } returns PackageManager.PERMISSION_GRANTED
        
        // When
        val result = permissionManager.hasSmsPermissions()
        
        // Then
        assertFalse(result)
    }
    
    @Test
    fun `hasSmsPermissions returns false when RECEIVE_SMS is denied`() {
        // Given
        every { 
            ContextCompat.checkSelfPermission(context, Manifest.permission.READ_SMS) 
        } returns PackageManager.PERMISSION_GRANTED
        
        every { 
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECEIVE_SMS) 
        } returns PackageManager.PERMISSION_DENIED
        
        // When
        val result = permissionManager.hasSmsPermissions()
        
        // Then
        assertFalse(result)
    }
    
    @Test
    fun `hasSmsPermissions returns false when both permissions are denied`() {
        // Given
        every { 
            ContextCompat.checkSelfPermission(context, Manifest.permission.READ_SMS) 
        } returns PackageManager.PERMISSION_DENIED
        
        every { 
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECEIVE_SMS) 
        } returns PackageManager.PERMISSION_DENIED
        
        // When
        val result = permissionManager.hasSmsPermissions()
        
        // Then
        assertFalse(result)
    }
    
    @Test
    fun `getRequiredSmsPermissions returns correct permissions array`() {
        // When
        val permissions = permissionManager.getRequiredSmsPermissions()
        
        // Then
        assertEquals(2, permissions.size)
        assertTrue(permissions.contains(Manifest.permission.READ_SMS))
        assertTrue(permissions.contains(Manifest.permission.RECEIVE_SMS))
    }
    
    @Test
    fun `smsPermissionState emits GRANTED when permissions are granted`() = runTest {
        // Given
        every { 
            ContextCompat.checkSelfPermission(context, Manifest.permission.READ_SMS) 
        } returns PackageManager.PERMISSION_GRANTED
        
        every { 
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECEIVE_SMS) 
        } returns PackageManager.PERMISSION_GRANTED
        
        // When
        permissionManager.updateSmsPermissionStatus()
        val state = permissionManager.smsPermissionState.first()
        
        // Then
        assertEquals(PermissionStatus.GRANTED, state)
    }
    
    @Test
    fun `smsPermissionState emits DENIED when permissions are denied`() = runTest {
        // Given
        every { 
            ContextCompat.checkSelfPermission(context, Manifest.permission.READ_SMS) 
        } returns PackageManager.PERMISSION_DENIED
        
        every { 
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECEIVE_SMS) 
        } returns PackageManager.PERMISSION_DENIED
        
        // When
        permissionManager.updateSmsPermissionStatus()
        val state = permissionManager.smsPermissionState.first()
        
        // Then
        assertEquals(PermissionStatus.DENIED, state)
    }
}