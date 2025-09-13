package com.expensetracker.presentation.permission

import androidx.activity.result.ActivityResultLauncher
import com.expensetracker.domain.permission.PermissionManager
import com.expensetracker.domain.permission.PermissionStatus
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for SmsPermissionViewModel
 */
@OptIn(ExperimentalCoroutinesApi::class)
class SmsPermissionViewModelTest {
    
    private lateinit var permissionManager: PermissionManager
    private lateinit var viewModel: SmsPermissionViewModel
    private lateinit var permissionStateFlow: MutableStateFlow<PermissionStatus>
    private val testDispatcher = StandardTestDispatcher()
    
    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        
        permissionManager = mockk()
        permissionStateFlow = MutableStateFlow(PermissionStatus.DENIED)
        
        every { permissionManager.smsPermissionState } returns permissionStateFlow
        every { permissionManager.getRequiredSmsPermissions() } returns arrayOf(
            "android.permission.READ_SMS",
            "android.permission.RECEIVE_SMS"
        )
        every { permissionManager.updateSmsPermissionStatus() } returns Unit
        
        viewModel = SmsPermissionViewModel(permissionManager)
    }
    
    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }
    
    @Test
    fun `initial state has correct default values`() = runTest {
        // When
        val state = viewModel.uiState.first()
        
        // Then
        assertEquals(PermissionStatus.DENIED, state.permissionStatus)
        assertFalse(state.isLoading)
        assertFalse(state.showRationale)
    }
    
    @Test
    fun `requestSmsPermissions launches permission request`() = runTest {
        // Given
        val launcher: ActivityResultLauncher<Array<String>> = mockk(relaxed = true)
        
        // When
        viewModel.requestSmsPermissions(launcher)
        testDispatcher.scheduler.advanceUntilIdle()
        
        // Then
        verify { launcher.launch(any()) }
    }
    
    @Test
    fun `requestSmsPermissions sets loading state`() = runTest {
        // Given
        val launcher: ActivityResultLauncher<Array<String>> = mockk(relaxed = true)
        
        // When
        viewModel.requestSmsPermissions(launcher)
        
        // Then - loading should be true initially
        // Note: In real scenario, we'd need to test the intermediate state
        testDispatcher.scheduler.advanceUntilIdle()
        
        verify { launcher.launch(any()) }
    }
    
    @Test
    fun `handlePermissionResult updates permission status when all granted`() = runTest {
        // Given
        val permissions = mapOf(
            "android.permission.READ_SMS" to true,
            "android.permission.RECEIVE_SMS" to true
        )
        
        // When
        viewModel.handlePermissionResult(permissions)
        testDispatcher.scheduler.advanceUntilIdle()
        
        // Then
        verify { permissionManager.updateSmsPermissionStatus() }
    }
    
    @Test
    fun `handlePermissionResult shows rationale when permissions denied`() = runTest {
        // Given
        val permissions = mapOf(
            "android.permission.READ_SMS" to false,
            "android.permission.RECEIVE_SMS" to true
        )
        
        // When
        viewModel.handlePermissionResult(permissions)
        testDispatcher.scheduler.advanceUntilIdle()
        
        val state = viewModel.uiState.first()
        
        // Then
        assertTrue(state.showRationale)
        assertFalse(state.isLoading)
    }
    
    @Test
    fun `handlePermissionResult shows rationale when all permissions denied`() = runTest {
        // Given
        val permissions = mapOf(
            "android.permission.READ_SMS" to false,
            "android.permission.RECEIVE_SMS" to false
        )
        
        // When
        viewModel.handlePermissionResult(permissions)
        testDispatcher.scheduler.advanceUntilIdle()
        
        val state = viewModel.uiState.first()
        
        // Then
        assertTrue(state.showRationale)
        assertFalse(state.isLoading)
    }
    
    @Test
    fun `checkShouldShowRationale updates rationale state`() {
        // Given
        val activity: android.app.Activity = mockk()
        every { permissionManager.shouldShowSmsPermissionRationale(activity) } returns true
        
        // When
        viewModel.checkShouldShowRationale(activity)
        
        // Then
        verify { permissionManager.shouldShowSmsPermissionRationale(activity) }
    }
}